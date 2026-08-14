package com.arshadshah.nimaz.presentation.viewmodel.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.repository.settings.AiSettings
import com.arshadshah.nimaz.domain.usecase.ai.AskWithProofUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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

@HiltViewModel
class AskViewModel @Inject constructor(
    private val askWithProof: AskWithProofUseCase,
    private val aiSettings: AiSettings,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(AskUiState())
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()

    init {
        // A pure transform: the previous version mutated a shared `mutableListOf` and
        // called `_uiState.update` from inside `combine`, which is a side effect in a
        // place that reads as a mapping — and that list was also mutated from `addRecent`
        // on a different coroutine, with no synchronisation.
        combine(
            aiSettings.aiAskEnabled,
            aiSettings.aiAskHintDismissed,
            aiSettings.aiHistoryEnabled,
            aiSettings.aiQuestionHistory,
        ) { enabled, hintDismissed, historyEnabled, historyJson ->
            // The stored history is the single source of truth, re-read on every change
            // rather than loaded once "if the in-memory list happens to be empty". That
            // condition is why switching "remember my questions" off left the questions
            // it had already loaded on screen for the rest of the session.
            Snapshot(
                aiEnabled = enabled,
                hintDismissed = hintDismissed,
                recentQuestions = if (historyEnabled) decodeHistory(historyJson) else emptyList(),
            )
        }
            .onEach { snapshot ->
                _uiState.update {
                    it.copy(
                        aiEnabled = snapshot.aiEnabled,
                        hintDismissed = snapshot.hintDismissed,
                        recentQuestions = snapshot.recentQuestions,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private data class Snapshot(
        val aiEnabled: Boolean,
        val hintDismissed: Boolean,
        val recentQuestions: List<String>,
    )

    fun onEvent(event: AskEvent) {
        when (event) {
            is AskEvent.UpdateQuestion ->
                _uiState.update { it.copy(question = event.question) }

            AskEvent.Submit -> submit(_uiState.value.question)
            is AskEvent.SelectRecent -> {
                // Distinguished from a typed submit: re-asking a remembered question is the
                // signal that the history list earns its place, and it was indistinguishable
                // from any other ask.
                telemetry.featureUsed(DOMAIN, "select_recent")
                _uiState.update { it.copy(question = event.question) }
                submit(event.question)
            }

            AskEvent.Clear -> {
                telemetry.featureUsed(DOMAIN, "clear")
                _uiState.update {
                    it.copy(question = "", phase = AskPhase.Idle, relatedTerms = emptyList())
                }
            }

            AskEvent.DismissHint ->
                launchSafely(telemetry, DOMAIN, "dismiss_hint") {
                    aiSettings.setAiAskHintDismissed(true)
                }
        }
    }

    private fun submit(rawQuestion: String) {
        val question = rawQuestion.trim()
        if (question.length < MIN_QUESTION_LENGTH) return

        // Every submit is one billed Worker invocation. Two guards, both checked
        // synchronously before anything is launched:
        //
        //  - In flight already. Tapping "Ask" twice is the normal reaction to a slow
        //    network, and the error card's retry button makes it one tap away. Without
        //    this, that is two Worker calls for one question.
        //  - Feature off. The use case refuses too — that is where the guarantee lives —
        //    but returning here keeps the UI out of a Loading phase it would never leave
        //    on its own.
        if (_uiState.value.phase == AskPhase.Loading) return
        if (!_uiState.value.aiEnabled) return

        telemetry.featureUsed(DOMAIN, "submitted") // action only — never the question text
        val startedAt = System.currentTimeMillis()
        // Reset the terms so a stale set can't drive the list if this ask fails;
        // only a fresh answer sets relatedTerms again.
        _uiState.update { it.copy(phase = AskPhase.Loading, relatedTerms = emptyList()) }

        launchSafely(
            telemetry,
            DOMAIN,
            "ask",
            onFailure = { _uiState.update { it.copy(phase = AskPhase.Error(AiError.Unknown)) } },
        ) {
            when (val outcome = askWithProof(question)) {
                is AskWithProofUseCase.Outcome.Answered -> {
                    // The proof count is the silent-degradation signal for the whole feature:
                    // an answer whose citations resolve to nothing still renders, and reads
                    // as a working answer. Zero proofs trending upward is the only way that
                    // shows. The duration is the cost signal `docs/ai-ask-with-proof.md`
                    // asks for — one billed Worker call per question.
                    telemetry.aiAnswered(
                        proofCount = outcome.proofs.size,
                        durationMs = System.currentTimeMillis() - startedAt,
                        confidence = outcome.confidence.name,
                    )
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
                    telemetry.error(DOMAIN, "ask_" + errorSlug(outcome.error))
                    _uiState.update { it.copy(phase = AskPhase.Error(outcome.error)) }
                }
            }
        }
    }

    private suspend fun addRecent(question: String) {
        val updated =
            (listOf(question) + _uiState.value.recentQuestions.filterNot { it == question })
                .take(MAX_RECENT)
        _uiState.update { it.copy(recentQuestions = updated) }
        // Persisting re-emits `aiQuestionHistory`, which flows back through the combine
        // above — so the stored list stays the source of truth and the two cannot drift.
        if (aiSettings.aiHistoryEnabled.first()) {
            aiSettings.setAiQuestionHistory(encodeHistory(updated))
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
        AiError.Unverified -> "unverified"
        AiError.ConsentRequired -> "consent_required"
        AiError.Unknown -> "unknown"
    }

    companion object {
        const val MIN_QUESTION_LENGTH = 3
        private const val MAX_RECENT = 10
        private const val DOMAIN = AppAnalytics.Feature.AI_ASK
    }
}
