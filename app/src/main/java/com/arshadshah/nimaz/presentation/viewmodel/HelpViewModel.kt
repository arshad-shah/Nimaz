package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import com.arshadshah.nimaz.domain.usecase.HelpUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HelpHomeUiState(
    val topics: List<HelpTopic> = emptyList(),
    val query: String = "",
    val results: List<HelpSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HelpTopicUiState(
    val detail: HelpTopicDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HelpGuideUiState(
    val guide: HelpGuideDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface HelpEvent {
    data class Search(val query: String) : HelpEvent
    data class LoadTopic(val topicId: String) : HelpEvent
    data class LoadGuide(val guideId: String) : HelpEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HelpViewModel @Inject constructor(
    private val useCases: HelpUseCases,
    preferences: PreferencesDataStore
) : ViewModel() {

    private val language: StateFlow<String> = preferences.appLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    private val query = MutableStateFlow("")

    private val _homeState = MutableStateFlow(HelpHomeUiState())
    val homeState: StateFlow<HelpHomeUiState> = _homeState.asStateFlow()

    private val _topicState = MutableStateFlow(HelpTopicUiState())
    val topicState: StateFlow<HelpTopicUiState> = _topicState.asStateFlow()

    private val _guideState = MutableStateFlow(HelpGuideUiState())
    val guideState: StateFlow<HelpGuideUiState> = _guideState.asStateFlow()

    init {
        // Topics re-resolve when the app language changes.
        viewModelScope.launch {
            language.flatMapLatest { lang -> useCases.getTopics(lang) }
                .catch { e -> _homeState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { topics -> _homeState.update { it.copy(topics = topics, isLoading = false) } }
        }
        // Search results, debounced.
        viewModelScope.launch {
            combine(query.debounce(200), language) { q, lang -> q to lang }
                .flatMapLatest { (q, lang) ->
                    if (q.isBlank()) flowOf(emptyList()) else useCases.search(q, lang)
                }
                .catch { /* keep last results on error */ }
                .collect { results ->
                    _homeState.update { it.copy(results = results, isSearching = query.value.isNotBlank()) }
                }
        }
    }

    fun onEvent(event: HelpEvent) {
        when (event) {
            is HelpEvent.Search -> {
                query.value = event.query
                _homeState.update { it.copy(query = event.query) }
            }
            is HelpEvent.LoadTopic -> loadTopic(event.topicId)
            is HelpEvent.LoadGuide -> loadGuide(event.guideId)
        }
    }

    private fun loadTopic(topicId: String) {
        _topicState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            language.flatMapLatest { lang -> useCases.getTopicDetail(topicId, lang) }
                .catch { e -> _topicState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { detail -> _topicState.update { it.copy(detail = detail, isLoading = false) } }
        }
    }

    private fun loadGuide(guideId: String) {
        _guideState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            language.flatMapLatest { lang -> useCases.getGuide(guideId, lang) }
                .catch { e -> _guideState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { guide -> _guideState.update { it.copy(guide = guide, isLoading = false) } }
        }
    }
}
