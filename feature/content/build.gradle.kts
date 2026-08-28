plugins {
    id("nimaz.android.feature")
    // The three `Adaptive*Screen` navigators persist which detail pane is showing, so their args
    // types are @Parcelize.
    id("kotlin-parcelize")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.content"

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

// **80/80, and the branch number was not free.** This is the seventh Compose module to hold the
// standard branch floor, and the closest any of them has come to missing it: the module reports
// **80.4% branches**, four above the gate. That margin is thin on purpose rather than by
// accident — the alternative was the softened 0.60 floor, and the arithmetic does not justify it
// here. Of the 255 branches still missing, **125 are the Compose compiler's `$dirty` bitmask**:
// one per parameter of every restartable composable, on the signature line and its closing
// paren, and neither side reachable from a test because which side runs depends on what the
// *caller* changed between recompositions. Discount those and the module stands at 89.4%. The
// remaining 130 are spread thinly, and three pockets account for half of them:
//
//   - **`QaidaAudioManager`'s `Player.Listener`** (12) — `STATE_ENDED`, the error arm, and the
//     `MEDIA_ITEM_TRANSITION_REASON_AUTO` branch that decides which key was *heard*. Robolectric
//     has no media pipeline, so the player never leaves `STATE_IDLE` and no callback ever fires.
//     Reaching them would mean reflecting into ExoPlayer's private listener set, which breaks on
//     a media3 bump and fails far from its cause. The consumer is covered instead:
//     `QaidaReaderViewModel` credits a cell from `completions` and from nowhere else.
//   - **`DuaViewModel.filterAndSortCategories`'s search arms** (10) — genuinely dead today.
//     `DuaCollectionUiState.searchQuery` is only ever written as `""`; no event sets it to
//     anything else, because `DuasCollectionScreen`'s search action navigates to
//     `Route.DuaSearch`, which is `:feature:search`'s screen against a different ViewModel.
//     `DuaCollectionSortTest` says so in place of a test that would have to reach past the
//     public surface to pretend otherwise. Wiring a query in is a change to make deliberately.
//   - **`QaidaPlayLineButton`** (4) — behind `QAIDA_AUDIO_UI_ENABLED`, which is `false` while the
//     recordings are regenerated. `QaidaLettersScreenTest` asserts the control is absent, and
//     that assertion is the thing to invert rather than delete when the flag flips.
//
// **Nothing was excluded to reach these numbers** — no `COVERAGE_EXCLUSIONS` entry was added or
// widened, and the list is shared with every locked module, so widening it would move theirs too.

// The library: duas, hadith, qaida, the ninety-nine names, the names of the Prophet, the prophets,
// and the catalog shell they share. Eight `screens/` packages in one module.
//
// **They move together because they cannot move apart.** `viewmodel/content` is a single package
// holding `DuaViewModel`, `HadithViewModel`, `QaidaReaderViewModel`, `ProphetViewModel`,
// `AsmaUlHusnaViewModel`, `AsmaUnNabiViewModel` and the shared `CatalogEvent`, consumed by all
// eight. Splitting them would mean eight modules importing a ninth, or eight copies of the catalog
// abstraction. This is the concrete case behind the rule that **the module boundary follows the
// ViewModel axis, not the `screens/` axis**.
//
// It takes the components those screens own — `NameCard`, `NameDetailHeader`,
// `NameDetailComponents`, `NamesAccent` and four of the five `Qaida*` organisms.
//
// **Four components #568 listed as moving here could not**, because their consumers are outside
// the feature, and the compiler is what says so:
//
//   HadithOfTheDayCard   read by TodayCarousel/TodayInfoCards — `screens/home` stays in `:app`
//   DuaOfTheMomentCard   read *only* by those two; no dua screen touches it at all
//   CitationRow          read only by `screens/quran`, which becomes `:feature:quran` in PR 19
//   QaidaCoursePath      read by KhatamJourneyTrail, which follows `screens/khatam` to `:feature:quran`
//
// All four went to `:core:ui` instead. The last two would have been `:feature:* -> :feature:*`
// edges, which `moduleBoundary` rejects outright; the first two would simply have broken `:app`.
// Fourth PR in a row where "used by the feature" turned out not to mean "used only by the
// feature" — enumerate consumers before moving a component, every time.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // Sharing — the hadith and dua readers build a branded card through `Shareables`.
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

    // The three `Adaptive*Screen` two-pane wrappers this feature owns — dua, hadith and names.
    // Six of the seven in `screens/adaptive` each compose a single feature's screens and travel
    // with it; only `AdaptiveSettingsScreen`, `AdaptiveQuranScreen` and `AdaptiveKhatamScreen`
    // are still in `:app`, waiting for their own modules.
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)

    // `QaidaAudioManager` came with the feature. It sat in `data/audio` beside the adhan and
    // Quran players, and the epic's plan sends that whole directory to PR 20 — but only
    // `QaidaReaderViewModel` uses this one, and PR 20 is `:feature:prayer`, where qaida audio
    // does not belong. `data/audio` turned out to be three features' audio in one directory.
    implementation(libs.media3.exoplayer)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:ui")))
    testImplementation(testFixtures(project(":core:common")))

    // Fifteen Compose component tests — the Qaida and Names families — under Robolectric. They
    // were stranded in `app/src/testDebug` when PR 17 moved their subjects here, and kept
    // compiling because nothing they touch is `internal`; see `FeatureTestsLiveWithSubjectTest`
    // in `:app` for what now catches that. `src/testDebug/resources/robolectric.properties`
    // carries the SDK and Application pins.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
