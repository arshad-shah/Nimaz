package com.arshadshah.nimaz.presentation.viewmodel.calendar

import java.time.LocalDate

sealed interface CalendarEvent {
    data class SelectDate(val date: LocalDate) : CalendarEvent
    data class NavigateToMonth(val month: Int, val year: Int) : CalendarEvent
    data class NavigateToHijriMonth(val month: Int, val year: Int) : CalendarEvent
    data class SetViewMode(val mode: CalendarViewMode) : CalendarEvent
    data class NavigateToYear(val year: Int, val isHijri: Boolean) : CalendarEvent
    data object LoadToday : CalendarEvent
    data object LoadUpcomingEvents : CalendarEvent
    data object NavigateToPreviousMonth : CalendarEvent
    data object NavigateToNextMonth : CalendarEvent
    data object NavigateToPreviousYear : CalendarEvent
    data object NavigateToNextYear : CalendarEvent
}
