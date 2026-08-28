plugins {
    id("nimaz.android.feature")
    // `AdaptiveSettingsScreen`'s navigator persists which detail pane is showing.
    id("kotlin-parcelize")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.settings"

    // `SyncViewModel.debugLog` gates verbose sync logging on the build type. A library's
    // `BuildConfig` carries only its *own* fields — never the application's, which is the trap
    // `IntegrityTokenProvider` documents — but `DEBUG` is one of its own, and it is the only thing
    // read here. So this is the one case in the epic where the answer is to generate the class
    // rather than pass the value in.
    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// **80/80 — the tenth Compose module in a row to hold the standard**, and the last module in the
// repo to join the ratchet. It stands at **94.2% lines** and **82.6% branches**, which is the
// widest line margin of any module locked so far and a branch margin of 41.
//
// The branch number is the one that took work. Of the 279 branches still missing, **141 sit on
// composable signature and parameter lines** — the Compose compiler's `$dirty` skippability check
// and its `$default` parameter mask, emitted once per parameter of every restartable composable
// across 24 screens. Discount those and the module stands at **89.9%**.
//
// The remaining 138 are spread thin, and the pockets worth naming are unreachable rather than
// untested:
//
//   - **`SyncMode.NONE` inside the two role helpers.** `RoleBadge` and `AuthTokenContent`'s role
//     line are both reached only from a `when` arm that has already established the mode is SEND
//     or RECEIVE — the screen shows the mode-selection content while it is NONE. The `NONE` arm
//     is exhaustiveness, not a state.
//   - **`check(totalImportSteps == IMPORT_STEP_COUNT)`** in `SyncViewModel.importWithProgress`.
//     It guards a hand-maintained constant against the list beside it, and both sides are in the
//     same file: the failing arm exists so a future edit to one and not the other stops the
//     import rather than reporting "step 13 of 10", which is what shipped before it. Making it
//     fire from a test would mean changing the constant, which is the thing it guards.
//   - **`Locale`-dependent `replaceFirstChar` arms** in the widget preview's date formatting (8).
//     The empty-string branch of `replaceFirstChar` cannot be reached from a `Month` or
//     `DayOfWeek` name.
//
// No `COVERAGE_EXCLUSIONS` entry was added or widened for any of it.
//
// The last feature module: 24 screens and the 1,324-line `SettingsViewModel`, plus location and
// sync. The most cross-referenced module in the app, and the one the epic deliberately left until
// every other feature had already taken what it owned.
//
// **Five screens arrive here from other features' directories.** `DuaSettingsScreen`,
// `HadithSettingsScreen`, `SelectReciterScreen`, `SelectTranslationScreen` and `LocationScreen`
// sit in `screens/{dua,hadith,quran,settings}` and every one of them dispatches `SettingsEvent` on
// `SettingsViewModel`. They stayed in `:app` through PRs 17, 19 and 20 for that reason and land
// here now — the ViewModel axis, cutting the other way for the last time.
//
// **`data/sync` did *not* come with `SyncViewModel`, and nothing about the screen would tell you.**
// The slice imports 21 DAOs and 14 entities directly, and `:core:database` is not on a feature
// module's classpath. It went to `:core:data`, which is the only module that can hold it, and
// `SyncViewModel` reaches it the same way every other ViewModel reaches persistence.
//
// **The adhan audio manager went to `:core:data` too, and one dead pair of methods was the only
// thing that had ever stopped it.** `AdhanAudioManager.playAdhanForNotification` and
// `.stopNotificationAdhan` called `AdhanPlaybackService`, which is pinned to `:app` by
// `R.drawable.ic_stat_nimaz` — so the manager looked like it needed a port. Neither method has a
// call site anywhere in the repository. Deleted; the manager moved unchanged otherwise. The
// Service itself stays in `:app`, reached directly by `BootReceiver` and `MainActivity` as before.
//
// **`PrayerNotificationScheduler` genuinely could not move**, for one line: `AppR.drawable
// .ic_stat_nimaz` at `:823`, inside `sendTestNotification()`. `SettingsViewModel` uses exactly
// three of its members, so those three joined the `PrayerAlarmScheduler` port that
// `RescheduleNotificationsUseCase` already needed — 4 members exposed of a 917-line class, the
// same shape as `AppUpdateController` (3 of 7) and `QuranPlayback` (13 of 39).
//
// `NotificationDiagnostics` came outright: 50 lines, `AlarmManager`/`PowerManager`/
// `NotificationManagerCompat` and no `R` at all.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // `SyncViewModel` drives `NearbyConnectionsManager`, `SyncDataExporter` and
    // `SyncDataImporter` — all three `:core:data`, for the classpath reasons above.
    implementation(project(":core:data"))
    // `SettingsViewModel` drives `AdhanAudioManager` and the adhan settings screens name
    // `AdhanSound` and `DownloadState`. Those were `:core:data`; they are `:core:audio`'s now,
    // with the rest of the audio stack.
    implementation(project(":core:audio"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.work.runtime.ktx)
    // `WidgetsScreen`'s live preview builds prayer times for a fake day, in the same
    // `kotlinx.datetime` vocabulary the calculator speaks.
    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:common")))
    // `createComponentComposeRule()` / `setThemedContent {}` — the one Compose harness, published
    // from `:core:ui` rather than copied per module.
    testImplementation(testFixtures(project(":core:ui")))
    // `LocationScreen` answers a permission launcher, and its test needs the activity that
    // launcher belongs to. `LocalActivity` rather than casting `LocalContext`, which lint
    // rejects — a Context is not always an Activity.
    testImplementation(libs.androidx.activity.compose)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
