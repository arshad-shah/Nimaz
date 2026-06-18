package com.arshadshah.nimaz.presentation.components.molecules.calendar

/**
 * Static, non-UI constants for the calendar component.
 */

/**
 * Weekday abbreviations, uppercase for header-style typography. Kept as a
 * module-level constant so the rendering composable stays declarative.
 */
internal val WEEKDAY_LABELS = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

/**
 * Index of Friday within [WEEKDAY_LABELS]. Friday gets special emphasis in the
 * header as a nod to Jumu'ah, the most significant day of the Islamic week.
 */
internal const val FRIDAY_INDEX = 5
