package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.GetSurahListUseCase
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.SearchDuasUseCase
import com.arshadshah.nimaz.domain.usecase.SearchHadithsUseCase
import com.arshadshah.nimaz.domain.usecase.SearchQuranUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

    private lateinit var quranUseCases: QuranUseCases
    private lateinit var hadithUseCases: HadithUseCases
    private lateinit var duaUseCases: DuaUseCases
    private lateinit var searchDuas: SearchDuasUseCase
    private lateinit var searchHadiths: SearchHadithsUseCase
    private lateinit var searchQuran: SearchQuranUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        searchQuran = mockk()
        every { searchQuran.invoke(any(), any()) } returns flowOf(emptyList())
        val getSurahList = mockk<GetSurahListUseCase>()
        every { getSurahList.search(any()) } returns flowOf(emptyList())
        quranUseCases = mockk(relaxed = true)
        every { quranUseCases.searchQuran } returns searchQuran
        every { quranUseCases.getSurahList } returns getSurahList

        searchHadiths = mockk()
        every { searchHadiths.invoke(any()) } returns flowOf(emptyList())
        hadithUseCases = mockk(relaxed = true)
        every { hadithUseCases.searchHadiths } returns searchHadiths

        searchDuas = mockk()
        every { searchDuas.invoke(any()) } returns flowOf(emptyList())
        duaUseCases = mockk(relaxed = true)
        every { duaUseCases.searchDuas } returns searchDuas
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SearchViewModel(quranUseCases, hadithUseCases, duaUseCases)

    @Test
    fun `typing a query runs the search without pressing enter`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        advanceUntilIdle()
        // The search must have executed off the query change alone — no ExecuteSearch event.
        verify { searchDuas.invoke("noor") }
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
        verify(exactly = 1) { searchDuas.invoke("noor") }
        verify(exactly = 0) { searchDuas.invoke("n") }
        verify(exactly = 0) { searchDuas.invoke("no") }
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
}
