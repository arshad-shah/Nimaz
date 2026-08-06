package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatCalculator
import com.arshadshah.nimaz.presentation.viewmodel.FakeZakatSettings
import com.google.common.truth.Truth.assertThat
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

/**
 * The nisab basis, the two metal prices and the currency moved off the calculator's form onto
 * their own screen. What has to survive the move: every one of them still **persists** — the
 * basis in particular, which used to live in `SavedStateHandle` and so was forgotten on every
 * cold start — and the threshold the screen previews is the same figure `ZakatCalculator` will
 * compare wealth against.
 *
 * The fake is the calculator's, deliberately: both ViewModels write and read the same seam, and
 * a second fake would let them drift.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZakatSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the stored basis is what the screen opens on`() = runTest(dispatcher) {
        val settings = FakeZakatSettings(nisabType = NisabType.SILVER.name)

        val viewModel = ZakatSettingsViewModel(settings, telemetry)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.nisabType).isEqualTo(NisabType.SILVER)
    }

    @Test
    fun `choosing a basis persists it`() = runTest(dispatcher) {
        val settings = FakeZakatSettings(nisabType = NisabType.GOLD.name)
        val viewModel = ZakatSettingsViewModel(settings, telemetry)
        advanceUntilIdle()

        viewModel.onEvent(ZakatSettingsEvent.SetNisabType(NisabType.SILVER))
        advanceUntilIdle()

        // Read back through the seam, not from the ViewModel's own state: a basis that only
        // reached the screen would be forgotten by the next cold start, which is the exact
        // defect the move was meant to fix.
        assertThat(viewModel.uiState.value.nisabType).isEqualTo(NisabType.SILVER)
        assertThat(ZakatSettingsViewModel(settings, telemetry).let {
            advanceUntilIdle()
            it.uiState.value.nisabType
        }).isEqualTo(NisabType.SILVER)
    }

    @Test
    fun `an edited price persists and moves the previewed threshold`() = runTest(dispatcher) {
        val settings = FakeZakatSettings(gold = 65.0)
        val viewModel = ZakatSettingsViewModel(settings, telemetry)
        advanceUntilIdle()
        val before = viewModel.uiState.value.nisabValue

        viewModel.onEvent(ZakatSettingsEvent.SetGoldPrice(85.0))
        advanceUntilIdle()

        val after = viewModel.uiState.value
        assertThat(after.goldPricePerGram).isEqualTo(85.0)
        assertThat(after.nisabValue).isGreaterThan(before)
        // The same function the calculation uses — a preview deriving its own threshold is how
        // the screen and the calculator come to promise different numbers.
        assertThat(after.nisabValue)
            .isEqualTo(ZakatCalculator.GOLD_NISAB_GRAMS * 85.0)
    }

    @Test
    fun `the silver price drives the threshold once silver is the basis`() = runTest(dispatcher) {
        val settings = FakeZakatSettings(gold = 65.0, silver = 0.80)
        val viewModel = ZakatSettingsViewModel(settings, telemetry)
        advanceUntilIdle()

        viewModel.onEvent(ZakatSettingsEvent.SetNisabType(NisabType.SILVER))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.nisabValue)
            .isEqualTo(ZakatCalculator.SILVER_NISAB_GRAMS * 0.80)
    }

    @Test
    fun `a currency change persists`() = runTest(dispatcher) {
        val settings = FakeZakatSettings(code = "USD")
        val viewModel = ZakatSettingsViewModel(settings, telemetry)
        advanceUntilIdle()

        viewModel.onEvent(ZakatSettingsEvent.SetCurrency("GBP"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.currency).isEqualTo("GBP")
    }

    @Test
    fun `no monetary amount reaches analytics`() = runTest(dispatcher) {
        val viewModel = ZakatSettingsViewModel(FakeZakatSettings(), telemetry)
        advanceUntilIdle()

        viewModel.onEvent(ZakatSettingsEvent.SetGoldPrice(1_234.56))
        viewModel.onEvent(ZakatSettingsEvent.SetSilverPrice(78.9))
        advanceUntilIdle()

        // A gold rate alone is a market fact; paired with the amounts on the calculator it is
        // somebody's finances. Only the fact that the setting was edited is recorded.
        val values = telemetry.settingChanges.map { it.value }
        assertThat(values).doesNotContain("1234.56")
        assertThat(values).doesNotContain("78.9")
        assertThat(telemetry.settingChanges.map { it.setting })
            .containsAtLeast("zakat_gold_price", "zakat_silver_price")
    }
}
