package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.MushafLayoutLineRow
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [MushafLayoutMapper] — the data-layer grouping of flat `(page, line)`
 * segments into a printed [com.arshadshah.nimaz.domain.model.MushafPageLayout].
 *
 * Covers the two acceptance scenarios of #268: a header page (p.1 Al-Fātiḥah, exercising
 * surah-header + basmalah + ayah line types) and a mid-juz page (a line that spans two
 * ayahs, exercising word-position slicing and multi-segment concatenation).
 */
class MushafLayoutMapperTest {

    private fun ayahRow(
        page: Int,
        line: Int,
        ayahId: Int,
        ayahNumber: Int,
        first: Int,
        last: Int,
        textIndopak: String
    ) = MushafLayoutLineRow(
        page = page,
        line = line,
        lineType = "ayah",
        surahId = 1,
        ayahId = ayahId,
        firstWordPosition = first,
        lastWordPosition = last,
        textIndopak = textIndopak,
        ayahNumberInSurah = ayahNumber
    )

    private fun headerRow(page: Int, line: Int, type: String, surahId: Int) =
        MushafLayoutLineRow(
            page = page,
            line = line,
            lineType = type,
            surahId = surahId,
            ayahId = null,
            firstWordPosition = null,
            lastWordPosition = null,
            textIndopak = null,
            ayahNumberInSurah = null
        )

    @Test
    fun `header page groups typed lines in order with header and basmalah word-less`() {
        // p.1 Al-Fātiḥah: header cartouche, basmalah, then the first ayah line.
        val rows = listOf(
            headerRow(page = 1, line = 1, type = "surah_header", surahId = 1),
            headerRow(page = 1, line = 2, type = "basmalah", surahId = 1),
            ayahRow(
                page = 1, line = 3, ayahId = 2, ayahNumber = 2,
                first = 1, last = 2, textIndopak = "الْحَمْدُ لِلّٰهِ"
            )
        )

        val layout = MushafLayoutMapper.toPageLayout(page = 1, rows = rows)

        assertThat(layout.page).isEqualTo(1)
        assertThat(layout.lines).hasSize(3)

        val header = layout.lines[0]
        assertThat(header.type).isEqualTo(MushafLineType.SURAH_HEADER)
        assertThat(header.lineNumber).isEqualTo(1)
        assertThat(header.surahId).isEqualTo(1)
        assertThat(header.words).isEmpty()

        val basmalah = layout.lines[1]
        assertThat(basmalah.type).isEqualTo(MushafLineType.BASMALAH)
        assertThat(basmalah.words).isEmpty()

        val ayahLine = layout.lines[2]
        assertThat(ayahLine.type).isEqualTo(MushafLineType.AYAH)
        assertThat(ayahLine.words.map { it.text }).containsExactly("الْحَمْدُ", "لِلّٰهِ").inOrder()
        assertThat(ayahLine.words.map { it.position }).containsExactly(1, 2).inOrder()
        assertThat(ayahLine.words.all { it.ayahId == 2 && it.ayahNumber == 2 }).isTrue()
    }

    @Test
    fun `mid-juz line spanning two ayahs concatenates sliced words in reading order`() {
        // A single printed line holds the tail of one ayah and the head of the next.
        val rows = listOf(
            // tail of ayah 100: words 3..4 of a 5-word ayah
            ayahRow(
                page = 50, line = 7, ayahId = 100, ayahNumber = 5,
                first = 3, last = 4, textIndopak = "w1 w2 w3 w4 w5"
            ),
            // head of ayah 101: words 1..2 of a 3-word ayah
            ayahRow(
                page = 50, line = 7, ayahId = 101, ayahNumber = 6,
                first = 1, last = 2, textIndopak = "x1 x2 x3"
            )
        )

        val layout = MushafLayoutMapper.toPageLayout(page = 50, rows = rows)

        assertThat(layout.lines).hasSize(1)
        val line = layout.lines.single()
        assertThat(line.lineNumber).isEqualTo(7)
        assertThat(line.type).isEqualTo(MushafLineType.AYAH)

        // Only the sliced words, in reading order, each carrying its own ayah metadata.
        assertThat(line.words.map { it.text }).containsExactly("w3", "w4", "x1", "x2").inOrder()
        assertThat(line.words.map { it.ayahId }).containsExactly(100, 100, 101, 101).inOrder()
        assertThat(line.words.map { it.position }).containsExactly(3, 4, 1, 2).inOrder()
    }

    @Test
    fun `rows are grouped and ordered by line even when supplied out of order`() {
        val rows = listOf(
            ayahRow(page = 5, line = 3, ayahId = 10, ayahNumber = 1, first = 1, last = 1, textIndopak = "a"),
            ayahRow(page = 5, line = 1, ayahId = 8, ayahNumber = 1, first = 1, last = 1, textIndopak = "b"),
            ayahRow(page = 5, line = 2, ayahId = 9, ayahNumber = 1, first = 1, last = 1, textIndopak = "c")
        )

        val layout = MushafLayoutMapper.toPageLayout(page = 5, rows = rows)

        assertThat(layout.lines.map { it.lineNumber }).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `empty page yields an empty layout`() {
        val layout = MushafLayoutMapper.toPageLayout(page = 999, rows = emptyList())
        assertThat(layout.isEmpty).isTrue()
        assertThat(layout.lines).isEmpty()
    }
}
