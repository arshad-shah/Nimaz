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
 * A Mushaf edition the reader can display — the single source of truth for everything that
 * varies between editions. Callers that need "how many pages does the Quran have" or "is
 * this edition line-accurate" ask the active [MushafScript] rather than testing for a
 * specific entry, so adding an edition stays a one-entry change here plus its data assets.
 *
 * Two kinds of edition exist:
 *
 * - **Ayah-flow** ([linesPerPage] `== null`): pagination comes from the `ayahs.page` column
 *   and the renderer flows ayahs into the page. [MADANI] is the classic 604-page Uthmani
 *   mushaf and works this way.
 * - **Line-accurate** ([linesPerPage] `!= null`): pagination and every line break come from
 *   `mushaf_layout_lines`, so each printed line is reproduced exactly — the property that
 *   makes an edition usable for hifz. Its glyph text comes from [textSource].
 *
 * @property totalPages pages in this edition's pagination.
 * @property linesPerPage printed lines per page, or null when the edition is ayah-flow.
 * @property textSource key into `mushaf_ayah_texts` for this edition's glyphs, or null when
 *   the edition renders the `ayahs` table's own Uthmani text. Editions that set identical
 *   glyphs share one source: [INDOPAK_16] and [INDOPAK_15] are verified identical, while
 *   [INDOPAK_13] differs in the vowel marks of 28 ayahs and so carries its own.
 */
enum class MushafScript(
    val totalPages: Int,
    val linesPerPage: Int? = null,
    val textSource: String? = null
) {
    MADANI(totalPages = 604),
    INDOPAK_16(totalPages = 548, linesPerPage = 16, textSource = "INDOPAK"),
    INDOPAK_15(totalPages = 610, linesPerPage = 15, textSource = "INDOPAK"),
    INDOPAK_13(totalPages = 847, linesPerPage = 13, textSource = "INDOPAK_13");

    /**
     * True when every printed line of this edition is reproduced from stored layout data
     * rather than flowed. Replaces the old "is this the 16-line edition" test, which stopped
     * being equivalent the moment a second line-accurate edition existed.
     */
    val isLineAccurate: Boolean get() = linesPerPage != null

    companion object {
        val DEFAULT = MADANI

        /** The largest page count across editions — the safe upper bound for a page number
         *  whose target edition isn't known yet (e.g. a deep-link resolved before the user's
         *  script preference is read). The reader clamps to the active edition afterwards. */
        val MAX_TOTAL_PAGES: Int = entries.maxOf { it.totalPages }

        /** Parse a stored enum name (see PreferencesDataStore.quranMushafScript), falling
         *  back to [DEFAULT] for null / legacy / unknown values so the reader never breaks. */
        fun fromName(name: String?): MushafScript =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
