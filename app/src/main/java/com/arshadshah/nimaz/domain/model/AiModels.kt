package com.arshadshah.nimaz.domain.model

import com.arshadshah.nimaz.core.navigation.Route

/**
 * The kind of Islamic source a proof passage comes from. Maps 1:1 to the
 * Worker's `source` field ("quran" | "hadith" | "dua").
 */
enum class ProofSource(val wire: String) {
    QURAN("quran"),
    HADITH("hadith"),
    DUA("dua");

    companion object {
        fun fromWire(value: String): ProofSource? =
            entries.firstOrNull { it.wire == value }
    }
}

/**
 * A single grounding passage sent to the AI Worker as evidence. Retrieved
 * locally from Room; the model may only answer from these.
 *
 * @param id citation ID (see [CitationId]) — e.g. `quran:2:255`, `hadith:{id}`.
 * @param text passage text (already truncated to the Worker's per-passage cap).
 * @param meta human-readable reference line, e.g. "Surah Al-Baqarah 255".
 */
data class ProofPassage(
    val id: String,
    val source: ProofSource,
    val text: String,
    val meta: String,
)

enum class AnswerConfidence { HIGH, MEDIUM, LOW }

/**
 * The raw grounded answer returned by the Worker, before local citation
 * resolution into [Proof] items.
 */
data class GroundedAnswer(
    val answer: String,
    val citationIds: List<String>,
    val confidence: AnswerConfidence,
    val insufficientEvidence: Boolean,
)

/**
 * A citation resolved back to a real local record, with a type-safe deep-link
 * [route] the UI can navigate to.
 */
data class Proof(
    val citationId: String,
    val source: ProofSource,
    val displayText: String,
    val meta: String,
    val route: Route,
)

/**
 * Errors surfaced by the AI feature, mapped from the Worker's error envelope
 * (and local/transport failures). The presentation layer renders friendly
 * messages from these.
 */
sealed interface AiError {
    data class RateLimited(val retryAfterSeconds: Long?) : AiError
    data object BudgetExceeded : AiError
    data object Attestation : AiError
    data object Network : AiError
    data class Invalid(val message: String) : AiError
    data object Unknown : AiError
}
