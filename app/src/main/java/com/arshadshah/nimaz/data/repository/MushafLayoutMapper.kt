package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.MushafLayoutLineRow
import com.arshadshah.nimaz.domain.model.MushafLine
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafWord

/**
 * Groups the flat `(page, line)` segments returned by
 * [com.arshadshah.nimaz.data.local.database.dao.QuranDao.getMushafLayoutByPage] into the
 * printed line structure of [MushafPageLayout] (sub-task 4/7 of #263).
 *
 * Each DB row is one *segment*: a contiguous run of one ayah's words on a single line,
 * stored (2/7) as an inclusive `first..last` position range into the ayah's space-split
 * `text_indopak`. A printed line may hold several such segments (e.g. the tail of one ayah
 * and the head of the next), so ayah segments sharing a [MushafLayoutLineRow.line] are
 * concatenated in reading order into one [MushafLine].
 *
 * Structural rows — surah-header and basmalah — carry no words and are emitted as their own
 * [MushafLine] even when the source data places a header and its basmalah on the *same*
 * `line_number` (81 of the 112 basmalah-bearing surahs do; the remaining 31 give the
 * basmalah its own line). The renderer (5/7) draws the header cartouche with its bismillah
 * suppressed and the basmalah on a dedicated line, so collapsing a header+basmalah line into
 * a single header (the previous `groupBy{line}.first()` behaviour) silently dropped the
 * basmalah for those 81 surahs — a fidelity defect caught in the 7/7 verification pass. Each
 * structural row therefore maps 1:1 to a [MushafLine], preserving the printed
 * header-then-basmalah stack.
 *
 * Pure and Android-free so it is unit-tested directly.
 */
object MushafLayoutMapper {

    fun toPageLayout(page: Int, rows: List<MushafLayoutLineRow>): MushafPageLayout {
        val lines = rows
            .groupBy { it.line }
            .toSortedMap()
            .flatMap { (lineNumber, segments) -> segments.toLines(page, lineNumber) }
        return MushafPageLayout(page = page, lines = lines)
    }

    /**
     * Expands one `line_number` group into its printed rows. Structural (header/basmalah)
     * rows each become their own word-less [MushafLine] in source order; every ayah segment
     * on the line concatenates into a single trailing ayah line. A line never mixes ayah and
     * structural rows in the shipped data, but handling both keeps the mapper total.
     */
    private fun List<MushafLayoutLineRow>.toLines(page: Int, lineNumber: Int): List<MushafLine> {
        val structural = ArrayList<MushafLine>(size)
        val ayahSegments = ArrayList<MushafLayoutLineRow>(size)
        for (row in this) {
            if (MushafLineType.fromString(row.lineType) == MushafLineType.AYAH) {
                ayahSegments += row
            } else {
                structural += MushafLine(
                    page = page,
                    lineNumber = lineNumber,
                    type = MushafLineType.fromString(row.lineType),
                    surahId = row.surahId,
                )
            }
        }
        if (ayahSegments.isEmpty()) return structural
        val ayahLine = MushafLine(
            page = page,
            lineNumber = lineNumber,
            type = MushafLineType.AYAH,
            surahId = ayahSegments.first().surahId,
            words = ayahSegments.flatMap { it.toWords() }
        )
        return structural + ayahLine
    }

    /**
     * Reconstructs a segment's glyph words by slicing the ayah's space-split `text_indopak`
     * with the inclusive [MushafLayoutLineRow.firstWordPosition]..[MushafLayoutLineRow.lastWordPosition]
     * range. Returns empty for header/basmalah rows (null ayah/positions/text).
     */
    private fun MushafLayoutLineRow.toWords(): List<MushafWord> {
        val ayah = ayahId ?: return emptyList()
        val from = firstWordPosition ?: return emptyList()
        val to = lastWordPosition ?: return emptyList()
        val glyphWords = textIndopak?.split(" ") ?: return emptyList()
        return (from..to).map { position ->
            MushafWord(
                text = glyphWords.getOrNull(position - 1).orEmpty(),
                ayahId = ayah,
                ayahNumber = ayahNumberInSurah ?: 0,
                position = position
            )
        }
    }
}
