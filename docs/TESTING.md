# Testing

Two on-device/JVM test surfaces exist:

- **Unit / Robolectric** (`app/src/test`, `app/src/testDebug`) — ViewModel logic and
  Compose "atom/molecule/organism" component tests run off-device under Robolectric.
  Coverage is reported via the `jacoco*Report` Gradle tasks (see `app/build.gradle.kts`).
- **Instrumented** (`app/src/androidTest`) — the suite documented here. Runs on an
  emulator/device against the real Hilt graph, Room database, WorkManager, and
  `MainActivity`/`NavGraph`.

## Running the instrumented suite

```bash
# Boot an emulator (API 29+; the app's minSdk) first, then:
./gradlew :app:connectedDebugAndroidTest

# A single class / package:
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.arshadshah.nimaz.db.PrayerDaoTest
```

Requires JDK 21 + Android SDK (`local.properties` `sdk.dir` or `ANDROID_HOME`).

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

## Module layout (`app/src/androidTest/java/com/arshadshah/nimaz`)

| Package          | What it covers |
|------------------|----------------|
| `support/`       | Shared infra — **no test owns a magic string**. `HiltTestRunner`, `Selectors` (nav labels, real `testTag`s, `R.string` lookups), `TestData` (entity builders), `NimazDbRule` (in-memory Room), `BaseAppTest` (Compose + Hilt base for UI flows). |
| `db/`            | Round-trip CRUD + `Flow` coverage for every major DAO (prayer, fasting, tasbih, khatam, quran, dua, hadith, zakat, tafseer, qaida, names, location, events), plus `DatabaseAssetTest` verifying the shipped prepopulated DB seeds. |
| `preferences/`   | `SettingsRepository` (DataStore) round-trips + export/import. |
| `notifications/` | `PrayerNotificationScheduler` channel creation + schedule/cancel smoke. |
| `work/`          | Every `@HiltWorker` widget/adhan worker executed via the real factory. |
| `navigation/`    | Launches `MainActivity` and walks the bottom-nav tabs + More-menu flows. |
| (root)           | `MigrationTest` (per-step) and `MigrationChainTest` (v7→current). |

## Conventions

- **Selectors live in `support/Selectors.kt`.** Tests reference `Selectors.NavLabel.*`,
  `Selectors.<Area>.<key>` (a `@StringRes` id resolved at runtime), or `Selectors.Tag.*`.
  Add new selectors there rather than inlining literals.
- **Entities come from `support/TestData.kt`.** Override only the fields a test asserts on.
- **DAO tests use the in-memory `NimazDbRule`** (hermetic, fast). Asset/migration
  behaviour is covered separately so a schema change fails in exactly one obvious place.
- **UI flows extend `BaseAppTest`** and must carry their own `@HiltAndroidTest`
  (the annotation is not inherited from the base).
