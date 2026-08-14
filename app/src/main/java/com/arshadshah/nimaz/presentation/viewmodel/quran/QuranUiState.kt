@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.compose.ui.text.font.FontFamily
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import kotlinx.coroutines.flow.first

data class QuranHomeUiState(
    val surahs: List<Surah> = emptyList(),
    val readingProgress: ReadingProgress? = null,
    val activeKhatam: Khatam? = null,
    val activeKhatamInsights: KhatamInsights? = null,
    val khatamReadAyahIds: Set<Int> = emptySet(),
    val completedKhatamCount: Int = 0,
    val verseOfTheDay: Ayah? = null,
    // The active Mushaf edition, so the Page tab's jump-to-page validates against the right
    // page count (604 Uthmani vs 548 IndoPak-16, #270).
    val mushafScript: MushafScript = MushafScript.DEFAULT,
    /**
     * The active edition's page↔ayah mapping (#325). Drives the Page tab's tile count, its
     * juz sections, the surah→page badges and khatam page progress, so all of them reflow
     * when the Mushaf layout setting changes instead of staying pinned to the Madani 604.
     */
    val pagination: MushafPagination = MushafPagination.fallback(MushafScript.DEFAULT),
    /**
     * Whether this install's artifact carries the thematic layer, so the home screen offers a
     * way into it only where there is something behind the offer. False between the migration
     * that creates the tables and the schemaVersion 24 release that fills them.
     */
    val hasThematicContent: Boolean = false,
    val isLoading: Boolean = true,
)

data class QuranReaderUiState(
    val readingMode: ReadingMode = ReadingMode.SURAH,
    val surahWithAyahs: SurahWithAyahs? = null,
    val ayahs: List<Ayah> = emptyList(),
    val title: String = "",
    val subtitle: String = "",
    val currentAyahIndex: Int = 0,
    val isLoading: Boolean = true,
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val selectedTranslatorId: String = "sahih_international",
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 28f,
    val arabicFontFamily: FontFamily = AmiriFontFamily,
    val keepScreenOn: Boolean = true,
    val continuousReading: Boolean = true,
    val favoriteAyahIds: Set<Int> = emptySet(),
    val pageCache: Map<Int, List<Ayah>> = emptyMap(),
    val showTajweed: Boolean = false,
    val tajweedUnderline: Boolean = false,
    val activeKhatamId: Long? = null,
    val khatamReadAyahIds: Set<Int> = emptySet(),
    // Per-page cache of line-accurate layouts (5/7). The reader pager keeps several pages
    // resident at once, so — mirroring [pageCache] — each visible page's layout is cached by
    // page number rather than a single field.
    val mushafPageLayoutCache: Map<Int, MushafPageLayout> = emptyMap(),
    // The Mushaf edition the reader renders, driven by the persisted settings picker (6/7,
    // #270). MADANI (default) keeps the ayah-flow Uthmani/604 page; the IndoPak editions
    // switch to their line-accurate layouts.
    val mushafScript: MushafScript = MushafScript.DEFAULT,
    /**
     * The active edition's page↔ayah mapping — the same object the Page tab reads, so the
     * pager's bounds come from the edition's real page ranges rather than the count declared
     * on the enum.
     */
    val pagination: MushafPagination = MushafPagination.fallback(MushafScript.DEFAULT),
    /**
     * The passage outline of the surah being read (schemaVersion 24), used to print a heading
     * where the mushaf's own outline starts a new subject. Empty in juz and page mode, and on
     * an install whose artifact predates the thematic layer.
     */
    val passages: List<AyahTheme> = emptyList(),
    /**
     * Notes the reader has written, by ayah id. A note lives on the verse's bookmark row, so
     * this is the annotated subset of the bookmarks the app already collects — carried here so
     * the ayah sheet's note editor opens on what is already written rather than on a blank
     * field that silently overwrites it.
     */
    val ayahNotes: Map<Int, String> = emptyMap(),
) {
    /** Whether to render stored line-accurate pages instead of flowing ayahs into a page. */
    val useLineAccurateLayout: Boolean get() = mushafScript.isLineAccurate

    /**
     * Passage headings by the verse number each one opens on, so the list can ask "does a
     * passage start here" per row instead of scanning 282 ranges for every one of them.
     */
    val passageStarts: Map<Int, AyahTheme> by lazy { passages.associateBy { it.ayahFrom } }

    /**
     * Language of the active translation — what its prose must be *drawn* in (face, direction
     * and leading; see [com.arshadshah.nimaz.presentation.theme.asTranslationText]). Derived
     * here rather than at each render site so the reader, the page view and the ayah sheet
     * cannot drift apart on it.
     */
    val translationLanguage: TranslationLanguage
        get() = QuranTranslation.fromId(selectedTranslatorId).language

    /** Number of pages in the active edition — the pager/nav bounds source of truth. */
    val totalPages: Int get() = pagination.totalPages
}

data class QuranSearchUiState(
    val query: String = "",
    val results: List<QuranSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

data class QuranBookmarksUiState(
    val bookmarks: List<QuranBookmark> = emptyList(),
    val isLoading: Boolean = true
)
