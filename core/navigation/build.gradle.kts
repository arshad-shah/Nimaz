plugins {
    id("nimaz.android.library")
    id("nimaz.android.compose")
    alias(libs.plugins.kotlin.serialization)
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.navigation"

    testOptions {
        unitTests {
            // `taggedComposable` is the one thing here that *runs*, and the only way to see it
            // apply its tag is to compose a real `NavHost` — so this module has one Robolectric
            // test. `src/testDebug/resources/robolectric.properties` pins the SDK and the
            // Application class; a properties file is a resource of its source set and does not
            // travel with the tests that need it.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

/**
 * Locked. Every feature module declares its destinations through this one, so a regression here
 * is a blank screen somewhere else; `check` now fails if its coverage slips — see
 * `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured.
 *
 * **80/80, both floors.** The module is Compose-adjacent but draws nothing, so it does not carry
 * the unreachable `$dirty` branches that make `:feature:quran` soften its branch floor to 60%.
 *
 * Two things here can never be covered, and both are worth knowing before someone tries:
 * `taggedComposable` is `inline`, so its bytecode lands in the *caller's* module and this
 * module's own copy is never executed however many tests compose it; and the 94 `@Serializable`
 * route declarations are a vocabulary, not behaviour. Together they are the ceiling, which is
 * why the floor is the standard 80 rather than the 84 the module reports today.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The route vocabulary, and nothing that renders one.
//
// Every feature module needs this to declare its destinations, and nothing can depend on it until
// it exists — which is why it is its own PR rather than part of the NavGraph decomposition that
// follows in PR 12. `NavGraph.kt` itself stays in `:app`: it imports 75 presentation types, and
// unpicking that is the *next* milestone's work, not this one's.
//
// **No import of `presentation.screens` or `presentation.viewmodel` may appear here.** That is the
// module's entire point, and `NavigationHasNoPresentationImportsTest` fails on one.
//
// Note what is deliberately absent: a dependency on `:core:ui`. `Route` carries the *identity* of
// a destination, never its label — see `NamesTab`, whose `@StringRes` stayed behind in `:app` for
// exactly that reason. A navigation → ui edge would be hard to undo once eleven feature modules
// depend on both.
dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    api(libs.kotlinx.serialization.json)

    // `taggedComposable` is a `NavGraphBuilder` extension that wraps a destination in a tagged
    // Box, so this module needs the navigation and Compose APIs even though it draws nothing.
    api(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    // `Box`/`fillMaxSize` live in compose.foundation.layout, which the catalogue has no alias for;
    // material3 brings it transitively and is what every consumer of this module already has.
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)

    // One Robolectric test, for `taggedComposable`. `CLAUDE.md` requires every destination to be
    // wired with it and `check_docs.py`'s NAV-04 enforces that they are — but nothing checked the
    // helper itself still applies the tag, and if it stopped, every instrumented navigation test
    // would fail on an emulator with no JVM test having said a word.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
