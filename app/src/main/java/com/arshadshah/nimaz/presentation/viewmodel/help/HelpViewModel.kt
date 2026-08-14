package com.arshadshah.nimaz.presentation.viewmodel.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.repository.settings.AppSettings
import com.arshadshah.nimaz.domain.usecase.HelpUseCases
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HelpViewModel @Inject constructor(
    private val useCases: HelpUseCases,
    appSettings: AppSettings,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val language: StateFlow<String> = appSettings.appLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    private val query = MutableStateFlow("")

    private val _homeState = MutableStateFlow(HelpHomeUiState())
    val homeState: StateFlow<HelpHomeUiState> = _homeState.asStateFlow()

    private val _topicState = MutableStateFlow(HelpTopicUiState())
    val topicState: StateFlow<HelpTopicUiState> = _topicState.asStateFlow()

    private val _guideState = MutableStateFlow(HelpGuideUiState())
    val guideState: StateFlow<HelpGuideUiState> = _guideState.asStateFlow()

    // Both loaders collect `language.flatMapLatest { … }`. `language` is a StateFlow, so the
    // collector never completes on its own however the inner repository flow behaves — and
    // neither was cancelled. Every help topic opened left a live collector on `_topicState`,
    // so a re-emission for an earlier topic replaced the one being read.
    // (AP-7.1b in docs/CLEAN_ARCHITECTURE_CHECKLIST.md.)
    private var topicJob: Job? = null
    private var guideJob: Job? = null

    /** Bumped by [HelpEvent.Retry] to re-run the topics load; see the init block. */
    private val retryTick = MutableStateFlow(0)

    /** What a [HelpEvent.Retry] should re-issue, per surface. */
    private var lastTopicId: String? = null
    private var lastGuideId: String? = null

    init {
        // Topics re-resolve when the app language changes — or when a retry is asked for.
        // The tick is what makes retry possible at all: `language` is a StateFlow of a
        // preference, so there is nothing to re-emit when the language has not changed.
        launchSafely(telemetry, DOMAIN, "launch") {
            combine(language, retryTick) { lang, _ -> lang }
                .flatMapLatest { lang ->
                    // Guarded INSIDE flatMapLatest. Flow.catch completes the flow it is
                    // applied to, so catching outside ended the whole chain on the first
                    // transient failure and Help stayed empty for the ViewModel's life.
                    // Here only the inner attempt ends; the language collector survives,
                    // so a language change or a retry recovers.
                    useCases.getTopics(lang)
                        .catchAndReport(telemetry, DOMAIN, "load_topics") { throwable ->
                            _homeState.update {
                                it.copy(
                                    isLoading = false,
                                    error = UiError(
                                        message = R.string.help_topics_load_failed,
                                        details = throwable.message,
                                    ),
                                )
                            }
                        }
                }
                .collect { topics ->
                    _homeState.update {
                        it.copy(topics = topics, isLoading = false, error = null)
                    }
                }
        }
        // Search results, debounced.
        launchSafely(telemetry, DOMAIN, "launch") {
            combine(query.debounce(SEARCH_DEBOUNCE_MS), language) { q, lang -> q to lang }
                .flatMapLatest { (q, lang) ->
                    if (q.isBlank()) {
                        flowOf(q to emptyList())
                    } else {
                        // Logged here, post-debounce: HelpScreen wires Search to
                        // onQueryChange, so logging in onEvent counted keystrokes.
                        // Length only — the query itself never reaches analytics.
                        telemetry.search(DOMAIN, q.trim().length)
                        useCases.search(q, lang)
                            .catchAndReport(telemetry, DOMAIN, "search") { emit(emptyList()) }
                            .map { results -> q to results }
                    }
                }
                .collect { (searchedQuery, results) ->
                    // isSearching is derived from the query these results are FOR, not from
                    // query.value at emission time — those are different flows and could
                    // disagree, rendering results under an already-cleared search box.
                    _homeState.update {
                        it.copy(results = results, isSearching = searchedQuery.isNotBlank())
                    }
                }
        }
    }

    fun onEvent(event: HelpEvent) {
        when (event) {
            is HelpEvent.Search -> {
                query.value = event.query
                _homeState.update { it.copy(query = event.query) }
            }

            is HelpEvent.LoadTopic -> {
                telemetry.featureUsed(DOMAIN, "open_topic")
                loadTopic(event.topicId)
            }

            is HelpEvent.LoadGuide -> {
                telemetry.featureUsed(DOMAIN, "open_guide")
                loadGuide(event.guideId)
            }

            HelpEvent.Retry -> retryFailedLoads()
        }
    }

    /** Re-runs only the surfaces currently failing, so a retry in a guide leaves Help alone. */
    private fun retryFailedLoads() {
        if (_homeState.value.error != null) {
            _homeState.update { it.copy(isLoading = true, error = null) }
            retryTick.value += 1
        }
        if (_topicState.value.error != null) lastTopicId?.let(::loadTopic)
        if (_guideState.value.error != null) lastGuideId?.let(::loadGuide)
    }

    private companion object {
        const val DOMAIN = "help"
        const val SEARCH_DEBOUNCE_MS = 200L
    }

    private fun loadTopic(topicId: String) {
        lastTopicId = topicId
        // Clears the previous attempt's error too: without it a retry showed the stale
        // failure under a fresh spinner.
        _topicState.update { it.copy(isLoading = true, error = null) }
        topicJob?.cancel()
        topicJob = launchSafely(telemetry, DOMAIN, "load_topic") {
            language.flatMapLatest { lang -> useCases.getTopicDetail(topicId, lang) }
                .catchAndReport(telemetry, DOMAIN, "load_topic") { throwable ->
                    _topicState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError(
                                message = R.string.help_topic_load_failed,
                                details = throwable.message,
                            ),
                        )
                    }
                }
                .collect { detail ->
                    _topicState.update {
                        it.copy(
                            detail = detail,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadGuide(guideId: String) {
        lastGuideId = guideId
        _guideState.update { it.copy(isLoading = true, error = null) }
        guideJob?.cancel()
        guideJob = launchSafely(telemetry, DOMAIN, "load_guide") {
            language.flatMapLatest { lang -> useCases.getGuide(guideId, lang) }
                .catchAndReport(telemetry, DOMAIN, "load_guide") { throwable ->
                    _guideState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError(
                                message = R.string.help_guide_load_failed,
                                details = throwable.message,
                            ),
                        )
                    }
                }
                .collect { guide ->
                    _guideState.update {
                        it.copy(
                            guide = guide,
                            isLoading = false
                        )
                    }
                }
        }
    }
}
