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
}

// The genuinely shared, feature-agnostic half of the old `core/` grab bag: formatting helpers,
// the telemetry seam and the string seam. It sits **below** `:core:ui`, so nothing here may
// reference `R` — a string that must be resolved outside a composable goes through
// `StringProvider`, which is exactly why that interface exists. `coreCommonBoundary` enforces
// both rules on every build.
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
}
