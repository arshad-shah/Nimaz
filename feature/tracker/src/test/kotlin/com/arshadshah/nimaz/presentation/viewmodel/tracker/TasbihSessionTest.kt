package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.repository.CounterFeedback
import com.arshadshah.nimaz.domain.repository.settings.TasbihSettings
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.TasbihUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import java.time.LocalDate

/**
 * The counter's session lifecycle — the part of the tasbih that survives a relaunch.
 *
 * The count lives in Room, so every defect here is permanent from the user's side: a tap that
 * starts a second session leaves an orphan row inflating the day's total, a lap that is not
 * written loses thirty-three counts, and a preset switch that abandons a session loses the whole
 * sitting. None of it is visible on screen — the number keeps going up either way.
 *
 * Four races and guards are pinned here that no screen test can reach:
 *
 *  - **Two taps 20 ms apart.** Both used to see `currentSession == null` and both inserted a
 *    session; the second hard-set `count = 1`, so the user tapped twice and the counter read 1.
 *    `startingSession` is checked and set in the same synchronous block, and the taps that land
 *    while the insert is in flight are counted rather than dropped.
 *  - **Auto-lap.** A lap rolls the count to zero and the lap count up, and the row written is the
 *    *within-lap* count — the database sums `currentCount + laps × target`, so writing the
 *    running total would double-count every lap.
 *  - **Switching dhikr mid-session.** The old session is completed, not abandoned. Switching to
 *    the same preset must not complete anything.
 *  - **`applyPersistedSelection`.** The Choose-Dhikr screen holds a *different* ViewModel
 *    instance and communicates through DataStore, so this runs on every emission; without the
 *    id guard it would reset the counter each time the flow re-emitted the id already selected.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasbihSessionTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private val todayProvider = FakeTodayProvider(LocalDate.of(2026, 8, 14))

    private lateinit var useCases: TasbihUseCases
    private lateinit var settings: TasbihSettings
    private lateinit var feedback: CounterFeedback

    private val selectedPresetId = MutableStateFlow(-1L)
    private val favorites = MutableStateFlow<Set<String>>(emptySet())

    private var nextSessionId = 1L
    private val inserted = mutableMapOf<Long, TasbihSession>()

    private fun preset(id: Long, target: Int = 33, name: String = "Dhikr $id") = TasbihPreset(
        id = id,
        name = name,
        arabicText = null,
        transliteration = null,
        translation = null,
        targetCount = target,
        category = TasbihCategory.CUSTOM,
        reference = null,
        isDefault = false,
        displayOrder = 0,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        useCases = mockk(relaxed = true)
        feedback = mockk(relaxed = true)
        settings = mockk(relaxed = true)

        every { settings.tasbihBeadMode } returns flowOf(false)
        every { settings.tasbihBeadDesign } returns flowOf("wood")
        every { settings.tasbihSelectedPresetId } returns selectedPresetId
        every { settings.tasbihFavorites } returns favorites
        every { settings.tasbihLeftHanded } returns flowOf(false)
        every { settings.tasbihPresetSeedVersion } returns flowOf(100)

        // A store that behaves like the DAO: an insert gets an id, and reading it back returns
        // the row. The ViewModel reads its own insert back before counting the pending taps.
        coEvery { useCases.insertSession(any()) } answers {
            val session = firstArg<TasbihSession>().copy(id = nextSessionId++)
            inserted[session.id] = session
            session.id
        }
        coEvery { useCases.getSessionById(any()) } answers { inserted[firstArg()] }
        coEvery { useCases.getActiveSession() } returns null
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TasbihViewModel(
        tasbihUseCases = useCases,
        tasbihSettings = settings,
        feedback = feedback,
        todayProvider = todayProvider,
        telemetry = telemetry,
    )

    @Test
    fun `the first tap opens a session and the counter reads one`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.Increment)
        advanceUntilIdle()

        assertThat(vm.counterState.value.count).isEqualTo(1)
        assertThat(vm.counterState.value.isActive).isTrue()
        assertThat(vm.counterState.value.currentSession).isNotNull()
        coVerify(exactly = 1) { useCases.insertSession(any()) }
    }

    @Test
    fun `two taps during the insert count two and open one session`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // Both taps land before the insert's coroutine has run — the exact race that used to
        // leave an orphan session and a counter reading 1.
        vm.onEvent(TasbihEvent.Increment)
        vm.onEvent(TasbihEvent.Increment)
        advanceUntilIdle()

        assertThat(vm.counterState.value.count).isEqualTo(2)
        coVerify(exactly = 1) { useCases.insertSession(any()) }
    }

    @Test
    fun `a tap is felt before it is counted`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(TasbihEvent.ToggleVibration(true))
        vm.onEvent(TasbihEvent.ToggleSound(true))
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.Increment)
        advanceUntilIdle()

        // The tick is emitted synchronously, so it stays in step with the finger rather than
        // waiting on a Room write.
        coVerify { feedback.tick(vibrate = true, sound = true) }
    }

    @Test
    fun `reaching the target rolls a lap and writes the within-lap count`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(TasbihEvent.SelectPreset(preset(1, target = 3)))
        advanceUntilIdle()

        repeat(3) { vm.onEvent(TasbihEvent.Increment); advanceUntilIdle() }

        assertThat(vm.counterState.value.laps).isEqualTo(1)
        assertThat(vm.counterState.value.count).isEqualTo(0)
        // The row stores the within-lap count; the database sums `currentCount + laps × target`,
        // so writing the running total would report 6 for three taps of a target of three.
        coVerify { useCases.updateSessionCount(any(), 0, 1) }
    }

    @Test
    fun `a completed lap is worth an analytics event, a tap is not`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(TasbihEvent.SelectPreset(preset(1, target = 2)))
        advanceUntilIdle()

        repeat(2) { vm.onEvent(TasbihEvent.Increment); advanceUntilIdle() }

        // Instrumenting the tap itself would emit one event per finger on a counter people run
        // past 100 — the lap is the bounded unit that answers "is the tasbih used".
        assertThat(telemetry.featureUsages.map { it.action }).contains("lap_completed")
        assertThat(telemetry.featureUsages.count { it.action == "session_started" }).isEqualTo(1)
    }

    @Test
    fun `raising the target mid-lap converts the overflow into laps`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(TasbihEvent.SelectPreset(preset(1, target = 100)))
        advanceUntilIdle()
        repeat(7) { vm.onEvent(TasbihEvent.Increment) }
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.SetTargetCount(3))
        advanceUntilIdle()

        // Seven counted against a target of three is two laps and one over — not seven, and not
        // a reset. The user's count is the thing that must survive the change.
        assertThat(vm.counterState.value.laps).isEqualTo(2)
        assertThat(vm.counterState.value.count).isEqualTo(1)
    }

    @Test
    fun `a target below one is refused`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.SetTargetCount(0))
        advanceUntilIdle()

        // The counter's progress arithmetic divides by the target, and `% 0` throws.
        assertThat(vm.counterState.value.targetCount).isEqualTo(1)
    }

    @Test
    fun `reset zeroes the count and says so to the database`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        repeat(4) { vm.onEvent(TasbihEvent.Increment); advanceUntilIdle() }

        vm.onEvent(TasbihEvent.Reset)
        advanceUntilIdle()

        assertThat(vm.counterState.value.count).isEqualTo(0)
        assertThat(vm.counterState.value.laps).isEqualTo(0)
        // A reset that only cleared the UI would come back on the next launch.
        coVerify { useCases.updateSessionCount(any(), 0, 0) }
    }

    @Test
    fun `switching dhikr mid-count completes the session rather than abandoning it`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(TasbihEvent.SelectPreset(preset(1)))
        advanceUntilIdle()
        repeat(5) { vm.onEvent(TasbihEvent.Increment); advanceUntilIdle() }
        val sessionId = vm.counterState.value.currentSession!!.id

        vm.onEvent(TasbihEvent.SelectPreset(preset(2, target = 99)))
        advanceUntilIdle()

        // Without this the five counts vanish from the history: the row stays open forever and
        // is never totalled.
        coVerify { useCases.completeSession(sessionId, any(), any()) }
        assertThat(vm.counterState.value.count).isEqualTo(0)
        assertThat(vm.counterState.value.targetCount).isEqualTo(99)
        assertThat(vm.counterState.value.currentSession).isNull()
    }

    @Test
    fun `re-choosing the dhikr already counting does not close its session`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        val chosen = preset(1)
        vm.onEvent(TasbihEvent.SelectPreset(chosen))
        advanceUntilIdle()
        repeat(3) { vm.onEvent(TasbihEvent.Increment); advanceUntilIdle() }
        val sessionId = vm.counterState.value.currentSession!!.id

        vm.onEvent(TasbihEvent.SelectPreset(chosen))
        advanceUntilIdle()

        // Tapping the row you are already counting is a no-op the user expects; completing the
        // session there would split one sitting into two history rows.
        coVerify(exactly = 0) { useCases.completeSession(sessionId, any(), any()) }
    }

    @Test
    fun `clearing to a free count completes what was in progress and resets the target`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            vm.onEvent(TasbihEvent.SelectPreset(preset(1, target = 7)))
            advanceUntilIdle()
            repeat(2) { vm.onEvent(TasbihEvent.Increment); advanceUntilIdle() }
            val sessionId = vm.counterState.value.currentSession!!.id

            vm.onEvent(TasbihEvent.ClearPreset)
            advanceUntilIdle()

            coVerify { useCases.completeSession(sessionId, any(), any()) }
            assertThat(vm.counterState.value.selectedPreset).isNull()
            assertThat(vm.counterState.value.targetCount).isEqualTo(33)
            coVerify { settings.setTasbihSelectedPresetId(-1L) }
        }

    @Test
    fun `clearing with nothing counted writes no session`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(TasbihEvent.SelectPreset(preset(1)))
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.ClearPreset)
        advanceUntilIdle()

        coVerify(exactly = 0) { useCases.completeSession(any(), any(), any()) }
    }

    @Test
    fun `a selection persisted by the other screen is applied here`() = runTest {
        coEvery { useCases.getPresetById(5L) } returns preset(5, target = 11)
        val vm = viewModel()
        advanceUntilIdle()

        // `ChooseDhikrScreen` holds its own ViewModel instance; DataStore is the only channel
        // between the two.
        selectedPresetId.value = 5L
        advanceUntilIdle()

        assertThat(vm.counterState.value.selectedPreset?.id).isEqualTo(5L)
        assertThat(vm.counterState.value.targetCount).isEqualTo(11)
    }

    @Test
    fun `re-emitting the id already selected does not reset the count`() = runTest {
        coEvery { useCases.getPresetById(5L) } returns preset(5)
        val vm = viewModel()
        advanceUntilIdle()
        selectedPresetId.value = 5L
        advanceUntilIdle()
        repeat(4) { vm.onEvent(TasbihEvent.Increment); advanceUntilIdle() }

        // DataStore re-emits on any write to the file, not only on a change to this key.
        selectedPresetId.value = 5L
        advanceUntilIdle()

        assertThat(vm.counterState.value.count).isEqualTo(4)
    }

    @Test
    fun `a persisted id of minus one clears a selection that is in place`() = runTest {
        coEvery { useCases.getPresetById(5L) } returns preset(5, target = 11)
        val vm = viewModel()
        advanceUntilIdle()
        selectedPresetId.value = 5L
        advanceUntilIdle()

        selectedPresetId.value = -1L
        advanceUntilIdle()

        assertThat(vm.counterState.value.selectedPreset).isNull()
        assertThat(vm.counterState.value.targetCount).isEqualTo(33)
    }

    @Test
    fun `favouriting adds an id and favouriting again takes it away`() = runTest {
        val saved = slot<Set<String>>()
        coEvery { settings.setTasbihFavorites(capture(saved)) } answers {
            favorites.value = saved.captured
        }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.ToggleFavorite(7L))
        advanceUntilIdle()
        assertThat(favorites.value).containsExactly("7")
        assertThat(vm.presetsState.value.favorites).containsExactly(7L)

        vm.onEvent(TasbihEvent.ToggleFavorite(7L))
        advanceUntilIdle()
        // One control, both directions: a star that could only be set would leave the user no
        // way to unpick a dhikr they starred by mistake.
        assertThat(favorites.value).isEmpty()
    }

    @Test
    fun `a favourite id that is not a number is ignored rather than crashing`() = runTest {
        favorites.value = setOf("3", "not-an-id")
        val vm = viewModel()
        advanceUntilIdle()

        // The set is stored as strings in DataStore, so a corrupted or hand-edited entry is
        // reachable — and it must not take the presets screen down.
        assertThat(vm.presetsState.value.favorites).containsExactly(3L)
    }

    @Test
    fun `an unfinished session from a previous launch is picked back up`() = runTest {
        val session = TasbihSession(
            id = 42L,
            presetId = 5L,
            presetName = "Dhikr 5",
            date = 0L,
            currentCount = 8,
            targetCount = 11,
            totalLaps = 2,
            isCompleted = false,
            duration = null,
            startedAt = 0L,
            completedAt = null,
            note = null,
        )
        coEvery { useCases.getActiveSession() } returns session
        coEvery { useCases.getPresetById(5L) } returns preset(5, target = 11)
        // The persisted selection agrees with the session's preset, as it does on a real
        // relaunch — the two are written together.
        selectedPresetId.value = 5L

        val vm = viewModel()
        advanceUntilIdle()

        // The count is in Room precisely so it survives a relaunch; dropping it on open would
        // lose whatever was counted before the app was swiped away.
        assertThat(vm.counterState.value.currentSession?.id).isEqualTo(42L)
        assertThat(vm.counterState.value.count).isEqualTo(8)
        assertThat(vm.counterState.value.laps).isEqualTo(2)
        assertThat(vm.counterState.value.targetCount).isEqualTo(11)
    }

    @Test
    fun `a stored session with a target of zero is repaired rather than dividing by it`() =
        runTest {
            coEvery { useCases.getActiveSession() } returns TasbihSession(
                id = 43L,
                presetId = null,
                presetName = null,
                date = 0L,
                currentCount = 4,
                targetCount = 0,
                totalLaps = 0,
                isCompleted = false,
                duration = null,
                startedAt = 0L,
                completedAt = null,
                note = null,
            )

            val vm = viewModel()
            advanceUntilIdle()

            // Every writer coerces to at least one, but a legacy or imported row need not have —
            // and this runs at init, so one bad row would take the counter down on open rather
            // than degrade. The row is still picked up; only the modulo is guarded.
            assertThat(vm.counterState.value.currentSession?.id).isEqualTo(43L)
            assertThat(vm.counterState.value.count).isEqualTo(0)
            assertThat(telemetry.exceptions).isEmpty()
        }

    @Test
    fun `the default adhkar are seeded once per version`() = runTest {
        every { settings.tasbihPresetSeedVersion } returns flowOf(0)

        viewModel()
        advanceUntilIdle()

        // Adhkar added after the prepackaged database shipped reach an existing install only
        // this way, and re-seeding on every launch would duplicate them.
        coVerify(exactly = 1) { useCases.seedMissingDefaults() }
        coVerify { settings.setTasbihPresetSeedVersion(any()) }
    }

    @Test
    fun `an install already at the current seed version is left alone`() = runTest {
        viewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { useCases.seedMissingDefaults() }
    }
}
