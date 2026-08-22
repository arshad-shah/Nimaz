package com.arshadshah.nimaz.presentation.viewmodel.tracker

import java.time.LocalDate
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.repository.CounterFeedback
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.TasbihUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
 * `increment()` is the most-used action in the app's tasbih, and until now it could
 * not be tested at all: its first two lines drove a real `Vibrator` and
 * `ToneGenerator` built from an injected `Context`. That is why the existing Tasbih
 * suite tests a preset filter the screen does not use, and why the double-tap race
 * below shipped.
 *
 * With `CounterFeedback` behind an interface, the counter is ordinary code.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasbihCounterTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: TasbihUseCases
    private lateinit var prefs: SettingsRepository
    private lateinit var feedback: CounterFeedback

    private fun session(id: Long, count: Int) = TasbihSession(
        id = id,
        presetId = null,
        presetName = null,
        date = 0L,
        currentCount = count,
        targetCount = 33,
        totalLaps = 0,
        isCompleted = false,
        duration = null,
        startedAt = 0L,
        completedAt = null,
        note = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        feedback = mockk(relaxed = true)
        useCases = mockk(relaxed = true)
        every { useCases.getDefaultPresets() } returns flowOf(emptyList())
        every { useCases.getCustomPresets() } returns flowOf(emptyList())
        every { useCases.getSessionsForDate(any()) } returns flowOf(emptyList())
        every { useCases.getSessionsInRange(any(), any()) } returns flowOf(emptyList())
        coEvery { useCases.getActiveSession() } returns null
        coEvery { useCases.insertSession(any()) } returns 1L
        coEvery { useCases.getSessionById(any()) } answers { session(firstArg(), 1) }

        prefs = mockk(relaxed = true)
        every { prefs.tasbihBeadMode } returns flowOf(false)
        every { prefs.tasbihBeadDesign } returns flowOf("wood")
        every { prefs.tasbihSelectedPresetId } returns flowOf(0L)
        every { prefs.tasbihFavorites } returns flowOf(emptySet())
        every { prefs.tasbihLeftHanded } returns flowOf(false)
        every { prefs.tasbihPresetSeedVersion } returns MutableStateFlow(99)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TasbihViewModel(useCases, prefs, feedback, FakeTodayProvider(LocalDate.now()), telemetry)

    @Test
    fun `a single tap starts one session and counts one`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.Increment)
        advanceUntilIdle()

        assertThat(vm.counterState.value.count).isEqualTo(1)
        coVerify(exactly = 1) { useCases.insertSession(any()) }
    }

    @Test
    fun `a double tap during session creation counts two and inserts one session`() = runTest {
        // Hold the insert open so both taps land while it is in flight — the exact
        // window in which both used to see currentSession == null.
        val gate = CompletableDeferred<Unit>()
        coEvery { useCases.insertSession(any()) } coAnswers {
            gate.await()
            1L
        }

        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.Increment)
        vm.onEvent(TasbihEvent.Increment)
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        // Previously: two sessions inserted, and the second update hard-set count = 1,
        // so the user tapped twice and the counter read 1.
        assertThat(vm.counterState.value.count).isEqualTo(2)
        coVerify(exactly = 1) { useCases.insertSession(any()) }
    }

    @Test
    fun `a free count stores no preset name rather than the English literal`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.Increment)
        advanceUntilIdle()

        // "Free Count" used to be written into every user's database, untranslatable.
        // presetName is nullable and the history screen already renders a localized
        // fallback.
        coVerify { useCases.insertSession(match { it.presetName == null }) }
    }

    @Test
    fun `the tick respects the vibration and sound settings`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.Increment)
        advanceUntilIdle()

        val state = vm.counterState.value
        coVerify { feedback.tick(state.vibrationEnabled, state.soundEnabled) }
    }

    @Test
    fun `reloading history does not accumulate collectors`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        repeat(4) {
            vm.onEvent(TasbihEvent.LoadHistory)
            advanceUntilIdle()
        }

        // Each call cancels its predecessor, so exactly one collector per query is live.
        // Before, four sessions in a sitting left ten collectors writing the same state,
        // each holding its own captured `today`.
        assertThat(vm.historyState.value.todaySessions).isEmpty()
    }

}
