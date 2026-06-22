package com.arshadshah.nimaz.core.util

import java.time.LocalDate

/** Number of milliseconds in a calendar day. */
const val MILLIS_PER_DAY: Long = 86_400_000L

/**
 * Epoch milliseconds at UTC midnight of this date.
 *
 * This is the canonical key used for date-bucketed database rows (prayer
 * records, fasts, tasbih sessions, …). The codebase previously computed it two
 * equivalent-but-different-looking ways — `toEpochDay() * 86400000L` and
 * `atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000` — which are unified here.
 */
fun LocalDate.toUtcMidnightMillis(): Long = toEpochDay() * MILLIS_PER_DAY
