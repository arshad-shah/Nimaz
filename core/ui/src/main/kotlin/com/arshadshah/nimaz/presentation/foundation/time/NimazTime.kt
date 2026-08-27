package com.arshadshah.nimaz.presentation.foundation.time

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.core.ui.R

/**
 * A time of day as hour-of-day (0-23) and minute.
 *
 * Always stored in 24-hour form no matter how it is displayed, so persisted values and
 * the alarm scheduler never have to care about the device's clock format.
 */
data class NimazTime(val hour: Int, val minute: Int) {

    /** Zero-padded 24-hour "HH:mm", the form persisted in preferences. */
    fun toStorageString(): String = "%02d:%02d".format(hour, minute)

    /** True for midday onwards — i.e. the PM half in 12-hour display. */
    val isPm: Boolean get() = hour >= 12

    /** This time's hour on a 12-hour clock, where midnight and midday are both 12. */
    val hour12: Int
        get() = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }

    /** Rebuilds a 24-hour [hour] from a 12-hour reading. */
    fun withHour12(hour12: Int, pm: Boolean): NimazTime {
        val base = hour12 % 12
        return copy(hour = if (pm) base + 12 else base)
    }

    companion object {
        /** Parses "HH:mm", falling back to [fallback] for malformed or out-of-range input. */
        fun parse(value: String?, fallback: NimazTime = NimazTime(6, 0)): NimazTime {
            val parts = value?.split(":") ?: return fallback
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return fallback
            val m = parts.getOrNull(1)?.toIntOrNull() ?: return fallback
            if (h !in 0..23 || m !in 0..59) return fallback
            return NimazTime(h, m)
        }
    }
}

/**
 * Formats a time for display, honouring the device's 12/24-hour setting.
 */
@Composable
fun rememberTimeFormatter(): (NimazTime) -> String {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val am = stringResource(R.string.time_period_am)
    val pm = stringResource(R.string.time_period_pm)
    return remember(is24Hour, am, pm) {
        { time ->
            if (is24Hour) time.toStorageString()
            else "%d:%02d %s".format(time.hour12, time.minute, if (time.isPm) pm else am)
        }
    }
}
