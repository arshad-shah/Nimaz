package com.arshadshah.nimaz.presentation.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.arshadshah.nimaz.presentation.viewmodel.ai.AskViewModel

data class SearchSettingsUiState(
    val aiEnabled: Boolean = false,
    val historyEnabled: Boolean = false,
    /** Persisted recent questions — shown in the clear-history confirm dialog. */
    val savedQuestions: List<String> = emptyList(),
    val showConsentSheet: Boolean = false,
    /**
     * The consent write failed. The sheet stays up and says so, because the alternative —
     * what shipped — is a switch the user just turned on quietly turning itself back off.
     */
    val consentFailed: Boolean = false,
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
    private val telemetry: Telemetry,
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
                _uiState.update { it.copy(showConsentSheet = false, consentFailed = false) }

            is SearchSettingsEvent.SetHistoryEnabled ->
                launchSafely(telemetry, DOMAIN, "set_history") {
                    settingsRepository.setAiHistoryEnabled(event.enabled)
                    if (!event.enabled) settingsRepository.setAiQuestionHistory("")
                    telemetry.settingChanged(
                        "ai_ask_history",
                        if (event.enabled) "on" else "off",
                    )
                }

            SearchSettingsEvent.ClearHistory ->
                launchSafely(telemetry, DOMAIN, "clear_history") {
                    settingsRepository.setAiQuestionHistory("")
                    telemetry.featureUsed(DOMAIN, "history_cleared")
                }
        }
    }

    private fun onToggleRequested() {
        if (_uiState.value.aiEnabled) {
            // Turning OFF is instant — no consent required.
            launchSafely(telemetry, DOMAIN, "disable") {
                settingsRepository.setAiAskEnabled(false)
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
            settingsRepository.setAiAskEnabled(true)
            settingsRepository.setAiConsentTimestamp(System.currentTimeMillis())
            telemetry.settingChanged("ai_ask", "on")
            _uiState.update { it.copy(showConsentSheet = false, consentFailed = false) }
        }
    }

    private companion object {
        private const val DOMAIN = AppAnalytics.Feature.AI_ASK
    }
}
