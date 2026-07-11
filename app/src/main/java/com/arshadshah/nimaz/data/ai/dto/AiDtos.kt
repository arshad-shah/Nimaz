package com.arshadshah.nimaz.data.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Envelope for `POST /v1/invoke`. Mirrors the Worker's InvokeEnvelopeSchema. */
@Serializable
data class InvokeRequest(
    val capability: String,
    val integrityToken: String,
    val deviceId: String,
    val input: AskInput,
)

@Serializable
data class AskInput(
    val question: String,
    val passages: List<PassageDto>,
)

@Serializable
data class PassageDto(
    val id: String,
    val source: String,
    val text: String,
    val meta: String,
)

/** Success body for ask-with-proof. Mirrors the Worker's AskOutputSchema. */
@Serializable
data class AskOutput(
    val answer: String,
    val citationIds: List<String>,
    val confidence: String,
    val insufficientEvidence: Boolean,
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
