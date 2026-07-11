# Nimaz AI Worker (`nimaz-ai`)

A Cloudflare Worker (Hono + TypeScript) that backs Nimaz's opt-in AI search.
It is a **capability registry**: every AI feature is one file under
`src/capabilities/` plus one line in `src/registry.ts`. The single capability is
**`search-assist`** — the app sends only the user's question; Claude answers
from mainstream Islamic knowledge and returns the supporting Quran references
plus related search terms, which the app resolves against its **local**
database (real records become the proof cards and the results list).

The Worker never stores questions or answers. It classifies the caller with
Play Integrity (never blocking — see below), enforces per-device/global daily
limits, calls Claude with a fixed cached prompt and a forced structured-output
tool, and returns strict JSON.

**Billing/auth:** Claude is reached through the Worker's **AI binding** →
the **`nimaz`** Cloudflare **AI Gateway** with **Unified Billing** — Cloudflare
holds the Anthropic credentials and bills the account's AI credits, so **no API
key or gateway token exists in the Worker**. The monthly USD cost cap is the
gateway's **Spend Limit** (dashboard), not Worker code.

## Architecture

```
POST /v1/invoke
  → integrity   (Play Integrity token → trust tier: verified | unverified)
  → rateLimit   (tiered per-device + global daily caps in KV)
  → dispatch    (registry lookup → Zod-validate input → build request
                 → env.AI.run("anthropic/claude-haiku-4-5", …, {gateway:{id:"nimaz"}})
                 → Zod-validate output)
```

The gateway call carries `metadata: { capability }` so the AI Gateway dashboard
breaks spend down per feature (never the question text). A tripped gateway
spend limit / exhausted credits maps to `BUDGET_EXCEEDED` (503), same as the
old in-Worker budget guard, so the app UX is unchanged. Each successful call
logs a structured `ai_usage` line (token counts only) and echoes the usage in
an `x-nimaz-usage` response header — that's how you verify prompt-cache reads.

`GET /v1/health` returns `{ ok: true, capabilities: [...] }` (no auth).

### Integrity never blocks

Attestation used to hard-fail requests (`ATTESTATION_FAILED`), which bricked
the feature for legitimate users whenever a token couldn't be fetched or
Google's API was unreachable. `checkIntegrity` now returns a **trust tier**
instead of throwing:

- `verified` — token decodes with `PLAY_RECOGNIZED` and the device meets
  device or basic integrity (or `SKIP_ATTESTATION=true`).
- `unverified` — missing/short token, no service account configured, Google
  API failure, or a failed verdict.

The tier only selects the per-device daily cap (`DAILY_DEVICE_LIMIT` vs the
much smaller `UNVERIFIED_DAILY_DEVICE_LIMIT`). The AI Gateway spend limit is
the hard cost backstop against abuse.

## API contract

`POST /v1/invoke`

```json
{
  "capability": "search-assist",
  "integrityToken": "<play-integrity-token, may be empty>",
  "deviceId": "<random UUID generated once per install>",
  "input": {
    "question": "What does the Quran say about patience?"
  }
}
```

- `question`: 3–500 chars. Nothing else is sent.

Success `200`:

```json
{
  "answer": "The Quran repeatedly encourages patience (sabr)...",
  "quranRefs": ["2:153", "39:10"],
  "terms": ["patience", "sabr", "hardship", "perseverance"],
  "confidence": "high"
}
```

- `quranRefs`: up to 8 `surah:ayah` references (surah 1–114 enforced; the app
  drops anything that doesn't resolve in its local Quran database).
- `terms`: 4–10 short keywords the app runs through its local library search.

Errors use a typed envelope with the matching HTTP status:

```json
{ "error": { "code": "RATE_LIMITED", "message": "...", "retryAfterSeconds": 3600 } }
```

| code                 | status | when                                                     |
| -------------------- | ------ | -------------------------------------------------------- |
| `INVALID_INPUT`      | 400    | bad envelope / schema / unknown capability               |
| `RATE_LIMITED`       | 429    | per-device (tiered) or global daily cap hit              |
| `UPSTREAM_ERROR`     | 502    | Claude call failed / returned bad shape                  |
| `BUDGET_EXCEEDED`    | 503    | gateway spend limit tripped / AI credits exhausted       |

## Configuration

Non-secret vars live in `wrangler.jsonc`:

| var                             | default | meaning                                                  |
| ------------------------------- | ------- | -------------------------------------------------------- |
| `SKIP_ATTESTATION`              | `false` | `true` forces the verified tier (local testing ONLY)     |
| `DAILY_DEVICE_LIMIT`            | `20`    | requests per verified device per UTC day                 |
| `UNVERIFIED_DAILY_DEVICE_LIMIT` | `5`     | requests per unverified device per UTC day               |
| `DAILY_GLOBAL_LIMIT`            | `500`   | requests across all devices per UTC day                  |

The monthly USD ceiling is **not** a var anymore — set a **Spend limit** on the
`nimaz` AI Gateway in the dashboard (AI → AI Gateway → nimaz → Settings).

The only secret — set with `wrangler secret put`, never committed:

- `GOOGLE_SERVICE_ACCOUNT_JSON` — the full service-account JSON (one string)
  used to mint an OAuth token for the Play Integrity API. Optional — without
  it every request simply runs at the unverified tier.

There is **no Anthropic key**: the AI binding authenticates via Unified
Billing (Cloudflare-managed provider credentials).

## Setup

```bash
cd worker
npm ci                      # or: npm install (first time, to create the lockfile)

# 1. KV namespace: already created and its id is committed in wrangler.jsonc
#    (a resource id, not a secret). To recreate it in another account:
#    npx wrangler kv namespace create NIMAZ_AI_KV
#    then paste the returned id into wrangler.jsonc.

# 2. Set the one secret (production):
npx wrangler secret put GOOGLE_SERVICE_ACCOUNT_JSON

# 3. Cloudflare dashboard (one-time, cannot be done from wrangler):
#    AI → AI Gateway → confirm the "nimaz" gateway exists.
#    AI → AI Gateway → "Credits Available" → Manage → add a payment method,
#    purchase credits, and set auto top-up (Unified Billing).
#    Recommended: set a Spend limit on the "nimaz" gateway as the monthly
#    cost backstop, and enable the gateway's ZDR (Zero-Data-Retention
#    provider endpoints) setting.
```

## Local development

```bash
# Attestation bypassed so you can curl it. The AI binding proxies to the real
# gateway even in dev, so you must be authenticated with Cloudflare
# (`npx wrangler login`, or CLOUDFLARE_API_TOKEN in the environment) —
# there is no Anthropic key to pass.
npx wrangler dev --var SKIP_ATTESTATION:true
```

Sample request (works against `wrangler dev` with `SKIP_ATTESTATION=true`):

```bash
curl -s http://localhost:8787/v1/invoke \
  -H 'content-type: application/json' \
  -d '{
    "capability": "search-assist",
    "integrityToken": "debug-skip",
    "deviceId": "11111111-1111-1111-1111-111111111111",
    "input": {
      "question": "What does the Quran say about patience?"
    }
  }' | jq
```

Expected: a JSON body with `answer`, `quranRefs` (e.g. `["2:153"]`), `terms`,
and `confidence`, plus an `x-nimaz-usage` response header with the token
usage. Run the same question twice and check the second response's header
shows `cache_read_input_tokens > 0` — that proves the system prompt's
`cache_control` survives the gateway.

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
