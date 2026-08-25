package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.repository.FakeHijriSettings
import com.arshadshah.nimaz.domain.repository.FakeZakatSettings
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.FakePrayerTimetableRepository
import com.arshadshah.nimaz.domain.usecase.buildFastingUseCases
import com.arshadshah.nimaz.domain.usecase.buildPrayerUseCases
import com.arshadshah.nimaz.domain.usecase.fasting.CountUnloggedRamadanDaysUseCase
import com.arshadshah.nimaz.domain.usecase.fasting.GetDaysUntilAyyamAlBeedUseCase
import com.arshadshah.nimaz.domain.usecase.fasting.GetRamadanCountdownUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

/**
 * Statistics windows, and the debt a missed Ramadan fast creates.
 *
 * Two things here write to the user's record in ways they cannot undo from the screen that
 * caused them:
 *
 *  - **A missed Ramadan day creates a make-up fast**, and creates *one*. The write path runs on
 *    every status change, so without the count guard, changing a Ramadan day's status back and
 *    forth would leave a make-up fast per tap — a debt of five for one missed day, with no way
 *    to tell which is real.
 *  - **An exemption's reason survives only while the day stays exempt.** Switching a day from
 *    exempt to not-fasted must drop "Travel" with it; carrying it forward would attach a reason
 *    to a claim the user never made, and the reason is what the make-up fast is filed under.
 *
 * The statistics windows are the third: three periods, three spans, and the only thing
 * distinguishing them is the pair of epochs the query is handed. A `when` arm that returned the
 * wrong span would show one period's numbers under another's label.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FastingStatsAndDebtTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 8, 13)

    private lateinit var repository: FastingRepository
    private val prayers = FakePrayerTimetableRepository()

    private fun record(
        date: LocalDate,
        status: FastStatus,
        type: FastType = FastType.VOLUNTARY,
        reason: ExemptionReason? = null,
        note: String? = null,
    ) = FastRecord(
        id = 1L,
        date = date.toUtcMidnightMillis(),
        hijriDate = null,
        hijriMonth = null,
        hijriYear = null,
        fastType = type,
        status = status,
        exemptionReason = reason,
        suhoorTime = null,
        iftarTime = null,
        note = note,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk(relaxed = true)

        coEvery { repository.getFastRecordForDate(any()) } returns null
        every { repository.getFastRecordsInRange(any(), any()) } returns flowOf(emptyList())
        every { repository.getPendingMakeupFasts() } returns flowOf(emptyList())
        every { repository.getAllMakeupFasts() } returns flowOf(emptyList())
        coEvery { repository.getFastingStats(any(), any()) } returns FastingStats(
            totalFasted = 0, ramadanFasted = 0, voluntaryFasted = 0,
            pendingMakeupCount = 0, totalFidyaPaid = 0.0,
            currentStreak = 0, startDate = 0, endDate = 0,
        )
        coEvery { repository.getRamadanFastedCount() } returns 0
        coEvery { repository.getVoluntaryFastCount() } returns 0
        coEvery { repository.getTotalFidyaPaid() } returns 0.0
        coEvery { repository.getMakeupFastCountForDate(any()) } returns 0
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): FastingViewModel {
        val provider = FakeTodayProvider(today)
        return FastingViewModel(
            buildFastingUseCases(repository),
            buildPrayerUseCases(prayers),
            provider,
            GetDaysUntilAyyamAlBeedUseCase(provider),
            GetRamadanCountdownUseCase(provider),
            CountUnloggedRamadanDaysUseCase(provider),
            FakeHijriSettings(),
            FakeZakatSettings(),
            RecordingTelemetry(),
        )
    }

    /**
     * The (start, end) epoch pair the stats query was last handed.
     *
     * A `mutableList` rather than a `slot`, because the query runs on init as well as on every
     * period change — mockk refuses to capture into a slot across more than one verified call.
     */
    private fun capturedStatsWindow(): Pair<Long, Long> {
        val starts = mutableListOf<Long>()
        val ends = mutableListOf<Long>()
        coVerify { repository.getFastingStats(capture(starts), capture(ends)) }
        return starts.last() to ends.last()
    }

    @Test
    fun `this month asks for the month so far`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(FastingEvent.SetStatsPeriod(FastingStatsPeriod.THIS_MONTH))
        advanceUntilIdle()

        assertThat(vm.statsState.value.period).isEqualTo(FastingStatsPeriod.THIS_MONTH)
        assertThat(capturedStatsWindow().first)
            .isEqualTo(today.withDayOfMonth(1).toUtcMidnightMillis())
    }

    @Test
    fun `this year asks from the first of January`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(FastingEvent.SetStatsPeriod(FastingStatsPeriod.THIS_YEAR))
        advanceUntilIdle()

        assertThat(capturedStatsWindow().first)
            .isEqualTo(today.withDayOfYear(1).toUtcMidnightMillis())
    }

    @Test
    fun `all time reaches back a decade`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(FastingEvent.SetStatsPeriod(FastingStatsPeriod.ALL_TIME))
        advanceUntilIdle()

        val (start, end) = capturedStatsWindow()
        assertThat(start).isEqualTo(today.minusYears(10).toUtcMidnightMillis())
        // The end is exclusive — tomorrow's midnight — so today's own fast is inside every
        // window rather than falling off the end of all of them.
        assertThat(end).isEqualTo(today.plusDays(1).toUtcMidnightMillis())
    }

    @Test
    fun `reloading statistics keeps the period the user chose`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(FastingEvent.SetStatsPeriod(FastingStatsPeriod.THIS_MONTH))
        advanceUntilIdle()

        vm.onEvent(FastingEvent.LoadStats)
        advanceUntilIdle()

        assertThat(vm.statsState.value.period).isEqualTo(FastingStatsPeriod.THIS_MONTH)
        assertThat(vm.statsState.value.isLoading).isFalse()
    }

    @Test
    fun `setting the status a day already has deletes the record`() = runTest {
        val yesterday = today.minusDays(1)
        coEvery {
            repository.getFastRecordForDate(yesterday.toUtcMidnightMillis())
        } returns record(yesterday, FastStatus.FASTED)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(FastingEvent.SetFastStatus(yesterday, FastStatus.FASTED))
        advanceUntilIdle()

        // Tap-to-clear: the segmented control has no "unset" cell, so choosing what the day
        // already says is the only way to withdraw the claim.
        coVerify { repository.deleteFastRecordByDate(yesterday.toUtcMidnightMillis()) }
    }

    @Test
    fun `an exemption reason is dropped when the day stops being exempt`() = runTest {
        val yesterday = today.minusDays(1)
        coEvery {
            repository.getFastRecordForDate(yesterday.toUtcMidnightMillis())
        } returns record(yesterday, FastStatus.EXEMPTED, reason = ExemptionReason.TRAVEL)

        val written = slot<FastRecord>()
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(FastingEvent.SetFastStatus(yesterday, FastStatus.NOT_FASTED))
        advanceUntilIdle()

        coVerify { repository.updateFastRecord(capture(written)) }
        // "Travel" is a reason for being exempt, not for not fasting. Carrying it forward would
        // attach a justification to a claim the user never made — and it is the string the
        // make-up fast is filed under.
        assertThat(written.captured.status).isEqualTo(FastStatus.NOT_FASTED)
        assertThat(written.captured.exemptionReason).isNull()
    }

    @Test
    fun `a note survives a status change`() = runTest {
        val yesterday = today.minusDays(1)
        coEvery {
            repository.getFastRecordForDate(yesterday.toUtcMidnightMillis())
        } returns record(yesterday, FastStatus.FASTED, note = "Kept it")

        val written = slot<FastRecord>()
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(FastingEvent.SetFastStatus(yesterday, FastStatus.NOT_FASTED))
        advanceUntilIdle()

        coVerify { repository.updateFastRecord(capture(written)) }
        // Unlike the reason, a note is the user's own words about the day and applies whatever
        // they later say about the fast.
        assertThat(written.captured.note).isEqualTo("Kept it")
    }

    @Test
    fun `a missed Ramadan day creates the make-up fast it owes`() = runTest {
        val ramadanDay = ramadanDay()
        val makeup = slot<MakeupFast>()

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(FastingEvent.SetFastStatus(ramadanDay, FastStatus.NOT_FASTED))
        advanceUntilIdle()

        coVerify { repository.insertMakeupFast(capture(makeup)) }
        assertThat(makeup.captured.originalDate).isEqualTo(ramadanDay.toUtcMidnightMillis())
        // Filed under a Hijri date, because a Ramadan fast is owed for a day of Ramadan and that
        // is what the make-up list shows.
        assertThat(makeup.captured.originalHijriDate).isNotEmpty()
    }

    @Test
    fun `a Ramadan day already carrying a debt does not gain a second one`() = runTest {
        coEvery { repository.getMakeupFastCountForDate(any()) } returns 1

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(FastingEvent.SetFastStatus(ramadanDay(), FastStatus.NOT_FASTED))
        advanceUntilIdle()

        // The write path runs on every status change. Without this guard, toggling a Ramadan
        // day back and forth would leave one make-up fast per tap.
        coVerify(exactly = 0) { repository.insertMakeupFast(any()) }
    }

    @Test
    fun `a missed day outside Ramadan owes nothing`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(FastingEvent.SetFastStatus(today.minusDays(1), FastStatus.NOT_FASTED))
        advanceUntilIdle()

        // A voluntary fast not kept is not a debt — only a Ramadan fast is owed back.
        coVerify(exactly = 0) { repository.insertMakeupFast(any()) }
    }

    @Test
    fun `completing a make-up fast and paying fidya both reach the repository`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(FastingEvent.CompleteMakeupFast(7L))
        vm.onEvent(FastingEvent.PayFidya(8L, 12.5))
        advanceUntilIdle()

        // The two doors out of a debt, and they must not be each other: one records a fast, the
        // other records money.
        coVerify { repository.markMakeupFastCompleted(7L, any()) }
        coVerify { repository.markFidyaPaid(8L, 12.5) }
    }

    @Test
    fun `editing a make-up fast writes it back unchanged but for the edit`() = runTest {
        val edited = MakeupFast(
            id = 9L,
            originalDate = 1_700_000_000_000L,
            originalHijriDate = "3 Ramadan 1445",
            reason = "Hospital stay",
            status = com.arshadshah.nimaz.domain.model.MakeupFastStatus.PENDING,
            completedDate = null,
            fidyaAmount = null,
            note = null,
            createdAt = 1L,
            updatedAt = 2L,
        )
        val written = slot<MakeupFast>()

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(FastingEvent.UpdateMakeupFast(edited))
        advanceUntilIdle()

        coVerify { repository.updateMakeupFast(capture(written)) }
        assertThat(written.captured.id).isEqualTo(9L)
        assertThat(written.captured.reason).isEqualTo("Hospital stay")
    }

    /** A real day of Ramadan, asked of the calculator rather than hardcoded. */
    private fun ramadanDay(): LocalDate =
        com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
            .getFirstDayOfRamadan(
                com.arshadshah.nimaz.domain.calendar.HijriDateCalculator.toHijri(today).year
            )
            .plusDays(2)
}
