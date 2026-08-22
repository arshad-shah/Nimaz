package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
 * Two detail requests in flight, resolving out of order.
 *
 * #364 R5 describes this against the three copies of the catalogue ViewModel; the dedupe layer
 * collapsed them onto [CatalogViewModel], so it is one fix and one test rather than three of
 * each — and the cancel-and-replace handle it asks for **landed with that dedupe**. This suite
 * is what pins that: it passes on the current code and fails on the pre-dedupe shape, which is
 * the only claim it is entitled to make.
 *
 * It does *not* exercise the narrower window a cancelled coroutine still leaves open — the read
 * completing before the cancel lands, so the write runs anyway. Awaiting a gate is a
 * cancellable suspension point, so cancelling kills the coroutine there every time and the
 * window cannot be forced deterministically from a test. The `requestedItemId` guard added
 * alongside this suite is defence for that case, and is honestly untested.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogDetailRaceTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private data class Row(val id: Int, val name: String)

    private val all = MutableStateFlow(listOf(Row(1, "Adam"), Row(12, "Muhammad")))

    /** Held open so a test can decide when a given id's read resolves. */
    private val gates = mutableMapOf<Int, CompletableDeferred<Unit>>()

    private inner class Source : CatalogSource<Row> {
        override fun all(): Flow<List<Row>> = all
        override fun favourites(): Flow<List<Row>> = MutableStateFlow(emptyList())
        override suspend fun byId(id: Int): Row? {
            gates[id]?.await()
            return all.value.firstOrNull { it.id == id }
        }

        override suspend fun toggleFavourite(id: Int) = Unit
        override fun idOf(item: Row): Int = item.id
        override fun matches(item: Row, query: String) = item.name.contains(query, true)
    }

    private inner class TestCatalog : CatalogViewModel<Row>(Source(), telemetry, "test_catalog")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the slower of two detail reads cannot overwrite the newer one`() = runTest {
        val adam = CompletableDeferred<Unit>()
        gates[1] = adam

        val vm = TestCatalog()
        advanceUntilIdle()

        // Open Adam — its read is held open, as a slow first query would be.
        vm.onEvent(CatalogEvent.LoadDetail(1))
        advanceUntilIdle()

        // Back, then open Muhammad before Adam's read has resolved. The detail screen fires
        // `LoadDetail` from a `LaunchedEffect(id)`, and a shared instance is reachable through
        // the list screen's back-stack entry, so both are genuinely in flight.
        vm.onEvent(CatalogEvent.LoadDetail(12))
        advanceUntilIdle()

        // Adam's read now resolves — second.
        adam.complete(Unit)
        advanceUntilIdle()

        // Before the guard: Adam's story rendered under Muhammad's route, with
        // isLoading = false to say it was ready.
        assertThat(vm.detailState.value.item?.id).isEqualTo(12)
        assertThat(vm.detailState.value.isLoading).isFalse()
    }

    @Test
    fun `an uncontested detail load still resolves`() = runTest {
        val vm = TestCatalog()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.LoadDetail(1))
        advanceUntilIdle()

        assertThat(vm.detailState.value.item?.id).isEqualTo(1)
        assertThat(vm.detailState.value.isLoading).isFalse()
    }
}
