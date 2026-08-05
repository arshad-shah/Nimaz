package com.arshadshah.nimaz.presentation.viewmodel.calendar

import androidx.annotation.StringRes
import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.CalendarMonth
import com.arshadshah.nimaz.domain.model.HijriDate
import com.arshadshah.nimaz.domain.model.IslamicEvent
import java.time.LocalDate

/**
 * [selectedDate] has no default on purpose — a `LocalDate.now()` data-class default is
 * evaluated once at construction and never again, which is the frozen-"today" shape behind
 * the rollover bugs in #363. The ViewModel anchors it through `TodayProvider`.
 */
data class CalendarUiState(
    val currentMonth: CalendarMonth? = null,
    val selectedDate: LocalDate,
    val selectedHijriDate: HijriDate? = null,
    val viewMode: CalendarViewMode = CalendarViewMode.GREGORIAN,
    val isLoading: Boolean = true,
    /** A string resource, resolved by the screen — see ARCHITECTURE §6.1. */
    @StringRes val error: Int? = null
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

/** [year] is anchored by the ViewModel — see [CalendarUiState]. */
data class YearOverviewUiState(
    val months: List<CalendarMonth> = emptyList(),
    val year: Int,
    val isHijriYear: Boolean = false,
    val isLoading: Boolean = true
)
