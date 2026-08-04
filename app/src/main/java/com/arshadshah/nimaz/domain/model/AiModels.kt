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
 * A hadith reference as cited by the model — `collection:number`, where
 * `collection` is the lowercase slug of one of the six canonical collections
 * the app ships (bukhari, muslim, abudawud, tirmidhi, nasai, ibnmajah) and
 * `number` is the hadith's standard reference number in that collection. The
 * combined form matches the `reference` value stored on local hadith records,
 * so a ref resolves with a single local lookup; anything that doesn't resolve
 * is dropped silently, exactly like an unresolvable Quran ref.
 */
data class HadithRef(val collection: String, val number: Int) {
    /** The exact `reference` value stored on local hadith records. */
    val reference: String get() = "$collection:$number"

    companion object {
        private val FORMAT = Regex("^([a-z]+):([1-9]\\d{0,4})$")

        /** Strict like [CitationId.parse]: malformed refs return null. */
        fun parse(raw: String): HadithRef? {
            val match = FORMAT.matchEntire(raw.trim().lowercase()) ?: return null
            val number = match.groupValues[2].toIntOrNull() ?: return null
            return HadithRef(match.groupValues[1], number)
        }
    }
}

/**
 * The result of the single `search-assist` Worker call: the model's answer to
 * the question, the Quran and Hadith references it cited in support, and
 * search terms for the app's LOCAL library. Nothing in here is shown raw —
 * [quranRefs] and [hadithRefs] are resolved against the local database
 * (unresolvable refs are dropped, so only real verses and hadiths ever surface
 * as proof), and [terms] drive the local keyword search that builds the
 * related-results list.
 */
data class SearchAssist(
    val answer: String,
    val quranRefs: List<CitationId.Quran>,
    val hadithRefs: List<HadithRef>,
    val terms: List<String>,
    val confidence: AnswerConfidence,
)

/**
 * A citation resolved back to a real local record, with a type-safe deep-link
 * [route] the UI can navigate to. Each variant carries the same structured
 * fields the local keyword search returns for that source, so the UI renders a
 * cited proof and a keyword result with identical content — same title,
 * subtitle and English [displayText] — differing only by the "Cited" marker.
 */
sealed interface Proof {
    val citationId: String
    val source: ProofSource

    /** English text for the snippet — verse translation / hadith English text. */
    val displayText: String
    val route: Route

    data class Quran(
        override val citationId: String,
        val surahNumber: Int,
        val ayahNumber: Int,
        /** English surah name, e.g. "Al-Furqan" — the keyword result's subtitle. */
        val surahName: String,
        override val displayText: String,
        override val route: Route,
    ) : Proof {
        override val source: ProofSource get() = ProofSource.QURAN
    }

    data class Hadith(
        override val citationId: String,
        /** The number keyword results title with (`hadith_result_format`). */
        val hadithNumber: Int,
        /** English collection name, e.g. "Sahih al-Bukhari" — the subtitle. */
        val bookName: String,
        override val displayText: String,
        override val route: Route,
    ) : Proof {
        override val source: ProofSource get() = ProofSource.HADITH
    }
}

/**
 * Errors surfaced by the AI feature, mapped from the Worker's error envelope
 * (and local/transport failures). The presentation layer renders friendly
 * messages from these. Play Integrity is the Worker's only guard: an explicit
 * failed verdict surfaces as [Unverified]; rate limits come from the AI
 * Gateway and surface as [RateLimited].
 */
sealed interface AiError {
    data class RateLimited(val retryAfterSeconds: Long?) : AiError
    data object BudgetExceeded : AiError
    data object Network : AiError
    data class Invalid(val message: String) : AiError
    data object Unverified : AiError

    /**
     * The question was not sent because the user has not opted in.
     *
     * Consent used to be checked in one place — a visibility condition in `SearchScreen` —
     * so nothing below the UI enforced it. `AskWithProofUseCase` now refuses first, and
     * this is what it refuses with: a state the UI can explain rather than a generic
     * failure the user would read as the feature being broken.
     */
    data object ConsentRequired : AiError

    data object Unknown : AiError
}
