package com.arshadshah.nimaz.widget.prayertimes

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
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
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetLoadingBox
import com.arshadshah.nimaz.widget.core.WidgetMessageBox

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
    val context = LocalContext.current
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is PrayerTimesWidgetState.Loading -> WidgetLoadingBox(
            background = backgroundColor,
            textSecondary = textSecondary,
            onClick = actionStartActivity<MainActivity>(),
        )

        is PrayerTimesWidgetState.Success -> {
            PrayerTimesSuccessContent(
                data = state.data,
                backgroundColor = backgroundColor,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )
        }

        is PrayerTimesWidgetState.Error -> WidgetMessageBox(
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

private enum class PrayerCellState { PAST, NEXT, UPCOMING }

@Composable
private fun PrayerTimesSuccessContent(
    data: PrayerTimesData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    val liveCountdown = if (data.nextPrayerEpochMillis > 0L) {
        WidgetUpdateScheduler.computeCountdown(data.nextPrayerEpochMillis)
    } else {
        data.timeUntilNext
    }
    val rightLine = buildString {
        if (data.hijriDate.isNotEmpty()) append(data.hijriDate)
        if (data.nextPrayerName.isNotEmpty() && liveCountdown.isNotEmpty() && liveCountdown != "—") {
            if (isNotEmpty()) append(" · ")
            append("${data.nextPrayerName} in $liveCountdown")
        }
    }.ifEmpty { "—" }

    // Five (name, time, passed) cells in order; the next prayer is the first not-passed.
    val cells = listOf(
        Triple("Fajr", data.fajrTime, data.fajrPassed),
        Triple("Dhuhr", data.dhuhrTime, data.dhuhrPassed),
        Triple("Asr", data.asrTime, data.asrPassed),
        Triple("Maghrib", data.maghribTime, data.maghribPassed),
        Triple("Isha", data.ishaTime, data.ishaPassed),
    )
    val nextIndex = cells.indexOfFirst { !it.third }

    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 12.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.locationName.ifEmpty { "Location" },
                    style = TextStyle(color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = rightLine,
                    style = TextStyle(color = textSecondary, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                cells.forEachIndexed { index, (name, time, passed) ->
                    val state = when {
                        passed -> PrayerCellState.PAST
                        index == nextIndex -> PrayerCellState.NEXT
                        else -> PrayerCellState.UPCOMING
                    }
                    PrayerPill(
                        name = name,
                        time = time.ifEmpty { "—" },
                        state = state,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        primaryColor = primaryColor,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerPill(
    name: String,
    time: String,
    state: PrayerCellState,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    val onPrimary = ColorProvider(R.color.widget_on_primary)
    val nameColor = if (state == PrayerCellState.NEXT) onPrimary else textSecondary
    val timeColor = when (state) {
        PrayerCellState.PAST -> textSecondary
        PrayerCellState.NEXT -> onPrimary
        PrayerCellState.UPCOMING -> textColor
    }
    val inner = GlanceModifier.let {
        if (state == PrayerCellState.NEXT) {
            it.background(primaryColor).cornerRadius(12.dp).padding(vertical = 6.dp, horizontal = 4.dp)
        } else it
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(modifier = inner.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                style = TextStyle(color = nameColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(
                text = time,
                style = TextStyle(color = timeColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
    }
}

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PrayerTimesWorker.enqueuePeriodicWork(context, force = true)
        WidgetUpdateScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        PrayerTimesWorker.cancel(context)
        WidgetUpdateScheduler.cancel(context)
    }
}
