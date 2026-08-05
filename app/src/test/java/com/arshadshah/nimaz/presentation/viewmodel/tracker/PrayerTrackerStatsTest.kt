package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * The statistics half of [PrayerTrackerViewModel].
 *
 * Two defects meet here. `loadStats()` was the **third** un-cancelled re-invocable load in
 * this file — `dateRecordsJob` and `historyJob` were given handles and this one was missed —
 * so a period change racing an in-flight read left the earlier period's numbers under the
 * later chip. And every window was one day too wide: the range ran `[today-7, tomorrow)`,
 * eight days, so "Week" scored eight days of prayers out of seven days' worth of chips.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTrackerStatsTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var prayerUseCases: PrayerUseCases
    private val telemetry = RecordingTelemetry()

    /** Every `(start, end)` pair asked of `getPrayerStats`, in order. */
    private val statsRanges = mutableListOf<Pair<Long, Long>>()

    /** The records Room hands back for whatever range the stats load is watching. */
    private val rangeRecords = MutableStateFlow(listOf(record(id = 1)))
    private val missedPrayers = MutableStateFlow(listOf(record(id = 9)))

    private var currentStreak = 3

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prayerUseCases = mockk(relaxed = true)

        every { prayerUseCases.getPrayerRecordsForDate(any()) } returns flowOf(emptyList())
        every { prayerUseCases.getPrayerRecordsInRange(any(), any()) } returns rangeRecords
        every { prayerUseCases.getMissedPrayersRequiringQada() } returns missedPrayers
        every { prayerUseCases.getCurrentLocation() } returns flowOf(null)
        coEvery { prayerUseCases.getCurrentStreak(any()) } answers { currentStreak }
        coEvery { prayerUseCases.getLongestStreak() } returns 12
        coEvery { prayerUseCases.getPrayerStats(any(), any()) } answers {
            statsRanges += firstArg<Long>() to secondArg<Long>()
            stats(totalPrayed = 0)
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PrayerTrackerViewModel(prayerUseCases, FakeTodayProvider(LocalDate.now()), telemetry)

    // -- The window (D8) ---------------------------------------------------

    @Test
    fun `the week window spans seven days, not eight`() = runTest {
        viewModel()
        advanceUntilIdle()

        val today = LocalDate.now()
        val (start, end) = statsRanges.first()

        // Exclusive end, so a seven-day window is exactly seven days wide.
        assertThat(end - start).isEqualTo(7 * MILLIS_PER_DAY)
        assertThat(start).isEqualTo(today.minusDays(6).toUtcMidnightMillis())
        assertThat(end).isEqualTo(today.plusDays(1).toUtcMidnightMillis())
    }

    @Test
    fun `the month window ends today and starts a month back, not a month and a day`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        statsRanges.clear()

        vm.onEvent(PrayerTrackerEvent.SetStatsPeriod(StatsPeriod.MONTH))
        advanceUntilIdle()

        val today = LocalDate.now()
        val (start, end) = statsRanges.first()

        assertThat(start).isEqualTo(today.minusMonths(1).plusDays(1).toUtcMidnightMillis())
        assertThat(end).isEqualTo(today.plusDays(1).toUtcMidnightMillis())
    }

    // -- The race (T18) ----------------------------------------------------

    @Test
    fun `an earlier period cannot leave its numbers under a later chip`() = runTest {
        // The slow query is the one the user moves away from — the shape that loses the race.
        coEvery { prayerUseCases.getPrayerStats(any(), any()) } coAnswers {
            val start = firstArg<Long>()
            val end = secondArg<Long>()
            statsRanges += start to end
            if (end - start > 8 * MILLIS_PER_DAY) {
                delay(500)
                stats(totalPrayed = 150)   // MONTH
            } else {
                delay(10)
                stats(totalPrayed = 35)    // WEEK
            }
        }

        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(PrayerTrackerEvent.SetStatsPeriod(StatsPeriod.MONTH))
        advanceTimeBy(20)                  // the month read is still in flight
        vm.onEvent(PrayerTrackerEvent.SetStatsPeriod(StatsPeriod.WEEK))
        advanceUntilIdle()

        assertThat(vm.statsState.value.period).isEqualTo(StatsPeriod.WEEK)
        assertThat(vm.statsState.value.stats?.totalPrayed).isEqualTo(35)
    }

    // -- Reactivity (T18) --------------------------------------------------

    @Test
    fun `the streak follows the prayer table instead of freezing at the last load`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.statsState.value.currentStreak).isEqualTo(3)

        // The Home card marks Fajr while the tracker sits in the back stack: Room
        // re-emits to every observer of `prayer_records`.
        currentStreak = 4
        rangeRecords.value = listOf(record(id = 1), record(id = 2))
        advanceUntilIdle()

        assertThat(vm.statsState.value.currentStreak).isEqualTo(4)
    }

    // -- Instrumentation ---------------------------------------------------

    @Test
    fun `a failing stats read is reported and does not leave the card spinning`() = runTest {
        coEvery { prayerUseCases.getPrayerStats(any(), any()) } throws IllegalStateException("db locked")

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.statsState.value.isLoading).isFalse()
        assertThat(telemetry.errors.map { it.type }).contains("load_stats")
        assertThat(telemetry.exceptions.map { it.message }).contains("db locked")
    }

    // -- The fourth un-cancelled collector ---------------------------------

    @Test
    fun `completing a qada prayer leaves one collector on the missed list`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(PrayerTrackerEvent.MarkQadaCompleted(record(id = 9)))
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.MarkQadaCompleted(record(id = 9)))
        advanceUntilIdle()

        assertThat(missedPrayers.subscriptionCount.value).isEqualTo(1)
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun stats(totalPrayed: Int) = PrayerStats(
    totalPrayed = totalPrayed,
    totalMissed = 0,
    totalJamaah = 0,
    prayedByPrayer = emptyMap(),
    missedByPrayer = emptyMap(),
    currentStreak = 0,
    longestStreak = 0,
    perfectDays = 0,
    startDate = 0L,
    endDate = 0L
)

private fun record(id: Long) = PrayerRecord(
    id = id,
    date = 0L,
    prayerName = PrayerName.FAJR,
    status = PrayerStatus.PRAYED,
    prayedAt = null,
    scheduledTime = 0L,
    isJamaah = false,
    isQadaFor = null,
    note = null,
    createdAt = 0L,
    updatedAt = 0L
)
