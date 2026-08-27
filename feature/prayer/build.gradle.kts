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

            /**
             * **One JVM per test class, only in this module.**
             *
             * `ArQiblaView` is the app's only CameraX surface, and `ProcessCameraProvider
             * .getInstance()` cannot complete on a machine with no camera: it leaves a pending
             * listener on the main looper and a half-initialised provider behind it. Robolectric
             * runs a whole module's classes in one JVM, so that state outlives the test that
             * created it and lands on whichever class launches an activity next — as
             * `FutureGarbageCollectedException: CameraX initInternal` raised from an unrelated
             * screen's test, or as `lightZ must be a finite positive, given=Infinity` thrown out
             * of `ThreadedRenderer.setup` before that class runs a line of its own.
             *
             * Both failures name neither CameraX nor the test that caused them, and both depend
             * on class order and on when a GC happens to run — so they are green locally and red
             * in CI at a rate no amount of re-running settles. Forking per class is what actually
             * contains it. Measured cost: this module's `testDebugUnitTest` goes from about one
             * minute to about four. The alternative is either an intermittently red gate or
             * leaving the AR qibla — the only camera surface in the app — untested.
             *
             * No other module needs this, and none of them has a camera dependency.
             */
            all { it.setForkEvery(1) }
        }
    }
}

/**
 * Locked at the standard line floor with a **softened branch floor**, measured at **84.1% lines /
 * 70.5% branches** with the tests added by #609.
 *
 * **The softening is one file.** `PrayerTimesPdfExporter` — 296 of the module's 2,853 lines and
 * 116 of its 760 branches — cannot run under Robolectric at all: `PdfDocument` throws
 * `"document is closed!"` on the first `startPage`, so every path past that point is unreachable
 * from a JVM test. `:feature:quran` hit exactly this with `TafseerPdfExporter` (#598) and settled
 * it the same way: leave it at zero, make the 80% up elsewhere, and say so here. Nothing is
 * excluded to reach these numbers — `COVERAGE_EXCLUSIONS` is untouched, and the file is counted
 * in full against both floors.
 *
 * The arithmetic is what decides the branch floor. 103 of the 224 branches still missing are in
 * that one file; discount them and the module stands at **81.6%** branches, over the standard.
 * Counting them, 80% is arithmetically out of reach — the remaining 121 are spread across six
 * Compose screens and twelve components, where a large share is the `$dirty` bitmask branch the
 * Compose compiler emits per parameter of every restartable composable, and no test can take both
 * sides of those. So: `lineFloor` at the standard, `branchFloor` at 0.60, which the module clears
 * by more than ten points.
 *
 * **What is *actually* uncovered, besides the exporter**, is two things and both are structural.
 * Twenty-four of `PrayerGraph`'s thirty lines are the five destination bodies: each constructs its
 * screen through `hiltViewModel()`, which resolves only inside a composed `NavHost` on a
 * Hilt-injected activity this module has no way to build. `PrayerGraphTest` covers the
 * registration those bodies hang off — the failure that ships is a destination that throws on tap
 * — and `:app`'s instrumented suite exercises the bodies. The rest is `@Preview` functions (three
 * lines each, their contents hoisted into the excluded `ComposableSingletons`), the
 * `hiltViewModel()` default arguments, and `QiblaCalibrationSheet`'s figure-8 `Canvas`, whose draw
 * block needs a software canvas the sheet's own popup window is not part of.
 *
 * `ArQiblaView`'s camera preview is *not* on that list: the CameraX binding cannot complete with
 * no camera, but the overlay drawn over it is fully covered, and the provider future's failure
 * arm is now guarded rather than crashing out of a main-thread listener.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.60)
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
