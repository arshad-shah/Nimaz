package com.arshadshah.nimaz.data.local.help

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val helpJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
data class HelpJsonRoot(
    val contentVersion: Int = 1,
    val topics: List<HelpTopicDto> = emptyList()
)

@Serializable
data class HelpTopicDto(
    val id: String,
    val order: Int,
    val icon: String,
    val color: String,
    val title: Map<String, String> = emptyMap(),
    val subtitle: Map<String, String> = emptyMap(),
    val items: List<HelpItemDto> = emptyList()
)

@Serializable
data class HelpItemDto(
    val id: String,
    val type: String,                 // "question" | "guide"
    val order: Int,
    val icon: String? = null,
    val estimatedMinutes: Int? = null,
    val question: Map<String, String> = emptyMap(),
    val answer: Map<String, String> = emptyMap(),
    val title: Map<String, String> = emptyMap(),
    val steps: List<HelpStepDto> = emptyList()
)

@Serializable
data class HelpStepDto(
    val id: String,
    val order: Int,
    val deeplink: String? = null,
    val pathLabels: List<String> = emptyList(),
    val title: Map<String, String> = emptyMap(),
    val body: Map<String, String> = emptyMap()
)
