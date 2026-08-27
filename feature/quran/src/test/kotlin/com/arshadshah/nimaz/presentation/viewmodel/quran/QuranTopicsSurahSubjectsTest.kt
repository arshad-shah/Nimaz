package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.GetTopicChildrenUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicDetailUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicTreeRootsUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicsForSurahUseCase
import com.arshadshah.nimaz.domain.usecase.HasThematicContentUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.SearchTopicsUseCase
import com.arshadshah.nimaz.domain.usecase.quran.RollUpTopicCounts
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
 * The subject browser's other three jobs: the tree switch, the search box, and one surah's
 * subject list.
 *
 * The distinction the whole feature turns on is "this install has no subject index" versus "this
 * surah has no subjects". They are one empty list and two completely different sentences, and
 * the ViewModel only asks the expensive question when the list comes back empty — so the answer
 * has to be right in both directions.
 *
 * Switching trees is the other one. A different hierarchy is a different set of parents, so
 * nothing may carry over: not the focus, not what was open, and above all not the cached
 * children, which are keyed by a parent id that means something else in the other tree.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranTopicsSurahSubjectsTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var useCases: QuranUseCases
    private lateinit var getChildren: GetTopicChildrenUseCase
    private lateinit var getRoots: GetTopicTreeRootsUseCase
    private lateinit var hasContent: HasThematicContentUseCase
    private lateinit var forSurah: GetTopicsForSurahUseCase
    private lateinit var settings: SettingsRepository

    private val doctrine = topic(1, "Doctrine")
    private val law = topic(2, "Law")

    private val surahs = listOf(
        Surah(
            number = 18,
            nameArabic = "الكهف",
            nameEnglish = "The Cave",
            nameTransliteration = "Al-Kahf",
            revelationType = RevelationType.MECCAN,
            ayahCount = 110,
            orderInMushaf = 18,
            startPage = 293,
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        getChildren = mockk()
        coEvery { getChildren.invoke(any(), any()) } returns emptyList()
        coEvery { getChildren.branchesIn(any()) } returns emptySet()

        getRoots = mockk()
        coEvery { getRoots.invoke(TopicTree.THEMATIC) } returns listOf(doctrine)
        coEvery { getRoots.invoke(TopicTree.ONTOLOGY) } returns listOf(law)

        hasContent = mockk()
        coEvery { hasContent.invoke() } returns true

        forSurah = mockk()
        coEvery { forSurah.invoke(any()) } returns emptyList()

        val getDetail = mockk<GetTopicDetailUseCase>()
        coEvery { getDetail.invoke(any(), any()) } returns null

        val search = mockk<SearchTopicsUseCase>()
        coEvery { search.invoke(any(), any()) } returns listOf(doctrine)
        coEvery { search.pathsFor(any(), any()) } returns emptyMap()

        settings = mockk(relaxed = true)
        every { settings.quranTranslatorId } returns MutableStateFlow("sahih_international")

        useCases = mockk(relaxed = true)
        every { useCases.getTopicChildren } returns getChildren
        every { useCases.getTopicTreeRoots } returns getRoots
        every { useCases.hasThematicContent } returns hasContent
        every { useCases.getTopicsForSurah } returns forSurah
        every { useCases.getTopicDetail } returns getDetail
        every { useCases.searchTopics } returns search
        every { useCases.getSurahList() } returns flowOf(surahs)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        QuranTopicsViewModel(useCases, RollUpTopicCounts(), settings, telemetry)

    private fun opened(): QuranTopicsViewModel = viewModel().also {
        it.onEvent(QuranTopicsEvent.OpenBrowser)
        dispatcher.scheduler.advanceUntilIdle()
    }

    // ---- Opening the browser ----

    @Test
    fun `an install with no subject index says so rather than showing an empty tree`() = runTest {
        coEvery { hasContent.invoke() } returns false

        val vm = opened()

        assertThat(vm.browseState.value.isAvailable).isFalse()
        assertThat(vm.browseState.value.isLoading).isFalse()
        coVerify(exactly = 0) { getRoots.invoke(any()) }
    }

    @Test
    fun `a read that fails clears the spinner instead of leaving it turning`() = runTest {
        coEvery { hasContent.invoke() } throws IllegalStateException("content database missing")

        val vm = opened()

        assertThat(vm.browseState.value.isLoading).isFalse()
    }

    @Test
    fun `reopening the browser does not reload a tree that is already there`() = runTest {
        val vm = opened()

        vm.onEvent(QuranTopicsEvent.OpenBrowser)
        advanceUntilIdle()

        coVerify(exactly = 1) { getRoots.invoke(TopicTree.THEMATIC) }
    }

    // ---- Switching hierarchy ----

    @Test
    fun `switching tree loads the other hierarchy's roots`() = runTest {
        val vm = opened()

        vm.onEvent(QuranTopicsEvent.SelectTree(TopicTree.ONTOLOGY))
        advanceUntilIdle()

        assertThat(vm.browseState.value.tree).isEqualTo(TopicTree.ONTOLOGY)
        assertThat(vm.browseState.value.rows.map { it.topic.id }).containsExactly(2)
    }

    @Test
    fun `switching tree drops the cached children keyed by the old tree's ids`() = runTest {
        coEvery { getChildren.invoke(1, TopicTree.THEMATIC) } returns listOf(topic(11, "God", 1))
        coEvery { getChildren.branchesIn(TopicTree.THEMATIC) } returns setOf(1)
        val vm = opened()
        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()
        assertThat(vm.browseState.value.children).isNotEmpty()

        vm.onEvent(QuranTopicsEvent.SelectTree(TopicTree.ONTOLOGY))
        advanceUntilIdle()

        val state = vm.browseState.value
        assertThat(state.children).isEmpty()
        assertThat(state.expanded).isEmpty()
        assertThat(state.focus).isEmpty()
    }

    @Test
    fun `selecting the tree already showing does nothing`() = runTest {
        val vm = opened()

        vm.onEvent(QuranTopicsEvent.SelectTree(TopicTree.THEMATIC))
        advanceUntilIdle()

        coVerify(exactly = 1) { getRoots.invoke(TopicTree.THEMATIC) }
    }

    // ---- The search box ----

    @Test
    fun `clearing the search empties the results as well as the box`() = runTest {
        val vm = opened()
        vm.onEvent(QuranTopicsEvent.Search("god"))
        advanceUntilIdle()
        assertThat(vm.browseState.value.searchResults).isNotEmpty()

        vm.onEvent(QuranTopicsEvent.ClearSearch)
        advanceUntilIdle()

        val state = vm.browseState.value
        assertThat(state.searchQuery).isEmpty()
        assertThat(state.searchResults).isEmpty()
        assertThat(state.isSearching).isFalse()
        assertThat(state.isSearchMode).isFalse()
    }

    // ---- One surah's subjects ----

    @Test
    fun `a surah's subjects arrive with the surah's name`() = runTest {
        coEvery { forSurah.invoke(18) } returns listOf(SurahTopic(doctrine, versesInSurah = 4))
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()

        val state = vm.surahSubjects.value
        assertThat(state.surahName).isEqualTo("The Cave")
        assertThat(state.subjects.map { it.topic.id }).containsExactly(1)
        assertThat(state.isAvailable).isTrue()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `a surah with subjects never pays for the availability question`() = runTest {
        coEvery { forSurah.invoke(18) } returns listOf(SurahTopic(doctrine, versesInSurah = 4))
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()

        coVerify(exactly = 0) { hasContent.invoke() }
    }

    @Test
    fun `an empty list on an install that has the index means this surah has none`() = runTest {
        coEvery { forSurah.invoke(18) } returns emptyList()
        coEvery { hasContent.invoke() } returns true
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()

        assertThat(vm.surahSubjects.value.subjects).isEmpty()
        assertThat(vm.surahSubjects.value.isAvailable).isTrue()
    }

    @Test
    fun `an empty list on an install without the index means the content predates it`() = runTest {
        coEvery { forSurah.invoke(18) } returns emptyList()
        coEvery { hasContent.invoke() } returns false
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()

        assertThat(vm.surahSubjects.value.isAvailable).isFalse()
    }

    @Test
    fun `asking for the surah already loaded does not re-run the query`() = runTest {
        coEvery { forSurah.invoke(18) } returns listOf(SurahTopic(doctrine, versesInSurah = 4))
        val vm = viewModel()
        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()

        coVerify(exactly = 1) { forSurah.invoke(18) }
    }

    @Test
    fun `a failed read clears the surah subjects spinner`() = runTest {
        coEvery { forSurah.invoke(18) } throws IllegalStateException("content database missing")
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()

        assertThat(vm.surahSubjects.value.isLoading).isFalse()
    }

    @Test
    fun `the subject filter is applied in memory and runs no query`() = runTest {
        coEvery { forSurah.invoke(18) } returns listOf(
            SurahTopic(doctrine, versesInSurah = 4),
            SurahTopic(law, versesInSurah = 2),
        )
        val vm = viewModel()
        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()

        vm.onEvent(QuranTopicsEvent.FilterSurahSubjects("law"))
        advanceUntilIdle()

        assertThat(vm.surahSubjects.value.query).isEqualTo("law")
        coVerify(exactly = 1) { forSurah.invoke(18) }
    }

    @Test
    fun `clearing the subject filter empties the box`() = runTest {
        val vm = viewModel()
        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(18))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.FilterSurahSubjects("law"))
        advanceUntilIdle()

        vm.onEvent(QuranTopicsEvent.ClearSurahSubjectsFilter)
        advanceUntilIdle()

        assertThat(vm.surahSubjects.value.query).isEmpty()
    }

    private fun topic(id: Int, name: String, parent: Int? = null) = QuranTopic(
        id = id,
        name = name,
        arabicName = "",
        description = "",
        wikiLink = "",
        ayahCount = 12,
        parentId = parent,
        thematicParentId = parent,
        ontologyParentId = parent,
        isThematic = true,
        isOntology = true,
        relatedTopicIds = emptyList(),
    )
}
