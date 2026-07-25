package com.arshadshah.nimaz.domain.model

/**
 * The line-accurate layout of a single page of the 16-line IndoPak Mushaf (sub-task 4/7 of
 * #263). Unlike the ayah-keyed [PageAyahRange], this groups a page's content the way it is
 * *printed*: one [MushafLine] per physical line, in order, so the renderer (5/7) can draw
 * ayah lines, surah-header cartouches and basmalah lines exactly where the printed Mushaf
 * breaks them — the property that makes a 16-line view usable for hifz.
 */
data class MushafPageLayout(
    val page: Int,
    val lines: List<MushafLine>
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

/**
 * One physical line of a 16-line Mushaf page.
 *
 * - [type] == [MushafLineType.AYAH]: [words] holds the line's glyph words in reading order.
 *   A single printed line can span more than one ayah, so consecutive [words] may carry
 *   different [MushafWord.ayahId] / [MushafWord.ayahNumber].
 * - [type] == [MushafLineType.SURAH_HEADER] / [MushafLineType.BASMALAH]: [words] is empty;
 *   the renderer uses [surahId] to draw the cartouche / centred basmalah.
 */
data class MushafLine(
    val page: Int,
    val lineNumber: Int,
    val type: MushafLineType,
    val surahId: Int,
    val words: List<MushafWord> = emptyList()
)

/** A single glyph word of an ayah as positioned on a Mushaf line. */
data class MushafWord(
    val text: String,
    val ayahId: Int,      // global ayah id (1-6236)
    val ayahNumber: Int,  // ayah number within its surah
    val position: Int     // 1-based position of the word within its ayah
)

enum class MushafLineType {
    AYAH,
    SURAH_HEADER,
    BASMALAH;

    companion object {
        /**
         * Maps the DB `line_type` string (see
         * [com.arshadshah.nimaz.data.local.database.entity.MushafLayoutIndopak16Entity]) to
         * the typed enum; unknown values fall back to [AYAH].
         */
        fun fromString(value: String): MushafLineType = when (value.lowercase()) {
            "surah_header" -> SURAH_HEADER
            "basmalah" -> BASMALAH
            else -> AYAH
        }
    }
}

/**
 * The Mushaf edition a page count belongs to. The classic Madani (Uthmani) mushaf
 * paginates to 604 pages; the 16-line IndoPak edition to 548. Callers that need "how many
 * pages does the Quran have" should ask the active [MushafScript] rather than hardcoding a
 * literal, so the total stays correct when the 16-line view is selected.
 */
enum class MushafScript(val totalPages: Int) {
    MADANI(604),
    INDOPAK_16(548);

    companion object {
        val DEFAULT = MADANI
    }
}
