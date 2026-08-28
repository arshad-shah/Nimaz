plugins {
    id("nimaz.android.feature")
    // The two `Adaptive*Screen` navigators persist which detail pane is showing.
    id("kotlin-parcelize")
    // `TajweedParser` decodes the per-letter rule JSON with kotlinx.serialization. Without the
    // plugin `@Serializable` is an inert annotation: the module compiles, and `decodeFromString`
    // throws at runtime into `CrashReporter`, so every ayah renders with no tajweed colouring at
    // all and nothing reports it. The same shape as the missing `@HiltWorker` processor in PR 13
    // — and, as there, only a test that actually runs the code catches it.
    alias(libs.plugins.kotlin.serialization)
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.quran"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

/**
 * Locked. The reader, khatam, tafseer and bookmarks — screens, components and ViewModels — are
 * tested, and `check` now fails if that stops being true. See `COVERAGE_EXCLUSIONS` and
 * `coverageFloor` in `build-logic` for what is measured and how the ratchet works.
 *
 * The line floor is the agreed 80%, set at the standard rather than at whatever the module
 * reports today: a floor pinned to the current number turns every unrelated refactor into a
 * coverage failure.
 *
 * **The branch floor is 60% here and 80% on `:core:database`, and that is deliberate.** The
 * Compose compiler emits a `$dirty` bitmask branch per parameter of every restartable composable
 * — the skippability check — and no test can take both sides of one: they depend on which
 * arguments the *caller* changed between recompositions, not on anything the component does. A
 * Compose-heavy module therefore carries thousands of branches that are unreachable by
 * construction, which is why this module reports ~65% branches while covering 81% of its lines.
 * Setting 80% here would not measure more testing; it would measure how many composables the
 * module has. 60% is above where it sits, so the ratchet still catches a real regression.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.60)
}

// The reader, khatam and bookmarks — the largest feature, and the one with the most machinery
// under it: the whole Mushaf rendering stack comes too.
//
// **Every component in it is genuinely feature-owned, which took checking.** Three appeared to
// have consumers outside the feature — `MushafLineLayout` in `QuranSettingsScreen`,
// `MushafContinuousText`/`QuranAyahItem` in `:core:ui`'s `QuranTextFormat`, `AyahActionSheet` in
// `NimazToneColors` — and all three were **comments**. Believing the raw grep would have sent
// them to `:core:ui` for nothing, which is the opposite of the mistake the four previous feature
// PRs made. Comments get blanked before any consumer scan now.
//
// **`QuranDao` stays in `:core:database`.** Four repositories use it — Quran, Khatam, Tafseer and
// Library — so moving it here would drag three unrelated ones along.
//
// **The eight Quran fonts stay in `:core:ui`.** They are chosen at runtime from a settings value,
// which is the resource-shrinking exposure #551 flags; nothing in this module changes it.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    // Sharing — ayah actions, the saved list, tafseer and the reader all build a branded card,
    // and `TafseerPdfExporter` hands its PDF to `ContentShareManager.shareFile`.
    implementation(project(":core:share"))
    // `QuranViewModel` drives playback through `QuranAudioManager`, which lives here rather than
    // in this module because `MainActivity` holds one too — see the module's PR.
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:ui")))
    testImplementation(testFixtures(project(":core:common")))

    // Thirteen Compose component tests plus two screen tests, all under Robolectric; see
    // `src/testDebug/resources/robolectric.properties` for the SDK and Application pins.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
