package com.arshadshah.nimaz.presentation.viewmodel.prayer

import java.time.LocalDate
import java.time.YearMonth
import com.arshadshah.nimaz.domain.model.DayPrayerTimes

/**
 * [currentMonth] and [expandedDay] are **not** defaulted to `now()`.
 *
 * They were, and a data-class default is evaluated when the instance is constructed — which for a
 * ViewModel's initial state is once, at `init`. After a month boundary `currentMonth` was still
 * the previous month until the user tapped, and `expandedDay` highlighted a row in the wrong
 * month. It also made the state class untestable without freezing the system clock, because
 * merely constructing it read the wall clock.
 *
 * The ViewModel sets both from `TodayProvider`, which also tells it when the day changes.
 *
 * [locationName] is null until a location resolves, and [isUsingFallbackLocation] says whether
 * the coordinates behind the timetable are the user's or the app's default. The screen turns
 * those two into copy; the ViewModel does not hardcode a city.
 */
data class MonthlyPrayerTimesUiState(
    val currentMonth: YearMonth? = null,
    val dayPrayerTimes: List<DayPrayerTimes> = emptyList(),
    val locationName: String? = null,
    val isUsingFallbackLocation: Boolean = false,
    val methodLabel: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val ramadanHijriYear: Int? = null,
    val isLoading: Boolean = true,
    val expandedDay: LocalDate? = null,
    /**
     * The Ramadan timetable, once computed, waiting for the share sheet to take it. Null both
     * before it is asked for and after it has been consumed — a one-shot in state rather than a
     * ViewModel method the screen calls for a value.
     */
    val ramadanExport: List<DayPrayerTimes>? = null,
)
