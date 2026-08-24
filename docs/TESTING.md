# Nimaz — Testing

> **Owns:** how the test suites are invoked, and the instrumented (`androidTest`) suite in full —
> how to run it, how it is wired into CI, its module layout, what it covers, and its conventions.
> **Update when:** you add or restructure a test source set or module, change how the suites are
> invoked, change the emulator/CI lane, or change a testing convention (screen tags, Hilt test
> rules, seeding).
> **Verified by:** review only — no mechanical check. `NAV-04`/`NAV-05` in
> `scripts/check_docs.py` guard the screen-tag contract these tests depend on.
> **Related:** [`NAVIGATION.md` §1](NAVIGATION.md#1-how-navigation-works) for `ScreenTags`,
> [`ARCHITECTURE.md`](ARCHITECTURE.md) for what the unit tests assert,
> [`DOCUMENTATION.md`](DOCUMENTATION.md) for the update contract.

Two on-device/JVM test surfaces exist:

- **Unit / Robolectric** (`src/test` and `src/testDebug` in every module) — ViewModel logic and
  Compose "atom/molecule/organism" component tests run off-device under Robolectric. Since the
  module split (#551) these live beside their subject: the design-system component tests are in
  `core/ui/src/testDebug`, each feature's ViewModel tests in that feature's module. Coverage is
  reported via the `jacoco*Report` Gradle tasks — see [§ How coverage is measured](#how-coverage-is-measured).
- **Instrumented** (`app/src/androidTest`) — the suite documented here. Runs on an
  emulator/device against the real Hilt graph, Room database, WorkManager, and
  `MainActivity`/`NavGraph`.

## Contents

- [Running the unit tests](#running-the-unit-tests)
- [Running the instrumented suite](#running-the-instrumented-suite)
- [CI (emulator.wtf)](#ci-emulatorwtf)
- [How it's wired](#how-its-wired)
- [Module layout (`app/src/androidTest/java/com/arshadshah/nimaz`)](#module-layout-appsrcandroidtestjavacomarshadshahnimaz)
- [How coverage is measured](#how-coverage-is-measured)
  - [What is not counted, and why](#what-is-not-counted-and-why)
  - [Two things that silently do not count](#two-things-that-silently-do-not-count)
  - [Reading the number](#reading-the-number)
  - [Where each module stands](#where-each-module-stands)
- [Coverage audit (what's validated)](#coverage-audit-whats-validated)
  - [The same ground, on the JVM (`:core:database/src/test`)](#the-same-ground-on-the-jvm-coredatabasesrctest)
  - [The reader, on the JVM (`:feature:quran/src/test` and `src/testDebug`)](#the-reader-on-the-jvm-featurequransrctest-and-srctestdebug)
  - [The layer everything compiles against (`:core:domain/src/test`)](#the-layer-everything-compiles-against-coredomainsrctest)
  - [The route vocabulary (`:core:navigation/src/test`)](#the-route-vocabulary-corenavigationsrctest)
  - [The Firebase wrappers and the formatters (`:core:common/src/test`)](#the-firebase-wrappers-and-the-formatters-corecommonsrctest)
  - [The Islamic calendar (`:feature:calendar/src/test`)](#the-islamic-calendar-featurecalendarsrctest)
- [Conventions](#conventions)

## Running the unit tests

```bash
./gradlew testDebugUnitTest              # every Android module — nineteen of them
./gradlew :core:domain:test              # :core:domain is a pure JVM module, so `test`, not `testDebugUnitTest`
./gradlew :build-logic:convention:test   # the convention plugins and their TestKit fixtures
./gradlew coverageFloor                  # every locked module's coverage gate
```

Two things to know about that set.

**Never `:app:testDebugUnitTest` on its own.** Since #551, `:app` holds 52 files — 8% of the
codebase — so naming it explicitly runs a small fraction of the suite and reports success. The
unqualified `testDebugUnitTest` reaches every Android module. `:core:domain` needs its own line
because a `kotlin-jvm` module has no build variants and therefore no `testDebugUnitTest` task at
all; asking for the unqualified task silently does not run it.

**`build-logic` is an included build, so `./gradlew test` does not reach into it** — an included
build's tasks only run when asked for by name. A change to a convention plugin, to
`FetchNimazDataTask` or to `NimazDataCredentials` is therefore **not** covered by
`testDebugUnitTest`, `lintDebug` or `assembleRelease`; run the second command above when you
touch `build-logic/`. CI does not rely on you remembering: `fastlane/Fastfile`'s `test` lane runs
`:build-logic:convention:test` first, before the `:app` tasks, so a broken convention plugin
fails the PR check rather than surfacing later as an unexplained build failure.

Use `:build-logic:convention:test` — the fully qualified form — everywhere. `-p build-logic test`
runs the same suite, but it is a second spelling of one command, and `ARCHITECTURE.md` and the
`Fastfile` both use the qualified one.

## Running the instrumented suite

```bash
# Boot an emulator (API 29+; the app's minSdk) first, then:
./gradlew :app:connectedDebugAndroidTest

# A single class / package:
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.arshadshah.nimaz.db.PrayerDaoTest
```

Requires JDK 21 + Android SDK (`local.properties` `sdk.dir` or `ANDROID_HOME`).

## CI (emulator.wtf)

`.github/workflows/android_instrumented_tests.yml` runs the instrumented suite in the
cloud on every push to `main` and on every pull request. It:

1. builds `:app:assembleDebug` + `:app:assembleDebugAndroidTest` (this also serves as
   the compile check for the androidTest sources / Hilt test components),
2. uploads both APKs as the `instrumentation-apks` artifact,
3. routes them to the [`emulator-wtf/actions/run-tests`](https://github.com/emulator-wtf/actions)
   action (sharded, Pixel2 / API 30 — note minSdk 29 rules out the default API 27 device),
4. publishes the JUnit results as a check.

**Setup:** add an `EW_API_TOKEN` repository secret (Settings → Secrets and variables →
Actions). Without it the workflow still builds and uploads the APKs but skips the cloud
run with a warning — it never hard-fails for a missing token.

## How it's wired

- **Runner:** `support/HiltTestRunner` swaps in `HiltTestApplication` so tests run on
  the full Hilt object graph **without** `NimazApp`'s production bootstrap (Firebase,
  `AppInitializer` locale/notification/adhan work). Registered as
  `testInstrumentationRunner` in `app/build.gradle.kts`.
- **Splash guard:** because the host Application under test is `HiltTestApplication`
  (not `NimazApp`), `MainActivity` resolves its initializer with a null-safe cast
  (`application as? NimazApp`) and lifts the splash immediately when there is none.
  This is behaviourally identical in production.
- **WorkManager:** worker tests configure WorkManager via `WorkManagerTestInitHelper`
  with the injected `HiltWorkerFactory` (`androidx.work:work-testing`).
- **Screen tags:** every routed destination is wrapped by `taggedComposable` in
  `NavGraph` with a stable tag from `core/navigation/ScreenTags`, and each bottom-nav
  item carries `ScreenTags.bottomNav(label)`. Because the tag is on the wrapper (composed
  immediately, before any data loads), navigation assertions are deterministic and
  independent of locale, seeded content, or on-screen copy. The hub lists (More,
  Settings) are also tagged so tests can scroll to off-screen entries. This is the one
  source of truth — `Selectors` in the test code references `ScreenTags` directly.

## Module layout (`app/src/androidTest/java/com/arshadshah/nimaz`)

| Package          | What it covers |
|------------------|----------------|
| `support/`       | Shared infra — **no test owns a magic string**. `HiltTestRunner`, `Selectors` (nav labels, real `testTag`s, `R.string` lookups), `TestData` (entity builders), `NimazDbRule` (in-memory Room), `BaseAppTest` (Compose + Hilt base for UI flows). |
| `db/`            | Round-trip CRUD + `Flow` coverage for every major DAO (prayer, fasting, tasbih, khatam, quran, dua, hadith, zakat, tafseer, qaida, names, location, events), plus `DatabaseAssetTest` verifying the shipped prepopulated DB seeds. |
| `preferences/`   | `SettingsRepository` (DataStore) round-trips + export/import, and the per-prayer alert style / reminder settings with their one-shot migration (`PrayerAlertSettingsTest`). |
| `notifications/` | `PrayerNotificationScheduler` channel creation + schedule/cancel smoke. |
| `work/`          | Every `@HiltWorker` widget/adhan worker executed via the real factory. |
| `navigation/`    | Launches `MainActivity` and asserts navigation **by `ScreenTags`**: all 5 bottom-nav tabs (`BottomNavigationTest`), every More-menu feature — daily practice, learning, tools, support (`FeatureNavigationTest`), every Settings sub-screen (`SettingsNavigationTest`), and back-navigation round-trips (`MoreMenuNavigationTest`). |
| (root)           | `MigrationTest` (per-step) and `MigrationChainTest` (v7→current). |

## How coverage is measured

There are two reports and one gate.

**`:app:jacocoTestReport`** is the merged one, across every module. It is **reported, not gated**
(#464): CI publishes the number, nothing fails on it.

**`moduleCoverage`** is per module, and every module that applies `jacoco` has one — the
convention plugins register it (see [`ARCHITECTURE.md` §
Convention plugins](ARCHITECTURE.md#convention-plugins-build-logic)). It measures that module's
own classes against the **same** `COVERAGE_EXCLUSIONS` the merged report uses, which is why the
list lives in `build-logic` rather than in `app/build.gradle.kts`: a module that passed its own
gate at 82% and read 61% in a report nobody could reconcile it with would be worse than no gate.

**`coverageFloor`** is the gate. It reads `moduleCoverage` back and fails `check` when the module
has slipped below the coverage it is locked at:

```bash
./gradlew coverageFloor          # unqualified: every module that has one
./gradlew :core:database:moduleCoverage   # …and the number behind it
```

A module joins by declaring a floor in its own build file; a module that has not been reached yet
declares none, still gets a report, and has no gate. The standard is **80% of lines**, and the
floor is a ratchet — raised when a module is brought up to it, never lowered to make a build
green. The branch floor is set per module, at 80% where branches are reachable and lower where
the Compose compiler's `$dirty` checks make them not (see
[Where each module stands](#where-each-module-stands)). `:core:database` is the first module
locked (#597); then `:feature:quran`, `:core:domain`, `:core:navigation`, `:core:common` and `:feature:calendar`.

### What is not counted, and why

Three groups are excluded, and the third is the one that needed a decision.

**Generated code** — `R`, `BuildConfig`, Hilt/Dagger, Room's `_Impl`, and the
`ComposableSingletons` class the Compose compiler emits per file for `@Preview` lambdas.

**`$DefaultImpls`** — the static bridge Kotlin emits beside an interface's default methods. Since
the compiler started emitting real JVM default methods it exists for binary compatibility and is
**never called**: the body is measured on the interface itself. `:core:database` alone carries 45
lines of it across eight DAOs, permanently 0% and unreachable by any test that could be written.

**Room entities and DAO row projections** — a primary constructor and nothing else, so every
"uncovered line" is a generated `equals`/`hashCode`/`toString`/`copy`/`componentN`. On
`:core:database` they were 798 of 1,310 measurable lines, 61% of the module: leaving them in makes
a floor mostly a statement about how many columns the database has. Matched by class-name suffix
rather than by package, because entities are declared in three places and a package glob would
miss two of them; `*Row` is anchored so it does not also swallow `ObserveKhatamRowProgressUseCase`.

One pattern came **off** the list. `*$Companion*` reads like more generated noise and is not: this
codebase keeps its Room migrations in `NimazDatabase.Companion`, so it was hiding all eighteen of
them — 210 lines of the code where a mistake is a crash on launch rather than a wrong screen.
A companion object is somewhere people put code.

It spans all nineteen modules because `coverageModules` in `app/build.gradle.kts` lists them
one by one, each with the globs for that module's class output and its `.exec` file. Two
assertions in the task's `doLast` keep it honest, and both exist because the failure they catch
reads as *low coverage* rather than as an error:

- a **class-count floor** (200, against a real 4,226) — a report that parses but describes
  nothing looks identical to 0%;
- a **per-module presence check** — every entry in `coverageModules` must contribute at least
  one package, so a module whose output path moved cannot quietly drop out and *raise* the
  percentage by measuring less.

**Add a module, add it to `coverageModules`.** There is no discovery step.

### Two things that silently do not count

**Robolectric used to contribute nothing.** Robolectric loads the classes under test through its
own instrumenting classloader, and the classes it hands back carry no source location, so
JaCoCo's agent skips them by default. The tests run, pass, and record nothing — and a class with
no execution data is indistinguishable from a class no test ever touched. The repository has 936
`@Test` methods in 154 Robolectric files across nine modules, so this was not a rounding error:
`LegacyUserDataImport` (94 lines, twelve passing Robolectric tests) reported **0%**, and
`presentation/components` — the design system, tested by 69 Robolectric files — reported
**13.2%**.

`configureRobolectricCoverage()` in `build-logic`'s `Conventions.kt` sets
`isIncludeNoLocationClasses` (with the mandatory `jdk.internal.*` exclusion) on every `Test` task
in any module that applies `jacoco`, and all three convention plugins call it. That single change
moved the merged report from **22.8% to 39.3%** of lines and **9.0% to 22.2%** of branches,
without a test being written. `AndroidLibraryConventionPluginTest` asserts the flag is set, and
asserts a module without the `jacoco` plugin still configures cleanly.

**Instrumented tests still contribute nothing.** `enableAndroidTestCoverage` is set nowhere and
the emulator.wtf lane collects no coverage, so the 121 instrumented tests documented below are
absent from the merged report. That is why `presentation/screens` reads ~3%: the screens are
exercised, just not measured. Wiring it is tracked separately.

### Reading the number

Compose UI is 41,138 of the 67,970 counted lines. The layer split is what the percentage is
actually made of — measured with `:app`'s own suite excluded, because it needs the private content
artifact, so it accounts for 26,383 of CI's 26,739 covered lines:

| Layer | Covered / total | % |
|---|---|---|
| `domain` | 3,294 / 4,181 | 78.8% |
| `presentation/viewmodel` | 5,497 / 8,251 | 66.6% |
| `presentation/other` (foundation, model) | 1,077 / 1,778 | 60.6% |
| `presentation/components` | 11,798 / 20,753 | 56.8% |
| `data` | 2,477 / 6,578 | 37.7% |
| `core` | 1,279 / 4,326 | 29.6% |
| `widget` | 364 / 1,598 | 22.8% |
| `presentation/screens` | 597 / 20,385 | 2.9% |

CI's comment on the pull request is the authoritative headline; the table above is that same run
without `:app`, which is the part that cannot be reproduced locally.

That layer table predates the exclusion change above and is not comparable to a number measured
after it — the entity and `$DefaultImpls` classes it counted are gone from the denominator, and the
companion objects it did not count are in it. CI's next run is the one to read.

### Where each module stands

`./gradlew moduleCoverage` prints all of them. The snapshot below is the starting line for the
module-by-module pass; a module is **locked** once it clears the 80% line floor and declares its
floors in its own `build.gradle.kts`.

**The line floor is 80% everywhere; the branch floor is not.** `:core:database`, `:core:domain`,
`:core:navigation` and `:core:common` are locked at 80/80 — none of them draws anything, so every
branch is one somebody wrote and a test can take both sides of. So is `:feature:calendar`, which
*is* a Compose module: the unreachable branch below is emitted **per parameter of every
restartable composable**, so its weight scales with how many composables a module has, and six
files never accumulate enough of them to move the number. Only `:feature:quran` is softened, to
80% lines and **60%** branches, because the Compose compiler
emits a `$dirty` bitmask branch per parameter of every restartable composable — the skippability
check — and neither side of one is reachable from a test: which side runs depends on what the
*caller* changed between recompositions. A Compose-heavy module therefore carries thousands of
branches that are unreachable by construction, and reports ~65% branches while covering 81% of
its lines. An 80% branch floor there would measure how many composables the module has, not how
well it is tested. Every Compose-heavy module reached after this one should expect the same
split, and should say so where it sets its floors.

| Module | Line | Branch | |
|---|---|---|---|
| `:core:database` | 97.3% | 86.8% | **locked** at 80/80 (#597) |
| `:feature:quran` | 81.2% | 64.6% | **locked** at 80 line / 60 branch |
| `:core:domain` | 82.3% | 83.9% | **locked** at 80/80 |
| `:core:navigation` | 84.2% | 85.3% | **locked** at 80/80 |
| `:core:common` | 92.7% | 82.3% | **locked** at 80/80 |
| `:feature:calendar` | 94.0% | 82.4% | **locked** at 80/80 |
| `:core:ui` | 50.3% | 47.8% | |
| `:core:data` | 39.0% | 31.2% | |
| `:feature:prayer` | 38.1% | 23.7% | |
| `:feature:tracker` | 30.4% | 24.4% | |
| `:feature:content` | 28.8% | 19.5% | |
| `:core:datastore` | 21.2% | 31.4% | |
| `:feature:search` | 18.8% | 13.9% | |
| `:feature:tools` | 18.0% | 29.3% | |
| `:feature:about` | 17.8% | 23.0% | |
| `:feature:widget` | 16.8% | 28.1% | |
| `:feature:onboarding` | 12.9% | 12.3% | |
| `:feature:settings` | 11.3% | 14.9% | |

`:app` is absent because its own suite needs the private content artifact and cannot be measured
locally; CI's merged report covers it.

## Coverage audit (what's validated)

| Layer / area | Covered by | Kind |
|---|---|---|
| Every DAO (prayer, fasting, tasbih, khatam, quran, dua, hadith, zakat, tafseer, qaida, names, location, events) | `db/*DaoTest`, `UserDataDaoTest` | CRUD + Flow round-trips on in-memory Room |
| **A content release reaching an install made before the database split** | `db/ContentReleaseTest` | the installer + `UserDataMigrator` + the real store, across two launches, on real SQLite and real SharedPreferences |
| Shipped prepopulated DB seeds | `db/DatabaseAssetTest` | real DI database (LFS asset) |
| Schema migrations (per-step + full v7→current) | `MigrationTest`, `MigrationChainTest` | `MigrationTestHelper` |
| Settings **persistence** (DataStore) | `preferences/SettingsRepositoryTest` | flow round-trips + export/import |
| **Per-prayer alert style + reminder**, and the migration off the old global pair | `preferences/PrayerAlertSettingsTest` | per-prayer isolation; migration runs once and never resets a later choice |
| Notification channels (incl. the muted channel the SILENT style needs) + schedule/cancel | `notifications/PrayerNotificationSchedulerTest` | Hilt singleton |
| Every background worker | `work/WidgetWorkersTest` | `HiltWorkerFactory` + `WorkManagerTestInitHelper` |
| App launch + all 5 bottom-nav tabs | `navigation/AppLaunchTest`, `BottomNavigationTest` | real `MainActivity`/`NavGraph`, by `ScreenTags` |
| Every More-menu feature opens (16) | `navigation/FeatureNavigationTest` | tag-asserted, with back round-trips |
| Every Settings sub-screen opens (8) | `navigation/SettingsNavigationTest` | tag-asserted |
| **First-run / onboarding completes + persists** | `behavior/OnboardingFlowTest` | drives the real screen |
| **Settings toggles actually work** (UI switch → DataStore) | `behavior/SettingsBehaviorTest` | Appearance haptic/24h/animations + notification vibration |
| **Tasbih counter increments on tap** | `behavior/TasbihCounterTest` | tag-driven interaction |
| **A surah's card, against the real content database** | `behavior/QuranSurahInfoSheetTest` | the sheet's facts and its opening page read from the shipped artifact's pagination, and "Read surah" reaching the reader through the real `NavGraph` |

**Deliberately validated at the data/VM layer, not via UI** (UI input is brittle and the
logic is unit-tested): Zakat calculation math (`ZakatDaoTest` + the Zakat ViewModel unit
tests), prayer-tracker status changes (`PrayerDaoTest.updatePrayerStatus`), bookmark/
favorite toggles for Quran/Hadith/Dua (the `*DaoTest` `toggle*` cases). The screens that
surface these all render and navigate (covered above); the state transitions are proven
on the layer that owns them.

**Known UI-test exclusion:** the Qibla compass screen recomposes continuously from the
sensor listener and never reaches Compose idle, so it is not driven by idling-based UI
tests (its tab presence is asserted in `AppLaunchTest`).

### The same ground, on the JVM (`:core:database/src/test`)

The instrumented `db/*DaoTest` classes above are on-device smoke — a row goes in, the same
row comes back. What is asserted on the JVM instead is everything about a table that only
shows up as a *wrong row count*: a transaction half-applied, an ordering nobody pinned, a
statistic with the wrong `WHERE`. None of it is visible from a ViewModel and none of it
raises an error, so it needs a real database and an explicit assertion.

It is also the half that runs on **every** pull request. The emulator lane needs
`EW_API_TOKEN` and skips with a warning without it, and instrumented runs contribute
nothing to the merged coverage report (see [above](#two-things-that-silently-do-not-count)).

| Area | Covered by | What it pins |
|---|---|---|
| Khatam progress | `dao/KhatamDaoTest` | exactly one active khatam; re-marking a verse does not inflate the total; `started_at` survives re-activation; the cascade behind "delete all my data" |
| Prayer tracking | `dao/PrayerDaoTest` | the review banner never overwrites a logged prayer, never marks sunrise, and stops at the range's ends; perfect days; per-prayer statistics |
| Fasting and the debt a missed fast leaves | `dao/FastingDaoTest` | one record per day; a makeup fast leaving `pending` by both doors (completed / fidya paid); the streak read stopping at today |
| Zakat history | `dao/ZakatDaoTest` | `getTotalPaid()` is null, not zero, on an empty table; a repeat calculation is a new row |
| The one bookmark table that replaced seven | `user/BookmarkDaoTest` | un-favouriting a bookmarked verse keeps the bookmark; `pruneEmpty` takes only the rows with neither flag; `kind` isolation |
| The one progress table that replaced three | `user/ProgressDaoTest` | `increment`/`decrement` as read-modify-writes: per-day keys, a floor at zero, and the fields a count must not clear |
| Counting sessions | `user/TasbihSessionDaoTest` | `currentCount + (totalLaps * targetCount)` across all four statistics; the ranked preset list |
| Commentary annotations | `user/TafseerUserDaoTest` | the `IN (:ayahIds)` range reads that replaced the cross-database join |
| Custom presets, reading position | `user/CustomPresetDaoTest`, `user/ReadingProgressDaoTest` | stable ordering under equal `display_order`; the single `id = 1` row |
| **Migration idempotence** | `NimazDatabaseMigrationTest` | every step run twice, and run against an artifact that arrived without the tables it names — the shape that crashes a *fresh* install, since Room runs migrations after `createFromAsset` too |
| Migration data repair | `NimazDatabaseMigrationTest` | translations de-duplicated to the lowest id; commentary folded into blocks without bridging a gap; juz/hizb/page derived from the columns that carried them |
| The legacy-import completion flag | `user/UserDataMigratorTest` | it is set when there is nothing to copy, and **never** on a run that failed — `ContentArtifactInstaller` deletes the source file once it is set |
| The content-artifact record | `content/SharedPreferencesContentArtifactStoreTest` | the defaults a fresh install reads, and that all three values survive a new store over the same file |
| The same release sequence, as logic | `ContentReleaseIntegrationTest` | which component runs when, and what the flag between them means — the ordering that decides whether the installer deletes rows the migrator has not copied yet |
| Swapping a content collection | `dao/ContentReplacementTest` | delete-children-first / insert-parents-first across the foreign keys; `is_custom` being the only thing that separates a shipped preset from a user's |
| FTS query assembly | `search/ContentSearchIndexQueryTest` | placeholders and bound arguments staying in step when the optional `source` filter is present; the two paths that must never reach a `MATCH` |

### The reader, on the JVM (`:feature:quran/src/test` and `src/testDebug`)

`:feature:quran` is the second module locked. It is 11,402 measurable lines — the largest feature
in the app — and almost all of it is Compose, so the tests are Robolectric: the screen or the
component is composed for real, driven through its own semantics, and asserted on what a reader
would see. `src/test` holds the ViewModel and screen tests, `src/testDebug` the component ones
(`@Preview` tooling is a debug-only dependency).

| Area | Covered by | What it pins |
|---|---|---|
| The reader's event table | `viewmodel/QuranViewModelEventsTest` | every event reaching its own handler, and `PrefetchPage` not retitling the reader |
| Notes, bookmarks, the page layout | `viewmodel/QuranViewModelAnnotationTest` | a note on an unmarked verse *creates* the mark, a note on a marked one updates that row; the optimistic bookmark flip reaching both copies of the verse; the line layout fetched once |
| A khatam's "today" line | `viewmodel/KhatamDetailPortionTest` | surah/ayah and global ayah id translated between without drift; a portion inside one surah named once and one that crosses named at both ends; a portion past the end left unlabelled |
| The subject browser | `viewmodel/QuranTopicsSurahSubjectsTest`, `QuranTopicsViewModelDescentTest` | "this install has no index" versus "this surah has none"; a tree switch dropping children keyed by the other tree's parent ids |
| The bookmarks screen's axes | `viewmodel/BookmarksViewModelAxesTest` | corpus and kind as independent filters |
| The tafseer notes dialog | `screens/quran/TafseerNotesDialogTest` | one field doing two jobs — the dialog leaving edit mode after a save, so the next note does not overwrite the one just edited |
| The surah card's seam | `screens/quran/SurahInfoSheetHostTest` | nothing drawn until the surah is known; the opening page taken from the pagination, not from the Madani `startPage` |
| The reader screens | `screens/quran/QuranReaderScreenRenderTest`, `QuranReaderPageModeTest`, `QuranReaderActionsTest` | what renders in each reading mode, and which event each action emits |
| The route graph | `screens/quran/QuranGraphTest` | all 19 destinations registered |
| Tajweed colouring | `components/organisms/QuranAyahItemTajweedTest` | the second renderer; tap-to-explain resolving a coloured word to its rule; a surah-opening verse not printing the bismillah the header already prints |
| Selecting commentary | `components/molecules/TafseerHighlightableTextGesturesTest` | a long press seeding a whole word, a tap inside a highlight reopening it, a tap outside one clearing the selection |
| The saved-item card | `components/organisms/SwipeableSavedCardTest` | the overflow menu closing *before* its action fires; a card opted out of swiping not deleting itself |
| The note editor | `components/molecules/NoteEditorSheetTest` | an emptied field saving `null` rather than `""`; the draft keyed on the subject so a different verse starts blank |
| The running head over a long document | `components/organisms/NimazScrollSpyIndexTest` | two sections with the same heading both surviving the lazy row's key; scrolling through a long section still naming it |

**What is left uncovered, and why.** `TafseerPdfExporter` and `PageWriter` (209 lines) cannot run
under Robolectric — its `PdfDocument` shadow throws *"document is closed!"* on the first
`startPage`. `AdaptiveQuranScreen`/`AdaptiveKhatamScreen` (134 lines) embed `hiltViewModel()`
screens, so composing them needs Hilt. `QuranGraph`'s destination lambdas are bodies that only
run when a destination is actually navigated to. The `@Preview` and `*Showcase` functions across
the module are tooling, not behaviour, and are deliberately not driven.

### The layer everything compiles against (`:core:domain/src/test`)

`:core:domain` is the third module locked, and the one whose tests are cheapest to run: a pure
JVM module, no Android SDK, no Robolectric, the whole suite in about a minute.

| Area | Covered by | What it pins |
|---|---|---|
| Every stored-value parser | `model/StoredEnumParserTest` | a round trip per entry, and the documented fallback for input a newer build wrote |
| Every hand-written enum label | `model/EnumLabelTest` | present, distinct and not the constant's own name — the copy-paste that puts two identical rows in a picker |
| The curated city catalogue | `model/CityCatalogTest` | no two cities sharing a lazy-list key (a repeat is a crash, not a duplicate row); every city carrying the region and flag its row is drawn with |
| Subject roll-up | `usecase/quran/RollUpTopicCountsTest` | a branch reporting its whole subtree, and a cycle in regenerated content costing a wrong number rather than a hung browser |
| Prayer and location use cases | `usecase/PrayerUseCasesTest` | each delegation reaching its own repository call; a saved location composed here, with `id = 0`, rather than by the caller |
| Qur'an use cases | `usecase/QuranUseCasesTest` | the same, plus the verse of the day surviving a negative `epochDay`, and a bulk translation read short-circuiting an empty `IN ()` |
| The day's dua, and notes on a commentary | `usecase/DailySelectionAndNotesTest` | the hour→category bands; an empty category yielding nothing rather than dividing by zero; a note whose verse cannot be read dropped rather than shown blank |
| The Hijri month grid | `usecase/calendar/BuildHijriMonthUseCaseTest` | a grid as long as the month actually is — 29 or 30, decided per month per year |
| The calculator's "today" questions | `calendar/HijriDateCalculatorTodayTest` | a Ramadan countdown inside one year, and "is it Ramadan" never disagreeing with "days remaining" |
| The night window and a location's own settings | `prayer/PrayerTimeCalculatorSunnahTest` | middle-of-the-night before the last third; the method, madhab and high-latitude rule on a saved `Location` reaching the calculation |

**Tests that were in the wrong module.** `LocationCatalogTest` lived in `:app/src/test` while the
code it exercised had moved to `:core:domain` — so the merged report counted it and this module's
own report did not. Its catalogue half is now `CityCatalogTest` here; the `formatCoordinates` half
stayed in `:app`, next to the settings screen that owns it. Worth checking for when a module is
brought up: a per-module number can be low because the tests are elsewhere, not because they are
missing.

**What is left uncovered, and why.** The `*UseCases` aggregators (`QuranUseCases` at 41 lines,
`PrayerUseCases` at 23, and six more) are `data class`es whose entire body is injected `val`s.
Nothing constructs one outside Hilt, so no behavioural test reaches them; they are not excluded
from the measurement, just left at zero. The rest is domain-model constructors and accessors,
which the tests above reach only where a behaviour actually returns one — deliberately, since a
test that asserts on a generated `copy()` measures nothing.

### The route vocabulary (`:core:navigation/src/test`)

The fourth module locked, and the one whose number moved most for the least work: **21 of the 30
points came from moving five test files out of `:app/src/test`**, where they were exercising this
module's code (see below).

| Area | Covered by | What it pins |
|---|---|---|
| The announcement route grammar | `AnnouncementRoutesTest` | every published key resolving, and the arguments a targeted announcement carries |
| The help deep-link grammar | `HelpDeepLinkTest` | all 22 keys resolving, and **no two leading to the same screen** — a working button that goes somewhere else is worse than no button |
| Content and proof targets | `ContentTargetRoutesTest`, `SearchProofNavigationTest` | a citation resolving to the destination that can show it |
| Worship destinations | `WorshipDestinationsTest` | each reminder type opening its own screen |
| The five bottom-nav tabs | `BottomNavDestinationTest` | no two sharing a title — the test tag is derived from it, so a duplicate collapses two tabs onto one tag and every instrumented tap becomes a coin flip |
| `taggedComposable` | `TaggedComposableTest` | the wrapper still applies its tag, still forwards the back-stack entry, and still lets a route's arguments through |

**`taggedComposable` had no test at all.** `CLAUDE.md` requires every destination to be wired with
it and `check_docs.py`'s NAV-04 fails one that is not — so the *usage* was enforced from two
directions while the helper itself was unchecked. If it stopped tagging, the failure would surface
as a wall of red on an emulator, in instrumented tests that are about something else. It is now a
single Robolectric test in `src/testDebug`, which is the only reason this module has a Robolectric
dependency at all.

**Tests that were in the wrong module, again.** `AnnouncementRoutesTest`, `HelpDeepLinkTest`,
`ContentTargetRoutesTest`, `SearchProofNavigationTest` and `WorshipDestinationsTest` all lived in
`:app/src/test` and imported nothing outside `:core:navigation` and `:core:domain`. They moved
wholesale. `EveryRouteIsRegisteredTest` stays in `:app`, because comparing the registered set
against the declared one needs to see every feature's graph.

**What is left uncovered, and why.** `taggedComposable` is `inline`, so its bytecode is emitted
into the *caller's* module — this module's own copy is never executed however many tests compose
it, and it reads as 10 permanently uncovered lines. The other residue is the 94 `@Serializable`
`Route` declarations, which are a vocabulary rather than behaviour. Together they are the
module's ceiling, which is why the floor is the standard 80 rather than the 84 it reports today.

### The Firebase wrappers and the formatters (`:core:common/src/test`)

The fifth module locked, and the one that best shows what a low number can hide: **`AppAnalytics`,
`CrashReporter`, `PerfMonitor` and `FirebaseTelemetry` were at 0% between them**, 229 lines, not
because they are hard to test but because "it only no-ops" reads like there is nothing to assert.

It is the opposite. Every call in those wrappers is guarded so it no-ops when Firebase is not
initialised — which is every build without `google-services.json`, including a fresh clone, a fork
and CI. That guarantee is why the app can log from `BootReceiver`, from a Worker and from
`NimazApp.onCreate` without any call site checking first. A missing `runCatching` is therefore not
a wrong number; it is an exception thrown from a broadcast receiver on a device that has never
opened the app. Nothing else catches it: the calls are fire-and-forget, so there is no return
value to assert on and no caller to notice.

Robolectric is what makes it testable — a real `Context`, a real `Bundle`, and Firebase genuinely
absent. That is the module's only reason for a Robolectric dependency.

| Area | Covered by | What it pins |
|---|---|---|
| Every `AppAnalytics` entry point | `monitoring/AppAnalyticsSafetyTest` | thirty-odd calls that must return rather than throw when Firebase is absent, plus both permission helpers defaulting to *allowed* when they cannot ask |
| The analytics catalogue | `monitoring/AppAnalyticsCatalogTest` | every event, param and user-property name inside Firebase's length limits, snake_case, and distinct — an over-length name is **silently dropped**, so the first symptom is a dashboard reading zero |
| Crashlytics, Performance, and the production `Telemetry` | `monitoring/FirebaseWrapperSafetyTest` | `newTrace` returning null being the ordinary path, `trace {}` still returning its block's value, and a block's own exception propagating rather than the wrapper's |
| Widget clock and countdown | `common/FormattingEdgesTest` | an out-of-range hour clamped rather than thrown — a throw inside a Glance widget is not a crash anyone reports, the widget just stops updating |
| Currency, grouping and date patterns | `common/FormattingEdgesTest` | a code with no symbol on this device rendering as itself rather than "Cayman Islands Dollar (KYD)", and a quoted Spanish pattern keeping its literals |

**What is left uncovered, and why.** `LocaleHelper` (10 lines) switches the process locale through
`resources.updateConfiguration`, which is a global side effect on the test JVM; it is left at zero
rather than made to run in a way that leaks into every test after it.

### The Islamic calendar (`:feature:calendar/src/test`)

The sixth module locked. Its screen was **140 lines at 0%** — the module's whole gap, in one file
— and what the screen holds is an arrangement decision no ViewModel test can see.

| Area | Covered by | What it pins |
|---|---|---|
| The screen's error handling | `screens/calendar/IslamicCalendarScreenTest` | a failed event read reported **above a grid that still draws**, not in place of it |
| The two event sections | same | neither heading appearing with nothing under it, and the upcoming list capped at five |
| The wide layout | same, `@Config(qualifiers = "w1000dp-h1200dp")` | grid and events side by side, and the third arm the compact layout does not have — an empty events column still carries its heading |
| The day markers | same | every `IslamicEventType` reaching an arm of `getEventDotColor`; a type with no arm is an exception on the grid, not a missing dot |
| The Hijri grid, year overview and upcoming list | `viewmodel/calendar/CalendarNavigationTest` | each loaded by its own cancellable job, so a second navigation wins; a hijri month as long as the month actually is |
| Stepping a month | same | the guards that stop a step relative to a grid that does not exist yet |
| The route graph | `screens/calendar/CalendarGraphTest` | both destinations registered — including `Route.IslamicMonth`, which nothing in the app's own UI opens, only an announcement deep link |

**The invariant worth the most** is the first one. `CalendarSection`'s KDoc records that
`loadToday()` used to run inside the events `try`, so a content-database fault left `currentMonth`
null and the screen rendered *nothing*. The fix made the error a section above a grid that still
draws — which only holds if the screen keeps drawing the grid, and that is a rendering fact.
`CalendarNavigationTest` pins the ViewModel half (the state still carries a month); the screen
test pins that the month is actually on screen beside the error.

## Conventions

- **Selectors live in `support/Selectors.kt`.** Tests reference `Selectors.NavLabel.*`,
  `Selectors.<Area>.<key>` (a `@StringRes` id resolved at runtime), or `Selectors.Tag.*`.
  Add new selectors there rather than inlining literals.
- **Entities come from `support/TestData.kt`.** Override only the fields a test asserts on.
- **DAO tests use the in-memory `NimazDbRule`** (hermetic, fast). Asset/migration
  behaviour is covered separately so a schema change fails in exactly one obvious place.
- **UI flows extend `BaseAppTest`** and must carry their own `@HiltAndroidTest`
  (the annotation is not inherited from the base).
