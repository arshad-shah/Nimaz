package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Browsing and searching the Qur'an's subject hierarchies.
 *
 * The browser is a *stack*, not a screen per level. Walking from "Stories" to "Musa" to
 * "parting of the Red Sea" is three levels of the same list, and the ontology goes five deep —
 * a route per level would mean five back-stack entries for one act of browsing, and a tree
 * switch would strand the user halfway down a tree that no longer exists. [TopicBrowseState.path]
 * holds the descent, [QuranTopicsEvent.Ascend] pops it.
 *
 * Search is a separate mode over the same list rather than a separate screen, because a query
 * that matches nothing should fall back to *where you were*, not to an empty screen.
 */
data class TopicBrowseState(
    val tree: TopicTree = TopicTree.THEMATIC,

    /** The descent from the tree's root. Empty means the roots are showing. */
    val path: List<QuranTopic> = emptyList(),

    /** What the current level lists — roots, or the children of `path.last()`. */
    val topics: List<QuranTopic> = emptyList(),

    val searchQuery: String = "",
    val searchResults: List<QuranTopic> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,

    /**
     * Whether this install's artifact carries the thematic layer at all. False means the
     * migration ran but a schemaVersion 24 artifact has not arrived yet — an explainable
     * state, not an error.
     */
    val isAvailable: Boolean = true,
) {
    val current: QuranTopic? get() = path.lastOrNull()
    val isBrowsingRoots: Boolean get() = path.isEmpty()

    /** What the list should show: search results while a query is live, else the level. */
    val visibleTopics: List<QuranTopic>
        get() = if (searchQuery.isBlank()) topics else searchResults
}

data class TopicDetailState(
    val detail: TopicDetail? = null,
    val isLoading: Boolean = true,
)

sealed interface QuranTopicsEvent {
    /** The browser is on screen and wants its current level. Idempotent — safe to re-send. */
    data object OpenBrowser : QuranTopicsEvent

    data class SelectTree(val tree: TopicTree) : QuranTopicsEvent

    /** Descend into a topic that has children. */
    data class Descend(val topic: QuranTopic) : QuranTopicsEvent

    /** Back up one level. Returns false-ish (a no-op) at the roots; the screen pops instead. */
    data object Ascend : QuranTopicsEvent

    data class Search(val query: String) : QuranTopicsEvent
    data object ClearSearch : QuranTopicsEvent

    data class LoadDetail(val topicId: Int, val tree: TopicTree) : QuranTopicsEvent
}

@HiltViewModel
class QuranTopicsViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases
) : ViewModel() {

    private val _browseState = MutableStateFlow(TopicBrowseState())
    val browseState: StateFlow<TopicBrowseState> = _browseState.asStateFlow()

    private val _detailState = MutableStateFlow(TopicDetailState())
    val detailState: StateFlow<TopicDetailState> = _detailState.asStateFlow()

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
                if (_browseState.value.topics.isEmpty()) loadRoots(_browseState.value.tree)
            }

            is QuranTopicsEvent.SelectTree -> {
                AppAnalytics.logFeatureUsed("quran_topics", "select_tree")
                selectTree(event.tree)
            }

            is QuranTopicsEvent.Descend -> {
                AppAnalytics.logFeatureUsed("quran_topics", "descend")
                descend(event.topic)
            }

            QuranTopicsEvent.Ascend -> ascend()

            is QuranTopicsEvent.Search -> {
                _browseState.update { it.copy(searchQuery = event.query) }
                queries.value = event.query
            }

            QuranTopicsEvent.ClearSearch -> {
                _browseState.update {
                    it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false)
                }
                queries.value = ""
            }

            is QuranTopicsEvent.LoadDetail -> {
                AppAnalytics.logFeatureUsed("quran_topics", "open_detail")
                loadDetail(event.topicId, event.tree)
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
                            it.copy(searchResults = emptyList(), isSearching = false)
                        }
                        return@collect
                    }
                    _browseState.update { it.copy(isSearching = true) }
                    val results = quranUseCases.searchTopics(query)
                    _browseState.update { state ->
                        // The query may have been cleared while this was in flight; dropping the
                        // stale result is what keeps a cleared box from repopulating itself.
                        if (state.searchQuery.isBlank()) state
                        else state.copy(searchResults = results, isSearching = false)
                    }
                }
        }
    }

    private fun selectTree(tree: TopicTree) {
        if (tree == _browseState.value.tree) return
        _browseState.update { it.copy(tree = tree, path = emptyList(), isLoading = true) }
        loadRoots(tree)
    }

    private fun loadRoots(tree: TopicTree) {
        viewModelScope.launch {
            val available = quranUseCases.hasThematicContent()
            val roots = if (available) quranUseCases.getTopicTreeRoots(tree) else emptyList()
            _browseState.update {
                it.copy(topics = roots, isLoading = false, isAvailable = available)
            }
        }
    }

    /**
     * Descend, but only where there is somewhere to go.
     *
     * A topic's children are not known until they are fetched, so this loads first and pushes
     * the path only when the level is non-empty — otherwise a leaf would push a level that
     * renders as an empty list with a back button.
     */
    private fun descend(topic: QuranTopic) {
        viewModelScope.launch {
            _browseState.update { it.copy(isLoading = true) }
            val tree = _browseState.value.tree
            val children = quranUseCases.getTopicDetail(topic.id, tree)?.children.orEmpty()
            _browseState.update { state ->
                if (children.isEmpty()) {
                    state.copy(isLoading = false)
                } else {
                    state.copy(
                        path = state.path + topic,
                        topics = children,
                        isLoading = false,
                        searchQuery = "",
                        searchResults = emptyList(),
                    )
                }
            }
        }
    }

    private fun ascend() {
        val state = _browseState.value
        if (state.path.isEmpty()) return
        val path = state.path.dropLast(1)
        _browseState.update { it.copy(path = path, isLoading = true) }
        viewModelScope.launch {
            val level = path.lastOrNull()
                ?.let { quranUseCases.getTopicDetail(it.id, state.tree)?.children.orEmpty() }
                ?: quranUseCases.getTopicTreeRoots(state.tree)
            _browseState.update { it.copy(topics = level, isLoading = false) }
        }
    }

    private fun loadDetail(topicId: Int, tree: TopicTree) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            val detail = quranUseCases.getTopicDetail(topicId, tree)
            _detailState.update { TopicDetailState(detail = detail, isLoading = false) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
