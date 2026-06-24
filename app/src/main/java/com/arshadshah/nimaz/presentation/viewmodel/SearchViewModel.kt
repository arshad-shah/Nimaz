package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

sealed class UnifiedSearchResult {
    data class QuranResult(val result: QuranSearchResult) : UnifiedSearchResult()
    data class HadithResult(val result: HadithSearchResult) : UnifiedSearchResult()
    data class DuaResult(val result: DuaSearchResult) : UnifiedSearchResult()
    data class SurahResult(val surah: Surah) : UnifiedSearchResult()
}

enum class SearchFilter {
    ALL, QURAN, HADITH, DUA
}

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
    val suggestions: List<String> = emptyList(),
    val error: String? = null
)

data class SearchStatsUiState(
    val totalResults: Int = 0,
    val quranCount: Int = 0,
    val hadithCount: Int = 0,
    val duaCount: Int = 0,
    val surahCount: Int = 0
)

sealed interface SearchEvent {
    data class UpdateQuery(val query: String) : SearchEvent
    data class SetFilter(val filter: SearchFilter) : SearchEvent
    data class SelectRecentSearch(val query: String) : SearchEvent
    data class RemoveRecentSearch(val query: String) : SearchEvent
    data object ExecuteSearch : SearchEvent
    data object ClearSearch : SearchEvent
    data object ClearRecentSearches : SearchEvent
}

/** Idle time after the last keystroke before a search-as-you-type lookup fires. */
private const val SEARCH_DEBOUNCE_MS = 300L

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val hadithUseCases: HadithUseCases,
    private val duaUseCases: DuaUseCases
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _statsState = MutableStateFlow(SearchStatsUiState())
    val statsState: StateFlow<SearchStatsUiState> = _statsState.asStateFlow()

    private val recentSearchesList = mutableListOf<String>()
    private val pendingSearchCount = AtomicInteger(0)

    // The in-flight search (debounce + the per-source lookups it launches as children).
    // Cancelled and replaced on each new query so stale results never clobber newer ones.
    private var searchJob: Job? = null

    fun onEvent(event: SearchEvent) {
        // Record the search action with its filter and query length only — never
        // the query text itself.
        if (event is SearchEvent.ExecuteSearch) {
            AppAnalytics.logSearch(
                filter = _searchState.value.selectedFilter.name,
                queryLength = _searchState.value.query.trim().length,
            )
        }
        when (event) {
            is SearchEvent.UpdateQuery -> updateQuery(event.query)
            is SearchEvent.SetFilter -> setFilter(event.filter)
            is SearchEvent.SelectRecentSearch -> selectRecentSearch(event.query)
            is SearchEvent.RemoveRecentSearch -> removeRecentSearch(event.query)
            SearchEvent.ExecuteSearch -> executeSearch()
            SearchEvent.ClearSearch -> clearSearch()
            SearchEvent.ClearRecentSearches -> clearRecentSearches()
        }
    }

    private fun updateQuery(query: String) {
        _searchState.update { it.copy(query = query) }

        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            searchJob?.cancel()
            clearResults()
            return
        }
        // Search-as-you-type, debounced. The recent-searches list is only touched on an
        // explicit submit (enter / recent tap), not on every keystroke.
        launchSearch(trimmed, addToRecent = false, debounceMillis = SEARCH_DEBOUNCE_MS)
    }

    private fun setFilter(filter: SearchFilter) {
        _searchState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredResults = applyFilter(state.allResults, filter)
            )
        }
        updateStats()
    }

    private fun selectRecentSearch(query: String) {
        _searchState.update { it.copy(query = query) }
        executeSearch()
    }

    private fun removeRecentSearch(query: String) {
        recentSearchesList.remove(query)
        _searchState.update { it.copy(recentSearches = recentSearchesList.toList()) }
    }

    private fun executeSearch() {
        val query = _searchState.value.query.trim()
        if (query.isBlank()) {
            searchJob?.cancel()
            clearResults()
            return
        }
        // Explicit submit (enter / recent search): search immediately and remember it.
        launchSearch(query, addToRecent = true, debounceMillis = 0L)
    }

    /**
     * Cancel any in-flight search and start a new one. [isSearching] flips on synchronously
     * (before the debounce delay) so the screen's "no results" state can never show while a
     * lookup is still pending. The per-source lookups are launched as children of [searchJob],
     * so cancelling it cancels them too.
     */
    private fun launchSearch(query: String, addToRecent: Boolean, debounceMillis: Long) {
        searchJob?.cancel()
        _searchState.update { it.copy(isSearching = true, error = null) }
        if (addToRecent) addToRecentSearches(query)
        searchJob = viewModelScope.launch {
            if (debounceMillis > 0) delay(debounceMillis)
            when (_searchState.value.selectedFilter) {
                SearchFilter.ALL -> searchAll(query, this)
                SearchFilter.QURAN -> searchQuranOnly(query, this)
                SearchFilter.HADITH -> searchHadithOnly(query, this)
                SearchFilter.DUA -> searchDuaOnly(query, this)
            }
        }
    }

    private fun searchAll(query: String, scope: CoroutineScope) {
        val totalSearches = 4
        pendingSearchCount.set(totalSearches)

        fun onSearchComplete() {
            if (pendingSearchCount.decrementAndGet() <= 0) {
                _searchState.update { it.copy(isSearching = false) }
            }
        }

        // Search Quran (include translations for English search terms)
        scope.launch {
            quranUseCases.searchQuran(query, "sahih_international").collect { results ->
                _searchState.update { state ->
                    val unified =
                        state.allResults.filterNot { it is UnifiedSearchResult.QuranResult } +
                                results.map { UnifiedSearchResult.QuranResult(it) }
                    state.copy(
                        quranResults = results,
                        allResults = unified,
                        filteredResults = applyFilter(unified, state.selectedFilter)
                    )
                }
                updateStats()
                onSearchComplete()
            }
        }

        // Search Surahs by name
        scope.launch {
            quranUseCases.getSurahList.search(query).collect { surahs ->
                _searchState.update { state ->
                    val unified =
                        state.allResults.filterNot { it is UnifiedSearchResult.SurahResult } +
                                surahs.map { UnifiedSearchResult.SurahResult(it) }
                    state.copy(
                        surahResults = surahs,
                        allResults = unified,
                        filteredResults = applyFilter(unified, state.selectedFilter)
                    )
                }
                updateStats()
                onSearchComplete()
            }
        }

        // Search Hadith
        scope.launch {
            hadithUseCases.searchHadiths(query).collect { results ->
                _searchState.update { state ->
                    val unified =
                        state.allResults.filterNot { it is UnifiedSearchResult.HadithResult } +
                                results.map { UnifiedSearchResult.HadithResult(it) }
                    state.copy(
                        hadithResults = results,
                        allResults = unified,
                        filteredResults = applyFilter(unified, state.selectedFilter)
                    )
                }
                updateStats()
                onSearchComplete()
            }
        }

        // Search Duas
        scope.launch {
            duaUseCases.searchDuas(query).collect { results ->
                _searchState.update { state ->
                    val unified =
                        state.allResults.filterNot { it is UnifiedSearchResult.DuaResult } +
                                results.map { UnifiedSearchResult.DuaResult(it) }
                    state.copy(
                        duaResults = results,
                        allResults = unified,
                        filteredResults = applyFilter(unified, state.selectedFilter)
                    )
                }
                updateStats()
                onSearchComplete()
            }
        }
    }

    private fun searchQuranOnly(query: String, scope: CoroutineScope) {
        val totalSearches = 2
        pendingSearchCount.set(totalSearches)

        fun onSearchComplete() {
            if (pendingSearchCount.decrementAndGet() <= 0) {
                _searchState.update { it.copy(isSearching = false) }
            }
        }

        scope.launch {
            quranUseCases.searchQuran(query, "sahih_international").collect { results ->
                val unified = results.map { UnifiedSearchResult.QuranResult(it) }
                _searchState.update {
                    it.copy(
                        quranResults = results,
                        allResults = unified,
                        filteredResults = unified
                    )
                }
                updateStats()
                onSearchComplete()
            }
        }

        scope.launch {
            quranUseCases.getSurahList.search(query).collect { surahs ->
                _searchState.update { state ->
                    val surahResults = surahs.map { UnifiedSearchResult.SurahResult(it) }
                    val combined = state.allResults + surahResults
                    state.copy(
                        surahResults = surahs,
                        allResults = combined,
                        filteredResults = combined
                    )
                }
                updateStats()
                onSearchComplete()
            }
        }
    }

    private fun searchHadithOnly(query: String, scope: CoroutineScope) {
        scope.launch {
            hadithUseCases.searchHadiths(query).collect { results ->
                val unified = results.map { UnifiedSearchResult.HadithResult(it) }
                _searchState.update {
                    it.copy(
                        hadithResults = results,
                        allResults = unified,
                        filteredResults = unified,
                        isSearching = false
                    )
                }
                updateStats()
            }
        }
    }

    private fun searchDuaOnly(query: String, scope: CoroutineScope) {
        scope.launch {
            duaUseCases.searchDuas(query).collect { results ->
                val unified = results.map { UnifiedSearchResult.DuaResult(it) }
                _searchState.update {
                    it.copy(
                        duaResults = results,
                        allResults = unified,
                        filteredResults = unified,
                        isSearching = false
                    )
                }
                updateStats()
            }
        }
    }

    private fun applyFilter(
        results: List<UnifiedSearchResult>,
        filter: SearchFilter
    ): List<UnifiedSearchResult> {
        return when (filter) {
            SearchFilter.ALL -> results
            SearchFilter.QURAN -> results.filter { it is UnifiedSearchResult.QuranResult || it is UnifiedSearchResult.SurahResult }
            SearchFilter.HADITH -> results.filter { it is UnifiedSearchResult.HadithResult }
            SearchFilter.DUA -> results.filter { it is UnifiedSearchResult.DuaResult }
        }
    }

    private fun clearSearch() {
        _searchState.update {
            SearchUiState(recentSearches = it.recentSearches)
        }
        _statsState.update { SearchStatsUiState() }
    }

    private fun clearResults() {
        _searchState.update { state ->
            state.copy(
                allResults = emptyList(),
                filteredResults = emptyList(),
                quranResults = emptyList(),
                hadithResults = emptyList(),
                duaResults = emptyList(),
                surahResults = emptyList(),
                isSearching = false
            )
        }
        _statsState.update { SearchStatsUiState() }
    }

    private fun addToRecentSearches(query: String) {
        recentSearchesList.remove(query) // Remove if exists to move to top
        recentSearchesList.add(0, query)
        if (recentSearchesList.size > 10) {
            recentSearchesList.removeAt(recentSearchesList.lastIndex)
        }
        _searchState.update { it.copy(recentSearches = recentSearchesList.toList()) }
    }

    private fun clearRecentSearches() {
        recentSearchesList.clear()
        _searchState.update { it.copy(recentSearches = emptyList()) }
    }

    private fun updateStats() {
        val state = _searchState.value
        _statsState.update {
            SearchStatsUiState(
                totalResults = state.filteredResults.size,
                quranCount = state.quranResults.size,
                hadithCount = state.hadithResults.size,
                duaCount = state.duaResults.size,
                surahCount = state.surahResults.size
            )
        }
    }
}
