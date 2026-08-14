package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahTopic
import com.arshadshah.nimaz.domain.model.TopicCitation
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.GetSurahListUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicDetailUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicsForSurahUseCase
import com.arshadshah.nimaz.domain.usecase.HasThematicContentUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
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
 * A subject opened from a surah has to keep the surah.
 *
 * "Subjects in this surah" used to navigate to the global browser at the top of the thematic
 * tree — the same twenty roots whichever surah you had been reading — and a subject opened from
 * anywhere listed its citations in Qur'anic order with nothing marking the surah you came from.
 * The reader arrived holding a specific question and every screen answered a general one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranTopicsViewModelSurahContextTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var forSurah: GetTopicsForSurahUseCase
    private lateinit var getDetail: GetTopicDetailUseCase
    private lateinit var hasContent: HasThematicContentUseCase
    private lateinit var settings: SettingsRepository

    private val patience = topic(id = 61, name = "Patience", ayahCount = 103)
    private val theCow = topic(id = 62, name = "The cow", arabic = "البقرة", ayahCount = 7)

    // Al-Baqarah leads with what it is mostly about, not with the busiest subject overall.
    private val baqarahSubjects = listOf(
        SurahTopic(topic = patience, versesInSurah = 12),
        SurahTopic(topic = theCow, versesInSurah = 7),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        forSurah = mockk()
        coEvery { forSurah.invoke(2) } returns baqarahSubjects
        coEvery { forSurah.invoke(1) } returns emptyList()

        getDetail = mockk()
        coEvery { getDetail.invoke(any(), any()) } returns detailOf(patience)

        hasContent = mockk()
        coEvery { hasContent.invoke() } returns true

        val surahs = mockk<GetSurahListUseCase>()
        every { surahs.invoke() } returns flowOf(
            listOf(surah(1, "The Opening"), surah(2, "The Cow"), surah(3, "The Family of Imran")),
        )

        settings = mockk(relaxed = true)
        every { settings.quranTranslatorId } returns MutableStateFlow("sahih_international")

        useCases = mockk(relaxed = true)
        every { useCases.getTopicsForSurah } returns forSurah
        every { useCases.getTopicDetail } returns getDetail
        every { useCases.getSurahList } returns surahs
        every { useCases.hasThematicContent } returns hasContent
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a surah's subjects arrive with the surah's own name, weightiest here first`() = runTest {
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(2))
        advanceUntilIdle()

        val state = vm.surahSubjects.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.surahName).isEqualTo("The Cow")
        assertThat(state.subjects.map { it.topic.name })
            .containsExactly("Patience", "The cow").inOrder()
        assertThat(state.citations).isEqualTo(19)
    }

    @Test
    fun `re-sending the same surah does not re-query`() = runTest {
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(2))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(2))
        advanceUntilIdle()

        coVerify(exactly = 1) { forSurah.invoke(2) }
    }

    /** The list is already in memory, so filtering is a predicate and not a second query. */
    @Test
    fun `filtering narrows what is loaded without going back to the corpus`() = runTest {
        val vm = viewModel()
        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(2))
        advanceUntilIdle()

        vm.onEvent(QuranTopicsEvent.FilterSurahSubjects("cow"))
        advanceUntilIdle()

        assertThat(vm.surahSubjects.value.visible.map { it.topic.name })
            .containsExactly("The cow")
        // Still the whole list underneath — the filter is a view, not a reload.
        assertThat(vm.surahSubjects.value.subjects).hasSize(2)
        coVerify(exactly = 1) { forSurah.invoke(2) }

        vm.onEvent(QuranTopicsEvent.ClearSurahSubjectsFilter)
        advanceUntilIdle()
        assertThat(vm.surahSubjects.value.visible).hasSize(2)
    }

    @Test
    fun `the filter matches the Arabic name too`() = runTest {
        val vm = viewModel()
        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(2))
        advanceUntilIdle()

        vm.onEvent(QuranTopicsEvent.FilterSurahSubjects("البقرة"))
        advanceUntilIdle()

        assertThat(vm.surahSubjects.value.visible.map { it.topic.id }).containsExactly(62)
    }

    /**
     * The one deliberate departure from Qur'anic order: the surah the reader came from is
     * lifted to the front. Everything else keeps the sequence the corpus gives.
     */
    @Test
    fun `the surah you came from is pinned to the top of the citations`() = runTest {
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadDetail(61, TopicTree.THEMATIC, fromSurah = 3))
        advanceUntilIdle()

        val state = vm.detailState.value
        assertThat(state.citationGroups.map { it.surahNumber })
            .containsExactly(3, 1, 2).inOrder()
        assertThat(state.citationGroups.first().isFromSurah).isTrue()
        assertThat(state.citationGroups.drop(1).none { it.isFromSurah }).isTrue()
    }

    @Test
    fun `the surah context carries that surah's own share of the subject`() = runTest {
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadDetail(61, TopicTree.THEMATIC, fromSurah = 2))
        advanceUntilIdle()

        val context = vm.detailState.value.surahContext
        assertThat(context?.surahNumber).isEqualTo(2)
        assertThat(context?.surahName).isEqualTo("The Cow")
        assertThat(context?.verseCount).isEqualTo(2)
    }

    /** A subject that never reaches the surah gets no context line rather than a zero. */
    @Test
    fun `a surah the subject does not touch produces no context`() = runTest {
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadDetail(61, TopicTree.THEMATIC, fromSurah = 114))
        advanceUntilIdle()

        assertThat(vm.detailState.value.surahContext).isNull()
        assertThat(vm.detailState.value.citationGroups.map { it.surahNumber })
            .containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `without a surah the citations keep the corpus order and nothing is marked`() = runTest {
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadDetail(61, TopicTree.THEMATIC))
        advanceUntilIdle()

        val state = vm.detailState.value
        assertThat(state.surahContext).isNull()
        assertThat(state.citationGroups.map { it.surahNumber })
            .containsExactly(1, 2, 3).inOrder()
        assertThat(state.citationGroups.none { it.isFromSurah }).isTrue()
    }

    /**
     * An empty list is a sentence about the install far more often than about the surah, and
     * the screen has to be able to tell which it is saying.
     */
    @Test
    fun `an empty list separates a missing index from a surah with nothing in it`() = runTest {
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(1))
        advanceUntilIdle()
        assertThat(vm.surahSubjects.value.subjects).isEmpty()
        assertThat(vm.surahSubjects.value.isAvailable).isTrue()

        coEvery { hasContent.invoke() } returns false
        val stale = viewModel()
        stale.onEvent(QuranTopicsEvent.LoadSurahSubjects(1))
        advanceUntilIdle()
        assertThat(stale.surahSubjects.value.isAvailable).isFalse()
    }

    /** A surah with rows never pays for the availability check — the rows are the answer. */
    @Test
    fun `a surah with subjects does not ask whether the layer exists`() = runTest {
        val vm = viewModel()

        vm.onEvent(QuranTopicsEvent.LoadSurahSubjects(2))
        advanceUntilIdle()

        assertThat(vm.surahSubjects.value.isAvailable).isTrue()
        coVerify(exactly = 0) { hasContent.invoke() }
    }

    /**
     * A surah's subjects come from all three hierarchies, so the tree a row opens in has to be
     * the one that actually places it — otherwise the subjects the thematic outline does not
     * carry open with no breadcrumb and no subtopics.
     */
    @Test
    fun `a subject opens in the hierarchy that places it`() {
        assertThat(patience.homeTree).isEqualTo(TopicTree.THEMATIC)
        assertThat(
            topic(id = 7, name = "Egypt", thematic = false, ontology = true).homeTree
        ).isEqualTo(TopicTree.ONTOLOGY)
        assertThat(
            topic(id = 8, name = "Ants", thematic = false, ontology = false).homeTree
        ).isEqualTo(TopicTree.INDEX)
    }

    private fun viewModel() = QuranTopicsViewModel(useCases, RollUpTopicCounts(), settings, RecordingTelemetry())

    /** Citations across three surahs, in the order the corpus gives them — by ayah id. */
    private fun detailOf(topic: QuranTopic) = TopicDetail(
        topic = topic,
        tree = TopicTree.THEMATIC,
        breadcrumb = emptyList(),
        children = emptyList(),
        related = emptyList(),
        citations = listOf(
            TopicCitation(ayahId = 5, surahNumber = 1, ayahNumber = 5),
            TopicCitation(ayahId = 160, surahNumber = 2, ayahNumber = 153),
            TopicCitation(ayahId = 184, surahNumber = 2, ayahNumber = 177),
            TopicCitation(ayahId = 500, surahNumber = 3, ayahNumber = 200),
        ),
    )

    private fun surah(number: Int, english: String) = Surah(
        number = number,
        nameArabic = "",
        nameEnglish = english,
        nameTransliteration = english,
        revelationType = RevelationType.MEDINAN,
        ayahCount = 7,
        orderInMushaf = number,
    )

    private fun topic(
        id: Int,
        name: String,
        arabic: String = "",
        ayahCount: Int = 12,
        thematic: Boolean = true,
        ontology: Boolean = false,
    ) = QuranTopic(
        id = id,
        name = name,
        arabicName = arabic,
        description = "",
        wikiLink = "",
        ayahCount = ayahCount,
        parentId = null,
        thematicParentId = null,
        ontologyParentId = null,
        isThematic = thematic,
        isOntology = ontology,
        relatedTopicIds = emptyList(),
    )
}
