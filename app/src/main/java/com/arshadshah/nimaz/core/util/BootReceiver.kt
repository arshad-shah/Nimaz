package com.arshadshah.nimaz.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.widget.WidgetUpdateScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Puts the app's alarms back after the device restarts or the app is replaced.
 *
 * **This used to be 812 lines doing two unrelated jobs.** Delivering five kinds of alarm is now
 * [PrayerAlarmReceiver]'s; what is left is recovery, which is genuinely an `:app` concern: it is
 * a manifest entry point, and it re-arms both the prayer alarms *and* the widget tick, which
 * lives in `:feature:widget`. That second call is why the split had to happen before
 * `:core:notifications` could exist — a `:core:*` module naming `WidgetUpdateScheduler` is a
 * `:core:*` -> `:feature:*` edge, and `moduleBoundary` fails the build on it.
 *
 * ### Why `MY_PACKAGE_REPLACED` is here
 *
 * An `AlarmManager` alarm holds a `PendingIntent`, and a `PendingIntent` names a component. Every
 * alarm armed by a build before this one names `BootReceiver`; they are answered by
 * [PrayerAlarmReceiver] now, so those alarms would fire into a receiver that no longer handles
 * their action and do nothing at all — silently, with no crash and no error state.
 *
 * `AppInitializer` re-arms everything on launch, so anyone who opens the app self-heals. Someone
 * who does not open it would have lost their prayer notifications indefinitely.
 * `MY_PACKAGE_REPLACED` closes that: the OS broadcasts it to the app it just replaced, taking it
 * out of the stopped state to do so, and the reschedule below re-arms every alarm against the new
 * component within seconds of the update — opened or not.
 *
 * ### What is deliberately *not* handled
 *
 * `ACTION_LOCKED_BOOT_COMPLETED` was in the `when` and had no manifest filter, so it never
 * arrived. Declaring one would need `android:directBootAware="true"`, and this receiver's work
 * reaches DataStore and Room, both of which live in credential-protected storage and are
 * unreadable before first unlock. The branch is removed rather than left looking supported.
 *
 * The two `QUICKBOOT_POWERON` actions were in the same position — handled in code, absent from
 * the manifest — but those *can* work: they are what some OEM fast-boot implementations send
 * instead of `BOOT_COMPLETED`, and the work they trigger needs nothing special. They are declared
 * now, so the code already written for them runs.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prayerRescheduler: PrayerRescheduler

    /**
     * Re-arm today's prayer alarms and the widget tick.
     *
     * Wrapped in [goAsync] because everything below outlives `onReceive`, and the moment
     * `onReceive` returns Android is free to decide the process has nothing to do and kill it.
     * That is worst here of anywhere: a reboot is precisely when this work has not been done yet
     * and nothing else will do it. The receiver instance is discarded after each call, so the
     * `CoroutineScope` this class used to hold as a field was never cancelled either.
     *
     * `goAsync()` returns `null` when the receiver was not dispatched by the framework — a unit
     * test calling `onReceive` directly — so it is treated as nullable. There is nothing to
     * finish in that case.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val trigger = TRIGGERS[intent.action] ?: return

        AppAnalytics.logNotificationReschedule(trigger = trigger)

        val pendingResult: BroadcastReceiver.PendingResult? = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                prayerRescheduler.rescheduleToday()
                // Widget periodic work is WorkManager's and survives the reboot on its own; the
                // per-minute countdown alarm is ours and does not. Nothing re-armed it, so the
                // countdown widgets stopped ticking at the first restart after they were placed
                // and never started again.
                WidgetUpdateScheduler.ensureScheduled(context)
            } catch (e: Exception) {
                // A receiver has nowhere to propagate to, and crashing on boot is worse than one
                // missed re-arm. Reported, then swallowed — but never at the cost of `finish()`,
                // which is what releases the wake lock `goAsync()` took.
                CrashReporter.recordException(e)
                AppAnalytics.logError("boot_reschedule", e.javaClass.simpleName, e.message)
            } finally {
                pendingResult?.finish()
                scope.cancel()
            }
        }
    }

    companion object {
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"

        /**
         * Action -> the `trigger` it reports as, which doubles as the set of actions handled.
         *
         * The analytics value is read as a funnel, and "boot" and "package_replaced" answer very
         * different questions about why notifications came back. Folding the second into the
         * first would make the recovery this receiver now performs invisible in exactly the data
         * that would show it working.
         */
        val TRIGGERS = mapOf(
            Intent.ACTION_BOOT_COMPLETED to "boot",
            ACTION_QUICKBOOT_POWERON to "boot",
            ACTION_HTC_QUICKBOOT_POWERON to "boot",
            Intent.ACTION_MY_PACKAGE_REPLACED to "package_replaced",
        )
    }
}
