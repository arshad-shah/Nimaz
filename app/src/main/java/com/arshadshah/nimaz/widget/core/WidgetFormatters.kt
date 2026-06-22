package com.arshadshah.nimaz.widget.core

import com.arshadshah.nimaz.core.util.formatClockTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Clock formatting shared by the prayer widgets, rendered in the device's
 * default locale (localized am/pm marker and digits — Nimaz is worldwide).
 *
 * @param includeAmPm when true, appends the locale's am/pm marker (used by the
 *        next-prayer widget); when false, returns a bare "h:mm" (used by the
 *        compact prayer-times grid). Ignored when [use24Hour] is true.
 * @param use24Hour the user's 24-hour-clock preference; renders "HH:mm".
 */
fun formatWidgetTime(
    hour: Int,
    minute: Int,
    includeAmPm: Boolean = false,
    use24Hour: Boolean = false,
): String {
    if (use24Hour || includeAmPm) return formatClockTime(hour, minute, use24Hour)
    // 12-hour without the am/pm marker — the grid is too narrow for it.
    return LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        .format(DateTimeFormatter.ofPattern("h:mm"))
}

/**
 * Format a remaining duration (in whole seconds) as a compact countdown such as
 * "2h 30m", "15m 42s" or "30s". Non-positive durations render as an em dash.
 */
fun formatWidgetCountdown(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "—"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
