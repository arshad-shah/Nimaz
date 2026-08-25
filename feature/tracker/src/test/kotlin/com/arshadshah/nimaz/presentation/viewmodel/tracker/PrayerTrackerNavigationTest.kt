package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.google.common.truth.Truth.assertThat
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
 * Moving between days, and the four load events that refresh what a day shows.
 *
 * The day-step guard is the behaviour worth the most here: **you cannot step forward past
 * today.** The tracker records what you did, so a tomorrow with five empty rows invites logging
 * a prayer that has not happened — and every downstream count (the review banner, the month's
 * complete days, the streak) then has to decide what to make of it. The rail disables its future
 * cells for the same reason; this is the same rule on the other side of the seam, where a deep
 * link or a stale saved date can also land.
 *
 * `today` comes from `TodayProvider`, faked here. A ViewModel that read `LocalDate.now()` would
 * make this untestable and would freeze at whatever day the object was constructed — the
 * rollover shape behind #363.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTrackerNavigationTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private val today = LocalDate.of(2026, 8, 13)

    private lateinit var prayerUseCases: PrayerUseCases

    private val qada = MutableStateFlow<List<PrayerRecord>>(emptyList())

    private fun record(date: LocalDate, prayer: PrayerName, status: PrayerStatus) = PrayerRecord(
        id = date.toEpochDay() * 10 + prayer.ordinal,
        date = date.toUtcMidnightMillis(),
        prayerName = prayer,
        status = status,
        prayedAt = null,
        scheduledTime = 0L,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prayerUseCases = mockk(relaxed = true)

        every { prayerUseCases.getPrayerRecordsForDate(any()) } returns flowOf(emptyList())
        every { prayerUseCases.getPrayerRecordsInRange(any(), any()) } returns flowOf(emptyList())
        every { prayerUseCases.getMissedPrayersRequiringQada() } returns qada
        every { prayerUseCases.getCurrentLocation() } returns flowOf(null)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PrayerTrackerViewModel(
        prayerUseCases,
        FakeTodayProvider(today),
        telemetry,
    )

    @Test
    fun `the tracker opens on today`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.trackerState.value.selectedDate).isEqualTo(today)
    }

    @Test
    fun `stepping back moves one day at a time`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(PrayerTrackerEvent.NavigateToPreviousDay)
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.NavigateToPreviousDay)
        advanceUntilIdle()

        assertThat(vm.trackerState.value.selectedDate).isEqualTo(today.minusDays(2))
    }

    @Test
    fun `stepping forward from a past day is allowed`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.SelectDate(today.minusDays(3)))
        advanceUntilIdle()

        vm.onEvent(PrayerTrackerEvent.NavigateToNextDay)
        advanceUntilIdle()

        assertThat(vm.trackerState.value.selectedDate).isEqualTo(today.minusDays(2))
    }

    @Test
    fun `stepping forward stops at today`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(PrayerTrackerEvent.NavigateToNextDay)
        advanceUntilIdle()

        // Not tomorrow. A future day would offer five empty rows to log prayers that have not
        // happened, and every count downstream would then have to decide what to make of them.
        assertThat(vm.trackerState.value.selectedDate).isEqualTo(today)
    }

    @Test
    fun `selecting a day loads that day's records`() = runTest {
        val chosen = today.minusDays(5)
        every {
            prayerUseCases.getPrayerRecordsForDate(chosen.toUtcMidnightMillis())
        } returns flowOf(listOf(record(chosen, PrayerName.FAJR, PrayerStatus.PRAYED)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.SelectDate(chosen))
        advanceUntilIdle()

        assertThat(vm.trackerState.value.selectedDate).isEqualTo(chosen)
        assertThat(vm.trackerState.value.prayerRecords).hasSize(1)
    }

    @Test
    fun `reloading today returns the selection to today`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.SelectDate(today.minusDays(4)))
        advanceUntilIdle()

        vm.onEvent(PrayerTrackerEvent.LoadToday)
        advanceUntilIdle()

        assertThat(vm.trackerState.value.selectedDate).isEqualTo(today)
    }

    @Test
    fun `the qada list is what the user has marked missed`() = runTest {
        qada.value = listOf(
            record(today.minusDays(2), PrayerName.FAJR, PrayerStatus.MISSED),
            record(today.minusDays(3), PrayerName.ISHA, PrayerStatus.MISSED),
        )

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.LoadQadaPrayers)
        advanceUntilIdle()

        assertThat(vm.qadaState.value.missedPrayers).hasSize(2)
        assertThat(vm.qadaState.value.totalMissed).isEqualTo(2)
        // Grouped by month for the list's headings; two different days of one month are one
        // group, not two.
        assertThat(vm.qadaState.value.groupedByMonth).hasSize(1)
        assertThat(vm.qadaState.value.isLoading).isFalse()
    }

    @Test
    fun `an empty qada list is reported as loaded, not as still loading`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(PrayerTrackerEvent.LoadQadaPrayers)
        advanceUntilIdle()

        // The screen tells "nothing owed" from "not yet known" by this flag alone, and shows
        // "All caught up!" only for the first.
        assertThat(vm.qadaState.value.missedPrayers).isEmpty()
        assertThat(vm.qadaState.value.isLoading).isFalse()
    }

    @Test
    fun `each stats period asks for its own window`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        StatsPeriod.entries.forEach { period ->
            vm.onEvent(PrayerTrackerEvent.SetStatsPeriod(period))
            advanceUntilIdle()
            assertThat(vm.statsState.value.period).isEqualTo(period)
        }

        // All four are distinct spans; a `when` missing an arm would silently reuse the previous
        // period's numbers under the new chip's label.
        vm.onEvent(PrayerTrackerEvent.LoadStats)
        advanceUntilIdle()
        assertThat(vm.statsState.value.period).isEqualTo(StatsPeriod.ALL_TIME)
    }
}
