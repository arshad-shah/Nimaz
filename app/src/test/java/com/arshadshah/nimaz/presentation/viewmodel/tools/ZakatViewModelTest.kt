package com.arshadshah.nimaz.presentation.viewmodel.tools

import androidx.lifecycle.SavedStateHandle
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.ZakatUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
 * `ZakatViewModel` had **no tests at all**, which is how two defects shipped:
 *
 *  - The gold and silver prices were hardcoded literals that no screen could change,
 *    and `ZakatCalculator` derives the **nisab threshold** from the gold price as well
 *    as the metal valuation — so a stale price changes whether zakat is owed at all.
 *  - Clearing the last asset field left the previous calculation on screen, because
 *    `recalculate()` skipped when nothing had a value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZakatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: ZakatUseCases
    private lateinit var settings: SettingsRepository

    private val goldPrice = MutableStateFlow(ZakatDefaults.GOLD_PRICE_PER_GRAM)
    private val silverPrice = MutableStateFlow(ZakatDefaults.SILVER_PRICE_PER_GRAM)
    private val currency = MutableStateFlow(ZakatDefaults.CURRENCY)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        every { useCases.getAllHistory() } returns flowOf(emptyList())
        coEvery { useCases.getTotalPaid() } returns 0.0

        settings = mockk(relaxed = true)
        every { settings.zakatGoldPricePerGram } returns goldPrice
        every { settings.zakatSilverPricePerGram } returns silverPrice
        every { settings.zakatCurrency } returns currency
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        ZakatViewModel(useCases, settings, telemetry, saved)

    @Test
    fun `setting a currency persists it and re-reaches state`() = runTest {
        // ZakatEvent.SetCurrency had a handler and no producer until the calculator grew a
        // picker: every figure was formatted with state.currency and nothing could change it.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.SetCurrency("GBP"))
        advanceUntilIdle()

        coVerify { settings.setZakatCurrency("GBP") }
    }

    @Test
    fun `a failing history stream clears the spinner instead of hanging on it`() = runTest {
        // ZakatHistoryUiState.isLoading defaults to true and loadHistory only cleared it
        // *inside* the collect, so a stream that threw before its first emission left the
        // history screen spinning for the lifetime of the ViewModel.
        every { useCases.getAllHistory() } returns kotlinx.coroutines.flow.flow {
            throw IllegalStateException("no such table: zakat_history")
        }

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.historyState.value.isLoading).isFalse()
        assertThat(telemetry.errors.map { it.domain }).contains("zakat")
    }

    @Test
    fun `clearing the last asset clears the result instead of leaving it stale`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()
        assertThat(vm.calculatorState.value.calculation).isNotNull()

        vm.onEvent(ZakatEvent.UpdateCash(0.0))
        advanceUntilIdle()

        // The breakdown card renders whenever calculation != null, so a stale value
        // here shows "zakat due" over an empty form.
        assertThat(vm.calculatorState.value.calculation).isNull()
    }

    @Test
    fun `an edited gold price is persisted, not just held in state`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateGoldPrice(85.0))
        advanceUntilIdle()

        coVerify { settings.setZakatGoldPricePerGram(85.0) }
    }

    @Test
    fun `a persisted price reaches the calculation`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateGold(100.0))
        advanceUntilIdle()
        val atDefaultPrice = vm.calculatorState.value.calculation!!.zakatDue

        // Simulate the DataStore write landing back through the observed flow.
        goldPrice.value = 85.0
        advanceUntilIdle()
        val atRealPrice = vm.calculatorState.value.calculation!!.zakatDue

        assertThat(vm.calculatorState.value.goldPricePerGram).isEqualTo(85.0)
        assertThat(atRealPrice).isGreaterThan(atDefaultPrice)
    }

    @Test
    fun `the gold price moves the nisab threshold, not just the amount`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // 87.48g is the gold nisab. Holding 80g is below the threshold at any price,
        // but the *value* comparison is what decides — so a price change can flip it.
        vm.onEvent(ZakatEvent.SetNisabType(NisabType.GOLD))
        vm.onEvent(ZakatEvent.UpdateCash(6_000.0))
        advanceUntilIdle()

        val atDefault = vm.calculatorState.value.calculation!!
        goldPrice.value = 85.0
        advanceUntilIdle()
        val atHigher = vm.calculatorState.value.calculation!!

        assertThat(atDefault.nisabValue).isNotEqualTo(atHigher.nisabValue)
        // 6,000 clears a 65/g nisab (5,686) but not an 85/g one (7,436).
        assertThat(atDefault.zakatDue).isGreaterThan(0.0)
        assertThat(atHigher.zakatDue).isEqualTo(0.0)
    }

    @Test
    fun `currency changes are persisted`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.SetCurrency("EUR"))
        advanceUntilIdle()

        coVerify { settings.setZakatCurrency("EUR") }
    }

    @Test
    fun `saving with no calculation does not insert`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        coVerify(exactly = 0) { useCases.insertCalculation(any()) }
    }

    /**
     * The longest form in the app lived in a `MutableStateFlow` and nothing else, so a phone
     * call part-way through entering thirteen figures returned the user to a blank form. The
     * saved handle is the same one the framework hands back after process death.
     */
    @Test
    fun `the typed form survives process death`() = runTest {
        val saved = SavedStateHandle()
        val before = viewModel(saved)
        advanceUntilIdle()

        before.onEvent(ZakatEvent.UpdateCash(10_000.0))
        before.onEvent(ZakatEvent.UpdateGold(120.0))
        before.onEvent(ZakatEvent.UpdateDebts(2_500.0))
        before.onEvent(ZakatEvent.SetNisabType(NisabType.SILVER))
        advanceUntilIdle()

        // A new ViewModel over the same handle is what the process restart produces.
        val after = viewModel(saved)
        advanceUntilIdle()

        val restored = after.calculatorState.value
        assertThat(restored.assets.cashOnHand).isEqualTo(10_000.0)
        assertThat(restored.assets.goldGrams).isEqualTo(120.0)
        assertThat(restored.liabilities.debts).isEqualTo(2_500.0)
        assertThat(restored.nisabType).isEqualTo(NisabType.SILVER)
        // And the result is recomputed from it, not left null under a filled-in form.
        assertThat(restored.calculation).isNotNull()
    }

    @Test
    fun `clearing the form also clears what would be restored`() = runTest {
        val saved = SavedStateHandle()
        val vm = viewModel(saved)
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()
        vm.onEvent(ZakatEvent.ClearAll)
        advanceUntilIdle()

        assertThat(viewModel(saved).calculatorState.value.assets.cashOnHand).isEqualTo(0.0)
    }

    @Test
    fun `a failing history load is reported rather than crashing`() = runTest {
        every { useCases.getAllHistory() } returns kotlinx.coroutines.flow.flow {
            throw IllegalStateException("no such table: zakat_history")
        }

        viewModel()
        advanceUntilIdle()

        assertThat(telemetry.errors.map { it.domain }).contains("zakat")
    }
}
