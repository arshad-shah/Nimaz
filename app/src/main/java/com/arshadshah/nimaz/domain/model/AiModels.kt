package com.arshadshah.nimaz.domain.model

import com.arshadshah.nimaz.core.navigation.Route

/** The kind of Islamic source a proof or search result comes from. */
enum class ProofSource {
    QURAN,
    HADITH,
    DUA,
}

enum class AnswerConfidence { HIGH, MEDIUM, LOW }

/**
 * The result of the single `search-assist` Worker call: the model's answer to
 * the question, the Quran references it cited in support, and search terms for
 * the app's LOCAL library. Nothing in here is shown raw — [quranRefs] are
 * resolved against the local Quran database (unresolvable refs are dropped, so
 * only real verses ever surface as proof), and [terms] drive the local
 * keyword search that builds the related-results list.
 */
data class SearchAssist(
    val answer: String,
    val quranRefs: List<CitationId.Quran>,
    val terms: List<String>,
    val confidence: AnswerConfidence,
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
 * messages from these. Attestation is no longer an error — integrity problems
 * only reduce the Worker-side rate limit, they never fail a request.
 */
sealed interface AiError {
    data class RateLimited(val retryAfterSeconds: Long?) : AiError
    data object BudgetExceeded : AiError
    data object Network : AiError
    data class Invalid(val message: String) : AiError
    data object Unknown : AiError
}
