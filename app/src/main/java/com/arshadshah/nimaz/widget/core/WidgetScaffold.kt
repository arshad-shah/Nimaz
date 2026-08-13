package com.arshadshah.nimaz.widget.core

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
import com.arshadshah.nimaz.R

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
 * `WidgetUpdateScheduler`. Subclasses say which widget and which work; [onWidgetEnabled] and
 * [onWidgetDisabled] are the hook for anything else, which is the alarm for those two.
 *
 * `onEnabled`/`onDisabled` are `final` on purpose: forgetting the `super` call in a Glance
 * receiver is a silent break, and there is nothing a subclass needs from overriding them that
 * the two hooks do not give it.
 */
abstract class WidgetWorkReceiver : GlanceAppWidgetReceiver() {

    abstract override val glanceAppWidget: GlanceAppWidget

    /** Start this widget's periodic refresh. */
    protected abstract fun enqueueWork(context: Context)

    /** Stop it. */
    protected abstract fun cancelWork(context: Context)

    protected open fun onWidgetEnabled(context: Context) = Unit
    protected open fun onWidgetDisabled(context: Context) = Unit

    final override fun onEnabled(context: Context) {
        super.onEnabled(context)
        enqueueWork(context)
        onWidgetEnabled(context)
    }

    final override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWork(context)
        onWidgetDisabled(context)
    }
}
