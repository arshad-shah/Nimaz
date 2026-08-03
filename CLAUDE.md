# Nimaz — agent guide

Nimaz is an offline-first Android Islamic companion app: **Kotlin + Jetpack Compose**,
**Clean Architecture** (`presentation → domain → data`) with **MVVM + UDF**, Hilt DI, Room,
DataStore, type-safe Navigation Compose.

## Read this first

The `docs/` folder is the source of truth — read the relevant doc before working, and **keep it
updated as part of your change** (see "Documentation is part of the work" below):

- **[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)** — layer patterns, DI, navigation, theming,
  diagrams, the new-feature recipe, and the tech-debt registry (§9). The architectural source of truth.
- **[`docs/NAVIGATION.md`](docs/NAVIGATION.md)** — the complete route graph + route table.
- **[`docs/SUBSYSTEMS.md`](docs/SUBSYSTEMS.md)** — audio, widgets, background work, notifications,
  database/migrations, preferences, content seeding, prayer-time calc, sync, init/monitoring.
- **[`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`](docs/CLEAN_ARCHITECTURE_CHECKLIST.md)** — tick-box
  anti-pattern backlog with detection commands.
- **[`docs/ai-ask-with-proof.md`](docs/ai-ask-with-proof.md)** — the opt-in "Ask with Proof" AI
  search feature: the Cloudflare Worker (`worker/`), the `search-assist` capability contract,
  local proof resolution, the smart local search, settings/consent, cost model, and the manual
  setup runbook.

When adding a feature, copy an existing one that follows the patterns — good references:
`AsmaUlHusna`, `Prophet`, `Khatam`, `Quran`.

> **AI / Worker map:** the "Ask with Proof" feature adds a Cloudflare Worker in **`worker/`**
> (capability-registry backend with a single `search-assist` capability, deployed by
> `.github/workflows/worker_deploy.yml`) and an Android slice under `data/ai/`,
> `domain/model/{AiModels,CitationId,LibrarySearch}`, `domain/repository/AiRepository`,
> `domain/usecase/ai/AskWithProofUseCase`, `domain/usecase/SearchLibraryUseCase` (smart local
> search, also the non-AI path), `core/di/AiModule`, the `Ask*`/`SearchSettings*`
> ViewModels/screens, and `Route.SearchSettings`. One Worker call per question (only the question
> text leaves the device); cited Quran and Hadith refs are resolved locally into proof cards. It
> is **off by default**. See `docs/ai-ask-with-proof.md`.

> Naming: the app is **Nimaz** and the package is **`com.arshadshah.nimaz`**. The older
> `docs/nimaz-pro-*.md` files are historical design/planning artifacts (they say "Nimaz Pro" /
> `com.nimazpro.app`) — treat them as background, not current truth.

## Documentation is part of the work

Docs only stay useful if they track reality. **In every change, update the docs your change
touches** — this is not optional:

- Add/remove/rename a `Route` → update **`docs/NAVIGATION.md`** (route table *and*, if the
  high-level map changes, the mermaid diagram; validate it).
- Change a subsystem (audio, widgets, workers, notifications, DB schema/migrations, DataStore,
  seeders, prayer-time calc, sync, init/monitoring) → update the relevant section of
  **`docs/SUBSYSTEMS.md`**.
- Change a layer pattern, DI convention, or resolve/introduce a deviation → update
  **`docs/ARCHITECTURE.md`** (and its §9 registry).
- Fix or discover a clean-architecture anti-pattern → tick / add to
  **`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`**.
- Change the DB schema → update **`docs/SUBSYSTEMS.md`**. Shipped **content** is no longer
  in this repo: it lives in **arshad-shah/nimaz-data** and arrives as a fetched artifact.
  The per-feature content seeders that used to carry it were all retired at versionCode 385, and
  `ContentPatchSeeder` — their generic replacement — was retired in turn at schemaVersion 24.
  A release now reaches existing installs by **replacing the content database**
  (`ContentArtifactInstaller`), which is possible because that database stopped holding user data
  at schemaVersion 23. The ledger is **`docs/retirement.yaml`** (one entry, `git-history-purge`,
  is still open and needs explicit human sign-off — see `docs/DATA_RETIREMENT.md`).

## Non-negotiable rules

1. Dependencies point inward: **domain never imports `data`** (no Room entity/DAO/DataStore);
   presentation never imports entities/DAOs.
2. ViewModels inject **`XxxUseCases`**, not repositories or DAOs.
3. ViewModels expose `StateFlow<XxxUiState>` (immutable `data class`) + a single
   `onEvent(event: XxxEvent)` (sealed interface). No exposed `MutableStateFlow`/`LiveData`.
4. Repositories return **domain models**; map at the data layer (`Entity.toDomain()` /
   `Model.toEntity()`).
5. DI lives in `core/di`: `@Binds` for interface→impl, `@Provides` for `XxxUseCases`,
   `@Singleton` in `SingletonComponent`.
6. Navigation is type-safe: add a `@Serializable` `Route` + `composable<Route.X>` in
   `NavGraph`. (Not every `Route` is a screen — some features are tabs inside a parent
   screen; validate before wiring.)
7. No hardcoded `Color(0xFF…)` in screens — use `MaterialTheme.colorScheme.*` / `NimazColors.*`
   and reuse `presentation/components` (atoms/molecules/organisms).
8. **Interactive UI comes from the design system — never hand-rolled.** A button is `NimazButton`
   (icon-only: `NimazIconButton`), **not** a `Text`/`Box`/`Surface` + `Modifier.clickable`. A
   whole-card tap target is `NimazCard(onClick = …)` (or `NimazMenuItem` for list rows), **not** a
   `Modifier.clickable` wrapped around the card — a wrapping `.clickable` paints a **sharp-cornered
   ripple** that ignores the card radius. `.clickable` on *inner* elements (a `Text`, an icon, a
   sub-row) is fine. For `EventCard`/`WorshipEventCard`, pass `onClick`/`onClickLabel`. See
   `docs/ARCHITECTURE.md` §8 (the `NimazButton`/`NimazCard` bullets).

## Verify before finishing

```bash
./gradlew :app:compileDebugKotlin     # runs KSP → validates Hilt + Room wiring
./gradlew :app:testDebugUnitTest
```

Requires JDK 21 + Android SDK (compileSdk 36); set `sdk.dir` in `local.properties` or
`ANDROID_HOME`. Develop on a feature branch; do not push to `dev` without explicit approval.

## Known deviations & cleanup backlog

Resolved vs open deviations are tracked in **§9 of `docs/ARCHITECTURE.md`**. A tick-box backlog
of clean-architecture anti-patterns to chip away at (with detection commands) lives in
**[`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`](docs/CLEAN_ARCHITECTURE_CHECKLIST.md)** — do not copy
open items; fix them and tick the box.
</content>
