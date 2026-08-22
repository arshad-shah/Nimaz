package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * The route vocabulary knows what a destination *is*, never what draws it.
 *
 * ## Why a test and not just the compiler
 *
 * `moduleBoundary` already stops this module declaring a Gradle dependency on `:app` or a
 * `:feature:*`. It cannot stop the subtler version, which is what #562 actually asks for: an
 * import of `presentation.screens` or `presentation.viewmodel` from **whatever module happens to
 * hold those packages at the time**. Today they are in `:app`, so such an import would not
 * compile and this test is belt-and-braces. From PR 13 onwards they move into `:feature:*`
 * modules — at which point a feature could add itself as a dependency here and the compiler would
 * be perfectly happy while the epic's whole point quietly inverted.
 *
 * The single import this module started with was `AnnouncementRoutes` reaching for
 * `presentation.screens.names.NamesTab` — a deep-link contract that had been living in a screen
 * file. It moved *here* rather than being tolerated there.
 *
 * ## The `:core:ui` clause
 *
 * A `Route` carries a destination's identity, never its label. `NamesTab` used to hold a
 * `@StringRes`, and keeping it would have forced this module to depend on `:core:ui` for three
 * strings. Once eleven feature modules depend on both, that edge is very hard to remove — so it
 * is refused here while refusing it is still cheap.
 */
class NavigationHasNoPresentationImportsTest {

    private companion object {
        /** CWD for a module's unit tests is the module directory. */
        const val MAIN = "src/main/kotlin"

        /**
         * A floor, so a wrong path or a failed walk cannot make this pass over an empty set. Seven
         * checks in this epic have been found green against exactly what they existed to catch;
         * this module has 8 source files, and the floor is well below that.
         */
        const val MINIMUM_FILES = 5

        /**
         * Packages this module must never import. `presentation.*` is the rule #562 states.
         * `core.ui` is the one this milestone added, for the reason in the class doc.
         */
        val FORBIDDEN = listOf(
            "com.arshadshah.nimaz.presentation.screens",
            "com.arshadshah.nimaz.presentation.viewmodel",
            "com.arshadshah.nimaz.core.ui",
        )
    }

    private fun sources(): List<File> =
        File(MAIN).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun `the scan finds this module's sources`() {
        val found = sources()
        assertWithMessage(
            "found ${found.size} Kotlin files under $MAIN from ${File(".").absolutePath} — " +
                "a scan that finds nothing passes the assertion below having checked nothing"
        ).that(found.size).isAtLeast(MINIMUM_FILES)
    }

    @Test
    fun `no source imports a screen, a ViewModel or the design system`() {
        val offenders = sources().flatMap { file ->
            file.readLines().withIndex().mapNotNull { (index, line) ->
                val import = Regex("""^\s*import\s+([\w.]+)""").find(line)?.groupValues?.get(1)
                    ?: return@mapNotNull null
                val bad = FORBIDDEN.firstOrNull { import.startsWith(it) } ?: return@mapNotNull null
                "${file.path}:${index + 1}  $import   (forbidden: $bad)"
            }
        }.sorted()

        assertWithMessage(
            "These imports point the wrong way:\n" +
                offenders.joinToString("\n") { "  $it" } +
                "\n\nA route names a destination; it must not reach for the thing that draws it. " +
                "If the type is really navigation — as `NamesTab` turned out to be, because its " +
                "ordinal is the deep-link contract — move it here. If it is really presentation, " +
                "keep it there and pass what navigation needs (an ordinal, an id) instead."
        ).that(offenders).isEmpty()
    }

    @Test
    fun `the detector matches a forbidden import`() {
        // Without this, a regex that quietly matched nothing would look exactly like a clean
        // module — the failure mode that made seven earlier guards in this epic useless.
        val line = "import com.arshadshah.nimaz.presentation.screens.names.NamesTab"
        val import = Regex("""^\s*import\s+([\w.]+)""").find(line)?.groupValues?.get(1)

        assertThat(import).isEqualTo("com.arshadshah.nimaz.presentation.screens.names.NamesTab")
        assertThat(FORBIDDEN.any { import!!.startsWith(it) }).isTrue()

        // …and does not fire on the imports this module legitimately has.
        listOf(
            "import com.arshadshah.nimaz.domain.model.MushafScript",
            "import androidx.navigation.NavGraphBuilder",
        ).forEach { legitimate ->
            val parsed = Regex("""^\s*import\s+([\w.]+)""").find(legitimate)!!.groupValues[1]
            assertWithMessage(legitimate)
                .that(FORBIDDEN.any { parsed.startsWith(it) })
                .isFalse()
        }
    }
}
