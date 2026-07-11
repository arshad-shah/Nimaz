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
 * Orchestrates the "Ask with Proof" flow. When AI is enabled the AI drives
 * retrieval end-to-end:
 *  1. Plan — ask the Worker (`search-plan`) what to fetch: keyword terms + the
 *     specific Quran references it judges relevant. Falls back to local keyword
 *     variants if planning fails or returns nothing.
 *  2. Retrieve — fan the plan's terms out over the existing Quran/Hadith/Dua
 *     search use cases and resolve its Quran refs, all against the LOCAL DB;
 *     rank by term overlap and cap the evidence set.
 *  3. Ground — call the Worker (`ask-with-proof`) with the retrieved passages so
 *     the answer is grounded ONLY in real local records.
 *  4. Resolve the returned citation IDs back to local records → type-safe
 *     [Proof] deep links (unresolvable IDs dropped silently).
 *
 * [Outcome.Answered.plannedTerms] carries the terms the retrieval actually used
 * so the caller can drive the on-screen results list from the same plan (no
 * second planning call). If retrieval finds nothing, returns an
 * insufficient-evidence result without the grounding call.
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
        data class Answered(
            val answer: GroundedAnswer,
            val proofs: List<Proof>,
            /** Terms retrieval used — feeds the results list from the same plan. */
            val plannedTerms: List<String> = emptyList(),
        ) : Outcome
        /** Local short-circuit — no matching passages, no grounding call made. */
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

        // 1. Let the AI plan retrieval; fall back to local keyword variants if the
        //    planning call fails or comes back empty (offline, rate-limited, etc.).
        val plan = aiRepository.planSearch(question).getOrNull()
        val terms = plan?.terms?.takeIf { it.isNotEmpty() } ?: queryVariants(question, content)
        val quranRefs = plan?.quranRefs.orEmpty()

        // 2. Retrieve locally using the plan.
        val candidates = retrieve(terms, quranRefs, sources)
        val passages = rankAndCap(candidates, content, cap)

        // No local matches → insufficient evidence, skip the grounding call.
        if (passages.isEmpty()) return Outcome.NoEvidence

        // 3. Ground the answer on the retrieved passages.
        val result = aiRepository.ask(question, passages)
        val answer = result.getOrElse { throwable ->
            val error = (throwable as? AiRequestException)?.error ?: AiError.Unknown
            return Outcome.Failed(error)
        }

        if (answer.insufficientEvidence && answer.citationIds.isEmpty()) {
            return Outcome.Answered(answer, emptyList(), terms)
        }

        val bySentId = passages.associateBy { it.id }
        val proofs = answer.citationIds.mapNotNull { id -> resolve(id, bySentId) }
        return Outcome.Answered(answer, proofs, terms)
    }

    // ── Retrieval ───────────────────────────────────────────────────────────

    private suspend fun retrieve(
        terms: List<String>,
        quranRefs: List<CitationId.Quran>,
        sources: Sources,
    ): List<ProofPassage> {
        val out = LinkedHashMap<String, ProofPassage>() // dedupe by citation id

        // Quran refs the AI flagged directly — resolve to passages up front so
        // they're included even if a keyword term wouldn't have surfaced them.
        if (sources.quran) {
            for (ref in quranRefs) {
                val passage = resolveQuranRefToPassage(ref) ?: continue
                out.putIfAbsent(passage.id, passage)
            }
        }

        if (sources.quran) {
            for (v in terms) {
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
            for (v in terms) {
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
            for (v in terms) {
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

    /** Resolve an AI-flagged Quran reference to a passage from the local DB. */
    private suspend fun resolveQuranRefToPassage(ref: CitationId.Quran): ProofPassage? {
        val ayah = quranUseCases.getAyahsBySurah(ref.surah).first()
            .firstOrNull { it.ayahNumber == ref.ayah } ?: return null
        val surahName = quranUseCases.getSurahByNumber(ref.surah)?.nameEnglish ?: "${ref.surah}"
        val text = ayah.translation?.takeIf { it.isNotBlank() } ?: ayah.textSimple
        return ProofPassage(
            id = ref.raw,
            source = ProofSource.QURAN,
            text = text.truncate(),
            meta = "Surah $surahName ${ref.ayah}",
        )
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

    /**
     * The search terms to fan the local lookups over.
     *
     * The DB search matches a single contiguous substring (`LIKE '%term%'`), so a
     * multi-word phrase like "patience during hardship" almost never matches a
     * translation and retrieval comes back empty — which surfaces as
     * "No supporting sources found" without ever calling the AI. We therefore
     * search each significant word **on its own** (that is what actually retrieves
     * passages), plus the full phrase for the rare exact-substring hit. Ranking
     * downstream re-sorts the union by how many of the question's terms overlap,
     * so single-word noise is outranked by passages matching several terms.
     */
    private fun queryVariants(question: String, content: List<String>): List<String> {
        val variants = LinkedHashSet<String>()
        variants += question.trim()
        // Individual content words, longest (most distinctive) first so the
        // strongest terms are still searched when we cap the count.
        content.sortedByDescending { it.length }
            .take(MAX_WORD_VARIANTS)
            .forEach { variants += it }
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
        /** Cap on individual-word searches per source, to bound DB work. */
        private const val MAX_WORD_VARIANTS = 8
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
