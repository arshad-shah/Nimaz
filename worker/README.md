# Nimaz AI Worker (`nimaz-ai`)

A Cloudflare Worker (Hono + TypeScript) that backs Nimaz's opt-in AI search.
It is a **capability registry**: every AI feature is one file under
`src/capabilities/` plus one line in `src/registry.ts`. The single capability is
**`search-assist`** — the app sends only the user's question; Claude answers
from mainstream Islamic knowledge and returns the supporting Quran references
plus related search terms, which the app resolves against its **local**
database (real records become the proof cards and the results list).

The Worker never stores questions or answers. Its only guard is **Play
Integrity** (a caller with no usable token is rejected; a Google-side outage
fails open within a bounded window — see below); it then calls Claude with a fixed cached
prompt and a forced structured-output tool and returns strict JSON. Request
throttling and the monthly cost cap are **not Worker code** — they are the
`nimaz` AI Gateway's Rate Limiting rule and Spend Limit (dashboard).

**Billing/auth:** Claude is reached through the **`nimaz`** Cloudflare
**AI Gateway** (Anthropic provider-native endpoint) with **Unified Billing** —
no provider key is attached, so Cloudflare injects its managed Anthropic
credentials and bills the account's AI credits. The Worker authenticates with
one scoped secret, **`CLOUDFLARE_AI_TOKEN`** (the gateway's authentication
token, sent as `cf-aig-authorization`) — **never an Anthropic key**. The
monthly USD cost cap is the gateway's **Spend Limit** (dashboard), not Worker
code.

## Architecture

```
POST /v1/invoke
  → integrity   (Play Integrity token → verified | unavailable | failed;
                 "failed" rejects — ATTESTATION_FAILED/403. Missing, short or
                 undecodable tokens are "failed", not "unavailable")
  → dispatch    (registry lookup → Zod-validate input → build request
                 → POST gateway.ai.cloudflare.com/v1/{acct}/nimaz/anthropic
                   /v1/messages  (Anthropic-native schema, Unified Billing)
                 → Zod-validate output)
```

The gateway call carries a `cf-aig-metadata: {"capability": …}` header so the
AI Gateway dashboard breaks spend down per feature (never the question text).
A tripped gateway spend limit / exhausted credits maps to `BUDGET_EXCEEDED`
(503); a tripped gateway Rate Limiting rule (429) passes through as
`RATE_LIMITED` (with `retryAfterSeconds` from the gateway's `retry-after`
header when present), so the app UX is unchanged from the old in-Worker
guards.
Each successful call logs a structured `ai_usage` line (token counts only) and
echoes the usage in an `x-nimaz-usage` response header. (Two other transports
were live-tested and rejected: the AI *binding* fails the Anthropic-native
schema with `7003: User Input Error`, and `api.cloudflare.com/…/ai/v1/messages`
rejects gateway-auth tokens with `401/10000` — the provider-native gateway URL
is the path that works.)

`GET /v1/health` returns `{ ok: true, capabilities: [...] }` (no auth).

### Integrity is the only guard — and it fails open only for our own faults

`checkIntegrity` returns one of three outcomes:

- `verified` — token decodes with `PLAY_RECOGNIZED`, the device meets device
  or basic integrity, and `requestDetails.requestPackageName` is
  `com.arshadshah.nimaz` (or `SKIP_ATTESTATION=true`). Proceeds.
- `failed` — **anything the caller controls**: no token, a token shorter than
  10 chars, a token Google refuses to decode (`400 INVALID_ARGUMENT`), a
  decoded payload with no `requestDetails`, or a verdict that misses app
  recognition / device integrity / the package name. Rejected with
  `ATTESTATION_FAILED` (403).
- `unavailable` — verification could not run for a reason **outside** the
  caller's control: no service account configured, or a Google-side error
  (token mint failure, 401/403/429/5xx from `decodeIntegrityToken`, network
  throw). **Proceeds** (fail-open): hard-failing every outage bricked the
  feature for legitimate users whenever Play services hiccuped.

The split matters. Treating a missing token as "verification could not run"
made the endpoint open to anyone who simply omitted it — the request shape is
public — and billed the account's AI credits. A caller can always withhold a
token, so that path can never be a reason to fail open.

**The fail-open path is bounded.** Consecutive Google-side failures are counted
per isolate (`MAX_CONSECUTIVE_FAIL_OPEN`, currently 10, inside a rolling
5-minute window); past the bound the Worker fails closed until Google answers a
decode again, so a sustained "verification is broken" condition can't be ridden
as a standing bypass. Failures older than the window start a fresh run, so
occasional blips never accumulate into a closed gate. The counter is
per-isolate best-effort — the Worker has no KV — and is defence in depth, not a
precise budget.

Abuse cost is further bounded by the AI Gateway: its **Rate Limiting rule**
throttles request volume (surfaced to the app as `RATE_LIMITED`) and its
**Spend Limit** is the hard monthly cost backstop. The Worker keeps no
per-device counters and has no KV.

Replay protection (binding a token to a per-request nonce/`requestHash`) is
**not** implemented — a captured token can be reused until it expires.

## API contract

`POST /v1/invoke`

```json
{
  "capability": "search-assist",
  "integrityToken": "<play-integrity-token; empty/short is rejected 403>",
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
  "hadithRefs": ["bukhari:1469", "muslim:2999"],
  "terms": ["patience", "sabr", "hardship", "perseverance"],
  "confidence": "high"
}
```

- `quranRefs`: up to 8 `surah:ayah` references (surah 1–114 enforced; the app
  drops anything that doesn't resolve in its local Quran database).
- `hadithRefs`: up to 6 `collection:number` references limited to the six
  collections the app ships (`bukhari`, `muslim`, `abudawud`, `tirmidhi`,
  `nasai`, `ibnmajah`); the app drops anything that doesn't resolve in its
  local Hadith database.
- `terms`: 4–10 short keywords the app runs through its local library search.

Errors use a typed envelope with the matching HTTP status:

```json
{ "error": { "code": "RATE_LIMITED", "message": "...", "retryAfterSeconds": 3600 } }
```

| code                 | status | when                                                     |
| -------------------- | ------ | -------------------------------------------------------- |
| `INVALID_INPUT`      | 400    | bad envelope / schema / unknown capability               |
| `ATTESTATION_FAILED` | 403    | no usable Play Integrity token, or the verdict failed    |
| `RATE_LIMITED`       | 429    | the gateway's Rate Limiting rule tripped (pass-through)  |
| `UPSTREAM_ERROR`     | 502    | Claude call failed / returned bad shape                  |
| `BUDGET_EXCEEDED`    | 503    | gateway spend limit tripped / AI credits exhausted       |

## Configuration

Non-secret vars live in `wrangler.jsonc`:

| var                | default | meaning                                             |
| ------------------ | ------- | --------------------------------------------------- |
| `SKIP_ATTESTATION` | `false` | `true` bypasses Play Integrity (local testing ONLY) |

Throttling and the monthly USD ceiling are **not** vars — set a **Rate
Limiting rule** and a **Spend limit** on the `nimaz` AI Gateway in the
dashboard (AI → AI Gateway → nimaz → Settings).

Secrets — set with `wrangler secret put`, never committed:

- `CLOUDFLARE_AI_TOKEN` — the `nimaz` gateway's authentication token (created
  from the gateway's **Authenticated Gateway** settings), sent as
  `cf-aig-authorization`. Unified Billing injects the Anthropic credentials.
  There is **no Anthropic key** anywhere. In CI it lives as the GitHub secret
  of the same name and is pushed to the Worker on every deploy.
- `GOOGLE_SERVICE_ACCOUNT_JSON` — the full service-account JSON (one string)
  used to mint an OAuth token for the Play Integrity API. Technically optional
  — without it verification is "unavailable" and **every request passes,
  unbounded**, since there is nothing to verify against. That is a pre-setup /
  dev state only: a deployed Worker without this secret is an open endpoint on
  the account's AI credits. Set it in production.

## Setup

```bash
cd worker
npm ci                      # or: npm install (first time, to create the lockfile)

# 1. Set the secrets (production):
npx wrangler secret put CLOUDFLARE_AI_TOKEN        # gateway auth token
npx wrangler secret put GOOGLE_SERVICE_ACCOUNT_JSON

# 2. Cloudflare dashboard (one-time, cannot be done from wrangler):
#    AI → AI Gateway → confirm the "nimaz" gateway exists.
#    AI → AI Gateway → "Credits Available" → Manage → add a payment method,
#    purchase credits, and set auto top-up (Unified Billing).
#    Gateway → Settings → Authenticated Gateway: enable it and create the
#    gateway authentication token — that's the CLOUDFLARE_AI_TOKEN secret.
#    Gateway → Settings → Rate Limiting: set the request throttle (this is
#    the ONLY rate limit — the Worker keeps no counters of its own).
#    Recommended: set a Spend limit on the "nimaz" gateway as the monthly
#    cost backstop, and enable the gateway's ZDR (Zero-Data-Retention
#    provider endpoints) setting.
```

## Local development

```bash
# Attestation bypassed so you can curl it. The gateway token comes from a
# .dev.vars file (gitignored) containing CLOUDFLARE_AI_TOKEN=... — there is
# no Anthropic key to pass.
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

Expected: a JSON body with `answer`, `quranRefs` (e.g. `["2:153"]`),
`hadithRefs` (e.g. `["bukhari:1469"]`), `terms`, and `confidence`, plus an
`x-nimaz-usage` response header with the token usage. A valid body proves the forced `submit_result` tool survived the
gateway. Note on caching: `cache_read_input_tokens` in the header is expected
to be 0 for now — Haiku 4.5 only caches prefixes ≥ 4096 tokens and this
capability's prompt + tool schema is well below that, so `cache_control` is
currently inert (it engages automatically if the prompt grows). CI runs this
same check after every deploy (`smoke-test` job in `worker_deploy.yml`).

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
