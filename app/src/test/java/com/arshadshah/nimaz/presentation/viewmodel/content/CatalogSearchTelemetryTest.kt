package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Catalogue search reports itself once per settled query, with the query's **length**.
 *
 * #359 §4 describes the old shape: the three catalogue screens dispatch `Search` from
 * `onQueryChange`, and the handler called `logFeatureUsed(feature, "search")`, so typing
 * "Ibrahim" emitted seven events and two backspaces emitted two more. The wrong helper as well
 * as the wrong rate — `featureUsed` has nowhere to put the length, which is the one number
 * `search` records.
 *
 * The issue lists this as three sites (`ProphetViewModel:63`, `AsmaUnNabi:67`, `AsmaUlHusna:67`).
 * It is one: those three were byte-identical and collapsed onto [CatalogViewModel] in an earlier
 * layer, so this suite covers all three surfaces.
 *
 * The filtering itself must stay per keystroke — it is an in-memory `List.filter` and the list
 * should track the finger — so the last test pins that the two rates are genuinely separate.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogSearchTelemetryTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private data class Row(val id: Int, val name: String)

    private val rows = MutableStateFlow(
        listOf(Row(1, "Ibrahim"), Row(2, "Musa"), Row(3, "Isa")),
    )

    private inner class Source : CatalogSource<Row> {
        override fun all(): Flow<List<Row>> = rows
        override fun favourites(): Flow<List<Row>> = MutableStateFlow(emptyList())
        override suspend fun byId(id: Int): Row? = rows.value.firstOrNull { it.id == id }
        override suspend fun toggleFavourite(id: Int) = Unit
        override fun idOf(item: Row): Int = item.id
        override fun matches(item: Row, query: String) = item.name.contains(query, true)
    }

    private inner class TestCatalog : CatalogViewModel<Row>(Source(), telemetry, FEATURE)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Typing a name is one event, not one per character. */
    @Test
    fun `a query typed character by character reports once`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()
        telemetry.clear()

        "Ibrahim".forEachIndexed { index, _ ->
            vm.onEvent(CatalogEvent.Search("Ibrahim".take(index + 1)))
            advanceTimeBy(50) // faster than the debounce, as a typist is
        }
        advanceUntilIdle()

        assertThat(telemetry.searches).hasSize(1)
        assertThat(telemetry.searches.single().filter).isEqualTo(FEATURE)
        assertThat(telemetry.searches.single().queryLength).isEqualTo(7)
    }

    /** And the length is the *trimmed* length — a trailing space is not a character searched for. */
    @Test
    fun `the reported length excludes surrounding whitespace`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()
        telemetry.clear()

        vm.onEvent(CatalogEvent.Search("  Musa  "))
        advanceUntilIdle()

        assertThat(telemetry.searches.single().queryLength).isEqualTo(4)
    }

    /**
     * Clearing the box is not a search. `onClear` used to emit an event with an empty query,
     * which is the same defect #359 names on `SearchViewModel` — a `search_performed` for a
     * search that never ran.
     */
    @Test
    fun `clearing the box reports nothing`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("Musa"))
        advanceUntilIdle()
        telemetry.clear()

        vm.onEvent(CatalogEvent.ClearSearch)
        advanceUntilIdle()

        assertThat(telemetry.searches).isEmpty()
    }

    /** Two different settled queries are two events; the same one twice is not. */
    @Test
    fun `a repeated query is not reported twice`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()
        telemetry.clear()

        vm.onEvent(CatalogEvent.Search("Musa"))
        advanceUntilIdle()
        vm.onEvent(CatalogEvent.Search("Musa"))
        advanceUntilIdle()

        assertThat(telemetry.searches).hasSize(1)
    }

    /**
     * The list still filters on every keystroke. Debouncing the *analytics* must not slow the
     * filter down — that would trade a real regression for a reporting fix.
     */
    @Test
    fun `filtering happens immediately, before any event is reported`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()
        telemetry.clear()

        vm.onEvent(CatalogEvent.Search("Ibr"))
        // No time advanced at all: the filter is synchronous inside `onEvent`.
        assertThat(vm.listState.value.filteredItems.map { it.name }).containsExactly("Ibrahim")
        assertThat(telemetry.searches).isEmpty()

        advanceUntilIdle()
        assertThat(telemetry.searches).hasSize(1)
    }

    private companion object {
        const val FEATURE = "test_catalog"
    }
}
