plugins {
    id("nimaz.android.feature")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.home"

    testOptions {
        unitTests {
            // Parity with every other feature module, and load-bearing rather than boilerplate:
            // `HomeViewModel` reports through the static `CrashReporter`/`AppAnalytics` before it
            // updates state, and without default values those hit an uninitialised Firebase and
            // throw — so the state update never happens and a test fails for a reason unrelated
            // to what it asserts.
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and
 * how the ratchet works.
 *
 * **80/80, at 89.2% lines and 80.7% branches.** The full branch floor was not a foregone
 * conclusion here: the unreachable `$dirty` bitmask branch the Compose compiler emits is one
 * *per parameter of every restartable composable*, and its weight scales with how many
 * composables a module has — twenty-one components plus two screens is well past the six that
 * let `:feature:calendar` hold 80, and it is the reason `:feature:quran` softened to 60. It
 * clears anyway, because Home's components arrived with thirty test classes and 205 tests
 * behind them. Measured, then locked; not guessed.
 *
 * **The margin on branches is 0.7 points.** Add tests with the change, not after it.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// Home: the first screen the app opens on, and the last surface to leave `:app`.
//
// **Nothing had to be unpicked.** Every `:app` symbol the group imported was declared inside the
// group — the screen, the graph, `viewmodel/home`, and the twenty-one components only Home
// renders. What is left in `:app` after this is 30-odd files with no `presentation/` directory at
// all, which is why `PresentationSourceRoots.ALL` had a root *removed* here rather than only
// added to: `assertAllExist` fails on a listed root that is no longer a directory.
//
// **Four things stayed in `:core:ui` that look like they belong here**, and one of them cannot
// move at all:
//
//   · `PrayerVisuals.kt` — `getPrayerColor`/`getPrayerIcon`/`getArabicPrayerName` are read by
//     `:core:ui`'s own `PrayerTimeCard`. Moving it would make `:core:ui` depend on this module,
//     which `moduleBoundary` fails the build on. Not a judgement call; a compile error.
//   · `EventCard.kt`, `EventCardVisuals.kt`, `DuaOfTheMomentCard.kt` — Home is their only
//     consumer *today*. That is the sentence `docs/ARCHITECTURE.md` records getting wrong five
//     times in #551: "used by the feature" is not "used only by the feature". They are generic
//     by construction — an event card with emphasis variants, a per-occasion token table, a
//     content-preview molecule whose sibling `HadithOfTheDayCard` already has a second
//     lookalike in `:feature:content` — and none of them blocks this extraction. Moving them is
//     a decision to take on its own evidence, not as a rider on a module move.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // `HomeViewModel` works in `kotlinx.datetime` — it reads the sunnah night window as
    // `Instant`s and converts them to the reader's zone to decide whether the night-worship card
    // is showing tonight's or tomorrow's. Same reason `:feature:prayer` declares it.
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":core:domain")))
    // RecordingTelemetry — the Telemetry seam's recording fake, beside its port.
    testImplementation(testFixtures(project(":core:common")))

    // The screen, the graph and every component run under Robolectric, so this module needs the
    // same harness the other feature modules use — including `src/test/resources/
    // robolectric.properties`, which pins the SDK and the Application class.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(testFixtures(project(":core:ui")))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
