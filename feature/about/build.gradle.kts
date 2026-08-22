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
}
