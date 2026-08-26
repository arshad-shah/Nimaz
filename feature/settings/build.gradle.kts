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
    // `SyncDataImporter`; `SettingsViewModel` drives `AdhanAudioManager`. All four are
    // `:core:data`, for the classpath reasons above.
    implementation(project(":core:data"))

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

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
