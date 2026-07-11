package com.arshadshah.nimaz.domain.usecase.ai

import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.CitationId
import com.arshadshah.nimaz.domain.model.GroundedAnswer
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.model.ProofPassage
import com.arshadshah.nimaz.domain.model.ProofSource
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.repository.AiRequestException
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Orchestrates the "Ask with Proof" flow:
 *  1. Local retrieval — fan out the existing Quran/Hadith/Dua search use cases
 *     over the question (plus keyword variants), rank by term overlap, cap the
 *     evidence set.
 *  2. Call the AI Worker via [AiRepository] with the retrieved passages.
 *  3. Resolve the returned citation IDs back to real local records, producing
 *     type-safe [Proof] deep links (unresolvable IDs are dropped silently).
 *
 * If retrieval finds nothing, returns a local insufficient-evidence result
 * WITHOUT any network call.
 */
class AskWithProofUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val quranUseCases: QuranUseCases,
    private val hadithUseCases: HadithUseCases,
    private val duaUseCases: DuaUseCases,
) {
    /** Which sources the user has enabled in Search Settings. */
    data class Sources(
        val quran: Boolean,
        val hadith: Boolean,
        val dua: Boolean,
    )

    sealed interface Outcome {
        data class Answered(val answer: GroundedAnswer, val proofs: List<Proof>) : Outcome
        /** Local short-circuit — no matching passages, no network call made. */
        data object NoEvidence : Outcome
        data class Failed(val error: AiError) : Outcome
    }

    suspend operator fun invoke(
        question: String,
        sources: Sources,
        maxProofs: Int,
    ): Outcome {
        val cap = maxProofs.coerceIn(1, MAX_PASSAGES)
        val content = contentWords(question)

        val candidates = retrieve(question, content, sources)
        val passages = rankAndCap(candidates, content, cap)

        // 4. Offline / no-results short-circuit: never hit the network.
        if (passages.isEmpty()) return Outcome.NoEvidence

        val result = aiRepository.ask(question, passages)
        val answer = result.getOrElse { throwable ->
            val error = (throwable as? AiRequestException)?.error ?: AiError.Unknown
            return Outcome.Failed(error)
        }

        if (answer.insufficientEvidence && answer.citationIds.isEmpty()) {
            return Outcome.Answered(answer, emptyList())
        }

        val bySentId = passages.associateBy { it.id }
        val proofs = answer.citationIds.mapNotNull { id -> resolve(id, bySentId) }
        return Outcome.Answered(answer, proofs)
    }

    // ── Retrieval ───────────────────────────────────────────────────────────

    private suspend fun retrieve(
        question: String,
        content: List<String>,
        sources: Sources,
    ): List<ProofPassage> {
        val variants = queryVariants(question, content)
        val out = LinkedHashMap<String, ProofPassage>() // dedupe by citation id

        if (sources.quran) {
            for (v in variants) {
                quranUseCases.searchQuran(v, QURAN_TRANSLATOR).first().forEach { r ->
                    val text = r.ayah.translation?.takeIf { it.isNotBlank() }
                        ?: r.ayah.textSimple
                    val id = CitationId.Quran(r.ayah.surahNumber, r.ayah.ayahNumber).raw
                    out.putIfAbsent(
                        id,
                        ProofPassage(
                            id = id,
                            source = ProofSource.QURAN,
                            text = text.truncate(),
                            meta = "Surah ${r.surahName} ${r.ayah.ayahNumber}",
                        ),
                    )
                }
            }
        }
        if (sources.hadith) {
            for (v in variants) {
                hadithUseCases.searchHadiths(v).first().forEach { r ->
                    val id = CitationId.Hadith(r.hadith.id).raw
                    out.putIfAbsent(
                        id,
                        ProofPassage(
                            id = id,
                            source = ProofSource.HADITH,
                            text = r.hadith.textEnglish.truncate(),
                            meta = "${r.bookName} #${r.hadith.hadithNumber}",
                        ),
                    )
                }
            }
        }
        if (sources.dua) {
            for (v in variants) {
                duaUseCases.searchDuas(v).first().forEach { r ->
                    val id = CitationId.Dua(r.dua.id).raw
                    out.putIfAbsent(
                        id,
                        ProofPassage(
                            id = id,
                            source = ProofSource.DUA,
                            text = r.dua.textEnglish.truncate(),
                            meta = "${r.categoryName}: ${r.dua.titleEnglish}",
                        ),
                    )
                }
            }
        }
        return out.values.toList()
    }

    /** Rank by term overlap, then greedily add under the 8000-char budget. */
    private fun rankAndCap(
        candidates: List<ProofPassage>,
        content: List<String>,
        cap: Int,
    ): List<ProofPassage> {
        val ranked = candidates.sortedByDescending { score(it.text, content) }
        val selected = mutableListOf<ProofPassage>()
        var totalChars = 0
        for (p in ranked) {
            if (selected.size >= cap) break
            if (totalChars + p.text.length > MAX_TOTAL_CHARS) continue
            selected += p
            totalChars += p.text.length
        }
        return selected
    }

    // ── Citation resolution ───────────────────────────────────────────────────

    private suspend fun resolve(
        rawId: String,
        bySentId: Map<String, ProofPassage>,
    ): Proof? {
        val parsed = CitationId.parse(rawId) ?: return null
        val sent = bySentId[parsed.raw]
        return when (parsed) {
            is CitationId.Quran -> {
                val ayah = quranUseCases.getAyahsBySurah(parsed.surah).first()
                    .firstOrNull { it.ayahNumber == parsed.ayah } ?: return null
                val surahName = quranUseCases.getSurahByNumber(parsed.surah)?.nameEnglish
                Proof(
                    citationId = parsed.raw,
                    source = ProofSource.QURAN,
                    displayText = sent?.text
                        ?: (ayah.translation?.takeIf { it.isNotBlank() } ?: ayah.textSimple),
                    meta = sent?.meta
                        ?: "Surah ${surahName ?: parsed.surah} ${parsed.ayah}",
                    route = Route.QuranReader(parsed.surah, parsed.ayah),
                )
            }

            is CitationId.Hadith -> {
                val hadith = hadithUseCases.getHadithById(parsed.hadithId) ?: return null
                Proof(
                    citationId = parsed.raw,
                    source = ProofSource.HADITH,
                    displayText = sent?.text ?: hadith.textEnglish,
                    meta = sent?.meta ?: (hadith.reference ?: "Hadith #${hadith.hadithNumber}"),
                    route = Route.HadithReader(parsed.hadithId),
                )
            }

            is CitationId.Dua -> {
                val dua = duaUseCases.getDuaById(parsed.duaId) ?: return null
                Proof(
                    citationId = parsed.raw,
                    source = ProofSource.DUA,
                    displayText = sent?.text ?: dua.textEnglish,
                    meta = sent?.meta ?: dua.titleEnglish,
                    route = Route.DuaReader(parsed.duaId),
                )
            }
        }
    }

    // ── Text helpers ──────────────────────────────────────────────────────────

    private fun String.truncate(): String =
        if (length <= MAX_PASSAGE_CHARS) this else take(MAX_PASSAGE_CHARS)

    private fun contentWords(question: String): List<String> =
        question.lowercase()
            .split(NON_WORD)
            .filter { it.length > 2 && it !in STOPWORDS }
            .distinct()

    /** Original question plus up to two stripped keyword variants. */
    private fun queryVariants(question: String, content: List<String>): List<String> {
        val variants = LinkedHashSet<String>()
        variants += question.trim()
        if (content.isNotEmpty()) variants += content.joinToString(" ")
        val top = content.sortedByDescending { it.length }.take(3)
        if (top.isNotEmpty()) variants += top.joinToString(" ")
        return variants.filter { it.isNotBlank() }
    }

    private fun score(text: String, content: List<String>): Int {
        if (content.isEmpty()) return 0
        val lower = text.lowercase()
        return content.count { lower.contains(it) }
    }

    companion object {
        const val MAX_PASSAGES = 8
        const val MAX_PASSAGE_CHARS = 1200
        const val MAX_TOTAL_CHARS = 8000
        private const val QURAN_TRANSLATOR = "sahih_international"
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        private val STOPWORDS = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all", "any", "can",
            "her", "was", "one", "our", "out", "has", "him", "his", "how", "what",
            "when", "where", "which", "who", "why", "with", "does", "did", "about",
            "that", "this", "there", "their", "them", "then", "they", "have", "from",
            "into", "over", "than", "your", "should", "would", "could", "islam",
        )
    }
}
