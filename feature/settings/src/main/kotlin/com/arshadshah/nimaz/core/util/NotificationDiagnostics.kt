package com.arshadshah.nimaz.core.util

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

/**
 * The device-level prerequisites for a prayer alert actually arriving on time.
 *
 * Every field here is read from the OS. Nothing is assumed and nothing is reported that
 * cannot be checked — a row that always says "OK" would be worse than no row, because it
 * would send someone looking for the fault somewhere else.
 */
data class NotificationDiagnostics(
    /** The user has granted POST_NOTIFICATIONS (or is on a release that never asked). */
    val notificationsPermitted: Boolean,
    /** The app may schedule exact alarms. Without it, alerts drift by minutes. */
    val exactAlarmsAllowed: Boolean,
    /** The app is exempt from battery optimisation, so Doze will not hold alarms back. */
    val batteryUnrestricted: Boolean,
) {
    /** True when at least one prerequisite is missing — what the hub's warning keys off. */
    val hasProblem: Boolean
        get() = !notificationsPermitted || !exactAlarmsAllowed || !batteryUnrestricted

    companion object {
        fun read(context: Context): NotificationDiagnostics {
            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val powerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager

            return NotificationDiagnostics(
                notificationsPermitted = NotificationManagerCompat.from(context)
                    .areNotificationsEnabled(),
                // The permission only exists from Android 12; before that exact alarms are
                // always allowed, so reporting anything else would be inventing a fault.
                exactAlarmsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                },
                batteryUnrestricted = powerManager
                    .isIgnoringBatteryOptimizations(context.packageName),
            )
        }
    }
}
