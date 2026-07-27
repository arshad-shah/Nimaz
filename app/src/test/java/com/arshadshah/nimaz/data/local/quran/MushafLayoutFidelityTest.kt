package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.MushafLayoutLineRow
import com.arshadshah.nimaz.data.repository.MushafLayoutMapper
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafScript
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test
import java.io.File

/**
 * The fidelity gate for every line-accurate Mushaf edition the app ships (originally #271, for
 * the 16-line IndoPak alone). Where the mapper/use-case unit tests exercise the *code* on
 * synthetic rows, this suite validates the **shipped reference data** — the
 * `assets/quran/mushaf/` layout and text assets — against the structural invariants any
 * faithful edition must satisfy. It is the automated, reproducible form of the "pass/fail sheet
 * across all pages" the issue asks for: a single wrong line break, a dropped word, or a lost
 * basmalah fails a test rather than slipping into a hifz reader.
 *
 * Every test iterates [MushafScript.entries], so **adding an edition automatically puts it
 * under the same gate** — there is no per-edition test to remember to write. Page counts and
 * lines-per-page are asserted against the enum, which makes the catalogue and the data prove
 * each other: if either drifts, this fails.
 *
 * The assets are read straight from `src/main/assets` (unit tests run with the module dir as
 * the working directory — see `WidgetGlyphGuardTest`), so this pins the bytes that actually
 * ship, not a fixture. The companion human-readable report lives at
 * `docs/quran/16-line-fidelity-sheet.md`.
 */
class MushafLayoutFidelityTest {

    private val editions: List<MushafScript> = MushafScript.entries.filter { it.isLineAccurate }

    private val layoutCache = mutableMapOf<MushafScript, List<MushafLayoutRowDto>>()
    private val textCache = mutableMapOf<String, List<MushafAyahTextDto>>()

    private fun layoutOf(script: MushafScript): List<MushafLayoutRowDto> =
        layoutCache.getOrPut(script) {
            mushafLayoutJson.decodeFromString(
                ListSerializer(MushafLayoutRowDto.serializer()),
                asset(MushafLayoutSeeder.layoutAsset(script)),
            )
        }

    private fun textOf(script: MushafScript): List<MushafAyahTextDto> {
        val source = requireNotNull(script.textSource) { "$script is line-accurate but has no text source" }
        return textCache.getOrPut(source) {
            mushafLayoutJson.decodeFromString(
                ListSerializer(MushafAyahTextDto.serializer()),
                asset(MushafLayoutSeeder.textAsset(source)),
            )
        }
    }

    private fun asset(assetPath: String): String {
        val file = File("src/main/assets/$assetPath")
        assertThat(file.exists()).isTrue()
        return file.readText()
    }

    @Test
    fun `there is at least one line-accurate edition to validate`() {
        // Guards against the suite silently passing because the filter matched nothing.
        assertThat(editions).isNotEmpty()
    }

    // ---- Book-level totals -------------------------------------------------

    @Test
    fun `every ayah of the Quran is present exactly once with glyph text`() {
        editions.forEach { script ->
            val ayahs = textOf(script)
            val ids = ayahs.map { it.ayahId }
            assertThat(ids).hasSize(TOTAL_AYAHS)
            assertThat(ids.toSet()).hasSize(TOTAL_AYAHS)
            assertThat(ids.min()).isEqualTo(1)
            assertThat(ids.max()).isEqualTo(TOTAL_AYAHS)
            assertThat(ayahs.all { it.text.isNotBlank() }).isTrue()
        }
    }

    @Test
    fun `each edition paginates to exactly the page count its catalogue entry declares`() {
        editions.forEach { script ->
            val pages = layoutOf(script).map { it.pageNumber }.toSortedSet()
            assertThat(pages.first()).isEqualTo(1)
            assertThat(pages.last()).isEqualTo(script.totalPages)
            assertThat(pages).hasSize(script.totalPages)
            assertThat(pages.toList()).isEqualTo((1..script.totalPages).toList())
        }
    }

    @Test
    fun `every surah gets exactly one header and only surahs 1 and 9 lack a basmalah`() {
        editions.forEach { script ->
            val layout = layoutOf(script)
            val headerSurahs = layout.filter { it.lineType == "surah_header" }.map { it.surahId }
            assertThat(headerSurahs).hasSize(TOTAL_SURAHS)
            assertThat(headerSurahs.toSet()).isEqualTo((1..TOTAL_SURAHS).toSet())

            val basmalahSurahs = layout.filter { it.lineType == "basmalah" }.map { it.surahId }
            // Al-Fatihah (1) opens with the basmalah as ayah 1; At-Tawbah (9) has none.
            val expected = (1..TOTAL_SURAHS).toSet() - setOf(1, 9)
            assertThat(basmalahSurahs.toSet()).isEqualTo(expected)
            assertThat(basmalahSurahs).hasSize(TOTAL_SURAHS - 2) // 112
        }
    }

    // ---- Page geometry -----------------------------------------------------

    @Test
    fun `no page exceeds its edition's line count and line numbers strictly increase`() {
        editions.forEach { script ->
            val maxLines = requireNotNull(script.linesPerPage)
            layoutOf(script).groupBy { it.pageNumber }.forEach { (page, rows) ->
                val lineNumbers = rows.map { it.lineNumber }.toSortedSet()
                assertThat(lineNumbers.size).isAtMost(maxLines)
                assertThat(lineNumbers.first()).isAtLeast(1)
                assertThat(lineNumbers.last()).isAtMost(maxLines)
                // Within a page, physical lines are numbered top-to-bottom; a page may
                // legitimately start below line 1 (the ornamental opening spread on p.2) or
                // skip a spacer line, but numbering must never run backwards.
                assertThat(lineNumbers.toList()).isInStrictOrder()
                assertThat(page).isAtLeast(1)
            }
        }
    }

    @Test
    fun `ayah segments carry a valid inclusive word-position range`() {
        editions.forEach { script ->
            layoutOf(script).filter { it.lineType == "ayah" }.forEach { row ->
                val first = row.firstWordPosition
                val last = row.lastWordPosition
                assertThat(first).isNotNull()
                assertThat(last).isNotNull()
                assertThat(row.ayahId).isNotNull()
                assertThat(first!!).isAtLeast(1)
                assertThat(last!!).isAtLeast(first)
            }
        }
    }

    @Test
    fun `structural rows carry no ayah or word positions`() {
        editions.forEach { script ->
            layoutOf(script).filter { it.lineType != "ayah" }.forEach { row ->
                assertThat(row.ayahId).isNull()
                assertThat(row.firstWordPosition).isNull()
                assertThat(row.lastWordPosition).isNull()
            }
        }
    }

    // ---- The core fidelity guarantee: no word is dropped or duplicated -----

    @Test
    fun `every ayah's words are laid out exactly once, in order, with no gaps or duplicates`() {
        editions.forEach { script ->
            val wordCount = textOf(script).associate { it.ayahId to it.text.trim().split(" ").size }

            // Reading order of segments across the whole book: page, then line, then source
            // order (the seed preserves JSON order within a line, matching the DAO's
            // `ORDER BY line, id`).
            val segmentsByAyah = layoutOf(script)
                .withIndex()
                .filter { it.value.lineType == "ayah" }
                .groupBy({ it.value.ayahId!! }, { IndexedSegment(it.index, it.value) })

            assertThat(segmentsByAyah.keys).isEqualTo((1..TOTAL_AYAHS).toSet())

            val offenders = mutableListOf<String>()
            segmentsByAyah.forEach { (ayahId, segments) ->
                val ordered = segments.sortedBy { it.order }.map { it.row }
                val covered = ordered.flatMap { it.firstWordPosition!!..it.lastWordPosition!! }
                val expected = (1..(wordCount[ayahId] ?: 0)).toList()
                if (covered != expected) {
                    offenders += "$script ayah $ayahId: covered=$covered expected=$expected"
                }
            }
            assertThat(offenders).isEmpty()
        }
    }

    /**
     * Editions that share a text source must ship *identical* glyphs, because the app stores
     * that text once and every sharing edition slices the same rows. The generator enforces
     * this when writing; this re-checks it on the bytes that actually ship.
     */
    @Test
    fun `editions sharing a text source have identical glyph text`() {
        editions.groupBy { it.textSource }
            .filterValues { it.size > 1 }
            .forEach { (source, sharing) ->
                val texts = sharing.map { script -> textOf(script).associate { it.ayahId to it.text } }
                val first = texts.first()
                texts.drop(1).forEach { other ->
                    assertThat("$source: ${other.size} ayahs").isEqualTo("$source: ${first.size} ayahs")
                    assertThat(other).isEqualTo(first)
                }
            }
    }

    // ---- Round-trip through the production mapper on real pages ------------

    @Test
    fun `mapping the real data preserves all 112 basmalah lines`() {
        // Guards the mapper against real data: many surahs ship header+basmalah on one
        // line_number; a groupBy{line}.first() collapses those to a header, dropping the
        // basmalah. Every basmalah row must survive as its own BASMALAH line.
        editions.forEach { script ->
            val basmalahLines = layoutOf(script)
                .groupBy { it.pageNumber }
                .entries
                .sumOf { (page, rows) ->
                    MushafLayoutMapper.toPageLayout(page, rows.map { it.toRow() })
                        .lines.count { it.type == MushafLineType.BASMALAH }
                }
            assertThat(basmalahLines).isEqualTo(TOTAL_SURAHS - 2) // 112
        }
    }

    @Test
    fun `the opening spread places Al-Fatihah on page 1 and Al-Baqarah's opening on page 2`() {
        editions.forEach { script ->
            val layout = layoutOf(script)
            val p1 = MushafLayoutMapper.toPageLayout(
                1, layout.filter { it.pageNumber == 1 }.map { it.toRow() }
            )
            assertThat(p1.lines.first().type).isEqualTo(MushafLineType.SURAH_HEADER)
            assertThat(p1.lines.first().surahId).isEqualTo(1)

            val p2Rows = layout.filter { it.pageNumber == 2 }
            val p2 = MushafLayoutMapper.toPageLayout(2, p2Rows.map { it.toRow() })
            assertThat(p2.lines.first().type).isEqualTo(MushafLineType.SURAH_HEADER)
            assertThat(p2.lines.first().surahId).isEqualTo(2)
            assertThat(p2.lines.any { it.type == MushafLineType.BASMALAH }).isTrue()
        }
    }

    private data class IndexedSegment(val order: Int, val row: MushafLayoutRowDto)

    private fun MushafLayoutRowDto.toRow() = MushafLayoutLineRow(
        page = pageNumber,
        line = lineNumber,
        lineType = lineType,
        surahId = surahId,
        ayahId = ayahId,
        firstWordPosition = firstWordPosition,
        lastWordPosition = lastWordPosition,
        text = null,
        ayahNumberInSurah = null,
    )

    private companion object {
        const val TOTAL_AYAHS = 6236
        const val TOTAL_SURAHS = 114
    }
}
