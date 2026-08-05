package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TopicCitation
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.GetSurahListUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicChildrenUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicDetailUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicTreeRootsUseCase
import com.arshadshah.nimaz.domain.usecase.HasThematicContentUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.SearchTopicsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
 * Two subject-browser requests in flight, resolving out of order.
 *
 * `QuranTopicsViewModel` is fully fakeable and already had 21 tests — and **none of them
 * covered this**, which is exactly #364's point about it: the tests that exist are the ones the
 * happy path suggests, and a race is not on the happy path. Every fake here can be held open,
 * so "the slower one lands second" is stated rather than hoped for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranTopicsRaceTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var settings: SettingsRepository

    //  Doctrine ─ God ─ The names of God
    //           └ Mercy
    private val doctrine = topic(id = 1, name = "Doctrine")
    private val god = topic(id = 11, name = "God", parent = 1)
    private val mercy = topic(id = 13, name = "Mercy", parent = 1)
    private val names = topic(id = 111, name = "The names of God", parent = 11)

    private val children = mapOf(
        1 to listOf(god, mercy),
        11 to listOf(names),
        13 to listOf(topic(id = 131, name = "Forgiveness", parent = 13)),
        111 to listOf(topic(id = 1111, name = "The Merciful", parent = 111)),
    )

    /** Held open so a test decides when a given topic's children resolve. */
    private val childGates = mutableMapOf<Int, CompletableDeferred<Unit>>()

    /** Held open so a test decides when a given topic's detail resolves. */
    private val detailGates = mutableMapOf<Int, CompletableDeferred<Unit>>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        val getChildren = mockk<GetTopicChildrenUseCase>()
        coEvery { getChildren.invoke(any(), any()) } coAnswers {
            val id = firstArg<Int>()
            childGates[id]?.await()
            children[id].orEmpty()
        }
        coEvery { getChildren.branchesIn(any()) } returns children.keys

        val getDetail = mockk<GetTopicDetailUseCase>()
        coEvery { getDetail.invoke(any(), any()) } coAnswers {
            val id = firstArg<Int>()
            detailGates[id]?.await()
            TopicDetail(
                topic = children.values.flatten().plus(doctrine).first { it.id == id },
                tree = TopicTree.THEMATIC,
                breadcrumb = emptyList(),
                children = emptyList(),
                related = emptyList(),
                citations = listOf(citation(id)),
            )
        }

        val getRoots = mockk<GetTopicTreeRootsUseCase>()
        coEvery { getRoots.invoke(any()) } returns listOf(doctrine)

        val hasContent = mockk<HasThematicContentUseCase>()
        coEvery { hasContent.invoke() } returns true

        val search = mockk<SearchTopicsUseCase>()
        coEvery { search.invoke(any(), any()) } returns emptyList()
        coEvery { search.pathsFor(any(), any()) } returns emptyMap()

        settings = mockk(relaxed = true)
        every { settings.quranTranslatorId } returns MutableStateFlow("sahih_international")

        // `loadDetail` names each citation's surah from this list. A relaxed mock hands back a
        // Flow that emits nothing, on which its `.first()` throws — and the throw lands before
        // the state write, so the symptom is an empty detail rather than an error.
        val getSurahList = mockk<GetSurahListUseCase>()
        every { getSurahList.invoke() } returns flowOf(
            listOf(
                Surah(
                    number = 1,
                    nameArabic = "الفاتحة",
                    nameEnglish = "The Opening",
                    nameTransliteration = "Al-Fatihah",
                    revelationType = RevelationType.MECCAN,
                    ayahCount = 7,
                    juzStart = 1,
                    orderInMushaf = 1,
                    startPage = 1,
                ),
            ),
        )

        useCases = mockk(relaxed = true)
        every { useCases.getSurahList } returns getSurahList
        every { useCases.getTopicChildren } returns getChildren
        every { useCases.getTopicDetail } returns getDetail
        every { useCases.getTopicTreeRoots } returns getRoots
        every { useCases.hasThematicContent } returns hasContent
        every { useCases.searchTopics } returns search
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a slower crumb tap cannot land on top of a later one`() = runTest {
        val vm = openedBrowser()

        // Descend twice so there are crumbs to tap: Doctrine › God › The names of God.
        vm.onEvent(QuranTopicsEvent.Focus(god))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Focus(names))
        advanceUntilIdle()

        // Crumb 0 (Doctrine) is on an uncached branch and slow; crumb 1 (God) answers at once.
        val slow = CompletableDeferred<Unit>()
        childGates[1] = slow

        vm.onEvent(QuranTopicsEvent.RebaseTo(0))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.RebaseTo(1))
        advanceUntilIdle()

        slow.complete(Unit)
        advanceUntilIdle()

        // Each of these ends in a whole-state update setting `focus` *and* `level` together, so
        // the slower one landing second put the reader on the crumb they did not tap.
        assertThat(vm.browseState.value.focus.map { it.id }).containsExactly(1, 11).inOrder()
    }

    @Test
    fun `a toggle that resolves after a rebase does not reopen its node`() = runTest {
        val vm = openedBrowser()

        val slow = CompletableDeferred<Unit>()
        childGates[13] = slow

        vm.onEvent(QuranTopicsEvent.Toggle(mercy))
        advanceUntilIdle()

        // The reader gives up waiting and descends instead. `focus` resets `expanded`.
        vm.onEvent(QuranTopicsEvent.Focus(god))
        advanceUntilIdle()

        slow.complete(Unit)
        advanceUntilIdle()

        // Mercy is not on screen at this level, so re-opening it would restore a node the
        // reader had navigated away from. The children are still cached, because caching them
        // is valid wherever the browser happens to be.
        assertThat(vm.browseState.value.expanded).doesNotContain(13)
        assertThat(vm.browseState.value.children).containsKey(13)
    }

    @Test
    fun `two toggles do not cancel each other`() = runTest {
        val vm = openedBrowser()

        val slow = CompletableDeferred<Unit>()
        childGates[11] = slow

        vm.onEvent(QuranTopicsEvent.Toggle(god))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Toggle(mercy))
        advanceUntilIdle()

        slow.complete(Unit)
        advanceUntilIdle()

        // Opening two rows is two independent intentions — a shared cancel-and-replace handle
        // here would have thrown away the first row's children.
        assertThat(vm.browseState.value.expanded).containsAtLeast(11, 13)
    }

    @Test
    fun `a slower detail load cannot replace a later one`() = runTest {
        val vm = openedBrowser()

        val slow = CompletableDeferred<Unit>()
        detailGates[11] = slow

        vm.onEvent(QuranTopicsEvent.LoadDetail(11, TopicTree.THEMATIC))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.LoadDetail(13, TopicTree.THEMATIC))
        advanceUntilIdle()

        slow.complete(Unit)
        advanceUntilIdle()

        // `_detailState.value = TopicDetailState(...)` is a whole-object assign, so the loser
        // of this race wiped the winner's detail *and* any previews already landed for it —
        // walking past the staleness guard the same function applies to its previews.
        assertThat(vm.detailState.value.detail?.topic?.id).isEqualTo(13)
    }

    @Test
    fun `an uncontested detail load still resolves`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.LoadDetail(11, TopicTree.THEMATIC))
        advanceUntilIdle()

        assertThat(vm.detailState.value.detail?.topic?.id).isEqualTo(11)
        assertThat(vm.detailState.value.isLoading).isFalse()
    }

    private fun openedBrowser(): QuranTopicsViewModel {
        val vm = QuranTopicsViewModel(useCases, settings)
        vm.onEvent(QuranTopicsEvent.OpenBrowser)
        dispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    private fun citation(topicId: Int) = TopicCitation(
        ayahId = topicId,
        surahNumber = 1,
        ayahNumber = 1,
    )

    private fun topic(id: Int, name: String, parent: Int? = null) = QuranTopic(
        id = id,
        name = name,
        arabicName = "",
        description = "",
        wikiLink = "",
        ayahCount = 12,
        parentId = parent,
        thematicParentId = parent,
        ontologyParentId = null,
        isThematic = true,
        isOntology = false,
        relatedTopicIds = emptyList(),
    )
}
