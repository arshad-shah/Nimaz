package com.arshadshah.nimaz.presentation.viewmodel.search

import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.Surah

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val allResults: List<UnifiedSearchResult> = emptyList(),
    val filteredResults: List<UnifiedSearchResult> = emptyList(),
    val quranResults: List<QuranSearchResult> = emptyList(),
    val hadithResults: List<HadithSearchResult> = emptyList(),
    val duaResults: List<DuaSearchResult> = emptyList(),
    val surahResults: List<Surah> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val error: String? = null
)

data class SearchStatsUiState(
    val totalResults: Int = 0,
    val quranCount: Int = 0,
    val hadithCount: Int = 0,
    val duaCount: Int = 0,
    val surahCount: Int = 0
)
