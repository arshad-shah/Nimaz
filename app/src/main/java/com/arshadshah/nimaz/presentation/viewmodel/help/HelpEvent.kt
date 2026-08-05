package com.arshadshah.nimaz.presentation.viewmodel.help

sealed interface HelpEvent {
    data class Search(val query: String) : HelpEvent
    data class LoadTopic(val topicId: String) : HelpEvent
    data class LoadGuide(val guideId: String) : HelpEvent

    /** Re-runs whichever of the three Help surfaces is currently failing. */
    data object Retry : HelpEvent
}
