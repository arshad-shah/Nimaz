package com.arshadshah.nimaz.presentation.screens.calendar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.CalendarMonth
import com.arshadshah.nimaz.domain.model.HijriDate
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.calendar.CalendarEvent
import com.arshadshah.nimaz.presentation.viewmodel.calendar.CalendarUiState
import com.arshadshah.nimaz.presentation.viewmodel.calendar.CalendarViewModel
import com.arshadshah.nimaz.presentation.viewmodel.calendar.EventsUiState
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The Islamic calendar screen: a grid, an events list, and one arrangement decision.
 *
 * The invariant worth the most here is that **a failed event read costs the markers, not the
 * screen**. The KDoc on `CalendarSection` records why: `loadToday()` used to run inside the
 * events `try`, so a content-database fault left `currentMonth` null and the screen rendered
 * nothing at all. The fix was to make the error a *section* above a grid that still draws — which
 * only holds if the screen actually keeps drawing the grid, and that is a rendering fact no
 * ViewModel test can see.
 *
 * The two content sections are the other half, and they are conditional in both layouts. A
 * heading with nothing under it is the failure mode: "Upcoming Events" over empty space reads as
 * a list that failed to load rather than one that is legitimately empty.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class IslamicCalendarScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val today = LocalDate.of(2026, 3, 20)

    private val calendarState = MutableStateFlow(CalendarUiState(selectedDate = today))
    private val eventsState = MutableStateFlow(EventsUiState(isLoading = false))
    private val events = mutableListOf<CalendarEvent>()

    private val viewModel: CalendarViewModel = mockk(relaxed = true) {
        every { this@mockk.calendarState } returns this@IslamicCalendarScreenTest.calendarState
        every { this@mockk.eventsState } returns this@IslamicCalendarScreenTest.eventsState
        every { onEvent(any()) } answers { events += firstArg<CalendarEvent>() }
    }

    private var backPressed = 0

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun islamicEvent(id: String, name: String, type: IslamicEventType) = IslamicEvent(
        id = id,
        nameArabic = "حدث",
        nameEnglish = name,
        description = null,
        hijriMonth = 9,
        hijriDay = 27,
        eventType = type,
        isHoliday = type == IslamicEventType.HOLIDAY,
        isFastingDay = type == IslamicEventType.FAST,
        isNightOfPower = type == IslamicEventType.NIGHT,
        gregorianDate = today,
        year = 2026,
        notes = null,
        priority = 0,
    )

    /** One real month of days, so the grid has something to draw and to key its markers by. */
    private fun month(
        withEventOn: LocalDate? = null,
        eventType: IslamicEventType = IslamicEventType.NIGHT,
    ): CalendarMonth {
        val first = LocalDate.of(2026, 3, 1)
        val days = (0 until 31).map { offset ->
            val date = first.plusDays(offset.toLong())
            val hijri = HijriDateCalculator.toHijri(date)
            CalendarDay(
                gregorianDate = date,
                hijriDate = HijriDate(hijri.day, hijri.month, hijri.year),
                events = if (date == withEventOn) {
                    listOf(islamicEvent("e", "Laylat al-Qadr", eventType))
                } else {
                    emptyList()
                },
                isToday = date == today,
                isCurrentMonth = true,
            )
        }
        return CalendarMonth(
            hijriMonth = days.first().hijriDate.month,
            hijriYear = days.first().hijriDate.year,
            days = days,
            events = days.flatMap { it.events },
        )
    }

    private fun render() {
        composeRule.setThemedContent {
            IslamicCalendarScreen(
                onNavigateBack = { backPressed++ },
                viewModel = viewModel,
            )
        }
    }

    private fun loaded(
        currentMonth: CalendarMonth? = month(),
        error: UiError? = null,
    ) {
        calendarState.value = CalendarUiState(
            currentMonth = currentMonth,
            selectedDate = today,
            isLoading = false,
            error = error,
        )
    }

    // ---- The frame ----

    @Test
    fun `the screen is titled, and back goes back`() {
        loaded()
        render()

        composeRule.onNodeWithText(str(R.string.islamic_calendar)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(backPressed).isEqualTo(1)
    }

    @Test
    fun `the today action asks to go back to today`() {
        loaded()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.today)).performClick()

        assertThat(events).contains(CalendarEvent.LoadToday)
    }

    // ---- The grid ----

    @Test
    fun `the header names the hijri month the grid is showing`() {
        val march = month()
        loaded(currentMonth = march)
        render()

        val expected = HijriDateCalculator.getHijriMonthName(march.hijriMonth)
        composeRule.onAllNodesWithText(expected, substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `choosing a day is reported as a selection`() {
        loaded()
        render()

        // Day 15 of the Gregorian month, which the grid draws as a tappable cell.
        composeRule.onAllNodesWithText("15").onFirst().performClick()

        assertThat(events.filterIsInstance<CalendarEvent.SelectDate>()).isNotEmpty()
    }

    @Test
    fun `the legend names every marker the grid can draw`() {
        loaded()
        render()

        composeRule.onNodeWithText(str(R.string.month_start)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.eid)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.holy_night)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.fasting)).assertIsDisplayed()
    }

    @Test
    fun `no month yet means no grid, rather than an empty one`() {
        loaded(currentMonth = null)
        render()

        composeRule.onNodeWithText(str(R.string.month_start)).assertDoesNotExist()
    }

    // ---- The error is a section, not a replacement ----

    @Test
    fun `a failed event read is reported above a grid that still draws`() {
        // `loadToday()` ran inside the events `try` once, so a content fault left `currentMonth`
        // null and the screen rendered nothing. The grid is correct and useful without markers.
        loaded(
            currentMonth = month(),
            error = UiError(R.string.calendar_events_load_failed, NimazErrorKind.GENERIC),
        )
        render()

        composeRule.onAllNodesWithText(str(R.string.calendar_events_load_failed), substring = true)
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.month_start)).assertIsDisplayed()
    }

    @Test
    fun `a screen with no error shows none`() {
        loaded()
        render()

        composeRule.onNodeWithText(str(R.string.calendar_events_load_failed), substring = true)
            .assertDoesNotExist()
    }

    // ---- The two event sections ----

    @Test
    fun `neither heading appears when there is nothing under it`() {
        // A heading over empty space reads as a list that failed to load.
        loaded()
        eventsState.value = EventsUiState(isLoading = false)
        render()

        composeRule.onNodeWithText(str(R.string.events)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.upcoming_events)).assertDoesNotExist()
    }

    @Test
    fun `the selected day's events appear under their own heading`() {
        loaded()
        eventsState.value = EventsUiState(
            eventsForSelectedDate = listOf(
                islamicEvent("qadr", "Laylat al-Qadr", IslamicEventType.NIGHT),
            ),
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText(str(R.string.events)).assertIsDisplayed()
        composeRule.onNodeWithText("Laylat al-Qadr").assertIsDisplayed()
    }

    @Test
    fun `the upcoming list appears under its own heading`() {
        loaded()
        eventsState.value = EventsUiState(
            upcomingEvents = listOf(islamicEvent("fitr", "Eid al-Fitr", IslamicEventType.HOLIDAY)),
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText(str(R.string.upcoming_events)).assertIsDisplayed()
        composeRule.onNodeWithText("Eid al-Fitr").assertIsDisplayed()
    }

    @Test
    fun `the upcoming list is capped at five, however many there are`() {
        loaded()
        eventsState.value = EventsUiState(
            upcomingEvents = (1..9).map {
                islamicEvent("e$it", "Event $it", IslamicEventType.HISTORICAL)
            },
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText("Event 5").assertIsDisplayed()
        composeRule.onNodeWithText("Event 6").assertDoesNotExist()
    }

    @Test
    fun `both sections show together, each under its own heading`() {
        loaded()
        eventsState.value = EventsUiState(
            eventsForSelectedDate = listOf(
                islamicEvent("qadr", "Laylat al-Qadr", IslamicEventType.NIGHT),
            ),
            upcomingEvents = listOf(islamicEvent("fitr", "Eid al-Fitr", IslamicEventType.HOLIDAY)),
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText(str(R.string.events)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.upcoming_events)).assertIsDisplayed()
        composeRule.onNodeWithText("Laylat al-Qadr").assertIsDisplayed()
        composeRule.onNodeWithText("Eid al-Fitr").assertIsDisplayed()
    }

    // ---- The day markers ----

    @Test
    fun `every kind of event gets a marker on its day`() {
        // `getEventDotColor` has an arm per type. A type with no arm is a
        // `NoWhenBranchMatchedException` on the grid, not a missing dot — so every one is
        // walked, through the same rendered screen rather than a fresh one each time.
        val marked = LocalDate.of(2026, 3, 15)
        loaded(currentMonth = month(withEventOn = marked, eventType = IslamicEventType.entries[0]))
        render()

        IslamicEventType.entries.forEach { type ->
            calendarState.value = CalendarUiState(
                currentMonth = month(withEventOn = marked, eventType = type),
                selectedDate = today,
                isLoading = false,
            )
            composeRule.waitForIdle()

            composeRule.onNodeWithText(str(R.string.month_start)).assertIsDisplayed()
        }
    }

    @Test
    fun `a month with no events at all still draws its grid`() {
        loaded(currentMonth = month(withEventOn = null))
        render()

        composeRule.onNodeWithText(str(R.string.month_start)).assertIsDisplayed()
        composeRule.onAllNodesWithText("15").onFirst().assertIsDisplayed()
    }

    // ---- The wide layout ----

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet the grid and the events sit side by side`() {
        loaded()
        eventsState.value = EventsUiState(
            upcomingEvents = listOf(islamicEvent("fitr", "Eid al-Fitr", IslamicEventType.HOLIDAY)),
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText(str(R.string.month_start)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.upcoming_events)).assertIsDisplayed()
        composeRule.onNodeWithText("Eid al-Fitr").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet layout keeps its events column labelled even with nothing in it`() {
        // The wide layout has a third arm the compact one does not: an empty right-hand column
        // still carries its heading, because a blank half-screen reads as a broken pane.
        loaded()
        eventsState.value = EventsUiState(isLoading = false)
        render()

        composeRule.onNodeWithText(str(R.string.events)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet layout caps its upcoming list at five too`() {
        loaded()
        eventsState.value = EventsUiState(
            upcomingEvents = (1..9).map {
                islamicEvent("e$it", "Event $it", IslamicEventType.HISTORICAL)
            },
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText("Event 5").assertIsDisplayed()
        composeRule.onNodeWithText("Event 6").assertDoesNotExist()
    }
}
