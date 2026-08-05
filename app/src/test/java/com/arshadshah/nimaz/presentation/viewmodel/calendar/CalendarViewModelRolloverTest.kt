package com.arshadshah.nimaz.presentation.viewmodel.calendar

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.domain.usecase.IslamicEventUseCases
import com.arshadshah.nimaz.domain.usecase.calendar.BuildCalendarMonthUseCase
import com.arshadshah.nimaz.domain.usecase.calendar.BuildHijriMonthUseCase
import com.arshadshah.nimaz.domain.usecase.calendar.CalendarUseCases
import com.arshadshah.nimaz.domain.usecase.calendar.GetEventsForDateUseCase
import com.arshadshah.nimaz.domain.usecase.calendar.GetEventsForMonthUseCase
import com.arshadshah.nimaz.domain.usecase.calendar.GetUpcomingEventsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
 * The calendar's relationship with "today" and with the content database.
 *
 * Three defects meet here. The month grid needs no events to draw, but `loadToday()` ran
 * *inside* the `try` that read them, so one content-database fault left the screen with no
 * grid, no message and no retry. The events themselves came from a `.first()` — a one-shot
 * read of a reactive source — so a content-database replacement mid-session served the old
 * set until the ViewModel died. And `isToday` was baked in at generation, so a grid built at
 * 23:59 kept highlighting yesterday.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelRolloverTest {

    private val dispatcher = StandardTestDispatcher()

    // Anchored on the Hijri calendar: 20 Dhul-Hijjah, so Muharram's events are genuinely
    // upcoming and the three-month window is not the thing under test.
    private val today: LocalDate = HijriDateCalculator.toGregorian(20, 12, 1446)
    private val todayProvider = FakeTodayProvider(today)
    private val telemetry = RecordingTelemetry()

    private lateinit var eventUseCases: IslamicEventUseCases
    private val events = MutableStateFlow(listOf(event("ashura", hijriMonth = 1, hijriDay = 10)))

    private val calendarUseCases = GetEventsForDateUseCase().let { forDate ->
        CalendarUseCases(
            buildGregorianMonth = BuildCalendarMonthUseCase(forDate),
            buildHijriMonth = BuildHijriMonthUseCase(forDate),
            eventsForMonth = GetEventsForMonthUseCase(),
            upcomingEvents = GetUpcomingEventsUseCase(),
            eventsForDate = forDate,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        eventUseCases = mockk(relaxed = true)
        every { eventUseCases.getAllEvents() } returns events
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = CalendarViewModel(
        eventUseCases,
        calendarUseCases,
        todayProvider,
        telemetry,
        dispatcher,
    )

    @Test
    fun `a failing event read still leaves a month grid on screen`() = runTest {
        every { eventUseCases.getAllEvents() } returns
            flow { throw IllegalStateException("no such table: islamic_events") }

        val vm = viewModel()
        advanceUntilIdle()

        // The grid is dates; it never needed the events.
        assertThat(vm.calendarState.value.currentMonth).isNotNull()
        assertThat(vm.calendarState.value.currentMonth!!.days).hasSize(today.lengthOfMonth())
        // …and the failure is said out loud rather than swallowed into a blank screen.
        assertThat(vm.calendarState.value.error?.message)
            .isEqualTo(R.string.calendar_events_load_failed)
        assertThat(telemetry.errors.map { it.type }).contains("load_events")
    }

    @Test
    fun `a replaced content database is picked up without recreating the ViewModel`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // ContentArtifactInstaller swaps the database under a running app.
        events.value = listOf(
            event("ashura", hijriMonth = 1, hijriDay = 10),
            event("new-year", hijriMonth = 1, hijriDay = 1),
        )
        advanceUntilIdle()

        assertThat(vm.eventsState.value.upcomingEvents.map { it.id })
            .containsAtLeast("ashura", "new-year")
    }

    @Test
    fun `the grid stops highlighting yesterday once the day changes`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.calendarState.value.currentMonth!!.days.filter { it.isToday }
            .map { it.gregorianDate }).containsExactly(today)

        // Midnight, with the screen still open.
        todayProvider.now = today.plusDays(1)
        advanceUntilIdle()

        assertThat(vm.calendarState.value.currentMonth!!.days.filter { it.isToday }
            .map { it.gregorianDate }).containsExactly(today.plusDays(1))
    }
}

private fun event(id: String, hijriMonth: Int, hijriDay: Int) = IslamicEvent(
    id = id,
    nameArabic = "",
    nameEnglish = id,
    description = null,
    hijriMonth = hijriMonth,
    hijriDay = hijriDay,
    eventType = IslamicEventType.HISTORICAL,
    isHoliday = false,
    isFastingDay = false,
    isNightOfPower = false,
    gregorianDate = null,
    year = null,
    notes = null,
    priority = 0,
)
