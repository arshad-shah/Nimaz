package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * Every declared `Route` is registered exactly once, and nothing is registered that is not a
 * `Route`.
 *
 * ## Why a count is not enough
 *
 * #563 warns that this is the dangerous part of splitting `NavGraph.kt`:
 *
 * > a route that silently stops being registered fails as a blank screen at runtime, not as a
 * > build error.
 *
 * `check_docs.py`'s NAV-03 compares **totals** — 94 registrations against 94 documented
 * destinations. That catches a route being dropped on its own, but not a route dropped while
 * another is duplicated, which is exactly the shape a copy-paste across eleven new files
 * produces. NAV-05 pairs every `Route` with a `ScreenTags` entry, so the naming already lines up;
 * this asserts the third side of the triangle, as **sets**.
 *
 * ## Comments are not registrations
 *
 * This test failed on its first run reporting a `Route.X` that does not exist — from the KDoc of
 * `taggedComposable` itself, and from the eleven graph files, all of which quote
 * `taggedComposable<Route.X>` as the shape to use. The identical trap caught NAV-03 in PR 11 of
 * #551, where the tempting fix was to edit the documented destination count and make it
 * permanently wrong. Any scan for this pattern has to blank comments first, so [withoutComments]
 * does.
 *
 * A duplicate registration is worth failing on for its own sake: Navigation Compose takes the last
 * one wired, so the earlier block becomes dead code that still looks live in review.
 *
 * ## Reading the sources
 *
 * The registrations are spread across `NavGraph.kt` and the eleven `*Graph.kt` feature extensions,
 * and will spread further as features move into their own modules — so the scan walks for them
 * rather than naming files, and floors what it finds. `Routes.kt` moved to `:core:navigation` in
 * PR 11, hence the sibling path.
 */
class EveryRouteIsRegisteredTest {

    private companion object {
        /** `Routes.kt` lives in `:core:navigation` since PR 11 of #551. */
        const val ROUTES = "../core/navigation/src/main/kotlin/com/arshadshah/nimaz/core/navigation/Routes.kt"

        /**
         * Where destinations are wired — now six roots, because the graph spans every module that
         * owns screens. Shared with the other three cross-module scans; see
         * [PresentationSourceRoots].
         */
        val REGISTRATION_ROOTS = PresentationSourceRoots.NAVIGABLE

        /**
         * A floor on the routes parsed out of `Routes.kt`. Well below the real count, so ordinary
         * additions never touch it, and far enough above zero that a failed parse fails here
         * rather than making every comparison below trivially true.
         */
        const val MINIMUM_ROUTES = 80

        /** `data object Foo` / `data class Foo(` at one indent inside the sealed interface. */
        val DECLARED = Regex("""^\s+data (?:object|class) (\w+)""", RegexOption.MULTILINE)

        val REGISTERED = Regex("""taggedComposable<Route\.(\w+)>""")
    }

    /**
     * [source] with `//` and block comments blanked, preserving line structure and string
     * contents. Written out rather than shared with `check_docs.py`'s equivalent because a test
     * that depends on a Python script to know what it is asserting is worse than sixteen lines of
     * duplication.
     */
    private fun withoutComments(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        var inLine = false
        var inBlock = false
        var inString = false
        while (i < source.length) {
            val ch = source[i]
            val next = source.getOrNull(i + 1)
            when {
                inLine -> if (ch == '\n') { inLine = false; out.append(ch) } else out.append(' ')
                inBlock -> {
                    if (ch == '*' && next == '/') { inBlock = false; out.append("  "); i += 2; continue }
                    out.append(if (ch == '\n') '\n' else ' ')
                }
                inString -> {
                    out.append(ch)
                    if (ch == '\\' && next != null) { out.append(next); i += 2; continue }
                    if (ch == '"') inString = false
                }
                ch == '/' && next == '/' -> { inLine = true; out.append("  "); i += 2; continue }
                ch == '/' && next == '*' -> { inBlock = true; out.append("  "); i += 2; continue }
                else -> { if (ch == '"') inString = true; out.append(ch) }
            }
            i++
        }
        return out.toString()
    }

    private fun declaredRoutes(): List<String> {
        val source = File(ROUTES)
        assertWithMessage(
            "Routes.kt not found at ${source.absolutePath}. This test reads it directly, so a " +
                "wrong path finds zero routes — and without the floor below would then pass " +
                "having compared two empty sets."
        ).that(source.isFile).isTrue()
        return DECLARED.findAll(withoutComments(source.readText())).map { it.groupValues[1] }.toList()
    }

    /** Every `taggedComposable<Route.X>` in the app, with duplicates kept. */
    private fun registeredRoutes(): List<String> {
        PresentationSourceRoots.assertAllExist(REGISTRATION_ROOTS)
        return PresentationSourceRoots.sources(REGISTRATION_ROOTS).flatMap { file ->
            REGISTERED.findAll(withoutComments(file.readText())).map { it.groupValues[1] }
        }
    }

    @Test
    fun `the scan finds the route declarations`() {
        assertThat(declaredRoutes().size).isAtLeast(MINIMUM_ROUTES)
        assertThat(registeredRoutes().size).isAtLeast(MINIMUM_ROUTES)
    }

    @Test
    fun `no route is registered twice`() {
        // Navigation Compose keeps the last registration, so a duplicate makes the earlier block
        // dead code that still reads as live. A count-based check cannot see this at all.
        val duplicates = registeredRoutes().groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys.sorted()

        assertWithMessage(
            "These routes are registered more than once:\n" +
                duplicates.joinToString("\n") { "  Route.$it" } +
                "\n\nOnly the last registration wins, so the others are unreachable."
        ).that(duplicates).isEmpty()
    }

    @Test
    fun `every declared route is wired to a destination`() {
        val missing = (declaredRoutes().toSet() - registeredRoutes().toSet()).sorted()

        assertWithMessage(
            "These routes are declared but never registered:\n" +
                missing.joinToString("\n") { "  Route.$it" } +
                "\n\nNavigating to one is a blank screen at runtime — not a build error, and not " +
                "something the destination *count* can see, because a drop here plus a duplicate " +
                "elsewhere leaves the total unchanged. If a Route is deliberately not a screen " +
                "(a tab inside a parent, say), it should not be a Route."
        ).that(missing).isEmpty()
    }

    @Test
    fun `nothing is registered that is not a declared route`() {
        val unknown = (registeredRoutes().toSet() - declaredRoutes().toSet()).sorted()
        assertWithMessage(
            "These destinations name a Route that Routes.kt does not declare:\n" +
                unknown.joinToString("\n") { "  Route.$it" }
        ).that(unknown).isEmpty()
    }
}
