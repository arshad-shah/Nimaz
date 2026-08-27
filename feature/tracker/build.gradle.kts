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

nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// **80/80.** The eighth Compose module in a row to hold the standard branch floor, at
// **81.3% branches** and **91.9% lines** — and the branch number was not free. Of the 280
// branches still missing, **131 are the Compose compiler's `$dirty` bitmask**: one per parameter
// of every restartable composable, on the signature line, its parameter lines and its closing
// paren, and neither side reachable from a test because which side runs depends on what the
// *caller* changed between recompositions. Discount those and the module stands at **89.1%**.
//
// The remaining 149 are spread thinly. Four pockets account for most of them, and each is
// unreachable rather than untested:
//
//   - **`TasbihCounterUiState.autoLap` is never false** (about 10 branches, across the counter's
//     lap arithmetic in `TasbihViewModel` and the capsule's `goalReached` in `TasbihScreen`).
//     No `TasbihEvent` sets it and no setting writes it, so the manual-lap arm is dead today.
//     Wiring a control to it is a change to make deliberately, not something to fake from a test.
//   - **`TasbihPresetsUiState.selectedCategory` is never set** (4). `ChooseDhikrScreen` filters
//     by its own tab state rather than through the ViewModel, so the state's category arm has no
//     writer. `TrackerDerivedStateTest` pins the derivation anyway — it is the guard that stopped
//     a re-emission of the presets flow silently dropping an active filter, and it is worth
//     keeping correct against the day something does set it.
//   - **The `PrayerStatus.PENDING`/`NOT_PRAYED` arms of `PrayerTrackerDayCard`'s picker
//     mappings** (3). `PICKER_STATUSES` holds only the four assertions a user can make, so the
//     two "nobody has said" values never reach `pickerLabel()` or `displayed()`. They exist
//     because `PrayerStatus` has six values and a `when` over it must be exhaustive.
//   - **`PrayerStatsScreen`'s date-parse fallback** (4). The `catch` runs only when a stored
//     `startDate` cannot be resolved to a `LocalDate`; neither `Long.MIN_VALUE` nor
//     `Long.MAX_VALUE` actually throws on the way through, so there is no value a test can
//     supply to reach it. It stays as the defence it is.
//
// The Hijri arm of `FastingComingUp`'s event list (17) is the one genuinely date-dependent
// pocket: which of Ashura, Arafah, Shawwal and mid-Sha'ban fall ahead of "today" changes with
// the day the suite runs, so some of those `takeIf`s take only one side on any given run.
//
// **Nothing was excluded to reach these numbers** — no `COVERAGE_EXCLUSIONS` entry was added or
// widened, and that list is shared with every locked module, so widening it would move theirs
// too.

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
