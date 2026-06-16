package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.ZoneOffset

/**
 * Unit tests for [PrayerTrackerViewModel] — the prayer status/stats state
 * machine. Focuses on the status-transition logic (prayedAt is stamped only
 * for PRAYED/LATE), the future-day navigation guard, the epoch conversion used
 * for record lookups, and the qada grouping-by-month aggregation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTrackerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: PrayerRepository
    private lateinit var viewModel: PrayerTrackerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        // Defaults so init {} (loadCurrentLocation/loadToday/loadStats/loadQada)
        // completes without touching real infrastructure.
        every { repository.getCurrentLocation() } returns flowOf(null)
        every { repository.getPrayerRecordsForDate(any()) } returns flowOf(emptyList())
        every { repository.getPrayerRecordsInRange(any(), any()) } returns flowOf(emptyList())
        every { repository.getMissedPrayersRequiringQada() } returns flowOf(emptyList())
        coEvery { repository.getPrayerStats(any(), any()) } returns emptyStats()
        coEvery { repository.getCurrentStreak(any()) } returns 0
        coEvery { repository.getLongestStreak() } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PrayerTrackerViewModel(repository)

    private fun dateToEpoch(date: LocalDate): Long =
        date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

    private fun emptyStats() = PrayerStats(
        totalPrayed = 0, totalMissed = 0, totalJamaah = 0,
        prayedByPrayer = emptyMap(), missedByPrayer = emptyMap(),
        currentStreak = 0, longestStreak = 0, perfectDays = 0,
        startDate = 0, endDate = 0
    )

    private fun prayerRecord(
        id: Long = 1,
        date: Long,
        prayerName: PrayerName = PrayerName.FAJR,
        status: PrayerStatus = PrayerStatus.MISSED
    ) = PrayerRecord(
        id = id, date = date, prayerName = prayerName, status = status,
        prayedAt = null, scheduledTime = date, isJamaah = false,
        isQadaFor = null, note = null, createdAt = date, updatedAt = date
    )

    // ── Initial state ───────────────────────────────────────────────

    @Test
    fun `initial tracker state selects today and finishes loading`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedDate).isEqualTo(LocalDate.now())
        assertThat(state.isLoading).isFalse()
    }

    // ── Date navigation ─────────────────────────────────────────────

    @Test
    fun `NavigateToNextDay does not advance past today`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.NavigateToNextDay)
        advanceUntilIdle()

        assertThat(viewModel.trackerState.value.selectedDate).isEqualTo(LocalDate.now())
    }

    @Test
    fun `NavigateToPreviousDay then NavigateToNextDay returns to today`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.NavigateToPreviousDay)
        advanceUntilIdle()
        assertThat(viewModel.trackerState.value.selectedDate)
            .isEqualTo(LocalDate.now().minusDays(1))

        viewModel.onEvent(PrayerTrackerEvent.NavigateToNextDay)
        advanceUntilIdle()
        assertThat(viewModel.trackerState.value.selectedDate).isEqualTo(LocalDate.now())
    }

    @Test
    fun `SelectDate updates state and loads that date's records`() = runTest {
        val target = LocalDate.of(2025, 6, 15)
        val epoch = dateToEpoch(target)
        every { repository.getPrayerRecordsForDate(epoch) } returns
            flowOf(listOf(prayerRecord(date = epoch)))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.SelectDate(target))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedDate).isEqualTo(target)
        assertThat(state.prayerRecords).hasSize(1)
        assertThat(state.isLoading).isFalse()
    }

    // ── Status transitions (prayedAt stamping) ──────────────────────

    @Test
    fun `MarkPrayerPrayed records PRAYED with a prayedAt timestamp and jamaah flag`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        val todayEpoch = dateToEpoch(LocalDate.now())

        viewModel.onEvent(PrayerTrackerEvent.MarkPrayerPrayed(PrayerName.FAJR, isJamaah = true))
        advanceUntilIdle()

        coVerify {
            repository.updatePrayerStatus(
                todayEpoch, PrayerName.FAJR, PrayerStatus.PRAYED,
                match { it != null }, true
            )
        }
    }

    @Test
    fun `MarkPrayerMissed records MISSED with no prayedAt timestamp`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        val todayEpoch = dateToEpoch(LocalDate.now())

        viewModel.onEvent(PrayerTrackerEvent.MarkPrayerMissed(PrayerName.ISHA))
        advanceUntilIdle()

        coVerify {
            repository.updatePrayerStatus(
                todayEpoch, PrayerName.ISHA, PrayerStatus.MISSED, null, false
            )
        }
    }

    @Test
    fun `UpdatePrayerStatus stamps prayedAt for LATE but not for PENDING`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        val todayEpoch = dateToEpoch(LocalDate.now())

        viewModel.onEvent(
            PrayerTrackerEvent.UpdatePrayerStatus(PrayerName.DHUHR, PrayerStatus.LATE)
        )
        viewModel.onEvent(
            PrayerTrackerEvent.UpdatePrayerStatus(PrayerName.ASR, PrayerStatus.PENDING)
        )
        advanceUntilIdle()

        coVerify {
            repository.updatePrayerStatus(
                todayEpoch, PrayerName.DHUHR, PrayerStatus.LATE, match { it != null }, false
            )
        }
        coVerify {
            repository.updatePrayerStatus(
                todayEpoch, PrayerName.ASR, PrayerStatus.PENDING, null, false
            )
        }
    }

    @Test
    fun `MarkQadaCompleted marks the record's prayer as QADA using its own date`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val recordDate = dateToEpoch(LocalDate.of(2025, 3, 5))
        val record = prayerRecord(date = recordDate, prayerName = PrayerName.MAGHRIB)

        viewModel.onEvent(PrayerTrackerEvent.MarkQadaCompleted(record))
        advanceUntilIdle()

        coVerify {
            repository.updatePrayerStatus(
                recordDate, PrayerName.MAGHRIB, PrayerStatus.QADA, match { it != null }, false
            )
        }
    }

    // ── Stats period ────────────────────────────────────────────────

    @Test
    fun `SetStatsPeriod updates the active period`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.SetStatsPeriod(StatsPeriod.YEAR))
        advanceUntilIdle()

        assertThat(viewModel.statsState.value.period).isEqualTo(StatsPeriod.YEAR)
        assertThat(viewModel.statsState.value.isLoading).isFalse()
    }

    // ── Qada grouping ───────────────────────────────────────────────

    @Test
    fun `qada prayers are grouped by month and counted`() = runTest {
        val marchRecord = prayerRecord(id = 1, date = dateToEpoch(LocalDate.of(2025, 3, 5)))
        val marchRecord2 = prayerRecord(id = 2, date = dateToEpoch(LocalDate.of(2025, 3, 20)))
        val aprilRecord = prayerRecord(id = 3, date = dateToEpoch(LocalDate.of(2025, 4, 2)))
        every { repository.getMissedPrayersRequiringQada() } returns
            flowOf(listOf(marchRecord, marchRecord2, aprilRecord))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.qadaState.value
        assertThat(state.totalMissed).isEqualTo(3)
        assertThat(state.groupedByMonth.keys).containsExactly("MARCH 2025", "APRIL 2025")
        assertThat(state.groupedByMonth["MARCH 2025"]).hasSize(2)
        assertThat(state.isLoading).isFalse()
    }
}
