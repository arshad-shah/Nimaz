package com.arshadshah.nimaz.widget.core

/**
 * 12-hour clock formatting shared by the prayer widgets.
 *
 * @param includeAmPm when true, appends " AM"/" PM" (used by the next-prayer
 *        widget); when false, returns a bare "h:mm" (used by the prayer-times
 *        grid).
 */
fun formatWidgetTime(hour: Int, minute: Int, includeAmPm: Boolean = false): String {
    val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
    return if (includeAmPm) {
        val amPm = if (hour >= 12) "PM" else "AM"
        String.format("%d:%02d %s", displayHour, minute, amPm)
    } else {
        String.format("%d:%02d", displayHour, minute)
    }
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
