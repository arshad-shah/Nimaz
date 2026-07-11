package com.arshadshah.nimaz.data.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Envelope for `POST /v1/invoke`. Mirrors the Worker's InvokeEnvelopeSchema.
 * [input] is a raw [JsonElement] so one envelope serves every capability — the
 * repository encodes the capability-specific input into it.
 */
@Serializable
data class InvokeRequest(
    val capability: String,
    val integrityToken: String,
    val deviceId: String,
    val input: JsonElement,
)

/** Input for the `search-assist` capability. Mirrors the Worker's SearchAssistInputSchema. */
@Serializable
data class AssistInput(
    val question: String,
)

/** Success body for `search-assist`. Mirrors the Worker's SearchAssistOutputSchema. */
@Serializable
data class AssistOutput(
    val answer: String,
    val quranRefs: List<String>,
    val terms: List<String>,
    val confidence: String,
)

/** Error envelope: `{ "error": { code, message, retryAfterSeconds? } }`. */
@Serializable
data class ApiError(
    val error: ApiErrorBody,
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String = "",
    @SerialName("retryAfterSeconds") val retryAfterSeconds: Long? = null,
)
