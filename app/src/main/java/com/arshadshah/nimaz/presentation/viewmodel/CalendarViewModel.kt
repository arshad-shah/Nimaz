package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.CalendarMonth
import com.arshadshah.nimaz.domain.model.HijriDate
import com.arshadshah.nimaz.domain.model.HijriMonth
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: CalendarMonth? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedHijriDate: HijriDate? = null,
    val viewMode: CalendarViewMode = CalendarViewMode.GREGORIAN,
    val isLoading: Boolean = true,
    val error: String? = null
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

enum class CalendarViewMode {
    GREGORIAN, HIJRI, DUAL
}

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

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val islamicEventDao: IslamicEventDao
) : ViewModel() {

    private var cachedEvents: List<IslamicEvent> = emptyList()

    private val _calendarState = MutableStateFlow(CalendarUiState())
    val calendarState: StateFlow<CalendarUiState> = _calendarState.asStateFlow()

    private val _hijriState = MutableStateFlow(HijriCalendarUiState())
    val hijriState: StateFlow<HijriCalendarUiState> = _hijriState.asStateFlow()

    private val _eventsState = MutableStateFlow(EventsUiState())
    val eventsState: StateFlow<EventsUiState> = _eventsState.asStateFlow()

    private val _yearState = MutableStateFlow(YearOverviewUiState())
    val yearState: StateFlow<YearOverviewUiState> = _yearState.asStateFlow()

    init {
        loadEventsFromDatabase()
    }

    private fun loadEventsFromDatabase() {
        viewModelScope.launch {
            try {
                cachedEvents = islamicEventDao.getAllEvents()
                    .first()
                    .map { it.toDomainModel() }
                loadToday()
                loadUpcomingEvents()
            } catch (e: Exception) {
                _calendarState.update { it.copy(error = "Failed to load events: ${e.message}", isLoading = false) }
            }
        }
    }

    fun onEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.SelectDate -> selectDate(event.date)
            is CalendarEvent.NavigateToMonth -> navigateToMonth(event.month, event.year)
            is CalendarEvent.NavigateToHijriMonth -> navigateToHijriMonth(event.month, event.year)
            is CalendarEvent.SetViewMode -> setViewMode(event.mode)
            is CalendarEvent.NavigateToYear -> navigateToYear(event.year, event.isHijri)
            CalendarEvent.LoadToday -> loadToday()
            CalendarEvent.LoadUpcomingEvents -> loadUpcomingEvents()
            CalendarEvent.NavigateToPreviousMonth -> navigateToPreviousMonth()
            CalendarEvent.NavigateToNextMonth -> navigateToNextMonth()
            CalendarEvent.NavigateToPreviousYear -> navigateToPreviousYear()
            CalendarEvent.NavigateToNextYear -> navigateToNextYear()
        }
    }

    private fun loadToday() {
        val today = LocalDate.now()
        selectDate(today)
        navigateToMonth(today.monthValue, today.year)
    }

    private fun selectDate(date: LocalDate) {
        val calculatorHijriDate = HijriDateCalculator.toHijri(date)
        val hijriDate = HijriDate(
            day = calculatorHijriDate.day,
            month = calculatorHijriDate.month,
            year = calculatorHijriDate.year
        )
        val eventsForDate = getEventsForDate(hijriDate)

        _calendarState.update {
            it.copy(
                selectedDate = date,
                selectedHijriDate = hijriDate
            )
        }

        _eventsState.update {
            it.copy(eventsForSelectedDate = eventsForDate)
        }
    }

    private fun navigateToMonth(month: Int, year: Int) {
        _calendarState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val calendarMonth = generateGregorianMonth(month, year)
            val eventsThisMonth = getEventsForMonth(month, year)

            _calendarState.update {
                it.copy(
                    currentMonth = calendarMonth,
                    isLoading = false
                )
            }

            _eventsState.update {
                it.copy(eventsThisMonth = eventsThisMonth)
            }
        }
    }

    private fun navigateToHijriMonth(month: Int, year: Int) {
        _hijriState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val days = generateHijriMonthDays(month, year)

            _hijriState.update {
                it.copy(
                    currentHijriMonth = month,
                    currentHijriYear = year,
                    days = days,
                    isLoading = false
                )
            }
        }
    }

    private fun setViewMode(mode: CalendarViewMode) {
        _calendarState.update { it.copy(viewMode = mode) }
    }

    private fun navigateToYear(year: Int, isHijri: Boolean) {
        _yearState.update { it.copy(isLoading = true, year = year, isHijriYear = isHijri) }

        viewModelScope.launch {
            val months = if (isHijri) {
                generateHijriYearMonths(year)
            } else {
                generateGregorianYearMonths(year)
            }

            _yearState.update {
                it.copy(months = months, isLoading = false)
            }
        }
    }

    private fun loadUpcomingEvents() {
        viewModelScope.launch {
            val events = getUpcomingIslamicEvents()

            _eventsState.update {
                it.copy(upcomingEvents = events, isLoading = false)
            }
        }
    }

    private fun navigateToPreviousMonth() {
        val current = _calendarState.value.currentMonth ?: return
        // Use the first day's gregorian date to determine current Gregorian month
        val firstDay = current.days.firstOrNull()?.gregorianDate ?: return
        val prev = firstDay.minusMonths(1)
        navigateToMonth(prev.monthValue, prev.year)
    }

    private fun navigateToNextMonth() {
        val current = _calendarState.value.currentMonth ?: return
        val firstDay = current.days.firstOrNull()?.gregorianDate ?: return
        val next = firstDay.plusMonths(1)
        navigateToMonth(next.monthValue, next.year)
    }

    private fun navigateToPreviousYear() {
        val current = _yearState.value
        navigateToYear(current.year - 1, current.isHijriYear)
    }

    private fun navigateToNextYear() {
        val current = _yearState.value
        navigateToYear(current.year + 1, current.isHijriYear)
    }

    private fun generateGregorianMonth(month: Int, year: Int): CalendarMonth {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        val days = mutableListOf<CalendarDay>()

        var currentDate = firstDay
        while (!currentDate.isAfter(lastDay)) {
            val calculatorHijriDate = HijriDateCalculator.toHijri(currentDate)
            val hijriDate = HijriDate(
                day = calculatorHijriDate.day,
                month = calculatorHijriDate.month,
                year = calculatorHijriDate.year
            )
            val events = getEventsForDate(hijriDate)

            days.add(
                CalendarDay(
                    gregorianDate = currentDate,
                    hijriDate = hijriDate,
                    isToday = currentDate == LocalDate.now(),
                    isCurrentMonth = true,
                    events = events
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        val firstHijri = days.firstOrNull()?.hijriDate
        val lastHijri = days.lastOrNull()?.hijriDate

        return CalendarMonth(
            hijriMonth = firstHijri?.month ?: 1,
            hijriYear = firstHijri?.year ?: 1446,
            days = days,
            events = days.flatMap { it.events }.distinctBy { it.id }
        )
    }

    private fun generateHijriMonthDays(month: Int, year: Int): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val daysInMonth = HijriDateCalculator.getDaysInHijriMonth(year, month)

        for (day in 1..daysInMonth) {
            val hijriDate = HijriDate(
                day = day,
                month = month,
                year = year
            )
            val gregorianDate = HijriDateCalculator.toGregorian(day, month, year)
            val events = getEventsForDate(hijriDate)

            days.add(
                CalendarDay(
                    gregorianDate = gregorianDate,
                    hijriDate = hijriDate,
                    isToday = gregorianDate == LocalDate.now(),
                    isCurrentMonth = true,
                    events = events
                )
            )
        }

        return days
    }

    private fun generateGregorianYearMonths(year: Int): List<CalendarMonth> {
        return (1..12).map { month ->
            generateGregorianMonth(month, year)
        }
    }

    private fun generateHijriYearMonths(year: Int): List<CalendarMonth> {
        // Would generate Hijri year months
        return emptyList()
    }

    private fun getEventsForDate(hijriDate: HijriDate): List<IslamicEvent> {
        return cachedEvents.filter { event ->
            event.hijriMonth == hijriDate.month && event.hijriDay == hijriDate.day
        }
    }

    private fun getEventsForMonth(month: Int, year: Int): List<IslamicEvent> {
        // Get all events that fall within this Gregorian month
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)

        return cachedEvents.filter { event ->
            val eventGregorian = getApproximateGregorianDate(event, year)
            eventGregorian != null && !eventGregorian.isBefore(firstDay) && !eventGregorian.isAfter(lastDay)
        }
    }

    private fun getUpcomingIslamicEvents(): List<IslamicEvent> {
        val today = LocalDate.now()
        val threeMonthsLater = today.plusMonths(3)

        return cachedEvents.mapNotNull { event ->
            val eventDate = getApproximateGregorianDate(event, today.year)
            if (eventDate != null && !eventDate.isBefore(today) && !eventDate.isAfter(threeMonthsLater)) {
                event.copy(gregorianDate = eventDate)
            } else null
        }.sortedBy { getApproximateGregorianDate(it, today.year) }
    }

    private fun getApproximateGregorianDate(event: IslamicEvent, year: Int): LocalDate? {
        return HijriDateCalculator.toGregorian(
            event.hijriDay,
            event.hijriMonth,
            _hijriState.value.currentHijriYear
        )
    }
}

private fun IslamicEventEntity.toDomainModel(): IslamicEvent {
    return IslamicEvent(
        id = id.toString(),
        nameArabic = nameArabic,
        nameEnglish = nameEnglish,
        description = description,
        hijriMonth = hijriMonth,
        hijriDay = hijriDay,
        eventType = try {
            IslamicEventType.valueOf(eventType.uppercase())
        } catch (_: IllegalArgumentException) {
            IslamicEventType.HOLIDAY
        },
        isHoliday = isHoliday == 1,
        isFastingDay = eventType.equals("fast", ignoreCase = true),
        isNightOfPower = eventType.equals("night", ignoreCase = true),
        gregorianDate = null,
        year = null,
        notes = null,
        priority = 0
    )
}
