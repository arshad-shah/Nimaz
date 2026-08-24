plugins {
    // `nimaz.android.feature` is library + compose + hilt in one id, so a feature module's
    // plugins block cannot drift from its siblings. `:feature:widget` predates the habit and
    // lists the three separately; it is switched to this id in the same commit.
    id("nimaz.android.feature")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.onboarding"

    testOptions {
        unitTests {
            // Parity with `:app` and `:core:ui`, and it is load-bearing rather than boilerplate.
            // `OnboardingViewModel`'s catch block calls the static `CrashReporter.recordException`
            // and `AppAnalytics.logError` *before* it sets `state.error`. Without default values
            // those hit an uninitialised Firebase and throw, `launchSafely` swallows the second
            // exception, and the user-visible error is never set — so two tests that passed in
            // `:app` failed here for a reason that had nothing to do with what they assert.
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and
 * how the ratchet works.
 *
 * **80/80, at 94.3% lines and 90.3% branches** — a Compose module holding the full branch floor,
 * like `:feature:calendar` and unlike `:feature:quran`. The unreachable `$dirty` bitmask branch
 * the Compose compiler emits is one per parameter of every restartable composable, so its weight
 * scales with how many composables a module has; there are eight here, and the ratio never
 * becomes the number.
 *
 * **Nothing is excluded, and nothing is left at zero.** The 226 lines of `OnboardingArt.kt` are
 * pure `Canvas` geometry, which #605 flagged as possibly uncoverable — and would be, if a test
 * only composed the tree: `Canvas(modifier)` runs, its `DrawScope` lambda does not.
 * `OnboardingArtTest` asks the `ComposeView` to draw itself into a software
 * `android.graphics.Canvas` under `@GraphicsMode(NATIVE)` and reads the pixels back, which runs
 * the draw blocks for real and makes the art assertable rather than merely covered. What remains
 * uncovered is the two `@Preview` functions in each file — private, so no test can call them —
 * and the `onComplete` lambda inside `onboardingGraph`, which only a composed `NavHost` reaches.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The first-run flow: three screens and the ViewModel behind them.
//
// **The cleanest extraction in the epic so far — zero couplings to resolve.** Not luck: it is what
// a feature module looks like when it is written the way `docs/ARCHITECTURE.md` describes.
// `OnboardingViewModel` injects two settings *seams* (`AppSettings`, `LocationSettings`) rather
// than the whole `SettingsRepository`, and three domain ports (`DeviceLocationRepository`,
// `PermissionChecker`, `PowerSettings`) rather than the Android APIs behind them, so nothing it
// touches lives below `:core:domain`. The screens draw entirely from the design system and name
// destinations through `Route`.
//
// Compare `:feature:about`, extracted in the same PR, which needed six couplings unpicked. The
// difference is not size — onboarding is 1,047 lines against about's 1,642 — it is whether the
// code went through a seam or reached for what happened to be in the same module.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // The onboarding art is maintained through its @Preview functions, so previews compile here
    // rather than only in :app — the same reasoning `:core:ui` records for its atoms.
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core:domain")))
    // RecordingTelemetry — the Telemetry seam's recording fake, beside its port.
    testImplementation(testFixtures(project(":core:common")))
    testImplementation(libs.turbine)

    // The screen, the art and the graph are all exercised under Robolectric, so this module needs
    // the same harness `:feature:calendar` and `:feature:quran` use — including
    // `src/testDebug/resources/robolectric.properties`, which pins the SDK and the Application
    // class. A properties file is a resource of its source set and does not travel with the tests
    // that need it.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(testFixtures(project(":core:ui")))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
