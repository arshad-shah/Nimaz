package com.arshadshah.nimaz.presentation.viewmodel

import app.cash.turbine.test
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
import kotlinx.coroutines.flow.MutableSharedFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTrackerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PrayerRepository
    private lateinit var viewModel: PrayerTrackerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        // Defaults so init() does not hang or crash.
        every { repository.getCurrentLocation() } returns flowOf(null)
        every { repository.getPrayerRecordsForDate(any()) } returns flowOf(emptyList())
        every { repository.getMissedPrayersRequiringQada() } returns flowOf(emptyList())
        coEvery { repository.getPrayerStats(any(), any()) } returns prayerStats()
        coEvery { repository.getCurrentStreak(any()) } returns 0
        coEvery { repository.getLongestStreak() } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PrayerTrackerViewModel(repository)

    private fun epochOf(date: LocalDate): Long =
        date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

    private fun prayerStats() = PrayerStats(
        totalPrayed = 0, totalMissed = 0, totalJamaah = 0,
        prayedByPrayer = emptyMap(), missedByPrayer = emptyMap(),
        currentStreak = 0, longestStreak = 0, perfectDays = 0,
        startDate = 0, endDate = 0
    )

    private fun record(
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
    fun `init selects today and finishes loading`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedDate).isEqualTo(LocalDate.now())
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `init loads stats and clears the stats loading flag`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.statsState.value.isLoading).isFalse()
        assertThat(viewModel.statsState.value.stats).isNotNull()
    }

    // ── SelectDate ──────────────────────────────────────────────────

    @Test
    fun `SelectDate updates the date and loads its records`() = runTest {
        val target = LocalDate.of(2025, 6, 10)
        val records = listOf(record(date = epochOf(target), status = PrayerStatus.PRAYED))
        every { repository.getPrayerRecordsForDate(epochOf(target)) } returns flowOf(records)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.SelectDate(target))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedDate).isEqualTo(target)
        assertThat(state.prayerRecords).hasSize(1)
        assertThat(state.isLoading).isFalse()
    }

    // ── Updating prayer status ──────────────────────────────────────

    @Test
    fun `MarkPrayerPrayed records PRAYED with a non-null prayedAt timestamp`() = runTest {
        val target = LocalDate.of(2025, 6, 10)
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(PrayerTrackerEvent.SelectDate(target))
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.MarkPrayerPrayed(PrayerName.FAJR, isJamaah = true))
        advanceUntilIdle()

        coVerify {
            repository.updatePrayerStatus(
                epochOf(target), PrayerName.FAJR, PrayerStatus.PRAYED,
                match { it != null }, true
            )
        }
    }

    @Test
    fun `MarkPrayerMissed records MISSED with a null prayedAt`() = runTest {
        val target = LocalDate.of(2025, 6, 10)
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(PrayerTrackerEvent.SelectDate(target))
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.MarkPrayerMissed(PrayerName.ASR))
        advanceUntilIdle()

        coVerify {
            repository.updatePrayerStatus(
                epochOf(target), PrayerName.ASR, PrayerStatus.MISSED, null, false
            )
        }
    }

    @Test
    fun `updating a prayer status refreshes stats`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.MarkPrayerPrayed(PrayerName.DHUHR))
        advanceUntilIdle()

        // init's loadStats() makes 2 getPrayerStats calls (period + monthly); the
        // post-update refresh must add at least one more.
        coVerify(atLeast = 3) { repository.getPrayerStats(any(), any()) }
    }

    @Test
    fun `MarkQadaCompleted sets the record to QADA status`() = runTest {
        val rec = record(id = 5, date = epochOf(LocalDate.of(2025, 1, 1)), prayerName = PrayerName.ISHA)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.MarkQadaCompleted(rec))
        advanceUntilIdle()

        coVerify {
            repository.updatePrayerStatus(rec.date, PrayerName.ISHA, PrayerStatus.QADA, match { it != null }, false)
        }
    }

    // ── Day navigation ──────────────────────────────────────────────

    @Test
    fun `NavigateToPreviousDay moves the selected date back one day`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.NavigateToPreviousDay)
        advanceUntilIdle()

        assertThat(viewModel.trackerState.value.selectedDate)
            .isEqualTo(LocalDate.now().minusDays(1))
    }

    @Test
    fun `NavigateToNextDay does not advance past today`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Already on today — next day would be the future, so it must stay put.
        viewModel.onEvent(PrayerTrackerEvent.NavigateToNextDay)
        advanceUntilIdle()

        assertThat(viewModel.trackerState.value.selectedDate).isEqualTo(LocalDate.now())
    }

    @Test
    fun `NavigateToNextDay advances when not yet at today`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.SelectDate(LocalDate.now().minusDays(2)))
        advanceUntilIdle()
        viewModel.onEvent(PrayerTrackerEvent.NavigateToNextDay)
        advanceUntilIdle()

        assertThat(viewModel.trackerState.value.selectedDate)
            .isEqualTo(LocalDate.now().minusDays(1))
    }

    // ── Stats period ────────────────────────────────────────────────

    @Test
    fun `SetStatsPeriod updates the period and reloads stats`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.SetStatsPeriod(StatsPeriod.YEAR))
        advanceUntilIdle()

        val state = viewModel.statsState.value
        assertThat(state.period).isEqualTo(StatsPeriod.YEAR)
        assertThat(state.isLoading).isFalse()
    }

    // ── Qada grouping ───────────────────────────────────────────────

    @Test
    fun `qada prayers are grouped by month with a total count`() = runTest {
        val jan = epochOf(LocalDate.of(2025, 1, 5))
        val feb = epochOf(LocalDate.of(2025, 2, 5))
        every { repository.getMissedPrayersRequiringQada() } returns flowOf(
            listOf(
                record(id = 1, date = jan, prayerName = PrayerName.FAJR),
                record(id = 2, date = jan, prayerName = PrayerName.ASR),
                record(id = 3, date = feb, prayerName = PrayerName.ISHA)
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.qadaState.value
        assertThat(state.totalMissed).isEqualTo(3)
        assertThat(state.groupedByMonth.keys).containsExactly("JANUARY 2025", "FEBRUARY 2025")
        assertThat(state.isLoading).isFalse()
    }

    // ── Flow collector cancellation when switching dates ────────────

    @Test
    fun `switching dates cancels the previous date's record collector`() = runTest {
        val dateA = LocalDate.of(2025, 6, 1)
        val dateB = LocalDate.of(2025, 6, 2)
        val flowA = MutableSharedFlow<List<PrayerRecord>>()
        val flowB = MutableSharedFlow<List<PrayerRecord>>()
        every { repository.getPrayerRecordsForDate(epochOf(dateA)) } returns flowA
        every { repository.getPrayerRecordsForDate(epochOf(dateB)) } returns flowB

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTrackerEvent.SelectDate(dateA))
        advanceUntilIdle()
        flowA.emit(listOf(record(id = 1, date = epochOf(dateA))))
        advanceUntilIdle()
        assertThat(viewModel.trackerState.value.prayerRecords).hasSize(1)

        viewModel.onEvent(PrayerTrackerEvent.SelectDate(dateB))
        advanceUntilIdle()
        flowB.emit(
            listOf(
                record(id = 2, date = epochOf(dateB)),
                record(id = 3, date = epochOf(dateB))
            )
        )
        advanceUntilIdle()
        assertThat(viewModel.trackerState.value.prayerRecords).hasSize(2)

        // Late emission from the cancelled collector must be ignored.
        flowA.emit(emptyList())
        advanceUntilIdle()
        assertThat(viewModel.trackerState.value.prayerRecords).hasSize(2)
        assertThat(viewModel.trackerState.value.selectedDate).isEqualTo(dateB)
    }

    // ── trackerState is observable via Turbine ──────────────────────

    @Test
    fun `trackerState emits the selected date change to collectors`() = runTest {
        val target = LocalDate.of(2025, 6, 10)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trackerState.test {
            assertThat(awaitItem().selectedDate).isEqualTo(LocalDate.now())
            viewModel.onEvent(PrayerTrackerEvent.SelectDate(target))
            advanceUntilIdle()
            assertThat(awaitItem().selectedDate).isEqualTo(target)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
