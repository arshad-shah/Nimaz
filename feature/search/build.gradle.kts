plugins {
    id("nimaz.android.feature")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.search"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

/**
 * Locked at the standard floors. Measured at **91.4% lines / 87.0% branches** with the tests
 * added by #607 — the fourth Compose module in a row to hold 80/80, so nothing is softened here.
 *
 * **What is left uncovered is one thing, and it is structural.** Ninety of the module's 107
 * uncovered lines are the four destination bodies inside `searchGraph`: each one calls
 * `SearchScreen(...)` with seven navigation lambdas, and none of it runs until a composed
 * `NavHost` reaches the destination — which constructs `SearchViewModel` and `AskViewModel`
 * through `hiltViewModel()` and therefore needs a Hilt-injected activity this module has no way
 * to build. `SearchGraphTest` covers the registration those bodies are attached to (the failure
 * that ships: a destination that throws on tap); `:app`'s instrumented suite is what exercises
 * the bodies. The rest is the two `hiltViewModel()` default arguments on `SearchScreen`, the
 * `null -> SearchFilter.ALL` arm of `LibrarySource?.asFilter()` — unreachable because its only
 * call site is already inside a `defaultScope != null` check — and a handful of `when`-merge
 * lines the compiler emits with no statement of their own. Nothing is excluded to reach these
 * numbers; `COVERAGE_EXCLUSIONS` is untouched.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// Local library search, and the opt-in "Ask with Proof" screen on top of it.
//
// **The only feature with a network dependency and a cross-repo contract — and none of it is in
// this module.** The `search-assist` client, its DTOs and `IntegrityTokenProvider` all went to
// `:core:data` in PR 9; `worker/` is not part of this epic at all. What moves here is the screen
// and the two ViewModels behind it.
//
// The `BuildConfig` trap #567 warns about is real and already handled. `AI_WORKER_BASE_URL` and
// `PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER` are read in exactly one place — `core/di/AiModule.kt`,
// which stays in `:app` until PR 22 — because PR 9 inverted both reads into constructor
// parameters rather than duplicating app identity into a library's build file. Nothing here can
// see `com.arshadshah.nimaz.BuildConfig`, and nothing here needs to.
//
// **Proof resolution reads content this module does not own.** Cited Quran and Hadith refs are
// resolved locally against the FTS4 index in the content artifact — content that will belong to
// `:feature:quran` and `:feature:content`. It reaches them through `:core:domain` repositories,
// never by depending on those modules, and `moduleBoundary` makes the alternative impossible.
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
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:common")))

    // The search screen, the Ask-with-Proof cards and the graph run under Robolectric — the
    // same harness `:feature:calendar`, `:feature:onboarding` and `:feature:tools` use,
    // including `src/testDebug/resources/robolectric.properties`, which pins the SDK and the
    // Application class. A properties file is a resource of its source set and does not travel
    // with the tests that need it.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(testFixtures(project(":core:ui")))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
