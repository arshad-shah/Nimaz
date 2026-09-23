package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.core.common.DefaultDispatcher
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicCatalog
import com.arshadshah.nimaz.domain.model.TopicCitation
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
    val surahArabicName: String = "",
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
    private val quranSettings: QuranPreferences,
    private val telemetry: Telemetry,
    @DefaultDispatcher private val computeDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _browseState = MutableStateFlow(TopicBrowseState())
    val browseState: StateFlow<TopicBrowseState> = _browseState.asStateFlow()

    private val _detailState = MutableStateFlow(TopicDetailState())
    val detailState: StateFlow<TopicDetailState> = _detailState.asStateFlow()

    private val _surahSubjects = MutableStateFlow(SurahSubjectsState())
    val surahSubjects: StateFlow<SurahSubjectsState> = _surahSubjects.asStateFlow()

    private val queries = MutableStateFlow("")

    /**
     * The subject index, once loaded. Kept so a search is a lookup rather than a query, and so
     * the detail screen's subtopic counts come from the same numbers the browser's cards showed.
     */
    private var catalog: TopicCatalog? = null

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
                if (catalog == null) loadCatalog()
            }

            is QuranTopicsEvent.SelectTree -> {
                if (event.tree != _browseState.value.tree) {
                    telemetry.featureUsed(AppAnalytics.Feature.QURAN_TOPICS, "select_tree")
                    _browseState.update { it.copy(tree = event.tree) }
                }
            }

            is QuranTopicsEvent.SetIndexSort -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN_TOPICS, "index_sort")
                _browseState.update { it.copy(indexSort = event.sort) }
            }

            is QuranTopicsEvent.Search -> {
                _browseState.update { it.copy(searchQuery = event.query) }
                queries.value = event.query
            }

            QuranTopicsEvent.ClearSearch -> {
                _browseState.update {
                    it.copy(
                        searchQuery = "",
                        searchResults = emptyList(),
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
     * Debounced so a query is run once the typing settles, not once per keystroke. The search
     * itself is in memory, but it walks 2,512 names and ranks the matches by subtree size — at
     * one per character a fast typist would queue a dozen of those to display the last.
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
                                isSearching = false,
                            )
                        }
                        return@collect
                    }
                    _browseState.update { it.copy(isSearching = true) }
                    // Logged post-debounce, where the search actually runs, so its usage reads
                    // beside `select_tree` rather than as zero.
                    telemetry.search(AppAnalytics.Feature.QURAN_TOPICS, query.trim().length)
                    val index = catalog ?: catalogOrNull()
                    val hits = if (index == null) emptyList() else withContext(computeDispatcher) {
                        index.search(query).map { topic ->
                            val tree = topic.homeTree
                            TopicSearchHit(
                                topic = topic,
                                tree = tree,
                                path = index.path(topic.id, tree),
                                verseCount = index.verseCount(topic.id, tree),
                            )
                        }
                    }
                    _browseState.update { state ->
                        // The query may have been cleared while this was in flight; dropping the
                        // stale result is what keeps a cleared box from repopulating itself.
                        if (state.searchQuery.isBlank()) state
                        else state.copy(searchResults = hits, isSearching = false)
                    }
                }
        }
    }

    /**
     * The whole subject index, counted once for all three tabs.
     *
     * Two queries and one fold, whatever tab is showing — so switching tabs never waits, and
     * every number on every card is the same distinct-verse count the subject screen lists.
     */
    private fun loadCatalog() {
        launchSafely(
            telemetry, AppAnalytics.Feature.QURAN_TOPICS, "load_catalog",
            // The browse screen has no error surface of its own — `isAvailable` already
            // distinguishes "this install has no thematic content" from an empty tab, and a
            // failed read is a third thing. Clearing the spinner resolves it to the unavailable
            // copy, which is at least true of what is on screen.
            onFailure = { _browseState.update { it.copy(isLoading = false, isAvailable = false) } },
        ) {
            val index = catalogOrNull()
            if (index == null) {
                _browseState.update { it.copy(isLoading = false, isAvailable = false) }
                return@launchSafely
            }
            val tabs = withContext(computeDispatcher) { browseTabs(index) }
            _browseState.update {
                it.copy(
                    themes = tabs.themes,
                    kinds = tabs.kinds,
                    index = tabs.index,
                    isLoading = false,
                    isAvailable = true,
                )
            }
        }
    }

    /** The catalogue, loading it on first use. Null when this install has no subject index. */
    private suspend fun catalogOrNull(): TopicCatalog? {
        catalog?.let { return it }
        if (!quranUseCases.hasThematicContent()) return null
        return quranUseCases.getAllTopics.catalog().takeUnless { it.isEmpty }?.also { catalog = it }
    }

    private fun browseTabs(index: TopicCatalog): TopicBrowseState {
        fun tally(tree: TopicTree) = { topic: QuranTopic ->
            TopicTally(
                topic = topic,
                verseCount = index.verseCount(topic.id, tree),
                childCount = index.childCount(topic.id, tree),
            )
        }
        val thematic = tally(TopicTree.THEMATIC)
        return TopicBrowseState(
            themes = index.roots(TopicTree.THEMATIC).map { root ->
                TopicThemeCard(
                    root = thematic(root),
                    branches = index.children(root.id, TopicTree.THEMATIC).map(thematic),
                )
            },
            kinds = index.roots(TopicTree.ONTOLOGY).map(tally(TopicTree.ONTOLOGY)),
            index = index.roots(TopicTree.INDEX)
                .map(tally(TopicTree.INDEX))
                .sortedBy { it.topic.name.lowercase() },
        )
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

            val surahs = quranUseCases.getSurahList().first().associateBy { it.number }
            val groups = detail.citations
                .groupBy { it.surahNumber }
                .map { (surah, citations) ->
                    CitationGroup(
                        surahNumber = surah,
                        surahName = surahs[surah]?.nameEnglish.orEmpty(),
                        citations = citations,
                        isFromSurah = surah == fromSurah,
                        surahArabicName = surahs[surah]?.nameArabic.orEmpty(),
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

            // The children's subtree counts. A branch's own count is usually zero, so without the
            // catalogue "Prophets" listed twenty-five subtopics reading "0 verses" each.
            if (detail.children.isNotEmpty()) {
                val index = catalogOrNull()
                if (index != null && requestedTopicId == topicId) {
                    val subtopics = withContext(computeDispatcher) {
                        index.children(topicId, detail.tree).map {
                            TopicTally(it, index.verseCount(it.id, detail.tree), index.childCount(it.id, detail.tree))
                        }
                    }
                    _detailState.update { state ->
                        if (state.detail?.topic?.id != topicId) state
                        else state.copy(subtopics = subtopics)
                    }
                }
            }

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
