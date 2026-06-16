package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.repository.ZakatRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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

/**
 * Unit tests for [ZakatViewModel] — the interactive calculator state machine.
 *
 * Unlike [com.arshadshah.nimaz.domain.model.ZakatCalculator], this ViewModel
 * folds gold/silver *grams* into the asset total (via the configured per-gram
 * prices) and auto-recalculates whenever an input changes. These tests pin
 * down that conversion, the nisab comparison, the auto-recalc gating, and the
 * history/persistence delegation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZakatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: ZakatRepository
    private lateinit var viewModel: ZakatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.getAllHistory() } returns flowOf(emptyList())
        coEvery { repository.getTotalPaid() } returns 0.0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ZakatViewModel(repository)

    // ── Auto-recalculation gating ───────────────────────────────────

    @Test
    fun `no calculation is produced while all inputs are zero`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(0.0))
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.calculation).isNull()
    }

    @Test
    fun `updating cash triggers a recalculation`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation
        assertThat(calc).isNotNull()
        assertThat(calc!!.totalAssets).isWithin(1e-9).of(10_000.0)
        assertThat(calc.isAboveNisab).isTrue() // default gold nisab = 87.48 * 65 = 5686.2
        assertThat(calc.zakatDue).isWithin(1e-9).of(10_000.0 * 0.025)
    }

    // ── Metal grams are converted to value and included in assets ───

    @Test
    fun `gold grams are converted to value using the configured price`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // 100g gold at the default 65/g = 6500, above gold nisab (5686.2).
        viewModel.onEvent(ZakatEvent.UpdateGold(100.0))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation!!
        assertThat(calc.goldValue).isWithin(1e-9).of(6_500.0)
        assertThat(calc.totalAssets).isWithin(1e-9).of(6_500.0)
        assertThat(calc.isAboveNisab).isTrue()
        assertThat(calc.zakatDue).isWithin(1e-9).of(6_500.0 * 0.025)
    }

    @Test
    fun `changing the gold price recalculates the nisab and zakat`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(6_000.0))
        advanceUntilIdle()
        // At 65/g the gold nisab is 5686.2 -> above; bump the price so the
        // nisab (87.48 * 100 = 8748) now exceeds the 6000 net worth.
        viewModel.onEvent(ZakatEvent.UpdateGoldPrice(100.0))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation!!
        assertThat(calc.nisabValue).isWithin(1e-9).of(87.48 * 100.0)
        assertThat(calc.isAboveNisab).isFalse()
        assertThat(calc.zakatDue).isEqualTo(0.0)
    }

    // ── Liabilities & net worth (ViewModel does NOT clamp to zero) ──

    @Test
    fun `net worth can go negative when liabilities exceed assets`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(1_000.0))
        viewModel.onEvent(ZakatEvent.UpdateDebts(3_000.0))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation!!
        assertThat(calc.netWorth).isWithin(1e-9).of(-2_000.0)
        assertThat(calc.isAboveNisab).isFalse()
        assertThat(calc.zakatDue).isEqualTo(0.0)
    }

    // ── Nisab type switch ───────────────────────────────────────────

    @Test
    fun `switching to silver nisab uses the silver threshold`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(1_000.0))
        viewModel.onEvent(ZakatEvent.SetNisabType(NisabType.SILVER))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation!!
        assertThat(calc.nisabType).isEqualTo(NisabType.SILVER)
        // silver nisab = 612.36 * 0.80 = 489.888, net worth 1000 is above it.
        assertThat(calc.nisabValue).isWithin(1e-6).of(612.36 * 0.80)
        assertThat(calc.isAboveNisab).isTrue()
    }

    // ── ClearAll / ToggleBreakdown ──────────────────────────────────

    @Test
    fun `ClearAll resets inputs but preserves prices and currency`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateGoldPrice(70.0))
        viewModel.onEvent(ZakatEvent.SetCurrency("GBP"))
        viewModel.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.ClearAll)
        advanceUntilIdle()

        val state = viewModel.calculatorState.value
        assertThat(state.assets.cashOnHand).isEqualTo(0.0)
        assertThat(state.calculation).isNull()
        assertThat(state.goldPricePerGram).isEqualTo(70.0)
        assertThat(state.currency).isEqualTo("GBP")
    }

    @Test
    fun `ToggleBreakdown flips the breakdown flag`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.showBreakdown).isFalse()
        viewModel.onEvent(ZakatEvent.ToggleBreakdown)
        assertThat(viewModel.calculatorState.value.showBreakdown).isTrue()
        viewModel.onEvent(ZakatEvent.ToggleBreakdown)
        assertThat(viewModel.calculatorState.value.showBreakdown).isFalse()
    }

    // ── Persistence delegation ──────────────────────────────────────

    @Test
    fun `SaveCalculation persists when a calculation exists`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()
        viewModel.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.insertCalculation(any()) }
    }

    @Test
    fun `SaveCalculation is a no-op when there is no calculation`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertCalculation(any()) }
    }

    @Test
    fun `MarkAsPaid and DeleteCalculation delegate to the repository`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.MarkAsPaid(42L))
        viewModel.onEvent(ZakatEvent.DeleteCalculation(7L))
        advanceUntilIdle()

        coVerify { repository.markAsPaid(42L, any()) }
        coVerify { repository.deleteCalculation(7L) }
    }

    // ── History mapping ─────────────────────────────────────────────

    @Test
    fun `history entities are mapped into the history state`() = runTest {
        val entity = ZakatHistoryEntity(
            id = 5,
            calculatedAt = 1000L,
            totalAssets = 10_000.0,
            totalLiabilities = 2_000.0,
            netWorth = 8_000.0,
            zakatDue = 200.0,
            nisabType = "GOLD",
            nisabValue = 5_686.2
        )
        every { repository.getAllHistory() } returns flowOf(listOf(entity))
        coEvery { repository.getTotalPaid() } returns 200.0

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.history).hasSize(1)
        assertThat(state.history.first().id).isEqualTo(5)
        assertThat(state.history.first().nisabType).isEqualTo(NisabType.GOLD)
        assertThat(state.totalZakatPaid).isWithin(1e-9).of(200.0)
    }

    @Test
    fun `unparseable nisab type in history falls back to gold`() = runTest {
        val entity = ZakatHistoryEntity(
            id = 1,
            calculatedAt = 1000L,
            totalAssets = 1.0,
            totalLiabilities = 0.0,
            netWorth = 1.0,
            zakatDue = 0.0,
            nisabType = "PLATINUM", // not a valid NisabType
            nisabValue = 0.0
        )
        every { repository.getAllHistory() } returns flowOf(listOf(entity))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.historyState.value.history.first().nisabType)
            .isEqualTo(NisabType.GOLD)
    }
}
