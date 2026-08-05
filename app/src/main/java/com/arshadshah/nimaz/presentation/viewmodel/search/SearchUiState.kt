package com.arshadshah.nimaz.presentation.viewmodel.search

import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.UnifiedSearchResult
import com.arshadshah.nimaz.presentation.viewmodel.UiError

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
    val error: UiError? = null
)

/**
 * The result count the screen renders, and nothing else.
 *
 * There were four more fields here — `quranCount`, `hadithCount`, `duaCount`, `surahCount` —
 * written on every search and read by no screen. They were not merely dead, they were *wrong*
 * in a way that would only have surfaced once someone rendered them: [totalResults] counted
 * `filteredResults` while the four counted the **unfiltered** per-corpus lists, so a HADITH
 * filter over 3 hadith and 40 Qur'an matches reported `totalResults = 3` beside
 * `quranCount = 40`. `applyFilter` also groups `SurahResult` under the QURAN filter while
 * `quranCount` counted only `quranResults`, so the two disagreed in the other direction too.
 * Deleted rather than corrected: no design calls for per-corpus counts, and the filter chips
 * already say which corpus is showing.
 */
data class SearchStatsUiState(
    val totalResults: Int = 0
)
