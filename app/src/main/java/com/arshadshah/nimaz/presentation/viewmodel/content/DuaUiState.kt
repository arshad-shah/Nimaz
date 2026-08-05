package com.arshadshah.nimaz.presentation.viewmodel.content

import androidx.annotation.StringRes
import androidx.compose.ui.text.font.FontFamily
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily

data class DuaCollectionUiState(
    val categories: List<DuaCategory> = emptyList(),
    val filteredCategories: List<DuaCategory> = emptyList(),
    val searchQuery: String = "",
    val sortAlphabetical: Boolean = false,
    val isLoading: Boolean = true,
    /**
     * What to tell the user, as a string resource the screen resolves in their language.
     *
     * Deliberately not the exception's `message`: this state used to carry `e.message`
     * straight to a `Text`, so a content-database fault reached the user as
     * `SQLiteException: no such table: duas`. The throwable still goes to the crash
     * report, which is the only place its wording is any use.
     */
    @StringRes val error: Int? = null
)

data class DuaCategoryUiState(
    val category: DuaCategory? = null,
    val duas: List<Dua> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val error: Int? = null
)

data class DuaReaderUiState(
    val duas: List<Dua> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true,
    @StringRes val error: Int? = null,
    val showArabic: Boolean = true,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 28f,
    val arabicFontFamily: FontFamily = AmiriFontFamily,
    val selectedArabicFontId: String = "amiri"
)

data class DuaSearchUiState(
    val query: String = "",
    val results: List<DuaSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

data class DuaFavoritesUiState(
    val favorites: List<DuaBookmark> = emptyList(),
    val isLoading: Boolean = true
)

data class DuaDailyProgressUiState(
    val progressList: List<DuaProgress> = emptyList(),
    val date: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true
)
