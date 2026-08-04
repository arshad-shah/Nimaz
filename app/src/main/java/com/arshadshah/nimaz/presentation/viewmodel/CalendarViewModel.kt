package com.arshadshah.nimaz.presentation.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.di.DefaultDispatcher
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.CalendarMonth
import com.arshadshah.nimaz.domain.model.HijriDate
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.usecase.IslamicEventUseCases
import com.arshadshah.nimaz.domain.usecase.calendar.CalendarUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: CalendarMonth? = null,
    val selectedDate: LocalDate = LocalDate.now(),
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
    private val islamicEventUseCases: IslamicEventUseCases,
    private val calendarUseCases: CalendarUseCases,
    private val todayProvider: TodayProvider,
    private val telemetry: Telemetry,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private var cachedEvents: List<IslamicEvent> = emptyList()

    /** The date every grid was last built for — the guard that makes rollover detectable. */
    private var renderedFor: LocalDate = todayProvider.today()

    // One handle per surface, per ARCHITECTURE §4.1: each of these is re-invoked as the user
    // navigates, and each writes one state.
    private var monthJob: Job? = null
    private var hijriJob: Job? = null
    private var yearJob: Job? = null
    private var upcomingJob: Job? = null

    private val _calendarState = MutableStateFlow(CalendarUiState())
    val calendarState: StateFlow<CalendarUiState> = _calendarState.asStateFlow()

    private val _hijriState = MutableStateFlow(HijriCalendarUiState())
    val hijriState: StateFlow<HijriCalendarUiState> = _hijriState.asStateFlow()

    private val _eventsState = MutableStateFlow(EventsUiState())
    val eventsState: StateFlow<EventsUiState> = _eventsState.asStateFlow()

    private val _yearState = MutableStateFlow(YearOverviewUiState())
    val yearState: StateFlow<YearOverviewUiState> = _yearState.asStateFlow()

    init {
        // Drawn first, and unconditionally. These need no events — a month grid is dates —
        // so running them inside the events `try` meant one content-database fault left the
        // whole screen blank: no grid, no message, no retry.
        loadToday()
        observeEvents()
        observeDateRollover()
    }

    /**
     * Collects the events instead of reading them once.
     *
     * `getAllEvents().first()` was a one-shot read of a reactive source, cached forever. After
     * `ContentArtifactInstaller` replaces the content database mid-session — explicitly
     * supported, see `SUBSYSTEMS.md` §5 — the calendar served the *old* event set until the
     * ViewModel was recreated.
     */
    private fun observeEvents() {
        launchSafely(telemetry, DOMAIN, "load_events") {
            islamicEventUseCases.getAllEvents()
                .catchAndReport(telemetry, DOMAIN, "load_events") {
                    // Deliberately no fallback emission: there are no events to report, and
                    // emitting an empty list here would run the collector below and clear
                    // the very error just set.
                    _calendarState.update { it.copy(error = R.string.error_generic) }
                }
                .collect { events ->
                    cachedEvents = events
                    _calendarState.update { it.copy(error = null) }
                    redrawForDate(renderedFor)
                }
        }
    }

    /**
     * Re-draws when the day changes underneath an open screen.
     *
     * `isToday` is baked into each `CalendarDay` when its month is generated, so a grid built
     * at 23:59 kept highlighting yesterday until the user navigated away and back.
     */
    private fun observeDateRollover() {
        launchSafely(telemetry, DOMAIN, "observe_today") {
            todayProvider.todayChanges.collect { today ->
                if (today == renderedFor && _calendarState.value.currentMonth != null) return@collect
                renderedFor = today
                redrawForDate(today)
            }
        }
    }

    /** Re-issues everything scoped to "today" or to the current event set, as one unit. */
    private fun redrawForDate(today: LocalDate) {
        val month = _calendarState.value.currentMonth
            ?.days?.firstOrNull()?.gregorianDate
            ?: today
        navigateToMonth(month.monthValue, month.year)
        selectDate(_calendarState.value.selectedDate)
        loadUpcomingEvents()
    }

    fun onEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.SelectDate -> telemetry.featureUsed(DOMAIN, "select_date")
            is CalendarEvent.SetViewMode -> telemetry.featureUsed(DOMAIN, "set_view_mode")
            is CalendarEvent.NavigateToYear -> telemetry.featureUsed(DOMAIN, "navigate_year")

            else -> {}
        }
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
        val today = todayProvider.today()
        renderedFor = today
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
        val eventsForDate = calendarUseCases.eventsForDate(cachedEvents, hijriDate)

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

        monthJob?.cancel()
        monthJob = launchSafely(
            telemetry,
            DOMAIN,
            "navigate_month",
            onFailure = { _calendarState.update { it.copy(isLoading = false) } },
        ) {
            // Off the main thread: a month is ~30 Hijri conversions, and the year view below
            // is ~365 of them plus a filter per day.
            val (calendarMonth, eventsThisMonth) = withContext(defaultDispatcher) {
                calendarUseCases.buildGregorianMonth(month, year, cachedEvents, renderedFor) to
                    calendarUseCases.eventsForMonth(month, year, cachedEvents)
            }

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

        hijriJob?.cancel()
        hijriJob = launchSafely(
            telemetry,
            DOMAIN,
            "navigate_hijri_month",
            onFailure = { _hijriState.update { it.copy(isLoading = false) } },
        ) {
            val days = withContext(defaultDispatcher) {
                calendarUseCases.buildHijriMonth(month, year, cachedEvents, renderedFor)
            }

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

        yearJob?.cancel()
        yearJob = launchSafely(
            telemetry,
            DOMAIN,
            "navigate_year",
            onFailure = { _yearState.update { it.copy(isLoading = false) } },
        ) {
            val months = withContext(defaultDispatcher) {
                if (isHijri) {
                    generateHijriYearMonths(year)
                } else {
                    (1..12).map { month ->
                        calendarUseCases.buildGregorianMonth(month, year, cachedEvents, renderedFor)
                    }
                }
            }

            _yearState.update {
                it.copy(months = months, isLoading = false)
            }
        }
    }

    private fun loadUpcomingEvents() {
        upcomingJob?.cancel()
        upcomingJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_upcoming",
            onFailure = { _eventsState.update { it.copy(isLoading = false) } },
        ) {
            val events = withContext(defaultDispatcher) {
                calendarUseCases.upcomingEvents(cachedEvents, renderedFor)
            }

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

    private fun generateHijriYearMonths(year: Int): List<CalendarMonth> {
        // Still unimplemented, and still reachable only from a year view that no screen
        // wires up (#357). Left as-is rather than quietly deleted: whether the year view
        // ships at all is that issue's call, not this one's.
        return emptyList()
    }

    private companion object {
        private const val DOMAIN = AppAnalytics.Feature.CALENDAR
    }
}

