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

    // Same reasoning, one file rather than a tree: `PrayerAlertChannelTest` reads `:app`'s
    // manifest to check that the FCM `default_notification_channel_id` meta-data still names
    // `NimazChannels.ANNOUNCEMENTS`. A manifest cannot reference a Kotlin constant, so that
    // literal is the one unavoidable duplicate of a channel id — and an undeclared input is an
    // assertion that stops running the moment the file it reads is the only thing that changed.
    inputs.file(rootProject.layout.projectDirectory.file("app/src/main/AndroidManifest.xml"))
        .withPropertyName("appManifest")
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
    // `api`, not `implementation`, and the BOM with it. `PerfMonitor.newTrace` returns a
    // `com.google.firebase.perf.metrics.Trace?` and `PerfMonitor.stop` takes one, so any module
    // using the start/stop pair — rather than the `trace { }` lambda — needs the type on its own
    // compile classpath. Kept as `implementation` it was not there, and the caller failed with
    // *"Cannot access class Trace"*: a public signature referring to a type the module does not
    // expose. The same slip `:core:ui` records for `WindowSizeClass`, which is why the BOM is
    // `api` here too — without it the artifact resolves in a consumer with no version.
    //
    // `analytics` and `crashlytics` stay `implementation` on purpose: `AppAnalytics`,
    // `CrashReporter` and `Telemetry` take and return only `String`, `Long` and `Throwable`, so
    // no Firebase type reaches a consumer's signature and the seam holds.
    api(platform(libs.firebase.bom))
    api(libs.firebase.perf)

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
