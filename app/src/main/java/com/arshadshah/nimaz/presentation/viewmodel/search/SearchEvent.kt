package com.arshadshah.nimaz.presentation.viewmodel.search

sealed interface SearchEvent {
    data class UpdateQuery(val query: String) : SearchEvent
    data class SetFilter(val filter: SearchFilter) : SearchEvent
    data class SelectRecentSearch(val query: String) : SearchEvent
    data class RemoveRecentSearch(val query: String) : SearchEvent
    data object ExecuteSearch : SearchEvent
    data object ClearSearch : SearchEvent
    data object ClearRecentSearches : SearchEvent

    /**
     * Replace the results list with matches for the AI's related search terms.
     * Emitted by the Search screen after an AI answer, so the list reflects what
     * the AI judged relevant. Purely local — no network call; the terms came
     * with the answer.
     */
    data class ApplyAiTerms(val terms: List<String>) : SearchEvent
}
