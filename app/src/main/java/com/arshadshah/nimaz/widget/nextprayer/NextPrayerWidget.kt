package com.arshadshah.nimaz.widget.nextprayer

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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.widget.WidgetUpdateScheduler
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetIcon
import com.arshadshah.nimaz.widget.core.WidgetLabel
import com.arshadshah.nimaz.widget.core.WidgetLoadingBox
import com.arshadshah.nimaz.widget.core.WidgetMessageBox
import com.arshadshah.nimaz.widget.core.WidgetPill
import com.arshadshah.nimaz.widget.core.prayerIconRes
import com.arshadshah.nimaz.widget.core.prayerShortName

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
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is NextPrayerWidgetState.Loading -> WidgetLoadingBox(
            background = backgroundColor,
            textSecondary = textSecondary,
            onClick = actionStartActivity<MainActivity>(),
        )

        is NextPrayerWidgetState.Success -> {
            NextPrayerSuccessContent(
                data = state.data,
                backgroundColor = backgroundColor,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )
        }

        is NextPrayerWidgetState.Error -> WidgetMessageBox(
            background = backgroundColor,
            onClick = actionStartActivity<MainActivity>(),
        ) {
            Text(
                text = context.getString(R.string.widget_tap_to_setup),
                style = TextStyle(color = textSecondary, fontSize = 12.sp)
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
    val liveCountdown = if (data.nextPrayerEpochMillis > 0L) {
        WidgetUpdateScheduler.computeCountdown(data.nextPrayerEpochMillis)
    } else {
        data.countdown.ifEmpty { "—" }
    }

    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 16.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetIcon(
                    resId = prayerIconRes(data.prayerName),
                    tint = primaryColor,
                    size = 16.dp,
                    contentDescription = data.prayerName,
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                WidgetLabel(
                    text = context.getString(R.string.widget_next_prayer),
                    color = textSecondary,
                )
            }
            Spacer(modifier = GlanceModifier.height(10.dp))
            Text(
                text = context.prayerShortName(data.prayerName).ifEmpty { "—" },
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
                    data.isTomorrow -> context.getString(R.string.widget_tomorrow)
                    else -> data.prayerTime.ifEmpty { "—" }
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

class NextPrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPrayerWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        NextPrayerWorker.enqueuePeriodicWork(context, force = true)
        WidgetUpdateScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        NextPrayerWorker.cancel(context)
        // Only cancel alarm if no other countdown widgets are active
        // For simplicity, always re-schedule — PrayerTimesWidget will also schedule
        WidgetUpdateScheduler.cancel(context)
    }
}
