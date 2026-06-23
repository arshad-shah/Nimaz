package com.arshadshah.nimaz.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Number of milliseconds in a calendar day. */
const val MILLIS_PER_DAY: Long = 86_400_000L

/**
 * Full weekday + long date, e.g. "Monday, January 5, 2026".
 *
 * Shared by the home greeting and prayer-tracker headers, which previously each
 * built their own `DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")`.
 */
val FULL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

/**
 * Month + year, e.g. "January 2026".
 *
 * Shared by the monthly prayer-times and prayer-stats month headers.
 */
val MONTH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")


/**
 * Epoch milliseconds at UTC midnight of this date.
 *
 * This is the canonical key used for date-bucketed database rows (prayer
 * records, fasts, tasbih sessions, …). The codebase previously computed it two
 * equivalent-but-different-looking ways — `toEpochDay() * 86400000L` and
 * `atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000` — which are unified here.
 */
fun LocalDate.toUtcMidnightMillis(): Long = toEpochDay() * MILLIS_PER_DAY
