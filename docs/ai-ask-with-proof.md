# Ask with Proof — grounded AI Q&A

An **opt-in**, privacy-disclosed, proof-driven question-answering feature. The
user types into Global Search's **single search bar** — the same text drives both
keyword search and, on submit, the AI ask (there is no separate "ask" field).

When AI is enabled it **drives retrieval end-to-end** (two Worker calls per
submit):

1. **Plan** (`search-plan`) — the app sends only the question; Claude returns the
   search *terms* and specific *Quran refs* it judges relevant. The app decides
   nothing about relevance itself.
2. **Retrieve (local)** — the app runs the plan's terms through the existing
   Quran/Hadith/Dua searches and resolves its Quran refs, all against Room. These
   real records populate **both** the results list (so the AI controls what's
   listed) and the evidence set. Falls back to local keyword variants if planning
   fails or is empty.
3. **Ground** (`ask-with-proof`) — the app sends the question + retrieved passages;
   Claude answers **only from the supplied passages**. Cited passages resolve back
   to real records shown as tappable "proof" cards that deep-link into the readers.

When AI is **off**, only local keyword search runs (no network, no behaviour
change). AI is **off by default** — a user who never opens Search Settings sees
nothing leave their device.

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
    App->>W: POST /v1/invoke (search-plan) {question}
    W-->>App: {terms, quranRefs}  (fallback: local keyword variants on failure)
    App->>Room: run terms + resolve quranRefs (local search use cases)
    Room-->>App: candidate passages + list results
    App->>App: rank by term overlap, cap ≤8, ≤8000 chars
    alt no passages
        App-->>U: "No supporting sources found" (no grounding call)
    else has passages
        App->>W: POST /v1/invoke (ask-with-proof) {question, passages}
        W->>W: integrity → rate limit → budget guard
        W->>GW: Messages API (forced submit_answer tool, cached system prompt)
        GW->>C: proxied request
        C-->>GW: tool_use JSON
        GW-->>W: response + usage
        W->>W: validate output, drop unknown citationIds, record spend
        W-->>App: {answer, citationIds, confidence, insufficientEvidence}
        App->>Room: resolve citationIds → records (round-trip)
        App-->>U: answer + confidence + proof cards + AI-driven results list
    end
```

Each submit makes **two** Worker calls (plan + ground) — see the cost note below.

Layers (Android):

```
presentation/screens/search/SearchScreen.kt         shared search+ask input bar; bridges AI plan → list
presentation/screens/search/AskComponents.kt        UI for the answer/proofs/hint
presentation/screens/settings/SearchSettingsScreen  consent + toggles + privacy
presentation/viewmodel/AskViewModel                 ask state machine (exposes plannedTerms)
presentation/viewmodel/SearchViewModel              results list (+ ApplyAiTerms event)
presentation/viewmodel/SearchSettingsViewModel      settings + consent state
domain/usecase/ai/AskWithProofUseCase               plan → retrieve → ground → resolve
domain/model/{AiModels,CitationId,SearchPlan}       ProofPassage/GroundedAnswer/Proof/AiError/SearchPlan
domain/repository/AiRepository                      gateway interface (planSearch + ask)
data/ai/{AiApiClient,IntegrityTokenProvider,DeviceIdProvider}
data/ai/dto/AiDtos                                  wire DTOs (mirror the Worker)
data/repository/AiRepositoryImpl                    error-envelope → AiError mapping
core/di/AiModule                                    Ktor client + wiring
worker/src/capabilities/{ask-with-proof,search-plan} the two AI capabilities
```

## Capability contract

Both capabilities go through the same `POST /v1/invoke` envelope and middleware
(integrity → rate limit → budget). The Android `input` is sent as a raw JSON
object so one envelope serves every capability.

### `search-plan` (retrieval planning — call 1)

```json
{ "capability": "search-plan", "integrityToken": "…", "deviceId": "…",
  "input": { "question": "3..500 chars" } }
```

Response:

```json
{ "terms": ["patience", "sabr", "hardship"], "quranRefs": ["2:153", "39:10"] }
```

`terms` are keyword/substring search terms; `quranRefs` are `surah:ayah` (real
references only — malformed ones are dropped in `parseResponse`). Hadith/Dua use
opaque local IDs the model can't know, so it plans them only through `terms`.

### `ask-with-proof` (grounded answer — call 2)

`POST /v1/invoke`:

```json
{
  "capability": "ask-with-proof",
  "integrityToken": "<play-integrity-token | debug-skip>",
  "deviceId": "<random per-install UUID>",
  "input": {
    "question": "3..500 chars",
    "passages": [
      { "id": "quran:2:153", "source": "quran|hadith|dua", "text": "≤1200 chars", "meta": "Surah Al-Baqarah 153" }
    ]
  }
}
```

Response (validated against a Zod schema before returning):

```json
{ "answer": "…", "citationIds": ["quran:2:153"], "confidence": "high|medium|low", "insufficientEvidence": false }
```

Error envelope (HTTP 400/401/429/502/503):

```json
{ "error": { "code": "RATE_LIMITED|BUDGET_EXCEEDED|ATTESTATION_FAILED|INVALID_INPUT|UPSTREAM_ERROR", "message": "…", "retryAfterSeconds": 3600 } }
```

### Citation ID grammar (`domain/model/CitationId.kt`)

Round-trips cleanly so a citation resolves back to a local record and a deep link:

| source | id form                | resolves to                    | Route                       |
| ------ | ---------------------- | ------------------------------ | --------------------------- |
| quran  | `quran:{surah}:{ayah}` | `getAyahsBySurah` → `ayahNumber` | `Route.QuranReader(surah, ayah)` |
| hadith | `hadith:{hadithId}`    | `getHadithById`                | `Route.HadithReader(hadithId)`   |
| dua    | `dua:{duaId}`          | `getDuaById`                   | `Route.DuaReader(duaId)`         |

Unparseable or unresolvable IDs are dropped silently.

## Settings & consent behaviour

Search Settings (`Route.SearchSettings`, reachable from the Global Search top-bar
action and the Settings hub):

- **AI answers** — master toggle. Turning it **on** first opens a consent
  `ModalBottomSheet` stating exactly what is shared (question + matched excerpts →
  Nimaz server on Cloudflare → Anthropic Claude), that nothing else leaves the
  device, that questions are never used for analytics, that answers can be wrong
  and must be verified against the cited sources, and that it is not a fatwa.
  "Enable" sets `aiAskEnabled=true` + `aiConsentTimestamp`. Turning it **off** is
  instant, no sheet.
- **Sources** — Quran / Hadith / Dua toggles + a proofs-count slider (3–8).
- **Privacy** — an expandable "What gets shared" repeating the disclosure; an "AI
  question history" toggle (off = recent questions kept in memory only; on =
  persisted to DataStore as a JSON list); and "Clear AI history".

DataStore keys (in `PreferencesDataStore`, declared on `SettingsRepository`):
`aiAskEnabled` (false), `aiConsentTimestamp` (0), `aiSourcesQuran/Hadith/Dua`
(true), `aiMaxProofs` (5), `aiHistoryEnabled` (false), `aiAskHintDismissed`
(false), `aiQuestionHistory` (JSON list).

### Privacy / analytics

The question text is **never** sent to Firebase. `AskViewModel` logs only event
names via `AppAnalytics.logEvent`: `ai_ask_submitted`, `ai_ask_answered`,
`ai_ask_error_{slug}` — no content payloads. The Worker stores nothing.

## Cost model

- Model: `claude-haiku-4-5`. Pricing: **$1 / MTok input**, **$5 / MTok output**;
  cached input reads billed at **10%** of the input rate.
- Each submit is **two** calls: `search-plan` (`max_tokens` 300) then
  `ask-with-proof` (`max_tokens` 600). Both prompts are marked
  `cache_control: ephemeral`, so after the first call each is read from cache at
  ~10% cost. Both count against the rate-limit/budget guards, so **one question
  consumes two invocations** of the per-device daily cap.
- temperature 0.2 for both.
- Guards (KV-backed, per UTC period): per-device daily cap (`DAILY_DEVICE_LIMIT`,
  20), global daily cap (`DAILY_GLOBAL_LIMIT`, 500), monthly USD budget
  (`MONTHLY_BUDGET_USD`, 10). Spend is tallied in integer microdollars after each
  call; when the month meets the cap the Worker returns `BUDGET_EXCEEDED` (503).

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

### 1. Cloudflare (Worker)

1. `cd worker && npm ci`
2. The KV namespace id is already committed in `worker/wrangler.jsonc` (a
   resource id, not a secret). To recreate it in a different account:
   `npx wrangler kv namespace create NIMAZ_AI_KV` and paste the returned id.
3. Set secrets:
   `npx wrangler secret put ANTHROPIC_API_KEY`
   `npx wrangler secret put GOOGLE_SERVICE_ACCOUNT_JSON`
4. (Optional) Create an AI Gateway named `nimaz` and set the var
   `AI_GATEWAY_BASE_URL` to
   `https://gateway.ai.cloudflare.com/v1/<account_id>/nimaz/anthropic`.
5. Deploy: push to `dev` (CI) or `npx wrangler deploy`. The Worker is served at
   the custom domain **`https://ai.arshadshah.com`** (configured via the
   `routes` block in `wrangler.jsonc`; `wrangler deploy` provisions the domain +
   certificate, provided the `arshadshah.com` zone is on the same account). It is
   also reachable at its `*.workers.dev` URL.
6. Keep `SKIP_ATTESTATION=false` in production. Use `--var SKIP_ATTESTATION:true`
   only for local `wrangler dev` testing.

### 2. Google Cloud / Play Console (Play Integrity)

1. In the Play Console, link/create a Google Cloud project and note its **project
   number**.
2. Enable the **Play Integrity API** in that Cloud project.
3. Create a **service account** with access to the Play Integrity API; download
   its JSON key — this is `GOOGLE_SERVICE_ACCOUNT_JSON` (step 1.3). Never commit it.
4. Put the project number into `gradle.properties` as
   `playIntegrityCloudProjectNumber` (or pass `-PplayIntegrityCloudProjectNumber=…`).

### 3. GitHub secrets (for `worker_deploy.yml`)

- `CLOUDFLARE_API_TOKEN` — token with Workers + KV edit permission.
- `CLOUDFLARE_ACCOUNT_ID` — the Cloudflare account id.

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
simply shows the network-error state (it never crashes).

## Testing

- Worker: `cd worker && npm ci && npm test && npx tsc --noEmit`.
- Android: `./gradlew lint testDebugUnitTest` (unit tests cover the citation
  grammar, retrieval/resolution, repository error mapping, and the ViewModel
  state machine).
- End-to-end smoke test against `wrangler dev` with `SKIP_ATTESTATION=true` — see
  the `curl` example in `worker/README.md`.
```
