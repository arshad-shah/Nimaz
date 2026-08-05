package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.presentation.viewmodel.FakePrayerTimetableRepository
import com.arshadshah.nimaz.presentation.viewmodel.buildPrayerUseCases
import com.arshadshah.nimaz.presentation.viewmodel.prayerCalculationSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * `MonthlyPrayerTimesViewModel` had no tests at all, while owning two things that are easy to
 * get wrong and invisible when they are: the month grid is anchored to "today" and re-anchors
 * on rollover, and the month build is a cancel-and-replace job so a fast pager cannot stack
 * a month's worth of astronomy per swipe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyPrayerTimesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 3, 15)
    private lateinit var prayers: FakePrayerTimetableRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prayers = FakePrayerTimetableRepository(prayerCalculationSettings())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(now: LocalDate = today) = MonthlyPrayerTimesViewModel(
        buildPrayerUseCases(prayers),
        FakeTodayProvider(now),
        dispatcher,
        RecordingTelemetry(),
    )

    @Test
    fun `the grid opens on the current month`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.state.value.currentMonth).isEqualTo(YearMonth.of(2026, 3))
    }

    @Test
    fun `a full month of days is built`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        // March has 31 days; the grid is one row per day, not per visible cell.
        assertThat(vm.state.value.dayPrayerTimes).hasSize(31)
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `paging forward moves the month and rebuilds it`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(MonthlyPrayerTimesEvent.NextMonth)
        advanceUntilIdle()

        assertThat(vm.state.value.currentMonth).isEqualTo(YearMonth.of(2026, 4))
        assertThat(vm.state.value.dayPrayerTimes).hasSize(30)
    }

    @Test
    fun `paging back from January lands in the previous December`() = runTest(dispatcher) {
        val vm = viewModel(now = LocalDate.of(2026, 1, 10))
        advanceUntilIdle()

        vm.onEvent(MonthlyPrayerTimesEvent.PreviousMonth)
        advanceUntilIdle()

        assertThat(vm.state.value.currentMonth).isEqualTo(YearMonth.of(2025, 12))
        assertThat(vm.state.value.dayPrayerTimes).hasSize(31)
    }

    @Test
    fun `expanding a day is a toggle, not a latch`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val day = LocalDate.of(2026, 3, 20)

        vm.onEvent(MonthlyPrayerTimesEvent.ToggleDayExpanded(day))
        advanceUntilIdle()
        assertThat(vm.state.value.expandedDay).isEqualTo(day)

        vm.onEvent(MonthlyPrayerTimesEvent.ToggleDayExpanded(day))
        advanceUntilIdle()
        assertThat(vm.state.value.expandedDay).isNull()
    }

    @Test
    fun `the ramadan export is a one-shot and clears once consumed`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(MonthlyPrayerTimesEvent.PrepareRamadanExport)
        advanceUntilIdle()
        assertThat(vm.state.value.ramadanExport).isNotNull()

        // Held in state rather than exposed as a method the screen calls for a value, so it
        // has to be cleared explicitly or a recomposition would re-open the share sheet.
        vm.onEvent(MonthlyPrayerTimesEvent.RamadanExportConsumed)
        advanceUntilIdle()
        assertThat(vm.state.value.ramadanExport).isNull()
    }
}
