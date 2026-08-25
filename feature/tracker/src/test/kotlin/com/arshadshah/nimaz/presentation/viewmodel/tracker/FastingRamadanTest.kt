package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
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

/**
 * Ramadan, from inside it — the arm of `loadRamadan` that only runs one month a year.
 *
 * Everything the fasting screen shows during Ramadan is decided here: the day number, the
 * progress denominator, how many days are fasted, how many are missed, and how many have gone
 * by with nothing recorded at all. The rest of the year the `else` arm runs instead, and that is
 * the arm every other test in this module exercises — so this whole branch has, until now, been
 * reachable only by waiting for Ramadan.
 *
 * The distinction the counts turn on is the one the redesign is built around: **missed** means a
 * day explicitly recorded as not fasted, and **unlogged** means a day nobody has said anything
 * about. Conflating them would have the app tell a user they missed days they simply had not got
 * round to logging — an accusation, from a fasting tracker, during Ramadan.
 *
 * "Today" is supplied through `TodayProvider` and the Ramadan dates come from
 * `HijriDateCalculator`, so the test asks the calculator when Ramadan is rather than hardcoding
 * a Gregorian date that would stop being in Ramadan next year.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FastingRamadanTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FastingRepository
    private val prayers = FakePrayerTimetableRepository()

    /** The tenth of the Ramadan whose year contains today — always a real Ramadan day. */
    private val ramadanYear = HijriDateCalculator.toHijri(LocalDate.now()).year + 1
    private val firstDay: LocalDate = HijriDateCalculator.getFirstDayOfRamadan(ramadanYear)
    private val dayTen: LocalDate = firstDay.plusDays(9)

    private fun fasted(date: LocalDate, status: FastStatus) = FastRecord(
        id = date.toEpochDay(),
        date = date.toUtcMidnightMillis(),
        hijriDate = null,
        hijriMonth = 9,
        hijriYear = ramadanYear,
        fastType = FastType.RAMADAN,
        status = status,
        exemptionReason = null,
        suhoorTime = null,
        iftarTime = null,
        note = null,
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
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(today: LocalDate): FastingViewModel {
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

    @Test
    fun `inside Ramadan the state says so and names the day`() = runTest {
        val vm = viewModel(dayTen)
        advanceUntilIdle()

        val state = vm.ramadanState.value
        assertThat(state.isRamadan).isTrue()
        assertThat(state.currentDay).isEqualTo(10)
        assertThat(state.isLoading).isFalse()
        // Days left in the month, not days left in the fast — the banner's denominator is
        // fasted + missed + remaining, and it has to add up to the month.
        assertThat(state.currentDay + state.remainingDays)
            .isEqualTo(HijriDateCalculator.getDaysInHijriMonth(ramadanYear, 9))
    }

    @Test
    fun `fasted and missed count only what was actually recorded`() = runTest {
        every { repository.getFastRecordsInRange(any(), any()) } returns flowOf(
            listOf(
                fasted(firstDay, FastStatus.FASTED),
                fasted(firstDay.plusDays(1), FastStatus.FASTED),
                fasted(firstDay.plusDays(2), FastStatus.NOT_FASTED),
                fasted(firstDay.plusDays(3), FastStatus.EXEMPTED),
                fasted(firstDay.plusDays(4), FastStatus.MAKEUP_DUE),
            )
        )

        val vm = viewModel(dayTen)
        advanceUntilIdle()

        val state = vm.ramadanState.value
        assertThat(state.fastedDays).isEqualTo(2)
        // An exempted day and a day owed are neither fasted nor missed — a user who was ill for
        // a week has not "missed" seven fasts, and being told they did is the wrong reading of
        // their own record.
        assertThat(state.missedDays).isEqualTo(1)
        assertThat(state.ramadanRecords).hasSize(5)
    }

    @Test
    fun `days gone by with no record at all are counted separately from missed`() = runTest {
        every { repository.getFastRecordsInRange(any(), any()) } returns flowOf(
            listOf(fasted(firstDay, FastStatus.FASTED))
        )

        val vm = viewModel(dayTen)
        advanceUntilIdle()

        val state = vm.ramadanState.value
        // Today is day ten and still in progress, so nine days have elapsed; one of them is
        // logged, leaving eight unlogged — and none of them is "missed", because nobody has
        // said so.
        assertThat(state.unloggedDays).isEqualTo(8)
        assertThat(state.missedDays).isEqualTo(0)
    }

    @Test
    fun `a fully logged Ramadan so far has nothing unlogged`() = runTest {
        every { repository.getFastRecordsInRange(any(), any()) } returns flowOf(
            (0L until 10L).map { fasted(firstDay.plusDays(it), FastStatus.FASTED) }
        )

        val vm = viewModel(dayTen)
        advanceUntilIdle()

        assertThat(vm.ramadanState.value.unloggedDays).isEqualTo(0)
        assertThat(vm.ramadanState.value.fastedDays).isEqualTo(10)
    }

    @Test
    fun `outside Ramadan the state says so and counts down to it instead`() = runTest {
        // Two months before the first day is comfortably outside Ramadan whichever year this
        // runs in.
        val vm = viewModel(firstDay.minusMonths(2))
        advanceUntilIdle()

        val state = vm.ramadanState.value
        assertThat(state.isRamadan).isFalse()
        assertThat(state.currentDay).isEqualTo(0)
        assertThat(state.daysUntilRamadan).isGreaterThan(0)
        assertThat(state.ramadanStartsOn).isNotNull()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `the countdown and the Ayyam al-Beed count are supplied whatever the month`() = runTest {
        val vm = viewModel(dayTen)
        advanceUntilIdle()

        // Both are computed in the ViewModel, where the clock and the user's Hijri day offset
        // live, precisely so the screen never reads the clock at composition (#492).
        assertThat(vm.ramadanState.value.daysUntilAyyamAlBeed).isAtLeast(0)
        assertThat(vm.ramadanState.value.ramadanStartsOn).isNotNull()
    }
}
