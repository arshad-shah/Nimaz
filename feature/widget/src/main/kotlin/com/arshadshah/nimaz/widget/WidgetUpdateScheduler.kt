package com.arshadshah.nimaz.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.common.formatWidgetCountdown
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidget
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidgetReceiver
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidget
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Schedules per-minute widget updates using AlarmManager.
 * WorkManager has a 15-minute minimum interval, so we use AlarmManager
 * for more frequent countdown refreshes.
 *
 * **One alarm serves both countdown widgets**, which is why [cancelIfUnused] exists and a plain
 * `cancel` no longer does: the two receivers share a request code, so the alarm they arm is the
 * same alarm, and the widget that was removed used to cancel the tick belonging to the one that
 * stayed. The surviving widget then fell back to its 15-minute worker and its countdown visibly
 * froze — the sort of thing that looks like the widget "sometimes not working".
 *
 * Alarms do not survive a reboot, and nothing re-armed this one, so the same freeze happened to
 * everybody on their next restart. [ensureScheduled] is the recovery, called from `BootReceiver`
 * and from every widget `onUpdate`.
 */
object WidgetUpdateScheduler {

    private const val ACTION_WIDGET_TICK = "com.arshadshah.nimaz.ACTION_WIDGET_TICK"
    private const val REQUEST_CODE = 9876
    private const val INTERVAL_MS = 60_000L // 1 minute

    /**
     * Arm the tick. Idempotent — `FLAG_UPDATE_CURRENT` reuses the one `PendingIntent`, so calling
     * this on every `onUpdate` re-arms a lost alarm without ever stacking a second one.
     */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = tickIntent(context)

        // Use setInexactRepeating for battery efficiency — close enough for countdown display
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            INTERVAL_MS,
            pendingIntent
        )
    }

    /**
     * Arm the tick if either countdown widget is on a home screen.
     *
     * The counterpart to [cancelIfUnused], for the paths that know a widget might be placed but
     * not whether one is: boot, and a package update.
     */
    fun ensureScheduled(context: Context) {
        if (isAnyCountdownWidgetPlaced(context)) schedule(context)
    }

    /**
     * Cancel the tick, but only once **neither** countdown widget is placed.
     *
     * Called from a receiver's `onDisabled`, which runs after that provider's last instance is
     * gone — so the check below sees the truth about the other one.
     */
    fun cancelIfUnused(context: Context) {
        if (isAnyCountdownWidgetPlaced(context)) return
        cancel(context)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(tickIntent(context))
    }

    /**
     * Compute a live countdown string from a target epoch.
     * Returns a formatted string like "2h 30m", "15m 42s", or "30s".
     */
    fun computeCountdown(targetEpochMillis: Long): String {
        if (targetEpochMillis <= 0L) return "—"
        val diff = targetEpochMillis - System.currentTimeMillis()
        return formatWidgetCountdown(diff / 1000)
    }

    private fun tickIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetTickReceiver::class.java).apply {
            action = ACTION_WIDGET_TICK
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun isAnyCountdownWidgetPlaced(context: Context): Boolean = try {
        val manager = AppWidgetManager.getInstance(context)
        manager != null && COUNTDOWN_RECEIVERS.any { receiver ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
        }
    } catch (e: Exception) {
        // A device without an AppWidget host, or a host that is not up yet. Assume placed: an
        // alarm that ticks for nothing costs a redraw a minute, a tick that never fires costs a
        // widget that stops updating.
        CrashReporter.recordException(e)
        true
    }

    /** The two widgets that draw a live countdown, and so need the one-minute tick. */
    private val COUNTDOWN_RECEIVERS = listOf(
        NextPrayerWidgetReceiver::class.java,
        PrayerTimesWidgetReceiver::class.java,
    )
}

class WidgetTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                NextPrayerWidget().updateAll(context)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                Log.e("WidgetTickReceiver", "Failed to update NextPrayerWidget", e)
            }
            try {
                PrayerTimesWidget().updateAll(context)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                Log.e("WidgetTickReceiver", "Failed to update PrayerTimesWidget", e)
            }
            pendingResult.finish()
        }
    }
}
