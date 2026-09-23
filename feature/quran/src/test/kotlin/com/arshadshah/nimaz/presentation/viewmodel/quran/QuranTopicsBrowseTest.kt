package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TopicCatalog
import com.arshadshah.nimaz.domain.model.TopicCitation
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.GetAllTopicsUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicDetailUseCase
import com.arshadshah.nimaz.domain.usecase.HasThematicContentUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The Topics front page: three tabs counted from one catalogue, and a search over it.
 *
 * The properties worth pinning are the ones the redesign exists for. Every tab is built at load,
 * so switching tabs is a state change and never a query. Every number is a **distinct-verse**
 * count through the whole branch, the same figure the subject screen lists. An install without
 * the subject index says so instead of drawing empty cards. And a search result arrives placed —
 * with the hierarchy it opens in and the path to it — rather than as a bare word.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranTopicsBrowseTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var allTopics: GetAllTopicsUseCase
    private lateinit var hasContent: HasThematicContentUseCase
    private lateinit var getDetail: GetTopicDetailUseCase
    private lateinit var settings: SettingsRepository

    //  Themes:  Stories (1) ─ Prophets (11) ─ Moses (111)
    //                       └ Nations (12)
    //  Kinds:   Living Creation (2) ─ Moses (111)
    //  Index:   Moses (111), Patience (3), ʿĀd (4), ٩٩ (5)
    private val stories = topic(1, "Stories", thematic = true)
    private val prophets = topic(11, "Prophets", thematic = true, thematicParent = 1, indexParent = 1)
    private val nations = topic(12, "Nations", thematic = true, thematicParent = 1, indexParent = 1)
    private val moses = topic(111, "Moses", thematic = true, ontology = true, thematicParent = 11, ontologyParent = 2)
    private val living = topic(2, "Living Creation", ontology = true, indexParent = 1)
    private val patience = topic(3, "Patience")
    private val ad = topic(4, "ʿĀd")
    private val ninetyNine = topic(5, "٩٩")

    private val catalog = TopicCatalog(
        listOf(stories, prophets, nations, moses, living, patience, ad, ninetyNine),
        mapOf(
            111 to listOf(10, 11, 12, 13),
            12 to listOf(13, 14), // 13 is Moses' too: Stories has 5 distinct verses, not 6
            3 to listOf(20, 21, 22),
            4 to listOf(30),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        allTopics = mockk()
        coEvery { allTopics.catalog() } returns catalog

        hasContent = mockk()
        coEvery { hasContent.invoke() } returns true

        getDetail = mockk()
        coEvery { getDetail.invoke(any(), any()) } returns null

        settings = mockk(relaxed = true)
        every { settings.quranTranslatorId } returns MutableStateFlow("sahih_international")

        useCases = mockk(relaxed = true)
        every { useCases.getAllTopics } returns allTopics
        every { useCases.hasThematicContent } returns hasContent
        every { useCases.getTopicDetail } returns getDetail
        every { useCases.getSurahList() } returns flowOf(
            listOf(
                Surah(20, "طه", "Ta-Ha", "Ta-Ha", RevelationType.MECCAN, 135, 20, 312),
            )
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = QuranTopicsViewModel(useCases, settings, RecordingTelemetry(), dispatcher)

    private fun opened(): QuranTopicsViewModel = viewModel().also {
        it.onEvent(QuranTopicsEvent.OpenBrowser)
        dispatcher.scheduler.advanceUntilIdle()
    }

    // ---- The three tabs ----

    @Test
    fun `themes are the outline's roots, each with its branches biggest first`() = runTest {
        val themes = opened().browseState.value.themes

        assertThat(themes.map { it.root.topic.id }).containsExactly(1)
        assertThat(themes.single().branches.map { it.topic.id }).containsExactly(11, 12).inOrder()
    }

    @Test
    fun `a card counts distinct verses through its whole branch`() = runTest {
        val card = opened().browseState.value.themes.single()

        assertThat(card.root.verseCount).isEqualTo(5)
        assertThat(card.branches.first { it.topic.id == 11 }.verseCount).isEqualTo(4)
    }

    @Test
    fun `kinds are the ontology's roots`() = runTest {
        val kinds = opened().browseState.value.kinds

        assertThat(kinds.map { it.topic.id }).containsExactly(2)
        assertThat(kinds.single().verseCount).isEqualTo(4)
    }

    @Test
    fun `the index lists its top-level entries alphabetically, with their sub-entries counted`() = runTest {
        val index = opened().browseState.value.index

        assertThat(index.map { it.topic.name }).isInOrder(compareBy<String> { it.lowercase() })
        assertThat(index.first { it.topic.id == 1 }.childCount).isEqualTo(3)
    }

    @Test
    fun `switching tab is a state change, not another load`() = runTest {
        val vm = opened()

        vm.onEvent(QuranTopicsEvent.SelectTree(TopicTree.ONTOLOGY))
        advanceUntilIdle()

        assertThat(vm.browseState.value.tree).isEqualTo(TopicTree.ONTOLOGY)
        coVerify(exactly = 1) { allTopics.catalog() }
    }

    @Test
    fun `reopening the browser does not rebuild the catalogue`() = runTest {
        val vm = opened()

        vm.onEvent(QuranTopicsEvent.OpenBrowser)
        advanceUntilIdle()

        coVerify(exactly = 1) { allTopics.catalog() }
    }

    // ---- The index ----

    @Test
    fun `accented names file under their letter, and names with no Latin letter file last`() = runTest {
        val sections = opened().browseState.value.indexSections

        assertThat(sections.first { it.letter == 'A' }.entries.map { it.topic.id }).contains(4)
        assertThat(sections.last().letter).isEqualTo(TopicBrowseState.OTHER_LETTER)
        assertThat(sections.last().entries.map { it.topic.id }).containsExactly(5)
    }

    @Test
    fun `most cited puts the heaviest entries first`() = runTest {
        val vm = opened()

        vm.onEvent(QuranTopicsEvent.SetIndexSort(TopicIndexSort.MOST_CITED))
        advanceUntilIdle()

        val state = vm.browseState.value
        assertThat(state.indexSort).isEqualTo(TopicIndexSort.MOST_CITED)
        assertThat(state.mostCited.first().topic.id).isEqualTo(111)
    }

    // ---- Availability ----

    @Test
    fun `an install with no subject index says so rather than drawing empty cards`() = runTest {
        coEvery { hasContent.invoke() } returns false

        val state = opened().browseState.value

        assertThat(state.isAvailable).isFalse()
        assertThat(state.isLoading).isFalse()
        coVerify(exactly = 0) { allTopics.catalog() }
    }

    @Test
    fun `a read that fails clears the spinner instead of leaving it turning`() = runTest {
        coEvery { allTopics.catalog() } throws IllegalStateException("content database missing")

        assertThat(opened().browseState.value.isLoading).isFalse()
    }

    // ---- Search ----

    @Test
    fun `a result carries the hierarchy it opens in, its path there, and its count`() = runTest {
        val vm = opened()

        vm.onEvent(QuranTopicsEvent.Search("moses"))
        advanceUntilIdle()

        val hit = vm.browseState.value.searchResults.single()
        assertThat(hit.topic.id).isEqualTo(111)
        assertThat(hit.tree).isEqualTo(TopicTree.THEMATIC)
        assertThat(hit.path.map { it.id }).containsExactly(1, 11).inOrder()
        assertThat(hit.verseCount).isEqualTo(4)
    }

    @Test
    fun `clearing the search empties the results as well as the box`() = runTest {
        val vm = opened()
        vm.onEvent(QuranTopicsEvent.Search("patience"))
        advanceUntilIdle()
        assertThat(vm.browseState.value.searchResults).isNotEmpty()

        vm.onEvent(QuranTopicsEvent.ClearSearch)
        advanceUntilIdle()

        val state = vm.browseState.value
        assertThat(state.searchQuery).isEmpty()
        assertThat(state.searchResults).isEmpty()
        assertThat(state.isSearchMode).isFalse()
    }

    // ---- The subject screen's subtopics ----

    @Test
    fun `subtopics arrive counted through their own branches, biggest first`() = runTest {
        coEvery { getDetail.invoke(1, TopicTree.THEMATIC) } returns TopicDetail(
            topic = stories,
            tree = TopicTree.THEMATIC,
            breadcrumb = emptyList(),
            children = listOf(nations, prophets),
            related = emptyList(),
            citations = (10..14).map { TopicCitation(it, 20, it) },
        )
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadDetail(1, TopicTree.THEMATIC))
        advanceUntilIdle()

        val state = vm.detailState.value
        assertThat(state.subtopics.map { it.topic.id to it.verseCount })
            .containsExactly(11 to 4, 12 to 2).inOrder()
        assertThat(state.citationGroups.single().surahArabicName).isEqualTo("طه")
    }

    @Test
    fun `where it appears needs more than one surah to say anything`() {
        val one = CitationGroup(20, "Ta-Ha", listOf(TopicCitation(1, 20, 1)))
        val two = CitationGroup(7, "Al-A'raf", listOf(TopicCitation(2, 7, 1), TopicCitation(3, 7, 2)))

        assertThat(TopicDetailState(citationGroups = listOf(one)).topSurahs).isEmpty()
        assertThat(TopicDetailState(citationGroups = listOf(one, two)).topSurahs.map { it.surahNumber })
            .containsExactly(7, 20).inOrder()
    }

    private fun topic(
        id: Int,
        name: String,
        thematic: Boolean = false,
        ontology: Boolean = false,
        thematicParent: Int? = null,
        ontologyParent: Int? = null,
        indexParent: Int? = null,
    ) = QuranTopic(
        id = id,
        name = name,
        arabicName = "",
        description = "",
        wikiLink = "",
        ayahCount = 0,
        parentId = indexParent,
        thematicParentId = thematicParent,
        ontologyParentId = ontologyParent,
        isThematic = thematic,
        isOntology = ontology,
        relatedTopicIds = emptyList(),
    )
}
