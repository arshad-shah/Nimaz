plugins {
    id("nimaz.android.feature")
    // `AdaptiveMoreScreen`'s two-pane navigator persists which detail pane is showing, so its
    // args type is @Parcelize.
    id("kotlin-parcelize")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.about"

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
 * Locked at the standard floors. Measured at **94.1% lines / 83.8% branches** with the tests added
 * by #610 — the sixth Compose module in a row to hold 80/80, so nothing is softened here. The
 * module started this pass at 17.8% lines / 23.0% branches, with nine screen files at zero.
 *
 * **What is left uncovered is one thing, and it is structural.** 122 of the 143 uncovered lines
 * are the seven destination bodies inside `aboutGraph`: each builds a screen with its navigation
 * lambdas, and none of it runs until a composed `NavHost` reaches the destination — which resolves
 * `MoreViewModel`, `HelpViewModel` and `LicensesViewModel` through `hiltViewModel()` on a
 * *`NavBackStackEntry`* owner, and that path constructs a `HiltViewModelFactory` against the
 * hosting activity before the store is ever consulted. A library module has no Hilt-injected
 * activity to give it. `AboutGraphTest` covers the registration those bodies hang off — the
 * failure that actually ships, a destination that throws the moment somebody taps its row — and
 * `:app`'s instrumented `FeatureNavigationTest` exercises the bodies on a device.
 *
 * `AdaptiveMoreScreen` is **not** in that category, which is the one difference from
 * `:feature:quran` (#598) and `:feature:search` (#607), where the adaptive screens were left at
 * zero for the same reason. `hiltViewModel()` only reaches for a Hilt factory when the
 * `ViewModelStoreOwner` supplies a default one, so a plain owner whose `ViewModelStore` is
 * pre-seeded hands the mock back first — see `AdaptiveMoreScreenTest.seededOwner`. That covers the
 * phone/tablet split, which is the whole reason About, Help and More are one module.
 *
 * Nothing is excluded to reach these numbers; `COVERAGE_EXCLUSIONS` is untouched.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// About, Help and More — one module, because they are one destination. `AdaptiveMoreScreen` puts
// all three in a single list-detail scaffold, and `aboutGraph` registers every route for all
// three; there is no `HelpGraph.kt` or `MoreGraph.kt` to split along.
//
// **Six couplings to `:app` had to be unpicked**, none of which #565 anticipated. Worth reading
// as a list, because PRs 15–21 will hit the same shapes:
//
//  1. `SettingsViewModel` in `AdaptiveMoreScreen` — a feature→feature edge, and dead code. Deleted;
//     see `CrossFeatureViewModelGuardTest`.
//  2. `restartApp`, `internal` in `:app`'s NavGraph.kt — its only caller was (1), so it went with it.
//  3. `BuildConfig.VERSION_NAME` / `VERSION_CODE`, and
//  4. `R.mipmap.ic_launcher_foreground` — app identity, which a library cannot read either way.
//     Both now arrive through `LocalAppIdentity`, stated once by `MainActivity`.
//  5. `LocalInAppUpdateManager` / `UpdateState` — `InAppUpdateManager` stays in `:app` for good
//     (it holds an `Activity`), so the **port** moved and the implementation did not:
//     `AppUpdateController` in `:core:ui`, three of the class's seven members.
//  6. `SubtitleSpec` and `WorshipReminderContent` — shared presentation helpers that `:app`
//     screens also use, so they moved *down* to `:core:ui` rather than across.
//
// `UiError` looked like a seventh and was not — it had already moved to `:core:ui` in PR 10.
//
// **AboutLibraries stays in `:app`, deliberately.** The plugin generates `R.raw.aboutlibraries`
// from the *applying project's* runtime classpath, so applying it here would list only this
// module's dependencies — silently, with the licence screen simply showing fewer entries. That is
// why `LibraryRepositoryImpl` was the one repository of nineteen left behind in PR 9. This module
// depends on the `LibraryRepository` interface in `:core:domain` and Hilt binds the `:app` impl.
// `LicenceCatalogueTest` floors the entry count so a future move cannot shrink it quietly.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // Sharing — `ContentShareManager.sendEmail`/`shareBranded` from the More menu, and `QrCodes`
    // for the share sheet's QR. `:core:ui` used to carry both; `ShareAppSheet` and `NimazQrCode`
    // arrived here in the same change, which is what let `:core:ui` drop `zxing` entirely.
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

    // The More screen's two-pane scaffold.
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)

    // "Rate this app" opens the Play in-app review flow directly from the More menu.
    implementation(libs.app.review)
    implementation(libs.app.review.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core:domain")))
    // RecordingTelemetry — the Telemetry seam's recording fake, beside its port.
    testImplementation(testFixtures(project(":core:common")))
    testImplementation(libs.turbine)

    // The seven screens, the More menu and the graph run under Robolectric — the same harness
    // `:feature:calendar`, `:feature:onboarding`, `:feature:tools`, `:feature:search` and
    // `:feature:prayer` use, including `src/test/resources/robolectric.properties`, which pins
    // the SDK and the Application class. A properties file is a resource of its source set and
    // does not travel with the tests that need it.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(testFixtures(project(":core:ui")))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
