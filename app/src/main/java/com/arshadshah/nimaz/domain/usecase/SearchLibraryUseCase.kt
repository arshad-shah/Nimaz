package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.model.LibrarySearchResults
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.usecase.SearchLibraryUseCase.Companion.PHRASE_SCORE
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Smart local search over the whole content library (Quran ayat + surah names,
 * Hadith, Duas).
 *
 * The ranking here is unchanged by the search index (#330) and is what decides its
 * query shape rather than the other way round: because a whole-phrase hit scores
 * [PHRASE_SCORE] and each word scores 1, the two passes have to ask *different*
 * questions of the index, so `ArabicSearchNormaliser.matchExpression` sends a
 * multi-word query as an FTS phrase and a single word as a prefix term. OR-ing the
 * phrase pass would hand every single-word hit the phrase score.
 *
 * What did change is what the repositories do underneath. They used to match a single
 * contiguous substring (`LIKE '%q%'`) per term per source — twelve full scans over 36 MB
 * of hadith and 21 MB of translations for a three-word query, and **zero rows for any
 * Arabic query ever**, because the corpus is vocalised. Where the artifact carries the
 * folded index they are index lookups instead; where it does not (an install older than
 * the index) they are the same scans as before.
 *
 * The ranking itself:
 *
 *  - The full phrase is always searched (exact-substring hits rank highest).
 *  - Multi-word queries are additionally tokenized into significant words and
 *    each word is searched on its own; records are ranked by how many words
 *    they match (a phrase hit counts extra), so a verse matching both
 *    "patience" and "hardship" outranks one matching only "patience".
 *  - [byTerms] runs the same union+rank over an explicit term list — used to
 *    build the results list from the AI's related search terms.
 *
 * Results are deduped per source, ordered by score, and capped so the UI stays
 * responsive.
 */
class SearchLibraryUseCase @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val hadithUseCases: HadithUseCases,
    private val duaUseCases: DuaUseCases,
) {
    suspend operator fun invoke(
        query: String,
        translatorId: String = DEFAULT_TRANSLATOR,
    ): LibrarySearchResults {
        val phrase = query.trim()
        if (phrase.isBlank()) return LibrarySearchResults.EMPTY
        val tokens = tokenize(phrase)
        // Single-word (or all-stopword) queries stay a plain phrase search;
        // multi-word queries add one search per significant word.
        val wordQueries = if (tokens.size >= 2) tokens else emptyList()
        return search(phrase, wordQueries, translatorId)
    }

    /** Search by an explicit term list (e.g. the AI's related terms). */
    suspend fun byTerms(
        terms: List<String>,
        translatorId: String = DEFAULT_TRANSLATOR,
    ): LibrarySearchResults {
        val cleaned = terms.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleaned.isEmpty()) return LibrarySearchResults.EMPTY
        return search(phrase = null, wordQueries = cleaned, translatorId = translatorId)
    }

    /**
     * Run the phrase (scored [PHRASE_SCORE]) and each word query (scored 1)
     * against every source, then merge per-record scores and rank.
     */
    private suspend fun search(
        phrase: String?,
        wordQueries: List<String>,
        translatorId: String,
    ): LibrarySearchResults {
        val quran = Ranked<Int, QuranSearchResult>()
        val surahs = Ranked<Int, Surah>()
        val hadith = Ranked<String, HadithSearchResult>()
        val duas = Ranked<String, DuaSearchResult>()

        val passes = buildList {
            if (phrase != null) add(phrase to PHRASE_SCORE)
            addAll(wordQueries.take(MAX_WORD_QUERIES).map { it to 1 })
        }

        for ((term, score) in passes) {
            quranUseCases.searchQuran(term, translatorId).first()
                .forEach { quran.add(it.ayah.id, it, score) }
            quranUseCases.getSurahList.search(term).first()
                .forEach { surahs.add(it.number, it, score) }
            hadithUseCases.searchHadiths(term).first()
                .forEach { hadith.add(it.hadith.id, it, score) }
            duaUseCases.searchDuas(term).first()
                .forEach { duas.add(it.dua.id, it, score) }
        }

        return LibrarySearchResults(
            quran = quran.top(MAX_PER_SOURCE),
            surahs = surahs.top(MAX_PER_SOURCE),
            hadith = hadith.top(MAX_PER_SOURCE),
            duas = duas.top(MAX_PER_SOURCE),
        )
    }

    /** Accumulates per-record scores while keeping first-seen insertion order. */
    private class Ranked<K, V> {
        private val entries = LinkedHashMap<K, Entry<V>>()

        private class Entry<V>(val value: V, var score: Int)

        fun add(key: K, value: V, score: Int) {
            val existing = entries[key]
            if (existing != null) existing.score += score
            else entries[key] = Entry(value, score)
        }

        fun top(limit: Int): List<V> =
            entries.values
                .sortedByDescending { it.score }
                .take(limit)
                .map { it.value }
    }

    companion object {
        const val DEFAULT_TRANSLATOR = "sahih_international"

        /** A whole-phrase hit outranks any combination of single-word hits. */
        private const val PHRASE_SCORE = 100

        /** Bound the per-source DB work for very long queries/term lists. */
        private const val MAX_WORD_QUERIES = 8
        private const val MAX_PER_SOURCE = 60

        private val NON_WORD = Regex("[^\\p{L}\\p{N}']+")
        private val STOPWORDS = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all", "any", "can",
            "her", "was", "one", "our", "out", "has", "him", "his", "how", "what",
            "when", "where", "which", "who", "why", "with", "does", "did", "about",
            "that", "this", "there", "their", "them", "then", "they", "have", "from",
            "into", "over", "than", "your", "should", "would", "could", "will",
            "say", "says", "said",
        )

        /** Significant lowercase words of a query, longest (most distinctive) first. */
        fun tokenize(query: String): List<String> =
            query.lowercase()
                .split(NON_WORD)
                .filter { it.length > 2 && it !in STOPWORDS }
                .distinct()
                .sortedByDescending { it.length }
    }
}
