# Nimaz AI Worker (`nimaz-ai`)

A Cloudflare Worker (Hono + TypeScript) that backs Nimaz's opt-in, proof-driven
AI features. It is a **capability registry**: every AI feature is one file under
`src/capabilities/` plus one line in `src/registry.ts`. The first capability is
**`ask-with-proof`** — grounded Q&A over Quran/Hadith/Dua passages the app
retrieves locally and sends up as evidence.

The Worker never stores questions or answers. It verifies the caller with Play
Integrity, enforces per-device/global/monthly limits, calls Claude with a fixed
grounding prompt and a forced structured-output tool, and returns strict JSON.

## Architecture

```
POST /v1/invoke
  → integrity   (Play Integrity token → Google decodeIntegrityToken)
  → rateLimit   (per-device + global daily caps in KV)
  → budgetGuard (monthly USD budget in KV; pre-call gate + post-call accounting)
  → dispatch    (registry lookup → Zod-validate input → build request
                 → Claude (via AI Gateway if configured) → Zod-validate output)
```

`GET /v1/health` returns `{ ok: true, capabilities: [...] }` (no auth).

## API contract

`POST /v1/invoke`

```json
{
  "capability": "ask-with-proof",
  "integrityToken": "<play-integrity-token>",
  "deviceId": "<random UUID generated once per install>",
  "input": {
    "question": "What does the Quran say about patience?",
    "passages": [
      {
        "id": "quran:2:153",
        "source": "quran",
        "text": "O you who believe, seek help through patience and prayer...",
        "meta": "Surah Al-Baqarah 153 (Sahih Intl)"
      }
    ]
  }
}
```

- `question`: 3–500 chars.
- `passages`: 1–8 items, each `text` ≤ 1200 chars, total passage text ≤ 8000 chars.
- `source`: `quran | hadith | dua`.

Success `200`:

```json
{
  "answer": "The sources describe patience as sought through prayer...",
  "citationIds": ["quran:2:153"],
  "confidence": "high",
  "insufficientEvidence": false
}
```

Errors use a typed envelope with the matching HTTP status:

```json
{ "error": { "code": "RATE_LIMITED", "message": "...", "retryAfterSeconds": 3600 } }
```

| code                 | status | when                                         |
| -------------------- | ------ | -------------------------------------------- |
| `INVALID_INPUT`      | 400    | bad envelope / schema / unknown capability   |
| `ATTESTATION_FAILED` | 401    | Play Integrity verification failed           |
| `RATE_LIMITED`       | 429    | per-device or global daily cap hit           |
| `UPSTREAM_ERROR`     | 502    | Claude call failed / returned bad shape      |
| `BUDGET_EXCEEDED`    | 503    | monthly budget reached                       |

## Configuration

Non-secret vars live in `wrangler.jsonc`:

| var                  | default | meaning                                             |
| -------------------- | ------- | --------------------------------------------------- |
| `SKIP_ATTESTATION`   | `false` | `true` bypasses Play Integrity (testing ONLY)       |
| `DAILY_DEVICE_LIMIT` | `20`    | requests per device per UTC day                     |
| `DAILY_GLOBAL_LIMIT` | `500`   | requests across all devices per UTC day             |
| `MONTHLY_BUDGET_USD` | `10`    | monthly spend ceiling before `BUDGET_EXCEEDED`      |
| `AI_GATEWAY_BASE_URL`| `""`    | route Claude via Cloudflare AI Gateway when set     |

Secrets are **not** in any file — set them with `wrangler secret put`:

- `ANTHROPIC_API_KEY` — your Anthropic API key.
- `GOOGLE_SERVICE_ACCOUNT_JSON` — the full service-account JSON (one string)
  used to mint an OAuth token for the Play Integrity API. Optional while
  `SKIP_ATTESTATION=true`.

## Setup

```bash
cd worker
npm ci                      # or: npm install (first time, to create the lockfile)

# 1. Create the KV namespace and paste the returned id into wrangler.jsonc
#    (replace "REPLACE_ME"):
npx wrangler kv namespace create NIMAZ_AI_KV

# 2. Set secrets (production):
npx wrangler secret put ANTHROPIC_API_KEY
npx wrangler secret put GOOGLE_SERVICE_ACCOUNT_JSON

# 3. (optional) Create an AI Gateway named "nimaz" in the Cloudflare dashboard
#    and set AI_GATEWAY_BASE_URL to:
#    https://gateway.ai.cloudflare.com/v1/<account_id>/nimaz/anthropic
```

## Local development

```bash
# Run with attestation bypassed and a real Anthropic key so you can curl it.
ANTHROPIC_API_KEY=sk-ant-... npx wrangler dev --var SKIP_ATTESTATION:true
```

Sample request (works against `wrangler dev` with `SKIP_ATTESTATION=true`):

```bash
curl -s http://localhost:8787/v1/invoke \
  -H 'content-type: application/json' \
  -d '{
    "capability": "ask-with-proof",
    "integrityToken": "debug-skip",
    "deviceId": "11111111-1111-1111-1111-111111111111",
    "input": {
      "question": "What does the Quran say about patience?",
      "passages": [
        {
          "id": "quran:2:153",
          "source": "quran",
          "text": "O you who believe, seek help through patience and prayer. Indeed, Allah is with the patient.",
          "meta": "Surah Al-Baqarah 153 (Sahih Intl)"
        }
      ]
    }
  }' | jq
```

Expected: a JSON body with `answer`, `citationIds: ["quran:2:153"]`,
`confidence`, and `insufficientEvidence: false`.

Health check:

```bash
curl -s http://localhost:8787/v1/health | jq
```

## Tests & typecheck

```bash
npm test              # vitest (Cloudflare Workers pool)
npm run typecheck     # tsc --noEmit
```

## Deploy

CI (`.github/workflows/worker_deploy.yml`) deploys on push to `dev` touching
`worker/**`. Manual deploy:

```bash
npx wrangler deploy
```

## Adding a new capability

1. Create `src/capabilities/<id>.ts` exporting a `Capability<I, O>` (define its
   Zod input/output schemas under `src/schemas/`).
2. Register it in `src/registry.ts`.
3. Add tests under `test/`.

Nothing in the middleware chain or dispatcher changes — the new capability is
reachable at `POST /v1/invoke` with `"capability": "<id>"` immediately.
