package com.arshadshah.nimaz.domain.model

/**
 * The line-accurate layout of a single page of a line-accurate Mushaf edition (sub-task 4/7
 * of #263). Unlike the ayah-keyed [PageAyahRange], this groups a page's content the way it is
 * *printed*: one [MushafLine] per physical line, in order, so the renderer (5/7) can draw
 * ayah lines, surah-header cartouches and basmalah lines exactly where the printed Mushaf
 * breaks them — the property that makes such a view usable for hifz.
 *
 * Nothing here is tied to a particular line count: the models describe whatever N lines the
 * active edition prints, so a 15- or 14-line edition renders through the same path. Which
 * edition a page belongs to is the caller's context — see
 * [com.arshadshah.nimaz.domain.model.quran.catalogue.MushafLayoutEdition].
 */
data class MushafPageLayout(
    val page: Int,
    val lines: List<MushafLine>
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

/**
 * One physical line of a printed Mushaf page.
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

// The Mushaf edition a page count belongs to used to be a `MushafScript` enum here, which
// meant every new layout was a code change in the domain model. It now lives in the content
// registry as `QuranEditions.mushafLayouts` — see
// com.arshadshah.nimaz.domain.model.quran.catalogue.MushafLayoutEdition. Values persisted as
// the old enum names (`MADANI`, `INDOPAK_16`) still resolve, via that edition's `legacyKeys`.
