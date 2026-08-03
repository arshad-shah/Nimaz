package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.usecase.GetTopicChildrenUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicDetailUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicTreeRootsUseCase
import com.arshadshah.nimaz.domain.usecase.HasThematicContentUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Descent in [QuranTopicsViewModel] must be honest about leaves, and cheap about branches.
 *
 * Most of the 2,512 subjects have no children. Descending into one used to load the whole
 * [com.arshadshah.nimaz.domain.model.TopicDetail] — breadcrumb, related subjects and every
 * citation, 153 of them for "Allah" — to discover there was nowhere to go, and then silently
 * do nothing. The screen offered no other target on the row, so the tap was simply dead.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranTopicsViewModelDescentTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var getChildren: GetTopicChildrenUseCase
    private lateinit var getDetail: GetTopicDetailUseCase

    private val doctrine = topic(id = 1, name = "Doctrine")
    private val god = topic(id = 11, name = "God")
    private val mercy = topic(id = 13, name = "Mercy")

    /** Doctrine has children; Mercy is a leaf. */
    private val children = mapOf(
        1 to listOf(god, mercy),
        11 to emptyList(),
        13 to emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        getChildren = mockk()
        coEvery { getChildren.invoke(any(), any()) } answers {
            children[firstArg<Int>()].orEmpty()
        }
        coEvery { getChildren.branchesIn(any()) } returns setOf(1)

        getDetail = mockk()
        coEvery { getDetail.invoke(any(), any()) } returns null

        val getRoots = mockk<GetTopicTreeRootsUseCase>()
        coEvery { getRoots.invoke(any()) } returns listOf(doctrine)

        val hasContent = mockk<HasThematicContentUseCase>()
        coEvery { hasContent.invoke() } returns true

        useCases = mockk(relaxed = true)
        every { useCases.getTopicChildren } returns getChildren
        every { useCases.getTopicDetail } returns getDetail
        every { useCases.getTopicTreeRoots } returns getRoots
        every { useCases.hasThematicContent } returns hasContent
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `descending a childless topic leaves the path where it was`() = runTest {
        val vm = QuranTopicsViewModel(useCases)

        vm.onEvent(QuranTopicsEvent.OpenBrowser)
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Descend(doctrine))
        advanceUntilIdle()

        assertThat(vm.browseState.value.path.map { it.id }).containsExactly(1)

        vm.onEvent(QuranTopicsEvent.Descend(mercy))
        advanceUntilIdle()

        assertThat(vm.browseState.value.path.map { it.id }).containsExactly(1)
        assertThat(vm.browseState.value.topics.map { it.id }).containsExactly(11, 13).inOrder()
        assertThat(vm.browseState.value.isLoading).isFalse()
    }

    @Test
    fun `descent asks for children, not for the whole topic detail`() = runTest {
        val vm = QuranTopicsViewModel(useCases)

        vm.onEvent(QuranTopicsEvent.OpenBrowser)
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Descend(doctrine))
        advanceUntilIdle()

        coVerify { getChildren.invoke(1, TopicTree.THEMATIC) }
        coVerify(exactly = 0) { getDetail.invoke(any(), any()) }
    }

    @Test
    fun `a leaf is not offered a disclosure control, and a branch is`() = runTest {
        val vm = QuranTopicsViewModel(useCases)

        vm.onEvent(QuranTopicsEvent.OpenBrowser)
        advanceUntilIdle()

        val state = vm.browseState.value
        assertThat(state.isBranch(doctrine)).isTrue()
        assertThat(state.isBranch(mercy)).isFalse()
    }

    /** A search result opens; it never expands, because expanding would discard the search. */
    @Test
    fun `a search result is never a branch`() = runTest {
        val vm = QuranTopicsViewModel(useCases)

        vm.onEvent(QuranTopicsEvent.OpenBrowser)
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Search("doctrine"))
        advanceUntilIdle()

        assertThat(vm.browseState.value.isBranch(doctrine)).isFalse()
    }

    private fun topic(id: Int, name: String) = QuranTopic(
        id = id,
        name = name,
        arabicName = "",
        description = "",
        wikiLink = "",
        ayahCount = 12,
        parentId = null,
        thematicParentId = null,
        ontologyParentId = null,
        isThematic = true,
        isOntology = false,
        relatedTopicIds = emptyList(),
    )
}
