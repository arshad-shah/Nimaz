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
 * and the head of the next), so rows are grouped by [MushafLayoutLineRow.line] and their
 * words concatenated in reading order. Surah-header / basmalah rows carry no words.
 *
 * Pure and Android-free so it is unit-tested directly.
 */
object MushafLayoutMapper {

    fun toPageLayout(page: Int, rows: List<MushafLayoutLineRow>): MushafPageLayout {
        val lines = rows
            .groupBy { it.line }
            .toSortedMap()
            .map { (lineNumber, segments) -> segments.toLine(page, lineNumber) }
        return MushafPageLayout(page = page, lines = lines)
    }

    private fun List<MushafLayoutLineRow>.toLine(page: Int, lineNumber: Int): MushafLine {
        val first = first()
        return MushafLine(
            page = page,
            lineNumber = lineNumber,
            type = MushafLineType.fromString(first.lineType),
            surahId = first.surahId,
            words = flatMap { it.toWords() }
        )
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
