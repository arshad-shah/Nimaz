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
