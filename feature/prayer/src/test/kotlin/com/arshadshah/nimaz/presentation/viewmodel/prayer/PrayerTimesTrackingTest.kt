package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.ResolvedLocation
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.google.common.truth.Truth.assertThat
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
import java.time.LocalDate

/**
 * Marking a prayer from the day pager — the third place in the app a prayer can be tracked.
 *
 * `PrayerTimesViewModelTest` covers the pager itself and asserts only that toggling *sunrise*
 * does nothing. The toggle that does something is untested there, and it has three guards, all
 * of which fail silently when wrong:
 *
 * - **The future is refused.** The screen hides the checkbox on a future day, but the ViewModel
 *   is what enforces it — a swipe forward and a stale composition would otherwise write a record
 *   for a prayer that has not happened, which then counts against a streak.
 * - **It is a toggle, not a set.** Tapping a prayer already marked has to clear it; a handler
 *   that always writes `PRAYED` gives a reader no way to undo a mistap.
 * - **`prayedAt` is cleared on the way back down.** A record marked not-prayed that keeps its
 *   timestamp is a row that disagrees with itself.
 *
 * The fourth is telemetry, and it is the reason this ViewModel is named in #359's follow-up: it
 * reported a generic `toggle_prayer` while the tracker and Home reported `prayer_tracked`, so a
 * dashboard built on the latter under-counted every prayer marked from this screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimesTrackingTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 8, 14)
    private val telemetry = RecordingTelemetry()
    private val records = MutableStateFlow<List<PrayerRecord>>(emptyList())

    private lateinit var prayerUseCases: PrayerUseCases

    private val settings = PrayerCalculationSettings(
        location = ResolvedLocation(51.5, -0.1, "London", isFallback = false),
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null,
        adjustments = emptyMap(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prayerUseCases = mockk(relaxed = true)
        every { prayerUseCases.observeCalculationSettings() } returns flowOf(settings)
        every { prayerUseCases.getPrayerRecordsForDate(any()) } returns records
        every { prayerUseCases.getDaySchedule(any<LocalDate>(), any()) } returns emptyList()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PrayerTimesViewModel(
        prayerUseCases = prayerUseCases,
        todayProvider = FakeTodayProvider(today),
        defaultDispatcher = dispatcher,
        telemetry = telemetry,
    )

    private fun dateKey(date: LocalDate) = date.toEpochDay() * 86_400_000L

    private fun record(name: PrayerName, status: PrayerStatus) = PrayerRecord(
        id = 0L,
        date = dateKey(today),
        prayerName = name,
        status = status,
        prayedAt = if (status == PrayerStatus.PRAYED) 1L else null,
        scheduledTime = 0L,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `marking an untracked prayer records it as prayed, with a time`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(PrayerTimesEvent.TogglePrayer(PrayerType.ASR))
        advanceUntilIdle()

        coVerify {
            prayerUseCases.updatePrayerStatus(
                dateKey(today),
                PrayerName.ASR,
                PrayerStatus.PRAYED,
                match { it != null },
                false,
            )
        }
    }

    @Test
    fun `marking a prayer already prayed clears it, and clears the time with it`() =
        runTest(dispatcher) {
            records.value = listOf(record(PrayerName.DHUHR, PrayerStatus.PRAYED))
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(PrayerTimesEvent.TogglePrayer(PrayerType.DHUHR))
            advanceUntilIdle()

            coVerify {
                prayerUseCases.updatePrayerStatus(
                    dateKey(today),
                    PrayerName.DHUHR,
                    PrayerStatus.NOT_PRAYED,
                    null,
                    false,
                )
            }
        }

    @Test
    fun `a future day cannot be tracked`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(PrayerTimesEvent.NextDay)
        advanceUntilIdle()

        vm.onEvent(PrayerTimesEvent.TogglePrayer(PrayerType.FAJR))
        advanceUntilIdle()

        // The screen also hides the control, but that is a rendering decision; this is the rule.
        coVerify(exactly = 0) {
            prayerUseCases.updatePrayerStatus(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `a past day can still be tracked, against that day's key`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(PrayerTimesEvent.PreviousDay)
        advanceUntilIdle()

        vm.onEvent(PrayerTimesEvent.TogglePrayer(PrayerType.MAGHRIB))
        advanceUntilIdle()

        // Catching up on yesterday is the common case, and the record has to land on *yesterday*:
        // a handler that keyed on "today" would quietly rewrite the wrong day.
        coVerify {
            prayerUseCases.updatePrayerStatus(
                dateKey(today.minusDays(1)),
                PrayerName.MAGHRIB,
                PrayerStatus.PRAYED,
                any(),
                false,
            )
        }
    }

    @Test
    fun `sunrise is not a prayer and is never recorded`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(PrayerTimesEvent.TogglePrayer(PrayerType.SUNRISE))
        advanceUntilIdle()

        coVerify(exactly = 0) {
            prayerUseCases.updatePrayerStatus(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `tracking is reported as a tracked prayer, not as a generic toggle`() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            telemetry.clear()

            vm.onEvent(PrayerTimesEvent.TogglePrayer(PrayerType.ISHA))
            advanceUntilIdle()

            // #359's third site. Reported under its own name, or every dashboard built on
            // `prayer_tracked` under-counts by however many people mark prayers from here.
            assertThat(telemetry.prayersTracked.map { it.prayer }).contains(PrayerName.ISHA.name)
        }
}
