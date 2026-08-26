plugins {
    id("nimaz.android.library")
    id("nimaz.android.compose")
    // `TajweedParser` decodes the per-letter rule JSON with kotlinx.serialization. Without the
    // plugin `@Serializable` is an inert annotation: the module compiles and `decodeFromString`
    // throws at runtime into `CrashReporter`, so every ayah renders with no tajweed colouring and
    // nothing reports it. It came from `:feature:quran` in PR 21 of #551, plugin and all — see the
    // dependency note below for why it moved.
    alias(libs.plugins.kotlin.serialization)
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.ui"

    // The Compose test harness is published from here rather than copied into every module that
    // renders a composable under Robolectric. AGP has its own test-fixtures support; applying the
    // `java-test-fixtures` plugin that `:core:domain` uses fails here with a duplicate
    // `testFixturesImplementation` configuration.
    testFixtures.enable = true

    testOptions {
        unitTests {
            // The Compose UI tests for the atoms run under Robolectric and read merged
            // Android resources — which this module now owns. `src/testDebug/resources/
            // robolectric.properties` pins the SDK and the Application class; a properties
            // file is a resource of its source set and does not travel with the tests that
            // need it, which is how :core:database lost its pin in PR 7.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        // The atoms and the theme are the design system's own API surface; a preview of one is
        // part of maintaining it, so tooling previews compile here rather than only in :app.
        compose = true
    }
}

// The design system, and the resources it draws from.
//
// This is the first module in the epic that owns `res/`, which changes how every other module
// spells `R`. With `android.nonTransitiveRClass=true` a module's `R` holds only its *own*
// resources, so once `strings.xml` lives here, `com.arshadshah.nimaz.R` no longer has
// `R.string.*` in it and every consumer imports `com.arshadshah.nimaz.core.ui.R` instead. That is
// a mechanical rename across 229 files, and it is the reason the diff is large rather than deep:
// the symbol after the `R.` never changes.
//
// What does NOT live here, despite #561 listing it under "every resource":
//   · `res/xml/`     — backup_rules, data_extraction_rules, file_paths, locales_config and the six
//                      `*_widget_info.xml`. All are referenced from `AndroidManifest.xml`, which
//                      is an application concern; the widget metadata goes to `:feature:widget`.
//   · `res/drawable/`, `res/layout/` — ten `ic_widget_*`, the `ic_stat_nimaz` notification icon and
//                      seven widget preview layouts. Widget and notification assets, not the
//                      design system.
//   · `res/mipmap-*/` — launcher icons; app identity.
//   · `res/raw/`      — holds only `keep.xml`. The `aboutlibraries` JSON it protects is *generated*
//                      by the AboutLibraries Gradle plugin into the applying project, so both it
//                      and the keep rule stay with `:app`. See `LibraryRepositoryImpl`, which
//                      documents the release-only failure that made the keep rule necessary.
// `TajweedParser` lives here as of PR 21 of #551, overturning PR 19's placement in
// `:feature:quran`. It moved because `TajweedLegendSheet` had to: `QuranSettingsScreen` shows the
// legend and left for `:feature:settings`, while `QuranReaderScreen` shows it too and stayed in
// `:feature:quran` — two feature modules, so the component goes down rather than across, and the
// parser it reads goes with it. PR 19 was right on the evidence it had (one consumer); the
// evidence changed one PR later. **"Used by the feature" is not "used only by the feature", and a
// module that is the only consumer today may not be next month.**
//
// It belongs here on its merits too: it returns an `AnnotatedString` of `SpanStyle`s built from
// `NimazColors.TajweedColors`, which is a `:core:ui` type. `scripts/check_tajweed_contrast.py`
// reads the theme files, not the parser, so the CI contrast gate is unaffected.
dependencies {
    // `api`, not `implementation`: `createComponentComposeRule()` returns a
    // `ComposeContentTestRule` and `setThemedContent` takes a `@Composable` lambda, so every
    // consumer needs both on its own compile classpath. The `implementation`/`api` slip that PR 15
    // caught on `WindowSizeClass` was this exact shape.
    testFixturesApi(platform(libs.androidx.compose.bom))
    testFixturesApi(libs.androidx.compose.ui.test.junit4)
    testFixturesApi(libs.androidx.compose.ui)
    testFixturesApi(libs.androidx.compose.material3)

    implementation(libs.kotlinx.serialization.json)
    api(project(":core:domain"))
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // `api`, not `implementation`: `currentWindowSizeClass()` is public here and returns
    // `androidx.window.core.layout.WindowSizeClass`, which this artifact brings in. Kept as
    // `implementation` the type stayed off consumers' compile classpath, so every caller in a
    // module that did not happen to declare the adaptive artifacts itself failed with
    // "Cannot access class WindowSizeClass" — a public signature referring to a type the module
    // does not expose. `:feature:about` masked it by declaring the adaptive deps for its own
    // list-detail scaffold; `:feature:tools` and `:feature:calendar` do not, and found it.
    api(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.zxing.core)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    // The Compose UI test harness the component tests in `src/testDebug` run against. It is
    // debug-only because `ui-test-manifest` supplies an Activity that only a debug variant has.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// **80/80 — the ninth Compose module in a row to hold the standard branch floor, and the largest
// Compose surface in the repo by an order of magnitude.** 13,000 measurable lines, 316 classes,
// 143 files: the whole design system, every atom, molecule and organism, plus the theme, the type
// scale, the share builders and the drawn surfaces.
//
// **It clears both floors with the thinnest margin of any module so far**: 80.5% lines (61 above)
// and 80.5% branches (24 above). That is deliberate rather than lucky — the arithmetic below is
// what the last few tests were aimed at — and it is worth stating plainly, because a module at
// half a point of margin is one unrelated refactor from a red gate. **Add tests with the change,
// not after it.**
//
// ## Why the branch number is the hard one here, and why it is still 80
//
// Of the branches this module still misses, **183 are the Compose compiler's `$dirty` bitmask** —
// one per parameter of every restartable composable, emitted on the signature line, and neither
// side reachable from a test because which side runs depends on what the *caller* changed between
// recompositions (#604). With 143 files of composables that is the largest such accumulation in
// the repo, and it is exactly the shape that argued `:feature:quran` down to a 0.60 branch floor.
//
// It does not force one here, for a reason worth writing down: the *other* Compose-generated
// branch — the `$default` parameter mask — **is** reachable, by calling a composable both with its
// defaults and with explicit arguments. That is a real test rather than a coverage trick: a
// parameter read into a local and then not passed on is a caller's choice silently ignored, and it
// is only ever visible on the one screen that sets it. Several of the tests in `src/testDebug` are
// deliberately option sweeps for that reason, and they are what made 80% branches reachable
// without softening anything.
//
// ## What is permanently uncovered, and why nothing was excluded to hide it
//
// `COVERAGE_EXCLUSIONS` is untouched. Two groups are counted in full against both floors and
// cannot be driven:
//
//   - **`@Preview` and `*Showcase` functions — 1,979 lines, 15.2% of the module, all of it at 0%.**
//     This is the design system, so previews *are* part of maintaining it: 263 `@Preview`
//     annotations across 86 of the 143 files, against zero in `:feature:about` and `:feature:tools`.
//     They are `private` by convention and calling them from a test would pin the tooling rather
//     than the product. Counting them, the rest of the module has to clear **94.4%** for the
//     module to read 80 — which is what it now does. `NimazCalendarShowcasePreviews.kt` is the
//     extreme case: 145 lines that are nothing but tooling.
//   - **The `$dirty` bitmask above**, 183 branches.
//
// Everything else that looked untestable turned out not to be, and the two techniques are worth
// naming because they account for most of the movement: the app's **drawn** surfaces — the
// fractured shamsa, the prayer sky, the compass dial, the qibla beam, the Islamic pattern, the
// khatam ring, the tree's margin rules — are covered by drawing them into a *software*
// `android.graphics.Canvas` under `@GraphicsMode(NATIVE)` and reading the pixels back
// (`testDebug/testing/SoftwareCanvas.kt`), and the components that clear their own semantics are
// addressed by the announcement they publish rather than by their text.
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}
