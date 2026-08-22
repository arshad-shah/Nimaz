package com.arshadshah.nimaz.presentation.viewmodel.ai

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
