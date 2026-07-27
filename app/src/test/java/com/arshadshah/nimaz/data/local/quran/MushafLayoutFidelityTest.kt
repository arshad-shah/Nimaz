package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.MushafLayoutLineRow
import com.arshadshah.nimaz.data.repository.MushafLayoutMapper
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.quran.catalogue.AyahTextSource
import com.arshadshah.nimaz.domain.model.quran.catalogue.MushafLayoutEdition
import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test
import java.io.File

/**
 * The fidelity gate for every line-accurate Mushaf edition the app ships (#271). Where the
 * mapper/use-case unit tests exercise the *code* on synthetic rows, this suite validates the
 * **shipped reference data** against the structural invariants any faithful printed edition
 * must satisfy. It is the automated, reproducible form of the "pass/fail sheet across all
 * pages" the issue asks for: a single wrong line break, a dropped word, or a lost basmalah
 * fails a test rather than slipping into a hifz reader.
 *
 * Every test iterates `QuranEditions.mushafLayouts` rather than naming one edition, and takes
 * its expected page count and line count from the catalogue entry. That is the point: a new
 * layout added per ADR-001 (catalogue entry + asset binding + asset, no code) is validated on
 * arrival, so a bad asset cannot ship simply because nobody wrote a test for it.
 *
 * The assets are read straight from `src/main/assets` (unit tests run with the module dir as
 * the working directory — see `WidgetGlyphGuardTest`), so this pins the bytes that actually
 * ship, not a fixture. The companion human-readable report for the 16-line edition lives at
 * `docs/quran/16-line-fidelity-sheet.md`.
 */
class MushafLayoutFidelityTest {

    /** One shipped edition's parsed assets, ready to assert over. */
    private class EditionData(
        val edition: MushafLayoutEdition,
        val layout: List<IndopakLayoutRowDto>,
        val ayahs: List<IndopakAyahDto>?
    )

    private val editions: List<EditionData> by lazy {
        QuranEditions.mushafLayouts.filter { it.hasLineLayout }.map { edition ->
            val assets = QuranContentAssets.mushafLayouts.getValue(edition.id)
            EditionData(
                edition = edition,
                layout = quranIndopakJson.decodeFromString(
                    ListSerializer(IndopakLayoutRowDto.serializer()),
                    asset(assets.layout.assetPath),
                ),
                ayahs = assets.ayahText?.let {
                    quranIndopakJson.decodeFromString(
                        ListSerializer(IndopakAyahDto.serializer()),
                        asset(it.assetPath),
                    )
                }
            )
        }
    }

    private fun asset(assetPath: String): String {
        val file = File("src/main/assets/$assetPath")
        assertWithMessage("missing bundled asset $assetPath").that(file.exists()).isTrue()
        return file.readText()
    }

    /** Runs [block] for each shipped line-accurate edition, naming it on failure. */
    private fun forEachEdition(block: (EditionData) -> Unit) {
        // A catalogue with no line-accurate edition would make every test below vacuous.
        assertThat(editions).isNotEmpty()
        editions.forEach(block)
    }

    // ---- Book-level totals -------------------------------------------------

    @Test
    fun `every ayah of the Quran is present exactly once with its edition text`() =
        forEachEdition { data ->
            val ayahs = data.ayahs ?: return@forEachEdition
            val id = data.edition.id
            val ids = ayahs.map { it.ayahId }
            assertWithMessage(id).that(ids).hasSize(TOTAL_AYAHS)
            assertWithMessage(id).that(ids.toSet()).hasSize(TOTAL_AYAHS)
            assertWithMessage(id).that(ids.min()).isEqualTo(1)
            assertWithMessage(id).that(ids.max()).isEqualTo(TOTAL_AYAHS)
            assertWithMessage(id).that(ayahs.all { it.textIndopak.isNotBlank() }).isTrue()
        }

    @Test
    fun `an edition that needs its own ayah text ships it`() = forEachEdition { data ->
        // The word positions index into one column. If that column is not one the prepopulated
        // DB already fills, the text has to ship with the layout or every page renders blank.
        if (data.edition.textSource == AyahTextSource.INDOPAK) {
            assertWithMessage(data.edition.id).that(data.ayahs).isNotNull()
        }
    }

    @Test
    fun `layout paginates to exactly the catalogue page count, contiguously`() =
        forEachEdition { data ->
            val id = data.edition.id
            val pages = data.layout.map { it.pageNumber }.toSortedSet()
            val total = data.edition.totalPages
            assertWithMessage(id).that(pages.first()).isEqualTo(1)
            assertWithMessage(id).that(pages.last()).isEqualTo(total)
            assertWithMessage(id).that(pages).hasSize(total)
            assertWithMessage(id).that(pages.toList()).isEqualTo((1..total).toList())
        }

    @Test
    fun `every surah gets exactly one header and only surahs 1 and 9 lack a basmalah`() =
        forEachEdition { data ->
            val id = data.edition.id
            val headerSurahs =
                data.layout.filter { it.lineType == "surah_header" }.map { it.surahId }
            assertWithMessage(id).that(headerSurahs).hasSize(TOTAL_SURAHS)
            assertWithMessage(id).that(headerSurahs.toSet()).isEqualTo((1..TOTAL_SURAHS).toSet())

            val basmalahSurahs =
                data.layout.filter { it.lineType == "basmalah" }.map { it.surahId }
            // Al-Fatihah (1) opens with the basmalah as ayah 1; At-Tawbah (9) has none.
            val expected = (1..TOTAL_SURAHS).toSet() - setOf(1, 9)
            assertWithMessage(id).that(basmalahSurahs.toSet()).isEqualTo(expected)
            assertWithMessage(id).that(basmalahSurahs).hasSize(TOTAL_SURAHS - 2) // 112
        }

    // ---- Page geometry -----------------------------------------------------

    @Test
    fun `no page exceeds the edition's line count and line numbers strictly increase`() =
        forEachEdition { data ->
            val maxLines = data.edition.linesPerPage!!
            data.layout.groupBy { it.pageNumber }.forEach { (page, rows) ->
                val lineNumbers = rows.map { it.lineNumber }.toSortedSet()
                val where = "${data.edition.id} p.$page"
                assertWithMessage(where).that(lineNumbers.size).isAtMost(maxLines)
                assertWithMessage(where).that(lineNumbers.first()).isAtLeast(1)
                assertWithMessage(where).that(lineNumbers.last()).isAtMost(maxLines)
                // Within a page, physical lines are numbered top-to-bottom; a page may
                // legitimately start below line 1 (the ornamental opening spread on p.2) or
                // skip a spacer line (p.290), but numbering must never run backwards.
                assertWithMessage(where).that(lineNumbers.toList()).isInStrictOrder()
            }
        }

    @Test
    fun `ayah segments carry a valid inclusive word-position range`() = forEachEdition { data ->
        data.layout.filter { it.lineType == "ayah" }.forEach { row ->
            val where = "${data.edition.id} p.${row.pageNumber} l.${row.lineNumber}"
            val first = row.firstWordPosition
            val last = row.lastWordPosition
            assertWithMessage(where).that(first).isNotNull()
            assertWithMessage(where).that(last).isNotNull()
            assertWithMessage(where).that(row.ayahId).isNotNull()
            assertWithMessage(where).that(first!!).isAtLeast(1)
            assertWithMessage(where).that(last!!).isAtLeast(first)
        }
    }

    @Test
    fun `structural rows carry no ayah or word positions`() = forEachEdition { data ->
        data.layout.filter { it.lineType != "ayah" }.forEach { row ->
            val where = "${data.edition.id} p.${row.pageNumber} l.${row.lineNumber}"
            assertWithMessage(where).that(row.ayahId).isNull()
            assertWithMessage(where).that(row.firstWordPosition).isNull()
            assertWithMessage(where).that(row.lastWordPosition).isNull()
        }
    }

    // ---- The core fidelity guarantee: no word is dropped or duplicated -----

    @Test
    fun `every ayah's words are laid out exactly once, in order, with no gaps or duplicates`() =
        forEachEdition { data ->
            val ayahs = data.ayahs ?: return@forEachEdition
            val wordCount = ayahs.associate { it.ayahId to it.textIndopak.trim().split(" ").size }

            // Reading order of segments across the book: page, then line, then source order
            // (the seed preserves JSON order within a line, matching DAO `ORDER BY line, id`).
            val segmentsByAyah = data.layout
                .withIndex()
                .filter { it.value.lineType == "ayah" }
                .groupBy({ it.value.ayahId!! }, { IndexedSegment(it.index, it.value) })

            assertWithMessage(data.edition.id)
                .that(segmentsByAyah.keys).isEqualTo((1..TOTAL_AYAHS).toSet())

            val offenders = mutableListOf<String>()
            segmentsByAyah.forEach { (ayahId, segments) ->
                val ordered = segments.sortedBy { it.order }.map { it.row }
                val covered = ordered.flatMap { it.firstWordPosition!!..it.lastWordPosition!! }
                val expected = (1..(wordCount[ayahId] ?: 0)).toList()
                if (covered != expected) {
                    offenders += "ayah $ayahId: covered=$covered expected=$expected"
                }
            }
            assertWithMessage(data.edition.id).that(offenders).isEmpty()
        }

    // ---- Round-trip through the production mapper on real pages ------------

    @Test
    fun `mapping the real data preserves all 112 basmalah lines`() = forEachEdition { data ->
        // Guards the mapper fix against real data: 81 surahs ship header+basmalah on one
        // line_number; the old groupBy{line}.first() collapsed those to a header, dropping the
        // basmalah. After the fix every basmalah row must survive as its own BASMALAH line.
        val basmalahLines = data.layout
            .groupBy { it.pageNumber }
            .entries
            .sumOf { (page, rows) ->
                MushafLayoutMapper
                    .toPageLayout(page, rows.map { it.toRow() }, data.edition.textSource)
                    .lines.count { it.type == MushafLineType.BASMALAH }
            }
        assertWithMessage(data.edition.id).that(basmalahLines).isEqualTo(TOTAL_SURAHS - 2)
    }

    @Test
    fun `the opening spread places Al-Fatihah on page 1 and Al-Baqarah's opening on page 2`() =
        forEachEdition { data ->
            val id = data.edition.id
            val source = data.edition.textSource
            val p1 = MushafLayoutMapper.toPageLayout(
                1, data.layout.filter { it.pageNumber == 1 }.map { it.toRow() }, source
            )
            assertWithMessage(id).that(p1.lines.first().type)
                .isEqualTo(MushafLineType.SURAH_HEADER)
            assertWithMessage(id).that(p1.lines.first().surahId).isEqualTo(1)

            val p2Rows = data.layout.filter { it.pageNumber == 2 }
            // The classic decorative opening: p.2 is vertically offset to mirror p.1, so its
            // first printed line is not line 1.
            assertWithMessage(id).that(p2Rows.minOf { it.lineNumber }).isGreaterThan(1)
            val p2 = MushafLayoutMapper.toPageLayout(2, p2Rows.map { it.toRow() }, source)
            assertWithMessage(id).that(p2.lines.first().type)
                .isEqualTo(MushafLineType.SURAH_HEADER)
            assertWithMessage(id).that(p2.lines.first().surahId).isEqualTo(2)
            assertWithMessage(id).that(p2.lines.any { it.type == MushafLineType.BASMALAH })
                .isTrue()
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
        textUthmani = null,
        textIndopak = null,
        ayahNumberInSurah = null,
    )

    private companion object {
        const val TOTAL_AYAHS = 6236
        const val TOTAL_SURAHS = 114
    }
}
