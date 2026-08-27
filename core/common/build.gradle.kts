import org.gradle.api.tasks.PathSensitivity

plugins {
    id("nimaz.android.library")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.common"

    // AGP has its own test-fixtures support; applying the `java-test-fixtures` plugin that
    // :core:domain uses fails here with a duplicate `testFixturesImplementation` configuration.
    // Same source set, same consumption from :app — a fake needed on both sides of the seam is
    // published, not copied.
    testFixtures.enable = true

    testOptions {
        unitTests {
            // The Firebase wrappers are exercised under Robolectric, which needs real Android
            // resources and a real `Context` to hand them.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and
 * how the ratchet works.
 *
 * **80/80, both floors.** Nothing here draws, so the module does not carry the unreachable
 * `$dirty` branches that make `:feature:quran` soften its branch floor to 60%.
 *
 * Most of the climb here was the three Firebase wrappers, which were at 0% between them — not
 * because they are hard to test but because "it only no-ops" reads like nothing to assert. It is
 * the opposite: a missing `runCatching` in `AppAnalytics` is an exception thrown from
 * `BootReceiver` on a device that has never opened the app.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The genuinely shared, feature-agnostic half of the old `core/` grab bag: formatting helpers,
// the telemetry seam and the string seam. It sits **below** `:core:ui`, so nothing here may
// reference `R` — a string that must be resolved outside a composable goes through
// `StringProvider`, which is exactly why that interface exists. `coreCommonBoundary` enforces
// both rules on every build.
// `DateTimeFormattingTest` asserts that SHIPPED_LOCALES matches the `values-*` directories the app
// actually ships — a fact that lives in another module's `res/`. Gradle cannot know that from the
// source alone, so without this the task stays UP-TO-DATE when those directories change and the
// assertion never runs. It did exactly that when PR 10 of #551 moved the five translations into
// `:core:ui`: two full local sweeps reported the module green, and CI, which starts from an empty
// build directory, failed. An assertion only fires if its task runs.
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.layout.projectDirectory.dir("core/ui/src/main/res"))
        .withPropertyName("shippedLocaleResources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // `AppAnalytics`, `CrashReporter` and `PerfMonitor` are Firebase wrappers whose entire
    // contract is that they no-op safely when Firebase is absent — which is the state of every
    // build without `google-services.json`, and of every test. Robolectric is what makes that
    // testable: a real `Context` and a real `Bundle`, with Firebase genuinely not initialised.
    // See `src/test/resources/robolectric.properties` for the SDK and Application pins.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
}
