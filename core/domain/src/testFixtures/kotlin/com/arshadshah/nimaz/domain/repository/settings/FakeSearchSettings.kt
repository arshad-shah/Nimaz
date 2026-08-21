package com.arshadshah.nimaz.domain.repository.settings

import com.arshadshah.nimaz.domain.model.MatchStrictness
import com.arshadshah.nimaz.domain.model.SearchPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The four stored search preferences, writable.
 *
 * Writable rather than fixed because the settings screen's whole job is writing them, so a test
 * of it has to be able to read back what it wrote.
 *
 * A test fixture rather than a plain test class because both sides of the seam need it: the
 * use-case tests that live in `:core:domain` and the ViewModel tests that live in `:app`.
 */
class FakeSearchSettings(
    resultsPerSource: Int = SearchPreferences.DEFAULT_RESULTS_PER_SOURCE,
    sources: String = "",
    strictness: String = MatchStrictness.BALANCED.name,
    defaultScope: String = "",
) : SearchSettings {
    private val perSourceFlow = MutableStateFlow(resultsPerSource)
    private val sourcesFlow = MutableStateFlow(sources)
    private val strictnessFlow = MutableStateFlow(strictness)
    private val scopeFlow = MutableStateFlow(defaultScope)

    override val searchResultsPerSource: Flow<Int> = perSourceFlow
    override suspend fun setSearchResultsPerSource(count: Int) {
        perSourceFlow.value = count
    }

    override val searchSources: Flow<String> = sourcesFlow
    override suspend fun setSearchSources(sources: String) {
        sourcesFlow.value = sources
    }

    override val searchStrictness: Flow<String> = strictnessFlow
    override suspend fun setSearchStrictness(strictness: String) {
        strictnessFlow.value = strictness
    }

    override val searchDefaultScope: Flow<String> = scopeFlow
    override suspend fun setSearchDefaultScope(scope: String) {
        scopeFlow.value = scope
    }
}
