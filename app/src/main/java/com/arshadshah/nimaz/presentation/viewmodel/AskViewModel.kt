package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.ai.AskWithProofUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed interface AskPhase {
    data object Idle : AskPhase
    data object Loading : AskPhase
    data class Answer(
        val answer: String,
        val confidence: AnswerConfidence,
        val proofs: List<Proof>,
    ) : AskPhase

    data class Error(val error: AiError) : AskPhase
}

data class AskUiState(
    val aiEnabled: Boolean = false,
    val hintDismissed: Boolean = false,
    val question: String = "",
    val recentQuestions: List<String> = emptyList(),
    val phase: AskPhase = AskPhase.Idle,
    /**
     * The AI's related search terms for the most recent answer. The Search
     * screen drives its results list from these (same single call — no extra
     * round-trip), so the list dynamically reflects what the AI judged relevant.
     */
    val relatedTerms: List<String> = emptyList(),
)

sealed interface AskEvent {
    data class UpdateQuestion(val question: String) : AskEvent
    data object Submit : AskEvent
    data object Clear : AskEvent
    data object DismissHint : AskEvent
    data class SelectRecent(val question: String) : AskEvent
}

@HiltViewModel
class AskViewModel @Inject constructor(
    private val askWithProof: AskWithProofUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(AskUiState())
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()

    // Recent AI questions kept in memory for the session; also persisted only
    // when history is enabled (mirrors SearchViewModel's recent-searches idiom).
    private val recentQuestions = mutableListOf<String>()

    init {
        combine(
            settingsRepository.aiAskEnabled,
            settingsRepository.aiAskHintDismissed,
            settingsRepository.aiHistoryEnabled,
            settingsRepository.aiQuestionHistory,
        ) { enabled, hintDismissed, historyEnabled, historyJson ->
            if (historyEnabled && recentQuestions.isEmpty() && historyJson.isNotBlank()) {
                recentQuestions.addAll(decodeHistory(historyJson))
            }
            _uiState.update {
                it.copy(
                    aiEnabled = enabled,
                    hintDismissed = hintDismissed,
                    recentQuestions = recentQuestions.toList(),
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: AskEvent) {
        when (event) {
            is AskEvent.UpdateQuestion ->
                _uiState.update { it.copy(question = event.question) }

            AskEvent.Submit -> submit(_uiState.value.question)
            is AskEvent.SelectRecent -> {
                _uiState.update { it.copy(question = event.question) }
                submit(event.question)
            }

            AskEvent.Clear ->
                _uiState.update {
                    it.copy(question = "", phase = AskPhase.Idle, relatedTerms = emptyList())
                }

            AskEvent.DismissHint ->
                viewModelScope.launch { settingsRepository.setAiAskHintDismissed(true) }
        }
    }

    private fun submit(rawQuestion: String) {
        val question = rawQuestion.trim()
        if (question.length < MIN_QUESTION_LENGTH) return

        AppAnalytics.logEvent(EVENT_SUBMITTED, null) // event name only — never the question text
        // Reset the terms so a stale set can't drive the list if this ask fails;
        // only a fresh answer sets relatedTerms again.
        _uiState.update { it.copy(phase = AskPhase.Loading, relatedTerms = emptyList()) }

        viewModelScope.launch {
            when (val outcome = askWithProof(question)) {
                is AskWithProofUseCase.Outcome.Answered -> {
                    AppAnalytics.logEvent(EVENT_ANSWERED, null)
                    addRecent(question)
                    _uiState.update {
                        it.copy(
                            phase = AskPhase.Answer(
                                answer = outcome.answer,
                                confidence = outcome.confidence,
                                proofs = outcome.proofs,
                            ),
                            relatedTerms = outcome.relatedTerms,
                        )
                    }
                }

                is AskWithProofUseCase.Outcome.Failed -> {
                    AppAnalytics.logEvent(EVENT_ERROR_PREFIX + errorSlug(outcome.error), null)
                    _uiState.update { it.copy(phase = AskPhase.Error(outcome.error)) }
                }
            }
        }
    }

    private fun addRecent(question: String) {
        recentQuestions.remove(question)
        recentQuestions.add(0, question)
        if (recentQuestions.size > MAX_RECENT) {
            recentQuestions.removeAt(recentQuestions.lastIndex)
        }
        _uiState.update { it.copy(recentQuestions = recentQuestions.toList()) }
        viewModelScope.launch {
            if (settingsRepository.aiHistoryEnabled.first()) {
                settingsRepository.setAiQuestionHistory(encodeHistory(recentQuestions))
            }
        }
    }

    private fun encodeHistory(list: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), list)

    private fun decodeHistory(raw: String): List<String> =
        runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())

    private fun errorSlug(error: AiError): String = when (error) {
        is AiError.RateLimited -> "rate_limited"
        AiError.BudgetExceeded -> "budget"
        AiError.Network -> "network"
        is AiError.Invalid -> "invalid"
        AiError.Unknown -> "unknown"
    }

    companion object {
        const val MIN_QUESTION_LENGTH = 3
        private const val MAX_RECENT = 10
        private const val EVENT_SUBMITTED = "ai_ask_submitted"
        private const val EVENT_ANSWERED = "ai_ask_answered"
        private const val EVENT_ERROR_PREFIX = "ai_ask_error_"
    }
}
