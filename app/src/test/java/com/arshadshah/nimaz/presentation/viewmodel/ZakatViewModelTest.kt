package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatCalculator
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

@OptIn(ExperimentalCoroutinesApi::class)
class ZakatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ZakatRepository
    private lateinit var viewModel: ZakatViewModel

    private val tolerance = 1e-6

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

    private fun historyEntity(
        id: Long = 1,
        nisabType: String = "GOLD",
        isPaid: Boolean = false
    ) = ZakatHistoryEntity(
        id = id,
        calculatedAt = 1_000L,
        totalAssets = 10_000.0,
        totalLiabilities = 1_000.0,
        netWorth = 9_000.0,
        zakatDue = 225.0,
        nisabType = nisabType,
        nisabValue = 5_000.0,
        isPaid = isPaid
    )

    // ── Initial state ───────────────────────────────────────────────

    @Test
    fun `initial calculator state has gold nisab and no calculation`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.calculatorState.value
        assertThat(state.nisabType).isEqualTo(NisabType.GOLD)
        assertThat(state.calculation).isNull()
        assertThat(state.isCalculating).isFalse()
    }

    @Test
    fun `init loads history and clears the loading flag`() = runTest {
        every { repository.getAllHistory() } returns flowOf(listOf(historyEntity(id = 7)))
        coEvery { repository.getTotalPaid() } returns 500.0

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.historyState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.history).hasSize(1)
        assertThat(state.history.first().id).isEqualTo(7)
        assertThat(state.totalZakatPaid).isEqualTo(500.0)
    }

    // ── Calculation: gold/silver are valued here (unlike pure calculator) ────

    @Test
    fun `updating cash triggers a calculation`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation
        assertThat(calc).isNotNull()
        assertThat(calc!!.totalAssets).isWithin(tolerance).of(10_000.0)
        // Default gold price 65 -> nisab 87.48 * 65 = 5686.2; net worth 10000 above.
        assertThat(calc.isAboveNisab).isTrue()
        assertThat(calc.zakatDue).isWithin(tolerance).of(10_000.0 * ZakatCalculator.ZAKAT_RATE)
    }

    @Test
    fun `gold grams are valued into total assets via the gold price`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // 100g gold at default 65/g = 6500 of assets.
        viewModel.onEvent(ZakatEvent.UpdateGold(100.0))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation
        assertThat(calc).isNotNull()
        assertThat(calc!!.goldValue).isWithin(tolerance).of(6_500.0)
        assertThat(calc.totalAssets).isWithin(tolerance).of(6_500.0)
    }

    @Test
    fun `liabilities are subtracted and net worth is not clamped to zero`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(1_000.0))
        viewModel.onEvent(ZakatEvent.UpdateDebts(5_000.0))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation
        assertThat(calc).isNotNull()
        // Net worth goes negative here (the ViewModel does not clamp).
        assertThat(calc!!.netWorth).isWithin(tolerance).of(-4_000.0)
        assertThat(calc.isAboveNisab).isFalse()
        assertThat(calc.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `switching nisab type recalculates with the silver threshold`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // 500 cash: above silver nisab (612.36 * 0.8 = 489.9) but below gold nisab.
        viewModel.onEvent(ZakatEvent.UpdateCash(500.0))
        advanceUntilIdle()
        assertThat(viewModel.calculatorState.value.calculation!!.isAboveNisab).isFalse()

        viewModel.onEvent(ZakatEvent.SetNisabType(NisabType.SILVER))
        advanceUntilIdle()

        val calc = viewModel.calculatorState.value.calculation!!
        assertThat(calc.nisabType).isEqualTo(NisabType.SILVER)
        assertThat(calc.isAboveNisab).isTrue()
        assertThat(calc.zakatDue).isWithin(tolerance).of(500.0 * ZakatCalculator.ZAKAT_RATE)
    }

    // ── Clear / toggle ──────────────────────────────────────────────

    @Test
    fun `clearAll resets assets but preserves prices and currency`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateGoldPrice(80.0))
        viewModel.onEvent(ZakatEvent.SetCurrency("GBP"))
        viewModel.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.ClearAll)
        advanceUntilIdle()

        val state = viewModel.calculatorState.value
        assertThat(state.assets.cashOnHand).isEqualTo(0.0)
        assertThat(state.calculation).isNull()
        assertThat(state.goldPricePerGram).isEqualTo(80.0)
        assertThat(state.currency).isEqualTo("GBP")
    }

    @Test
    fun `toggleBreakdown flips the breakdown flag`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.showBreakdown).isFalse()
        viewModel.onEvent(ZakatEvent.ToggleBreakdown)
        assertThat(viewModel.calculatorState.value.showBreakdown).isTrue()
        viewModel.onEvent(ZakatEvent.ToggleBreakdown)
        assertThat(viewModel.calculatorState.value.showBreakdown).isFalse()
    }

    // ── Persistence events ──────────────────────────────────────────

    @Test
    fun `saveCalculation does nothing when there is no calculation yet`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertCalculation(any()) }
    }

    @Test
    fun `saveCalculation persists the current calculation`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()
        viewModel.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        coVerify {
            repository.insertCalculation(match { it.netWorth == 10_000.0 && it.nisabType == "GOLD" })
        }
    }

    @Test
    fun `markAsPaid delegates to the repository`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.MarkAsPaid(42L))
        advanceUntilIdle()

        coVerify { repository.markAsPaid(eq(42L), any()) }
    }

    @Test
    fun `deleteCalculation delegates to the repository`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ZakatEvent.DeleteCalculation(13L))
        advanceUntilIdle()

        coVerify { repository.deleteCalculation(13L) }
    }

    // ── History mapping edge case ───────────────────────────────────

    @Test
    fun `history mapping falls back to gold for an unknown nisab type`() = runTest {
        every { repository.getAllHistory() } returns flowOf(listOf(historyEntity(nisabType = "BOGUS")))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.historyState.value.history.first().nisabType).isEqualTo(NisabType.GOLD)
    }
}
