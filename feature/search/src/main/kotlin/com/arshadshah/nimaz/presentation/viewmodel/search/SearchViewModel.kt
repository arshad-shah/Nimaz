package com.arshadshah.nimaz.presentation.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.PerfMonitor
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.LibrarySearchResults
import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.UnifiedSearchResult
import com.arshadshah.nimaz.domain.usecase.ObserveSearchPreferencesUseCase
import com.arshadshah.nimaz.domain.usecase.SearchLibraryUseCase
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchFilter {
    ALL, QURAN, HADITH, DUA, NAMES
}

/**
 * The chip that corresponds to a [LibrarySource], with `null` — "no default" — as [ALL].
 *
 * Exhaustive on purpose: a source added to [LibrarySource] stops this compiling until it has
 * a chip to filter by, which is the only thing that keeps the two lists from drifting.
 */
private fun LibrarySource?.asFilter(): SearchFilter = when (this) {
    LibrarySource.QURAN -> SearchFilter.QURAN
    LibrarySource.HADITH -> SearchFilter.HADITH
    LibrarySource.DUAS -> SearchFilter.DUA
    LibrarySource.NAMES -> SearchFilter.NAMES
    null -> SearchFilter.ALL
}

/** Idle time after the last keystroke before a search-as-you-type lookup fires. */
private const val SEARCH_DEBOUNCE_MS = 300L
private const val DOMAIN = "global_search"

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchLibrary: SearchLibraryUseCase,
    private val searchPreferences: ObserveSearchPreferencesUseCase,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _statsState = MutableStateFlow(SearchStatsUiState())
    val statsState: StateFlow<SearchStatsUiState> = _statsState.asStateFlow()

    private val recentSearchesList = mutableListOf<String>()

    // The in-flight search (debounce + lookup). Cancelled and replaced on each
    // new query so stale results never clobber newer ones.
    private var searchJob: Job? = null

    /**
     * Whether a filter has been chosen for *this* screen — by a tap, or by the screen being
     * opened scoped (Duas → search passes `initialFilter`).
     *
     * The stored default scope is read asynchronously, so without this it would race the
     * screen's `LaunchedEffect(initialFilter)` and sometimes overwrite it. The precedence is
     * not in doubt — "search duas", said by opening search from duas, beats "usually start on
     * hadith", said once in settings — so the flag settles it rather than the scheduler.
     */
    private var filterChosenForThisScreen = false

    init {
        viewModelScope.launch {
            val defaultScope = searchPreferences().first().defaultScope
            if (!filterChosenForThisScreen && defaultScope != null) {
                setFilter(defaultScope.asFilter())
            }
        }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.UpdateQuery -> updateQuery(event.query)
            // The filter is the one place this screen learns what people are looking for
            // without recording what they typed — which of Qur'an, hadith or dua they narrow
            // to is the shape, and it was not recorded at all.
            is SearchEvent.SetFilter -> {
                filterChosenForThisScreen = true
                telemetry.featureUsed(DOMAIN, "set_filter_" + event.filter.name.lowercase())
                setFilter(event.filter)
            }

            is SearchEvent.SelectRecentSearch -> selectRecentSearch(event.query)
            is SearchEvent.RemoveRecentSearch -> {
                telemetry.featureUsed(DOMAIN, "remove_recent")
                removeRecentSearch(event.query)
            }

            SearchEvent.ExecuteSearch -> executeSearch()
            SearchEvent.ClearSearch -> {
                telemetry.featureUsed(DOMAIN, "clear")
                clearSearch()
            }

            SearchEvent.ClearRecentSearches -> {
                telemetry.featureUsed(DOMAIN, "clear_recents")
                clearRecentSearches()
            }

            is SearchEvent.ApplyAiTerms -> applyAiTerms(event.terms)
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
        launchSearch(debounceMillis = SEARCH_DEBOUNCE_MS) { searchLibrary(trimmed) }
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
        addToRecentSearches(query)
        launchSearch(debounceMillis = 0L) { searchLibrary(query) }
    }

    /**
     * Populate the results list from the AI's related terms (Global Search only).
     * Runs the terms through the smart local search so the list shows what the
     * AI judged relevant. Replaces the debounced keyword search that ran while
     * the user was typing.
     */
    private fun applyAiTerms(terms: List<String>) {
        val cleaned = terms.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleaned.isEmpty()) return
        launchSearch(debounceMillis = 0L) { searchLibrary.byTerms(cleaned) }
    }

    /**
     * Cancel any in-flight search and start a new one. [SearchUiState.isSearching]
     * flips on synchronously (before the debounce delay) so the screen's
     * "no results" state can never show while a lookup is still pending.
     */
    private fun launchSearch(
        debounceMillis: Long,
        lookup: suspend () -> LibrarySearchResults,
    ) {
        searchJob?.cancel()
        _searchState.update { it.copy(isSearching = true, error = null) }
        searchJob = launchSafely(telemetry, DOMAIN, "launch_search") {
            if (debounceMillis > 0) delay(debounceMillis)
            // Logged here rather than in `onEvent`, which is both too early and too narrow.
            // Too early: it fired before `executeSearch` had checked the query, so tapping the
            // keyboard's search key on an empty box emitted `search_performed` with
            // `query_length = 0` and no search ran. Too narrow: it was on `ExecuteSearch` only,
            // so search-as-you-type — the way this screen is actually used — was never counted
            // at all. Past the debounce and inside the job that survived cancellation, this
            // fires once per search that genuinely runs, typed or submitted.
            // Length and filter only; the query itself never reaches analytics.
            telemetry.search(
                filter = _searchState.value.selectedFilter.name,
                queryLength = _searchState.value.query.trim().length,
            )
            val results = runCatching {
                telemetry.trace(PerfMonitor.Traces.LIBRARY_SEARCH) { lookup() }
            }.getOrElse { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _searchState.update {
                    it.copy(
                        isSearching = false,
                        error = UiError(
                            message = R.string.search_failed,
                            details = e.message,
                        ),
                    )
                }
                return@launchSafely
            }
            publish(results)
        }
    }

    private fun publish(results: LibrarySearchResults) {
        val unified = results.quran.map { UnifiedSearchResult.QuranResult(it) } +
                results.surahs.map { UnifiedSearchResult.SurahResult(it) } +
                results.hadith.map { UnifiedSearchResult.HadithResult(it) } +
                results.duas.map { UnifiedSearchResult.DuaResult(it) } +
                results.names.map { UnifiedSearchResult.NameResult(it) }
        _searchState.update { state ->
            state.copy(
                quranResults = results.quran,
                surahResults = results.surahs,
                hadithResults = results.hadith,
                duaResults = results.duas,
                nameResults = results.names,
                allResults = unified,
                filteredResults = applyFilter(unified, state.selectedFilter),
                isSearching = false,
            )
        }
        updateStats()
    }

    private fun applyFilter(
        results: List<UnifiedSearchResult>,
        filter: SearchFilter
    ): List<UnifiedSearchResult> {
        // One predicate, shared with the chip counts — see `SearchFilter.accepts`.
        return results.filter { filter.accepts(it) }
    }

    private fun clearSearch() {
        searchJob?.cancel()
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

    /**
     * The count the screen shows is the count of what the screen is showing — `filteredResults`,
     * the same list the results column renders. See [SearchStatsUiState] for the four per-corpus
     * counts that used to sit beside it and disagree with it.
     */
    private fun updateStats() {
        _statsState.update {
            SearchStatsUiState(totalResults = _searchState.value.filteredResults.size)
        }
    }
}
