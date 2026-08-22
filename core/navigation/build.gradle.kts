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
}
