# Nimaz — agent guide

Nimaz is an offline-first Android Islamic companion app: **Kotlin + Jetpack Compose**,
**Clean Architecture** (`presentation → domain → data`) with **MVVM + UDF**, Hilt DI, Room,
DataStore, type-safe Navigation Compose.

**Mid-migration to Gradle modules (#551).** `domain/` now lives in **`:core:domain`**, a pure JVM
module under `core/domain/src/{main,test,testFixtures}/kotlin/` — no Android SDK on its classpath,
so `import android.*` there is a compile error and `androidFreeClasspath` (wired into `check`)
fails on any `androidx` artifact someone adds later. Everything else is still `:app` and moves out
over the remaining PRs. A move does **not** change package names, so imports read the same either
side of a module boundary. Two consequences worth knowing before you edit:

- **Kotlin will not smart-cast a `val` from another module.** `if (ayah.translation != null)
  Text(ayah.translation)` no longer compiles across the boundary — bind a local first.
- **A fake used on both sides goes in `core/domain/src/testFixtures/`**, not copied into each.

## Read this first

The `docs/` folder is the source of truth. **[`docs/README.md`](docs/README.md) is the index** —
it says which doc owns which area. **[`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md) is the
contract** — it says what you must update, and it is enforced on every PR.

Read the doc that owns the area before you change it, and update it **in the same commit**:

- **[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)** — layer patterns, DI, navigation, theming,
  diagrams, the new-feature recipe, and the tech-debt registry (§9). The architectural source of truth.
- **[`docs/NAVIGATION.md`](docs/NAVIGATION.md)** — the route graph, the full route reference, the
  announcement route grammar, and the help deep-link grammar.
- **[`docs/SUBSYSTEMS.md`](docs/SUBSYSTEMS.md)** — audio, widgets, background work, notifications,
  database/migrations, preferences, content delivery, prayer-time calc, sync, init/monitoring, and
  FCM announcements. §0 is the inventory of every Service, Worker, widget, DataStore file and
  notification channel the app ships.
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

> Naming: the app is **Nimaz** and the package is **`com.arshadshah.nimaz`**. The
> `docs/archive/nimaz-pro-*.md` files are historical design/planning artifacts (they say
> "Nimaz Pro" / `com.nimazpro.app`) — background only, never current truth. The same goes for
> `docs/superpowers/{plans,specs}/`: dated per-change records, written once and never updated.

## Documentation is part of the work

**A change is not finished until the doc that owns the area is updated in the same commit.**
Not a follow-up, not an issue — the same commit. The full ownership matrix, house style, diagram
standard and check list live in **[`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md)**; read it
once, then use the summary below.

**23 of these obligations are enforced mechanically.** Run this before you finish — it is
seconds, needs no Android toolchain, and also runs on every PR
(`.github/workflows/docs_check.yml`):

```bash
python3 scripts/check_docs.py           # all checks; --only NAV|SUB|DOC to narrow
```

It fails on: an undocumented `Route` *and* a documented route that no longer exists; a stale
destination count; a destination wired without a `ScreenTags` tag; an undocumented announcement
or help deep-link key (both directions); an undocumented Service, Worker, widget, notification
channel or DataStore file; a schema version that disagrees with `NIMAZ_DATABASE_VERSION`; an
undocumented FCM payload key, announcement type or celebration event; a doc missing its header
block, its index entry or its contents list; and any broken link or anchor between docs.

Diagrams are checked separately (needs Node, so it is its own step):
`npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs`.

The obligations, in short:

- Add/remove/rename a `Route`, or wire a destination → **`docs/NAVIGATION.md`** §3 (and the §2
  mermaid map if the high-level shape changed; validate it). Wire it with
  `taggedComposable<Route.X>(ScreenTags.X)`, never a bare `composable`.
- Add/change an announcement route key or a help deep-link key → **`docs/NAVIGATION.md`** §4/§5.
- Add/rename a Service, Worker, widget, notification channel or DataStore file → the
  **`docs/SUBSYSTEMS.md`** §0 inventory **and** the owning section.
- Change a subsystem's behaviour (audio, widgets, workers, notifications, DB schema/migrations,
  DataStore, content delivery, prayer-time calc, sync, init/monitoring, FCM) → the relevant
  section of **`docs/SUBSYSTEMS.md`**.
- Change a layer pattern, DI convention, or resolve/introduce a deviation → update
  **`docs/ARCHITECTURE.md`** (and its §9 registry).
- Fix or discover a clean-architecture anti-pattern → tick / add to
  **`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`**.
- Add a doc → give it the standard header block, add it to **`docs/README.md`** and to the
  ownership matrix in **`docs/DOCUMENTATION.md`** §1.
- Change the DB schema → update **`docs/SUBSYSTEMS.md`** (§5, including the schema-version
  line). Shipped **content** is no longer
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
6. Navigation is type-safe: add a `@Serializable` `Route`, a `ScreenTags` entry, and a
   `taggedComposable<Route.X>(ScreenTags.X)` in `NavGraph` — never a bare `composable`, which
   leaves the screen untestable. (Not every `Route` is a screen — some features are tabs inside
   a parent screen; validate before wiring.)
7. No hardcoded `Color(0xFF…)` in screens — use `MaterialTheme.colorScheme.*` / `NimazColors.*`
   and reuse `presentation/components` (atoms/molecules/organisms).
8. **Interactive UI comes from the design system — never hand-rolled.** A button is `NimazButton`
   (icon-only: `NimazIconButton`), **not** a `Text`/`Box`/`Surface` + `Modifier.clickable`. A
   whole-card tap target is `NimazCard(onClick = …)` (or `NimazMenuItem` for list rows), **not** a
   `Modifier.clickable` wrapped around the card — a wrapping `.clickable` paints a **sharp-cornered
   ripple** that ignores the card radius. `.clickable` on *inner* elements (a `Text`, an icon, a
   sub-row) is fine. For `EventCard`/`WorshipEventCard`, pass `onClick`/`onClickLabel`. Rows in a
   `NimazMenuGroup` are separated with **`NimazMenuDivider()`** (`inset = false` where there is no
   icon column), never a hand-measured `NimazDivider`; every arrow/chevron comes from
   **`NimazIcons`** (`Forward` for "this row opens something"), never a per-call-site
   `ArrowForward`/`ChevronRight`/`KeyboardArrowRight`. See `docs/ARCHITECTURE.md` §8 (the
   `NimazButton`/`NimazCard`/`NimazMenuDivider`/`NimazIcons` bullets).

## Verify before finishing

```bash
./gradlew :app:compileDebugKotlin     # runs KSP → validates Hilt + Room wiring
./gradlew :app:testDebugUnitTest
./gradlew :core:domain:check          # domain tests + androidFreeClasspath — seconds, no Android
./gradlew :app:lintDebug              # SLOW (~10 min) and CI-blocking — do not skip it
python3 scripts/check_docs.py         # docs still describe the code (no toolchain needed)

# Only when a Route, a ScreenTags entry or a screen's signature changed:
./gradlew :app:assembleDebugAndroidTest
```

**None of the first four compile `androidTest`.** `FeatureNavigationTest` names `ScreenTags`
constants directly, so removing or renaming one leaves the instrumented source set broken while
all four gates stay green — a branch that goes out clean locally and red on the emulator. If you
touched navigation, build `androidTest` too.

**`lintDebug` is a real gate, not an optional extra.** `fastlane/Fastfile`'s `test` lane runs
`:app:testDebugUnitTest` *and* `:app:lintDebug` — the same two tasks listed above — so every PR
check fails on a lint **error**, and lint catches a class of defect the other three cannot:
`LocalContextGetResourceValueCall` (a `context.getString`
inside a composable, which does not re-resolve across a configuration change) and
`MissingTranslation` (a new string absent from a shipped locale). Neither breaks a build or a test.
It is slow, and running it is still cheaper than a red `dev`.

Requires JDK 21 + Android SDK (compileSdk 36); set `sdk.dir` in `local.properties` or
`ANDROID_HOME`. Develop on a feature branch; do not push to `dev` without explicit approval.

## Known deviations & cleanup backlog

Resolved vs open deviations are tracked in **§9 of `docs/ARCHITECTURE.md`**. A tick-box backlog
of clean-architecture anti-patterns to chip away at (with detection commands) lives in
**[`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`](docs/CLEAN_ARCHITECTURE_CHECKLIST.md)** — do not copy
open items; fix them and tick the box.
</content>
