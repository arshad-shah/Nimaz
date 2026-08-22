plugins {
    id("nimaz.android.feature")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.tracker"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

// What the user did: prayer tracking, fasting, and the tasbih counter.
//
// One module for the same reason as `:feature:content` — `viewmodel/tracker` is a single package
// holding `PrayerTrackerViewModel`, `FastingViewModel` and `TasbihViewModel`, and splitting it
// would mean inventing a shared module underneath.
//
// **The `screens/prayer` cut is by ViewModel, not by directory.** Six of its nine files drive
// `viewmodel/tracker` and come here — the tracker and stats screens, qada, the day card and
// `PrayerDayStatus`. The two that drive `viewmodel/prayer` (`PrayerTimesScreen`,
// `MonthlyPrayerTimesScreen`) stay for PR 20, and `PrayerGraph.kt` splits with them: the three
// tracking routes now register in `trackerGraph`. Prayer *times* and prayer *tracking* share
// `PrayerRepository` through `:core:data`, which is the seam that lets them live apart.
//
// The `dua -> tracker` edge #569 flags was resolved a PR early, in PR 17: `DuaViewModel` injects
// `TasbihUseCases` from `:core:domain` and handles `DuaEvent.AddToTasbih`, so nothing in
// `:feature:content` names anything here.
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
    testImplementation(testFixtures(project(":core:ui")))
    testImplementation(testFixtures(project(":core:common")))

    // `FastingDayCardTest` is a Compose test under Robolectric; see
    // `src/testDebug/resources/robolectric.properties` for the SDK and Application pins.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
