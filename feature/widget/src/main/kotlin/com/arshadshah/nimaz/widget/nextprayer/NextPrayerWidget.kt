package com.arshadshah.nimaz.widget.nextprayer

import com.arshadshah.nimaz.widget.core.launchAppComponent

import com.arshadshah.nimaz.core.ui.R as UiR
import com.arshadshah.nimaz.feature.widget.R

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.widget.core.WidgetError
import com.arshadshah.nimaz.widget.core.WidgetLoading
import com.arshadshah.nimaz.widget.core.WidgetPalette
import com.arshadshah.nimaz.widget.core.WidgetWorkReceiver
import com.arshadshah.nimaz.widget.WidgetUpdateScheduler
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetIcon
import com.arshadshah.nimaz.widget.core.WidgetLabel
import com.arshadshah.nimaz.widget.core.WidgetMessageBox
import com.arshadshah.nimaz.widget.core.WidgetPill
import com.arshadshah.nimaz.widget.core.prayerIconRes
import com.arshadshah.nimaz.presentation.foundation.tokens.prayerShortName

class NextPrayerWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<NextPrayerWidgetState> =
        NextPrayerStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState<NextPrayerWidgetState>()
                NextPrayerContent(state)
            }
        }
    }
}

@Composable
private fun NextPrayerContent(state: NextPrayerWidgetState) {
    val context = LocalContext.current
    val palette = WidgetPalette()

    when (state) {
        is NextPrayerWidgetState.Loading -> WidgetLoading(palette)

        is NextPrayerWidgetState.Success -> {
            NextPrayerSuccessContent(
                data = state.data,
                backgroundColor = palette.background,
                textColor = palette.text,
                textSecondary = palette.textSecondary,
                primaryColor = palette.primary
            )
        }

        is NextPrayerWidgetState.Error -> WidgetMessageBox(
            background = palette.background,
            onClick = actionStartActivity(LocalContext.current.launchAppComponent()),
        ) {
            Text(
                text = context.getString(R.string.widget_tap_to_setup),
                style = TextStyle(color = palette.textSecondary, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun NextPrayerSuccessContent(
    data: NextPrayerData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    val context = LocalContext.current

    // Which prayer is next is decided here, not by the worker: the widget redraws every minute
    // and the worker runs every fifteen, so reading the worker's answer meant naming a prayer
    // that had already started for up to a quarter of an hour. `schedule` is empty only for
    // state written by a version that did not persist one, in which case the flat fields — the
    // worker's answer — are all there is.
    val entry = data.nextEntry(System.currentTimeMillis())

    val liveCountdown = if (entry.epochMillis > 0L) {
        WidgetUpdateScheduler.computeCountdown(entry.epochMillis)
    } else {
        data.countdown.ifEmpty { "—" }
    }

    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity(LocalContext.current.launchAppComponent()),
        padding = 16.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetIcon(
                    resId = prayerIconRes(entry.prayerName),
                    tint = primaryColor,
                    size = 16.dp,
                    contentDescription = entry.prayerName,
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                WidgetLabel(
                    text = context.getString(UiR.string.widget_next_prayer),
                    color = textSecondary,
                )
            }
            Spacer(modifier = GlanceModifier.height(10.dp))
            Text(
                text = context.prayerShortName(entry.prayerName).ifEmpty { "—" },
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                // The worker stores a clock time, or nothing plus `isTomorrow` — resolving
                // the word here means a language change lands on the next redraw rather than
                // waiting up to 30 minutes for the worker.
                text = when {
                    entry.isTomorrow -> context.getString(R.string.widget_tomorrow)
                    else -> entry.prayerTime.ifEmpty { "—" }
                },
                style = TextStyle(
                    color = textColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            WidgetPill(container = ColorProvider(R.color.widget_primary_dim)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (data.isValid && liveCountdown != "—") "in " else "",
                        style = TextStyle(color = primaryColor, fontSize = 12.sp),
                    )
                    Text(
                        text = liveCountdown,
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                    )
                }
            }
        }
    }
}

class NextPrayerWidgetReceiver : WidgetWorkReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPrayerWidget()

    override fun enqueueWork(context: Context, force: Boolean) =
        NextPrayerWorker.enqueuePeriodicWork(context, force = force)

    override fun refreshNow(context: Context) = NextPrayerWorker.enqueueImmediateWork(context)

    override fun cancelWork(context: Context) = NextPrayerWorker.cancel(context)

    override fun onWidgetPresent(context: Context) =
        WidgetUpdateScheduler.schedule(context)

    override fun onWidgetAbsent(context: Context) =
        WidgetUpdateScheduler.cancelIfUnused(context)
}
