@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.widget.hijricalendar

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.layout.ColumnScope
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

    companion object {
        /**
         * Intent action used when the widget is tapped. [MainActivity] reads
         * this in its [MainActivity.handleIntent] and surfaces a pending
         * route to the NavGraph so the user lands on the Islamic Calendar
         * screen. System Back returns them to Home (start destination).
         */
        const val ACTION_OPEN_ISLAMIC_CALENDAR =
            "com.arshadshah.nimaz.ACTION_OPEN_ISLAMIC_CALENDAR"
    }

    override val stateDefinition: GlanceStateDefinition<HijriCalendarWidgetState> =
        HijriCalendarStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Build the deep-link intent once so every tappable region shares it.
        val openCalendar = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_ISLAMIC_CALENDAR
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        provideContent {
            GlanceTheme {
                val state = currentState<HijriCalendarWidgetState>()
                HijriCalendarContent(state, openCalendar)
            }
        }
    }
}

@Composable
private fun HijriCalendarContent(
    state: HijriCalendarWidgetState,
    openCalendarIntent: Intent,
) {
    val context = LocalContext.current
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
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity(openCalendarIntent)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = context.getString(R.string.widget_loading),
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
                primaryColor = primaryColor,
                openCalendarIntent = openCalendarIntent,
            )
        }

        is HijriCalendarWidgetState.Error -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity(openCalendarIntent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = context.getString(R.string.widget_tap_to_refresh),
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
    primaryColor: ColorProvider,
    openCalendarIntent: Intent,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(openCalendarIntent))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Left: header + weekday strip + month grid.
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(end = 10.dp)
        ) {
            CalendarHeader(
                hijriMonthName = data.hijriMonthName,
                hijriYear = data.hijriYear,
                gregorianDate = data.gregorianDate,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor,
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            WeekdayStrip(
                primaryColor = primaryColor,
                textSecondary = textSecondary,
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            MonthGrid(
                data = data,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor,
                backgroundColor = backgroundColor,
            )
        }

        // Right: today highlight + events.
        EventsPanel(
            data = data,
            textColor = textColor,
            textSecondary = textSecondary,
            primaryColor = primaryColor,
            backgroundColor = backgroundColor,
        )
    }
}

/**
 * Header strip: bold Hijri month + year on the left, soft gregorian date
 * on the right. Uses an accent dot before the month name to echo the
 * "this app uses small colored dots" vocabulary from the rest of the design
 * system (prayer card dots, today carousel indicators).
 */
@Composable
private fun CalendarHeader(
    hijriMonthName: String,
    hijriYear: Int,
    gregorianDate: String,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(6.dp)
                .cornerRadius(3.dp)
                .background(primaryColor)
        ) {}
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "$hijriMonthName $hijriYear",
            style = TextStyle(
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Box(modifier = GlanceModifier.defaultWeight()) {}
        Text(
            text = gregorianDate,
            style = TextStyle(color = textSecondary, fontSize = 11.sp)
        )
    }
}

/**
 * Two-letter weekday labels with Friday tinted in primary as a Jumu'ah
 * accent — matches the [NimazCalendar] in-app molecule so users see one
 * design language across the widget and the screen they land on.
 */
@Composable
private fun WeekdayStrip(
    primaryColor: ColorProvider,
    textSecondary: ColorProvider,
) {
    val labels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val isFriday = index == 5
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = if (isFriday) primaryColor else textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

/**
 * The numeric grid. Today gets a primary-filled circle with onPrimary text;
 * every other day is plain text. Empty leading cells render as transparent
 * placeholders so the grid stays rectangular.
 *
 * Declared as a [ColumnScope] extension so the per-row `defaultWeight()`
 * calls inside resolve — otherwise the function loses the enclosing scope.
 */
@Composable
private fun ColumnScope.MonthGrid(
    data: HijriCalendarData,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
    backgroundColor: ColorProvider,
) {
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
                                    .size(24.dp)
                                    .cornerRadius(12.dp)
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = TextStyle(
                                        color = backgroundColor,
                                        fontSize = 11.sp,
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
                                    fontSize = 11.sp,
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

/**
 * Right rail: big bold today number under a small "TODAY" eyebrow label,
 * followed by a thin divider and the events list. Events get a small
 * leading dot in the primary color so they read as a typed list rather
 * than free-floating text.
 */
@Composable
private fun EventsPanel(
    data: HijriCalendarData,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
    backgroundColor: ColorProvider,
) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxHeight()
            .width(86.dp)
            .padding(start = 10.dp)
    ) {
        Text(
            text = context.getString(R.string.widget_today),
            style = TextStyle(
                color = primaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = data.todayHijriDay.toString(),
            style = TextStyle(
                color = textColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        // Thin separator so the events list reads as its own zone rather
        // than running into the big today number above it.
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(textSecondary)
        ) {}
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (data.events.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_no_events),
                style = TextStyle(color = textSecondary, fontSize = 10.sp)
            )
        } else {
            data.events.forEach { event ->
                EventRow(
                    event = event,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    primaryColor = primaryColor,
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun EventRow(
    event: HijriCalendarEventData,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
) {
    Row(verticalAlignment = Alignment.Top) {
        // Tiny accent dot — visual marker that this is a list item, not a
        // paragraph. 4dp circle aligned with the text baseline-ish via a
        // small top padding.
        Box(
            modifier = GlanceModifier
                .padding(top = 5.dp)
                .size(4.dp)
                .cornerRadius(2.dp)
                .background(primaryColor)
        ) {}
        Spacer(modifier = GlanceModifier.width(6.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
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
                style = TextStyle(color = textSecondary, fontSize = 9.sp),
                maxLines = 1
            )
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
