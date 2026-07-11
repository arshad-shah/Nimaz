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
import javax.inject.Inject

data class SearchSettingsUiState(
    val aiEnabled: Boolean = false,
    val sourcesQuran: Boolean = true,
    val sourcesHadith: Boolean = true,
    val sourcesDua: Boolean = true,
    val maxProofs: Int = 5,
    val historyEnabled: Boolean = false,
    val showConsentSheet: Boolean = false,
)

sealed interface SearchSettingsEvent {
    /** Master toggle tapped. Enabling first opens the consent sheet. */
    data object ToggleAiRequested : SearchSettingsEvent
    data object ConsentAccepted : SearchSettingsEvent
    data object ConsentDismissed : SearchSettingsEvent
    data class SetSourceQuran(val enabled: Boolean) : SearchSettingsEvent
    data class SetSourceHadith(val enabled: Boolean) : SearchSettingsEvent
    data class SetSourceDua(val enabled: Boolean) : SearchSettingsEvent
    data class SetMaxProofs(val count: Int) : SearchSettingsEvent
    data class SetHistoryEnabled(val enabled: Boolean) : SearchSettingsEvent
    data object ClearHistory : SearchSettingsEvent
}

@HiltViewModel
class SearchSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchSettingsUiState())
    val uiState: StateFlow<SearchSettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            settingsRepository.aiAskEnabled,
            settingsRepository.aiSourcesQuran,
            settingsRepository.aiSourcesHadith,
            settingsRepository.aiSourcesDua,
        ) { enabled, quran, hadith, dua ->
            _uiState.update {
                it.copy(
                    aiEnabled = enabled,
                    sourcesQuran = quran,
                    sourcesHadith = hadith,
                    sourcesDua = dua,
                )
            }
        }.launchIn(viewModelScope)

        combine(
            settingsRepository.aiMaxProofs,
            settingsRepository.aiHistoryEnabled,
        ) { maxProofs, history ->
            _uiState.update { it.copy(maxProofs = maxProofs, historyEnabled = history) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: SearchSettingsEvent) {
        when (event) {
            SearchSettingsEvent.ToggleAiRequested -> onToggleRequested()
            SearchSettingsEvent.ConsentAccepted -> onConsentAccepted()
            SearchSettingsEvent.ConsentDismissed ->
                _uiState.update { it.copy(showConsentSheet = false) }

            is SearchSettingsEvent.SetSourceQuran ->
                viewModelScope.launch { settingsRepository.setAiSourcesQuran(event.enabled) }

            is SearchSettingsEvent.SetSourceHadith ->
                viewModelScope.launch { settingsRepository.setAiSourcesHadith(event.enabled) }

            is SearchSettingsEvent.SetSourceDua ->
                viewModelScope.launch { settingsRepository.setAiSourcesDua(event.enabled) }

            is SearchSettingsEvent.SetMaxProofs ->
                viewModelScope.launch {
                    settingsRepository.setAiMaxProofs(event.count.coerceIn(3, 8))
                }

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
