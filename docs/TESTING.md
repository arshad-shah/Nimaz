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
  - [Prayer times, the month table and the qibla (`:feature:prayer/src/test` and `src/testDebug`)](#prayer-times-the-month-table-and-the-qibla-featureprayersrctest-and-srctestdebug)
  - [About, Help and the More menu (`:feature:about/src/test`)](#about-help-and-the-more-menu-featureaboutsrctest)
  - [The repositories, the sync and the adhan store (`:core:data/src/test`)](#the-repositories-the-sync-and-the-adhan-store-coredatasrctest)
  - [The library, rendered (`:feature:content/src/test`)](#the-library-rendered-featurecontentsrctest)
  - [What the user did (`:feature:tracker/src/test` and `src/testDebug`)](#what-the-user-did-featuretrackersrctest-and-srctestdebug)
  - [The design system itself (`:core:ui/src/test` and `src/testDebug`)](#the-design-system-itself-coreuisrctest-and-srctestdebug)
  - [Every preference the app can change (`:feature:settings/src/test` and `src/testDebug`)](#every-preference-the-app-can-change-featuresettingssrctest-and-srctestdebug)
  - [What is left in `:app` (`app/src/test` and `src/testDebug`)](#what-is-left-in-app-appsrctest-and-srctestdebug)
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
locked (#597); then `:feature:quran`, `:core:domain`, `:core:navigation`, `:core:common`, `:feature:calendar`, `:core:datastore`, `:feature:onboarding`, `:feature:tools`, `:feature:search`, `:feature:widget`, `:feature:prayer`, `:feature:about` and `:core:data`.

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

**The line floor is 80% everywhere; the branch floor is not, and in one module there is none.** `:core:database`, `:core:domain`,
`:core:navigation`, `:core:common`, `:core:datastore` and `:core:data` are locked at 80/80 — none of
them draws anything, so every branch is one somebody wrote and a test can take both sides of. So are `:feature:calendar`,
`:feature:onboarding`, `:feature:tools`, `:feature:search`, `:feature:widget`, `:feature:about`,
`:feature:content`, `:feature:tracker` and `:feature:settings`, which *are* Compose modules: the
unreachable branch below is emitted **per parameter of every restartable composable**, so its
weight scales with how many composables a module has, and six files — or eight, or the two screens
in `:feature:tools`, or the one screen and four cards in `:feature:search`, or the seven screens in
`:feature:about` — never accumulate enough of them to move the number.
`:feature:widget` is Compose of a different kind — six **Glance** widgets — and holds **89.3%**
branches, which settles the question for the widget surface too.
`:feature:onboarding` reports **90.3%** branches, the highest of any Compose module here.
`:feature:content` is the **lowest** to hold the standard, and the closest any module has come to
missing it: **80.4%**, four branches above the gate. It is also the largest Compose surface locked
so far — nineteen screens across five corpora — which is the point: even there, the `$dirty`
branches below never accumulate enough to force the softened floor. What they do is eat the
margin, and a module that reports 80.4% is one unrelated refactor from a red gate. That is the
ratchet working, not a reason to soften it.
`:feature:tracker` is the **eighth** Compose module to hold the standard, at **81.3%** across
three trackers, twelve screens and two drawn surfaces — with **131 of its 280 missing branches**
being that same bitmask, and 89.1% once they are discounted.
`:core:ui` is the **ninth**, and settles the question for the largest Compose surface in the repo:
13,000 measurable lines, 316 classes and 143 files of design system, holding **80.5% lines and
80.5% branches**. It is also the *thinnest* margin of any module — 61 lines and 24 branches — and
the reason is arithmetic rather than thoroughness: **1,979 of its lines are `@Preview` and
`*Showcase` functions**, 15.2% of the module, permanently at 0% because they are `private` tooling
that would pin the previews rather than the product. Counting them, everything else has to clear
**94.4%**. See [the design system's audit section](#the-design-system-itself-coreuisrctest-and-srctestdebug).
`:feature:settings` is the **tenth** and the last module in the repo to join the ratchet, at
**94.2% lines and 82.6% branches** across 24 screens, a 1,400-line `SettingsViewModel`, sync and
location. **141 of its 279 missing branches sit on composable signature and parameter lines** —
the `$dirty` skippability check and the `$default` parameter mask together — and discounting them
it stands at **89.9%**. Its line margin is the widest of any locked module and its branch margin
is 41; what bought both was that the module's *screens* had never been composed at all, so the
work was breadth rather than the last twenty branches.
**With this one locked, every module in the repo is on the ratchet.**

**Two modules are softened to 80 line / 60 branch, and for two different reasons.**
`:feature:prayer` is the second, and its reason is arithmetic rather than Compose:
`PrayerTimesPdfExporter` cannot execute under Robolectric at all, and it carries **103 of the
224 branches the module still misses**. Discount that one file and the module stands at 81.6%
branches, over the standard; count it — which the gate does, because nothing is excluded to
reach these numbers — and 80% is out of reach whatever else is tested. The remaining 121 are
spread over six Compose screens and twelve components, where the `$dirty` bitmask below takes
its usual share. `:feature:quran` is the first, softened to
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
| `:feature:prayer` | 84.1% | 70.5% | **locked** at 80 line / 60 branch (#609) |
| `:feature:about` | 94.1% | 83.8% | **locked** at 80/80 (#610) |
| `:core:data` | 88.1% | 84.6% | **locked** at 80/80 (#611) |
| `:core:ui` | 80.5% | 80.5% | **locked** at 80/80 (#614) |
| `:feature:tracker` | 91.9% | 81.3% | **locked** at 80/80 (#613) |
| `:feature:content` | 85.1% | 80.4% | **locked** at 80/80 (#612) |
| `:feature:settings` | 94.2% | 82.6% | **locked** at 80/80 (#615) |
| `:app` | 82.6% | 53.5% | **locked** at 80 line, **no branch floor** (#630) |

**`:app` is the one module gated on lines only, and that is the finding rather than an
omission.** It was outside the campaign because its own suite needs the private content artifact:
`:app:testDebugUnitTest` depends on `mergeDebugAssets`, which `orderAssetConsumersAfter` makes
depend on `fetchNimazData`. `-x fetchNimazData` is the local workaround — no JVM unit test reads
the 180 MB content database, and the one that did (`DeviceStateCorpusTest`) is the only casualty.
CI has the token and runs the whole thing.

Its branch number cannot support either of the two floors this campaign sanctions. Of 1,790
missing branches, **786 are composable signature and parameter masks** — `:app`'s composables are
unusually wide (`HomeScreen` takes 17 parameters and hands 19 to `HomeCompactContent`), so 44% of
the module's branches are mask against 17% in `:core:ui`. Cover every branch outside them — every
`when`, every null-check, the whole composition root, the whole player — and the module reads
**76.1%**, so 80 is arithmetically impossible. Discount the 144 that sit inside `@Preview` bodies
as well and the reachable ceiling is about **62.6%**, which would leave a 0.60 gate with two
points of headroom for the next composable anyone adds. A floor that tight measures how many
parameters the screens happen to have. The number is therefore recorded, not gated, and
`app/build.gradle.kts` carries the same arithmetic where the floor is set.

**Adding `:app` to the ratchet changes what root `./gradlew coverageFloor` needs.** It now
includes `:app`, and therefore the content-repo credential. Without one, run it as
`./gradlew coverageFloor -x fetchNimazData`.

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


### Prayer times, the month table and the qibla (`:feature:prayer/src/test` and `src/testDebug`)

The twelfth module locked: **38.1% lines to 84.1%**, from 1,086 covered lines to 2,398, and 23.7%
to **70.5%** branches. 108 tests added across fourteen new classes, taking the module's suite from
62 to 170. Three screens — the month timetable, the day pager and the qibla compass — were at
**0%** between them, 855 lines of the 1,767 missing, and nothing had ever composed one.

**This is the second module locked at a softened branch floor, and the first where the reason is
not Compose.** `PrayerTimesPdfExporter` cannot run under Robolectric — `PdfDocument` throws
`"document is closed!"` on the first `startPage` — and it holds 296 lines and 116 branches, 103 of
them still missing. `:feature:quran` settled the same problem the same way for `TafseerPdfExporter`
in #598: leave it at zero, make the 80% up elsewhere, record it. Nothing is excluded to reach these
numbers; the file is counted in full against both floors.

**Three things here are worth knowing before adding a test to this module.**

- **The PDF export path is still testable — the failure arm is.** `MonthlyPrayerTimesScreen`
  renders a month's PDF *inside a click handler* and reports start-then-outcome through the
  ViewModel. Under Robolectric the render always fails, which makes this the one place the
  `onFailure` arm runs for real: the assertion is that a month whose PDF cannot be produced still
  reports, still leaves the timetable on screen, and never throws out of composition.
- **`ArQiblaView` is the app's only camera surface, and CameraX does not clean up after itself.**
  `ProcessCameraProvider.getInstance()` cannot complete with no camera: it leaves a pending
  listener on the main looper and a half-initialised provider behind it, and Robolectric runs a
  module's classes in one JVM, so that state lands on whichever class launches an activity next —
  as `FutureGarbageCollectedException: CameraX initInternal` raised from an unrelated screen's
  test, or as `lightZ must be a finite positive, given=Infinity` out of `ThreadedRenderer.setup`
  before that class runs a line of its own. Neither names CameraX; both depend on class order and
  on when a GC happens to run. **This module therefore forks a JVM per test class**
  (`unitTests.all { it.setForkEvery(1) }`), which is the only thing that actually contains it, at
  a cost of about three minutes. No other module needs it, and none of them has a camera
  dependency. Every activity-launching class here also pins its **density** in `@Config`, because
  `lightZ` is a theme dimension resolved against `DisplayMetrics.density`.
- **The AR overlay's geometry is covered by drawing it, per the campaign's canvas technique.**
  `QiblaArtTest` asks the `ComposeView` to draw into a software `android.graphics.Canvas` under
  `@GraphicsMode(NATIVE)` and reads the pixels back; composing the tree alone runs `Canvas(modifier)`
  and none of its `DrawScope` lambda. Not `captureToImage()`, which hangs.

| Area | Covered by | What it pins |
|---|---|---|
| The month header against the month's rows | `MonthlyPrayerTimesScreenTest` | header and grid are two renderings of one fact drawn from different places — `currentMonth` and each row's own date. `:feature:calendar` shipped exactly this bug; a header naming the wrong month over a correct timetable reads as the prayer times being wrong |
| One row per day, and the right day expanded | same | a row dispatches **its own** date, and only the expanded row draws the six times — a card ignoring `expandedDay` opens every row at once |
| What the header calls the place | same | `isUsingFallbackLocation` wins over any name still in state, and a blank name reads as "Not set". `FallbackLocation` exists so a surface can say it is using a stand-in rather than caption a timetable with a city the reader has never been to |
| The export chooser | same | offered only once there is a month to export, counts the days it would produce, and offers the Ramadan row only in Ramadan |
| A PDF that cannot be rendered | same | start is reported, then exactly one outcome, and the timetable stays on screen. A silent failure here is a share button that does nothing |
| The Ramadan timetable | same | the row *asks* — a month of astronomy belongs off the click handler — and the result arriving in state is shared once and then acknowledged, or a recomposition re-opens the share sheet |
| What a single day row claims | `MonthlyPrayerTimesEventRowTest` | "today" compared against the system date inside the row; the highest-priority Islamic event matched by Hijri date; the fast length shown in Ramadan and nowhere else. Dates are derived from `HijriDateCalculator`, not hardcoded, so nothing starts failing the year the Hijri calendar moves past it |
| A day with no computable times | `MonthlyPrayerTimesScreenTest` | six `--:--` cells rather than six blanks, and the row still present. At high latitudes this is a real day, not a rendering bug |
| Which day the pager is on, and how it says so | `PrayerTimesScreenTest` | the relative label, the "Today" chip that renders only `if (!isToday)`, and the arrows. A screen that loses the chip strands a reader on a day they browsed to |
| Tracking toggles on a future day | same | withheld — five toggles today (sunrise is not a prayer), none on a day that has not happened |
| Swiping between days | same | the gesture, not just the arrows: a drag threshold read with the wrong sign pages backwards, which is indistinguishable from the arrows being swapped |
| The day-info card and the month picker | same | daylight, sun and method are reported; `--:--` when there is no sunrise at all; picking a date in the sheet reports the choice |
| Marking a prayer from the pager | `PrayerTimesTrackingTest` | the future is refused, a marked prayer clears (and drops its timestamp), yesterday is keyed to *yesterday*, sunrise is never recorded, and it reports as `prayer_tracked` — #359's third site, the one that made dashboards under-count |
| The compass lifecycle | `QiblaScreenTest` | `StartCompass` on entry and `StopCompass` on disposal. A screen that never sends stop leaves the magnetometer registered after the reader has left — a drain nobody can see |
| The qibla error surface | same | shown only when there is no bearing to draw, and carrying both a retry and a route to setting a location. An error replacing a working compass is worse than the stale refresh behind it |
| The camera gate | same | AR is `state.isArMode && cameraPermissionGranted`, held in local state no ViewModel test can reach. The permission is requested rather than assumed, a denial is explained, and a grant enters AR |
| Where the compass thinks it is | `QiblaViewModelLocationTest` | a stored location yields bearing, distance and declination; 0,0 is an error rather than a compass pointing north; a nameless location still gets a name to show; an explicitly chosen location recomputes rather than only being stored |
| The confirmation haptic | same | a rising edge — once on the way in, again only after turning away and back — and silent when vibration is off, without losing the reported alignment |
| The bearing/status capsule | `QiblaStatusCapsuleTest` | three states from two booleans: the turn hint is withheld until the compass settles, alignment is announced even before it does, and the **sign** of `rotationToQibla` decides the direction. Dropping the sign is confidently wrong half the time |
| The compass view's two layouts | `CompassQiblaViewLayoutTest` | the tablet branch is a second copy of the same children and must lose none of them; `needsCalibration` is `UNRELIABLE || LOW`, not `UNRELIABLE` alone; with no location the dial draws but claims no bearing |
| The calibration sheet | `QiblaCalibrationSheetTest` | every accuracy the sensor can report has its own word — the label changing as you wave the phone is the whole point of the sheet — plus the gesture, the three steps and the way out. The frame clock is pinned manually: the figure-8 animation never lets it idle |
| The AR overlay's words | `ArQiblaViewTest` | outside the 60° field of view the overlay says so in words and names the shorter way round. A beam clamped to the screen edge is indistinguishable from one pointing just off frame, so someone with their back to Mecca is shown an arrow that looks right |
| The AR overlay's geometry | `QiblaArtTest` | drawn into a bitmap: nothing at all without a location, a beam that lands where the qibla is rather than in the middle of the screen, a sweep arc when it is behind you, and a dimmed — not dropped — beam on an unreliable compass |
| The night-worship window's other two states | `NightWorshipScreenTest` | loading is guarded on `lastThirdAt == null` too, so a refresh does not blank a live countdown; a failure is reported **inline** so the hub's other cards survive it, with a retry that dispatches |
| The manual retry and the failed night | `NightWorshipRefreshTest` | `Refresh` is the only way out of a failed hub without leaving the screen, and it is a separate path from the observation `init` starts, with its own error handling. A settings read that throws is reported rather than reaching `viewModelScope`'s uncaught handler |
| The five prayer destinations | `PrayerGraphTest` | all five register, and each resolves as a start destination in its own right. `Qibla` is registered twice over under two routes — a graph registering only one works from the tools hub and crashes from the bottom bar |

**One production edit, and it is a real fix.** `ArQiblaView` called `cameraProviderFuture.get()`
*outside* the `try` that guarded `bindToLifecycle`. A future that completes exceptionally — the
provider failing to initialise at all — then throws `ExecutionException` from a listener running on
the main executor with nothing above it to catch, and the app dies as the AR qibla opens, on
exactly the devices whose camera stack is already unhappy. The `get()` is inside the guard now.

**What stays uncovered, and why.** `PrayerTimesPdfExporter` (258 lines) for the reason above.
Twenty-four of `PrayerGraph`'s thirty lines are the five destination bodies: each builds its screen
through `hiltViewModel()`, which resolves only inside a composed `NavHost` on a Hilt-injected
activity this module cannot build — the same structural gap `:feature:search` records, and `:app`'s
instrumented suite is what exercises them. The rest is `@Preview` functions (three lines each,
their contents hoisted into the excluded `ComposableSingletons`), the `hiltViewModel()` default
arguments, and `QiblaCalibrationSheet`'s figure-8 `Canvas`, whose draw block needs a software
canvas the sheet's own popup window is not part of. No `COVERAGE_EXCLUSIONS` entry was added or
widened.


### About, Help and the More menu (`:feature:about/src/test`)

The thirteenth module locked: **17.8% lines to 94.1%**, from 434 covered lines to 2,291, and 23.0%
to **83.8%** branches. 139 tests added across thirteen new classes, taking the module's suite from
58 to 197. Nine files — every screen the module owns — were at **0%**, 1,713 of the 2,000 missing
lines, and nothing had ever composed one.

**Nothing is softened.** This is the sixth Compose module in a row to hold 80/80, and it is the
largest of them: seven screens, a bottom sheet and two renderer files. The `$dirty` bitmask
branches are here as everywhere, and at this size they still do not add up to enough to move the
number.

**Four things here are worth knowing before adding a test to this module.**

- **`hiltViewModel()` does not always need Hilt, and that is how `AdaptiveMoreScreen` got
  covered.** `:feature:quran` (#598) and `:feature:search` (#607) both left their adaptive screens
  at zero because the screens they embed resolve their ViewModels through `hiltViewModel()`. But
  that function only reaches for a `HiltViewModelFactory` when the `ViewModelStoreOwner` it is
  given supplies a **default factory**; hand it a plain owner whose `ViewModelStore` already holds
  the ViewModel and the store lookup answers first, with no factory consulted at all.
  `AdaptiveMoreScreenTest.seededOwner` builds exactly that, keying each mock by asking a real
  `ViewModelProvider` for it rather than by spelling out the default-key string. It does **not**
  rescue a destination body inside a `NavHost`: there the owner is a `NavBackStackEntry`, which
  *is* a `HasDefaultViewModelProviderFactory`, and the Hilt factory is built — and throws — before
  the store is read.
- **Section headings are uppercased by the component, not by the string.** `NimazSectionTitle`
  renders `text.uppercase()` unless told otherwise, so `onNodeWithText(getString(R.string.…))`
  finds nothing for a heading. Both Help classes carry a `sectionTitle()` helper for it. The pin
  labels are the mirror-image trap: `more_pin_zakat` and `zakat` are the same single word, so a
  screen test that leaves the default pins in place finds two nodes for "Zakat" and fails on the
  count rather than on anything real — the row tests unpin first, and `MoreMenuScreenTest` asserts
  the collision deliberately in the one test about the defaults.

| Area | Covered by | What it pins |
|---|---|---|
| Every More row opens its own destination | `MoreMenuScreenTest` | seventeen rows built from twenty lambdas of one type, tapped in order and asserted as a sequence. A row wired to its neighbour's lambda still opens *something*, so only the order shows it up; `FeatureNavigationTest` on the emulator taps a row and checks a tag, which cannot see a swap between two rows whose screens both exist |
| What a More subtitle claims | same | the row renders the figure from the field that belongs to it. `MoreSubtitlesTest` pins what each mapper returns; a row passing `khatamJuz` where `qaidaLesson` belongs still resolves to a well-formed string |
| The loading contract | same | a figure that has not arrived renders as *absent* — never a dash, a zero or a spinner. `MoreUiState`'s defaults are all null, which is the state the screen opens in |
| The pin row | same | a pill opens the same destination its menu row does (it is a *view* of those lambdas, not a second navigation surface), an empty row says so, and every member of the enum has a pill — asserted at tablet width, because a `LazyRow` composes about four pills at phone width |
| Coming back to More | same | `LifecycleResumeEffect` dispatches `Refresh`. The worship countdown is a snapshot over a dozen settings, so without it the row reports an hour-old "in 5h 12m" and nothing looks wrong |
| The pin cap | `PinnedShortcutsSheetTest` | at five pins an unpinned row is disabled **and a pinned one is not** — the exception that makes the cap survivable, since otherwise the row you want gone is the one you cannot reach. Plus: a new pin is appended, never inserted, so adding one does not reshuffle an arrangement |
| Which pane a tap moves | `AdaptiveMoreScreenTest` | About and Help push destinations on a phone and move the scaffold's detail pane on a tablet — the one difference between the two branches, and silent either way round. The licence list stays a push on both, because it has no pane |
| The About pane's own links | same | the tablet branch rebuilds About's lambdas rather than reusing the graph's, so privacy, terms, contact and back are a second implementation that can drift from the phone's |
| The version the app reports | `AboutScreenTest` | what `LocalAppIdentity` supplies. Its default is an em dash and build 0 on purpose; About reporting "—" on a shipped build is a support mail nobody can answer, and no crash or other test surfaces it |
| Where an update tap goes | same | three states send it three different ways — check, start, and the `completeUpdate` lambda that arrives *inside* `Downloaded` — and three more must send it nowhere, because a tap during a download restarts it. `UpdatePromptTest` pins the labels; the row looks identical whichever method it calls |
| A build with no update mechanism | same | a null `AppUpdateController` (a debug build, a test, a `@Preview`) renders and absorbs the tap rather than crashing |
| The licence list's counts | `LicensesScreenTest` | read off the whole catalogue, not the filtered rows. A filter chip whose own number moves when you use it is unreadable, and "1 of 4" is the only thing telling a reader the rest still exist |
| Three ways to end up with no rows | same | a query with no hits, an empty catalogue and a failed load are distinguishable on screen. "No libraries match" shown for an empty catalogue is a claim about a search nobody ran |
| The list's three controls | same | search, family filter and grouping toggle dispatch rather than filtering in place; tapping the selected chip clears the filter; the chip row hides itself when everything carries one licence |
| What a licence detail shows | `LicenseDetailScreenTest` | version, author, website and licence text are each optional in AboutLibraries' output, and each missing one has to leave the layout intact. A library with no licence text loses its **copy** button with it — copying an empty entry looks exactly like a working copy |
| The licence text control | same | collapsed under a fade by default, and the button says which way it goes. A dual-licensed library shows both texts and is filed under the first, which is what keeps the section counts agreeing with the library count |
| A library not in the bundled list | same | NOT\_FOUND with a way out and **no retry** — it will not be there next time either, so "try again" would be a lie |
| The licence family vocabulary | `LicenseVisualsTest` | total and distinct: every family has its own name and tone, because the list groups, filters and colours by family. `OTHER` alone has no plain-terms gloss — a licence the app cannot place it does not paraphrase |
| Search highlighting | same | every occurrence, case-insensitively, with the original casing intact. The list searches name, author and coordinate at once, so lighting up one of two matches reads as a search that half-worked |
| Help's three empty-looking states | `HelpScreenTest` | searching-with-no-hits, a failed topic load and the topic grid are the same state shape with different flags. The failure renders as a **section** so the search bar above it stays usable — search is a different query and may well work |
| A help topic's optional halves | `HelpTopicDetailScreenTest` | help content ships as data, so a topic with no questions, or no guides, or neither, is a shape the renderer meets — the headings have to be absent rather than empty. An answer stays hidden until its question is tapped, and opening one leaves the others shut |
| Load failure versus genuine absence | same, and `HelpGuideScreenTest` | both leave the detail null, and only the branch order tells them apart. "This topic is unavailable" for a transient failure blames the catalogue, and no retry then appears to contradict it |
| The near end of a help deep link | `HelpGuideScreenTest` | a step **with** a route renders a tappable breadcrumb and hands that route out verbatim; a step **without** one renders the same breadcrumb inert. `:core:navigation`'s `HelpDeepLinkTest` pins the far end — a screen that never calls `onDeepLink` passes every key assertion there |
| Retry, serving three surfaces | `HelpViewModelRetryTest` | one event, and it must re-run **only** what is failing: re-running a healthy surface throws its content back into a loading state. The home retry needs `retryTick` at all because `appLanguage` is a `StateFlow` with nothing to re-emit when the language has not changed |
| Content keys this build does not know | `HelpContentUiTest` | every `iconKey` and `colorKey` the content ships is one this build recognises, and an unknown one degrades rather than throws. A renamed key is silent — nothing logs, the screen still lays out, and every topic just starts looking the same |
| The seven About destinations | `AboutGraphTest` | all seven register, and each resolves as a start destination in its own right. All seven are reached directly: More from the bottom bar, About and Help from the menu *and* from Settings, and the three argument-carrying ones from a row |

**What stays uncovered, and why.** 122 of the 143 uncovered lines are `aboutGraph`'s seven
destination bodies, for the structural reason in the second bullet above — the same gap
`:feature:search` and `:feature:prayer` record, and `:app`'s instrumented suite is what exercises
them. The rest is `LicenceCatalogueTest`'s subject staying in `:app` (the AboutLibraries plugin
reads the *applying* project's classpath, so this module renders a catalogue it cannot produce),
the `hiltViewModel()` default arguments, and a handful of `when`-merge lines the compiler emits
with no statement of their own. No `COVERAGE_EXCLUSIONS` entry was added or widened.


### The repositories, the sync and the adhan store (`:core:data/src/test`)

The fourteenth module locked: **39.0% lines to 88.1%**, from 1,666 covered lines to 3,762, and
31.2% to **84.6%** branches. 431 tests added across twenty new classes, taking the module's suite
from 266 to 697. Five files were at **0%** between them — `AdhanAudioManager` (256 lines),
`NearbyConnectionsManager` and its two callbacks (334), `IntegrityTokenProvider` (39),
`AnnouncementBootstrap` (30) and `AdhanSound` (37) — 696 of the 2,602 missing lines.

**Nothing is softened, and nothing was excluded.** There is no Compose here, so the `$dirty`
bitmask that buys `:feature:quran` and `:feature:prayer` their 60% branch floor does not exist in
this module: every branch is one somebody wrote.

**Playbook step 1 found nothing owed to this module, which took checking.** `:app/src/test` holds
`LibraryMappingTest`, `LibraryRepositoryImplTest` and `DeviceStateCorpusTest`, and #611 flagged all
three as candidates to move. None of them belongs here: `LibraryRepositoryImpl` lives in `:app`
(the AboutLibraries plugin reads the *applying* project's classpath), and `DeviceStateCorpusTest`
builds a `NimazDatabase` — a `:core:database` type — and reaches no `:core:data` class at all.
`NextSurahToPlayTest`, `QuranAudioManagerDownloadTest` and `QuranReciterCdnMapTest` all exercise
`QuranAudioManager`, which stays in `:app` because `MainActivity` holds one. Every line of the rise
above is new tests.

**Four things here are worth knowing before adding a test to this module.**

- **The two halves of sync are only worth testing together.** An exporter that drops a column and
  an importer that never reads it agree perfectly, and the user loses the data with nothing on
  screen. `SyncRoundTripTest` exports from a populated set of DAOs and imports into an empty one,
  so a column that goes missing anywhere along the way fails an assertion about the *receiving*
  device. `SyncFixtures` holds the rows both halves use, once.
- **A DAO mock that does not persist breaks the bookmark merge.** `importBookmarks` and
  `importFavorites` both read `bookmarkDao.all()` and both write it, and the whole point of the
  merge is that the second sees what the first wrote. `SyncRoundTripTest` therefore keeps a real
  `markStore` list behind the mock rather than a fixed `returns`; with a static stub the round trip
  "loses" the bookmark half and the test fails for a reason that is not in the production code.
- **`NearbyConnectionsManager`'s callbacks are private fields, but they are *handed to* the
  client.** Mock `Nearby.getConnectionsClient` and capture the arguments of `startAdvertising`,
  `startDiscovery` and `acceptConnection`, and the whole protocol replays without radios. Two
  further notes: a GMS `Task`'s listeners post to the **main looper**, which under Robolectric is
  the thread the test is suspended on, so tests that await a result stub a `Task` whose listeners
  fire inline (the same trick `AndroidDeviceLocationRepositoryTest` and
  `IntegrityTokenProviderTest` use); and the STREAM branch reads on a background `Thread`, so its
  assertions poll rather than following the call.
- **A `Geocoder` constructed inside a private function is unreachable through Robolectric's own
  shadow**, because `ShadowGeocoder` keeps its answers on the *instance*.
  `device/StubGeocoder.kt` is a `@Config(shadows = …)` replacement that keeps them statically and
  implements the blocking overloads as well as the listener ones, so both sides of the API-33
  split in `AndroidDeviceLocationRepository.geocode` are reachable.

| Area | Covered by | What it pins |
|---|---|---|
| Export and import agree | `SyncRoundTripTest` | a populated phone's records arrive on an empty one, category by category. Row **ids never travel** — khatams merge on `createdAt`, prayers on `date`+`prayerName`, fasts on `date`, presets on `name`, sessions on `startedAt`, highlights on the span they cover, locations on coordinates — because `id` is `autoGenerate` and both phones have handed out 1, 2, 3… |
| A khatam's children | same | re-parented through the sender-id → local-id map the parent import returns. Without it another device's read verses attach to whichever local khatam holds that id and inflate its progress, which no screen can show as wrong |
| The older side never wins | same | every `local == null \|\| incoming.updatedAt > local.updatedAt` has both arms taken, on all eleven tables. Getting one backwards overwrites a record the user made more recently on *this* phone with a stale copy from the other, and there is no undo |
| The merge is additive | same | the payload lists what the sending device **has** — there are no tombstones — so a flag set on either side stays set and nothing is deleted for being absent. A name favourite unions both ways; an arriving hadith mark cannot clear a local favourite the wire format has no field for |
| The current location never travels | same, and `SyncDataExporterTest` | only favourites are exported, the payload type has no `isCurrentLocation` field, and an imported location is never made current. The failure is a sync that moves the receiving phone's prayer times to another city |
| The seven-table wire format | `SyncDataExporterTest` | one consolidated `bookmarks` row is fanned back out by kind and by flag, because a phone on this version has to sync with one that still has seven tables. A verse that is both bookmarked and favourited appears in **both** lists; a filter reading `bookmarked` where it means `favourite` silently drops one |
| The shared `progress` table | same | dua counts, qaida lessons and qaida cells all come out of one `progressDao.all()` and are told apart by nothing but `kind` |
| The progress bar's arithmetic | same, and `SyncDataExporterStepsTest` | eleven callbacks, eleven distinct labels, monotonic and never past the total — the defect that filled the bar to 120%, captioned it "Step 11 of 10" and rewound it to 80% |
| The signal protocol | `SyncSignalTest` | every signal round-trips, no two encode alike, and **anything that is not a signal decodes to null rather than throwing** — `onPayloadReceived` runs `decode` on every non-gzip BYTES payload, so an exception there takes the transfer down |
| Which branch a payload takes | `NearbyConnectionsManagerTest` | signals, data and files share one channel and only the leading `0x1F` separates them. A signal routed into the importer tries to import the word "cancel"; data routed into the signal decoder completes the sync having imported nothing |
| A signal's own transfer updates | same | skipped by payload id. Each signal generates its own IN_PROGRESS/SUCCESS pair, so without the filter the bar jumps to 100% and the screen calls the transfer done while the real payload is still in flight |
| A disconnect at the end | same | the partner always disconnects when it is finished, so an unguarded `onDisconnected` rewrites every successful sync as "connection lost". Completed, Error and Cancelled all survive it |
| The `@Singleton`'s handler | same | `stopAll` clears the data and signal callbacks. They capture the `SyncViewModel`, so without this a destroyed ViewModel and its cancelled scope stay reachable for the life of the process |
| An HTML error page saved as `.mp3` | `AdhanAudioManagerTest` | rejected by magic bytes **and deleted**, so the next attempt re-downloads rather than trusting a file that exists, is named right and plays nothing. ID3, bare MPEG sync and RIFF all count as audio; a bare sync word is common and rejecting it would re-download forever |
| Repairing an install whose URLs changed | same | the version stamp beside the files, and a bump deleting the recordings. This is the undo for Mishary's *regular* URL serving the Fajr recording — every install that had already downloaded it kept playing the wrong adhan. The generated beep survives, because a URL change cannot have staled a file with no URL |
| The generated chime | same | `SIMPLE_BEEP` has no URL and is synthesised into a WAV on the device. The round trip is the point: a wrong header means `isDownloaded` rejects the manager's own output and the beep can never finish downloading. Also asserted: real RIFF/WAVE/data chunks whose declared sizes match the file, 44.1 kHz, and samples that are not silence |
| Deleting one variant of a pair | same | regular and Fajr are separate files, so removing one must not reset the pair's state while the other is on disk |
| The Fajr flag | `AdhanSoundTest` | every sound resolves a different file and URL for Fajr, no two sounds share a file name, and a stored preference naming a sound that no longer exists falls back rather than crashing |
| The attestation token | `IntegrityTokenProviderTest` | the provider is prepared **once** and reused — the classic API throttled per app-instance, so a few asks worked and then every fetch failed — and a stale provider is re-prepared once and retried. A debug build that cannot attest sends `"debug-skip"`; a release build sends `""`, because sending the skip token from a release build would be an attestation bypass shipped to users |
| The announcements channel | `AnnouncementBootstrapTest` | its own low-importance channel, kept apart from the prayer and adhan ones, under the id the manifest hands the OS — a mismatch drops every background announcement with no error anywhere. A build with no `google-services.json` must not reach for messaging, and a messaging call that throws must not take the launch down |
| An empty console text box | `AnnouncementPayloadExtrasTest` | sends `""`, not nothing. Every optional field is trimmed and emptied to null, or a `cta_label` of `""` draws a button with no words and a `proof_ref` of `""` a citation card citing nothing. Extras and payload map to the same announcement, so a cold start and a running app agree |
| The three overlapping topic hierarchies | `QuranRepositoryImplThematicTest` | a breadcrumb picks its tree **per topic**, because search spans all three — an ontology result must not come back pathless while the thematic tab is selected. Both cycle guards terminate: the parent columns are content, regenerated per release elsewhere, and a cycle in them hangs a screen rather than erroring |
| A branch topic's verses | same | the whole **subtree**, not the node. Asking only for a node's own citations is what made "Doctrine" open on "0 verses" |
| Topic search order | same | index order is relevance order and `getTopics` does not preserve it, so a relevance search that silently comes back in id order looks like it works |
| A line-accurate mushaf page | `QuranRepositoryImplReadingTest` | resolved through the edition's own layout table, memoised per edition, and an unknown span emits an **empty** page — not the unrelated Madani page, which is the actual failure mode (#325) |
| Every verse read path | same | stamps `isBookmarked` from the *user's* database, which is a different file from the content. A path that forgets shows a reader with no bookmarks and no error. A stale translator preference resolves to the default rather than querying for rows that cannot exist |
| One row, two flags | `QuranRepositoryImplMarksTest`, `DuaRepositoryImplMarksTest`, `FavouriteCataloguesTest` | bookmarking and favouriting share a row, so turning one off clears the *flag* when the other is set and removes the row only when nothing is. Three catalogues, three repositories and three copies of the same `when`; this is what keeps them agreeing |
| The running totals | `QuranRepositoryImplMarksTest` | `updateReadingPosition` rewrites the row and neither `totalAyahsRead` nor `currentKhatmaCount` is an argument. Reading them back off the existing row is the only thing between a page turn and a khatma counter reset to zero |
| String ids over Int tables | `HadithRepositoryImplIdsTest`, `DuaRepositoryImplMarksTest` | the domain carries strings and every table is keyed by an Int, so every read parses. A lookup of one thing returns **null**; a list query falls back to **0** — an id no book has, so a `LazyColumn` gets a list to be empty of. Getting either wrong is a crash on a deep link, or book 0's hadiths under another book's title. A *write* against an unparseable id must do nothing at all |
| Chapters, which are not a table | `HadithRepositoryImplIdsTest` | derived from `GROUP BY chapter_id`, keyed `"{bookId}_{chapterId}"`, and the stored id is 0-based while the number a reader sees is 1-based. An off-by-one is every chapter heading in the app being wrong by one |
| Hadith of the day | same | deterministic from the day of the year, and an install whose content artifact has not landed yet has **none** — the modulo would divide by zero on the home screen |
| Localised help search | `HelpRepositoryImplSearchTest` | two queries merged, and the localised hit must win: de-duplicating the other way shows an English title to a reader in French. Only questions and titles are searched, or a result's "title" is a paragraph from the middle of a guide |
| `SUM()` over no rows | `TasbihRepositoryImplStatsTest` | is NULL in SQLite, and every one of those nulls reaches a screen that renders a number. A missing `?: 0` crashes the stats screen for a user who has not counted anything this week |
| Repairing a lost default preset | same | inserted with `id = 0` so Room assigns a fresh one. Reusing the default's id would **replace** whatever custom preset happens to hold it |
| The qibla needle's input | `AndroidCompassSensorsTest` | the filter is **seeded** on the first sample — a 0.97 low-pass started from zero needs ~100 samples to converge, which is what made the screen say "ready" and then sweep the needle in from a meaningless heading. Nothing is published until both sensors have reported, accuracy comes from the magnetometer only, and the listener is unregistered when collection ends |
| The geocoder's fallback chain | `AndroidDeviceLocationRepositoryTest` | locality, then county, then state, then feature name. Reverse-geocoding a point in open country returns an address with no locality, and without the chain the prayer-times header showed nothing. A geocoder that throws on a flaky connection returns empty rather than crashing the search box |
| What counts as location permission | same | fine **or** coarse. Requiring fine tells a user who granted approximate location that they granted nothing. Notification permission is a runtime grant only from Tiramisu; asking below that returns denied on every device and the app nags forever |
| The tasbih tick | `AndroidCounterFeedbackTest` | vibration and sound are independent settings, and crossing them buzzes a phone in a mosque. `release()` twice is safe, and a tick after it is a no-op rather than a use-after-free |

**What stays uncovered, and why.** The largest single gap is `AdhanAudioManager.downloadFile` and
the retry loop above it — about 75 lines and 50 branches. It opens a `HttpURLConnection` against
the CDN URL baked into `AdhanSound`, and there is no seam: exercising it would need either a real
network request from a unit test or a JVM-wide `URLStreamHandlerFactory`, which is global,
one-shot per process, and would make every other Robolectric class in the module order-dependent
in the way #620 documents. Everything the manager does *around* the transfer is covered.
`MediaPlayer` playback is the same shape at smaller scale. The rest is spread thinly over
`PrayerRepositoryImpl`, `TafseerRepositoryImpl` and `KhatamRepositoryImpl`, which were already
past the floor. **No `COVERAGE_EXCLUSIONS` entry was added or widened** — the list is shared with
every locked module, so widening it would move their numbers too.


### The library, rendered (`:feature:content/src/test`)

The fifteenth module locked: **28.8% lines to 85.1%**, from 1,380 covered lines to 4,072, and
19.5% to **80.4%** branches. Taken in **two passes** because 2,448 lines is more than one review
should carry — #624 took the hadith and dua corpora, #625 took the prophets, the names, qaida, the
catalog and `contentGraph`, and declared the floors.

**Nineteen files were at 0%**, which is every screen the module owns: the seven hadith and dua
screens (1,429 lines), `ProphetDetailScreen` (193), `ContentGraph` (154), `NamesScreen` (133),
`QaidaReaderScreen` (117), `FavouritesScreen` (96), `AsmaUlHusnaDetailScreen` (76),
`QaidaHomeScreen` (65), `QaidaAudioManager` (83 across two classes), the three adaptive wrappers
(137), `AsmaUnNabiDetailScreen` (39), `QaidaLettersScreen` (34) and the two catalog shells (68).
Nothing in the repository had ever composed one of them.

**What makes this module worth testing is that its failures are silent.** Every screen here
renders *shipped* content — narrations and supplications this build did not write, arriving as a
fetched artifact from `arshad-shah/nimaz-data` — and the way that goes wrong is never a crash. A
field the artifact does not carry renders as a blank line; an optional grade or reference
vanishes; a load failure reports itself as "no chapters found" and tells a reader to give up on a
collection that is fine. The `when` ordering that separates *loading*, *failed* and *genuinely
empty* is load-bearing on five of these screens, and `chapters.isEmpty()` is true in all three
states.

**The display settings were pinned at the persistence layer and nowhere else.**
`hadithShowArabic`, `hadithShowChain`, `duaShowTransliteration` and the rest were covered by
`:core:datastore` (#603) — that they round-trip. What no test asserted is that the screens
*honour* them: that turning the chain off actually removes the chain. Each is a separate `if` in
a reader, and a reader who hides the Arabic and gets it anyway can only report "the setting does
nothing".

**Four things here are worth knowing before adding a test to this module.**

- **`hiltViewModel()` does not need Hilt on the adaptive screens.** `AdaptiveHadithScreen` and
  `AdaptiveDuaScreen` hand their inner screens no ViewModel, so the default argument runs — and
  per #604's playbook item 8, an owner whose `ViewModelStore` already holds the mock answers
  before any factory is consulted. Both classes seed one. This is what makes the phone-vs-tablet
  branch reachable at all.
- **A `LazyColumn` at 2,200dp is not always tall enough.** The dua category-icon test renders
  forty-five rows to cover the emoji→icon lookup and runs at `w411dp-h6000dp`; the rest of the
  class stays at the usual height.
- **A `WhileSubscribed` flow's `.value` is null until something collects it**, and three of
  `QaidaReaderViewModel`'s moves read exactly that. A walk asserted without a subscriber passes
  for the *wrong reason* — the move is refused rather than performed — so `QaidaCourseWalkTest`
  has a named `subscribedViewModel()` helper and one test that deliberately does **not** use it.
- **The Qaida course map is drawn art.** `QaidaCoursePath`'s medallions carry no text, only a
  content description per lesson — which is the better contract anyway: it carries the number,
  the title *and* whether the lesson is locked, and that phrase is all a screen-reader user gets.

| Area | Covered by | What it pins |
|---|---|---|
| The four ways into the hadith reader | `HadithReaderScreenTest` | one `LaunchedEffect` turns four route shapes into four different loads, and their guards overlap by construction — `bookId` is empty for a search hit *and* for a grade shelf. The order is what keeps a **bookmark** from being looked up as a primary key, which lands the reader on a real narration from an arbitrary book rather than crashing |
| Each display toggle removing its own element | same, and `DuaReaderScreenTest` | Arabic, translation, grade and chain in the hadith reader; Arabic, transliteration and translation in the dua reader. The transliteration is guarded **twice** — the setting, and whether the artifact carries one — so "off" and "absent" have to produce the same page |
| A narration with no grade, narrator or reference | `HadithReaderScreenTest` | renders none of the three badges rather than three empty pills. The chain of whitespace is not offered at all |
| The isnād, expanded | same | collapsed by default; expanding splits the chain on its four separators and lists the narrators. A chain the separators do not match is shown **whole** instead of as a one-dot timeline |
| "Narrated by" not said twice | same | the dataset carries the prefix inline for some collections and a bare name for others. Prefixing unconditionally yields "Narrated by Narrated by Abu Huraira" |
| Failure vs emptiness, on five screens | `HadithChaptersScreenTest`, `HadithReaderScreenTest`, `HadithCollectionScreenTest`, `DuaOccasionScreenTest`, `DuaReaderScreenTest` | the error branch sits **before** the empty branch on every one of them, because the list is empty in both cases. Getting it back to front tells a reader their collection is empty when the database could not be read |
| Retry re-issuing the right load | same five | `HadithEvent.Retry` re-runs only the failing surface; the dua screens re-issue the **route's** id, not whatever the ViewModel loaded last |
| The hadith of the day's two lives | `HadithCollectionScreenTest` | `getHadithOfTheDay` is best-effort, so `hadithOfTheDay` is null on a healthy screen and the card shows shipped fallback text. Nothing distinguishes the two at a glance, and bookmarking while the fallback is up must dispatch **nothing** rather than the fallback's imaginary id |
| Which grades are browsable | same | exactly Sahih, Hasan and Ḍaʿīf. Mawḍūʿ (fabricated) is a warning label on a narration, not a shelf to invite anyone onto, and the list it is absent from is a private `val` no compiler check guards |
| Every collection getting its own cover | same | the six Kutub al-Sittah ids plus the default arm, all seven resolved while the card composes |
| A bookmark citing the number a reader sees | same | the card dispatches `hadithNumberInBook`, not `hadithNumber`. The two differ wherever numbering restarts per volume, and a bookmark saved under the wrong one reopens on a different narration |
| The chapter search being derived | `HadithChaptersScreenTest` | rows come from `filteredChapters`, which recomputes from `searchQuery` on every read. Rendering `chapters` instead passes every test that does not search — and the header still renders when the search matches nothing, so a typo does not blank the screen |
| A chapter opened under its own book | same | `onNavigateToChapter(bookId, chapter.id)` carries both, because the reader keys on the composite `bookId_chapterId`; the id alone resolves the chapter header to null |
| The dua library's two layouts | `DuasCollectionScreenTest` | curated order splits into a four-card grid and a list below it; alphabetical collapses both into one A–Z list. `take(4)`/`drop(4)` are a partition only while both branches run, and nine categories all have to survive the split |
| Every category emoji resolving to an icon | same | forty-three mapped keys plus an unknown emoji and a category with none. An emoji the artifact starts using turns silently into a mosque, and they all look alike |
| The sort control naming where it goes | same | the content description inverts with the state rather than describing it — the only thing a screen-reader user has to go on |
| The shared dua row, in both of its forms | `DuaCategoryScreenTest`, `DuaOccasionScreenTest` | given `onOccasionClick` the occasion is a badge into that occasion's whole list; without it, plain text. The occasion screen passes nothing, because a chip navigating to the list you are on is a dead end |
| The occasion label being localised | `DuaCategoryScreenTest`, `DuaOccasionLabelsTest` | `DuaOccasion.displayName()` is hardcoded English — right for a log line, wrong on a chip. All seventeen occasions map to distinct resources, and the mosque and home pairs are opposite rather than interchangeable |
| A repeat count of zero | `DuaCategoryScreenTest`, `DuaReaderScreenTest` | the artifact stores `0` for "no prescribed count", and an unguarded badge reads "0x" |
| The occasion screen not borrowing a header | `DuaOccasionScreenTest` | it renders from `categoryState`, the surface `LoadCategory` also fills, so a stale `category` left in it would put another collection's name above these duas |
| Adding a dua to the tasbih | `DuaReaderScreenTest` | goes through this feature's own ViewModel, not a `hiltViewModel<TasbihViewModel>()` reach across the module boundary — and the toast is the reader's only feedback that it happened |
| Favouriting citing the category too | same | a `DuaBookmark` is keyed on both ids; the dua's alone files the favourite under no collection |
| Phone push vs tablet pane | `AdaptiveHadithScreenTest`, `AdaptiveDuaScreenTest` | the same tap must push a route on a phone and move the scaffold's detail pane on a tablet. Backwards, a tablet stacks a full-screen list over a layout built to show both, and a phone's rows do nothing — neither crashes, and the inner screens' own tests cannot see it because the lambda they assert on is the wrapper's choice |
| The detail pane's second decision | same two | chapters or reader, dua list or dua, from whether the pane's args carry the second id — which the list pane built two taps earlier |
| Nineteen graph destinations, eleven of them argument-carrying | `ContentGraphTest` | the largest graph in the app. An unregistered destination throws only when a **user taps the row** — not at build time, and in none of the four gates `CLAUDE.md` lists. Four hadith routes are the same screen reached four ways, so losing one breaks exactly one entry point and nothing else. **This test is also what caught `docs/NAVIGATION.md` claiming 21** |
| Which fields each catalogue searches | `CatalogueSearchFieldsTest` | `CatalogViewModel` is generic and `CatalogViewModelTest` drives it with a synthetic source, so the three **real** sources were at 0%. The prophets' search also covers the **title** and the **era** — how you look for a prophet whose name you cannot spell — and dropping an arm quietly returns fewer results with no crash and no empty state |
| A slower detail read losing to a newer one | same | `requestedItemId` is set synchronously and checked *after* the suspension point, because a coroutine cancelled after its last suspension still runs to the end of its block. All three catalogue screens share one ViewModel per back-stack entry, so this is one fast back-and-forward away |
| Every optional prophet section | `ProphetDetailScreenTest` | lessons, cited verses and miracles are each guarded; the timeline's four label/value pairs are the shape where two get swapped and the page still reads as a well-formed timeline. The only header in the app rendered with `number = null` |
| The shared catalogue shell's two guards | `CatalogDetailScreensTest` | the FAB appears only when there is an item, and `isLoading || item == null` is **one** branch — a stale deep link leaves `isLoading = false` with no item, and without the second half the screen renders a header of nulls |
| Three catalogues behind one search box | `NamesScreenTest` | one query reaches all three ViewModels, which is what makes the per-tab match counts mean anything; a dispatch reaching only the visible tab looks identical from the tab you are on. Clearing sends `ClearSearch`, not `Search("")` — they leave the same list, and only one keeps an empty query out of the debounced analytics flow |
| A favourites section that is empty rendering nothing | `FavouritesScreenTest` | the check lives in `favouriteSection`, so a fourth kind of favourite cannot forget it. The whole-screen empty state is the screen's, not a section's: a reader with two starred prophets and no starred names must see the prophets |
| The Qaida course walk, and its guards | `QaidaCourseWalkTest` | next refuses a **locked** lesson — the gate the whole progression rests on, duplicated from the screen where it is only cosmetic — previous stops at the start, and resume falls back to lesson 1 once `nextLessonId` is null. Re-selecting the open lesson is a no-op because `selectLesson` stops audio, and a `LaunchedEffect` re-fires on every recomposition |
| Which cell is highlighted | same | resolved from the audio key against the **open** lesson's cells. Audio outlives the screen, so a clip still sounding after a lesson change must light nothing rather than a foreign tile |
| A lesson finished during this visit, once | `QaidaReaderScreenTest` | `openedComplete` is captured from the first non-null status, so re-opening a finished lesson to practise does not put confetti in front of it — and finishing one while reading still celebrates |
| Where a letter is made | `QaidaLettersScreenTest` | `makhrajLabel` and `makhrajEmoji` are two `when`s over the same five-value enum, and the sheet is the only place either runs. This line is the whole of what the sheet teaches |
| A clip resolving to the right file | `QaidaAudioManagerTest` | a drop-in under `filesDir/qaida_audio/` beats the bundled asset — the entire on-demand delivery mode, one `if`, reachable through no setting. A **truncated** download is a zero-length file that exists, and trusting `exists()` alone plays silence forever with nothing on screen to say why |
| A tap that should do nothing | same | a cell whose `audio_key` the artifact does not carry is refused rather than queued: queuing `""` asks for `qaida/audio/.mp3`, which fails asynchronously and surfaces as a *playback error* on a tap that should simply have been ignored. A line filters the same way, and one that filters down to a single clip is played as a tap |
| The dua collection's sort | `DuaCollectionSortTest` | curated `displayOrder` or A–Z by **lowercased** English name — sorting without lowercasing puts every capitalised name before every lowercase one, which reads as no order at all. The toggle persists the state it is moving *to* |
| The reader's paging window | same | opening a dua loads its whole category so the pager can page; a dua missing from its own category list opens at the top rather than at index **-1**, and one whose category comes back empty opens alone rather than resolving and then claiming "not found" |
| The hadith reader's anchor | `HadithReaderAnchorTest` | held **by id, not by index**, so a content refresh that inserts a row above the reader keeps them on the hadith they were reading. An index-based anchor passes "a refresh does not reset the reader" and still moves them. A refresh that removes the anchored hadith falls back to the top rather than indexing at -1 |
| A retry re-running only what failed | same | a retry tapped in the reader must not also re-fetch the book list behind it, and nothing on screen would say whether it did |
| What the copy button puts on the clipboard | `HadithReaderScreenTest` | built by a local function nothing else calls, and it is what a reader pastes into a message. Narrator and reference are guarded separately from the page's own guards, so a hadith can render perfectly and paste with "Narrated by " and a trailing blank line |
| An id with an underscore | same | `bookId.isEmpty() && !chapterId.contains("_")` is the whole classifier, and hadith ids in this dataset **do** contain underscores (`bukhari_1_1`) — the two shapes are one character apart and the rule is a convention, not a proof |


**What stays uncovered, and why.** 125 of the 255 missing branches are the Compose compiler's
`$dirty` bitmask — one per parameter of every restartable composable, on the signature line and
its closing paren, neither side reachable because which one runs depends on what the *caller*
changed between recompositions. Discount those and the module stands at **89.4% branches**. Of the
rest, three pockets account for half: `QaidaAudioManager`'s `Player.Listener` (12 branches —
Robolectric has no media pipeline, so the player never leaves `STATE_IDLE` and no callback fires;
reaching them would mean reflecting into ExoPlayer's private listener set, and its *consumer* is
covered instead); `DuaViewModel.filterAndSortCategories`'s search arms (10 — **genuinely dead**,
because `searchQuery` is only ever written as `""` and the collection's search action navigates to
`:feature:search`'s screen instead); and `QaidaPlayLineButton` (4 — behind
`QAIDA_AUDIO_UI_ENABLED`, `false` while the recordings are regenerated, with the *absence* of the
control asserted so the test is inverted rather than deleted when the flag flips). Sharing the
hadith of the day is also uncovered: `shareBranded` hops to `Dispatchers.Default` to render a card
before it shares anything, and that hop does not complete under the Compose test clock.
**No `COVERAGE_EXCLUSIONS` entry was added or widened** — the list is shared with every locked
module, so widening it would move their numbers too.

### What the user did (`:feature:tracker/src/test` and `src/testDebug`)

The sixteenth module locked: **30.4% lines to 91.9%**, from 1,575 covered lines to 4,755, and
24.4% to **81.3%** branches. 341 tests over 34 classes, 23 of them new. No production code
changed beyond one stale doc comment (`trackerGraph` said it held 11 destinations; it holds 14).

**Nine files were at 0%** — every screen the module owns and both of its drawn surfaces:
`TasbihScreen` (463 lines), `FastTrackerScreen` (281), `MakeupFastsScreen` (247),
`ChooseDhikrScreen` (218), `PrayerStatsScreen` (218), `PrayerTrackerDayCard` (202),
`TasbihHistoryScreen` (193), `FastingComingUp` (189), `RamadanCards` (161), `AddPresetScreen`
(149), `TasbihBeads` (163 across four classes), `FastDaySheets` (104),
`MakeupFastEditBottomSheet` (92), `TrackerGraph` (89) and `QadaPrayerContent` (79).
`PrayerTrackerScreen` had seven covered lines of 232.

**This is where the app writes the user's own record of worship, and none of it can be undone.**
`:core:database` (#597) already pins the arithmetic underneath — that confirming a week of
unrecorded prayers never overwrites a logged one, never marks sunrise and stops at the range's
ends; that a missed fast leaves a `pending` row clearable by either door; that perfect days and
per-prayer stats add up. What none of that can see is the **surface**: whether the offer is made
at the right time, against the right day, and for the right count. A review banner that offered
to mark a fully-logged week as missed is a one-tap way to fabricate a qada list, and the DAO test
would still be green.

#### The prayer tracker

`PrayerTrackerScreenTest` (25) composes the tracker against a mocked ViewModel and pins the
review banner in both directions: **no banner over a complete week**, a count that is the count of
actually-unrecorded prayers, a `MISSED` row excluded because it is already an assertion, a
`NOT_PRAYED` row *included* because clearing a status must read back as "nobody has said", and a
confirmation that asks for the seven days **behind** today rather than today itself. It also pins
the load window — the displayed month **and** the trailing review window, always, which is what
stopped paging to another month leaving the banner counting days it had no records for — and that
`QADA` is offered only once a prayer's time has passed, which used to be reachable by marking a
prayer prayed early. `PrayerTrackerNavigationTest` (9) takes the day stepper: back one day at a
time, forward only as far as today.

`PrayerStatsScreenTest` (11) takes the three insights, which are derived in the screen and
nowhere else: the weakest prayer is named only below 90%, a prayer with **no** record counts as
100% rather than 0% (or it would be reported as the user's weakest every time), and a fresh
install with everything at zero is offered no insight at all instead of opening on "Overall
completion: 0%". `PrayerRadialChartTest` (4) draws the radar chart into a software canvas and
asserts the polygon's *size follows the record* — three charts side by side in one composition,
each measured by the pixels in which it differs from an empty one. `QadaPrayersScreenTest` (6)
pins that an empty list says prayers land there only when the user says so, and that an empty
list which is still **loading** is not called caught up.

#### Fasting

`FastTrackerScreenTest` (22) pins which Ramadan card shows — banner during, countdown within
thirty days, neither outside, and the banner winning when both would qualify — the month-paging
arithmetic across a year boundary in both directions, and that the exemption and note sheets
write against the **selected** day rather than today. `FastingWindowTest` (7) supplies a fixed
clock through `ProvideNimazClock`'s `timeSource` seam and walks the suhoor→iftar band through all
four of its sentences, including the half-a-schedule case a location change can leave behind.
`FastingComingUpTest` (11) pins the upcoming-fasts list: sorted by date (built unsorted it opened
with next Monday when today was a Thursday), deduplicated across the two Hijri years it is
gathered from, and marking a day logged only for a `FASTED` record.

`MakeupFastsScreenTest` (16) is the debt: both ways of settling a fast filed under *Settled* and
neither under *Owed*, only pending rows offering a way to act, and — the one that matters most —
choosing fidya in the sheet raising `PayFidya` rather than `UpdateMakeupFast`, because recording
a payment as a completed fast loses the money and the debt at once.
`FastingRamadanTest` (6) reaches the arm of `loadRamadan` that runs one month a year, and pins the
distinction the redesign is built on: **missed** means recorded as not fasted, **unlogged** means
nobody has said. `FastingStatsAndDebtTest` (12) pins the three statistics windows and that a
missed Ramadan day creates exactly **one** make-up fast however many times its status is changed.

#### The tasbih

`TasbihScreenTest` (17) and `TasbihSessionTest` (21) split the counter between what it shows and
what it stores. The count lives in Room, so a lost tap is a permanently wrong total: the session
test pins the two-taps-during-the-insert race that used to leave an orphan session and a counter
reading 1, that a lap writes the **within-lap** count (the database sums `currentCount + laps ×
target`, so writing the running total would double every lap), that switching dhikr mid-count
completes the old session rather than abandoning it, and that a re-emission of the persisted
selection does not reset a count in progress.

`TasbihBeadsTest` (10) draws the strand into a software canvas: it paints edge to edge, it
mirrors for a left-handed user — a setting with no other confirmation anywhere, since the strand
is a `Canvas` with no text in it — each material paints in its own colours, an unknown design key
falls back rather than blanking the counter, and one gesture is one bead whether it is a tap or a
flick. `ChooseDhikrScreenTest` (15) pins that the tab and the search box narrow **together**, and
that deleting a custom dhikr asks first and keeps it on cancel. `AddPresetScreenTest` (10) pins
the edit form's seeding rule: the fields are copied from the loaded preset **once**, because the
presets flow re-emits on any write to that table and re-seeding would discard whatever the user
had typed. `TasbihHistoryScreenTest` (9) pins the live today total and the `%d:%02d` session
length that is formatted nowhere else.

`TrackerGraphTest` (6) asserts all **fourteen** destinations register, and register once. Seven of
them share a screen composable with another route, so a copy-paste that registered the same
`Route` twice would look right in review and leave the other one throwing at the tap that opens
it. Fourteen is asserted as a number because #625 recounted every graph and found five documented
counts wrong, this one among them — `check_docs.py`'s NAV-03 compares the 94-destination total
against `Routes.kt`, not the per-graph split.

**What stays uncovered, and why.** 131 of the 280 missing branches are the Compose compiler's
`$dirty` bitmask — one per parameter of every restartable composable, on the signature line, its
parameter lines and its closing paren, neither side reachable because which one runs depends on
what the *caller* changed between recompositions. Discount those and the module stands at
**89.1% branches**. Of the rest, four pockets are unreachable rather than untested:
`TasbihCounterUiState.autoLap` is never false (~10 branches — no event sets it, so the manual-lap
arm is dead until a control is wired to it); `TasbihPresetsUiState.selectedCategory` is never set
(4 — `ChooseDhikrScreen` filters through its own tab state, though `TrackerDerivedStateTest` pins
the derivation anyway, since it is the guard that stopped a re-emission of the presets flow
silently dropping an active filter); the `PENDING`/`NOT_PRAYED` arms of the day card's picker
mappings (3 — `PICKER_STATUSES` holds only the four assertions a user can make, and the `when`
must still be exhaustive over six); and `PrayerStatsScreen`'s date-parse fallback (4 — neither
`Long.MIN_VALUE` nor `Long.MAX_VALUE` actually throws on the way to a `LocalDate`, so no stored
value a test can supply reaches the `catch`). `FastingComingUp`'s Hijri event arms (17) are the
one date-dependent pocket: which of Ashura, Arafah, Shawwal and mid-Sha'ban fall ahead of "today"
changes with the day the suite runs.
**No `COVERAGE_EXCLUSIONS` entry was added or widened** — the list is shared with every locked
module, so widening it would move their numbers too.

### The design system itself (`:core:ui/src/test` and `src/testDebug`)

The largest module in the repo — 13,000 measurable lines, 316 classes, 143 files — and the one
whose regressions are never local. `CLAUDE.md` rule 8 makes several of these components
*mandatory* (a button is `NimazButton`, a card tap target is `NimazCard(onClick = …)`, rows are
separated by `NimazMenuDivider()`, arrows come from `NimazIcons`) precisely because hand-rolled
equivalents got the ripple, the radius or the target size wrong. A defect here is a defect on every
screen at once.

**Three tests were living in `:app/src/test` and moved to the module that owns their subject** —
`ShareablesZakatTest` (`core/share`), `TranslationFontFamilyTest` (`theme/`) and
`CompassDegreesTest` (`foundation/geometry`). The merged `:app` report counted them either way;
what changes is the attribution. The first of those has since moved once more, with the other five
share tests, into **`:core:share`** — same reason, one module further out.

**Two techniques account for most of the new coverage, and both are worth reusing.**

`testing/SoftwareCanvas.kt` draws a composable into a **software** `android.graphics.Canvas` under
`@GraphicsMode(NATIVE)` and reads the pixels back. This module owns the app's drawn surfaces, and
composing a `Canvas` runs the call while its `DrawScope` lambda never executes — so
`PrayerSkySceneArtTest` (11) pins that midday is brighter than midnight, that the sun crosses the
sky rather than sitting still, that a full moon out-shines a new one and that every phase's path
operations produce something; `CompassPrimitivesArtTest` (10) pins **that the qibla needle points
where the qibla is** — a sign error there sends somebody to pray facing the opposite way and draws
a perfectly convincing dial doing it — and that facing turns it green; `NimazErrorStateArtTest` (6)
pins the fractured shamsa's four layers; `QiblaGeometryTest` (8) pins that the beam rises at the
`x` it is handed and the off-screen arc hugs the edge it points at; `NimazPatternBackgroundTest`
(8) pins that the ornament preference actually reaches the drawing rather than only the setting;
`QaidaArtTest` (6), `KhatamProgressDrawTest` (4), `CompassTurnArcTest` (3), `NimazMarginRuleTest`
(4), `NimazSkeletonTest` (5), `HarakatColouringTest` (4) and `NimazScreenScaffoldTest` (3) cover
the rest. `captureToImage()` is *not* the route — it goes through `PixelCopy` and hangs (#604).

The second is that **a component setting `clearAndSetSemantics` is addressable only by its
announcement**. `ZakatSummaryHeroTest` (8) asserts the plinth reads as one phrase, `KhatamVisuals`
and `NimazTimeDisplay` the same, and `QaidaCoursePathTest` (10) addresses every lesson medallion by
what a learner is told — including that a **locked** medallion is not offered as a control at all.

**The rest, by what they pin.** `NimazErrorStateTest` (18): the app's one failure state at three
scales, and that a stack trace stays behind its toggle. `ShareablesTest` (24) and
`ShareablesBlankFieldsTest` (5): every share body, with each optional field absent *and* blank —
the content database is a fetched artifact, so `""` is the shape that actually arrives.
`ContentShareManagerTest` (6): the intent the app really starts. (Those three, and the two
`ShareCardRenderer` tests, are `:core:share`'s now — listed here because the module they describe
came out of this one.) `WorshipReminderContentTest` (10):
eleven reminder types across two parallel `when`s, and that the `Context` and `StringProvider`
overloads never disagree — the file's own KDoc records them doing so once, giving every
Arafah/Ashura reminder the Arafah body. `HadithGradeChipTest` (7): that no two authenticity grades
share a colour, which is the most serious thing this app can get wrong. `NimazCalendarDayCellTest`
(15): the day cell's five marks and the priority between them, including the `NaN` completion
fraction a day with no scheduled prayers produces. `PrayerDisplayStatusTest` (8): that an unlogged
prayer whose time has not come reads as *upcoming* rather than missed. `PrayerTimeDisplayTest` (8):
which row is current at both ends of the day — the two regressions `PrayerClock`'s KDoc records.
`ExpandableSearchBarTest` (14) and `NimazSearchBarOptionsTest` (5): the search bar's panel state
machine and its autofocus. `NimazListPickerVariantsTest` (8): that the search corpus includes the
description, because people remember a calculation method by where it is used.
`NimazNumberStepperFieldTest` (5): typing into a stepper, its digit filter and its clamp.
`AdaptiveSpacingTest` (5) at three window widths, `NimazThemeTest` (8) including the deliberate
`surfaceTint` deviation from Material, `BeadColorsTest` (6), `DesignScaleTest` (5),
`TranslationTypographyTest` (11) and the option sweeps for the components whose parameters no other
test supplies.

**160 test classes, 1,072 `@Test` methods.** No production code was changed, and **no
`COVERAGE_EXCLUSIONS` entry was added or widened** — the list is shared with every locked module,
so widening it would move their numbers too.

**What stays uncovered, and why 80/80 was still the right pair.** Two groups are counted in full
against both floors and cannot be driven:

- **`@Preview` and `*Showcase` functions — 1,979 lines, 15.2% of the module, all at 0%.** This is
  the design system, so previews *are* part of maintaining it: 263 `@Preview` annotations across 86
  of the 143 files, against zero in `:feature:about` and `:feature:tools`. They are `private` by
  convention, and driving them would pin the tooling rather than the product. Counting them,
  everything else has to clear **94.4%** for the module to read 80 — which is what it now does.
  `NimazCalendarShowcasePreviews.kt` is the extreme case: 145 lines that are nothing but tooling.
- **183 branches of the Compose compiler's `$dirty` bitmask** — one per parameter of every
  restartable composable, neither side reachable because which one runs depends on what the
  *caller* changed between recompositions. With 143 files of composables this is the largest such
  accumulation in the repo, and it is the pressure that argued `:feature:quran` down to a 0.60
  branch floor. It does not force one here because the *other* Compose-generated branch — the
  `$default` parameter mask — **is** reachable, by calling a composable both with its defaults and
  with explicit arguments. Several tests here are deliberately option sweeps for that reason, and
  a parameter read into a local and then not passed on is a real defect: the caller's choice is
  silently ignored, visible only on the one screen that sets it.

Three smaller things are structurally out of reach and are left at zero rather than excluded:
`SearchResultCard` and its two colour helpers in `NimazSearchBar.kt` are `private` and have no
caller in the module; `shareFile`'s `FileProvider` lookup needs an authority declared in `:app`'s
manifest, so a library unit test cannot reach it (`:app`'s instrumented suite does); and the cloud
band in `PrayerSkyScene` is blitted through `ColorFilter.tint(…, BlendMode.Modulate)`, which does
not survive Robolectric's canvas — the bake and the wrap-around double blit run, the composite
does not.


### Every preference the app can change (`:feature:settings/src/test` and `src/testDebug`)

The last module to join the ratchet, and the largest by surface: 24 screens, a 1,400-line
`SettingsViewModel`, device-to-device sync and the location picker. It went from **11.3% lines**
to **94.2%** over 35 test classes and 521 `@Test` methods, with no production code changed and no
`COVERAGE_EXCLUSIONS` entry added or widened.

**The loader had never run in a test at all.** `SettingsViewModel.loadSettings()` reads fifty-odd
preferences with `.first()`, and the module's existing suite built the ViewModel on a
`mockk(relaxed = true)` `SettingsRepository` — whose flow properties answer with a relaxed `Flow`
that never emits. `first()` on that throws, `launchSafely` swallows it, and all 152 of the
loader's lines were dead behind a green test. What that hides is the worst thing a settings screen
can do: every control renders its **compile-time default** rather than the user's choice, and the
first toggle they touch writes that default back over the real preference.
`testing/SettingsRepositoryStub.kt` fixes it by making the *reads* real `MutableStateFlow`s while
leaving the writes on the mockk, so the loader runs and `coVerify { repo.setHapticFeedback(false) }`
still works. Reuse it for anything that takes the whole `SettingsRepository`.

| File | Pins |
|---|---|
| `SettingsLoadTest` (24) | Every stored preference reaching the state holder that owns it, and the three string-parsed values' fallbacks — an unreadable calculation method silently becomes Muslim World League, which changes every prayer time in the app, so the telemetry error is asserted rather than the fallback alone. Also that the Quran state *collects* DataStore rather than snapshotting it, which is why a reciter picked on one screen shows on the one behind it. |
| `SettingsEventTableTest` (51) | The whole 78-branch `when`, each event against the exact setter it must write — the crossing this module is most exposed to, since a branch that updates the right field and writes the wrong setter is invisible from the screen. Also which events rearm the alarms and which must not: a preference that changes *when* a notification fires has to reschedule, one that changes only *how* must not. |
| `SettingsGraphTest` (5) | All twenty destinations registered, exactly once each — including the four that live under `screens/{dua,hadith,quran}` and are registered here because the module boundary follows the ViewModel axis. |
| `PrayerNotificationsScreenTest` (19) | The highest-stakes screen: that sunrise is not a sixth prayer (no alert style, no reminder, its toggle inside Fajr's row), and that "remind me before every prayer" writes the app-wide pair **and** all five per-prayer events — the pair alone changes no notification, which is what the control did before the per-prayer split. |
| `NotificationSettingsScreenTest` (16), `NotificationSoundScreenTest` (14), `NotificationWeeklyScreenTest` (11), `WorshipRemindersScreenTest` (15) | The notification tree. Each hub subtitle is handed the settings that belong to it; the master switch *removes* the rows rather than dimming them; closing the voice picker stops whatever it was auditioning, by any of four routes; and the Ramadan reminders stay visible outside Ramadan — a regression to hiding them would be invisible for eleven months a year. |
| `NotificationDiagnosticsScreenTest` (12), `NotificationDiagnosticsTest` (5) | Each badge against an arranged device state, and each row opening the system screen it reports on rather than a neighbour's. `hasProblem` is an OR, and before Android 12 exact alarms are reported allowed rather than as a fault the device cannot clear. |
| `SyncScreenTest` (36), `SyncViewModelTest` (23) | Sync is one screen wearing seven faces chosen by an *ordered* `when`, so the ordering is the logic: a cancelled sync must not offer "Accept" for a connection the partner already dropped, and an error must outrank a completed connection, because "Sync Complete!" over a failed import tells the user their data moved when it did not. On the ViewModel side: the sender exporting on connect and the receiver not, each cancellation reason recorded separately, and the Ack that lets the other device stop waiting. Every key `SyncPayload.categories()` can produce has a label, so a new category cannot appear in the manifest as *No data* beside a real count. |
| `WidgetsScreenTest` (11) | Six live previews built by the real `PrayerTimeCalculator` — pinned by *five formatted clock times*, because an unwired gallery renders an em dash for every one and looks entirely plausible in a screenshot. Plus the fallbacks: a blank stored name, and an unset location computing against Dublin rather than (0, 0) in the Atlantic. |
| `LocationScreenTest` (22), `LocationViewModelTest` (24) | The GPS button's two paths, with the permission launcher answered from the test — the only way to reach the callback where "fine **or** coarse counts as granted" lives. Selecting a place writes both the preference the calculator reads and the database row the recents list is built from; a save that fails must not leave the card showing a city the calculator has never heard of. |
| `SearchSettingsScreenTest` (24) | The AI opt-in: enabling must open the disclosure sheet rather than write the preference, and a consent write that fails must say so instead of closing over a switch that quietly stayed off. Also that the last search source on cannot be switched off, since `SearchPreferences.sanitised` reads an empty set straight back as "everything" — a switch that flips itself. |
| `AdaptiveSettingsScreenTest` (12) | The same rows behaving differently by width: navigating on a phone, opening a detail pane on a tablet, and the two that still navigate on a tablet because they are not panes. Reached without Hilt through a pre-seeded `ViewModelStore` — four ViewModels, because four of the nine panes take one of their own. |
| `QuranSettingsScreenTest` (19), `DuaSettingsScreenTest` (11), `HadithSettingsScreenTest` (10), `SelectReciterScreenTest` (9), `SelectTranslationScreenTest` (12) | The reading settings and their two pickers. Tajweed is gated on the Madani script and *says why* it is unavailable; the colour-blind underline is gated on tajweed. The reciter preview auditions the reciter whose row it is in — that button previously played silence from a fresh ViewModel's empty playlist. |
| `AppearanceSettingsScreenTest` (16), `LanguageScreenTest` (7), `SettingsScreenTest` (11), `PrayerSettingsScreenTest` (16), `ZakatSettingsScreenTest` (12) | Theme, language, the hub and the two calculation screens. "Follow device" is a switch above two cards, so three states are expressed through two controls and turning following *off* must land on the theme the device currently resolves to. The hub's two destructive confirmations are asserted apart, because crossing them makes "reset settings" clear every tracked prayer. |
| `VoiceOptionCardTest` (14, `src/testDebug`) | The shared voice row: its two taps stay separate, so pressing play does not also change the user's reciter. |

**Two matchers were needed and are worth knowing about.** `testing/SettingsRowMatchers.kt` holds
them. A settings row must be addressed as `hasText(title) and hasClickAction()` rather than by
text, because a section header repeats its rows' words *and* because `NimazSettingsItem` expresses
"disabled" by dropping its `clickable` — so a bare `onNodeWithText` lands on the raw `Text`, which
reports neither enabled nor disabled and makes "this row is off-limits" unassertable. Inside an
**accordion** even that fails: the accordion is a clickable card and merges its whole body into one
node carrying every row's text and the card's own `OnClick`. There, `tappableAncestorCount(title)`
is the assertion — an enabled row has two tappable ancestors, a disabled one has only the card.

Three things are left at zero or partly covered rather than excluded, and the shapes are worth
recognising: **`SyncMode.NONE` in the two role helpers**, reached only from a `when` arm that has
already established the mode is SEND or RECEIVE; **the `check()` guarding
`IMPORT_STEP_COUNT`** against the list beside it, whose failing arm exists so a future edit to one
and not the other stops the import rather than reporting "step 13 of 10"; and the
**`Locale`-dependent `replaceFirstChar` arms** in the widget preview's date formatting, whose
empty-string branch cannot be reached from a `Month` or `DayOfWeek` name.


### What is left in `:app` (`app/src/test` and `src/testDebug`)

The nineteenth and last module of #551, and the smallest surface of the campaign: `:app` held 52
files and ~11,500 lines, and only what genuinely cannot live in a library — the composition root,
the alarm scheduler, the notification receiver, three foreground services, the Quran audio
session, the Home screen and its ViewModel. It went from **32.6% lines** to **82.6%**, with no
production code changed and no `COVERAGE_EXCLUSIONS` entry added or widened.

> **Home has since left**, for `:feature:home`, and with it the three rows below marked
> *(now `:feature:home`)* — 33 test classes and 205 tests, moved intact. `:app` has no
> `presentation/` directory at all now. The rows are kept because what they pin is unchanged and
> the reasoning is where it was written.
>
> **`:app`'s own line floor came down to 0.75 with them, and the module did not get worse.** The
> same tests pass in `:feature:home`, where they are gated at 80/80; what is left in `:app` is
> proportionally more composition root, so the *ratio* falls while total measured coverage across
> the repo rises. A floor on a module that is being emptied has to be re-measured at each
> extraction rather than assumed — which is also why `:app:coverageFloor` being absent from the
> PR lane (`fastlane android test` runs `testDebugUnitTest` and `lintDebug`, not `check`) is worth
> knowing: `./gradlew check` is where this shows up.

**Two things had to be built before a single test could run**, and both are reusable.

`testing/TestEntryPointApplication.kt` is the smaller of them. `@AndroidEntryPoint` is not a
no-op at run time: the Hilt Gradle plugin rewrites each `BroadcastReceiver.onReceive` and every
`Service.onCreate` to inject first, and the generated base class then asks the **application** for
a component — so an ordinary `android.app.Application` throws *"Hilt BroadcastReceiver must be
attached to an @HiltAndroidApp Application"* before a line of the subject runs. That is why the
receiver, both adhan services and the Quran audio service had no unit tests at all despite being
where every notification and every sound the app makes comes from. `HiltTestApplication` would
satisfy Hilt and is the wrong tool — it builds the real singleton graph, so a test about an
`AlarmManager` call ends up opening a Room database from a content artifact this machine cannot
fetch. The class implements only the two interfaces Hilt actually reaches for
(`GeneratedComponentManager` for the receiver, a `ServiceComponentBuilder` behind
`ServiceComponentManager` for the services) and hands back doubles the test chose.

The other is a **measurement** fix, and it is the one worth carrying forward. `:app`'s
`moduleCoverage` measured `intermediates/built_in_kotlinc/debug` like every other module, while
the tests load the classes AGP's ASM pipeline rewrote — which for every `@AndroidEntryPoint` class
is a *different* class file. JaCoCo says so, quietly, in a line nobody reads:

    [ant:jacocoReport] Execution data for class …/BootReceiver does not match.

and then reports that class as **0% however thoroughly it is tested**. Three classes and 509 lines
were affected, and the signature is a file at 50% whose outer class is at 0% while its nested
lambdas report normally. `:app` now measures the transformed root instead; no other module has an
`@AndroidEntryPoint` class a unit test constructs, so the fix is in `app/build.gradle.kts` rather
than in `build-logic`.

| File | Pins |
|---|---|
| `core/util/PrayerNotificationSchedulerTest` (29) | Every alarm the app lives by, read back off `shadowOf(alarmManager).scheduledAlarms` rather than verified as a call. Nothing in the UI shows that an alarm was expected, so this class was at 1% while notifications were the app's headline feature. It pins that the midnight chain — the whole recurrence mechanism, since every other alarm is a one-shot — is armed for 00:01 tomorrow; that turning notifications off *cancels* rather than silently leaving yesterday's alarms armed; that no alarm is ever armed in the past, because Android delivers a past trigger immediately and the notification then re-posts on every reschedule for the rest of the day; and that a malformed stored khatam time falls back to 06:00 rather than throwing and taking every prayer alarm with it. Also the deliberate asymmetry `cancelAllPrayerNotifications` leaves behind: the daily summary is a recap rather than a prayer alert and survives it. |
| `core/util/PrayerAlarmReceiverTest` (30), `PrayerAlarmReceiverEdgesTest` (13), `BootReceiverTest` (8), `BootReceiverManifestTest` (4) | Where every prayer notification is actually produced. The alert style decides sound, and the tests assert on the *channel* because that is what Android reads: a silenced prayer lands on the muted channel at low priority, vibration off lands on the no-vibration sibling, and sunrise cannot be silenced at all because it is the end of Fajr's window rather than a prayer. The adhan path pins the rule that matters religiously — a missing variant falls back to the beep and **never** to the other variant, because playing the Fajr adhan at Dhuhr is wrong — and that Do Not Disturb gates the audio only, so a reader in a meeting is still told a prayer came in. Every handler is wrapped in `catch (e: Exception)`, which is right for a receiver with nowhere to propagate to and is also why each arm needs a test to exist at all: a handler that throws on its first line looks exactly like a quiet day. The recovery half is its own receiver and its own two files: `BootReceiverTest` covers the four actions that re-arm the alarms — including `MY_PACKAGE_REPLACED`, which is what carries an install across the rename of a `PendingIntent`'s target — and `BootReceiverManifestTest` checks the half of a receiver's contract that lives in XML. That second one exists because three branches of the old receiver read as supported, were covered by a unit test calling `onReceive` directly, and on a device never arrived once: there was no `<intent-filter>` behind them. |
| `core/init/AppInitializerTest` (15) | The five startup tasks and the 5-second budget that lets the UI open regardless. A slow migration must not hold a reader at the splash screen when they opened the app to check Maghrib, and a task that throws must not stop the other four. Also that today's alarms are re-armed on every start — nothing else does it — and that an install which already has the audio does not re-download it on every cold start. |
| `core/util/InAppUpdateManagerTest` (20) | The update banner's state machine, driven through a stubbed `AppUpdateManagerFactory`. The recoveries are the point: a failed check, a cancelled Play dialog and a flow Play refuses to launch each have to leave the banner interactive, because a banner stuck on a spinner is a dead control with no other route to the update. Play's callbacks post to the main looper, so every assertion idles it first — without that the whole class passes reading `Checking`. |
| `data/audio/AdhanPlaybackServiceTest` (14), `AdhanDownloadServiceTest` (13), `QuranAudioServiceTest` (15) | The three foreground services, each built with `Robolectric.buildService`. What they produce is a notification, and the endings are what a reader sees when something half-works: a download that fetched one variant is reported as **partial** rather than done, because "downloaded" with no Fajr file means silence at the one prayer nobody is awake to notice. The Quran service's notification has to follow the audio state — Pause while playing, ongoing only while playing, download progress instead of the reciter while preparing — and take itself down when audio ends rather than sitting in the shade. |
| `data/audio/AdhanDownloadWorkerTest` (8) | The background half, which is the path app-init and a prayer broadcast actually take since a foreground service cannot start from the background on Android 12+. Its retry budget is what stops a permanently broken URL waking the device forever; `setRunAttemptCount` is the only way to reach that fork. |
| `data/audio/QuranAudioManagerTest` (24), `QuranNextSurahPlaylistSourceTest` (6), `HttpAyahAudioDownloaderTest` (5) | The reader-facing half of the audio session. The rolling rule has four separate refusals and none of them is visible from the player: the setting off, any repeat set (a repeat is a request to *stay*), a single verse played on its own — which is not a reading, so carrying on into a whole surah would be the app deciding to keep going after being asked for one thing — and there being no 115th surah. The downloader is exercised against a real loopback socket rather than a mock, because what is asserted is cancellability, the defect the seam exists for. |
| `presentation/screens/home/HomeScreenTest` (20), `HomeScreenBannersTest` (11) *(now `:feature:home`)* | 783 lines and two complete layouts that had never been composed. The screen is where state becomes *arrangement*: which of the two layouts a window gets, whether the prayer-times failure takes the screen or stays in a card, and — the one worth stating — that the banner slot is a **queue**, not a stack. Three warnings apply on a fresh install, and rendering all three in place is what used to push the prayer card below the fold; the slot shows one and puts the rest behind a sheet. Four of the five update states had never rendered, including the downloaded one whose action is the only way an update is ever installed. |
| `presentation/viewmodel/home/HomeViewModelBehaviourTest` (24) *(now `:feature:home`)* | The dashboard's loaders and its two prayer writes. `HomeViewModelTest` beside it is a construction-safety suite; this is the rest. The banner tests subscribe to `announcement` *before* asserting, because it is a `WhileSubscribed` flow and with no collector `announcement.value` is the default — `dismissAnnouncement` then returns at its first line and the test is green over a ViewModel that did nothing. Also that sunrise records nothing at all, which is what stopped the engagement dashboard counting toggles that never happened. |
| `presentation/components/…` (9 new classes) *(now `:feature:home`)* | The Home components' remaining forks. The prayer card's **status picker** only appears when a passed, non-sunrise row is expanded, so nothing that merely rendered the card could reach it — and it holds the app's position that a prayer whose time has passed with nothing recorded is not counted as missed until the reader says so. `TodayCarouselPagesTest` scrolls the pager, because `PageSize.Fill` means one page per width whatever the viewport and the hadith and dua branches are otherwise unreachable. The banner variant `when` exists twice, in the carousel and in the slot compact Home actually uses, and three of its four arms had never run. `HijriPrimaryTest` renders both headers with "Hijri first" on — a preference the app offers and neither header had ever been drawn with. |

**What is deliberately left at zero.** `NavGraph` and `MainActivity` (252 lines, 194 branches)
are the composition root: `MainActivity` is `@AndroidEntryPoint` and its body is
`setContent { NavGraph(…) }`, and a destination inside a `NavHost` receives a `NavBackStackEntry`
as its `ViewModelStoreOwner` — which *is* a `HasDefaultViewModelProviderFactory`, so Hilt's factory
is constructed and throws before any seeded store is read. They are the instrumented suite's job,
and `app/src/androidTest`'s navigation package drives the real graph on a device.
`QuranAudioManager`'s player listener (180 branches) fires on ExoPlayer reaching `STATE_ENDED` or
transitioning items, which needs a player actually decoding audio. And the 35 `@Preview` functions
are tooling the app never runs.


## Conventions

- **Selectors live in `support/Selectors.kt`.** Tests reference `Selectors.NavLabel.*`,
  `Selectors.<Area>.<key>` (a `@StringRes` id resolved at runtime), or `Selectors.Tag.*`.
  Add new selectors there rather than inlining literals.
- **Entities come from `support/TestData.kt`.** Override only the fields a test asserts on.
- **DAO tests use the in-memory `NimazDbRule`** (hermetic, fast). Asset/migration
  behaviour is covered separately so a schema change fails in exactly one obvious place.
- **UI flows extend `BaseAppTest`** and must carry their own `@HiltAndroidTest`
  (the annotation is not inherited from the base).
