package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.SurahTopic
import com.arshadshah.nimaz.domain.model.TopicCitation
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One visible row of the tree: a subject, and how far in it sits. */
data class TopicRowItem(val topic: QuranTopic, val depth: Int)

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
    val canGoBack: Boolean get() = expanded.any { id -> rows.any { it.topic.id == id } } ||
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

/**
 * One surah's worth of a topic's citations, under the surah's own name.
 *
 * [isFromSurah] marks the group the reader arrived from, which is drawn first and named as
 * theirs — see [TopicDetailState.surahContext].
 */
data class CitationGroup(
    val surahNumber: Int,
    val surahName: String,
    val citations: List<TopicCitation>,
    val isFromSurah: Boolean = false,
)

/** The surah a subject was opened from, and how much of this subject sits in it. */
data class TopicSurahContext(
    val surahNumber: Int,
    val surahName: String,
    val verseCount: Int,
)

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

sealed interface QuranTopicsEvent {
    /** The browser is on screen and wants its current level. Idempotent — safe to re-send. */
    data object OpenBrowser : QuranTopicsEvent

    data class SelectTree(val tree: TopicTree) : QuranTopicsEvent

    /** Open or close a node's children in place. */
    data class Toggle(val topic: QuranTopic) : QuranTopicsEvent

    /** Re-root the tree on [topic], pushing it onto the crumb bar. */
    data class Focus(val topic: QuranTopic) : QuranTopicsEvent

    /**
     * Re-root at a crumb. [index] is a position in [TopicBrowseState.focus];
     * [ROOT] goes back to the whole tree.
     */
    data class RebaseTo(val index: Int) : QuranTopicsEvent {
        companion object {
            const val ROOT = -1
        }
    }

    /**
     * What the system back gesture does: close the innermost open node, or — with nothing open
     * — step back out of one focus. The screen pops only when neither is left.
     */
    data object Back : QuranTopicsEvent

    data class Search(val query: String) : QuranTopicsEvent
    data object ClearSearch : QuranTopicsEvent

    /** The subjects one surah speaks about. Idempotent — safe to re-send for the same surah. */
    data class LoadSurahSubjects(val surahNumber: Int) : QuranTopicsEvent

    /** Filter the loaded surah subjects. In memory; no query is run. */
    data class FilterSurahSubjects(val query: String) : QuranTopicsEvent

    data object ClearSurahSubjectsFilter : QuranTopicsEvent

    /**
     * One subject's detail. [fromSurah] is the surah the reader came from, whose citations are
     * pinned to the top — null when they came from somewhere with no surah in hand.
     */
    data class LoadDetail(
        val topicId: Int,
        val tree: TopicTree,
        val fromSurah: Int? = null,
    ) : QuranTopicsEvent
}

@HiltViewModel
class QuranTopicsViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _browseState = MutableStateFlow(TopicBrowseState())
    val browseState: StateFlow<TopicBrowseState> = _browseState.asStateFlow()

    private val _detailState = MutableStateFlow(TopicDetailState())
    val detailState: StateFlow<TopicDetailState> = _detailState.asStateFlow()

    private val _surahSubjects = MutableStateFlow(SurahSubjectsState())
    val surahSubjects: StateFlow<SurahSubjectsState> = _surahSubjects.asStateFlow()

    private val queries = MutableStateFlow("")

    /**
     * Only the query pipeline. The roots are *not* loaded here.
     *
     * Both screens resolve this ViewModel per back-stack entry, and topic detail is reachable
     * from a topic description's cross-links — so a reader wandering five topics deep would
     * fire five root queries for a list none of those screens shows. The browser asks for its
     * own level with [QuranTopicsEvent.OpenBrowser].
     */
    init {
        observeQueries()
    }

    fun onEvent(event: QuranTopicsEvent) {
        when (event) {
            QuranTopicsEvent.OpenBrowser -> {
                if (_browseState.value.level.isEmpty()) loadRoots(_browseState.value.tree)
            }

            is QuranTopicsEvent.SelectTree -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.QURAN_TOPICS, "select_tree")
                selectTree(event.tree)
            }

            is QuranTopicsEvent.Toggle -> toggle(event.topic)

            is QuranTopicsEvent.Focus -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.QURAN_TOPICS, "focus_branch")
                focus(event.topic)
            }

            is QuranTopicsEvent.RebaseTo -> rebaseTo(event.index)

            QuranTopicsEvent.Back -> back()

            is QuranTopicsEvent.Search -> {
                _browseState.update { it.copy(searchQuery = event.query) }
                queries.value = event.query
            }

            QuranTopicsEvent.ClearSearch -> {
                _browseState.update {
                    it.copy(
                        searchQuery = "",
                        searchResults = emptyList(),
                        searchPaths = emptyMap(),
                        isSearching = false,
                    )
                }
                queries.value = ""
            }

            is QuranTopicsEvent.LoadSurahSubjects -> {
                if (_surahSubjects.value.surahNumber != event.surahNumber) {
                    AppAnalytics.logFeatureUsed(AppAnalytics.Feature.QURAN_TOPICS, "open_surah_subjects")
                    loadSurahSubjects(event.surahNumber)
                }
            }

            is QuranTopicsEvent.FilterSurahSubjects ->
                _surahSubjects.update { it.copy(query = event.query) }

            QuranTopicsEvent.ClearSurahSubjectsFilter ->
                _surahSubjects.update { it.copy(query = "") }

            is QuranTopicsEvent.LoadDetail -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.QURAN_TOPICS, "open_detail")
                loadDetail(event.topicId, event.tree, event.fromSurah)
            }
        }
    }

    /**
     * Debounced so a query is run once the typing settles, not once per keystroke. Each search
     * is an FTS walk plus an `IN (…)` over up to 60 ids; at one per character a fast typist
     * would queue a dozen of them to display the last.
     */
    @OptIn(FlowPreview::class)
    private fun observeQueries() {
        viewModelScope.launch {
            queries
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _browseState.update {
                            it.copy(
                                searchResults = emptyList(),
                                searchPaths = emptyMap(),
                                isSearching = false,
                            )
                        }
                        return@collect
                    }
                    _browseState.update { it.copy(isSearching = true) }
                    val tree = _browseState.value.tree
                    val results = quranUseCases.searchTopics(query)
                    val paths = quranUseCases.searchTopics.pathsFor(results, tree)
                    _browseState.update { state ->
                        // The query may have been cleared while this was in flight; dropping the
                        // stale result is what keeps a cleared box from repopulating itself.
                        if (state.searchQuery.isBlank()) state
                        else state.copy(
                            searchResults = results,
                            searchPaths = paths,
                            isSearching = false,
                        )
                    }
                }
        }
    }

    private fun selectTree(tree: TopicTree) {
        if (tree == _browseState.value.tree) return
        // A different hierarchy is a different set of parents, so nothing carries over: not the
        // focus, not what was open, and not the cached children keyed by a parent id that means
        // something else here.
        _browseState.update {
            it.copy(
                tree = tree,
                focus = emptyList(),
                expanded = emptySet(),
                children = emptyMap(),
                isLoading = true,
            )
        }
        loadRoots(tree)
    }

    private fun loadRoots(tree: TopicTree) {
        viewModelScope.launch {
            val available = quranUseCases.hasThematicContent()
            val roots = if (available) quranUseCases.getTopicTreeRoots(tree) else emptyList()
            val branches =
                if (available) quranUseCases.getTopicChildren.branchesIn(tree) else emptySet()
            _browseState.update {
                it.copy(
                    level = roots,
                    branchIds = branches,
                    isLoading = false,
                    isAvailable = available,
                )
            }
        }
    }

    /**
     * Open or close a node.
     *
     * Closing keeps the descendants' open state — reopening restores the shape the reader had
     * rather than making them walk back down. Opening loads the children once; after that the
     * map answers and there is no query at all.
     */
    private fun toggle(topic: QuranTopic) {
        val state = _browseState.value
        if (topic.id in state.expanded) {
            _browseState.update { it.copy(expanded = it.expanded - topic.id) }
            return
        }
        if (state.children.containsKey(topic.id)) {
            _browseState.update { it.copy(expanded = it.expanded + topic.id) }
            return
        }
        viewModelScope.launch {
            val loaded = quranUseCases.getTopicChildren(topic.id, state.tree)
            _browseState.update {
                // An empty result means the branch set and the corpus disagree. Cache it anyway
                // so the row stops asking, and leave it closed rather than opening onto nothing.
                it.copy(
                    children = it.children + (topic.id to loaded),
                    expanded = if (loaded.isEmpty()) it.expanded else it.expanded + topic.id,
                )
            }
        }
    }

    /** Re-root on [topic], with everything between the current root and it becoming crumbs. */
    private fun focus(topic: QuranTopic) {
        val state = _browseState.value
        val trail = state.focus + ancestorsWithin(state, topic) + topic
        viewModelScope.launch {
            val level = state.children[topic.id]
                ?: quranUseCases.getTopicChildren(topic.id, state.tree)
            _browseState.update {
                it.copy(
                    focus = trail,
                    level = level,
                    expanded = emptySet(),
                    children = it.children + (topic.id to level),
                    searchQuery = "",
                    searchResults = emptyList(),
                    searchPaths = emptyMap(),
                )
            }
        }
    }

    private fun rebaseTo(index: Int) {
        val state = _browseState.value
        if (index >= state.focus.lastIndex) return
        if (index < 0) {
            _browseState.update {
                it.copy(focus = emptyList(), expanded = emptySet(), isLoading = true)
            }
            loadRoots(state.tree)
            return
        }
        val trail = state.focus.take(index + 1)
        val target = trail.last()
        viewModelScope.launch {
            val level = state.children[target.id]
                ?: quranUseCases.getTopicChildren(target.id, state.tree)
            _browseState.update {
                it.copy(focus = trail, level = level, expanded = emptySet())
            }
        }
    }

    /**
     * Close the innermost open node, else step out of one focus.
     *
     * Innermost, not outermost: a reader who opened three levels expects back to undo the last
     * of them, the way it undoes the last of anything else.
     */
    private fun back() {
        val state = _browseState.value
        val deepest = state.rows.lastOrNull { it.topic.id in state.expanded }
        if (deepest != null) {
            _browseState.update { it.copy(expanded = it.expanded - deepest.topic.id) }
            return
        }
        if (state.focus.isNotEmpty()) rebaseTo(state.focus.lastIndex - 1)
    }

    /**
     * [topic]'s ancestors inside what is currently on screen, root-first.
     *
     * Walked with the parent ids the model already carries rather than a query, against an
     * index of the nodes this browser has loaded. The visited set is the same guard the
     * repository's breadcrumb walk uses: the corpus's parents are not guaranteed acyclic.
     */
    private fun ancestorsWithin(state: TopicBrowseState, topic: QuranTopic): List<QuranTopic> {
        val loaded = (state.level + state.children.values.flatten()).associateBy { it.id }
        val trail = ArrayDeque<QuranTopic>()
        val seen = mutableSetOf(topic.id)
        var parentId = topic.parentIn(state.tree)
        while (parentId != null && seen.add(parentId)) {
            val parent = loaded[parentId] ?: break
            trail.addFirst(parent)
            parentId = parent.parentIn(state.tree)
        }
        return trail.toList()
    }

    /**
     * The subjects one surah is cited under, and the surah's own name for the top bar.
     *
     * One query for the list; the name comes from the surah list, which the home screen has
     * already warmed. A surah whose artifact predates the thematic layer simply has no rows,
     * which the screen says in words rather than treating as a failure.
     */
    private fun loadSurahSubjects(surahNumber: Int) {
        viewModelScope.launch {
            _surahSubjects.value = SurahSubjectsState(
                surahNumber = surahNumber,
                isLoading = true,
            )
            val subjects = quranUseCases.getTopicsForSurah(surahNumber)
            // Only asked when there is nothing to show, because that is the only time the
            // answer changes what is said. It is a cached count either way.
            val available = subjects.isNotEmpty() || quranUseCases.hasThematicContent()
            val name = quranUseCases.getSurahList().first()
                .firstOrNull { it.number == surahNumber }
                ?.nameEnglish
                .orEmpty()
            _surahSubjects.update { state ->
                // A second surah may have been asked for while this was in flight — the pane
                // layouts can swap the detail surah without leaving the screen.
                if (state.surahNumber != surahNumber) state
                else state.copy(
                    surahName = name,
                    subjects = subjects,
                    isAvailable = available,
                    isLoading = false,
                )
            }
        }
    }

    /**
     * A topic, its citations grouped under the surahs they fall in, and a line of each verse.
     *
     * The previews are one query for the whole list — `getTranslationsForAyahs` takes the ids
     * as an `IN (…)` — so "Allah" costs two reads rather than 153. A translation the device
     * does not have simply yields nothing, and the rows fall back to bare references.
     *
     * The groups stay in the corpus's order — which is Qur'anic order — with exactly one
     * exception: the surah the reader came from is lifted to the front. Ordering by relevance
     * would be replacing the mushaf's sequence with one of our own; lifting the surah they are
     * holding is answering the question they opened the subject with.
     */
    private fun loadDetail(topicId: Int, tree: TopicTree, fromSurah: Int? = null) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            val detail = quranUseCases.getTopicDetail(topicId, tree)
            if (detail == null) {
                _detailState.value = TopicDetailState(isLoading = false)
                return@launch
            }

            val names = quranUseCases.getSurahList().first().associate {
                it.number to it.nameEnglish
            }
            val groups = detail.citations
                .groupBy { it.surahNumber }
                .map { (surah, citations) ->
                    CitationGroup(
                        surahNumber = surah,
                        surahName = names[surah].orEmpty(),
                        citations = citations,
                        isFromSurah = surah == fromSurah,
                    )
                }
                .sortedByDescending { it.isFromSurah }

            _detailState.value = TopicDetailState(
                detail = detail,
                citationGroups = groups,
                // Only where the subject actually reaches that surah. A context line reading
                // "0 verses in Al-Fatiha" is a label for something that is not there.
                surahContext = groups.firstOrNull { it.isFromSurah }?.let { group ->
                    TopicSurahContext(
                        surahNumber = group.surahNumber,
                        surahName = group.surahName,
                        verseCount = group.citations.size,
                    )
                },
                isLoading = false,
            )

            val previews = previewsFor(detail.citations.map { it.ayahId })
            if (previews.isNotEmpty()) {
                _detailState.update { state ->
                    // The reader may have opened a different topic while this was in flight;
                    // previews keyed to the previous one must not land on top of it.
                    if (state.detail?.topic?.id != topicId) state
                    else state.copy(previews = previews)
                }
            }
        }
    }

    private suspend fun previewsFor(ayahIds: List<Int>): Map<Int, String> {
        if (ayahIds.isEmpty()) return emptyMap()
        val translatorId = settingsRepository.quranTranslatorId.first()
        return runCatching {
            quranUseCases.getAyahTranslation.forAyahs(ayahIds, translatorId)
        }.getOrDefault(emptyMap())
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
