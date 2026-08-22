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
}
