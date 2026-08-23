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
  - [Two things that silently do not count](#two-things-that-silently-do-not-count)
  - [Reading the number](#reading-the-number)
- [Coverage audit (what's validated)](#coverage-audit-whats-validated)
  - [The same ground, on the JVM (`:core:database/src/test`)](#the-same-ground-on-the-jvm-coredatabasesrctest)
- [Conventions](#conventions)

## Running the unit tests

```bash
./gradlew testDebugUnitTest              # every Android module — nineteen of them
./gradlew :core:domain:test              # :core:domain is a pure JVM module, so `test`, not `testDebugUnitTest`
./gradlew :build-logic:convention:test   # the convention plugins and their TestKit fixtures
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

`:app:jacocoTestReport` is the one merged report. It is **reported, not gated** (#464): CI
publishes the number, nothing fails on it.

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

## Coverage audit (what's validated)

| Layer / area | Covered by | Kind |
|---|---|---|
| Every DAO (prayer, fasting, tasbih, khatam, quran, dua, hadith, zakat, tafseer, qaida, names, location, events) | `db/*DaoTest`, `UserDataDaoTest` | CRUD + Flow round-trips on in-memory Room |
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
| FTS query assembly | `search/ContentSearchIndexQueryTest` | placeholders and bound arguments staying in step when the optional `source` filter is present; the two paths that must never reach a `MATCH` |

## Conventions

- **Selectors live in `support/Selectors.kt`.** Tests reference `Selectors.NavLabel.*`,
  `Selectors.<Area>.<key>` (a `@StringRes` id resolved at runtime), or `Selectors.Tag.*`.
  Add new selectors there rather than inlining literals.
- **Entities come from `support/TestData.kt`.** Override only the fields a test asserts on.
- **DAO tests use the in-memory `NimazDbRule`** (hermetic, fast). Asset/migration
  behaviour is covered separately so a schema change fails in exactly one obvious place.
- **UI flows extend `BaseAppTest`** and must carry their own `@HiltAndroidTest`
  (the annotation is not inherited from the base).
