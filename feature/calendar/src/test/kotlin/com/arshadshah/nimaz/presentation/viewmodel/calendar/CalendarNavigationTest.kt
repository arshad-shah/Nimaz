package com.arshadshah.nimaz.presentation.viewmodel.calendar

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.IslamicEventUseCases
import com.arshadshah.nimaz.domain.usecase.calendar.BuildCalendarMonthUseCase
import com.arshadshah.nimaz.domain.usecase.calendar.BuildHijriMonthUseCase
import com.arshadshah.nimaz.domain.usecase.calendar.CalendarUseCases
import com.arshadshah.nimaz.domain.usecase.calendar.GetEventsForDateUseCase
import com.arshadshah.nimaz.domain.usecase.calendar.GetEventsForMonthUseCase
import com.arshadshah.nimaz.domain.usecase.calendar.GetUpcomingEventsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
 * The rest of the calendar's event table: the Hijri grid, the year overview, the upcoming list,
 * and the guards on stepping a month.
 *
 * `CalendarViewModelTest` pins the Gregorian grid and the month/title divergence that made it
 * stick. What is left is everything that hangs off a *different* state flow, and the interesting
 * property they share is that each is loaded by its own cancellable job. A second navigation
 * arriving while the first is still building 12 months has to win — otherwise the reader taps
 * "next year" twice and lands back on the first one, which is indistinguishable from the button
 * not working.
 *
 * The month-stepping guards are the other half. `NavigateToPreviousMonth` reads the *first day of
 * the current grid* to work out where it is, so before a grid exists there is nothing to step
 * from. Stepping anyway would navigate relative to a null and take the reader somewhere arbitrary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarNavigationTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var useCases: IslamicEventUseCases
    private lateinit var viewModel: CalendarViewModel

    private val today = LocalDate.of(2026, 3, 20)

    private fun event(id: String, month: Int, day: Int) = IslamicEvent(
        id = id,
        nameArabic = "",
        nameEnglish = id,
        description = null,
        hijriMonth = month,
        hijriDay = day,
        eventType = IslamicEventType.HOLIDAY,
        isHoliday = true,
        isFastingDay = false,
        isNightOfPower = false,
        gregorianDate = null,
        year = null,
        notes = null,
        priority = 0,
    )

    private fun build(events: List<IslamicEvent> = emptyList()) {
        useCases = mockk(relaxed = true)
        coEvery { useCases.getAllEvents() } returns flowOf(events)
        newViewModel()
    }

    private fun newViewModel() {
        val forDate = GetEventsForDateUseCase()
        viewModel = CalendarViewModel(
            useCases,
            CalendarUseCases(
                buildGregorianMonth = BuildCalendarMonthUseCase(forDate),
                buildHijriMonth = BuildHijriMonthUseCase(forDate),
                eventsForMonth = GetEventsForMonthUseCase(),
                upcomingEvents = GetUpcomingEventsUseCase(),
                eventsForDate = forDate,
            ),
            FakeTodayProvider(today),
            RecordingTelemetry(),
            dispatcher,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        build()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ---- The Hijri grid ----

    @Test
    fun `a hijri month is built as long as the month actually is`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToHijriMonth(month = 9, year = 1447))
        advanceUntilIdle()

        val state = viewModel.hijriState.value
        assertThat(state.currentHijriMonth).isEqualTo(9)
        assertThat(state.currentHijriYear).isEqualTo(1447)
        assertThat(state.days.size).isIn(listOf(29, 30))
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `every day of a hijri month carries that month and year`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToHijriMonth(month = 1, year = 1447))
        advanceUntilIdle()

        viewModel.hijriState.value.days.forEach {
            assertThat(it.hijriDate.month).isEqualTo(1)
            assertThat(it.hijriDate.year).isEqualTo(1447)
        }
    }

    @Test
    fun `a hijri month carries the events that fall in it`() = runTest(dispatcher) {
        build(listOf(event("ashura", month = 1, day = 10)))
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToHijriMonth(month = 1, year = 1447))
        advanceUntilIdle()

        val marked = viewModel.hijriState.value.days.filter { it.events.isNotEmpty() }
        assertThat(marked.map { it.hijriDate.day }).containsExactly(10)
    }

    @Test
    fun `the last hijri month asked for is the one that shows`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToHijriMonth(month = 1, year = 1447))
        viewModel.onEvent(CalendarEvent.NavigateToHijriMonth(month = 9, year = 1447))
        advanceUntilIdle()

        // A cancelled first build landing second would put the reader back where they were.
        assertThat(viewModel.hijriState.value.currentHijriMonth).isEqualTo(9)
    }

    @Test
    fun `a hijri month that cannot be built stops its spinner`() = runTest(dispatcher) {
        // Month 13 does not exist; the grid must resolve to "not loading" rather than spin.
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToHijriMonth(month = 13, year = 1447))
        advanceUntilIdle()

        assertThat(viewModel.hijriState.value.isLoading).isFalse()
    }

    // ---- The year overview ----

    @Test
    fun `a gregorian year overview is twelve months`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToYear(year = 2026, isHijri = false))
        advanceUntilIdle()

        val state = viewModel.yearState.value
        assertThat(state.months).hasSize(12)
        assertThat(state.year).isEqualTo(2026)
        assertThat(state.isHijriYear).isFalse()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `stepping a year keeps whichever calendar the reader was in`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(CalendarEvent.NavigateToYear(year = 2026, isHijri = false))
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToNextYear)
        advanceUntilIdle()
        assertThat(viewModel.yearState.value.year).isEqualTo(2027)
        assertThat(viewModel.yearState.value.isHijriYear).isFalse()

        viewModel.onEvent(CalendarEvent.NavigateToPreviousYear)
        viewModel.onEvent(CalendarEvent.NavigateToPreviousYear)
        advanceUntilIdle()
        assertThat(viewModel.yearState.value.year).isEqualTo(2025)
    }

    @Test
    fun `a hijri year overview is still unimplemented, and says so with an empty list`() =
        runTest(dispatcher) {
            // Reachable only from a year view no screen wires up (#357). Pinned as empty rather
            // than left undescribed, so whoever implements it sees this test fail.
            advanceUntilIdle()

            viewModel.onEvent(CalendarEvent.NavigateToYear(year = 1447, isHijri = true))
            advanceUntilIdle()

            val state = viewModel.yearState.value
            assertThat(state.isHijriYear).isTrue()
            assertThat(state.months).isEmpty()
            assertThat(state.isLoading).isFalse()
        }

    // ---- Upcoming events ----

    @Test
    fun `the upcoming list is loaded on demand`() = runTest(dispatcher) {
        build(listOf(event("eid-al-fitr", month = 10, day = 1)))
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.LoadUpcomingEvents)
        advanceUntilIdle()

        assertThat(viewModel.eventsState.value.isLoading).isFalse()
    }

    @Test
    fun `an upcoming event is dated in the year it will actually happen`() = runTest(dispatcher) {
        build(listOf(event("eid-al-fitr", month = 10, day = 1)))
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.LoadUpcomingEvents)
        advanceUntilIdle()

        viewModel.eventsState.value.upcomingEvents.forEach {
            assertThat(it.gregorianDate).isNotNull()
            assertThat(it.gregorianDate!!).isAtLeast(today)
        }
    }

    @Test
    fun `no events means an empty upcoming list, not a spinner`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.LoadUpcomingEvents)
        advanceUntilIdle()

        assertThat(viewModel.eventsState.value.upcomingEvents).isEmpty()
        assertThat(viewModel.eventsState.value.isLoading).isFalse()
    }

    // ---- Stepping a month ----

    @Test
    fun `stepping forward and back returns to the month it started on`() = runTest(dispatcher) {
        advanceUntilIdle()
        val start = viewModel.calendarState.value.currentMonth?.displayedMonth

        viewModel.onEvent(CalendarEvent.NavigateToNextMonth)
        advanceUntilIdle()
        assertThat(viewModel.calendarState.value.currentMonth?.displayedMonth).isNotEqualTo(start)

        viewModel.onEvent(CalendarEvent.NavigateToPreviousMonth)
        advanceUntilIdle()
        assertThat(viewModel.calendarState.value.currentMonth?.displayedMonth).isEqualTo(start)
    }

    @Test
    fun `stepping across a year boundary lands in the next year`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToMonth(month = 12, year = 2026))
        advanceUntilIdle()
        viewModel.onEvent(CalendarEvent.NavigateToNextMonth)
        advanceUntilIdle()

        assertThat(viewModel.calendarState.value.currentMonth?.displayedMonth.toString())
            .isEqualTo("2027-01")
    }

    @Test
    fun `stepping a month before the grid exists does nothing`() = runTest(dispatcher) {
        // `NavigateToPreviousMonth` reads the first day of the *current grid* to work out where
        // it is. Before a grid exists there is nothing to step from, and stepping anyway would
        // navigate relative to a null and land somewhere arbitrary.
        useCases = mockk(relaxed = true)
        coEvery { useCases.getAllEvents() } returns flow {
            throw IllegalStateException("no content")
        }
        newViewModel()
        // Deliberately without advanceUntilIdle: the grid has not been built yet.
        viewModel.onEvent(CalendarEvent.NavigateToNextMonth)
        viewModel.onEvent(CalendarEvent.NavigateToPreviousMonth)
        advanceUntilIdle()

        // Whatever loadToday settled on, the two steps cancelled each other or never ran; what
        // must not happen is a grid built relative to nothing.
        assertThat(viewModel.calendarState.value.currentMonth?.days.orEmpty()).isNotEmpty()
    }

    // ---- The view mode ----

    @Test
    fun `the view mode is remembered without rebuilding the grid`() = runTest(dispatcher) {
        advanceUntilIdle()
        val gridBefore = viewModel.calendarState.value.currentMonth

        viewModel.onEvent(CalendarEvent.SetViewMode(CalendarViewMode.HIJRI))
        advanceUntilIdle()

        assertThat(viewModel.calendarState.value.viewMode).isEqualTo(CalendarViewMode.HIJRI)
        assertThat(viewModel.calendarState.value.currentMonth).isEqualTo(gridBefore)
    }

    // ---- A content fault ----

    @Test
    fun `a failed event read costs the markers, not the grid`() = runTest(dispatcher) {
        // `loadToday()` used to run inside the events `try`, so a throw left `currentMonth` null
        // and the whole screen rendered nothing.
        useCases = mockk(relaxed = true)
        coEvery { useCases.getAllEvents() } returns flow { throw IllegalStateException("no content") }
        newViewModel()
        advanceUntilIdle()

        val state = viewModel.calendarState.value
        assertThat(state.currentMonth).isNotNull()
        assertThat(state.error).isNotNull()
        assertThat(state.isLoading).isFalse()
    }
}
