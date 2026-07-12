package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class SearchSettingsUiState(
    val aiEnabled: Boolean = false,
    val historyEnabled: Boolean = false,
    /** Persisted recent questions — shown in the clear-history confirm dialog. */
    val savedQuestions: List<String> = emptyList(),
    val showConsentSheet: Boolean = false,
)

sealed interface SearchSettingsEvent {
    /** Master toggle tapped. Enabling first opens the consent sheet. */
    data object ToggleAiRequested : SearchSettingsEvent
    data object ConsentAccepted : SearchSettingsEvent
    data object ConsentDismissed : SearchSettingsEvent
    data class SetHistoryEnabled(val enabled: Boolean) : SearchSettingsEvent
    data object ClearHistory : SearchSettingsEvent
}

@HiltViewModel
class SearchSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(SearchSettingsUiState())
    val uiState: StateFlow<SearchSettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            settingsRepository.aiAskEnabled,
            settingsRepository.aiHistoryEnabled,
            settingsRepository.aiQuestionHistory,
        ) { enabled, history, historyJson ->
            _uiState.update {
                it.copy(
                    aiEnabled = enabled,
                    historyEnabled = history,
                    savedQuestions = decodeHistory(historyJson),
                )
            }
        }.launchIn(viewModelScope)
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
            SearchSettingsEvent.ToggleAiRequested -> onToggleRequested()
            SearchSettingsEvent.ConsentAccepted -> onConsentAccepted()
            SearchSettingsEvent.ConsentDismissed ->
                _uiState.update { it.copy(showConsentSheet = false) }

            is SearchSettingsEvent.SetHistoryEnabled ->
                viewModelScope.launch {
                    settingsRepository.setAiHistoryEnabled(event.enabled)
                    if (!event.enabled) settingsRepository.setAiQuestionHistory("")
                    AppAnalytics.logFeatureUsed(
                        "ai_ask",
                        if (event.enabled) "history_on" else "history_off",
                    )
                }

            SearchSettingsEvent.ClearHistory ->
                viewModelScope.launch {
                    settingsRepository.setAiQuestionHistory("")
                    AppAnalytics.logFeatureUsed("ai_ask", "history_cleared")
                }
        }
    }

    private fun onToggleRequested() {
        if (_uiState.value.aiEnabled) {
            // Turning OFF is instant — no consent required.
            viewModelScope.launch {
                settingsRepository.setAiAskEnabled(false)
                AppAnalytics.logFeatureUsed("ai_ask", "disabled")
            }
        } else {
            // Turning ON requires explicit consent first.
            _uiState.update { it.copy(showConsentSheet = true) }
        }
    }

    private fun onConsentAccepted() {
        viewModelScope.launch {
            settingsRepository.setAiAskEnabled(true)
            settingsRepository.setAiConsentTimestamp(System.currentTimeMillis())
            AppAnalytics.logFeatureUsed("ai_ask", "enabled")
        }
        _uiState.update { it.copy(showConsentSheet = false) }
    }
}
