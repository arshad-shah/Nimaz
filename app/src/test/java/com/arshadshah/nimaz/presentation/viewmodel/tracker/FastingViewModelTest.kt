package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.fasting.CountUnloggedRamadanDaysUseCase
import com.arshadshah.nimaz.domain.usecase.fasting.GetDaysUntilAyyamAlBeedUseCase
import com.arshadshah.nimaz.domain.usecase.fasting.GetRamadanCountdownUseCase
import com.arshadshah.nimaz.domain.repository.FakeHijriSettings
import com.arshadshah.nimaz.domain.repository.FakeZakatSettings
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry

import app.cash.turbine.test
import com.arshadshah.nimaz.domain.repository.SettingsRepository
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
import com.arshadshah.nimaz.presentation.viewmodel.buildFastingUseCases
import com.arshadshah.nimaz.presentation.viewmodel.buildPrayerUseCases
import com.arshadshah.nimaz.presentation.viewmodel.FakePrayerTimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class FastingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FastingRepository
    private val prayers = FakePrayerTimetableRepository()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: FastingViewModel

    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // Provide defaults so ViewModel init doesn't crash
        every { settingsRepository.latitude } returns flowOf(53.3498)
        every { settingsRepository.longitude } returns flowOf(-6.2603)
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
        return FastingViewModel(
            buildFastingUseCases(repository),
            buildPrayerUseCases(prayers),
            FakeTodayProvider(LocalDate.now()),
            GetDaysUntilAyyamAlBeedUseCase(FakeTodayProvider(LocalDate.now())),
            GetRamadanCountdownUseCase(FakeTodayProvider(LocalDate.now())),
            CountUnloggedRamadanDaysUseCase(FakeTodayProvider(LocalDate.now())),
            FakeHijriSettings(),
            FakeZakatSettings(),
            RecordingTelemetry(),
        )
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

    // ── The selected day is a real day, not a relabelled today ──────

    @Test
    fun `SelectDate loads that day's record into selectedRecord`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        val epoch = dateToEpoch(target)
        coEvery { repository.getFastRecordForDate(epoch) } returns
            createFastRecord(date = epoch, status = FastStatus.FASTED)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectDate(target))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedRecord?.status).isEqualTo(FastStatus.FASTED)
        assertThat(state.isSelectedToday).isFalse()
    }

    @Test
    fun `SelectDate on today marks the selection as today`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectDate(LocalDate.now()))
        advanceUntilIdle()

        assertThat(viewModel.trackerState.value.isSelectedToday).isTrue()
    }

    @Test
    fun `SelectDate loads the Monday-to-Sunday week around the selection`() = runTest {
        // 2026-08-12 is a Wednesday, so its week runs Mon 10 Aug to Sun 16 Aug. The query is
        // half-open, so it ends at Mon 17 Aug.
        val wednesday = LocalDate.of(2026, 8, 12)
        val weekStart = dateToEpoch(LocalDate.of(2026, 8, 10))
        val weekEndExclusive = dateToEpoch(LocalDate.of(2026, 8, 17))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectDate(wednesday))
        advanceUntilIdle()

        coVerify { repository.getFastRecordsInRange(weekStart, weekEndExclusive) }
    }

    @Test
    fun `the week query spans two months when the week straddles a boundary`() = runTest {
        // 2026-09-02 is a Wednesday; its week starts Mon 31 Aug — in the previous month, which
        // is precisely what a single-month calendar query cannot cover.
        val wednesday = LocalDate.of(2026, 9, 2)
        val weekStart = dateToEpoch(LocalDate.of(2026, 8, 31))
        val weekEndExclusive = dateToEpoch(LocalDate.of(2026, 9, 7))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectDate(wednesday))
        advanceUntilIdle()

        coVerify { repository.getFastRecordsInRange(weekStart, weekEndExclusive) }
    }

    @Test
    fun `SelectDate exposes the week's records`() = runTest {
        val monday = dateToEpoch(LocalDate.of(2026, 8, 10))
        every { repository.getFastRecordsInRange(any(), any()) } returns flowOf(
            listOf(
                createFastRecord(id = 1, date = monday),
                createFastRecord(id = 2, date = monday + 86_400_000),
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SelectDate(LocalDate.of(2026, 8, 12)))
        advanceUntilIdle()

        assertThat(viewModel.trackerState.value.weekRecords).hasSize(2)
    }

    @Test
    fun `SelectDate loads that date's own suhoor and iftar, not today's`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Six months away, so the two schedules cannot coincide by accident.
        viewModel.onEvent(FastingEvent.SelectDate(LocalDate.now().plusMonths(6)))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedSuhoorAt).isNotNull()
        assertThat(state.selectedIftarAt).isNotNull()
        assertThat(state.selectedSuhoorAt).isNotEqualTo(state.suhoorAt)
        assertThat(state.selectedIftarAt).isNotEqualTo(state.iftarAt)
    }

    // ── SetFastStatus ───────────────────────────────────────────────

    @Test
    fun `SetFastStatus writes a record for that day`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetFastStatus(target, FastStatus.FASTED))
        advanceUntilIdle()

        coVerify {
            repository.insertFastRecord(
                match { it.status == FastStatus.FASTED && it.date == dateToEpoch(target) }
            )
        }
    }

    @Test
    fun `SetFastStatus with the status a day already has clears the record`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        val epoch = dateToEpoch(target)
        coEvery { repository.getFastRecordForDate(epoch) } returns
            createFastRecord(date = epoch, status = FastStatus.FASTED)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetFastStatus(target, FastStatus.FASTED))
        advanceUntilIdle()

        coVerify { repository.deleteFastRecordByDate(epoch) }
        coVerify(exactly = 0) { repository.insertFastRecord(any()) }
    }

    @Test
    fun `SetFastStatus to a different status updates rather than clears`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        val epoch = dateToEpoch(target)
        coEvery { repository.getFastRecordForDate(epoch) } returns
            createFastRecord(id = 7, date = epoch, status = FastStatus.FASTED)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetFastStatus(target, FastStatus.NOT_FASTED))
        advanceUntilIdle()

        coVerify { repository.updateFastRecord(match { it.status == FastStatus.NOT_FASTED }) }
        coVerify(exactly = 0) { repository.deleteFastRecordByDate(any()) }
    }

    @Test
    fun `an ordinary day is written as a voluntary fast`() = runTest {
        // 15 Aug 2026 falls in Safar 1448 — nowhere near Ramadan.
        val ordinary = LocalDate.of(2026, 8, 15)
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetFastStatus(ordinary, FastStatus.FASTED))
        advanceUntilIdle()

        coVerify {
            repository.insertFastRecord(match { it.fastType == FastType.VOLUNTARY })
        }
    }

    @Test
    fun `a Ramadan day is written as a Ramadan fast without being asked`() = runTest {
        // 5 March 2025 is inside Ramadan 1446.
        val ramadanDay = LocalDate.of(2025, 3, 5)
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetFastStatus(ramadanDay, FastStatus.FASTED))
        advanceUntilIdle()

        coVerify {
            repository.insertFastRecord(match { it.fastType == FastType.RAMADAN })
        }
    }

    @Test
    fun `a missed Ramadan day still auto-creates its makeup fast`() = runTest {
        val ramadanDay = LocalDate.of(2025, 3, 5)
        val epoch = dateToEpoch(ramadanDay)
        coEvery { repository.getFastRecordForDate(epoch) } returns null
        coEvery { repository.getMakeupFastCountForDate(epoch) } returns 0

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetFastStatus(ramadanDay, FastStatus.NOT_FASTED))
        advanceUntilIdle()

        coVerify { repository.insertMakeupFast(any()) }
    }

    // ── SaveExemption ───────────────────────────────────────────────

    @Test
    fun `SaveExemption stores the reason against an exempted day`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SaveExemption(target, ExemptionReason.TRAVEL))
        advanceUntilIdle()

        coVerify {
            repository.insertFastRecord(
                match {
                    it.status == FastStatus.EXEMPTED &&
                        it.exemptionReason == ExemptionReason.TRAVEL
                }
            )
        }
    }

    @Test
    fun `SaveExemption keeps a note already on the day`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        val epoch = dateToEpoch(target)
        coEvery { repository.getFastRecordForDate(epoch) } returns
            createFastRecord(id = 3, date = epoch).copy(note = "kept")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SaveExemption(target, ExemptionReason.ILLNESS))
        advanceUntilIdle()

        coVerify { repository.updateFastRecord(match { it.note == "kept" }) }
    }

    // ── SaveNote ────────────────────────────────────────────────────

    @Test
    fun `SaveNote keeps the day's existing status`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        val epoch = dateToEpoch(target)
        coEvery { repository.getFastRecordForDate(epoch) } returns
            createFastRecord(id = 4, date = epoch, status = FastStatus.FASTED)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SaveNote(target, "felt easy"))
        advanceUntilIdle()

        coVerify {
            repository.updateFastRecord(
                match { it.status == FastStatus.FASTED && it.note == "felt easy" }
            )
        }
    }

    @Test
    fun `SaveNote on an unlogged day does not invent a fasted status`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        coEvery { repository.getFastRecordForDate(any()) } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SaveNote(target, "planning to fast"))
        advanceUntilIdle()

        coVerify {
            repository.insertFastRecord(match { it.status == FastStatus.NOT_FASTED })
        }
    }

    @Test
    fun `a blank note clears rather than storing an empty string`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        val epoch = dateToEpoch(target)
        coEvery { repository.getFastRecordForDate(epoch) } returns
            createFastRecord(id = 6, date = epoch).copy(note = "old")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SaveNote(target, "   "))
        advanceUntilIdle()

        coVerify { repository.updateFastRecord(match { it.note == null }) }
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
