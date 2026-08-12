package com.arshadshah.nimaz.presentation.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.SearchPreferences
import com.arshadshah.nimaz.domain.repository.settings.AiSettings
import com.arshadshah.nimaz.domain.repository.settings.SearchSettings
import com.arshadshah.nimaz.domain.usecase.ObserveSearchPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.arshadshah.nimaz.presentation.viewmodel.ai.AskViewModel

@HiltViewModel
class SearchSettingsViewModel @Inject constructor(
    private val aiSettings: AiSettings,
    private val searchSettings: SearchSettings,
    searchPreferences: ObserveSearchPreferencesUseCase,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(SearchSettingsUiState())
    val uiState: StateFlow<SearchSettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            aiSettings.aiAskEnabled,
            aiSettings.aiHistoryEnabled,
            aiSettings.aiQuestionHistory,
        ) { enabled, history, historyJson ->
            _uiState.update {
                it.copy(
                    aiEnabled = enabled,
                    historyEnabled = history,
                    savedQuestions = decodeHistory(historyJson),
                )
            }
        }.launchIn(viewModelScope)

        // The same use case search itself reads, so the screen can never show a value search
        // is not using — including the corrections `sanitised` makes to a preferences file
        // written by some other build.
        searchPreferences()
            .onEach { prefs -> _uiState.update { it.copy(search = prefs) } }
            .launchIn(viewModelScope)
    }

    // Same wire format as AskViewModel's encodeHistory: a JSON string list.
    private fun decodeHistory(raw: String): List<String> =
        if (raw.isBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString(ListSerializer(String.serializer()), raw)
            }.getOrDefault(emptyList())
        }

    fun onEvent(event: SearchSettingsEvent) {
        when (event) {
            is SearchSettingsEvent.SetResultsPerSource ->
                launchSafely(telemetry, SEARCH_DOMAIN, "set_results_per_source") {
                    searchSettings.setSearchResultsPerSource(
                        event.count.coerceIn(
                            SearchPreferences.MIN_RESULTS_PER_SOURCE,
                            SearchPreferences.MAX_RESULTS_PER_SOURCE,
                        )
                    )
                    telemetry.settingChanged("search_results_per_source", event.count.toString())
                }

            is SearchSettingsEvent.ToggleSource -> onToggleSource(event.source)

            is SearchSettingsEvent.SetStrictness ->
                launchSafely(telemetry, SEARCH_DOMAIN, "set_strictness") {
                    searchSettings.setSearchStrictness(event.strictness.name)
                    telemetry.settingChanged(
                        "search_strictness",
                        event.strictness.name.lowercase(),
                    )
                }

            is SearchSettingsEvent.SetDefaultScope ->
                launchSafely(telemetry, SEARCH_DOMAIN, "set_default_scope") {
                    searchSettings.setSearchDefaultScope(event.source?.name.orEmpty())
                    telemetry.settingChanged(
                        "search_default_scope",
                        event.source?.name?.lowercase() ?: "all",
                    )
                }

            SearchSettingsEvent.ToggleAiRequested -> onToggleRequested()
            SearchSettingsEvent.ConsentAccepted -> onConsentAccepted()
            SearchSettingsEvent.ConsentDismissed ->
                _uiState.update { it.copy(showConsentSheet = false, consentFailed = false) }

            is SearchSettingsEvent.SetHistoryEnabled ->
                launchSafely(telemetry, DOMAIN, "set_history") {
                    aiSettings.setAiHistoryEnabled(event.enabled)
                    if (!event.enabled) aiSettings.setAiQuestionHistory("")
                    telemetry.settingChanged(
                        "ai_ask_history",
                        if (event.enabled) "on" else "off",
                    )
                }

            SearchSettingsEvent.ClearHistory ->
                launchSafely(telemetry, DOMAIN, "clear_history") {
                    aiSettings.setAiQuestionHistory("")
                    telemetry.featureUsed(DOMAIN, "history_cleared")
                }
        }
    }

    /**
     * Switching a source on or off — except the last one on, which is refused.
     *
     * An empty source set is search that returns nothing for every query. `sanitised` would
     * quietly read it back as "everything", so obeying the tap would show a switch that turns
     * itself back on; the screen disables the last remaining switch instead, and this is the
     * guard behind it.
     *
     * The default scope follows: it is stored as a source name, and a scope pointing at a
     * source that is no longer searched opens the results list already filtered to nothing.
     */
    private fun onToggleSource(source: LibrarySource) {
        val current = _uiState.value.search
        val updated = if (source in current.sources) {
            current.sources - source
        } else {
            current.sources + source
        }
        if (updated.isEmpty()) return

        launchSafely(telemetry, SEARCH_DOMAIN, "toggle_source") {
            searchSettings.setSearchSources(ObserveSearchPreferencesUseCase.encode(updated))
            if (current.defaultScope != null && current.defaultScope !in updated) {
                searchSettings.setSearchDefaultScope("")
            }
            telemetry.settingChanged(
                "search_source_" + source.name.lowercase(),
                if (source in updated) "on" else "off",
            )
        }
    }

    private fun onToggleRequested() {
        if (_uiState.value.aiEnabled) {
            // Turning OFF is instant — no consent required.
            launchSafely(telemetry, DOMAIN, "disable") {
                aiSettings.setAiAskEnabled(false)
                telemetry.settingChanged("ai_ask", "off")
            }
        } else {
            // Turning ON requires explicit consent first.
            _uiState.update { it.copy(showConsentSheet = true, consentFailed = false) }
        }
    }

    /**
     * Closes the sheet **after** the writes commit, not before.
     *
     * The previous order closed it synchronously while the DataStore write was still
     * queued in a `launch`. If that write failed, the user had consented, the sheet was
     * gone, and the `combine` above re-emitted `aiEnabled = false` — a switch that flipped
     * itself back with no explanation, on the one control that governs whether anything
     * leaves the device.
     */
    private fun onConsentAccepted() {
        launchSafely(
            telemetry,
            DOMAIN,
            "consent",
            onFailure = { _uiState.update { it.copy(consentFailed = true) } },
        ) {
            aiSettings.setAiAskEnabled(true)
            aiSettings.setAiConsentTimestamp(System.currentTimeMillis())
            telemetry.settingChanged("ai_ask", "on")
            _uiState.update { it.copy(showConsentSheet = false, consentFailed = false) }
        }
    }

    private companion object {
        private const val DOMAIN = AppAnalytics.Feature.AI_ASK

        /** Local search is not the AI feature — its telemetry does not belong under it. */
        private const val SEARCH_DOMAIN = "global_search"
    }
}
