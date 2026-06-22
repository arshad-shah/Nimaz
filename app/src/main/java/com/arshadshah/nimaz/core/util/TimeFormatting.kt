package com.arshadshah.nimaz.core.util

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Canonical wall-clock time formatting for on-screen display.
 *
 * Nimaz is used worldwide, so time is rendered in the device's **default
 * locale**: the am/pm marker and the digits follow the user's locale (e.g.
 * Arabic-Indic numerals "٣:٤٥ م" on an Arabic device). 12-hour ("3:45 PM") by
 * default, 24-hour ("15:45") when [use24Hour] is true — pass the user's
 * `use24HourFormat` preference.
 *
 * This replaces the dozen-odd copy-pasted `String.format("%d:%02d %s", …,
 * "AM"/"PM")` helpers and ad-hoc `DateTimeFormatter.ofPattern("h:mm a",
 * Locale.US/ENGLISH)` instances that previously hardcoded English am/pm.
 *
 * The formatter is built per call so it always reflects the *current* default
 * locale (it can change at runtime without the process restarting).
 */
fun formatClockTime(hour: Int, minute: Int, use24Hour: Boolean): String =
    LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).formatClock(use24Hour)

/** Format a [LocalTime] for display — see [formatClockTime]. */
fun LocalTime.formatClock(use24Hour: Boolean): String = format(clockFormatter(use24Hour))

/** Format a [LocalDateTime]'s time-of-day for display — see [formatClockTime]. */
fun LocalDateTime.formatClock(use24Hour: Boolean): String = format(clockFormatter(use24Hour))

private fun clockFormatter(use24Hour: Boolean): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a")

/**
 * Format an elapsed length in minutes as "13h 45m" — used for fasting length
 * (Fajr → Maghrib). This is a duration, not a clock time, so it is independent
 * of the 12/24-hour preference.
 */
fun formatFastLength(minutes: Int): String =
    "${minutes / 60}h ${"%02d".format(minutes % 60)}m"
