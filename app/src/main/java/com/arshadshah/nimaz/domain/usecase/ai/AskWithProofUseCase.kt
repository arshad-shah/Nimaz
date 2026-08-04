package com.arshadshah.nimaz.domain.usecase.ai

import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.CitationId
import com.arshadshah.nimaz.domain.model.HadithRef
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.repository.AiRequestException
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.SearchLibraryUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Orchestrates the "Ask with Proof" flow — one AI call, everything else local:
 *
 *  1. `search-assist` — send ONLY the question to the Worker. The model answers
 *     from mainstream Islamic knowledge and returns the Quran and Hadith
 *     references that support the answer plus search terms for the local
 *     library.
 *  2. Resolve each returned reference against the LOCAL database (Quran refs
 *     against the Quran tables, hadith refs against the hadiths table via
 *     their canonical `collection:number` reference). Every reference that
 *     resolves becomes a [Proof] with the real local text and a type-safe deep
 *     link; anything that doesn't resolve is dropped silently — so the proof
 *     cards can never show a verse or hadith that doesn't exist.
 *  3. Hand the model's [Outcome.Answered.relatedTerms] back to the caller so the
 *     Search screen can drive its results list from them (related Quran/Hadith/
 *     Dua records found in the local DB — no extra AI call).
 *
 * There is no "no evidence" dead end anymore: the answer stands on its own and
 * the UI simply shows however many proofs resolved (possibly none, e.g. for an
 * out-of-scope question).
 */
class AskWithProofUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val quranUseCases: QuranUseCases,
    private val hadithUseCases: HadithUseCases,
    private val settingsRepository: SettingsRepository,
) {
    sealed interface Outcome {
        data class Answered(
            val answer: String,
            val confidence: AnswerConfidence,
            /** AI-cited Quran/Hadith references resolved to real local records. */
            val proofs: List<Proof>,
            /** Search terms for the local library — feeds the results list. */
            val relatedTerms: List<String>,
        ) : Outcome

        data class Failed(val error: AiError) : Outcome
    }

    suspend operator fun invoke(question: String): Outcome {
        // The consent gate lives here, below every caller, rather than in the one screen
        // that happens to hide the entry point today. Nothing sends a question off the
        // device without this check passing first.
        if (!settingsRepository.aiAskEnabled.first()) {
            return Outcome.Failed(AiError.ConsentRequired)
        }

        val assist = aiRepository.assist(question).getOrElse { throwable ->
            val error = (throwable as? AiRequestException)?.error ?: AiError.Unknown
            return Outcome.Failed(error)
        }

        val quranProofs = assist.quranRefs
            .mapNotNull { ref -> resolve(ref) }
            .take(MAX_PROOFS)
        val hadithProofs = assist.hadithRefs
            .mapNotNull { ref -> resolve(ref) }
            .take(MAX_HADITH_PROOFS)

        return Outcome.Answered(
            answer = assist.answer,
            confidence = assist.confidence,
            proofs = quranProofs + hadithProofs,
            relatedTerms = assist.terms,
        )
    }

    /**
     * Resolve an AI-cited Quran reference to the real local record, or null.
     * The lookup joins the same translator the local keyword search uses, so a
     * cited verse shows the same English translation a keyword result would.
     */
    private suspend fun resolve(ref: CitationId.Quran): Proof? {
        val surahWithAyahs = quranUseCases
            .getSurahWithAyahs(ref.surah, SearchLibraryUseCase.DEFAULT_TRANSLATOR)
            .first() ?: return null
        val ayah = surahWithAyahs.ayahs
            .firstOrNull { it.ayahNumber == ref.ayah } ?: return null
        return Proof.Quran(
            citationId = ref.raw,
            surahNumber = ref.surah,
            ayahNumber = ref.ayah,
            surahName = surahWithAyahs.surah.nameEnglish,
            displayText = ayah.translation?.takeIf { it.isNotBlank() } ?: ayah.textSimple,
            route = Route.QuranReader(ref.surah, ref.ayah),
        )
    }

    /** Resolve an AI-cited hadith reference to the real local record, or null. */
    private suspend fun resolve(ref: HadithRef): Proof? {
        val hadith = hadithUseCases.getHadithByReference(ref.reference) ?: return null
        val bookName = hadithUseCases.getBookById(hadith.bookId)?.nameEnglish
        return Proof.Hadith(
            // The proof carries the local hadith id (hadith:{id}) — the same
            // citation key the results list derives for its hadith rows, so a
            // cited hadith dedupes against the related results like a verse.
            citationId = CitationId.Hadith(hadith.id).raw,
            hadithNumber = hadith.hadithNumber,
            bookName = bookName ?: "Hadith",
            displayText = hadith.textEnglish.takeIf { it.isNotBlank() } ?: hadith.textArabic,
            route = Route.HadithReader(hadith.id),
        )
    }

    companion object {
        const val MAX_PROOFS = 8
        const val MAX_HADITH_PROOFS = 6
    }
}
