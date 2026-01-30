package com.arshadshah.nimaz.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.glance.appwidget.updateAll
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidget
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Schedules per-minute widget updates using AlarmManager.
 * WorkManager has a 15-minute minimum interval, so we use AlarmManager
 * for more frequent countdown refreshes.
 */
object WidgetUpdateScheduler {

    private const val ACTION_WIDGET_TICK = "com.arshadshah.nimaz.ACTION_WIDGET_TICK"
    private const val REQUEST_CODE = 9876
    private const val INTERVAL_MS = 60_000L // 1 minute

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetTickReceiver::class.java).apply {
            action = ACTION_WIDGET_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setInexactRepeating for battery efficiency — close enough for countdown display
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            INTERVAL_MS,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetTickReceiver::class.java).apply {
            action = ACTION_WIDGET_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Compute a live countdown string from a target epoch.
     * Returns a formatted string like "2h 30m", "15m 42s", or "30s".
     */
    fun computeCountdown(targetEpochMillis: Long): String {
        if (targetEpochMillis <= 0L) return "—"
        val now = System.currentTimeMillis()
        val diff = targetEpochMillis - now
        if (diff <= 0L) return "—"

        val totalSeconds = diff / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

class WidgetTickReceiver : BroadcastReceiver() {
    private val scope = MainScope()

    override fun onReceive(context: Context, intent: Intent?) {
        scope.launch {
            try {
                NextPrayerWidget().updateAll(context)
            } catch (_: Exception) {}
            try {
                PrayerTimesWidget().updateAll(context)
            } catch (_: Exception) {}
        }
    }
}
