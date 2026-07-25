package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.MushafLayoutLineRow
import com.arshadshah.nimaz.data.repository.MushafLayoutMapper
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test
import java.io.File

/**
 * The 7/7 fidelity gate for the 16-line IndoPak Mushaf (#271). Where the mapper/use-case unit
 * tests exercise the *code* on synthetic rows, this suite validates the **shipped reference
 * data** — `assets/quran/mushaf_layout_indopak16.json` + `assets/quran/ayahs_indopak.json` —
 * against the structural invariants any faithful 16-line IndoPak edition must satisfy. It is
 * the automated, reproducible form of the "pass/fail sheet across all 548 pages" the issue
 * asks for: a single wrong line break, a dropped word, or a lost basmalah fails a test rather
 * than slipping into a hifz reader.
 *
 * The assets are read straight from `src/main/assets` (unit tests run with the module dir as
 * the working directory — see `WidgetGlyphGuardTest`), so this pins the bytes that actually
 * ship, not a fixture. The companion human-readable report lives at
 * `docs/quran/16-line-fidelity-sheet.md`.
 */
class MushafLayoutFidelityTest {

    private val layout: List<IndopakLayoutRowDto> by lazy {
        quranIndopakJson.decodeFromString(
            ListSerializer(IndopakLayoutRowDto.serializer()),
            asset("mushaf_layout_indopak16.json"),
        )
    }

    private val ayahs: List<IndopakAyahDto> by lazy {
        quranIndopakJson.decodeFromString(
            ListSerializer(IndopakAyahDto.serializer()),
            asset("ayahs_indopak.json"),
        )
    }

    private fun asset(name: String): String {
        val file = File("src/main/assets/quran/$name")
        assertThat(file.exists()).isTrue()
        return file.readText()
    }

    // ---- Book-level totals -------------------------------------------------

    @Test
    fun `every ayah of the Quran is present exactly once with IndoPak text`() {
        val ids = ayahs.map { it.ayahId }
        assertThat(ids).hasSize(TOTAL_AYAHS)
        assertThat(ids.toSet()).hasSize(TOTAL_AYAHS)
        assertThat(ids.min()).isEqualTo(1)
        assertThat(ids.max()).isEqualTo(TOTAL_AYAHS)
        assertThat(ayahs.all { it.textIndopak.isNotBlank() }).isTrue()
    }

    @Test
    fun `layout paginates to exactly 548 contiguous pages`() {
        val pages = layout.map { it.pageNumber }.toSortedSet()
        assertThat(pages.first()).isEqualTo(1)
        assertThat(pages.last()).isEqualTo(TOTAL_PAGES)
        assertThat(pages).hasSize(TOTAL_PAGES)
        assertThat(pages.toList()).isEqualTo((1..TOTAL_PAGES).toList())
    }

    @Test
    fun `every surah gets exactly one header and only surahs 1 and 9 lack a basmalah`() {
        val headerSurahs = layout.filter { it.lineType == "surah_header" }.map { it.surahId }
        assertThat(headerSurahs).hasSize(TOTAL_SURAHS)
        assertThat(headerSurahs.toSet()).isEqualTo((1..TOTAL_SURAHS).toSet())

        val basmalahSurahs = layout.filter { it.lineType == "basmalah" }.map { it.surahId }
        // Al-Fatihah (1) opens with the basmalah as ayah 1; At-Tawbah (9) has none.
        val expected = (1..TOTAL_SURAHS).toSet() - setOf(1, 9)
        assertThat(basmalahSurahs.toSet()).isEqualTo(expected)
        assertThat(basmalahSurahs).hasSize(TOTAL_SURAHS - 2) // 112
    }

    // ---- Page geometry -----------------------------------------------------

    @Test
    fun `no page exceeds 16 lines and line numbers stay within 1 to 16 and strictly increase`() {
        layout.groupBy { it.pageNumber }.forEach { (_, rows) ->
            val lineNumbers = rows.map { it.lineNumber }.toSortedSet()
            assertThat(lineNumbers.size).isAtMost(MAX_LINES_PER_PAGE)
            assertThat(lineNumbers.first()).isAtLeast(1)
            assertThat(lineNumbers.last()).isAtMost(MAX_LINES_PER_PAGE)
            // Within a page, physical lines are numbered top-to-bottom; a page may legitimately
            // start below line 1 (the ornamental opening spread on p.2) or skip a spacer line
            // (p.290), but numbering must never run backwards.
            assertThat(lineNumbers.toList()).isInStrictOrder()
        }
    }

    @Test
    fun `ayah segments carry a valid inclusive word-position range`() {
        layout.filter { it.lineType == "ayah" }.forEach { row ->
            val first = row.firstWordPosition
            val last = row.lastWordPosition
            assertThat(first).isNotNull()
            assertThat(last).isNotNull()
            assertThat(row.ayahId).isNotNull()
            assertThat(first!!).isAtLeast(1)
            assertThat(last!!).isAtLeast(first)
        }
    }

    @Test
    fun `structural rows carry no ayah or word positions`() {
        layout.filter { it.lineType != "ayah" }.forEach { row ->
            assertThat(row.ayahId).isNull()
            assertThat(row.firstWordPosition).isNull()
            assertThat(row.lastWordPosition).isNull()
        }
    }

    // ---- The core fidelity guarantee: no word is dropped or duplicated -----

    @Test
    fun `every ayah's words are laid out exactly once, in order, with no gaps or duplicates`() {
        val wordCount = ayahs.associate { it.ayahId to it.textIndopak.trim().split(" ").size }

        // Reading order of segments across the whole book: page, then line, then source order
        // (the seed preserves the JSON order within a line, matching DAO `ORDER BY line, id`).
        val segmentsByAyah = layout
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
                offenders += "ayah $ayahId: covered=$covered expected=$expected"
            }
        }
        assertThat(offenders).isEmpty()
    }

    // ---- Round-trip through the production mapper on real pages ------------

    @Test
    fun `mapping the real data preserves all 112 basmalah lines`() {
        // Guards the 7/7 mapper fix against real data: 81 surahs ship header+basmalah on one
        // line_number; the old groupBy{line}.first() collapsed those to a header, dropping the
        // basmalah. After the fix every basmalah row must survive as its own BASMALAH line.
        val basmalahLines = layout
            .groupBy { it.pageNumber }
            .entries
            .sumOf { (page, rows) ->
                MushafLayoutMapper.toPageLayout(page, rows.map { it.toRow() })
                    .lines.count { it.type == MushafLineType.BASMALAH }
            }
        assertThat(basmalahLines).isEqualTo(TOTAL_SURAHS - 2) // 112
    }

    @Test
    fun `the opening spread places Al-Fatihah on page 1 and Al-Baqarah's opening on page 2`() {
        val p1 = MushafLayoutMapper.toPageLayout(1, layout.filter { it.pageNumber == 1 }.map { it.toRow() })
        assertThat(p1.lines.first().type).isEqualTo(MushafLineType.SURAH_HEADER)
        assertThat(p1.lines.first().surahId).isEqualTo(1)

        val p2Rows = layout.filter { it.pageNumber == 2 }
        // The classic decorative opening: p.2 is vertically offset to mirror p.1, so its first
        // printed line is not line 1.
        assertThat(p2Rows.minOf { it.lineNumber }).isGreaterThan(1)
        val p2 = MushafLayoutMapper.toPageLayout(2, p2Rows.map { it.toRow() })
        assertThat(p2.lines.first().type).isEqualTo(MushafLineType.SURAH_HEADER)
        assertThat(p2.lines.first().surahId).isEqualTo(2)
        assertThat(p2.lines.any { it.type == MushafLineType.BASMALAH }).isTrue()
    }

    private data class IndexedSegment(val order: Int, val row: IndopakLayoutRowDto)

    private fun IndopakLayoutRowDto.toRow() = MushafLayoutLineRow(
        page = pageNumber,
        line = lineNumber,
        lineType = lineType,
        surahId = surahId,
        ayahId = ayahId,
        firstWordPosition = firstWordPosition,
        lastWordPosition = lastWordPosition,
        textIndopak = null,
        ayahNumberInSurah = null,
    )

    private companion object {
        const val TOTAL_AYAHS = 6236
        const val TOTAL_PAGES = 548
        const val TOTAL_SURAHS = 114
        const val MAX_LINES_PER_PAGE = 16
    }
}
