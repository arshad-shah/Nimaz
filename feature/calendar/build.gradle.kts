plugins {
    id("nimaz.android.feature")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.calendar"

    testOptions {
        unitTests {
            // Parity with `:app`, `:core:ui` and the other feature modules. A ViewModel that
            // reports through the static `CrashReporter`/`AppAnalytics` before updating its state
            // needs this, or the reporter throws on an uninitialised Firebase and the state update
            // never happens — which is how two `:feature:onboarding` tests failed in PR 14 for a
            // reason unrelated to what they assert.
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

// The Islamic calendar — one screen, one ViewModel.
//
// Small enough to question, and it still earns its own module: it owns a distinct ViewModel
// package and has zero cross-feature imports, so folding it into a neighbour would create the
// coupling the split exists to remove. It reads `IslamicEvent`, which `fasting`, `prayer` and
// `settings` also read — but that is a `:core:domain` model, so it is not a feature-to-feature
// edge, and `IslamicEventRepositoryImpl` stays in `:core:data` like every other repository.
//
// `CalendarDayState` and `CalendarLegendItem` come from `presentation/foundation/calendar`, which
// moved to `:core:ui` in PR 10 — worth checking rather than assuming, since the package name
// still reads like `:app`.
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

    // `IslamicEventCardTest` is a Compose component test under Robolectric, so this module needs
    // the same harness `:core:ui` uses — including `src/testDebug/resources/robolectric.properties`,
    // which pins the SDK and the Application class. A properties file is a resource of its source
    // set and does not travel with the tests that need it, which is how `:core:database` lost its
    // pin in PR 7.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
