package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.SurahTopic
import com.arshadshah.nimaz.domain.model.TopicCitation
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.quran.RollUpTopicCounts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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

@HiltViewModel
class QuranTopicsViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val rollUpTopicCounts: RollUpTopicCounts,
    private val quranSettings: QuranPreferences,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _browseState = MutableStateFlow(TopicBrowseState())
    val browseState: StateFlow<TopicBrowseState> = _browseState.asStateFlow()

    private val _detailState = MutableStateFlow(TopicDetailState())
    val detailState: StateFlow<TopicDetailState> = _detailState.asStateFlow()

    private val _surahSubjects = MutableStateFlow(SurahSubjectsState())
    val surahSubjects: StateFlow<SurahSubjectsState> = _surahSubjects.asStateFlow()

    private val queries = MutableStateFlow("")

    /**
     * The in-flight browse navigation — a focus or a crumb rebase.
     *
     * One handle, because the browser is at one place at a time. Without it, tapping crumb 0
     * and then crumb 2 left both `getTopicChildren` calls racing, and each ends in a whole-state
     * update setting `focus` *and* `level` together: whichever query was slower wrote last, so
     * a reader who tapped crumb 2 landed on crumb 0.
     */
    private var browseJob: Job? = null

    /** The in-flight topic-detail load. See [requestedTopicId]. */
    private var detailJob: Job? = null

    /**
     * The topic the detail pane is currently *for*, set synchronously when it is asked for.
     *
     * Cancelling [detailJob] is necessary but not sufficient: a coroutine cancelled after its
     * last suspension point still runs to the end of its block. This is what the writes are
     * checked against, the way [loadSurahSubjects] already checks its surah.
     */
    private var requestedTopicId: Int? = null

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
                telemetry.featureUsed(AppAnalytics.Feature.QURAN_TOPICS, "select_tree")
                selectTree(event.tree)
            }

            is QuranTopicsEvent.Toggle -> toggle(event.topic)

            is QuranTopicsEvent.Focus -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN_TOPICS, "focus_branch")
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
                    telemetry.featureUsed(AppAnalytics.Feature.QURAN_TOPICS, "open_surah_subjects")
                    loadSurahSubjects(event.surahNumber)
                }
            }

            is QuranTopicsEvent.FilterSurahSubjects ->
                _surahSubjects.update { it.copy(query = event.query) }

            QuranTopicsEvent.ClearSurahSubjectsFilter ->
                _surahSubjects.update { it.copy(query = "") }

            is QuranTopicsEvent.LoadDetail -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN_TOPICS, "open_detail")
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
        launchSafely(telemetry, AppAnalytics.Feature.QURAN_TOPICS, "observe_queries") {
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
                    // Logged post-debounce, where the FTS walk actually happens. The topic
                    // search ran the most expensive query in the feature and logged nothing,
                    // so its usage read as zero next to `select_tree` and `focus_branch`.
                    telemetry.search(AppAnalytics.Feature.QURAN_TOPICS, query.trim().length)
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
        launchSafely(
            telemetry, AppAnalytics.Feature.QURAN_TOPICS, "load_roots",
            // The browse screen has no error surface of its own — `isAvailable` already
            // distinguishes "this install has no thematic content" from "this branch is
            // empty", and a failed read is a third thing. Clearing the spinner resolves it
            // to the unavailable copy, which is at least true of what is on screen.
            onFailure = { _browseState.update { it.copy(isLoading = false) } },
        ) {
            val available = quranUseCases.hasThematicContent()
            val roots = if (available) quranUseCases.getTopicTreeRoots(tree) else emptyList()
            val branches =
                if (available) quranUseCases.getTopicChildren.branchesIn(tree) else emptySet()
            // Once per tree load, here — not per composition, and not per row. A branch's own
            // citation count is usually zero, so without this the three roots opened reading
            // "0 verses" under a home screen advertising 2,512 subjects.
            val counts = if (available) {
                rollUpTopicCounts(quranUseCases.getAllTopics(), tree)
            } else {
                emptyMap()
            }
            _browseState.update {
                it.copy(
                    level = roots,
                    branchIds = branches,
                    rolledUpCounts = counts,
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
        val focusWhenAsked = state.focus
        launchSafely(telemetry, AppAnalytics.Feature.QURAN_TOPICS, "toggle") {
            val loaded = quranUseCases.getTopicChildren(topic.id, state.tree)
            _browseState.update {
                // An empty result means the branch set and the corpus disagree. Cache it anyway
                // so the row stops asking, and leave it closed rather than opening onto nothing.
                //
                // Caching is always safe — a node's children do not depend on where the browser
                // is. *Expanding* does: a focus or rebase landing while this was in flight reset
                // `expanded` and moved to a different level, where this row is not on screen, so
                // opening it would re-open a node the reader had just navigated away from.
                //
                // Deliberately not sharing `browseJob`: two toggles on two rows are both
                // legitimate and must not cancel each other.
                it.copy(
                    children = it.children + (topic.id to loaded),
                    expanded = when {
                        loaded.isEmpty() -> it.expanded
                        it.focus != focusWhenAsked -> it.expanded
                        else -> it.expanded + topic.id
                    },
                )
            }
        }
    }

    /** Re-root on [topic], with everything between the current root and it becoming crumbs. */
    private fun focus(topic: QuranTopic) {
        val state = _browseState.value
        val trail = state.focus + ancestorsWithin(state, topic) + topic
        browseJob?.cancel()
        browseJob = launchSafely(telemetry, AppAnalytics.Feature.QURAN_TOPICS, "focus") {
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
        browseJob?.cancel()
        browseJob = launchSafely(
            telemetry,
            AppAnalytics.Feature.QURAN_TOPICS,
            "rebase_to",
            onFailure = { _browseState.update { it.copy(isLoading = false) } },
        ) {
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
        launchSafely(
            telemetry,
            AppAnalytics.Feature.QURAN_TOPICS,
            "load_surah_subjects",
            onFailure = { _surahSubjects.update { it.copy(isLoading = false) } },
        ) {
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
        requestedTopicId = topicId
        detailJob?.cancel()
        detailJob = launchSafely(
            telemetry,
            AppAnalytics.Feature.QURAN_TOPICS,
            "load_detail",
            onFailure = { _detailState.update { it.copy(isLoading = false) } },
        ) {
            _detailState.update { it.copy(isLoading = true) }
            val detail = quranUseCases.getTopicDetail(topicId, tree)
            if (requestedTopicId != topicId) return@launchSafely
            if (detail == null) {
                _detailState.value = TopicDetailState(isLoading = false)
                return@launchSafely
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

            // A hard assign here was the compounding half of the race: with two loads in
            // flight, the one completing second replaced the whole object — wiping not just
            // the other topic's detail but any previews that had already landed for it,
            // walking straight past the staleness guard this same function applies below.
            if (requestedTopicId != topicId) return@launchSafely
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
        val translatorId = quranSettings.quranTranslatorId.first()
        return runCatching {
            quranUseCases.getAyahTranslation.forAyahs(ayahIds, translatorId)
        }.getOrDefault(emptyMap())
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
