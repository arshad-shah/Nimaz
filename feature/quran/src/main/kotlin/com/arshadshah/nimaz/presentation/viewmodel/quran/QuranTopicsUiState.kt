package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.SurahTopic
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree

/**
 * Browsing and searching the Qur'an's subject hierarchies.
 *
 * The browser is a **front page, not a tree**. Each hierarchy gets the shape that suits it:
 * Themes is three chapter cards whose branches are chips, sized by how much of the Qur'an sits
 * under each; Kinds is a grid of fourteen tiles; Index is a concordance — A–Z with a letter
 * rail, or its most-cited entries. Every tap opens a subject, and the subject screen is where
 * its subtopics, verses and place in the tree live.
 *
 * It used to be one indented tree that opened in place, with a crumb bar and a "focus this
 * branch" control for the depth where indenting ran out. That kept the context, but a list of
 * nouns and numbers is the same list whichever hierarchy it came from, and all three read the
 * same: bland. The counts on every card come from one [com.arshadshah.nimaz.domain.model.TopicCatalog]
 * built once per load, so they are distinct verses and switching tabs costs no query.
 *
 * Search is a mode over the same screen rather than another screen, because a query that
 * matches nothing should fall back to *where you were*, not to an empty page.
 */
data class TopicBrowseState(
    val tree: TopicTree = TopicTree.THEMATIC,

    /** Themes: one card per root of the curated outline, with its branches. */
    val themes: List<TopicThemeCard> = emptyList(),

    /** Kinds: the ontology's roots, biggest first. */
    val kinds: List<TopicTally> = emptyList(),

    /** Index: every top-level entry of the concordance, alphabetically. */
    val index: List<TopicTally> = emptyList(),

    val indexSort: TopicIndexSort = TopicIndexSort.A_TO_Z,

    val searchQuery: String = "",

    /** Matches across all three hierarchies, each placed in the one it would open in. */
    val searchResults: List<TopicSearchHit> = emptyList(),

    val isSearching: Boolean = false,
    val isLoading: Boolean = true,

    /**
     * Whether this install's artifact carries the thematic layer at all. False means the
     * migration ran but a schemaVersion 24 artifact has not arrived yet — an explainable
     * state, not an error.
     */
    val isAvailable: Boolean = true,
) {
    val isSearchMode: Boolean get() = searchQuery.isNotBlank()

    /**
     * The index under its initial letters, for the A–Z list and its rail.
     *
     * Keyed on the first Latin letter of the name with its accents stripped, so "ʿĀd" files
     * under A beside "Aaron" rather than under a section of its own. A name with no Latin
     * letter at all files under `#`, last.
     */
    val indexSections: List<TopicIndexSection> by lazy {
        index.groupBy { indexLetter(it.topic.name) }
            .toSortedMap(compareBy<Char> { it == OTHER_LETTER }.thenBy { it })
            .map { (letter, entries) -> TopicIndexSection(letter, entries) }
    }

    /** The concordance's most-cited entries — the other way into 1,780 of them. */
    val mostCited: List<TopicTally> by lazy {
        index.sortedWith(compareByDescending<TopicTally> { it.verseCount }.thenBy { it.topic.name })
            .take(MOST_CITED_LIMIT)
    }

    companion object {
        const val MOST_CITED_LIMIT = 120
        const val OTHER_LETTER = '#'

        fun indexLetter(name: String): Char {
            val folded = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
            return folded.firstOrNull { it in 'A'..'Z' || it in 'a'..'z' }?.uppercaseChar()
                ?: OTHER_LETTER
        }
    }
}

/** A subject and how many distinct verses sit beneath it in the hierarchy being shown. */
data class TopicTally(
    val topic: QuranTopic,
    val verseCount: Int,
    val childCount: Int = 0,
)

/** One root of the curated outline and its branches, biggest first. */
data class TopicThemeCard(
    val root: TopicTally,
    val branches: List<TopicTally>,
)

data class TopicIndexSection(val letter: Char, val entries: List<TopicTally>)

enum class TopicIndexSort { A_TO_Z, MOST_CITED }

/**
 * A search match, with where it sits.
 *
 * [tree] is the hierarchy the match opens in — the curated outline if it is there, else the
 * ontology, else the index — and [path] is its ancestors in that tree, so sixty matched words
 * are not sixty free-floating words.
 */
data class TopicSearchHit(
    val topic: QuranTopic,
    val tree: TopicTree,
    val path: List<QuranTopic>,
    val verseCount: Int,
)

/**
 * The subjects one surah speaks about.
 *
 * A flat, weighted list and not a tree: the three hierarchies place a subject relative to other
 * subjects, which is a question about the index. "What is this surah about" is a question about
 * these verses, and its answer is the subjects they are actually cited under, most-cited first.
 *
 * [query] filters what is already loaded rather than re-querying. The list is at most a few
 * hundred rows and already in memory, so a debounce and an FTS walk would buy latency and a
 * result set that no longer means "in this surah".
 */
data class SurahSubjectsState(
    val surahNumber: Int = 0,
    val surahName: String = "",
    val subjects: List<SurahTopic> = emptyList(),
    val query: String = "",

    /**
     * Whether this install's artifact carries the thematic layer at all.
     *
     * The same distinction [TopicBrowseState.isAvailable] draws, and for the same reason: an
     * empty list means "your content predates the subject index" far more often than it means
     * "this surah has no subjects", and those are two different sentences.
     */
    val isAvailable: Boolean = true,

    val isLoading: Boolean = true,
) {
    val visible: List<SurahTopic> by lazy {
        val needle = query.trim()
        if (needle.isEmpty()) subjects
        else subjects.filter {
            it.topic.name.contains(needle, ignoreCase = true) ||
                    it.topic.arabicName.contains(needle)
        }
    }

    /**
     * How many subject-to-verse citations land in this surah.
     *
     * Citations and not distinct verses: a verse indexed under three subjects is three of
     * these, and de-duplicating would need the ayah ids, which is the citation list this
     * screen deliberately does not load.
     */
    val citations: Int by lazy { subjects.sumOf { it.versesInSurah } }
}

data class TopicDetailState(
    val detail: TopicDetail? = null,

    /**
     * The citations, grouped by surah in the order the corpus gives them.
     *
     * Grouped and not re-sorted: the citations arrive ordered by ayah id, which is Qur'anic
     * order, and re-sorting would be replacing the mushaf's sequence with one of our own.
     */
    val citationGroups: List<CitationGroup> = emptyList(),

    /**
     * The first line of each cited verse, by ayah id.
     *
     * The whole citation list used to read `2:153 — Open in reader`, 153 times, which is a row
     * that tells you nothing and a subtitle that tells you less. Empty when the reader's chosen
     * translation has no text for these verses; the rows then show the reference alone rather
     * than a gap where a sentence should be.
     */
    val previews: Map<Int, String> = emptyMap(),

    /**
     * The surah this subject was opened from, when it was opened from one.
     *
     * Null everywhere else, and the screen then shows exactly what it showed before: the
     * citations in Qur'anic order with no group singled out.
     */
    val surahContext: TopicSurahContext? = null,

    /**
     * The subtopics with their own subtrees counted, biggest first.
     *
     * [TopicDetail.children] carries each child's *own* citations, which for a branch is
     * usually zero — the "0 verses" the old detail screen showed under "Prophets". Empty until
     * the catalogue has been counted; the screen shows the children without numbers meanwhile.
     */
    val subtopics: List<TopicTally> = emptyList(),

    val isLoading: Boolean = true,
) {
    /**
     * The surahs this subject is most concentrated in, for the "where it appears" chart.
     *
     * Only when there is more than one surah — a chart of one bar says nothing the verse list
     * does not.
     */
    val topSurahs: List<CitationGroup> by lazy {
        if (citationGroups.size < 2) emptyList()
        else citationGroups.sortedByDescending { it.citations.size }.take(TOP_SURAHS)
    }

    private companion object {
        const val TOP_SURAHS = 5
    }
}
