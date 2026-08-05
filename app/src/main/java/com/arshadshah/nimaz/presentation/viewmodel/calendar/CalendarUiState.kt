package com.arshadshah.nimaz.presentation.viewmodel.calendar

import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.CalendarMonth
import com.arshadshah.nimaz.domain.model.HijriDate
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import java.time.LocalDate

data class CalendarUiState(
    val currentMonth: CalendarMonth? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedHijriDate: HijriDate? = null,
    val viewMode: CalendarViewMode = CalendarViewMode.GREGORIAN,
    val isLoading: Boolean = true,
    /** A string resource, resolved by the screen — see ARCHITECTURE §6.1. */
    val error: UiError? = null
)

data class HijriCalendarUiState(
    val currentHijriMonth: Int? = null,
    val currentHijriYear: Int = 1446, // Default year
    val days: List<CalendarDay> = emptyList(),
    val isLoading: Boolean = true
)

data class EventsUiState(
    val upcomingEvents: List<IslamicEvent> = emptyList(),
    val eventsForSelectedDate: List<IslamicEvent> = emptyList(),
    val eventsThisMonth: List<IslamicEvent> = emptyList(),
    val isLoading: Boolean = true
)

data class YearOverviewUiState(
    val months: List<CalendarMonth> = emptyList(),
    val year: Int = LocalDate.now().year,
    val isHijriYear: Boolean = false,
    val isLoading: Boolean = true
)
