package com.arshadshah.nimaz.presentation.viewmodel.tools

import androidx.lifecycle.SavedStateHandle
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.repository.FakeZakatSettings
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.usecase.ZakatUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
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

/**
 * `ZakatViewModel` used to inject the whole 179-member `SettingsRepository` to reach three
 * fields. It now injects `ZakatSettings` — an eight-member seam — so the test can supply a real
 * fake instead of a relaxed mock over a surface it never touches.
 *
 * The assertions that matter: the currency and the nisab basis written through the seam reach
 * state, and nothing outside those eight members is reachable from the ViewModel at all. The
 * basis is here rather than in the calculator's `SavedStateHandle` since the settings screen
 * took ownership of it — a `SavedStateHandle` survives process death, a preference survives a
 * cold start too.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZakatSettingsSeamTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: ZakatUseCases

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
        val settings = FakeZakatSettings(code = "GBP")

        val viewModel = ZakatViewModel(useCases, settings, telemetry, SavedStateHandle())
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.currency).isEqualTo("GBP")
    }

    @Test
    fun `the nisab basis comes from the seam, not from the saved form`() = runTest(dispatcher) {
        // The basis used to live only in SavedStateHandle, so a cold start forgot which ruling
        // the user follows — and the silver nisab is roughly an order of magnitude lower than
        // the gold one, so forgetting it changes whether zakat is owed at all.
        val settings = FakeZakatSettings(nisabType = NisabType.SILVER.name)

        val viewModel = ZakatViewModel(useCases, settings, telemetry, SavedStateHandle())
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.nisabType).isEqualTo(NisabType.SILVER)
    }

    @Test
    fun `an unknown basis name falls back rather than throwing`() = runTest(dispatcher) {
        // Names arrive from synced payloads written by other builds.
        val settings = FakeZakatSettings(nisabType = "PLATINUM")

        val viewModel = ZakatViewModel(useCases, settings, telemetry, SavedStateHandle())
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.nisabType).isEqualTo(NisabType.DEFAULT)
    }

    @Test
    fun `a gold price written through the seam is observed`() = runTest(dispatcher) {
        val settings = FakeZakatSettings(gold = 60.0)

        val viewModel = ZakatViewModel(useCases, settings, telemetry, SavedStateHandle())
        advanceUntilIdle()
        settings.setZakatGoldPricePerGram(75.0)
        advanceUntilIdle()

        assertThat(viewModel.calculatorState.value.goldPricePerGram).isEqualTo(75.0)
    }
}
