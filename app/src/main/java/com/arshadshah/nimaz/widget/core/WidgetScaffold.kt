package com.arshadshah.nimaz.widget.core

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.core.ui.R

/**
 * The frame every widget shares, minus the part that differs.
 *
 * All six widgets open with the same four colour reads and the same two non-success branches
 * — a loading box and a "tap to refresh" message, both opening `MainActivity` — before
 * anything specific to that widget happens (#488). The success branch is the whole of what
 * makes a widget itself, so it stays where it is.
 *
 * [WidgetPalette] was already here for the colour half and **no widget used it**: the type had
 * been extracted and never adopted, so the four lines stayed copied six times anyway. That is
 * the other half of this change, and the reason an unused helper is worse than none — it reads
 * as done.
 */

/** The "still loading" frame. Tapping it opens the app, like every other widget state. */
@Composable
fun WidgetLoading(palette: WidgetPalette) {
    WidgetLoadingBox(
        background = palette.background,
        textSecondary = palette.textSecondary,
        onClick = actionStartActivity<MainActivity>(),
    )
}

/**
 * The "something went wrong" frame.
 *
 * Deliberately not an error message: a widget cannot explain itself in the space it has, and
 * the useful thing a reader can do is open the app, which is what the tap does.
 */
@Composable
fun WidgetError(palette: WidgetPalette) {
    // Reads the context itself, the way `WidgetLoadingBox` does — a caller that only wants
    // the standard error frame should not have to thread one through.
    val context = LocalContext.current
    WidgetMessageBox(
        background = palette.background,
        onClick = actionStartActivity<MainActivity>(),
    ) {
        Text(
            text = context.getString(R.string.widget_tap_to_refresh),
            style = TextStyle(color = palette.textSecondary, fontSize = 12.sp)
        )
    }
}

/**
 * A receiver whose whole job is starting and stopping its widget's refresh work.
 *
 * All six did exactly this, and the two countdown widgets additionally drove
 * `WidgetUpdateScheduler`. Subclasses say which widget and which work; [onWidgetPresent] and
 * [onWidgetAbsent] are the hook for anything else, which is the alarm for those two.
 *
 * **Every lifecycle callback is `final` on purpose:** forgetting the `super` call in a Glance
 * receiver is a silent break, and there is nothing a subclass needs from overriding them that
 * the hooks do not give it.
 *
 * ### Why [onUpdate] re-arms everything
 *
 * Refresh used to be armed in `onEnabled` alone. `onEnabled` fires once — when the *first*
 * instance of a provider is placed — and never again while any instance remains, so it was the
 * app's only chance to get its scheduling right, for the lifetime of the widget. Anything that
 * lost that schedule afterwards was permanent: a force-stop drops the app's jobs and alarms, and
 * `AlarmManager` alarms in particular do not survive a reboot at all, so the per-minute countdown
 * tick simply stopped the first time a user restarted their phone and never came back. The widget
 * kept drawing whatever the 15-minute worker last stored.
 *
 * `onUpdate` is the recovery channel, and its value is that it does not depend on anything the
 * app persisted: the system broadcasts it on boot, after a package update, and every
 * `updatePeriodMillis`, whatever state WorkManager is or is not in. Re-arming here is idempotent
 * — periodic work is enqueued with `KEEP`, and the alarm's `PendingIntent` replaces itself — so
 * the cost of it being unnecessary is nil, and the cost of it being missing is a dead widget.
 */
abstract class WidgetWorkReceiver : GlanceAppWidgetReceiver() {

    abstract override val glanceAppWidget: GlanceAppWidget

    /**
     * Start this widget's periodic refresh.
     *
     * @param force restart the schedule even if one already exists. True when the widget has
     *   just been placed, false when re-arming something that should already be running.
     */
    protected abstract fun enqueueWork(context: Context, force: Boolean)

    /** Refresh this widget's data once, now. */
    protected abstract fun refreshNow(context: Context)

    /** Stop the periodic refresh. */
    protected abstract fun cancelWork(context: Context)

    /** Anything else that must run while this widget is on a home screen. */
    protected open fun onWidgetPresent(context: Context) = Unit

    /** The counterpart to [onWidgetPresent], for when the last instance is removed. */
    protected open fun onWidgetAbsent(context: Context) = Unit

    final override fun onEnabled(context: Context) {
        super.onEnabled(context)
        enqueueWork(context, force = true)
        // The first periodic run is not guaranteed to be prompt, and a widget that sits on
        // em-dash skeletons for its first quarter of an hour reads as broken.
        refreshNow(context)
        onWidgetPresent(context)
    }

    final override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        enqueueWork(context, force = false)
        refreshNow(context)
        onWidgetPresent(context)
    }

    final override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWork(context)
        onWidgetAbsent(context)
    }
}
