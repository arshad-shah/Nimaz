package com.arshadshah.nimaz.presentation.viewmodel.search

import com.arshadshah.nimaz.domain.model.LibrarySearchResults
import com.arshadshah.nimaz.domain.usecase.SearchLibraryUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private lateinit var searchLibrary: SearchLibraryUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        searchLibrary = mockk()
        coEvery { searchLibrary.invoke(any(), any()) } returns LibrarySearchResults.EMPTY
        coEvery { searchLibrary.byTerms(any(), any()) } returns LibrarySearchResults.EMPTY
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SearchViewModel(searchLibrary)

    @Test
    fun `typing a query runs the search without pressing enter`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        advanceUntilIdle()
        // The search must have executed off the query change alone — no ExecuteSearch event.
        coVerify { searchLibrary.invoke("noor", any()) }
    }

    @Test
    fun `a non-blank query marks the state as searching before results arrive`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        // Synchronously (before the debounce/search runs) the state must read as searching,
        // so the screen's empty state can't flash "no results" prematurely.
        assertThat(vm.searchState.value.isSearching).isTrue()
    }

    @Test
    fun `rapid typing debounces to a single search for the final query`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("n"))
        vm.onEvent(SearchEvent.UpdateQuery("no"))
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        advanceUntilIdle()
        coVerify(exactly = 1) { searchLibrary.invoke("noor", any()) }
        coVerify(exactly = 0) { searchLibrary.invoke("n", any()) }
        coVerify(exactly = 0) { searchLibrary.invoke("no", any()) }
    }

    @Test
    fun `clearing the query stops searching and empties results`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        advanceUntilIdle()
        vm.onEvent(SearchEvent.UpdateQuery(""))
        advanceUntilIdle()
        assertThat(vm.searchState.value.isSearching).isFalse()
        assertThat(vm.searchState.value.filteredResults).isEmpty()
    }

    @Test
    fun `AI terms replace the results list via the smart search`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.ApplyAiTerms(listOf("patience", " sabr ", "")))
        advanceUntilIdle()
        coVerify { searchLibrary.byTerms(listOf("patience", "sabr"), any()) }
    }
}
