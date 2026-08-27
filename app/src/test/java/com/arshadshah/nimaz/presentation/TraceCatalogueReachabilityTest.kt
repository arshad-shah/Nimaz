package com.arshadshah.nimaz.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Every trace in `PerfMonitor.Traces` is started somewhere.
 *
 * A performance catalogue is the one kind of constant whose absence is invisible. A name nothing
 * calls does not warn, does not fail a build, and does not show up as a gap in Firebase — the
 * dashboard simply has no card for it, which looks exactly like a screen nobody used. When this
 * test was written the catalogue had grown to sixteen entries and **fourteen had no call site
 * at all**: the SDK was wired and paid for, the seam had just gained `Telemetry.trace`, and the
 * only two traces the app actually started were the two that predated all of it.
 *
 * The opposite direction is checked too. A call site naming a trace that is not in the catalogue
 * is a string literal by another name — it reports, but nobody can find it next to its
 * neighbours, and nothing stops a second site spelling it differently and splitting the graph.
 *
 * Scanned across every module's `src/main`, because the catalogue is deliberately reachable from
 * all of them: `:core:database` starts the artifact-install trace, ViewModels in seven feature
 * modules start the rest, and `:app` keeps the two entry-point ones.
 */
class TraceCatalogueReachabilityTest {

    private companion object {
        const val CATALOGUE =
            "../core/common/src/main/kotlin/com/arshadshah/nimaz/core/monitoring/PerfMonitor.kt"

        /** `const val NAME = "value"` inside the `Traces` object. */
        val DECLARATION = Regex("const val ([A-Z][A-Z0-9_]*)\\s*=\\s*\"([a-z0-9_]+)\"")

        /** `Traces.NAME`, wherever it is used. */
        val REFERENCE = Regex("""\bTraces\.([A-Z][A-Z0-9_]*)""")

        /**
         * Sixteen today. A floor rather than an equality so adding a trace is one edit, not two —
         * but a *shrinking* catalogue means someone deleted a measurement, which is worth a red
         * build and a moment's thought.
         */
        const val MINIMUM_TRACES = 16

        /** Nineteen modules plus `:app`. A scan that finds three has stopped scanning. */
        const val MINIMUM_SOURCE_ROOTS = 15
    }

    /** Every module's main sources: `:app`'s, plus every directory under `core/` and `feature/`. */
    private fun mainSourceRoots(): List<File> {
        val modules = listOf(File("../core"), File("../feature"))
            .flatMap { it.listFiles().orEmpty().toList() }
            .filter { it.isDirectory }
            .map { File(it, "src/main") }
        return (modules + File("src/main")).filter { it.isDirectory }
    }

    private fun kotlinSources(): List<File> =
        mainSourceRoots()
            .flatMap { it.walkTopDown().filter { f -> f.isFile && f.extension == "kt" } }
            .distinctBy { it.path }

    @Test
    fun `every declared trace is started somewhere, and every started trace is declared`() {
        val catalogue = File(CATALOGUE)
        assertThat(catalogue.isFile).isTrue()

        val roots = mainSourceRoots()
        assertThat(roots.size).isAtLeast(MINIMUM_SOURCE_ROOTS)

        val declared = DECLARATION.findAll(catalogue.readText())
            .map { it.groupValues[1] }
            .toSet()
        assertThat(declared.size).isAtLeast(MINIMUM_TRACES)

        val referenced = kotlinSources()
            .filterNot { it.path.endsWith("PerfMonitor.kt") }
            .flatMap { file -> REFERENCE.findAll(file.readText()).map { it.groupValues[1] } }
            .toSet()

        assertThat(declared - referenced).isEmpty()
        assertThat(referenced - declared).isEmpty()
    }

    /**
     * Two traces cannot share a wire name. Firebase keys on the string, so a duplicate silently
     * merges two unrelated operations into one graph and the constants make it look deliberate.
     */
    @Test
    fun `no two traces share a name`() {
        val names = DECLARATION.findAll(File(CATALOGUE).readText())
            .map { it.groupValues[2] }
            .toList()

        assertThat(names.size).isAtLeast(MINIMUM_TRACES)
        assertThat(names).containsNoDuplicates()
    }

    /**
     * Firebase caps a trace name at 100 characters and **silently drops** anything longer, which
     * is the worst possible failure for a measurement: no error, no data, and a dashboard that
     * looks like the code path is never taken.
     */
    @Test
    fun `no trace name is long enough for Firebase to drop it`() {
        val tooLong = DECLARATION.findAll(File(CATALOGUE).readText())
            .map { it.groupValues[2] }
            .filter { it.length > 100 }
            .toList()

        assertThat(tooLong).isEmpty()
    }
}
