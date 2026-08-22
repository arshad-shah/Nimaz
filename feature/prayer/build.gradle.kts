plugins {
    id("nimaz.android.feature")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.prayer"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

// When each prayer *is*, and which way to face: prayer times, the monthly table, qibla and the
// night-worship window. The counterpart to `:feature:tracker`, which owns what the user *did*
// about them; the two share `PrayerRepository` through `:core:domain`, which is the seam that
// lets prayer times and prayer tracking live in different modules at all.
//
// **#571 asked for the adhan players and the prayer notification machinery too, and they are not
// here.** Grepping the whole move set for them returns nothing: no file under `screens/prayer`,
// `screens/qibla`, `screens/worship`, `viewmodel/prayer` or `viewmodel/worship` names
// `AdhanAudioManager`, `AdhanSound`, either adhan Service, `AdhanDownloadWorker`,
// `PrayerNotificationScheduler`, `PrayerAlarmTimes`, `PrayerRescheduler`,
// `NotificationContentHelper`, `NotificationDiagnostics` or `BootReceiver`. Their real consumers
// are `SettingsViewModel`, `NotificationSoundScreen`, `NotificationSettingsScreen`,
// `NotificationDiagnosticsScreen` — the settings surface, which becomes `:feature:settings` in
// PR 21 — plus `AppInitializer`, `ServiceAdhanDownloader` and `MainActivity` in `:app`.
//
// Moving them here would therefore have created a `:feature:settings -> :feature:prayer` edge in
// the very next PR: exactly the coupling #571 forbids, and exactly the reason it already sends
// `viewmodel/location` to `:feature:settings` rather than here. So SUB-01/SUB-03 are unchanged by
// this PR — the four Services and every notification channel stay where they are, and PR 21 needs
// a port for the two `SettingsViewModel` reaches for. Recorded on #571 and #572.
//
// The one `core/util` file the feature does touch is `PrayerTimesPdfExporter`, read by
// `MonthlyPrayerTimesScreen` and nothing else, so it comes.
//
// **Two components went *down* to `:core:ui` rather than across.** `PrayerTimeCard` and
// `PrayerSkyScene` are read by `PrayerTimesScreen` here *and* by `HomeScreen`/`HomeHero`, which
// stay in `:app` — the sixth consecutive PR where "used by the feature" was not "used only by the
// feature". `PrayerTimesSectionHeader`, `CountdownTimer`, `JumuahCard` and `WorshipEventCard` are
// the mirror case: they read as prayer components and have no consumer in this module at all, so
// they stay in `:app` with the home surface that does read them.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)

    // `PrayerTimesViewModel` and `MonthlyPrayerTimesViewModel` work in `kotlinx.datetime`
    // instants and zones — the prayer-time calculation's own vocabulary.
    implementation(libs.kotlinx.datetime)

    // CameraX, for `ArQiblaView` alone: the AR qibla overlay draws the bearing over a live
    // preview. It is the only camera surface in the app, so all four artifacts follow it out of
    // `:app` rather than being shared.
    //
    // The `CAMERA` permission stays in `:app`'s manifest and this module declares none. Unlike
    // `AndroidHaptics` in `:core:data` — where moving the code made `:core:data:lintDebug` report
    // `MissingPermission` for `VIBRATE`, because `Vibrator.vibrate` is `@RequiresPermission` —
    // CameraX's `bindToLifecycle` carries no such annotation, so lint has nothing to check and
    // adding a duplicate declaration here would be cargo cult. Verified: `:feature:prayer:lint`
    // is clean, and the merged manifest is unchanged at 103 components.
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:ui")))
    testImplementation(testFixtures(project(":core:common")))

    // Six Compose tests — the qibla molecules and the night-worship content — under Robolectric;
    // see `src/testDebug/resources/robolectric.properties` for the SDK and Application pins.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
