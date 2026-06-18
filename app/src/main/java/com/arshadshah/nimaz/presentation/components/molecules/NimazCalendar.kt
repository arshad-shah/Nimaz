package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import java.time.format.TextStyle
import com.arshadshah.nimaz.R
import androidx.compose.ui.res.stringResource

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
 * @param isHijriMonthStart Marks this cell as the first day of a Hijri month.
 *   Renders a colored top stripe so the start (and, by the cell before it, the
 *   end) of each Islamic month is easy to spot when scanning the grid.
 * @param hijriMonthStartLabel Short Hijri month name shown on the start cell
 *   (e.g. "Rajab"). Only drawn when [isHijriMonthStart] is true.
 */
data class CalendarDayState(
    val indicatorColor: Color? = null,
    val indicatorPosition: IndicatorPosition = IndicatorPosition.BOTTOM_CENTER,
    val backgroundColor: Color? = null,
    val textColor: Color? = null,
    val fontWeight: FontWeight? = null,
    val isHijriMonthStart: Boolean = false,
    val hijriMonthStartLabel: String? = null
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
    val haptic = LocalHapticFeedback.current

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

        // Calendar grid card. The card surface is the page-level container;
        // the inner grid swaps with a horizontal slide when displayedMonth
        // changes so consumers get a "real" month transition without any
        // public API change.
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
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                WeekdayHeaderRow()

                Spacer(modifier = Modifier.height(8.dp))

                // Animate month swap. We detect direction by comparing the
                // incoming month against the previous frame's value — fwd
                // slides in from the right, back slides in from the left.
                AnimatedContent(
                    targetState = displayedMonth,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val width = 80
                        (slideInHorizontally(
                            animationSpec = tween(220)
                        ) { if (forward) it / 2 else -it / 2 } + fadeIn(tween(220)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(180)
                                ) { if (forward) -width else width } + fadeOut(tween(180))
                            )
                    },
                    label = "calendar_month_swap"
                ) { monthToRender ->
                    CalendarGrid(
                        displayedMonth = monthToRender,
                        today = today,
                        selectedDate = selectedDate,
                        dayStateProvider = dayStateProvider,
                        selectionStyle = selectionStyle,
                        onDateSelected = { date ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDateSelected(date)
                        }
                    )
                }

                // Legend
                if (legendItems.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        legendItems.forEach { item ->
                            NimazLegendItem(color = item.color, label = item.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    displayedMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    dayStateProvider: (LocalDate) -> CalendarDayState,
    selectionStyle: SelectionStyle,
    onDateSelected: (LocalDate) -> Unit,
) {
    val calendarDays = remember(displayedMonth) { buildCalendarDays(displayedMonth) }

    Column(modifier = Modifier.fillMaxWidth()) {
        calendarDays.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
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

        // Tonal icon buttons — stand out clearly against both the page
        // background and the calendar card without being heavy.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
                onClick = onPrevious,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_previous_month),
                    modifier = Modifier.size(20.dp)
                )
            }
            FilledTonalIconButton(
                onClick = onNext,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.cd_next_month),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeaderRow(modifier: Modifier = Modifier) {
    // Tinted strip behind the labels makes the header visually distinct from
    // the date grid below — without it, the labels float on the card surface
    // and read as just "more cells with no number."
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .padding(vertical = 8.dp)
    ) {
        WEEKDAY_LABELS.forEachIndexed { index, day ->
            // Friday gets the primary tint + Bold weight as a small but
            // intentional nod to Jumu'ah — the most significant day of the
            // week in Islamic practice.
            val isFriday = index == 5
            val labelColor = if (isFriday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val weight = if (isFriday) FontWeight.Bold else FontWeight.SemiBold

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = weight,
                    color = labelColor,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Weekday abbreviations, uppercase for header-style typography. Kept as a
 * file-level constant so the rendering composable stays declarative.
 */
private val WEEKDAY_LABELS = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

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
    val scheme = MaterialTheme.colorScheme

    // Background priority: explicit override > selection > today > none.
    // Selection now wins over today so tapping a date always reads back
    // visually — today becomes a softer secondary cue (primaryContainer).
    val isSelectedBackgroundFill = isSelected && selectionStyle == SelectionStyle.BACKGROUND
    val defaultBackgroundColor = when {
        isSelectedBackgroundFill -> scheme.primary
        isToday -> scheme.primaryContainer
        else -> Color.Transparent
    }
    val backgroundColor = dayState.backgroundColor ?: defaultBackgroundColor

    val defaultTextColor = when {
        isSelectedBackgroundFill -> scheme.onPrimary
        isToday -> scheme.onPrimaryContainer
        !isCurrentMonth -> scheme.onSurface.copy(alpha = 0.30f)
        else -> scheme.onSurface
    }
    val textColor = dayState.textColor ?: defaultTextColor
    val fontWeight = dayState.fontWeight ?: when {
        isSelectedBackgroundFill -> FontWeight.Bold
        isToday -> FontWeight.SemiBold
        else -> FontWeight.Normal
    }

    // Accessibility: "Monday, 5 January 2026, selected" rather than "5".
    val locale = LocalLocale.current.platformLocale
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
    val a11yLabel = buildString {
        append("$dayName, ${date.dayOfMonth} $monthName ${date.year}")
        if (isToday) append(", today")
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (isSelected && selectionStyle == SelectionStyle.BORDER) {
                    Modifier.border(2.dp, scheme.primary, RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = a11yLabel
                selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = textColor,
            fontSize = 13.sp
        )

        // Hijri month-start marker — a colored top stripe plus the short month
        // name. Because each Islamic month begins exactly one cell after the
        // previous one ends, marking the start also visually delimits the end
        // of the preceding month. On a primary-filled selected cell the accent
        // flips to onPrimary so it stays legible.
        if (dayState.isHijriMonthStart) {
            val accent = if (isSelectedBackgroundFill) scheme.onPrimary else scheme.primary
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accent)
            )
            dayState.hijriMonthStartLabel?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 5.dp),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Indicator dot — picks a contrasting tone on selected cells so the
        // dot stays visible against primary fill.
        dayState.indicatorColor?.let { color ->
            val resolvedDot = if (isSelectedBackgroundFill) {
                // On a primary-filled selected cell, white-ish dots read better
                // than the caller's raw color which may sit too close in tone.
                scheme.onPrimary
            } else color
            when (dayState.indicatorPosition) {
                IndicatorPosition.BOTTOM_CENTER -> Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(resolvedDot)
                )
                IndicatorPosition.TOP_END -> Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(resolvedDot)
                )
            }
        }
    }
}

// --- Helpers ---

/**
 * Builds the date grid for a month, including padding days from the previous
 * month to fill the first week. Returns 5 weeks (35 cells) for months that
 * fit, or 6 weeks (42 cells) for months that need the extra row — e.g. a
 * 31-day month starting on Friday/Saturday wraps over 6 weeks and the
 * previous hard-coded 35-cell list silently truncated the last days.
 */
private fun buildCalendarDays(yearMonth: YearMonth): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    val offset = if (firstOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0
        else firstOfMonth.dayOfWeek.value
    val startDate = firstOfMonth.minusDays(offset.toLong())
    val totalDays = offset + yearMonth.lengthOfMonth()
    val weeks = ((totalDays + 6) / 7).coerceIn(5, 6)
    return List(weeks * 7) { startDate.plusDays(it.toLong()) }
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
                    // Jan 20, 2026 is 1 Sha'ban 1447 — demo the month-start marker.
                    val isMonthStart = date == LocalDate.of(2026, 1, 20)
                    CalendarDayState(
                        indicatorColor = eventDays[date],
                        isHijriMonthStart = isMonthStart,
                        hijriMonthStartLabel = if (isMonthStart) "Shab" else null
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

@Preview(showBackground = true, name = "NimazCalendar - 6-week month")
@Composable
private fun NimazCalendarSixWeekPreview() {
    // October 2026: starts on a Thursday and has 31 days, so it wraps over
    // 6 weeks. With the old hard-coded 35-cell list, Oct 30 and Oct 31 were
    // dropped off the bottom. This preview verifies the dynamic week count.
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 10),
                selectedDate = LocalDate.of(2026, 10, 31),
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {}
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
