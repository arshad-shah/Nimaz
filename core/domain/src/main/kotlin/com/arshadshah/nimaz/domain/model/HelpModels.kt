package com.arshadshah.nimaz.domain.model

data class HelpTopic(
    val id: String,
    val iconKey: String,
    val colorKey: String,
    val title: String,
    val subtitle: String,
    val order: Int,
    val itemCount: Int
)

sealed interface HelpItem {
    val id: String
    val order: Int

    data class HelpQuestion(
        override val id: String,
        override val order: Int,
        val question: String,
        val answer: String
    ) : HelpItem

    data class HelpGuide(
        override val id: String,
        override val order: Int,
        val iconKey: String?,
        val title: String,
        val estimatedMinutes: Int?,
        val stepCount: Int
    ) : HelpItem
}

data class HelpStep(
    val id: String,
    val order: Int,
    val title: String,
    val body: String,
    val deeplinkRoute: String?,
    val pathLabels: List<String>
)

data class HelpTopicDetail(
    val topic: HelpTopic,
    val questions: List<HelpItem.HelpQuestion>,
    val guides: List<HelpItem.HelpGuide>
)

data class HelpGuideDetail(
    val id: String,
    val title: String,
    val estimatedMinutes: Int?,
    val steps: List<HelpStep>
)

data class HelpSearchResult(
    val topicId: String,
    val itemId: String?,   // null when the hit is the topic itself
    val isGuide: Boolean,
    val title: String,
    val snippet: String
)
