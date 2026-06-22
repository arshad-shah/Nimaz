package com.arshadshah.nimaz.presentation.viewmodel

import app.cash.turbine.test
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.domain.repository.FastingRepository
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
class FastingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FastingRepository
    private lateinit var prayerTimeCalculator: PrayerTimeCalculator
    private lateinit var preferencesDataStore: PreferencesDataStore
    private lateinit var viewModel: FastingViewModel

    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        prayerTimeCalculator = mockk(relaxed = true)
        preferencesDataStore = mockk(relaxed = true)

        // Provide defaults so ViewModel init doesn't crash
        every { preferencesDataStore.latitude } returns flowOf(53.3498)
        every { preferencesDataStore.longitude } returns flowOf(-6.2603)
        coEvery { repository.getFastRecordForDate(any()) } returns null
        every { repository.getFastRecordsInRange(any(), any()) } returns flowOf(emptyList())
        every { repository.getPendingMakeupFasts() } returns flowOf(emptyList())
        every { repository.getAllMakeupFasts() } returns flowOf(emptyList())
        coEvery { repository.getFastingStats(any(), any()) } returns FastingStats(
            totalFasted = 0, ramadanFasted = 0, voluntaryFasted = 0,
            pendingMakeupCount = 0, totalFidyaPaid = 0.0,
            currentStreak = 0, startDate = 0, endDate = 0
        )
        coEvery { repository.getRamadanFastedCount() } returns 0
        coEvery { repository.getVoluntaryFastCount() } returns 0
        coEvery { repository.getTotalFidyaPaid() } returns 0.0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FastingViewModel {
        return FastingViewModel(buildFastingUseCases(repository), prayerTimeCalculator, preferencesDataStore)
    }

    private fun dateToEpoch(date: LocalDate): Long {
        return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
    }

    private fun createFastRecord(
        id: Long = 1,
        date: Long = now,
        fastType: FastType = FastType.RAMADAN,
        status: FastStatus = FastStatus.FASTED
    ) = FastRecord(
        id = id, date = date, hijriDate = "1/9/1446",
        hijriMonth = 9, hijriYear = 1446,
        fastType = fastType, status = status,
        exemptionReason = null, suhoorTime = null, iftarTime = null,
        note = null, createdAt = now, updatedAt = now
    )

    // ── Initial state ───────────────────────────────────────────────

    @Test
    fun `initial tracker state has loading then completes`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.selectedDate).isEqualTo(LocalDate.now())
    }

    @Test
    fun `initial calendar state loads current month`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.calendarState.value
        assertThat(state.selectedMonth).isEqualTo(LocalDate.now().monthValue)
        assertThat(state.selectedYear).isEqualTo(LocalDate.now().year)
        assertThat(state.isLoading).isFalse()
    }

    // ── SelectDate event ────────────────────────────────────────────

    @Test
    fun `SelectDate updates selected date and loads record`() = runTest {
        val targetDate = LocalDate.of(2025, 6, 15)
        val epoch = dateToEpoch(targetDate)
        val record = createFastRecord(date = epoch)

        coEvery { repository.getFastRecordForDate(epoch) } returns record

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectDate(targetDate))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedDate).isEqualTo(targetDate)
        assertThat(state.todayRecord).isNotNull()
        assertThat(state.isFastingToday).isTrue()
    }

    @Test
    fun `SelectDate with no record sets todayRecord to null`() = runTest {
        val targetDate = LocalDate.of(2025, 6, 15)
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectDate(targetDate))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.todayRecord).isNull()
        assertThat(state.isFastingToday).isFalse()
    }

    // ── StartFast event ─────────────────────────────────────────────

    @Test
    fun `StartFast inserts record via repository`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.of(2025, 6, 15)
        viewModel.onEvent(FastingEvent.StartFast(date, FastType.VOLUNTARY))
        advanceUntilIdle()

        coVerify {
            repository.insertFastRecord(match { record ->
                record.fastType == FastType.VOLUNTARY &&
                record.status == FastStatus.FASTED
            })
        }
    }

    // ── CompleteFast / BreakFast events ─────────────────────────────

    @Test
    fun `CompleteFast updates status to FASTED`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.of(2025, 6, 15)
        viewModel.onEvent(FastingEvent.CompleteFast(date))
        advanceUntilIdle()

        coVerify { repository.updateFastStatus(dateToEpoch(date), FastStatus.FASTED) }
    }

    @Test
    fun `BreakFast updates status to NOT_FASTED`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.of(2025, 6, 15)
        viewModel.onEvent(FastingEvent.BreakFast(date))
        advanceUntilIdle()

        coVerify { repository.updateFastStatus(dateToEpoch(date), FastStatus.NOT_FASTED) }
    }

    // ── ToggleTodayFast event ───────────────────────────────────────

    @Test
    fun `ToggleTodayFast starts fast when no existing record`() = runTest {
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.ToggleTodayFast)
        advanceUntilIdle()

        coVerify { repository.insertFastRecord(any()) }
    }

    @Test
    fun `ToggleTodayFast breaks fast when currently fasted`() = runTest {
        val todayEpoch = dateToEpoch(LocalDate.now())
        val record = createFastRecord(date = todayEpoch, status = FastStatus.FASTED)
        coEvery { repository.getFastRecordForDate(todayEpoch) } returns record

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.ToggleTodayFast)
        advanceUntilIdle()

        coVerify { repository.updateFastStatus(todayEpoch, FastStatus.NOT_FASTED) }
    }

    // ── SelectMonth event ───────────────────────────────────────────

    @Test
    fun `SelectMonth updates calendar state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectMonth(3, 2025))
        advanceUntilIdle()

        val state = viewModel.calendarState.value
        assertThat(state.selectedMonth).isEqualTo(3)
        assertThat(state.selectedYear).isEqualTo(2025)
    }

    @Test
    fun `SelectMonth queries correct date range`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectMonth(3, 2025))
        advanceUntilIdle()

        // March 2025: start = March 1, end = April 1 (exclusive)
        val marchStart = dateToEpoch(LocalDate.of(2025, 3, 1))
        val aprilStart = dateToEpoch(LocalDate.of(2025, 4, 1))

        coVerify { repository.getFastRecordsInRange(marchStart, aprilStart) }
    }

    // ── Calendar data loading with records ──────────────────────────

    @Test
    fun `calendar state includes records from repository`() = runTest {
        val records = listOf(
            createFastRecord(id = 1, date = dateToEpoch(LocalDate.of(2025, 3, 5))),
            createFastRecord(id = 2, date = dateToEpoch(LocalDate.of(2025, 3, 10)))
        )
        every { repository.getFastRecordsInRange(any(), any()) } returns flowOf(records)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Switch to March 2025
        viewModel.onEvent(FastingEvent.SelectMonth(3, 2025))
        advanceUntilIdle()

        val state = viewModel.calendarState.value
        assertThat(state.records).hasSize(2)
    }

    // ── SetFastType event ───────────────────────────────────────────

    @Test
    fun `SetFastType updates selected fast type`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetFastType(FastType.VOLUNTARY))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedFastType).isEqualTo(FastType.VOLUNTARY)
    }

    // ── SaveFastForDate event ───────────────────────────────────────

    @Test
    fun `SaveFastForDate inserts new record when none exists`() = runTest {
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.of(2025, 6, 15)
        viewModel.onEvent(
            FastingEvent.SaveFastForDate(
                date = date,
                status = FastStatus.FASTED,
                fastType = FastType.VOLUNTARY,
                exemptionReason = null,
                note = "test note"
            )
        )
        advanceUntilIdle()

        coVerify { repository.insertFastRecord(any()) }
    }

    @Test
    fun `SaveFastForDate updates existing record`() = runTest {
        val date = LocalDate.of(2025, 6, 15)
        val epoch = dateToEpoch(date)
        val existing = createFastRecord(id = 5, date = epoch)
        coEvery { repository.getFastRecordForDate(epoch) } returns existing

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(
            FastingEvent.SaveFastForDate(
                date = date,
                status = FastStatus.NOT_FASTED,
                fastType = FastType.RAMADAN,
                exemptionReason = null,
                note = ""
            )
        )
        advanceUntilIdle()

        coVerify { repository.updateFastRecord(match { it.id == 5L }) }
    }

    @Test
    fun `SaveFastForDate creates makeup fast for missed Ramadan day`() = runTest {
        val date = LocalDate.of(2025, 3, 5) // During Ramadan 1446
        val epoch = dateToEpoch(date)
        coEvery { repository.getFastRecordForDate(epoch) } returns null
        coEvery { repository.getMakeupFastCountForDate(epoch) } returns 0

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(
            FastingEvent.SaveFastForDate(
                date = date,
                status = FastStatus.NOT_FASTED,
                fastType = FastType.RAMADAN,
                exemptionReason = null,
                note = ""
            )
        )
        advanceUntilIdle()

        coVerify { repository.insertMakeupFast(any()) }
    }

    @Test
    fun `SaveFastForDate does not create duplicate makeup fast`() = runTest {
        val date = LocalDate.of(2025, 3, 5)
        val epoch = dateToEpoch(date)
        coEvery { repository.getFastRecordForDate(epoch) } returns null
        coEvery { repository.getMakeupFastCountForDate(epoch) } returns 1 // already exists

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(
            FastingEvent.SaveFastForDate(
                date = date,
                status = FastStatus.NOT_FASTED,
                fastType = FastType.RAMADAN,
                exemptionReason = null,
                note = ""
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertMakeupFast(any()) }
    }

    // ── DeleteFastRecord event ──────────────────────────────────────

    @Test
    fun `DeleteFastRecord calls repository and dismisses sheet`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.of(2025, 6, 15)
        viewModel.onEvent(FastingEvent.DeleteFastRecord(date))
        advanceUntilIdle()

        coVerify { repository.deleteFastRecordByDate(dateToEpoch(date)) }

        val sheetState = viewModel.sheetState.value
        assertThat(sheetState.isVisible).isFalse()
    }

    // ── OpenFastSheet / DismissFastSheet ────────────────────────────

    @Test
    fun `OpenFastSheet makes sheet visible with correct date`() = runTest {
        val date = LocalDate.of(2025, 6, 15)
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.OpenFastSheet(date))
        advanceUntilIdle()

        val sheetState = viewModel.sheetState.value
        assertThat(sheetState.isVisible).isTrue()
        assertThat(sheetState.date).isEqualTo(date)
    }

    @Test
    fun `DismissFastSheet hides the sheet`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Open then dismiss
        viewModel.onEvent(FastingEvent.OpenFastSheet(LocalDate.now()))
        advanceUntilIdle()
        viewModel.onEvent(FastingEvent.DismissFastSheet)
        advanceUntilIdle()

        assertThat(viewModel.sheetState.value.isVisible).isFalse()
    }

    // ── Stats period ────────────────────────────────────────────────

    @Test
    fun `SetStatsPeriod updates period and reloads stats`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetStatsPeriod(FastingStatsPeriod.ALL_TIME))
        advanceUntilIdle()

        val state = viewModel.statsState.value
        assertThat(state.period).isEqualTo(FastingStatsPeriod.ALL_TIME)
        assertThat(state.isLoading).isFalse()
    }

    // ── Job cancellation (the bug fix) ──────────────────────────────

    @Test
    fun `switching months cancels previous calendar Flow collector`() = runTest {
        // Use MutableSharedFlow to control emissions manually
        val marchFlow = MutableSharedFlow<List<FastRecord>>()
        val aprilFlow = MutableSharedFlow<List<FastRecord>>()

        val marchStart = dateToEpoch(LocalDate.of(2025, 3, 1))
        val aprilStart = dateToEpoch(LocalDate.of(2025, 4, 1))
        val mayStart = dateToEpoch(LocalDate.of(2025, 5, 1))

        every { repository.getFastRecordsInRange(marchStart, aprilStart) } returns marchFlow
        every { repository.getFastRecordsInRange(aprilStart, mayStart) } returns aprilFlow

        viewModel = createViewModel()
        advanceUntilIdle()

        // Load March
        viewModel.onEvent(FastingEvent.SelectMonth(3, 2025))
        advanceUntilIdle()

        // Emit March data
        val marchRecords = listOf(createFastRecord(id = 1, date = marchStart))
        marchFlow.emit(marchRecords)
        advanceUntilIdle()
        assertThat(viewModel.calendarState.value.records).hasSize(1)

        // Switch to April — this should cancel the March collector
        viewModel.onEvent(FastingEvent.SelectMonth(4, 2025))
        advanceUntilIdle()

        // Emit April data
        val aprilRecords = listOf(
            createFastRecord(id = 2, date = aprilStart),
            createFastRecord(id = 3, date = aprilStart + 86400000)
        )
        aprilFlow.emit(aprilRecords)
        advanceUntilIdle()
        assertThat(viewModel.calendarState.value.records).hasSize(2)

        // Now emit from March flow again — should NOT overwrite April data
        // because the March collector should have been cancelled
        marchFlow.emit(emptyList())
        advanceUntilIdle()

        // April data should still be intact
        assertThat(viewModel.calendarState.value.records).hasSize(2)
        assertThat(viewModel.calendarState.value.selectedMonth).isEqualTo(4)
    }
}
