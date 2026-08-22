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
    testImplementation(testFixtures(project(":core:common")))
}
