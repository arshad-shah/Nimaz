package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * One test suite for what used to be three copies of the same ViewModel.
 *
 * `AsmaUlHusnaViewModel`, `AsmaUnNabiViewModel` and `ProphetViewModel` were byte-identical
 * after identifier substitution, and between them had four tests — none of which touched the
 * filter. That is the shape a triplicated file always ends up in: nobody writes the same test
 * three times, so it gets written zero times.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private data class Row(val id: Int, val name: String)

    private val all = MutableStateFlow(
        listOf(Row(1, "Ar-Rahman"), Row(2, "Ar-Rahim"), Row(3, "Al-Malik")),
    )
    private val favourites = MutableStateFlow(listOf(Row(3, "Al-Malik")))
    private var toggled = mutableListOf<Int>()
    private var failing = false

    private inner class Source : CatalogSource<Row> {
        override fun all(): Flow<List<Row>> =
            if (failing) flow { throw IllegalStateException("no such table") } else all

        override fun favourites(): Flow<List<Row>> = favourites
        override suspend fun byId(id: Int): Row? = all.value.firstOrNull { it.id == id }
        override suspend fun toggleFavourite(id: Int) { toggled += id }
        override fun idOf(item: Row): Int = item.id
        override fun matches(item: Row, query: String): Boolean =
            item.name.contains(query, ignoreCase = true)
    }

    private inner class TestCatalog : CatalogViewModel<Row>(Source(), telemetry, "test_catalog")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the filtered view starts as the whole list`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()

        assertThat(vm.listState.value.filteredItems.map { it.id }).containsExactly(1, 2, 3)
    }

    @Test
    fun `searching narrows the list`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("rahi"))
        advanceUntilIdle()

        assertThat(vm.listState.value.filteredItems.map { it.id }).containsExactly(2)
    }

    @Test
    fun `favourites are tracked alongside the list, not as a filter on it`() = runTest {
        // There used to be a `ToggleFavoritesFilter` event and a `showFavoritesOnly` flag,
        // because each of the three name screens had its own all/favourites chip row. There
        // is one Favourites destination now, and it reads this list directly — so the flag
        // and its event went with the chips.
        val vm = TestCatalog()
        advanceUntilIdle()

        assertThat(vm.listState.value.favorites.map { it.id }).containsExactly(3)

        // A query narrows what is *listed* and leaves the favourites alone.
        vm.onEvent(CatalogEvent.Search("rahman"))
        advanceUntilIdle()

        assertThat(vm.listState.value.filteredItems.map { it.id }).containsExactly(1)
        assertThat(vm.listState.value.favorites.map { it.id }).containsExactly(3)
    }

    @Test
    fun `a new emission is filtered by the query already on screen`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()
        vm.onEvent(CatalogEvent.Search("malik"))
        advanceUntilIdle()

        // The bug the three copies were one forgotten `applyFilters()` away from: the list
        // refreshes and quietly ignores the filter the user has set.
        all.value = all.value + Row(4, "Al-Quddus")
        advanceUntilIdle()

        assertThat(vm.listState.value.filteredItems.map { it.id }).containsExactly(3)
    }

    @Test
    fun `clearing the query restores the list`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()
        vm.onEvent(CatalogEvent.Search("malik"))
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.ClearSearch)
        advanceUntilIdle()

        assertThat(vm.listState.value.filteredItems.map { it.id }).containsExactly(1, 2, 3)
    }

    @Test
    fun `opening a detail loads that item`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.LoadDetail(2))
        advanceUntilIdle()

        assertThat(vm.detailState.value.item?.id).isEqualTo(2)
        assertThat(vm.detailState.value.isLoading).isFalse()
    }

    @Test
    fun `a failing list load clears the spinner and is reported`() = runTest {
        failing = true
        val vm = TestCatalog()
        advanceUntilIdle()

        assertThat(vm.listState.value.isLoading).isFalse()
        assertThat(telemetry.errors.map { it.type }).contains("load_items")
    }
}
