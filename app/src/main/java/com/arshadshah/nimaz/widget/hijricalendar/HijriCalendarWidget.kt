package com.arshadshah.nimaz.widget.hijricalendar

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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R

class HijriCalendarWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<HijriCalendarWidgetState> =
        HijriCalendarStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState<HijriCalendarWidgetState>()
                HijriCalendarContent(state)
            }
        }
    }
}

@Composable
private fun HijriCalendarContent(state: HijriCalendarWidgetState) {
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is HijriCalendarWidgetState.Loading -> {
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

        is HijriCalendarWidgetState.Success -> {
            HijriCalendarSuccessContent(
                data = state.data,
                backgroundColor = backgroundColor,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )
        }

        is HijriCalendarWidgetState.Error -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap to refresh",
                    style = TextStyle(color = textSecondary, fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
private fun HijriCalendarSuccessContent(
    data: HijriCalendarData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp)
    ) {
        // Left side: Calendar grid
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(end = 8.dp)
        ) {
            // Header row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${data.hijriMonthName} ${data.hijriYear}",
                    style = TextStyle(
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Box(modifier = GlanceModifier.defaultWeight()) {}
                Text(
                    text = data.gregorianDate,
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Day-of-week labels
            val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = GlanceModifier.defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Calendar grid — each row gets equal weight to fill the space
            val totalCells = data.firstDayOfWeekOffset + data.daysInMonth
            val totalRows = (totalCells + 6) / 7
            for (row in 0 until totalRows) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - data.firstDayOfWeekOffset + 1

                        Box(
                            modifier = GlanceModifier.defaultWeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..data.daysInMonth) {
                                val isToday = dayNumber == data.todayHijriDay
                                if (isToday) {
                                    Box(
                                        modifier = GlanceModifier
                                            .size(22.dp)
                                            .cornerRadius(11.dp)
                                            .background(primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = TextStyle(
                                                color = ColorProvider(R.color.widget_background),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = TextStyle(
                                            color = textColor,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Divider
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(textSecondary)
        ) {}

        // Right side: Events panel
        Column(
            modifier = GlanceModifier
                .fillMaxHeight()
                .width(80.dp)
                .padding(start = 8.dp)
        ) {
            Text(
                text = "Today",
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = data.todayHijriDay.toString(),
                style = TextStyle(
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (data.events.isEmpty()) {
                Text(
                    text = "No events",
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 10.sp
                    )
                )
            } else {
                data.events.forEach { event ->
                    Column(modifier = GlanceModifier.padding(bottom = 4.dp)) {
                        Text(
                            text = event.name,
                            style = TextStyle(
                                color = textColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2
                        )
                        Text(
                            text = event.type.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = TextStyle(
                                color = primaryColor,
                                fontSize = 9.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

class HijriCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HijriCalendarWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        HijriCalendarWorker.enqueuePeriodicWork(context, force = true)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        HijriCalendarWorker.cancel(context)
    }
}
