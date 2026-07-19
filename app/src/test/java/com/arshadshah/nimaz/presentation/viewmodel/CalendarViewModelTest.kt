package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.usecase.IslamicEventUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
import java.time.YearMonth

/**
 * Regression cover for the calendar grid staying stuck on the current month.
 *
 * Month navigation updated `currentMonth` (which drives the header title) but
 * never `selectedDate` (which drove the grid), so the title moved and the grid
 * did not. These tests pin the month a screen should display to a single
 * source — [com.arshadshah.nimaz.domain.model.CalendarMonth.displayedMonth] —
 * so the two can never diverge again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var useCases: IslamicEventUseCases
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        coEvery { useCases.getAllEvents() } returns flowOf(emptyList<IslamicEvent>())
        viewModel = CalendarViewModel(useCases)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts on the current month`() = runTest(dispatcher) {
        advanceUntilIdle()

        val displayed = viewModel.calendarState.value.currentMonth?.displayedMonth
        assertThat(displayed).isEqualTo(YearMonth.from(LocalDate.now()))
    }

    @Test
    fun `navigating to the next month advances the displayed month`() = runTest(dispatcher) {
        advanceUntilIdle()
        val start = viewModel.calendarState.value.currentMonth?.displayedMonth

        viewModel.onEvent(CalendarEvent.NavigateToNextMonth)
        advanceUntilIdle()

        assertThat(viewModel.calendarState.value.currentMonth?.displayedMonth)
            .isEqualTo(start?.plusMonths(1))
    }

    @Test
    fun `navigating to the previous month rewinds the displayed month`() = runTest(dispatcher) {
        advanceUntilIdle()
        val start = viewModel.calendarState.value.currentMonth?.displayedMonth

        viewModel.onEvent(CalendarEvent.NavigateToPreviousMonth)
        advanceUntilIdle()

        assertThat(viewModel.calendarState.value.currentMonth?.displayedMonth)
            .isEqualTo(start?.minusMonths(1))
    }

    @Test
    fun `navigating months does not move the user's selected date`() = runTest(dispatcher) {
        advanceUntilIdle()
        val selectedBefore = viewModel.calendarState.value.selectedDate

        viewModel.onEvent(CalendarEvent.NavigateToNextMonth)
        advanceUntilIdle()

        // Paging the calendar is a browsing action, not a selection action.
        assertThat(viewModel.calendarState.value.selectedDate).isEqualTo(selectedBefore)
    }

    @Test
    fun `navigating forward then back returns to the starting month`() = runTest(dispatcher) {
        advanceUntilIdle()
        val start = viewModel.calendarState.value.currentMonth?.displayedMonth

        viewModel.onEvent(CalendarEvent.NavigateToNextMonth)
        advanceUntilIdle()
        viewModel.onEvent(CalendarEvent.NavigateToPreviousMonth)
        advanceUntilIdle()

        assertThat(viewModel.calendarState.value.currentMonth?.displayedMonth).isEqualTo(start)
    }

    @Test
    fun `navigating across a year boundary keeps the year in step`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(CalendarEvent.NavigateToMonth(12, 2026))
        advanceUntilIdle()
        assertThat(viewModel.calendarState.value.currentMonth?.displayedMonth)
            .isEqualTo(YearMonth.of(2026, 12))

        viewModel.onEvent(CalendarEvent.NavigateToNextMonth)
        advanceUntilIdle()

        assertThat(viewModel.calendarState.value.currentMonth?.displayedMonth)
            .isEqualTo(YearMonth.of(2027, 1))
    }
}
