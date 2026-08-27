package com.arshadshah.nimaz

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Fails when a test in `:app` exercises a subject that now lives in a `:feature:*` module.
 *
 * **Why this exists.** PR 19 of #551 moved the Quran components into `:feature:quran` and left
 * eleven of their tests in `app/src/testDebug`. Every local gate was green: `:app` depends on the
 * feature, the subjects were public, and the tests resolved across the module boundary exactly as
 * they had inside one module. CI was not green — two of those tests read `computeJuzHeaderIndices`
 * and `BottomActions`, which are `internal`, and `internal` is scoped to a *module*.
 *
 * Two things made that worse than a one-off:
 *
 * 1. **Kotlin's incremental compiler does not re-check a file whose own module did not change.**
 *    `./gradlew :app:compileDebugUnitTestKotlin` reported BUILD SUCCESSFUL locally against the
 *    exact sources CI rejected; only `--rerun-tasks`, or CI's clean checkout, surfaced it. A
 *    compile only checks what it recompiles.
 * 2. **The same stranding was already sitting in two merged PRs** — fifteen files for
 *    `:feature:content` (PR 17) and one for `:feature:tracker` (PR 18), inert only because
 *    nothing they touch is `internal` yet. Both were swept up with the eleven.
 *
 * So: a test that still compiles is not evidence it is in the right module. This turns the rule
 * into a failure at `:app:testDebugUnitTest` time, before the visibility of any member narrows.
 *
 * **How it decides.** Every top-level declaration in every module's `src/main` is indexed to its
 * owning module. For each `:app` test source, the symbols it names are resolved against that
 * index, counting a symbol only when the test *imports* it, names it *fully qualified*, or shares
 * its package — three forms, because two of them were needed:
 *
 * - `MushafLinePageFitTest` names `pageFitFontSize` with a plain import, but the declaration lives
 *   in `MushafLineLayout.kt`, not a file named after the test. **Searching the symbol rather than
 *   the file name is the only reading that finds it** — the standing rule on #551, earned twice
 *   before this.
 * - `CompassQiblaViewTest` imports nothing and writes
 *   `com.arshadshah.nimaz.presentation.components.organisms.qibla.CompassQiblaView(...)` inline. An
 *   imports-only scan reported it clean; it was stranded.
 *
 * The qualified form took two attempts. Reading a dotted chain as one package/symbol pair looks
 * right and is not: the package group is greedy, so it backtracks to the *last* legal split, and
 * `…components.molecules.ReaderGoToKind.JUZ` handed back the symbol `JUZ`. The guard passed
 * against a test written to trip it. It now splits the whole chain and takes every capitalised
 * segment — verified by three negative runs, listed below.
 *
 * A symbol declared in more than one module is ambiguous and ignored — that is what keeps generic
 * names like `layout`, `fill` and `Map` from attributing a `:app` test to a random feature. A test
 * is reported only when it names symbols unique to exactly one feature module and none unique to
 * `:app`; anything mixed is a legitimate `:app` test of `:app` code that happens to mention a
 * feature type (`ScreenStateConventionTest` is the deliberate case — it reads every screen in the
 * app by design).
 *
 * Comments are blanked before any of this. Six times in this epic a consumer scan has reported a
 * KDoc reference as a live call site.
 */
class FeatureTestsLiveWithSubjectTest {

    @Test
    fun `no app test exercises a subject that lives in a feature module`() {
        val index = indexDeclarations()
        val tests = appTestSources()

        // Floors, not filters. A mis-rooted scan finds nothing and would otherwise pass — the
        // failure mode that let `MaterialTextFieldGuardTest` and `WidgetGlyphGuardTest` sit green.
        assertThat(index).hasSize(index.size)
        assertThat(index.size).isAtLeast(MINIMUM_SYMBOLS)
        assertThat(tests.size).isAtLeast(MINIMUM_TEST_FILES)

        val stranded = tests.mapNotNull { file ->
            strandedModule(file, index)?.let { (module, symbols) ->
                "${file.relativeTo(repoRoot).path} -> $module (via ${symbols.sorted().take(4)})"
            }
        }

        assertThat(stranded).isEmpty()
    }

    /** `Symbol -> the modules declaring it`, plus the package each declaration sits in. */
    private fun indexDeclarations(): Map<String, Set<Owner>> {
        val index = mutableMapOf<String, MutableSet<Owner>>()
        moduleMainRoots().forEach { (module, root) ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val source = file.readText().blankComments()
                    val pkg = PACKAGE.find(source)?.groupValues?.get(1).orEmpty()
                    source.lineSequence().forEach { line ->
                        DECLARATION.matchAt(line, 0)?.let { match ->
                            index.getOrPut(match.groupValues[1]) { mutableSetOf() }
                                .add(Owner(module, pkg))
                        }
                    }
                }
        }
        return index
    }

    private fun strandedModule(file: File, index: Map<String, Set<Owner>>): Pair<String, Set<String>>? {
        val source = file.readText().blankComments()
        val pkg = PACKAGE.find(source)?.groupValues?.get(1).orEmpty()

        val named = IMPORT.findAll(source).map { it.groupValues[1] }.toMutableSet()
        val qualifiedPackages = mutableSetOf<String>()
        // Every dotted chain rooted at the package, split into all of its parts rather than one
        // package/symbol pair. A single regex with a greedy package group backtracks to the *last*
        // legal split, so `…molecules.ReaderGoToKind.JUZ` yielded the symbol `JUZ` and the guard
        // saw nothing — caught by running the negative case, not by reading the pattern.
        QUALIFIED.findAll(source).forEach { match ->
            val parts = match.value.split('.')
            parts.forEach { part -> if (part.firstOrNull()?.isUpperCase() == true) named += part }
            parts.indices.drop(ROOT_SEGMENTS).forEach { end ->
                qualifiedPackages += parts.subList(0, end).joinToString(".")
            }
        }

        val byModule = mutableMapOf<String, MutableSet<String>>()
        SYMBOL.findAll(source).map { it.value }.distinct().forEach { word ->
            val owners = index[word] ?: return@forEach
            val modules = owners.map { it.module }.distinct()
            if (modules.size != 1) return@forEach // ambiguous name — proves nothing
            val visible = word in named || owners.any { it.pkg == pkg || it.pkg in qualifiedPackages }
            if (visible) byModule.getOrPut(modules.single()) { mutableSetOf() } += word
        }

        if (APP in byModule) return null
        val features = byModule.filterKeys { it.startsWith(FEATURE_PREFIX) }
        return if (features.size == 1) features.entries.single().toPair() else null
    }

    private fun moduleMainRoots(): List<Pair<String, File>> =
        buildList {
            add(APP to repoRoot.resolve("app/src/main"))
            listOf("core", "feature").forEach { kind ->
                repoRoot.resolve(kind).listFiles().orEmpty().sorted().forEach { module ->
                    val main = module.resolve("src/main")
                    if (main.isDirectory) add(":$kind:${module.name}" to main)
                }
            }
        }

    private fun appTestSources(): List<File> =
        listOf("app/src/test", "app/src/testDebug")
            .map { repoRoot.resolve(it) }
            .onEach { check(it.isDirectory) { "test source root missing: $it" } }
            .flatMap { it.walkTopDown().filter { file -> file.isFile && file.extension == "kt" } }

    private data class Owner(val module: String, val pkg: String)

    private companion object {
        /** `:app`'s project directory is the working directory of a `:app` unit test. */
        val repoRoot: File = File("..").canonicalFile

        const val APP = "app"
        const val FEATURE_PREFIX = ":feature:"

        /**
         * Floors, measured rather than guessed: **2,920 symbols across 19 modules, and 59
         * `:app` test files** (92 before PR 19's sweep, 74 after it, 59 once `:feature:settings`
         * left in PR 21). Both figures fall as modules leave, so the floors sit well below — they
         * exist to catch a scan that reads nothing, not to pin a count.
         *
         * Measuring matters, and the coverage floor added in PR 22 is the cautionary tale: it was
         * guessed at 1,500, then re-guessed at 200 from a declaration count, and only *measured*
         * after CI failed for an unrelated reason. The real figure is 4,736. Neither guess was
         * informed by running the task.
         */
        const val MINIMUM_SYMBOLS = 1_500
        const val MINIMUM_TEST_FILES = 40

        val PACKAGE = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
        val IMPORT = Regex("""^import\s+(?:[\w.]+\.)?(\w+)$""", RegexOption.MULTILINE)
        val QUALIFIED = Regex("""\bcom\.arshadshah\.nimaz(?:\.\w+)+\b""")

        /** `com`, `arshadshah`, `nimaz` — the shortest prefix that can be a package here. */
        const val ROOT_SEGMENTS = 3
        val SYMBOL = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""")

        /** Top-level declarations only — a leading space means it is a member or a local. */
        val DECLARATION = Regex(
            """(?:@\w+(?:\([^)]*\))?\s*)*""" +
                """(?:public |internal |private |abstract |open |sealed |data |enum |annotation |value )*""" +
                """(?:class|object|interface|fun|val|var|typealias)\s+""" +
                """(?:<[^>]*>\s+)?(?:[A-Za-z_][\w.]*\.)?([A-Za-z_][A-Za-z0-9_]*)"""
        )

        fun String.blankComments(): String =
            replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)) { match ->
                "\n".repeat(match.value.count { it == '\n' })
            }.replace(Regex("""//[^\n]*"""), "")
    }
}
