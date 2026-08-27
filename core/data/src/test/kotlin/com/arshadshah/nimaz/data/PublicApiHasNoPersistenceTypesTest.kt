package com.arshadshah.nimaz.data

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * No persistence type may appear in this module's **public** API.
 *
 * ## Why this and not the criterion the issue proposed
 *
 * #560 offers two exit criteria for `:core:data`. The first — *"every repository still returns
 * domain models"* — is **already true and buys nothing**: `:core:domain` contains no `data.*`
 * import at all, and it is a `kotlin-jvm` module, so one would not compile. The second — *"no
 * `*Entity` type appears in any public API"* — was **already false** when the issue was written,
 * at `MushafLayoutMapper.toPageLayout(page, rows: List<MushafLayoutLineRow>)`, a public `object`
 * taking a `QuranDao` projection type.
 *
 * That is the shape worth guarding. A repository returning an entity is loud — the interface it
 * implements lives in `:core:domain` and would not compile. A *helper* in this module exposing a
 * Room type in its signature is silent: nothing in `:core:domain` is involved, so nothing objects,
 * and the leak only matters later when a feature module reaches for the helper and drags a
 * database type into the presentation layer with it.
 *
 * ## How
 *
 * Reads the sources rather than reflecting over the classpath. Reflection would need the Room
 * compiler's generated classes and would have to distinguish "declared in a signature" from
 * "mentioned in a body", which is exactly the distinction that matters here and is easier to see
 * in the text. The cost is that this is a heuristic on visibility modifiers and on type-name
 * suffixes; it is a floor, not a proof.
 *
 * ## Effective visibility, not the modifier on the line
 *
 * The first draft of this test read one line at a time, and its first run reported
 * `MushafLayoutMapper.toPageLayout` — the very declaration whose fix motivated the test. The fix
 * was `internal object MushafLayoutMapper`, and a member of an `internal` container is internal
 * whatever its own modifier says. A line-local check cannot see that, and the "fix" it pushes
 * people toward is a redundant `internal` on every member of an already-internal object, which
 * teaches the wrong rule and leaves the guard reporting noise until someone deletes it.
 *
 * So the scan tracks enclosing containers by indentation and treats a declaration as public only
 * when it and every container around it are public. [containerVisibilityIsInherited] pins that
 * behaviour directly, because it is the part most likely to regress into either uselessness (all
 * containers read as non-public → nothing is ever checked) or noise (the original bug).
 */
class PublicApiHasNoPersistenceTypesTest {

    private companion object {
        /** CWD for a module's unit tests is the module directory. */
        const val MAIN = "src/main/kotlin"

        /**
         * A floor, so a wrong path or a failed glob cannot make this pass over an empty set —
         * the failure mode #553 exists to prevent, and the one that made three guards in this
         * epic green against code that violated them.
         */
        const val MINIMUM_FILES = 25

        /**
         * The same floor for the declaration walk, which is the part with moving parts. The scan
         * finds 293; this sits well below that so ordinary edits do not touch it, and far above
         * the 16 that the constructor-continuation bug produced, so that regression fails here.
         */
        const val MINIMUM_DECLARATIONS = 200

        /**
         * Suffixes that mean "this is a persistence type". `*Row` covers Room's `@Query`
         * projections, which are the ones that actually leaked here — `MushafLayoutLineRow` is
         * not an `@Entity` and a check looking only for `*Entity` would have missed it.
         */
        val PERSISTENCE_SUFFIXES = listOf("Entity", "Entities", "Row", "Dao")

        val NON_PUBLIC = Regex("""\b(private|internal)\b""")

        /** `class` / `object` / `interface` declarations, whose visibility their members inherit. */
        val CONTAINER = Regex("""^(\s*)(?:@\w+\s+)*(?:[\w@]+\s+)*?(class|object|interface)\s+\w+""")

        /** Declarations whose signature is part of the module's API, before visibility is applied. */
        val DECLARATION = Regex(
            """^\s*(?:@\w+\s+)*(?:abstract\s+|open\s+|override\s+|suspend\s+|private\s+|internal\s+)*(?:fun|val|var)\s+[^\n]*"""
        )

        /** Leading characters that mean "this line continues the declaration above". */
        val CONTINUATION = setOf(')', ':', ',')

        /** ktlint holds this module to four-space indentation, so one level is four columns. */
        const val INDENT = 4
    }

    /** One `fun`/`val`/`var` whose signature is reachable from outside this module. */
    private data class PublicDeclaration(val file: File, val line: Int, val signature: String)

    /**
     * Every public declaration in [text], with enclosing-container visibility applied.
     *
     * Indentation is the nesting signal. That is safe here because this is Kotlin the compiler has
     * already formatted and ktlint has already checked, not arbitrary text.
     */
    private fun publicDeclarations(text: String, file: File = File(".")): List<PublicDeclaration> {
        // (indent, isPublic) for each container currently open, outermost first.
        val containers = ArrayDeque<Pair<Int, Boolean>>()
        val found = mutableListOf<PublicDeclaration>()
        val lines = text.lines()

        lines.forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed
            // A wrapped constructor closes on `) : SomeInterface {`, which sits at the *class's*
            // own indent and would otherwise pop the class off the stack — leaving every member
            // below it looking top-level and silently unchecked. That is the shape of nearly every
            // repository in this module, and it cut the scan from 293 declarations to 16.
            if (line.trimStart().firstOrNull() in CONTINUATION) return@forEachIndexed
            val indent = line.takeWhile { it == ' ' }.length

            while (containers.isNotEmpty() && containers.last().first >= indent) {
                containers.removeLast()
            }

            CONTAINER.find(line)?.let { match ->
                val declaration = line.substring(0, match.range.last + 1)
                val enclosingIsPublic = containers.all { it.second }
                containers.addLast(indent to (enclosingIsPublic && !NON_PUBLIC.containsMatchIn(declaration)))
                return@forEachIndexed
            }

            if (!containers.all { it.second }) return@forEachIndexed

            // A declaration is part of the API only where it sits *directly* inside its container.
            // Without this, every local `val` in a function body is read as a member — the second
            // bug this test found in itself, which reported three locals holding Room entities,
            // the ordinary business of a mapping layer.
            val expected = if (containers.isEmpty()) 0 else containers.last().first + INDENT
            if (indent != expected) return@forEachIndexed

            if (DECLARATION.find(line) == null) return@forEachIndexed
            val signature = signatureAt(lines, index)
            if (NON_PUBLIC.containsMatchIn(signature.substringBefore('('))) return@forEachIndexed
            found += PublicDeclaration(file, index + 1, signature)
        }
        return found
    }

    /**
     * The full signature starting at [start], joining continuation lines until the parameter list
     * closes.
     *
     * A wrapped signature is the normal shape for anything with more than two parameters, so a
     * check that read only the first line would be blind to most of the declarations most likely
     * to carry a persistence type. [wrappedSignaturesAreJoined] pins it.
     */
    private fun signatureAt(lines: List<String>, start: Int): String {
        val builder = StringBuilder()
        var depth = 0
        var index = start
        while (index < lines.size) {
            val line = lines[index]
            val cut = if (depth == 0 && index > start) line.substringBefore(" {") else line
            builder.append(if (index == start) cut.trim() else " " + cut.trim())
            depth += cut.count { it == '(' } - cut.count { it == ')' }
            if (depth <= 0) break
            index++
        }
        // Only what precedes the body or initialiser, so a persistence type used *inside* a
        // function does not count — mapping entities to domain models is this module's job.
        return builder.toString().substringBefore(" =").substringBefore(" {").trim()
    }

    private fun mainSources(): List<File> =
        File(MAIN).walkTopDown().filter { it.extension == "kt" }.toList()

    private fun PublicDeclaration.leakedSuffixes(): List<String> =
        PERSISTENCE_SUFFIXES.filter { suffix -> Regex("""\b\w+$suffix\b""").containsMatchIn(signature) }

    @Test
    fun `the scan finds this module's sources`() {
        val files = mainSources()
        assertWithMessage(
            "found ${files.size} Kotlin files under $MAIN from ${File(".").absolutePath} — " +
                "a scan that finds nothing passes every assertion below having checked nothing"
        ).that(files.size).isAtLeast(MINIMUM_FILES)
    }

    @Test
    fun `the scan finds public declarations to check`() {
        // The second half of the same floor. The file walk could succeed while the declaration
        // scan matched nothing — a broken regex, or every container misread as non-public — and
        // the leak assertion would still pass, over an empty list.
        val declarations = mainSources().flatMap { publicDeclarations(it.readText(), it) }
        assertWithMessage(
            "the scan found ${declarations.size} public declarations across this module; if this " +
                "is near zero the visibility walk has broken and the check below is vacuous"
        ).that(declarations.size).isAtLeast(MINIMUM_DECLARATIONS)
    }

    @Test
    fun `no public declaration names a Room entity, projection row or DAO`() {
        val offenders = mainSources()
            .flatMap { publicDeclarations(it.readText(), it) }
            .filter { it.leakedSuffixes().isNotEmpty() }
            .map { "${it.file.path}:${it.line}  ${it.signature}" }
            .sorted()

        assertWithMessage(
            "These public declarations expose a persistence type:\n" +
                offenders.joinToString("\n") { "  $it" } +
                "\n\nA repository returning an entity would not compile — the interface is in " +
                ":core:domain. This catches the quiet version: a helper in this module whose " +
                "signature hands a Room type to whoever calls it. Make the declaration (or its " +
                "enclosing class/object) `internal`, or map to a domain model first."
        ).that(offenders).isEmpty()
    }

    @Test
    fun `the guard matches a leaked signature and not its fix`() {
        // Without this, a regex that silently matched nothing would look exactly like a clean
        // module. Three guards in this epic shipped green against violating code for precisely
        // that reason.
        val leaked = """
            object MushafLayoutMapper {
                fun toPageLayout(page: Int, rows: List<MushafLayoutLineRow>): MushafPageLayout
            }
        """.trimIndent()

        val found = publicDeclarations(leaked).filter { it.leakedSuffixes().isNotEmpty() }
        assertWithMessage("the guard does not detect the leak it was written for")
            .that(found.map { it.signature })
            .containsExactly("fun toPageLayout(page: Int, rows: List<MushafLayoutLineRow>): MushafPageLayout")
    }

    @Test
    fun localsInsideFunctionBodiesAreNotApi() {
        // The second bug this test found in itself. A mapping layer holds Room entities in locals
        // constantly — that is its job — so reading them as members reported three false leaks and
        // would have trained the next reader to ignore the guard.
        val body = """
            class QuranRepositoryImpl(private val quranDao: QuranDao) {
                fun getSurah(number: Int): Flow<Surah?> {
                    return surahFlow.map { surah ->
                        val surahEntity = quranDao.getSurahByNumber(number)
                        val ayahEntities = emptyList<AyahEntity>()
                        surahEntity.toDomain(ayahEntities)
                    }
                }
            }
        """.trimIndent()

        assertThat(publicDeclarations(body).map { it.signature })
            .containsExactly("fun getSurah(number: Int): Flow<Surah?>")
    }

    @Test
    fun wrappedSignaturesAreJoined() {
        // A signature long enough to wrap is exactly the one most likely to carry a persistence
        // type, and reading only its first line would find `fun toPageLayout(` — no parameters, no
        // return type, nothing to match. The guard would be blind precisely where it matters most.
        val wrapped = """
            object MushafLayoutMapper {
                fun toPageLayout(
                    page: Int,
                    rows: List<MushafLayoutLineRow>,
                ): MushafPageLayout {
                    return MushafPageLayout(page, emptyList())
                }
            }
        """.trimIndent()

        val found = publicDeclarations(wrapped)
        assertThat(found.map { it.signature })
            .containsExactly("fun toPageLayout( page: Int, rows: List<MushafLayoutLineRow>, ): MushafPageLayout")
        assertWithMessage("a wrapped signature must still be seen to leak")
            .that(found.single().leakedSuffixes())
            .contains("Row")
    }

    @Test
    fun containerVisibilityIsInherited() {
        // The bug this test's first run exposed: `toPageLayout` is public *on its line*, but its
        // container is `internal`, so it is not part of this module's API and reporting it sends
        // the reader to add a redundant modifier.
        val fixed = """
            internal object MushafLayoutMapper {
                fun toPageLayout(page: Int, rows: List<MushafLayoutLineRow>): MushafPageLayout
            }
        """.trimIndent()

        assertWithMessage("a member of an `internal` container is not public API")
            .that(publicDeclarations(fixed))
            .isEmpty()

        // …and the walk must *close* a container, or every declaration after the first internal
        // one in a file would be excluded and the guard would quietly stop checking the rest.
        val reopened = """
            internal object Hidden {
                fun hidden(row: QuranEntity): Unit
            }

            object Exposed {
                fun exposed(row: QuranEntity): Unit
            }
        """.trimIndent()

        assertThat(publicDeclarations(reopened).map { it.signature })
            .containsExactly("fun exposed(row: QuranEntity): Unit")
    }
}
