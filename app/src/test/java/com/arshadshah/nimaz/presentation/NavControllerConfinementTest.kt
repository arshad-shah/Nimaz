package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * `NavController` belongs to the graph, not to the screens.
 *
 * ## Why this is worth a test
 *
 * A screen that holds a `NavController` can navigate anywhere, which means it depends on every
 * destination in the app. That is precisely what stops a feature moving into its own module, and
 * it is why #563 lists these screens as the thing to fix before `NavGraph.kt` can be split at
 * all: eleven feature modules cannot each import a graph that lives above them.
 *
 * Seven screens held one — every file under `presentation/screens/adaptive`, the large-screen
 * list-detail family. Between them they used exactly two operations, `navigate(Route)` 68 times
 * and `popBackStack()` three times, so the lift to the `onNavigate` / `onBack` lambdas that the
 * other 84 screens already took was one mechanical pattern rather than seven investigations.
 *
 * Nothing in the compiler stops the eighth from appearing. `androidx.navigation` is on the
 * classpath of anything that can see a `Route`, so re-adding a `NavController` parameter to a
 * screen compiles perfectly well and un-picks the decomposition quietly, one screen at a time.
 *
 * ## Scope, and the one exemption
 *
 * `NavGraph.kt` owns the controller, and since PR 12 of #551 so do the eleven `*Graph.kt` feature
 * extensions. That exemption was added deliberately and is worth justifying, because I added this
 * test two commits before needing to widen it.
 *
 * The rule that matters is that a **screen** must not navigate arbitrarily — that is what makes it
 * depend on every destination in the app. A `NavGraphBuilder.quranGraph(...)` extension *is* the
 * navigation wiring; holding a controller there is its whole job, and `NavController` is an
 * `androidx` type, so it creates no dependency on `:app` and will travel into `:feature:quran`
 * unchanged.
 *
 * #563 sketches `quranGraph(onNavigate: (Route) -> Unit)` instead. That does not fit: 11 of the
 * 158 `navigate` calls in those blocks pass a `NavOptionsBuilder` — `popUpTo`, `launchSingleTop` —
 * which `(Route) -> Unit` cannot express, and flattening them would change back-stack behaviour
 * with no build error and no test failure, surfacing as a wrong back button several screens later.
 *
 * The exemption is keyed on the filename, so a screen cannot acquire it by accident, and
 * [theGraphItselfStillOwnsOne] stops the whole test passing by the type disappearing.
 */
class NavControllerConfinementTest {

    private companion object {
        /**
         * Every module's presentation sources. Kept in [PresentationSourceRoots] rather than here
         * because four tests need the same list, and PR 14 of #551 broke three of them at once by
         * moving `screens/{about,help,more,onboarding}` into feature modules.
         */
        val SCREEN_ROOTS = PresentationSourceRoots.NAVIGABLE

        /**
         * The one file allowed to own the controller, relative to `:app`. Named as a *file*, not
         * a directory: PR 11 of #551 found `MaterialTextFieldGuardTest` asserting only that its
         * directory existed, which stayed true after the directory was emptied, so the scan went
         * on passing over almost nothing.
         */
        const val GRAPH = "src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt"

        /** A floor, so a wrong root cannot make this pass having read nothing. */
        const val MINIMUM_FILES = 200

        val NAV_CONTROLLER = Regex("""\bNav(Host)?Controller\b""")

        /**
         * The eleven feature graphs of PR 12. A floor rather than an equality, so adding a twelfth
         * feature does not fail this — but re-merging them into `NavGraph.kt` does.
         */
        const val EXPECTED_FEATURE_GRAPHS = 11
    }

    private fun screenSources(): List<File> {
        PresentationSourceRoots.assertAllExist(SCREEN_ROOTS)
        return PresentationSourceRoots.sources(SCREEN_ROOTS)
            .filterNot { it.name.endsWith("Graph.kt") }
    }

    @Test
    fun `the scan reaches every screen root`() {
        val scanned = screenSources().size
        assertWithMessage(
            "scanned $scanned files across $SCREEN_ROOTS — a scan that finds nothing passes the " +
                "assertion below having checked nothing"
        ).that(scanned).isAtLeast(MINIMUM_FILES)
    }

    @Test
    fun `no screen names NavController`() {
        val offenders = screenSources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> NAV_CONTROLLER.containsMatchIn(line) }
                .map { (index, line) -> "${file.path}:${index + 1}  ${line.trim()}" }
        }.sorted()

        assertWithMessage(
            "These screens name a NavController:\n" +
                offenders.joinToString("\n") { "  $it" } +
                "\n\nA screen holding a controller can navigate anywhere, so it depends on every " +
                "destination in the app — which is what stops its feature moving into its own " +
                "module (#563). Take `onNavigate: (Route) -> Unit` and `onBack: () -> Unit` " +
                "instead, as the other 84 screens do, and let the graph supply them."
        ).that(offenders).isEmpty()
    }

    @Test
    fun theGraphItselfStillOwnsOne() {
        // The complement, so this test cannot pass by the type having vanished entirely — which
        // would mean the app no longer navigates, not that the rule is satisfied.
        val graph = File(GRAPH)
        assertWithMessage("NavGraph.kt not found at ${graph.absolutePath}").that(graph.isFile).isTrue()
        assertThat(graph.readText()).contains("rememberNavController()")

        // And the exemption is not covering an empty set: the feature graphs must exist and must
        // be the things holding the controller. If they vanished, the check above would still pass
        // while the decomposition had been undone.
        val graphFiles = PresentationSourceRoots.sources(SCREEN_ROOTS)
            .filter { it.name.endsWith("Graph.kt") }
        assertWithMessage("no *Graph.kt feature extensions found — was NavGraph.kt re-merged?")
            .that(graphFiles.size).isAtLeast(EXPECTED_FEATURE_GRAPHS)
    }
}
