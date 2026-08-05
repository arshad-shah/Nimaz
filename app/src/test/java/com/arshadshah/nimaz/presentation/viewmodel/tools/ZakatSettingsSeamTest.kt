package com.arshadshah.nimaz.presentation.viewmodel.tools

import androidx.lifecycle.SavedStateHandle
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import com.arshadshah.nimaz.domain.usecase.ZakatUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
 * `ZakatViewModel` used to inject the whole 179-member [SettingsRepository] to reach
 * three fields. It now injects [ZakatSettings] — a six-member seam — so the test can
 * supply a real fake instead of a relaxed mock over a surface it never touches.
 *
 * The assertion that matters: a currency written through the seam reaches state, and
 * nothing outside those six members is reachable from the ViewModel at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZakatSettingsSeamTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: ZakatUseCases

    /** A hand-written fake — six members, not two hundred. */
    private class FakeZakatSettings(
        initialGold: Double = ZakatDefaults.GOLD_PRICE_PER_GRAM,
        initialSilver: Double = ZakatDefaults.SILVER_PRICE_PER_GRAM,
        initialCurrency: String = ZakatDefaults.CURRENCY,
    ) : ZakatSettings {
        private val gold = MutableStateFlow(initialGold)
        private val silver = MutableStateFlow(initialSilver)
        private val currencyFlow = MutableStateFlow(initialCurrency)

        override val zakatGoldPricePerGram: Flow<Double> = gold
        override val zakatSilverPricePerGram: Flow<Double> = silver
        override val zakatCurrency: Flow<String> = currencyFlow

        override suspend fun setZakatGoldPricePerGram(pricePerGram: Double) {
            gold.value = pricePerGram
        }

        override suspend fun setZakatSilverPricePerGram(pricePerGram: Double) {
            silver.value = pricePerGram
        }

        override suspend fun setZakatCurrency(currency: String) {
            currencyFlow.value = currency
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        every { useCases.getAllHistory() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { useCases.getTotalPaid() } returns 0.0
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `currency from the seam reaches state`() = runTest(dispatcher) {
        val settings = FakeZakatSettings(initialCurrency = "GBP")

        val viewModel = ZakatViewModel(useCases, settings, telemetry, SavedStateHandle())
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.currency).isEqualTo("GBP")
    }

    @Test
    fun `a gold price written through the seam is observed`() = runTest(dispatcher) {
        val settings = FakeZakatSettings(initialGold = 60.0)

        val viewModel = ZakatViewModel(useCases, settings, telemetry, SavedStateHandle())
        advanceUntilIdle()
        settings.setZakatGoldPricePerGram(75.0)
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.goldPricePerGram).isEqualTo(75.0)
    }
}
