package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.components.atoms.NimazLegendItem
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Position of the status indicator dot within a day cell.
 */
enum class IndicatorPosition {
    BOTTOM_CENTER,
    TOP_END
}

/**
 * Visual state for a single calendar day cell.
 *
 * @param indicatorColor Color of the status dot. Null means no dot.
 * @param indicatorPosition Where to place the status dot.
 * @param backgroundColor Custom background color for the cell (e.g., Ramadan highlighting).
 *   Null uses the default (today/selected/transparent).
 * @param textColor Custom text color override. Null uses the default.
 * @param fontWeight Custom font weight override. Null uses the default.
 */
data class CalendarDayState(
    val indicatorColor: Color? = null,
    val indicatorPosition: IndicatorPosition = IndicatorPosition.BOTTOM_CENTER,
    val backgroundColor: Color? = null,
    val textColor: Color? = null,
    val fontWeight: FontWeight? = null
)

/**
 * A legend entry displayed below the calendar grid.
 */
data class CalendarLegendItem(
    val color: Color,
    val label: String
)

/**
 * A reusable month calendar grid with navigation, day selection, status indicators, and legend.
 *
 * Supports three usage patterns:
 * - **Islamic Calendar**: Event-type colored dots at bottom-center, with legend.
 * - **Prayer Tracker**: Completion status dots at top-end, with selected border.
 * - **Fasting Tracker**: Custom day backgrounds (Ramadan) and status dots.
 *
 * @param displayedMonth The month and year to display.
 * @param selectedDate The currently selected date. Null means no selection.
 * @param onDateSelected Called when a day cell is tapped.
 * @param onPreviousMonth Called when the previous-month button is tapped.
 * @param onNextMonth Called when the next-month button is tapped.
 * @param modifier Modifier for the root layout.
 * @param dayStateProvider Returns [CalendarDayState] for each date, controlling indicators and styling.
 * @param legendItems Legend entries shown below the grid. Empty list hides the legend.
 * @param showNavigation Whether to show the month navigation header.
 * @param headerTitle Custom title text. Defaults to "Month Year" format.
 * @param headerSubtitle Optional subtitle composable below the title (e.g., Arabic month name).
 * @param selectionStyle How the selected date is visually indicated.
 */
@Composable
fun NimazCalendar(
    displayedMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    dayStateProvider: (LocalDate) -> CalendarDayState = { CalendarDayState() },
    legendItems: List<CalendarLegendItem> = emptyList(),
    showNavigation: Boolean = true,
    headerTitle: String? = null,
    headerSubtitle: (@Composable () -> Unit)? = null,
    selectionStyle: SelectionStyle = SelectionStyle.BACKGROUND
) {
    val today = remember { LocalDate.now() }
    val calendarDays = remember(displayedMonth) { buildCalendarDays(displayedMonth) }

    Column(modifier = modifier) {
        // Navigation header
        if (showNavigation) {
            CalendarNavigationHeader(
                title = headerTitle ?: displayedMonth.formatDefault(),
                subtitle = headerSubtitle,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Calendar grid card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                // Weekday headers
                WeekdayHeaderRow()

                Spacer(modifier = Modifier.height(10.dp))

                // Day cells grid
                calendarDays.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        week.forEach { date ->
                            val isCurrentMonth = date.month == displayedMonth.month
                            val isToday = date == today
                            val isSelected = date == selectedDate
                            val dayState = dayStateProvider(date)

                            CalendarDayCell(
                                date = date,
                                isCurrentMonth = isCurrentMonth,
                                isToday = isToday,
                                isSelected = isSelected,
                                dayState = dayState,
                                selectionStyle = selectionStyle,
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Legend
                if (legendItems.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 15.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        legendItems.forEachIndexed { index, item ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            NimazLegendItem(color = item.color, label = item.label)
                        }
                    }
                }
            }
        }
    }
}

/**
 * How the selected date is visually indicated.
 */
enum class SelectionStyle {
    /** Fills the cell background (used by Islamic calendar, fasting tracker). */
    BACKGROUND,
    /** Draws a border around the cell (used by prayer tracker). */
    BORDER
}

// --- Internal composables ---

@Composable
private fun CalendarNavigationHeader(
    title: String,
    subtitle: (@Composable () -> Unit)?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            subtitle?.invoke()
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onPrevious,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onNext,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeaderRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    dayState: CalendarDayState,
    selectionStyle: SelectionStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultBackgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isSelected && selectionStyle == SelectionStyle.BACKGROUND ->
            MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color.Transparent
    }

    val backgroundColor = dayState.backgroundColor ?: defaultBackgroundColor

    val defaultTextColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val textColor = dayState.textColor ?: defaultTextColor
    val fontWeight = dayState.fontWeight
        ?: if (isToday) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (isSelected && selectionStyle == SelectionStyle.BORDER) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(10.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = textColor,
            fontSize = 13.sp
        )

        // Indicator dot
        dayState.indicatorColor?.let { color ->
            when (dayState.indicatorPosition) {
                IndicatorPosition.BOTTOM_CENTER -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
                IndicatorPosition.TOP_END -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

// --- Helpers ---

private fun buildCalendarDays(yearMonth: YearMonth): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    val offset = if (firstOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0
    else firstOfMonth.dayOfWeek.value
    val startDate = firstOfMonth.minusDays(offset.toLong())
    return List(35) { startDate.plusDays(it.toLong()) }
}

private fun YearMonth.formatDefault(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$monthName $year"
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "NimazCalendar - Default")
@Composable
private fun NimazCalendarDefaultPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = LocalDate.of(2026, 1, 15),
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "NimazCalendar - Islamic Events")
@Composable
private fun NimazCalendarIslamicPreview() {
    val eidColor = Color(0xFFEAB308)
    val holyColor = Color(0xFF22C55E)
    val fastColor = Color(0xFFA855F7)

    // Simulate some event days
    val eventDays = mapOf(
        LocalDate.of(2026, 1, 5) to eidColor,
        LocalDate.of(2026, 1, 12) to holyColor,
        LocalDate.of(2026, 1, 20) to fastColor,
        LocalDate.of(2026, 1, 27) to holyColor
    )

    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = LocalDate.of(2026, 1, 12),
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                headerTitle = "Rajab 1447",
                dayStateProvider = { date ->
                    CalendarDayState(
                        indicatorColor = eventDays[date]
                    )
                },
                legendItems = listOf(
                    CalendarLegendItem(eidColor, "Eid"),
                    CalendarLegendItem(holyColor, "Holy Night"),
                    CalendarLegendItem(fastColor, "Fasting")
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "NimazCalendar - Prayer Tracker")
@Composable
private fun NimazCalendarPrayerTrackerPreview() {
    val today = LocalDate.of(2026, 1, 31)
    val completedDays = setOf(25, 26, 27, 28, 29)
    val partialDays = setOf(22, 23, 24)
    val missedDays = setOf(20, 21)

    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = LocalDate.of(2026, 1, 29),
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                selectionStyle = SelectionStyle.BORDER,
                dayStateProvider = { date ->
                    if (date.month.value == 1 && date.isBefore(today)) {
                        val day = date.dayOfMonth
                        CalendarDayState(
                            indicatorColor = when {
                                day in completedDays -> NimazColors.StatusColors.Prayed
                                day in partialDays -> NimazColors.StatusColors.Partial
                                day in missedDays -> NimazColors.StatusColors.Missed
                                else -> null
                            },
                            indicatorPosition = IndicatorPosition.TOP_END
                        )
                    } else {
                        CalendarDayState()
                    }
                },
                legendItems = listOf(
                    CalendarLegendItem(NimazColors.StatusColors.Prayed, "Complete"),
                    CalendarLegendItem(NimazColors.StatusColors.Partial, "Partial"),
                    CalendarLegendItem(NimazColors.StatusColors.Missed, "Missed")
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "NimazCalendar - Fasting Tracker")
@Composable
private fun NimazCalendarFastingTrackerPreview() {
    val ramadanDays = (1..28).toSet()
    val fastedDays = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val missedDays = setOf(11)

    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 3),
                selectedDate = LocalDate.of(2026, 3, 10),
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                headerTitle = "March 2026",
                dayStateProvider = { date ->
                    if (date.month.value == 3) {
                        val day = date.dayOfMonth
                        val isRamadan = day in ramadanDays
                        CalendarDayState(
                            indicatorColor = when {
                                day in fastedDays -> NimazColors.FastingColors.Fasted
                                day in missedDays -> Color(0xFFEF4444)
                                else -> null
                            },
                            backgroundColor = if (isRamadan)
                                NimazColors.FastingColors.Ramadan.copy(alpha = 0.15f)
                            else null,
                            textColor = if (isRamadan)
                                NimazColors.FastingColors.Ramadan
                            else null,
                            fontWeight = if (isRamadan) FontWeight.SemiBold else null
                        )
                    } else {
                        CalendarDayState()
                    }
                },
                legendItems = listOf(
                    CalendarLegendItem(NimazColors.FastingColors.Fasted, "Fasted"),
                    CalendarLegendItem(Color(0xFFEF4444), "Missed"),
                    CalendarLegendItem(NimazColors.FastingColors.Ramadan, "Ramadan")
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "NimazCalendar - No Navigation")
@Composable
private fun NimazCalendarNoNavPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                showNavigation = false
            )
        }
    }
}
