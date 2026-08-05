package com.arshadshah.nimaz.presentation.viewmodel.ai

sealed interface AskEvent {
    data class UpdateQuestion(val question: String) : AskEvent
    data object Submit : AskEvent
    data object Clear : AskEvent
    data object DismissHint : AskEvent
    data class SelectRecent(val question: String) : AskEvent
}
