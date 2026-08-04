package com.arshadshah.nimaz.presentation.viewmodel.content

import androidx.compose.ui.text.font.FontFamily
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily

data class HadithCollectionUiState(
    val books: List<HadithBook> = emptyList(),
    val hadithOfTheDay: Hadith? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HadithChaptersUiState(
    val book: HadithBook? = null,
    val chapters: List<HadithChapter> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /**
     * The chapters the list should show — **derived**, never stored.
     *
     * Stored, this had to be recomputed wherever an input changed, and `loadBook`'s Room
     * collector rebuilt it as the whole `chapters` list without consulting [searchQuery]. Any
     * write that re-emitted the chapters flow therefore wiped the user's search while the
     * search field kept showing what they had typed.
     */
    val filteredChapters: List<HadithChapter>
        get() = if (searchQuery.isBlank()) {
            chapters
        } else {
            chapters.filter { chapter ->
                chapter.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                        chapter.nameArabic.contains(searchQuery)
            }
        }
}

data class HadithReaderUiState(
    val chapter: HadithChapter? = null,
    val hadiths: List<Hadith> = emptyList(),
    val currentHadithIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showArabic: Boolean = true,
    val showTranslation: Boolean = true,
    val showGrade: Boolean = true,
    val showChain: Boolean = true,
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 24f,
    val arabicFontFamily: FontFamily = AmiriFontFamily,
    val selectedArabicFontId: String = "amiri"
)

data class HadithSearchUiState(
    val query: String = "",
    val results: List<HadithSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedBookId: String? = null
)

data class HadithBookmarksUiState(
    val bookmarks: List<HadithBookmark> = emptyList(),
    val isLoading: Boolean = true
)
