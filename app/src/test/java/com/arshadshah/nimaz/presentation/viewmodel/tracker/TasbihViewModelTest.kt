package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.core.feedback.CounterFeedback
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.repository.settings.TasbihSettings
import com.arshadshah.nimaz.domain.usecase.TasbihUseCases
import com.google.common.truth.Truth.assertThat
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TasbihViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private val todayProvider = FakeTodayProvider(LocalDate.of(2026, 8, 14))

    private lateinit var tasbihUseCases: TasbihUseCases
    private lateinit var tasbihSettings: TasbihSettings
    private lateinit var feedback: CounterFeedback
    private lateinit var viewModel: TasbihViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        tasbihUseCases = mockk(relaxed = true)
        feedback = mockk(relaxed = true)
        tasbihSettings = mockk(relaxed = true)

        every { tasbihSettings.tasbihBeadMode } returns flowOf(false)
        every { tasbihSettings.tasbihBeadDesign } returns flowOf("default")
        every { tasbihSettings.tasbihSelectedPresetId } returns flowOf(-1L)
        every { tasbihSettings.tasbihFavorites } returns flowOf(emptySet())
        every { tasbihSettings.tasbihLeftHanded } returns flowOf(false)
        every { tasbihSettings.tasbihPresetSeedVersion } returns flowOf(100)

        // Relaxed mock handles use-case invocations with appropriate defaults

        viewModel = TasbihViewModel(
            tasbihUseCases = tasbihUseCases,
            tasbihSettings = tasbihSettings,
            feedback = feedback,
            todayProvider = todayProvider,
            telemetry = telemetry,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `counter starts at zero`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.counterState.value.count).isEqualTo(0)
    }

    @Test
    fun `initial presets list is empty`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.presetsState.value.defaultPresets).isEmpty()
        assertThat(viewModel.presetsState.value.customPresets).isEmpty()
    }

    @Test
    fun `initial history lists are empty`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.historyState.value.todaySessions).isEmpty()
    }

    @Test
    fun `ToggleVibration event updates counterState`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(TasbihEvent.ToggleVibration(false))
        advanceUntilIdle()
        assertThat(viewModel.counterState.value.vibrationEnabled).isFalse()
    }

    @Test
    fun `ToggleSound event updates counterState`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(TasbihEvent.ToggleSound(true))
        advanceUntilIdle()
        assertThat(viewModel.counterState.value.soundEnabled).isTrue()
    }

    @Test
    fun `SetCounterStyle BEADS updates counterStyle in state`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(TasbihEvent.SetCounterStyle(TasbihCounterStyle.BEADS))
        advanceUntilIdle()
        assertThat(viewModel.counterState.value.counterStyle).isEqualTo(TasbihCounterStyle.BEADS)
    }

    @Test
    fun `no selected preset on init`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.counterState.value.selectedPreset).isNull()
    }
}
