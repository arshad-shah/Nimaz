plugins {
    id("nimaz.android.feature")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.tools"

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

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and
 * how the ratchet works.
 *
 * **80/80, at 97.5% lines and 83.6% branches** — the third Compose module in a row to hold the
 * full branch floor, after `:feature:calendar` (82.4%) and `:feature:onboarding` (90.3%). The
 * unreachable `$dirty` bitmask branch the Compose compiler emits is one per parameter of every
 * restartable composable, so its weight scales with how many composables a module has rather than
 * with how much Compose is in it: there are two screens here, and the ratio never becomes the
 * number. `:feature:quran`, with ninety-odd files, is still the only module softened to 0.60.
 *
 * **Nothing is excluded.** Twenty-five lines are uncovered and they fall into three groups, none
 * of which is worth a production change to reach:
 *
 *  - the eleven lambda bodies inside `toolsGraph` (`popBackStack`, three `navigate` calls), which
 *    only a composed `NavHost` reaches — and composing one means giving both screens a Hilt
 *    ViewModel. `ToolsGraphTest` covers the registrations, which is where the crash-on-tap lives;
 *  - each screen's `viewModel: ZakatViewModel = hiltViewModel()` default argument, evaluated only
 *    when the graph omits it;
 *  - `ZakatViewModel.calculate`'s `catch`. `ZakatCalculator.calculate` is arithmetic over
 *    `Double`s with no division by a user-supplied figure, so no input reaches it. The handler is
 *    still right to exist — it is what keeps a future arithmetic fault an inline error beside the
 *    form rather than a crash mid-calculation — and `ZakatPersistenceTest` covers the identical
 *    shape on all four write paths, where failures do happen.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The zakat calculator and its history — `screens/zakat`, `screens/tools` (the graph) and
// `viewmodel/tools`.
//
// **Nothing to unpick.** Second module in a row after `:feature:onboarding`, and for the same
// reason: `ZakatViewModel` reads the `ZakatSettings` seam rather than `SettingsRepository`, and
// everything the screens import already lives below them — `formatCurrency` and
// `currencySymbolOf` in `:core:common`, `ContentShareManager`/`Shareables` and `UiError` in
// `:core:ui`. At 1,324 lines it is larger than `:feature:about`'s screens, which needed six
// couplings resolved; size is not what decides this.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // Sharing — the zakat calculator shares its result as a branded card.
    implementation(project(":core:share"))

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

    // The two zakat screens and the graph are exercised under Robolectric, the same harness
    // `:feature:calendar` and `:feature:onboarding` use — including
    // `src/testDebug/resources/robolectric.properties`, which pins the SDK and the Application
    // class. A properties file is a resource of its source set and does not travel with the
    // tests that need it.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(testFixtures(project(":core:ui")))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
