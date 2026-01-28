package com.arshadshah.nimaz.widget.prayertimes

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

class PrayerTimesWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<PrayerTimesWidgetState> =
        PrayerTimesStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState<PrayerTimesWidgetState>()
                PrayerTimesContent(state)
            }
        }
    }
}

@Composable
private fun PrayerTimesContent(state: PrayerTimesWidgetState) {
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is PrayerTimesWidgetState.Loading -> {
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

        is PrayerTimesWidgetState.Success -> {
            PrayerTimesSuccessContent(
                data = state.data,
                backgroundColor = backgroundColor,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )
        }

        is PrayerTimesWidgetState.Error -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap to setup",
                    style = TextStyle(color = textSecondary, fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
private fun PrayerTimesSuccessContent(
    data: PrayerTimesData,
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
            .padding(12.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = data.locationName.ifEmpty { "Location" },
                        style = TextStyle(
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = data.hijriDate.ifEmpty { "—" },
                        style = TextStyle(color = textSecondary, fontSize = 11.sp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = data.nextPrayerName.ifEmpty { "—" },
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (data.timeUntilNext.isNotEmpty()) "in ${data.timeUntilNext}" else "—",
                        style = TextStyle(color = textSecondary, fontSize = 10.sp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Prayer times row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrayerTimeItem("Fajr", data.fajrTime, data.fajrPassed, textColor, textSecondary, primaryColor, GlanceModifier.defaultWeight())
                PrayerTimeItem("Dhuhr", data.dhuhrTime, data.dhuhrPassed, textColor, textSecondary, primaryColor, GlanceModifier.defaultWeight())
                PrayerTimeItem("Asr", data.asrTime, data.asrPassed, textColor, textSecondary, primaryColor, GlanceModifier.defaultWeight())
                PrayerTimeItem("Mgrb", data.maghribTime, data.maghribPassed, textColor, textSecondary, primaryColor, GlanceModifier.defaultWeight())
                PrayerTimeItem("Isha", data.ishaTime, data.ishaPassed, textColor, textSecondary, primaryColor, GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun PrayerTimeItem(
    name: String,
    time: String,
    isPassed: Boolean,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            style = TextStyle(
                color = if (isPassed) textSecondary else textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = time.ifEmpty { "—" },
            style = TextStyle(
                color = if (isPassed) textSecondary else primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PrayerTimesWorker.enqueuePeriodicWork(context, force = true)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        PrayerTimesWorker.cancel(context)
    }
}
