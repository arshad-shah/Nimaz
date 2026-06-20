package com.arshadshah.nimaz.presentation.components.molecules.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure date helpers for laying out and labelling the calendar grid. No Compose
 * or UI dependencies — easy to unit-test and reuse.
 */

/**
 * Builds the date grid for a month, including padding days from the previous
 * month to fill the first week. Returns 5 weeks (35 cells) for months that
 * fit, or 6 weeks (42 cells) for months that need the extra row — e.g. a
 * 31-day month starting on Friday/Saturday wraps over 6 weeks and the
 * previous hard-coded 35-cell list silently truncated the last days.
 */
internal fun buildCalendarDays(yearMonth: YearMonth): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    val offset = if (firstOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0
    else firstOfMonth.dayOfWeek.value
    val startDate = firstOfMonth.minusDays(offset.toLong())
    val totalDays = offset + yearMonth.lengthOfMonth()
    val weeks = ((totalDays + 6) / 7).coerceIn(5, 6)
    return List(weeks * 7) { startDate.plusDays(it.toLong()) }
}

/**
 * Default "Month Year" header title, e.g. "January 2026".
 */
internal fun YearMonth.formatDefault(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$monthName $year"
}
