package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.model.LibrarySearchResults
import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.NameSearchResult
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.search.SearchMatch
import com.arshadshah.nimaz.domain.search.SearchTermExpander
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
 * The ranking itself, in three tiers:
 *
 *  - **The exact phrase** ([PHRASE_SCORE]). Always searched.
 *  - **Every word, any order** ([ALL_WORDS_SCORE]) — multi-word queries only. "patience
 *    hardship" reaches "be patient in times of hardship", which the phrase cannot, and
 *    ranks it above a hadith that only says "patience". Without this tier a record had
 *    to turn up in *each* single-word list to collect both points, and each of those lists
 *    is capped.
 *  - **Each significant word** ([WORD_SCORE] each).
 *
 * Each word is widened to its other endings and synonyms on the way to the index
 * (`SearchTermExpander`), and the index returns each list **best match first** — most
 * occurrences of the rarest words — instead of in book order. That order is kept as the
 * tie-break inside a tier, so two phrase hits are ordered by how well they match rather
 * than by which book came first.
 *
 * [byTerms] runs the same union+rank over an explicit term list — used to build the
 * results list from the AI's related search terms.
 *
 * Results are deduped per source, ordered by score, and capped so the UI stays
 * responsive.
 */
class SearchLibraryUseCase @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val hadithUseCases: HadithUseCases,
    private val duaUseCases: DuaUseCases,
    private val searchNames: SearchNamesUseCase,
    private val searchPreferences: ObserveSearchPreferencesUseCase,
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
        // Read once per search rather than collected: a preference change mid-query would
        // otherwise mean half the passes ran under the old settings and half under the new.
        val prefs = searchPreferences().first()

        val quran = Ranked<Int, QuranSearchResult>()
        val surahs = Ranked<Int, Surah>()
        val hadith = Ranked<String, HadithSearchResult>()
        val duas = Ranked<String, DuaSearchResult>()
        val names = Ranked<String, NameSearchResult>()

        val words = wordQueries.take(prefs.strictness.wordPasses)
        val passes = buildList {
            if (phrase != null) add(Pass(phrase, SearchMatch.AUTO, PHRASE_SCORE))
            // Only for a typed phrase: [byTerms] hands over unrelated keywords, and "every
            // keyword at once" is a question none of them was asked to answer.
            if (phrase != null && words.size >= 2) {
                add(Pass(words.joinToString(" "), SearchMatch.ALL_WORDS, ALL_WORDS_SCORE))
            }
            addAll(words.map { Pass(it, SearchMatch.AUTO, WORD_SCORE) })
        }

        for ((term, match, score) in passes) {
            // A source the user switched off is not queried at all — the cost of a search is
            // one query per source per pass, so this is the setting that makes a long query
            // cheaper as well as the one that makes its results narrower.
            if (LibrarySource.QURAN in prefs.sources) {
                quranUseCases.searchQuran(term, translatorId, match).first()
                    .forEachIndexed { rank, it -> quran.add(it.ayah.id, it, score, rank) }
                quranUseCases.getSurahList.search(term, match).first()
                    .forEachIndexed { rank, it -> surahs.add(it.number, it, score, rank) }
            }
            if (LibrarySource.HADITH in prefs.sources) {
                hadithUseCases.searchHadiths(term, match).first()
                    .forEachIndexed { rank, it -> hadith.add(it.hadith.id, it, score, rank) }
            }
            if (LibrarySource.DUAS in prefs.sources) {
                duaUseCases.searchDuas(term, match).first()
                    .forEachIndexed { rank, it -> duas.add(it.dua.id, it, score, rank) }
            }
            // Names are a small in-memory list matched on substrings; "every word" means
            // nothing to them, and the phrase and word passes already cover them.
            if (LibrarySource.NAMES in prefs.sources && match == SearchMatch.AUTO) {
                searchNames(term)
                    .forEachIndexed { rank, it -> names.add("${it.catalog}:${it.id}", it, score, rank) }
            }
        }

        return LibrarySearchResults(
            quran = quran.top(prefs.resultsPerSource),
            surahs = surahs.top(prefs.resultsPerSource),
            hadith = hadith.top(prefs.resultsPerSource),
            duas = duas.top(prefs.resultsPerSource),
            names = names.top(prefs.resultsPerSource),
        )
    }

    /** One question put to every source: the text, how to match it, and what a hit is worth. */
    private data class Pass(val term: String, val match: SearchMatch, val score: Int)

    /**
     * Accumulates per-record scores. Ties are broken by the best position the record held
     * in any list it appeared in — each list arrives best match first — and then by
     * first-seen order.
     */
    private class Ranked<K, V> {
        private val entries = LinkedHashMap<K, Entry<V>>()

        private class Entry<V>(val value: V, var score: Int, var bestRank: Int)

        fun add(key: K, value: V, score: Int, rank: Int) {
            val existing = entries[key]
            if (existing != null) {
                existing.score += score
                existing.bestRank = minOf(existing.bestRank, rank)
            } else {
                entries[key] = Entry(value, score, rank)
            }
        }

        fun top(limit: Int): List<V> =
            entries.values
                // Stable sort: equal scores and ranks keep first-seen order.
                .sortedWith(compareByDescending<Entry<V>> { it.score }.thenBy { it.bestRank })
                .take(limit)
                .map { it.value }
    }

    companion object {
        const val DEFAULT_TRANSLATOR = "sahih_international"

        /** A whole-phrase hit outranks any combination of the hits below it. */
        private const val PHRASE_SCORE = 100

        /**
         * Every word present, any order. Above any realistic sum of single-word hits
         * (`strictness.wordPasses` is single digits), below a phrase.
         */
        private const val ALL_WORDS_SCORE = 10

        /** One significant word. */
        private const val WORD_SCORE = 1

        /** Bound the per-source DB work for very long queries/term lists. */
        // MAX_WORD_QUERIES and MAX_PER_SOURCE used to live here as 8 and 60. They are
        // SearchPreferences.strictness and .resultsPerSource now: the 60 was invisible and
        // unchangeable, so a search for الله returning 180 results — three sources at their
        // cap — read as a defect rather than as a setting.

        private val NON_WORD = Regex("[^\\p{L}\\p{N}']+")
        private val STOPWORDS = SearchTermExpander.STOPWORDS

        /** Significant lowercase words of a query, longest (most distinctive) first. */
        fun tokenize(query: String): List<String> =
            query.lowercase()
                .split(NON_WORD)
                .filter { it.length > 2 && it !in STOPWORDS }
                .distinct()
                .sortedByDescending { it.length }
    }
}
