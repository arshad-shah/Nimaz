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
  - [Every preference, read back (`:core:datastore/src/test`)](#every-preference-read-back-coredatastoresrctest)
  - [The first run, drawn and driven (`:feature:onboarding/src/test` and `src/testDebug`)](#the-first-run-drawn-and-driven-featureonboardingsrctest-and-srctestdebug)
  - [The zakat calculator and its history (`:feature:tools/src/test` and `src/testDebug`)](#the-zakat-calculator-and-its-history-featuretoolssrctest-and-srctestdebug)
  - [Search, and the question that leaves the device (`:feature:search/src/test` and `src/testDebug`)](#search-and-the-question-that-leaves-the-device-featuresearchsrctest-and-srctestdebug)
  - [The six widgets, composed for real (`:feature:widget/src/test`)](#the-six-widgets-composed-for-real-featurewidgetsrctest)
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
locked (#597); then `:feature:quran`, `:core:domain`, `:core:navigation`, `:core:common`, `:feature:calendar`, `:core:datastore`, `:feature:onboarding`, `:feature:tools`, `:feature:search` and `:feature:widget`.

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
`:core:navigation`, `:core:common` and `:core:datastore` are locked at 80/80 — none of them draws
anything, so every branch is one somebody wrote and a test can take both sides of. So are `:feature:calendar`,
`:feature:onboarding`, `:feature:tools`, `:feature:search` and `:feature:widget`, which *are* Compose modules: the
unreachable branch below is emitted **per parameter of every restartable composable**, so its
weight scales with how many composables a module has, and six files — or eight, or the two screens
in `:feature:tools`, or the one screen and four cards in `:feature:search` — never accumulate
enough of them to move the number.
`:feature:widget` is Compose of a different kind — six **Glance** widgets — and holds **89.3%**
branches, which settles the question for the widget surface too.
`:feature:onboarding` reports **90.3%** branches, the highest of any Compose module here. Only `:feature:quran` is softened, to
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
| `:core:datastore` | 96.1% | 84.3% | **locked** at 80/80 |
| `:feature:onboarding` | 94.3% | 90.3% | **locked** at 80/80 (#605) |
| `:feature:tools` | 97.5% | 83.6% | **locked** at 80/80 (#606) |
| `:feature:search` | 91.4% | 87.0% | **locked** at 80/80 (#607) |
| `:feature:widget` | 97.6% | 89.3% | **locked** at 80/80 (#608) |
| `:core:ui` | 50.3% | 47.8% | |
| `:core:data` | 39.0% | 31.2% | |
| `:feature:prayer` | 38.1% | 23.7% | |
| `:feature:tracker` | 30.4% | 24.4% | |
| `:feature:content` | 28.8% | 19.5% | |
| `:feature:about` | 17.8% | 23.0% | |
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

### Every preference, read back (`:core:datastore/src/test`)

The seventh module locked. `PreferencesDataStore` was **329 lines at 0%**, for a structural reason
worth remembering: **no screen constructs it** — they read through a `SettingsSeams` interface, by
design, and `CLAUDE.md` requires it — so nothing in a test did either. "Nothing constructs it" is a
design success and a coverage blind spot at the same time.

What that left uncovered is a specific bug. `PreferenceKeyGoldenTest` pins the key *strings*, so a
renamed key fails loudly. Nothing pinned **which key a getter reads**: `showTranslation` returning
`SHOW_TRANSLITERATION` compiles, persists, round-trips, and passes the golden — and the setting the
reader toggles changes a different one. With 96 getters declared in blocks of near-identical lines,
that is the failure the round-trip table is for.

| Area | Covered by | What it pins |
|---|---|---|
| All 74 plain preferences | `PreferencesDataStoreTest` | the documented default on a fresh install, the written value coming back, and — the cross-wiring check — **nothing else moving** when one is written |
| The Hijri day offset | same | the one setter that does not store what it is given: clamped to the two days either side that a moon-sighting difference can actually be |
| The three per-prayer families | `PreferencesDataStoreKeyedTest` | isolation. A `when (prayer.lowercase())` over six arms, written three times; a crossed arm is invisible in the settings screen *and* in the key golden, because both keys exist and both are spelled right |
| Sunrise | same | a notification that defaults off, and an adhan that cannot be turned on at all — there is no key for it, and the reader answers false outright |
| Alert style and pre-reminders | same | every style round-tripping, and one prayer's choice not reaching another's |
| Export / import | same | a payload round-tripping across all five type families, and a key from a newer build skipped rather than guessed at — DataStore keys are typed, so guessing is how an import ends in a `ClassCastException` on read |
| The one-shot migration | same | it runs once, and **never resets a choice the reader has since made** |
| The announcement store | `AnnouncementLocalDataSourceTest` | an FCM payload surviving JSON and a later build; an unknown type resolving to no banner rather than a half-built one; an unknown *occasion* falling back to the generic treatment rather than costing the banner; dismissal being permanent |
| The AI install id | `DeviceIdProviderTest` | stable across process restarts, and a generated UUID — **never** a hardware identifier |

### The first run, drawn and driven (`:feature:onboarding/src/test` and `src/testDebug`)

The eighth module locked, and the one with the furthest to travel: **12.9% lines to 94.3%**, from
97 covered lines to 711. Two files were 600 of the 657 that were missing, and both were missing for
the same reason — nothing had ever composed the screen, and nothing had ever *drawn* it.

The stakes are unlike any other screen's. `NavGraph` reads `onboardingCompleted` **once**, when it
builds the graph, to choose a start destination. A walkthrough that fails to emit
`CompleteOnboarding` does not cost the user a screen; it puts them back in onboarding on every
launch, for ever. The two halves of finishing — the event and the navigation — are dispatched from
two different buttons on two different pages, each behind a page comparison, and no ViewModel test
can see whether the right button is on the right page.

**The art is covered by drawing it, not by declaring it uncoverable.** #605 flagged
`OnboardingArt.kt` (226 lines of `Canvas` geometry) as possibly unreachable, and left at zero the
module could not have passed 80% at all — 528 coverable lines out of 754 is 70% with everything
else perfect. Composing the tree is not enough: `Canvas(modifier)` runs, its `DrawScope` lambda —
which is the whole file — does not. `OnboardingArtTest` asks the `ComposeView` to draw itself into
a software `android.graphics.Canvas` under `@GraphicsMode(NATIVE)`, which makes Compose's
`RenderNodeLayer` invoke the draw block directly instead of replaying a render node, and reads the
pixels back. `captureToImage()` is the route it does **not** take — that goes through `PixelCopy`
on a real window and hangs under Robolectric.

| Area | Covered by | What it pins |
|---|---|---|
| The way out of onboarding | `OnboardingScreenTest` | `CompleteOnboarding` and the navigation fire together, **once**, and only from the last page or Skip; Skip is gone from the last page, so there are never two buttons that both end the flow |
| Paging | same | each page carries its own copy, Back returns to the previous one, and the last page swaps Next for Get Started |
| The funnel | same | every page reached is reported in order — the analytic that fired zero times in production while the pager drove itself locally |
| The permission cards | same | a granted permission says so and stops asking; a detected location is **named** rather than shown the generic granted label; a permission granted while the page is open updates in place |
| The notification permission, both platforms | same | it asks the system from Tiramisu, and grants itself below it — where there is no `POST_NOTIFICATIONS` to request and a launcher would leave the card stuck |
| The location dialog's answer | same | **coarse alone counts as granted**: an AND across the two results would tell a user who picked "approximate" that they had refused |
| Short screens | same | both pages switch to a compact layout below 500dp of usable height, and the way forward stays on screen |
| The four emblems | `OnboardingArtTest` | each paints something, and each paints *its own* artwork — a `when` arm that fell through, or the shield's lost `return@Canvas`, ships the wrong drawing and compiles fine |
| The mihrab's geometry | same | letterboxed rather than stretched when its box is the wrong shape, and centred in it — the `min(w/116, h/150)` scale and the halved offsets |
| The khatam band | same | it tiles the full measured width, and fades downwards instead of reading as a solid gold bar over the title |
| The illuminated field | same | three gradient stops, not a flat teal fill |
| The first-run flag | `OnboardingViewModelTest` | read back at construction; a read that fails **ends the loading state** and does not guess `true`, which would skip the first run outright |
| A completion that cannot be persisted | same | reported as a failure, and **not** counted in the funnel — a completion the user will be shown again next launch makes the first-run denominator disagree with the step funnel above it |
| Permission results | same | each updates only the field it answered for; granting location detects a location without waiting to be asked again |
| The graph | `OnboardingGraphTest` | `Route.Onboarding` registers, and the graph builds with it as the start destination — the failure here is thrown on the first frame of the first launch |

### The zakat calculator and its history (`:feature:tools/src/test` and `src/testDebug`)

The ninth module locked: **18.0% lines to 97.5%**, from 181 covered lines to 981, and 29.3% to
83.6% branches. Two screens were 727 of the 825 that were missing. The ViewModel was already the
best-covered part, so nothing here was a matter of finding tests filed in the wrong module — the
gap was that nothing had ever composed either screen.

Zakat is the one feature in the app where a wrong number has a religious and financial consequence,
and **the arithmetic was never the exposed part**. `ZakatCalculator` has had its own tests in
`:core:domain` since it was written, and neither screen does a sum. What had no coverage at all was
everything between the user and that calculation: which asset a typed figure is filed against,
whether the symbol on the fields is the one beside the total, whether the threshold that decides
"you owe this" from "you owe nothing" is even reported on the form, and whether a write that failed
says so. The calculator is thirteen near-identical `InputCard` call sites; a copy-paste between any
two of them takes input in the right box, files it against the wrong asset, and looks correct.

Two component contracts shaped the tests rather than the other way round. `ZakatSummaryHero` puts
`clearAndSetSemantics` over its plinth and over each stat tile, so that TalkBack reads "Zakat Due,
$450.00, Above nisab" as one phrase instead of three fragments — which means the hero is
addressable *only* by that content description, and the assertions here are on the accessibility
contract itself. And the hero's collapse-on-scroll can only be exercised on a phone-height
`@Config`: the rest of the class runs at `w411dp-h2200dp` so the `LazyColumn` composes every row,
and on a viewport that tall the whole form fits and there is nothing to scroll.

| Area | Covered by | What it pins |
|---|---|---|
| Every asset row | `ZakatCalculatorScreenTest` | each of the nine files its figure against **its own** asset — distinct values per row, so any two crossed shows |
| Every liability row | same | the same for the four deducted rows, and that none of them lands on the asset side — the sign error that doubles someone's zakat |
| Weight versus money | same | gold and silver take **grams**; the money rows lead with the currency symbol and the weight rows follow with `g`. The field these replaced chose between the two by comparing its suffix against `"$"`, so every non-dollar currency rendered a dollar sign |
| The chosen currency | same | the symbol on the fields and the figure beside the total come from the same code, and no dollar total survives on a euro form |
| The nisab basis | same | reported on the form before anything is typed, follows a basis change, and opens the settings screen rather than being editable mid-entry — a reader who cannot see the threshold cannot tell why the total says zero |
| The verdict | same | above the nisab the hero says so and states the amount; below it, the *other* verdict is asserted absent — "Above nisab" over a zero reads as a calculation that failed rather than an answer |
| The hero's tiles | same | the net wealth and the threshold it was measured against, without which the headline figure has to be taken on trust |
| The collapsing hero | same | collapses on scroll, re-expands **only at a true top**, and never takes the amount with it. The asymmetry is the fix: the hero sits above the list and the list takes the height the hero leaves, so one symmetric threshold is a loop that presented as a hero which never collapsed |
| The breakdown | same | shows the working, renders liabilities as a subtraction, keeps its header when collapsed, and is absent entirely over an empty form — the defect where clearing the last field left a zakat figure over a blank form |
| A failed calculation | same | reported **inline**, with every typed figure still on the form, and its retry re-runs the sum rather than asking the user to retype anything |
| The action bar | same | save and share are disabled until there is a calculation — an enabled share over a null one is what produces a card of zeroes — and enabled together once there is |
| The top bar | same | reset goes through the ViewModel; history and settings **navigate**, keeping the destination decision out of the ViewModel |
| The wide layout | same | assets and liabilities side by side with no accordion to open, the same action bar, the same basis row, and input still reaching the right event |
| The three history states, in order | `ZakatHistoryScreenTest` | the error branch is checked **before** the empty one: a failed read also leaves the list empty, so the other order tells someone with years of records that they have none |
| A saved calculation | same | renders its own stored figures and its own basis — recomputing would silently restate last year's zakat at today's gold price |
| Paid versus unpaid | same | the badge tracks `isPaid`, "mark as paid" is **not** offered on something already paid, the paid date comes from `paidAt` rather than `calculatedAt`, and an entry with no recorded date does not invent one |
| Which entry was acted on | same | mark-paid and delete carry the id of the row that was tapped, not of the first row |
| The list's identity | same | one card per recorded calculation — a key collision collapses several years into one, and the only symptom is a history shorter than it should be |
| Saving | `ZakatPersistenceTest` | writes the calculation **on screen** rather than recomputing at save time, which would re-read the metal prices and file a figure the user never saw; an empty form writes nothing |
| A write that fails | same | reported on the screen the user is actually on — a save failure never touches the history state and never clears the form, a mark-paid or delete failure never touches the calculator |
| Marking paid | same | stamps the time of payment, not the time of calculation — for zakat those can be a lunar year apart |
| Reading the history back | same | the running total is the repository's paid total, not a client-side sum over the list, which would report the whole history as paid |
| A history stream that fails first | same | **ends the spinner** and sets an error: `isLoading` used to be cleared only inside the collect, so a missing table left the screen spinning for the life of the ViewModel |
| The form's lifecycle | same | the thirteen typed figures survive process death and recompute on restore; clearing reaches the saved state too; a reset keeps the persisted basis and prices, which decide whether anything is owed at all |
| The graph | `ToolsGraphTest` | both zakat destinations register and either can be a start destination — the two navigate to each other, so a missing registration is a crash on a tap the user made deliberately |

**Nothing is excluded, and the twenty-five uncovered lines are recorded in the module's build
file**: the lambda bodies inside `toolsGraph` (reachable only from a composed `NavHost`, which
would need Hilt ViewModels for both screens), each screen's `hiltViewModel()` default argument, and
`ZakatViewModel.calculate`'s `catch` — arithmetic over `Double`s with no user-supplied divisor, so
no input reaches it. `ZakatPersistenceTest` covers the identical handler shape on all four write
paths, where failures do happen.

### Search, and the question that leaves the device (`:feature:search/src/test` and `src/testDebug`)

The tenth module locked: **18.8% lines to 91.4%**, from 233 covered lines to 1,130, and 13.9% to
**87.0%** branches. 130 tests, 111 of them new. Both ViewModels were already reasonably covered —
the two screen files were 843 of the 1,004 lines that were missing, and nothing had ever composed
either of them.

**This is the app's only surface where a bug sends user text off the device.** "Ask with Proof" is
opt-in, and until #607 that promise was pinned in two places a user never sees: `AiOptInDefaultsTest`
(in `:core:datastore`) on the stored default, and `AskWithProofConsentTest` (in `:core:domain`) on
the use case's refusal. Neither says anything about the screen, and the screen is where consent is
offered — or bypassed. The assertion that the Ask affordance is *absent*, not merely disabled,
while the feature is off is a privacy assertion, not a layout one.

The other half is the ordinary keyword search, where the load-bearing property is that three
different situations must not look alike. A search in flight, a search that found nothing and a
search that failed all render a list with nothing in it, and no ViewModel test can tell them apart:
the difference is entirely which branch the screen takes and in what order. "No results for X" over
a lookup that never ran reads as a fact about the reader's library rather than as a failure.

| Area | Covered by | What it pins |
|---|---|---|
| A search still running | `SearchScreenTest` | shows progress and **never** the no-results sentence — the screen half of the contract the ViewModel keeps by flipping `isSearching` synchronously |
| A search that found nothing | same | names the query it found nothing for; "No results" alone reads as an empty library |
| A search that failed | same | the failure is reported **before** the no-results branch, with the typed query still in the bar and a retry that re-runs the search — the ordering is what stops a failed lookup being reported as an empty library |
| Every result row | same | each of the five kinds dispatches its own callback with its own identifiers: the verse, the surah (not a verse inside it), the hadith's **book then record id** in that order, the dua, and the name **with its catalogue** — three catalogues share one id space each, so a dropped catalogue opens the wrong record silently |
| Name catalogue labels | same | each of the three is tagged distinctly, and a name with no transliteration still has a title |
| The filter chips | same | appear only once there is something to scope, count from the *unfiltered* results with the same predicate the list filters by (a surah hit counts under Qur'an), show a bare label at zero, and dispatch the filter that was tapped |
| An initially scoped search | same | opening search from Duas scopes it on first composition; opening it plainly scopes nothing |
| The resting screen | same | recent searches are offered, tapping one runs it, removing one is not wired to clear-all, and no header is drawn over an empty history |
| The consent boundary | `SearchScreenAskTest` | with AI off there is **no control that sends a question anywhere**, on Global Search or on any scoped search — even with the feature switched on in settings, a scoped search offers nothing to ask |
| The opt-in offer | same | states what leaves the device, sends "turn it on" to Search settings rather than flipping the switch itself, remembers a decline, and stays out of the way mid-search |
| One bar, two jobs | same | what is typed reaches the question as well as the search, so Ask sends what is on screen; emptying the bar clears the answer with it |
| The answer | same | its text, its confidence and the note saying it is not a ruling — the trust note is the only thing on screen that says so |
| Cited sources | same | marked "Cited", and each opens the record it cites (`ContentTarget.Ayah` / `.Hadith`, resolved to a route in `:core:navigation`) |
| The dedup | same | a verse the AI cited and the keyword search also matched appears **once** — twice would read as two independent sources agreeing. A surah or a name has no citation form and therefore survives beside the cited rows |
| Filtering an answer | same | narrowing to Qur'an keeps the cited verses, to Hadith the cited hadiths, and to Duas or Names hides the cited strip rather than filtering it to nothing |
| The list under an answer | same | the keyword rows stay on screen while the related-terms lookup runs, and the count line is held back until it lands; rows render with no query to highlight, which is the state the `indexOf("")` guard exists for |
| Failures on the AI side | same | a retry is offered for a dropped connection and nothing else; keyword results carry on under a failed ask, because the library is local |
| The AI's related terms | same | drive the local search on Global Search, and are ignored entirely on a search that never asked |
| Every error card | `AskComponentsTest` | seven `AiError` cases, each with its own copy, and retry offered **only** where retrying can help — a retry beside a daily cap invites taps against a limit only time lifts, and on the retryable ones each tap is one billed Worker call |
| "You can ask again in …" | same | seconds round **up** into minutes and minutes into hours; rounding down promises a limit that has not lifted. No retry-after means no invented number |
| A failed lookup, in the ViewModel | `SearchSessionTest` | surfaces as an error with the resource message and the exception text kept for a bug report — and the next search clears it, so no error banner is left standing over fresh results |
| A superseded search | same | a slow first lookup landing after a faster second one is discarded — results for a query the box no longer holds |
| The recent list | same | written on submit and never on a keystroke, de-duplicated to the top, capped at ten, removable one at a time, and **kept** when the query is cleared |
| Telemetry | same, `AskHistoryTest` | the query length and filter are recorded, the words never are; an empty submit counts as no search at all |
| Every stored default scope | `SearchSessionTest` | each `LibrarySource` opens search on its own chip, and a scope this build does not recognise falls back to everything rather than crashing |
| The filter predicate | same | every branch, including `SurahResult` under QURAN — the case the deleted per-corpus counters got wrong in both directions |
| The question history | `AskHistoryTest` | written only while "remember my questions" is on — the switch gates the **write**, not the display — de-duplicated, capped at ten, and unreadable stored JSON degrades to no history rather than throwing while the resting screen composes |
| Every failure slug | same | seven distinct names, so a budget cap and a failing integrity check are not one undifferentiated error rate; a thrown failure still takes the screen out of Loading |
| The graph | `SearchGraphTest` | all four destinations register and each resolves as a start destination in its own right — every one is reached directly, three from a section screen and Global Search from home |

**Nothing is excluded, and the 107 uncovered lines are recorded in the module's build file.**
Ninety of them are the four destination bodies inside `searchGraph`: each calls `SearchScreen(...)`
with seven navigation lambdas, and none of it runs until a composed `NavHost` reaches the
destination — which builds both ViewModels through `hiltViewModel()` and so needs a Hilt-injected
activity this module cannot construct. `SearchGraphTest` covers the registration those bodies hang
off, which is the failure that actually ships. The remainder is the two `hiltViewModel()` default
arguments, the `null ->` arm of `LibrarySource?.asFilter()` (unreachable — its only call site is
already inside a `defaultScope != null` check), and a few `when`-merge lines with no statement of
their own.


### The six widgets, composed for real (`:feature:widget/src/test`)

The eleventh module locked: **16.8% lines to 97.6%**, from 268 covered lines to 1,557, and 28.1%
to **89.3%** branches. 96 tests, 82 of them new. The six Glance widget files alone were 764 of the
1,327 missing lines, and nothing had ever composed one.

**Read this before adding a widget test: Glance is not ordinary Compose, and the campaign's usual
pattern does not reach it.** A Glance composable renders to `RemoteViews`, not to a semantics
tree, so `createComponentComposeRule()` / `onNodeWithText` finds nothing. What was tried, and what
came of it:

- **`GlanceAppWidget.compose(context, state = …)` — this is the answer.** It runs the widget's
  real `provideGlance` against a state you hand it and returns the `RemoteViews` the launcher
  would be given; `remoteViews.apply(context, null)` inflates those under Robolectric into an
  ordinary Android view tree, and the text a reader would see is on its `TextView`s. No DataStore
  file is written, so the tests stay hermetic. `WidgetRenderer` / `RenderedWidget` in the test
  source set wrap it, and every widget test in the module goes through them. This covers
  `provideGlance`, the `stateDefinition`, the state `when` and every **private** content
  composable behind it — none of which needs to be made visible.
- **`androidx.glance:glance-appwidget-testing` — assertion-only, and not used here.** Its
  `runGlanceAppWidgetUnitTest` DSL exposes `provideComposable`/`setState` and a
  `GlanceNodeAssertion` with `assertExists`, `assertDoesNotExist` and `assert`. There is **no
  `performClick`**, so a Glance `clickable { }` cannot be fired from a JVM test — the lambda
  action is resolved by the AppWidget host. It also cannot reach a private composable, which
  `compose()` can. The module does not depend on it.
- **The consequence.** `togglePrayerStatus` — the one widget action that writes user data — is
  `internal` rather than `private` and is called directly by `TogglePrayerStatusTest`. The tile
  that invokes it is covered by the render test. That visibility change is the only production
  edit in the module.

**Why any of this is worth testing: a throw inside a widget is not a crash anyone reports.** The
launcher keeps drawing the last frame, so the failure looks like prayer times that quietly stopped
being right. Every render test below is a guard against exactly that silence.

| Area | Covered by | What it pins |
|---|---|---|
| The Hijri date, in every state | `HijriDateWidgetRenderTest` | day, month-with-year and both gregorian lines; an empty payload — the frame every install shows before its first worker run — falls back to em dashes rather than blank lines |
| Which prayer is "next" | `NextPrayerWidgetRenderTest` | chosen from the wall clock at render time, **not** from what the worker stored: a payload whose stored answer has already passed names the following prayer instead. This is the whole reason the widget persists a schedule |
| State from an older release | same | a payload with no `schedule` still renders, off the flat fields; tomorrow's first prayer is captioned rather than given a clock time; an invalid payload drops the "in " prefix instead of writing "in —" |
| The five-prayer strip | `PrayerTimesWidgetRenderTest` | all five times and the **short** names — Maghrib is `Mgrb`, not a truncation — and the header naming the next prayer beside the Hijri date. After Isha the countdown clause is dropped, and with no Hijri date there is no dangling separator |
| The tick-boxes | `PrayerTrackerWidgetRenderTest` | exactly one check vector is drawn per prayed prayer, counted against the unprayed baseline — the tiles carry no text that tells the two branches apart, and a tile that renders the wrong one tells a reader they have not prayed something they have |
| The khatam card's two layouts | `KhatamWidgetRenderTest` | an active khatam draws name, juz medallion and percentage; no active khatam draws the start prompt instead. The pace line joins target and streak, shows either alone with no separator, and falls back to the juz position when there is neither; the ayah count is pluralised |
| The month grid | `HijriCalendarWidgetRenderTest` | every day of the month appears and nothing past it, a month spilling into a sixth week keeps its last days, today appears twice (grid disc and right rail), and the weekday strip is Sunday-first and **localised** — it was a hardcoded English `listOf("Su", …)` |
| The events rail | same | events are listed with their type spelled as a phrase rather than `RELIGIOUS_OBSERVANCE`; a month with none says so; a fast *or* a recommended observance earns the star icon, and the two conditions are independent |
| Loading and error frames | all six render tests | each state draws its own frame and none of the data — including the two widgets whose error frame carries a second retry line |
| `hasData`, per widget | all six render tests | which payloads are worth keeping on screen through a failed refresh. Every widget's default state is `Success` with an empty payload, so "is it Success" is not the question — and getting this wrong wipes a correct widget on one transient throw |
| The refresh body all six workers share | `RefreshWidgetTest` | a widget on no home screen succeeds **without loading**; a success publishes to every placed instance; a failure over real data redraws and keeps it; a failure over nothing publishes the error frame carrying the throw's own message; retries stop after the third attempt; an unreachable AppWidget host retries instead of failing outright; and the failure handler failing is swallowed |
| Each worker's own wiring | `WidgetWorkerRefreshTest` | every worker publishes **its own** state type loaded from **its own** data source — six near-identical bodies is the shape a copy-paste mistake hides in, and one pointed at another widget's state definition would compile and then overwrite the wrong widget |
| Refresh scheduling | `WidgetWorkSchedulingTest` | each widget enqueues periodic and one-shot work under its own unique names; re-arming without force keeps the schedule already running (`onUpdate` fires on every boot and package update, so it must be idempotent); forcing replaces it; cancelling clears both |
| Receiver lifecycle | `WidgetWorkReceiverTest` | `onEnabled` forces and refreshes, `onUpdate` re-arms **without** forcing and still refreshes, `onDisabled` cancels — and the two optional hooks stay optional. `onUpdate` is the recovery channel for a schedule the app lost: a force-stop drops WorkManager jobs and alarms never survive a reboot |
| Which receiver drives what | `WidgetReceiverWiringTest` | each of the six arms its own worker and cancels its own; **only** the two countdown widgets arm the per-minute alarm, and removing one of them while the other is placed leaves the shared tick running |
| The per-minute tick | `WidgetUpdateSchedulerTest` | arming is idempotent, `cancelIfUnused` stops the tick only once no countdown widget is left, `ensureScheduled` arms nothing when none is placed, and `computeCountdown` returns an em dash for an unset target rather than a negative duration |
| The tick's two redraws | `WidgetTickReceiverTest` | both countdown widgets are redrawn, one failing does not cost the other its redraw, and both failing does not take the receiver down |
| Widget state on disk | `JsonGlanceStateDefinitionTest` | a round-trip, one store per file name process-wide, and the two forward-compatibility guards: a field written by a newer release is ignored, and a **truncated** file falls back to the default and stays usable. Without those, the first release to rename a field left every widget stuck on its error frame permanently — only clearing app data brought it back |
| The one write | `TogglePrayerStatusTest` | unprayed flips to prayed with a timestamp and back again without one; the first tap of a day inserts rather than doing nothing; the tile is refreshed after the write; and a repository failure is reported rather than crashing the **launcher**, which is the process a widget tap runs in |

**Nothing is excluded, and the 38 uncovered lines are recorded in the module's build file.** Most
are the `$dirty` bitmask branch the Compose compiler emits per composable parameter, which no test
can take both sides of; the rest are a handful of `when`-merge lines with no statement of their
own. No `COVERAGE_EXCLUSIONS` entry was added or widened.


## Conventions

- **Selectors live in `support/Selectors.kt`.** Tests reference `Selectors.NavLabel.*`,
  `Selectors.<Area>.<key>` (a `@StringRes` id resolved at runtime), or `Selectors.Tag.*`.
  Add new selectors there rather than inlining literals.
- **Entities come from `support/TestData.kt`.** Override only the fields a test asserts on.
- **DAO tests use the in-memory `NimazDbRule`** (hermetic, fast). Asset/migration
  behaviour is covered separately so a schema change fails in exactly one obvious place.
- **UI flows extend `BaseAppTest`** and must carry their own `@HiltAndroidTest`
  (the annotation is not inherited from the base).
