package com.arshadshah.nimaz.presentation

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
 * ## Scope
 *
 * Only `rememberNavController()` in `NavGraph.kt` is allowed to name the type. When `NavGraph.kt`
 * is split into per-feature graph extensions, the `NavHost` and its controller stay in `:app` —
 * so this test should keep passing unchanged, and if it starts failing during that split, a graph
 * function has taken a controller it should have taken a lambda.
 */
class NavControllerConfinementTest {

    private companion object {
        /** Screens live here and in `:core:ui`; both are checked. */
        val SCREEN_ROOTS = listOf(
            "src/main/java/com/arshadshah/nimaz/presentation",
            "../core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation",
        )

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
    }

    private fun screenSources(): List<File> =
        SCREEN_ROOTS.map(::File).flatMap { root ->
            assertWithMessage("screen root missing: ${root.absolutePath}")
                .that(root.isDirectory).isTrue()
            root.walkTopDown().filter { it.isFile && it.extension == "kt" }
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
    fun `the graph itself still owns one`() {
        // The complement, so this test cannot pass by the type having vanished entirely — which
        // would mean the app no longer navigates, not that the rule is satisfied.
        val graph = File(GRAPH)
        assertWithMessage("NavGraph.kt not found at ${graph.absolutePath}").that(graph.isFile).isTrue()
        assertThat(graph.readText()).contains("rememberNavController()")
    }
}
