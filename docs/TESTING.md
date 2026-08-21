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

- **Unit / Robolectric** (`app/src/test`, `app/src/testDebug`) — ViewModel logic and
  Compose "atom/molecule/organism" component tests run off-device under Robolectric.
  Coverage is reported via the `jacoco*Report` Gradle tasks (see `app/build.gradle.kts`).
- **Instrumented** (`app/src/androidTest`) — the suite documented here. Runs on an
  emulator/device against the real Hilt graph, Room database, WorkManager, and
  `MainActivity`/`NavGraph`.

## Running the unit tests

```bash
./gradlew testDebugUnitTest              # every Android module — today that is only :app
./gradlew :build-logic:convention:test   # the convention plugins and their TestKit fixtures
```

Two things to know about that pair.

**Prefer `testDebugUnitTest` over `:app:testDebugUnitTest`.** They are the same tests today,
because `:app` is the only module with a unit-test source set. They stop being the same the
moment the multi-module split (#551) lands a module, and a habit of naming `:app` explicitly
would then quietly skip every other module's suite. The all-module form costs nothing now and
is correct later. The full multi-module story lands with the split itself.

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

## Conventions

- **Selectors live in `support/Selectors.kt`.** Tests reference `Selectors.NavLabel.*`,
  `Selectors.<Area>.<key>` (a `@StringRes` id resolved at runtime), or `Selectors.Tag.*`.
  Add new selectors there rather than inlining literals.
- **Entities come from `support/TestData.kt`.** Override only the fields a test asserts on.
- **DAO tests use the in-memory `NimazDbRule`** (hermetic, fast). Asset/migration
  behaviour is covered separately so a schema change fails in exactly one obvious place.
- **UI flows extend `BaseAppTest`** and must carry their own `@HiltAndroidTest`
  (the annotation is not inherited from the base).
