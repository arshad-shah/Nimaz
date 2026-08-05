package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.GetTopicChildrenUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicDetailUseCase
import com.arshadshah.nimaz.domain.usecase.GetTopicTreeRootsUseCase
import com.arshadshah.nimaz.domain.usecase.HasThematicContentUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.SearchTopicsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The subject browser is a tree that opens in place, and it has to be honest about leaves.
 *
 * Most of the 2,512 subjects have no children. Opening one used to load the whole
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
    private lateinit var settings: SettingsRepository

    //  Doctrine ─ God ─ The names of God ─ The Merciful ─ (deeper still)
    //           └ Mercy (a leaf)
    private val doctrine = topic(id = 1, name = "Doctrine")
    private val god = topic(id = 11, name = "God", parent = 1)
    private val mercy = topic(id = 13, name = "Mercy", parent = 1)
    private val names = topic(id = 111, name = "The names of God", parent = 11)
    private val merciful = topic(id = 1111, name = "The Merciful", parent = 111)
    private val forgiving = topic(id = 1112, name = "The Forgiving", parent = 1111)

    private val children = mapOf(
        1 to listOf(god, mercy),
        11 to listOf(names),
        111 to listOf(merciful),
        1111 to listOf(forgiving),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        getChildren = mockk()
        coEvery { getChildren.invoke(any(), any()) } answers {
            children[firstArg<Int>()].orEmpty()
        }
        coEvery { getChildren.branchesIn(any()) } returns children.keys

        getDetail = mockk()
        coEvery { getDetail.invoke(any(), any()) } returns null

        val getRoots = mockk<GetTopicTreeRootsUseCase>()
        coEvery { getRoots.invoke(any()) } returns listOf(doctrine)

        val hasContent = mockk<HasThematicContentUseCase>()
        coEvery { hasContent.invoke() } returns true

        val search = mockk<SearchTopicsUseCase>()
        coEvery { search.invoke(any(), any()) } returns listOf(god)
        coEvery { search.pathsFor(any(), any()) } returns mapOf(11 to listOf(doctrine))

        settings = mockk(relaxed = true)
        every { settings.quranTranslatorId } returns MutableStateFlow("sahih_international")

        useCases = mockk(relaxed = true)
        every { useCases.getTopicChildren } returns getChildren
        every { useCases.getTopicDetail } returns getDetail
        every { useCases.getTopicTreeRoots } returns getRoots
        every { useCases.hasThematicContent } returns hasContent
        every { useCases.searchTopics } returns search
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `toggling a childless topic opens nothing`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()
        assertThat(vm.browseState.value.rows.map { it.topic.id })
            .containsExactly(1, 11, 13).inOrder()

        vm.onEvent(QuranTopicsEvent.Toggle(mercy))
        advanceUntilIdle()

        assertThat(vm.browseState.value.expanded).doesNotContain(13)
        assertThat(vm.browseState.value.rows.map { it.topic.id })
            .containsExactly(1, 11, 13).inOrder()
    }

    @Test
    fun `opening a node asks for children, not for the whole topic detail`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()

        coVerify { getChildren.invoke(1, TopicTree.THEMATIC) }
        coVerify(exactly = 0) { getDetail.invoke(any(), any()) }
    }

    @Test
    fun `children are fetched once and answered from the cache after that`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()

        coVerify(exactly = 1) { getChildren.invoke(1, TopicTree.THEMATIC) }
    }

    @Test
    fun `a leaf is not offered a disclosure control, and a branch is`() = runTest {
        val vm = openedBrowser()

        val state = vm.browseState.value
        assertThat(state.isBranch(doctrine)).isTrue()
        assertThat(state.isBranch(mercy)).isFalse()
    }

    /** A search result opens; it never expands, because expanding would discard the search. */
    @Test
    fun `a search result is never a branch, and carries its path`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.Search("god"))
        advanceUntilIdle()

        val state = vm.browseState.value
        assertThat(state.isSearchMode).isTrue()
        assertThat(state.isBranch(god)).isFalse()
        assertThat(state.searchPaths[11]?.map { it.name }).containsExactly("Doctrine")
    }

    /** Past the cap there is no text column left, so the row re-roots instead of indenting. */
    @Test
    fun `the tree stops indenting at the cap`() = runTest {
        val vm = openedBrowser()

        listOf(doctrine, god, names, merciful).forEach {
            vm.onEvent(QuranTopicsEvent.Toggle(it))
            advanceUntilIdle()
        }

        val state = vm.browseState.value
        assertThat(state.rows.maxOf { it.depth }).isEqualTo(3)
        assertThat(state.rows.map { it.topic.id }).doesNotContain(forgiving.id)
        assertThat(state.isAtIndentCap(3)).isTrue()
    }

    @Test
    fun `focusing a branch re-roots the tree and builds the crumb trail`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Toggle(god))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Focus(names))
        advanceUntilIdle()

        val state = vm.browseState.value
        assertThat(state.focus.map { it.name })
            .containsExactly("Doctrine", "God", "The names of God").inOrder()
        assertThat(state.rows.map { it.topic.id }).containsExactly(1111)
        assertThat(state.expanded).isEmpty()
    }

    @Test
    fun `a crumb re-roots at its own level`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Toggle(god))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Focus(names))
        advanceUntilIdle()

        vm.onEvent(QuranTopicsEvent.RebaseTo(0))
        advanceUntilIdle()

        val state = vm.browseState.value
        assertThat(state.focus.map { it.name }).containsExactly("Doctrine")
        assertThat(state.rows.map { it.topic.id }).containsExactly(11, 13).inOrder()

        vm.onEvent(QuranTopicsEvent.RebaseTo(QuranTopicsEvent.RebaseTo.ROOT))
        advanceUntilIdle()

        assertThat(vm.browseState.value.focus).isEmpty()
        assertThat(vm.browseState.value.rows.map { it.topic.id }).containsExactly(1)
    }

    /** Back undoes the last thing opened, then steps out of a focus, then leaves the screen. */
    @Test
    fun `back closes the innermost node before it touches the focus`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Toggle(god))
        advanceUntilIdle()
        assertThat(vm.browseState.value.canGoBack).isTrue()

        vm.onEvent(QuranTopicsEvent.Back)
        advanceUntilIdle()
        assertThat(vm.browseState.value.rows.map { it.topic.id })
            .containsExactly(1, 11, 13).inOrder()

        vm.onEvent(QuranTopicsEvent.Back)
        advanceUntilIdle()
        assertThat(vm.browseState.value.rows.map { it.topic.id }).containsExactly(1)
        assertThat(vm.browseState.value.canGoBack).isFalse()
    }

    /** A closed ancestor keeps what was open beneath it, so reopening restores the shape. */
    @Test
    fun `reopening a closed node restores what was open under it`() = runTest {
        val vm = openedBrowser()

        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Toggle(god))
        advanceUntilIdle()
        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()

        assertThat(vm.browseState.value.rows.map { it.topic.id }).containsExactly(1)

        vm.onEvent(QuranTopicsEvent.Toggle(doctrine))
        advanceUntilIdle()

        assertThat(vm.browseState.value.rows.map { it.topic.id })
            .containsExactly(1, 11, 111, 13).inOrder()
    }

    private fun openedBrowser(): QuranTopicsViewModel {
        val vm = QuranTopicsViewModel(useCases, settings)
        vm.onEvent(QuranTopicsEvent.OpenBrowser)
        dispatcher.scheduler.advanceUntilIdle()
        return vm
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
        ontologyParentId = null,
        isThematic = true,
        isOntology = false,
        relatedTopicIds = emptyList(),
    )
}
