package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.SurahTopic
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicBrowseState.Companion.MAX_DEPTH

/**
 * Browsing and searching the Qur'an's subject hierarchies.
 *
 * The browser is **one tree that expands in place**, not a stack of levels. Descending used to
 * replace the list with the children, so walking from "Stories" to "Musa" to "the parting of the
 * sea" discarded every sibling on the way and left a breadcrumb that had truncated by level
 * three of a hierarchy that goes five deep. Now a node's children insert beneath it and the
 * context stays on screen.
 *
 * What replaces the descent stack is [TopicBrowseState.focus]: a *rebase*, taken deliberately
 * from a row too deep to indent further, and shown as a bar of tappable crumbs. Still one route
 * for the whole thing — the ontology is five levels and a route per level would mean five
 * back-stack entries for one act of browsing, and a tree switch would strand the reader halfway
 * down a tree that no longer exists.
 *
 * Search is a separate mode over the same list rather than a separate screen, because a query
 * that matches nothing should fall back to *where you were*, not to an empty screen.
 */
data class TopicBrowseState(
    val tree: TopicTree = TopicTree.THEMATIC,

    /**
     * The branch the tree is currently rooted at, from the hierarchy's own root down. Empty
     * means the whole tree is showing. Only ever grown by an explicit "focus this branch".
     */
    val focus: List<QuranTopic> = emptyList(),

    /** The top level of what is showing — the tree's roots, or [focus]'s last node's children. */
    val level: List<QuranTopic> = emptyList(),

    /** Which nodes are open. Kept when an ancestor closes, so reopening it restores the shape. */
    val expanded: Set<Int> = emptySet(),

    /** Children by parent id, loaded on first expand and kept for the session. */
    val children: Map<Int, List<QuranTopic>> = emptyMap(),

    /**
     * Which ids in [tree] have children.
     *
     * A row has to know it is a branch *before* it is tapped, because that is what decides
     * whether it gets a disclosure control at all — and a leaf that offers one is the dead tap
     * this state exists to remove. Loaded once per tree.
     */
    val branchIds: Set<Int> = emptySet(),

    /**
     * Verses beneath each subject, its whole subtree included, keyed by topic id.
     *
     * Rolled up **once per tree load** and kept here, not recomputed per composition: a fold
     * over 2,512 nodes to draw ten rows would be waste, and asking per node would be 2,512
     * queries. The rows carried each topic's own citation count before this, which for a branch
     * is usually zero — so the browser opened on three roots reading "0 verses".
     */
    val rolledUpCounts: Map<Int, Int> = emptyMap(),

    val searchQuery: String = "",
    val searchResults: List<QuranTopic> = emptyList(),

    /** Each result's ancestors, root-first, so a flat match is not a free-floating word. */
    val searchPaths: Map<Int, List<QuranTopic>> = emptyMap(),

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
     * The tree, flattened to what is actually on screen.
     *
     * Recursion stops at [MAX_DEPTH] whatever the expanded set says: past four levels of indent
     * a 390dp screen has no text column left, which is what "focus this branch" is for.
     *
     * `lazy` rather than a getter — the state is immutable, the list is read on every
     * recomposition, and walking a few hundred nodes each time to draw ten rows is waste.
     */
    val rows: List<TopicRowItem> by lazy {
        buildList {
            fun walk(items: List<QuranTopic>, depth: Int) {
                items.forEach { topic ->
                    add(TopicRowItem(topic, depth))
                    if (depth < MAX_DEPTH && topic.id in expanded) {
                        walk(children[topic.id].orEmpty(), depth + 1)
                    }
                }
            }
            walk(level, 0)
        }
    }

    /** Whether [topic] can be opened up. Never in search: a result is an answer, not a level. */
    fun isBranch(topic: QuranTopic): Boolean = !isSearchMode && topic.id in branchIds

    /**
     * Whether a branch at [depth] is opened in place or taken as a new root.
     *
     * At the cap there is no room to indent again, so the row offers to rebase the tree on
     * itself instead — which is the same act, just paid for with a crumb rather than 20dp.
     */
    fun isAtIndentCap(depth: Int): Boolean = depth >= MAX_DEPTH

    /** Back has somewhere to go while anything is open or the tree is rooted somewhere. */
    val canGoBack: Boolean
        get() = expanded.any { id -> rows.any { it.topic.id == id } } ||
                focus.isNotEmpty()

    private companion object {
        const val MAX_DEPTH = 3
    }
}

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

    val isLoading: Boolean = true,
)
