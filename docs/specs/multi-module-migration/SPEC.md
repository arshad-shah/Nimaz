# Multi-module migration — assessment and spec

**Repo:** `arshad-shah/nimaz` · **Base:** `dev` · **Written against:** `77534cb`
**Scope:** the Gradle module structure only. No behaviour changes, no UI changes, no schema
changes. Every phase below is a refactor whose success criterion is that nothing observable
changed.
**Execution plan:** [`EPIC.md`](EPIC.md) — the issue tree, the 22-PR stack and the
`epic/multi-module` integration branch that turns this assessment into merged work.
**Status:** proposal. Nothing here has been implemented.

---

## 0. How to work from this document

§1–§3 are the assessment: what the code looks like today, what is already in good shape, and
what actually stands in the way. §4 is the target module graph. §5 is the phase order. §6 is
the validation strategy, which is the half of this document that decides whether the migration
succeeds or quietly breaks the release lane.

Phases are meant to ship as separate PRs. Phase 4 is the critical path; do not start Phase 5
without it.

---

## 1. What the code looks like today

Measured on `77534cb`.

| Layer | Files | LOC | Share |
|---|---:|---:|---:|
| `presentation/` | 428 | 97,290 | 51% |
| `data/` | 98 | 18,351 | 10% |
| `core/` | 54 | 11,377 | 6% |
| `domain/` | 103 | 9,693 | 5% |
| `widget/` | 39 | 3,530 | 2% |
| **total `app/src`** | **1,121** | **190,505** | |

Modules: `:app` and `:baselineprofile`. Tests: 2,333 unit `@Test` methods across 360 files,
121 instrumented across 37. No `buildSrc`, no `build-logic`, and no ArchUnit/Konsist-style
architecture test anywhere in the repo.

**Measured build baseline** (cold daemon, warm Gradle build cache, this machine):
`./gradlew :app:compileDebugKotlin` = **3m 44s** for Kotlin compilation alone — no dexing, no
resource merging, no lint. Gradle's own output notes that configuration cache is not enabled.

Within `presentation/`, the design system is the larger half:

| Package | Files | LOC |
|---|---:|---:|
| `components/molecules` | 88 | 21,093 |
| `components/organisms` | 42 | 12,481 |
| `components/atoms` | 52 | 9,293 |
| `theme` + `foundation` + `model` | 37 | 3,779 |
| `screens/` (26 directories) | 107 | ~35,000 |

---

## 2. What is already in good shape

This matters, because each item below is work a typical modularization has to do *first* and
that Nimaz has already done. The migration is unusually well-prepared.

1. **The domain layer is genuinely pure.** Across 103 files there is not one `android`,
   `androidx`, `dagger`, `room` or Firebase import. The only external annotation is
   `javax.inject.Inject` (28 files) — a plain JVM annotation, fine in a `kotlin-jvm` module.
   `:core:domain` can be a non-Android module on day one.
2. **Screens are navigation-decoupled.** 84 of 107 screen files take `onNavigate`/`onBack`
   lambdas; only 7 import `NavController` or `NavHostController`. This is the single biggest
   determinant of feature-extraction difficulty and it is already handled.
3. **Vertical slices are clean at the data layer.** 22 DAOs map to 19 repository
   implementations nearly 1:1. `QuranDao` is the only DAO used by more than one repository
   (four: Quran, Khatam, Tafseer, Library).
4. **Interface segregation on settings is done.** `domain/repository/settings/SettingsSeams.kt`
   already splits the flat preference store into **11** feature-scoped interfaces
   (`QuranPreferences`, `HadithDisplaySettings`, `DuaDisplaySettings`, `TasbihSettings`,
   `ZakatSettings`, `HijriSettings`, `SearchSettings`, `AiSettings`, `LocationSettings`,
   `MoreSettings`, `AppSettings`), and `SettingsRepository` extends all 11 while declaring a
   further 44 vals and 48 functions of its own. Each feature module depends on its own seam
   rather than the whole store. Normally the fiddliest part of a split.

   Note that `docs/ARCHITECTURE.md` §9 still records **nine** seams — `HijriSettings` and
   `SearchSettings` were added without the doc being updated. Correct it in Phase 0; it is a
   small instance of exactly the drift the module boundaries are meant to make impossible.
5. **`android.nonTransitiveRClass=true` is already set**, which multi-module R-class
   resolution requires.
6. **Widgets are isolated** — zero imports from `presentation/`.
7. **Tests mirror the main package tree**, so they move with the code they cover.
8. **The design system barely leaks** — only 3 files across `components/`, `theme/` and
   `foundation/` reference a feature package.

---

## 3. The real blockers

### 3.1 `NavGraph.kt` — 1,442 LOC, the largest file in the repository

It imports **70 screen composables** and registers **94 destinations** via `taggedComposable<Route.X>`.
Every screen in the app is reachable from this one file, which means **no feature can move into
its own module while it exists in this form.** This is the critical path.

### 3.2 `RepositoryModule.kt` — 863 LOC

One Hilt module carrying every `@Binds` interface→impl pair plus an `object UseCaseModule` of
`@Provides` functions at the bottom. `DatabaseModule.kt` similarly provides both databases and
all 22 DAOs one method at a time. Both must dissolve into per-module slices.

### 3.3 Screens and ViewModels are grouped on different axes

`screens/` has 26 directories organised by feature. `viewmodel/` has 16 organised by *domain
area*. The mismatch is the source of essentially all cross-feature coupling:

| Shared ViewModel package | Consumed by |
|---|---|
| `viewmodel/content` | dua, hadith, qaida, asma, asmaunnabi, names, prophets, catalog |
| `viewmodel/tracker` | prayer, fasting, tasbih, dua |
| `viewmodel/settings` | quran, dua, hadith (`SettingsViewModel` + `SettingsEvent`) |

Plus `screens/adaptive`, which depends on twelve features by design — it is the list-detail shell.

**The module boundary must follow the ViewModel grouping, not the screen grouping.** One module
per `screens/` directory would produce 26 modules in a dense mesh: strictly worse than the
monolith. Grouping by ViewModel package yields ~11 modules in a near-tree shape.

### 3.4 Resources — 1,910 strings across 6 locales

A single `strings.xml` with 1,910 entries, referenced from 206 presentation files, translated
into `values-de`, `values-fr`, `values-id`, `values-ms`, `values-tr`. Splitting these per feature
is the highest-toil, lowest-value work in the migration, and `bundle.language.enableSplit = false`
means every locale ships in the base APK regardless. **Recommendation: do not split them.**

### 3.5 Two Room databases

`NimazDatabase` (15 entities, 22 DAOs) and `NimazUserDatabase`. A Room `@Database` class must
see all its entities at compile time, so entities cannot be distributed across feature modules.
One `:core:database` module owns both databases, all entities, all DAOs and all migrations.
It is a shared rebuild hub and that is unavoidable.

### 3.6 `core/` is a grab bag, not a foundation

Nothing in it is "core" in the dependency-order sense:

| Package | Files | LOC | Reaches into |
|---|---:|---:|---|
| `core/util` | 24 | 5,336 | domain (11 files), presentation, data, widget |
| `core/navigation` | 6 | 2,279 | presentation (2), domain (4) |
| `core/di` | 9 | 1,426 | data (6), domain (5) |
| `core/share` | 5 | 1,019 | domain (1) |
| `core/monitoring` | 6 | 960 | — |
| `core/init`, `feedback`, `text`, `time` | 4 | 357 | data (1) |

`core/util` has to be triaged file by file before anything can depend on it.

### 3.7 A domain → navigation inversion

Five domain files import `core.navigation.Route`: `UnifiedBookmark.kt`, `AiModels.kt`,
`Announcement.kt`, `AskWithProofUseCase.kt`, `AnnouncementUseCases.kt`. Domain must not know
about navigation. Hold a domain-level target type and map it to `Route` at the presentation edge.

Three further references are KDoc-only (`QuranReciter.kt`, `MushafLayout.kt`,
`QuranTranslation.kt` point at `data`/`presentation` symbols in comments). Harmless to the
compiler, but they read as real dependencies to the next person.

### 3.8 No convention-plugin infrastructure

No `buildSrc`, no `build-logic`. `gradle/libs.versions.toml` has no `android-library` or
`kotlin-jvm` plugin alias. Note the AGP 9 constraint already documented in `app/build.gradle.kts`:
the standalone `kotlin-android` plugin must not be applied because AGP 9 compiles Kotlin itself.
That applies to every library module too.

---

## 4. Target module graph

```
:app
  MainActivity, NimazApp, the NavHost + NavigationSuiteScaffold shell,
  screens/adaptive, screens/home, the merged manifest, Firebase, signing,
  fetchNimazData, R8 config
   │
   ├─ :feature:quran      quran, khatam, bookmarks
   ├─ :feature:prayer     prayer times, qibla, night worship, adhan audio
   ├─ :feature:tracker    prayer tracker, fasting, tasbih
   ├─ :feature:content    dua, hadith, qaida, asma, asmaunnabi, names, prophets, catalog
   ├─ :feature:settings   18 screens, SettingsViewModel
   ├─ :feature:search     search + AI ask-with-proof
   ├─ :feature:tools      zakat
   ├─ :feature:calendar   Islamic calendar, events
   ├─ :feature:onboarding
   ├─ :feature:about      about, help, licenses, more
   └─ :feature:widget     6 Glance widgets, receivers, workers
        │
        ├─ :core:ui          atoms, generic Nimaz* molecules, theme, foundation,
        │                    ALL strings and locale resources
        ├─ :core:navigation  the Route hierarchy, ScreenTags, taggedComposable,
        │                    the announcement and help deep-link grammars.
        │                    No composables from features. No screen imports.
        ├─ :core:data        19 repository impls + mappers
        ├─ :core:database    both Room DBs, 15 entities, 22 DAOs, migrations, schemas/
        ├─ :core:datastore   PreferencesDataStore + the 11 SettingsSeams impls
        ├─ :core:common      util, time, monitoring, share, text
        └─ :core:domain      models, repository interfaces, 33 use cases   ← pure JVM
```

Eleven feature modules, seven core modules. Rules: `:core:*` never depends on `:feature:*`;
no `:feature:*` depends on another `:feature:*`; only `:app` depends on features.

Feature-specific molecules and organisms (`Home*`, `Mushaf*`, `Qaida*`, `Quran*`, `Prayer*`,
`Khatam*`, `Fasting*`, `Hadith*`, `Name*`) travel to their feature module. Only genuinely
generic `Nimaz*` components stay in `:core:ui`.

---

## 5. Migration phases

### Phase 0 — Foundations, no code moves

- Add a `build-logic` included build with convention plugins: `nimaz.android.library`,
  `nimaz.android.feature`, `nimaz.jvm.library`, `nimaz.android.hilt`, `nimaz.android.compose`.
  These carry compileSdk/minSdk, Java 21, the `-Xannotation-default-target=param-property`
  compiler arg, and the Hilt/KSP wiring so no module repeats them.
- Add `android-library` and `kotlin-jvm` aliases to `libs.versions.toml`.
- Enable `org.gradle.configuration-cache=true`. Multi-module gains most of its speed from
  configuration cache plus the build cache already enabled.
- **Record the baseline measurements in §6.5.** Without a before number, none of this can be
  shown to have worked.

*Gate:* `:app` builds identically; all 2,333 unit and 121 instrumented tests green;
`python3 scripts/check_docs.py` green.

### Phase 1 — Extract `:core:domain` as a pure JVM module

Fix §3.7 first (the five `Route` imports and the three KDoc references), then move `domain/`
wholesale under the `kotlin-jvm` plugin.

Highest-leverage single step: it compiles without AGP or Robolectric, its tests run in seconds,
and from here a stray `import android.*` in domain is a build failure rather than a review
comment. The sibling repo `arshad-shah/foolscap` ran exactly this step ("Make the pure layer its
own module") and is the model to copy.

### Phase 2 — The data-side core modules

Triage `core/util` first (§3.6) — split genuinely shared helpers into `:core:common` and push
the rest down to their real owners. Then:

- `:core:database` — both `@Database` classes, entities, DAOs, migrations, **and the `schemas/`
  directory**; the `room.schemaLocation` KSP arg and the `androidTest` `assets.srcDir(schemas)`
  wiring move with it.
- `:core:datastore` — `PreferencesDataStore` (990 LOC) plus the 11 settings seams.
- `:core:data` — 19 repository impls and mappers.

### Phase 3 — `:core:ui`

Atoms, the generic `Nimaz*` molecules, `theme/`, `foundation/`, and every resource including the
full `strings.xml` and all five translation directories. Fix the three design-system → feature
leaks. Feature-specific molecules and organisms stay put for now; they travel in Phase 5.

### Phase 4 — Decompose `NavGraph.kt` (the unlock)

- `:core:navigation` holds the `Route` hierarchy, `ScreenTags`, the `taggedComposable` helper,
  `HelpDeepLink`, `AnnouncementRoutes` and `WorshipDestinations` — and nothing that imports a
  screen.
- Convert each of the 94 destination registrations into per-feature
  `fun NavGraphBuilder.quranGraph(onNavigate: (Route) -> Unit)` extensions placed beside the
  screens they register — still inside `:app` at this point.
- `:app` keeps the `NavHost` and `NavigationSuiteScaffold` shell and calls each feature's graph
  function.
- **Every destination must still be registered with `taggedComposable`**, never a bare
  `composable` — `scripts/check_docs.py` NAV-04 enforces this and NAV-03 checks the count.

**This phase moves no files between modules and is independently shippable.** Ship it as its own
PR. It is the change that makes Phase 5 possible and it can be reviewed on its own merits.

### Phase 5 — Feature modules, one PR each, least-coupled first

1. `:feature:widget` — zero presentation dependencies, self-contained. Proves the pipeline on
   something that cannot break a screen.
2. `:feature:onboarding`, `:feature:about`
3. `:feature:tools`, `:feature:calendar`
4. `:feature:search`
5. `:feature:content` — dua, hadith, qaida, asma, asmaunnabi, names, prophets and catalog move
   **together**, because their ViewModels are one package.
6. `:feature:tracker` — prayer tracker, fasting, tasbih, likewise.
7. `:feature:quran` — quran, khatam, bookmarks. `QuranDao` stays in `:core:database`.
8. `:feature:prayer` — prayer times, qibla and night worship
9. `:feature:settings` — 18 screens and a 1,324-line ViewModel. Largest and most
   cross-referenced; do it last.

All 26 `screens/` directories are accounted for: `screens/adaptive` and `screens/home` stay in `:app`. The adaptive shell depends on twelve
features because it is the shell; moving it would invert the graph.

Each PR moves: screens, ViewModels, that feature's molecules and organisms, its Hilt module
slice, its nav-graph extension, and its tests.

### Phase 6 — Dissolve the DI god-modules and add guardrails

`RepositoryModule.kt` disappears; each module owns its `@Binds`/`@Provides`. `DatabaseModule`'s
DAO providers move to `:core:database`. Add the dependency-graph guardrail (§6.6).

---

## 6. How we validate

Six tiers, cheapest and most automatic first. Every phase gate runs tiers 1–3.

### 6.1 Tier 1 — let the compiler carry the load

This is the return on the whole exercise, so it is worth stating as the success metric rather
than a side effect. Today `presentation → domain → data` is enforced by review alone — there is
no architecture test in the repo. After Phase 1 it is enforced by the absence of the Android SDK
on `:core:domain`'s classpath.

**Metric:** count the architectural rules that became compile errors at each phase. If a phase
adds no enforcement, ask why it is being done.

### 6.2 Tier 2 — behavioural equivalence, every phase

- **All 2,333 unit tests and 121 instrumented tests green.** Non-negotiable, every phase.
- Because tests mirror the package tree and move with their code, **a test that stops compiling
  after a move is a genuine coupling signal, not migration noise.** Do not relax a test to make
  it compile — that is precisely the fault line where a regression hides.
- **Room schema identity.** After `:core:database` moves, diff the exported schema JSON for the
  current version byte-for-byte against the pre-move file. If Room regenerates it differently the
  identity hash has changed, and every existing install will attempt a destructive migration.
  Confirm `MigrationTestHelper` still finds the schemas via the `androidTest` assets wiring.
- **Hilt graph resolution.** `./gradlew :app:kspDebugKotlin` failing is the signal. Add a smoke
  instrumented test injecting the top ~20 singletons, so a missing binding fails loudly rather
  than on first open of some rarely-visited screen.

### 6.3 Tier 3 — the repo's own CI checks, which this migration will break

**`scripts/check_docs.py` is the sharpest hazard in this migration and deserves its own gate.**
It hardcodes

```python
APP = ROOT / "app/src/main/java/com/arshadshah/nimaz"
```

and derives every source path from it: `core/navigation`, `data/local/database/NimazDatabase.kt`,
`widget/`, `data/announcement/AnnouncementPayloadMapper.kt`, `domain/model/Announcement.kt`.
**All of those move.** The failure modes split in two, and only one of them is safe:

- **Loud (fine).** Single-file reads (`read()` has no existence guard) and `widget_packages()`
  (`.iterdir()`) raise on a missing path. CI goes red, someone fixes it.
- **Silent (dangerous).** SUB-02 (Workers), SUB-03 (Services), SUB-05 (notification channel ids)
  and SUB-06 (DataStore file names) scan `APP.rglob(...)`. Once `widget/` (6 workers) and
  `data/audio/` (3 services, 1 worker) leave that tree, those globs return a *shrinking* set and
  the "every X is documented" assertions pass **vacuously**. CI stays green while checking
  progressively less.

**Therefore:** in Phase 0, before any code moves, refactor `check_docs.py` to resolve its roots
across all modules, and add a self-check asserting each scan found a non-zero, expected-minimum
count. An empty scan must fail, not pass.

Also on the CI lane:

- **`scripts/coverage_summary.py`** reads a pinned XML path (`app/build/reports/jacoco/jacocoTestReport.xml`)
  that Phase 0 changes.
- **`fastlane/Fastfile`** runs `:app:testDebugUnitTest` and `:app:lintDebug`. After the split
  those cover a shrinking fraction of the codebase — move to all-module `testDebugUnitTest` /
  `lintDebug`.
- **The five JaCoCo report tasks** in `app/build.gradle.kts` hardcode `src/main/java` and
  `intermediates/classes/debug`. **These have already failed silently once** — producing 237-byte
  reports that read as 0% rather than as an error. Re-point them per module or move to a merged
  multi-module report, and **assert the report is non-empty**, because an empty JaCoCo report and
  genuine zero coverage look identical at a glance.
- **`scripts/check_tajweed_contrast.py`** and `check_mermaid.mjs` — re-check their path roots too.

### 6.4 Tier 4 — artifact equivalence, which catches what tests cannot

Build a release AAB before and after each phase and diff:

- **Merged manifest** (`build/intermediates/merged_manifests/release/AndroidManifest.xml`) —
  identical modulo element ordering. There are 14 components: six widget receivers,
  `WidgetTickReceiver`, `BootReceiver`, three audio services, `NimazMessagingService`, a
  `FileProvider` and `MainActivity`. Manifest merging across library modules is exactly where an
  `exported` or `process` attribute changes silently.
- **DEX class list and method count.**
- **Merged resources.** With six locales and `enableSplit = false`, a dropped `values-tr` is
  invisible until a Turkish user opens the app.
- **R8 output.** `isMinifyEnabled = true` *and* `isShrinkResources = true`. **Resource shrinking
  across module boundaries is the most likely source of a silent, release-only regression in this
  whole migration** — an asset referenced by name rather than by `R.` reference can survive in the
  monolith and be stripped once it lives in a library module. The eight Quran fonts in `res/font`
  are exactly this shape of risk. Diff the resource-removal report, not just the mapping file.
- **BuildConfig fields survive:** `CONTENT_ARTIFACT_SHA256` (computed from `data.lock.json`),
  `PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER`, `AI_WORKER_BASE_URL`, and the `versionCode`/`versionName`
  that CI bumps and pushes back to `dev`.

Runtime-only hazards:

- **Widgets.** Six Glance widgets, seven receivers, six workers, thin instrumented coverage. Pin
  all six manually and confirm they update after any phase touching `:feature:widget` or
  `:core:database`.
- **Baseline profile.** Fully-qualified class names do not change under this migration (package
  names are stable), so `app/src/main/baseline-prof.txt` should still match — but regenerate and
  diff anyway. A profile that quietly stops matching costs startup time and reports no error.
- **The `fetchNimazData` task-ordering workaround.** `app/build.gradle.kts` makes every
  `merge*Assets` and every `lint*` task depend on `fetchNimazData`. This has already broken the
  release lane once while both PR lanes stayed green, because lint-vital runs for release only.
  **Once a library module consumes those generated assets the same race reappears there.** Move
  the workaround into the convention plugin rather than copying it per module.
- **Firebase.** `google-services.json` exists only on the CI release lane and the plugins are
  applied conditionally on its presence. Confirm the Crashlytics mapping upload still resolves
  once `:app`'s own class set has shrunk to a shell.

### 6.5 Tier 5 — did it actually pay off?

Baseline all of these in Phase 0 and re-measure at every phase gate.

**The baseline is [`BASELINE.md`](BASELINE.md)** — measured on `mm/02-baseline-metrics`, with the
protocol it was taken under written down beside it. The `:app:compileDebugKotlin` = 3m 44s figure
that used to stand here has **no recorded protocol** (no branch, daemon state, `clean` vs
`--rerun-tasks`, or content-cache state) and is not reproducible as stated; do not compare
against it. `BASELINE.md` also records which rows are expected *not* to improve, and pins exactly
which number the stopping rule below is read from.

| Measurement | Expectation |
|---|---|
| Clean `:app:assembleDebug` | Roughly flat, possibly slightly worse |
| Incremental: touch a leaf screen | **Should improve substantially — the number that matters** |
| Incremental: touch `NimazButton.kt` | Will not improve; shared hub by design |
| Incremental: touch a `domain` model | Will not improve much |
| Incremental: touch `strings.xml` | Will not improve; deliberately unsplit |
| `testDebugUnitTest` wall time | Should improve via parallel module execution |
| `:core:domain:test` alone | Should drop to seconds — no pre-migration value exists; first measured at the PR 5 gate |
| Configuration time (`--profile`) | Watch it — more modules means more configuration |

**Stopping rule:** if incremental rebuild after touching a leaf screen has not improved by at
least 40% once Phase 5 is half done, the split is not paying for its complexity. Stop and
reassess rather than finishing the remaining features out of momentum.

### 6.6 Tier 6 — guardrails so it does not regress

- **A dependency-graph check** wired into `check`: fail the build if any `:core:*` depends on a
  `:feature:*`, or if any `:feature:*` depends on another `:feature:*`. Without this the graph
  degrades back to a mesh within months.
- **Update `docs/ARCHITECTURE.md`** — §9's tech-debt registry and the DI section, which currently
  states "All modules live in `core/di`", become wrong at Phase 6.
- **Update `docs/CLEAN_ARCHITECTURE_CHECKLIST.md`** — several of its detection commands grep
  paths that move.

---

## 7. What I would push back on

- **26 screen directories are not 26 modules.** Group by ViewModel package: eleven feature
  modules, not twenty-six.
- **Do not split `strings.xml`.** 1,910 strings across six locales for no build-time gain given
  `enableSplit = false`.
- **Leave `screens/adaptive` in `:app`.** It depends on twelve features because it is the shell.
  That is not a violation to be fixed.
- **Phase 4 before Phase 5, always.** Attempting a feature module while `NavGraph.kt` still
  imports 70 screens produces a circular dependency and a bad week.
- **Do not start Phase 5 without the Phase 0 baseline numbers.** The justification for this work
  is build times. If we cannot show the before, we cannot show the after, and the whole thing
  becomes a matter of taste.
- **Fix `check_docs.py` in Phase 0, not reactively.** It is the one piece of CI that will go
  green while silently checking less, and that is worse than it going red.
