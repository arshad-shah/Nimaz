# Ask with Proof — AI-assisted search with local proof

An **opt-in**, privacy-disclosed AI layer over Global Search. The user types
into Global Search's **single search bar** — the same text drives both keyword
search and, on submit, the AI ask (there is no separate "ask" field).

When AI is enabled, each submit makes **one** Worker call (`search-assist`):

1. **Ask** — the app sends ONLY the question text. Claude answers from
   mainstream Islamic knowledge and returns (a) the **Quran references** that
   support the answer and (b) related **search terms** for the local library.
2. **Prove (local)** — the app resolves each returned reference against the
   LOCAL Quran database. Every reference that resolves becomes a tappable
   "proof" card showing the **real verse text** with a deep link into the
   reader; anything that doesn't resolve is dropped silently, so a proof card
   can never show a verse that doesn't exist.
3. **Enhance (local)** — the AI's terms run through the smart local search
   (`SearchLibraryUseCase`) and replace the results list, so the list
   dynamically shows the Quran/Hadith/Dua records the AI judged relevant.

There is **no "no supporting sources found" dead end**: the answer stands on
its own, proof cards show whatever resolved, and the related results are always
explorable. When AI is **off**, only local keyword search runs (no network, no
behaviour change). AI is **off by default** — a user who never opens Search
Settings sees nothing leave their device.

## Architecture

```mermaid
sequenceDiagram
    participant U as User
    participant App as Nimaz (AskViewModel)
    participant Room as Room (Quran/Hadith/Dua)
    participant W as Worker (nimaz-ai)
    participant GW as Cloudflare AI Gateway
    participant C as Claude (Haiku 4.5)

    U->>App: Ask a question
    App->>W: POST /v1/invoke (search-assist) {question}
    W->>W: integrity → tiered rate limit
    W->>GW: anthropic/v1/messages (cf-aig-authorization, forced submit_result tool)
    GW->>C: Unified Billing — Cloudflare-managed Anthropic credentials
    C-->>GW: tool_use JSON
    GW-->>W: native response + usage
    W->>W: validate output, drop malformed refs, log usage
    W-->>App: {answer, quranRefs, terms, confidence}
    App->>Room: resolve quranRefs → real verses (proof cards)
    App->>Room: run terms through SearchLibraryUseCase (results list)
    App-->>U: answer + confidence + proof cards + AI-driven results list
```

One Worker call per submit; everything else is local. On the `W → GW → Claude`
leg the Worker calls the `nimaz` AI Gateway's Anthropic provider-native
endpoint with **Unified Billing**: no provider key is attached, so Cloudflare
injects its managed Anthropic credentials and bills the account's AI credits.
The Worker's only credential is `CLOUDFLARE_AI_TOKEN` — the gateway's
authentication token (`cf-aig-authorization`) — never an Anthropic key.

### Why attestation never blocks

Play Integrity used to hard-fail requests (ATTESTATION_FAILED) whenever the
token couldn't be fetched or Google's API hiccuped — bricking the feature for
legitimate users. The Worker now **classifies** each request into a trust tier
instead of gating on it:

| Tier         | When                                                                | Per-device daily cap              |
| ------------ | ------------------------------------------------------------------- | --------------------------------- |
| `verified`   | Token decodes with `PLAY_RECOGNIZED` + device/basic integrity        | `DAILY_DEVICE_LIMIT` (20)         |
| `unverified` | Missing token, Integrity API unreachable/unconfigured, failed verdict | `UNVERIFIED_DAILY_DEVICE_LIMIT` (5) |

Every failure path degrades to `unverified` — a smaller cap, never an error.
The AI Gateway spend limit remains the hard cost backstop against abuse.

Layers (Android):

```
presentation/screens/search/SearchScreen.kt         shared search+ask input bar; bridges AI terms → list
presentation/screens/search/AskComponents.kt        AI answer card / proof rows / loading / error+retry
presentation/screens/settings/SearchSettingsScreen  consent + toggles + privacy
presentation/viewmodel/AskViewModel                 ask state machine (exposes relatedTerms)
presentation/viewmodel/SearchViewModel              results list (+ ApplyAiTerms event)
presentation/viewmodel/SearchSettingsViewModel      settings + consent state
domain/usecase/ai/AskWithProofUseCase               ask → resolve refs → proofs (+ related terms)
domain/usecase/SearchLibraryUseCase                 smart multi-word local search (also non-AI path)
domain/model/{AiModels,CitationId,LibrarySearch}    SearchAssist/Proof/AiError/LibrarySearchResults
domain/repository/AiRepository                      gateway interface (assist)
data/ai/{AiApiClient,IntegrityTokenProvider,DeviceIdProvider}
data/ai/dto/AiDtos                                  wire DTOs (mirror the Worker)
data/repository/AiRepositoryImpl                    error-envelope → AiError mapping
core/di/AiModule                                    Ktor client + wiring
worker/src/capabilities/search-assist               the single AI capability
```

## Capability contract

Everything goes through the same `POST /v1/invoke` envelope and middleware
(integrity tier → rate limit). The Android `input` is sent as a raw
JSON object so one envelope serves every capability.

### `search-assist` (the single call)

```json
{ "capability": "search-assist", "integrityToken": "<token or empty>", "deviceId": "…",
  "input": { "question": "3..500 chars" } }
```

Response (validated against a Zod schema before returning):

```json
{
  "answer": "≤120 words, descriptive, never a fatwa",
  "quranRefs": ["2:153", "39:10"],
  "terms": ["patience", "sabr", "hardship"],
  "confidence": "high|medium|low"
}
```

`quranRefs` are `surah:ayah` (standard mushaf numbering); the Worker drops
malformed/impossible refs (surah 1–114) and the app only surfaces refs that
resolve against its local database. Hadith/Dua use opaque local IDs the model
can't know, so they are reached only through `terms` in the results list.

Error envelope (HTTP 400/429/502/503):

```json
{ "error": { "code": "RATE_LIMITED|BUDGET_EXCEEDED|INVALID_INPUT|UPSTREAM_ERROR", "message": "…", "retryAfterSeconds": 3600 } }
```

(There is no ATTESTATION_FAILED anymore — see the tier table above.)

### Citation ID grammar (`domain/model/CitationId.kt`)

Round-trips cleanly so a citation resolves back to a local record and a deep link:

| source | id form                | resolves to                    | Route                       |
| ------ | ---------------------- | ------------------------------ | --------------------------- |
| quran  | `quran:{surah}:{ayah}` | `getAyahsBySurah` → `ayahNumber` | `Route.QuranReader(surah, ayah)` |
| hadith | `hadith:{hadithId}`    | `getHadithById`                | `Route.HadithReader(hadithId)`   |
| dua    | `dua:{duaId}`          | `getDuaById`                   | `Route.DuaReader(duaId)`         |

Unparseable or unresolvable IDs are dropped silently. (The current AI flow
only produces `quran:` citations; the grammar keeps hadith/dua for future use.)

## Smart local search (`SearchLibraryUseCase`)

The non-AI search got better too. The DAO queries match one contiguous
substring (`LIKE '%q%'`), so multi-word queries used to return nothing. The
use case now:

- always searches the whole phrase (exact hits score highest),
- additionally tokenizes multi-word queries into significant words (stopwords
  dropped) and searches each word, ranking records by how many words they
  match,
- exposes `byTerms(terms)` — the same union+rank over the AI's related terms,
- dedupes per source, caps results, and keeps the UI responsive.

`SearchViewModel` uses it for search-as-you-type, submits, and `ApplyAiTerms`.

## Settings & consent behaviour

Search Settings (`Route.SearchSettings`, reachable from the Global Search top-bar
action and the Settings hub):

- **AI answers** — master toggle. Turning it **on** first opens a consent
  `ModalBottomSheet` stating exactly what is shared (**only the question text**
  → Nimaz server on Cloudflare → Anthropic Claude), that the cited verses and
  results are looked up locally, that questions are never used for analytics,
  that answers can be wrong and must be verified against the cited sources,
  and that it is not a fatwa. "Enable" sets `aiAskEnabled=true` +
  `aiConsentTimestamp`. Turning it **off** is instant, no sheet.
- **Privacy** — an expandable "What gets shared" repeating the disclosure; an
  "AI question history" toggle (off = recent questions kept in memory only;
  on = persisted to DataStore as a JSON list); and "Clear AI history".

DataStore keys (in `PreferencesDataStore`, declared on `SettingsRepository`):
`aiAskEnabled` (false), `aiConsentTimestamp` (0), `aiHistoryEnabled` (false),
`aiAskHintDismissed` (false), `aiQuestionHistory` (JSON list). (The old
per-source toggles and proofs-count slider were removed with the single-call
rebuild — sources are no longer uploaded, so there is nothing to configure.)

### Privacy / analytics

Only the question text ever leaves the device. It is **never** sent to
Firebase. `AskViewModel` logs only event names via `AppAnalytics.logEvent`:
`ai_ask_submitted`, `ai_ask_answered`, `ai_ask_error_{slug}` — no content
payloads. The Worker stores nothing.

## Cost model

- **Billing: Cloudflare AI Gateway Unified Billing.** The Worker calls
  `claude-haiku-4-5` through the `nimaz` gateway's Anthropic provider-native
  endpoint (auth: the `CLOUDFLARE_AI_TOKEN` gateway authentication token);
  Cloudflare holds the Anthropic credentials and draws spend from the
  account's **AI credits** — one Cloudflare invoice, no Anthropic account/key. Provider
  per-token rates pass through with **no markup**; the one cost on top is a
  **5% fee on credit purchases** (a $100 top-up costs $105). Auto top-up keeps
  answers from stalling when credits run low.
- Model: `claude-haiku-4-5` (the provider-native endpoint takes the plain
  Anthropic model name). Pricing: **$1 / MTok input**, **$5 / MTok output**;
  cached input reads billed at **10%** of the input rate.
- Each submit is **one** call: `search-assist` (`max_tokens` 700,
  temperature 0.2). The gateway's provider-native endpoint uses the
  Anthropic-native schema, so the forced `submit_result` tool and the
  `cache_control` marker both survive the gateway. Caching caveat: Haiku 4.5 only caches prompt prefixes ≥ **4096
  tokens**, and this capability's system prompt + tool schema is well below
  that — so the marker is currently inert (no cache entry, full input price,
  ~$0.002/question either way). It engages automatically if the prompt grows.
  One question consumes one invocation of the per-device daily cap (the old
  design burned two).
- Guards: tiered per-device daily cap (`DAILY_DEVICE_LIMIT` 20 verified /
  `UNVERIFIED_DAILY_DEVICE_LIMIT` 5) and global daily cap
  (`DAILY_GLOBAL_LIMIT`, 500), both KV-backed in the Worker. The monthly USD
  ceiling is the **AI Gateway Spend Limit** (dashboard setting on the `nimaz`
  gateway) — when it trips (or credits run out) the Worker maps the gateway
  error to `BUDGET_EXCEEDED` (503), same app UX as the old KV budget tally it
  replaces.
- Observability: every call attaches a `cf-aig-metadata: {"capability": …}`
  header (spend per feature in the dashboard; never the question text), logs
  an `ai_usage` line (token counts only), and echoes usage in the
  `x-nimaz-usage` response header.

## Adding a new capability

**Worker** (`worker/`):
1. Add Zod input/output schemas in `src/schemas/<id>.ts`.
2. Create `src/capabilities/<id>.ts` exporting a `Capability<I, O>` (build the
   request, force a structured-output tool, parse/validate the response).
3. Register it in `src/registry.ts`. It is immediately reachable at
   `POST /v1/invoke` with `"capability": "<id>"`. Nothing in the middleware chain
   changes. Add tests in `worker/test/`.

**Android**: add DTOs mirroring the new input/output, a `domain/usecase/…`
orchestrator, and wire it in `core/di/AiModule.kt` (reuse `AiApiClient`).

## Runbook — manual setup (one-time)

These are required to make the feature actually work end-to-end. Nothing here is
committed to the repo.

### 1. Cloudflare (Worker + Unified Billing)

1. `cd worker && npm ci`
2. The KV namespace id is already committed in `worker/wrangler.jsonc` (a
   resource id, not a secret). To recreate it in a different account:
   `npx wrangler kv namespace create NIMAZ_AI_KV` and paste the returned id.
3. Set the secrets:
   `CLOUDFLARE_AI_TOKEN` — the `nimaz` gateway's authentication token
   (gateway → Settings → **Authenticated Gateway** → create token). Store it
   as the GitHub Actions secret of the same name; CI pushes it into the
   Worker on every deploy (manual alternative:
   `npx wrangler secret put CLOUDFLARE_AI_TOKEN`). This is the Worker's only
   Claude credential.
   `npx wrangler secret put GOOGLE_SERVICE_ACCOUNT_JSON`
   (If the Google one is absent the Worker still works — every request simply
   runs at the smaller unverified rate-limit tier.) There is **no
   `ANTHROPIC_API_KEY`**: Unified Billing injects the Anthropic credentials.
4. **Unified Billing (dashboard, cannot be scripted):** AI → AI Gateway →
   confirm the `nimaz` gateway exists → *Credits Available* → **Manage** →
   add a payment method, **purchase credits**, and set **auto top-up**
   (threshold + recharge amount) so answers don't stall when credits run low.
   Until credits exist, every ask returns `BUDGET_EXCEEDED`/`UPSTREAM_ERROR`.
5. **Spend limit (recommended):** on the `nimaz` gateway settings, set a
   monthly USD Spend Limit — this replaced the Worker's old KV
   `MONTHLY_BUDGET_USD` tally as the hard cost backstop.
6. **ZDR (recommended):** enable the gateway's Zero-Data-Retention setting so
   Unified-Billing requests route through ZDR provider endpoints — it matches
   the app's "only the question is sent" promise. (Separate from the
   gateway's own logging toggle, which you can also turn off.)
7. Deploy: push to `dev` (CI) or `npx wrangler deploy`. The Worker is served at
   the custom domain **`https://ai.arshadshah.com`** (configured via the
   `routes` block in `wrangler.jsonc`; `wrangler deploy` provisions the domain +
   certificate, provided the `arshadshah.com` zone is on the same account). It is
   also reachable at its `*.workers.dev` URL.
8. Keep `SKIP_ATTESTATION=false` in production. Use `--var SKIP_ATTESTATION:true`
   only for local `wrangler dev` testing (it forces the verified tier).
9. Smoke test (post-deploy): CI runs one automatically (`smoke-test` job in
   `worker_deploy.yml`) — it asks the same question twice and asserts a valid
   `{answer, quranRefs, terms, confidence}` body (forced `submit_result` tool
   survives the gateway) and prints the `x-nimaz-usage` headers.
   `cache_read_input_tokens` is expected to be 0 (prompt below Haiku 4.5's
   4096-token cacheable minimum). Manually, confirm in the AI Gateway
   dashboard that the request is logged with a cost and credits were deducted.
10. One-time cleanup once the switch is verified in production:
    `npx wrangler secret delete ANTHROPIC_API_KEY` — the old direct-Anthropic
    secret is no longer read by any code.

### 2. Google Cloud / Play Console (Play Integrity)

1. In the Play Console, link/create a Google Cloud project and note its **project
   number**.
2. Enable the **Play Integrity API** in that Cloud project.
3. Create a **service account** with access to the Play Integrity API; download
   its JSON key — this is `GOOGLE_SERVICE_ACCOUNT_JSON` (step 1.3). Never commit it.
4. Put the project number into `gradle.properties` as
   `playIntegrityCloudProjectNumber` (or pass `-PplayIntegrityCloudProjectNumber=…`).

Note: even with none of this configured, AI answers work — devices just get the
unverified daily cap.

### 3. GitHub secrets (for `worker_deploy.yml`)

- `CLOUDFLARE_API_TOKEN` — token with Workers + KV edit permission (deploys).
- `CLOUDFLARE_ACCOUNT_ID` — the Cloudflare account id.
- `CLOUDFLARE_AI_TOKEN` — the gateway's authentication token. CI pushes it as
  a Worker secret on every deploy (`secrets:` input of wrangler-action), so it
  never needs a manual `wrangler secret put`.

(The Android/deploy secrets — `FIREBASE_CONFIG`, `PLAY_STORE_CONFIG_JSON`,
`KEYSTORE_*`, `BUMP_APP_*` — are unchanged.)

### 4. gradle.properties (per environment)

```properties
nimazAiWorkerUrlDebug=https://ai.arshadshah.com
nimazAiWorkerUrl=https://ai.arshadshah.com
playIntegrityCloudProjectNumber=<google-cloud-project-number>
```

These committed defaults point at the production custom domain. Until the Worker
is deployed and the domain resolves, the app still builds and runs — asking
simply shows the network-error state with a retry button (it never crashes).

## Testing

- Worker: `cd worker && npm ci && npm test && npx tsc --noEmit`.
- Android: `./gradlew lint testDebugUnitTest` (unit tests cover the citation
  grammar, ref resolution, repository error mapping, the smart local search,
  and the ViewModel state machines).
- End-to-end smoke test against `wrangler dev` with `SKIP_ATTESTATION=true` — see
  the `curl` example in `worker/README.md`.
