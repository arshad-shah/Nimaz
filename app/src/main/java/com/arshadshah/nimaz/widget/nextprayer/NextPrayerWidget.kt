package com.arshadshah.nimaz.widget.nextprayer

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.widget.WidgetUpdateScheduler

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
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is NextPrayerWidgetState.Loading -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "Loading...",
                        style = TextStyle(color = textSecondary, fontSize = 12.sp)
                    )
                }
            }
        }

        is NextPrayerWidgetState.Success -> {
            NextPrayerSuccessContent(
                data = state.data,
                backgroundColor = backgroundColor,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )
        }

        is NextPrayerWidgetState.Error -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tap to setup",
                        style = TextStyle(color = textSecondary, fontSize = 12.sp)
                    )
                }
            }
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
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Next Prayer",
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = data.prayerName.ifEmpty { "—" },
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = data.prayerTime.ifEmpty { "—" },
                style = TextStyle(
                    color = textColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Compute countdown live from stored epoch for freshness
            val liveCountdown = if (data.nextPrayerEpochMillis > 0L) {
                WidgetUpdateScheduler.computeCountdown(data.nextPrayerEpochMillis)
            } else {
                data.countdown.ifEmpty { "—" }
            }

            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(R.color.widget_primary_dim))
                    .cornerRadius(8.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (data.isValid && liveCountdown != "—") "in " else "",
                        style = TextStyle(color = primaryColor, fontSize = 12.sp)
                    )
                    Text(
                        text = liveCountdown,
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
