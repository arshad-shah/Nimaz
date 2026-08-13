package com.arshadshah.nimaz.presentation.components.molecules.calendar

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NavArrowDirection
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazLegendItem
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDot
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazNavArrowButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

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
 * @param headerAlignment Horizontal placement of the title block in the navigation
 *   header. Defaults to [CalendarHeaderAlignment.START] (the original layout).
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
    selectionStyle: SelectionStyle = SelectionStyle.BACKGROUND,
    headerAlignment: CalendarHeaderAlignment = CalendarHeaderAlignment.START
) {
    val today = remember { LocalDate.now() }
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier) {
        // Navigation header
        if (showNavigation) {
            CalendarNavigationHeader(
                title = headerTitle ?: displayedMonth.formatDefault(),
                subtitle = headerSubtitle,
                alignment = headerAlignment,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Calendar grid card. The card surface is the page-level container;
        // the inner grid swaps with a horizontal slide when displayedMonth
        // changes so consumers get a "real" month transition without any
        // public API change.
        NimazCard(
            tone = NimazTone.NEUTRAL,
            style = NimazCardStyle.ELEVATED,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
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
                            NimazLegendItem(
                                color = item.color,
                                label = item.label,
                                style = item.indicatorStyle,
                            )
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

// --- Internal composables ---

@Composable
private fun CalendarNavigationHeader(
    title: String,
    subtitle: (@Composable () -> Unit)?,
    alignment: CalendarHeaderAlignment,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (alignment) {
            CalendarHeaderAlignment.START -> {
                // Title left, both arrows pushed to the right (original layout).
                HeaderTitleBlock(
                    title = title,
                    subtitle = subtitle,
                    horizontalAlignment = Alignment.Start,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                NavButton(
                    NavArrowDirection.PREVIOUS,
                    R.string.cd_previous_month,
                    onPrevious
                )
                NavButton(NavArrowDirection.NEXT, R.string.cd_next_month, onNext)
            }

            CalendarHeaderAlignment.CENTER -> {
                // Arrows flank a centered title.
                NavButton(
                    NavArrowDirection.PREVIOUS,
                    R.string.cd_previous_month,
                    onPrevious
                )
                HeaderTitleBlock(
                    title = title,
                    subtitle = subtitle,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                NavButton(NavArrowDirection.NEXT, R.string.cd_next_month, onNext)
            }

            CalendarHeaderAlignment.END -> {
                // Both arrows left, title right-aligned.
                NavButton(
                    NavArrowDirection.PREVIOUS,
                    R.string.cd_previous_month,
                    onPrevious
                )
                NavButton(NavArrowDirection.NEXT, R.string.cd_next_month, onNext)
                HeaderTitleBlock(
                    title = title,
                    subtitle = subtitle,
                    horizontalAlignment = Alignment.End,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeaderTitleBlock(
    title: String,
    subtitle: (@Composable () -> Unit)?,
    horizontalAlignment: Alignment.Horizontal,
    textAlign: TextAlign,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = textAlign
        )
        subtitle?.invoke()
    }
}

/**
 * Month-navigation arrow. Delegates to the shared [NimazNavArrowButton] so the
 * calendar header reads the same as every other prev/next control (issue #227).
 */
@Composable
private fun NavButton(
    direction: NavArrowDirection,
    contentDescriptionRes: Int,
    onClick: () -> Unit
) {
    NimazNavArrowButton(
        direction = direction,
        onClick = onClick,
        contentDescription = stringResource(contentDescriptionRes),
        size = 44.dp
    )
}

@Composable
private fun WeekdayHeaderRow(modifier: Modifier = Modifier) {
    // The header strip is a *nested* surface — it sits inside the calendar card,
    // so it separates with an outline + a quiet MUTED fill rather than a
    // hand-rolled tint. Without it the labels float on the card surface and read
    // as just "more cells with no number."
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.OUTLINED,
        tone = NimazTone.MUTED,
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            WEEKDAY_LABELS.forEachIndexed { index, day ->
                // Friday gets the primary tint + Bold weight as a small but
                // intentional nod to Jumu'ah — the most significant day of the
                // week in Islamic practice.
                val isFriday = index == FRIDAY_INDEX
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
    // A Hijri month start emphasizes the primary number (accent + bold) when the
    // Hijri date is the centered one — unless an explicit override is supplied or
    // the cell is a primary-filled selection (where onPrimary already contrasts).
    val emphasizedPrimaryColor =
        if (dayState.emphasizePrimary && !isSelectedBackgroundFill) scheme.primary else null
    val textColor = dayState.textColor ?: emphasizedPrimaryColor ?: defaultTextColor
    val fontWeight = dayState.fontWeight ?: when {
        dayState.emphasizePrimary -> FontWeight.Bold
        isSelectedBackgroundFill -> FontWeight.Bold
        isToday -> FontWeight.SemiBold
        else -> FontWeight.Normal
    }

    // Accessibility: "Monday, 5 January 2026, selected" rather than "5".
    val locale = LocalLocale.current.platformLocale
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
    val a11yLabel = if (isToday) {
        stringResource(
            R.string.calendar_a11y_day_today_format,
            dayName, date.dayOfMonth, monthName, date.year
        )
    } else {
        stringResource(
            R.string.calendar_a11y_day_format,
            dayName, date.dayOfMonth, monthName, date.year
        )
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
            text = dayState.primaryLabel ?: date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = textColor,
            fontSize = 13.sp
        )

        // Dual-date overlay — the "other" calendar's day, tucked into the top-end
        // corner in a quiet, muted tone so it never competes with the centered
        // number. On a Hijri month start it flips to the primary accent + bold,
        // which is the layout-stable replacement for the old stripe/pill marker.
        dayState.secondaryLabel?.let { secondary ->
            val secondaryColor = when {
                isSelectedBackgroundFill -> scheme.onPrimary.copy(alpha = 0.9f)
                dayState.emphasizeSecondary -> scheme.primary
                else -> scheme.onSurfaceVariant.copy(alpha = 0.8f)
            }
            Text(
                text = secondary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 4.dp),
                color = secondaryColor,
                fontWeight = if (dayState.emphasizeSecondary) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 9.sp,
                maxLines = 1,
                softWrap = false
            )
        }

        // Indicator dot — picks a contrasting tone on selected cells so the
        // dot stays visible against primary fill. When a fill bar is also present,
        // BOTTOM_CENTER lifts clear of it (see the fill bar block below) so the two
        // render as distinct marks instead of overlapping; TOP_END never shares the
        // bar's band, so it is unaffected either way.
        dayState.indicatorColor?.let { color ->
            val resolvedDot = if (isSelectedBackgroundFill) {
                // On a primary-filled selected cell, white-ish dots read better
                // than the caller's raw color which may sit too close in tone.
                scheme.onPrimary
            } else color
            when (dayState.indicatorPosition) {
                IndicatorPosition.BOTTOM_CENTER -> NimazStatusDot(
                    color = resolvedDot,
                    style = dayState.indicatorStyle,
                    size = NimazStatusDotSize.SMALL,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Bar (when present) occupies the 3dp-6dp band; 8dp clears it
                        // with a 2dp gap. Bar-less cells keep the original 4dp.
                        .padding(bottom = if (dayState.indicatorBar != null) 8.dp else 4.dp)
                )

                IndicatorPosition.TOP_END -> NimazStatusDot(
                    color = resolvedDot,
                    style = dayState.indicatorStyle,
                    size = NimazStatusDotSize.MEDIUM,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                )
            }
        }

        // Fill bar — how much of the day was completed. Pinned to the very bottom of the
        // cell so a BOTTOM_CENTER dot (raised above it, see the dot block above) and the
        // bar read as two distinct marks with clear space between them, rather than
        // overlapping.
        dayState.indicatorBar?.let { rawFraction ->
            val fraction = if (rawFraction.isNaN()) 0f else rawFraction.coerceIn(0f, 1f)
            val barColor = when {
                isSelectedBackgroundFill -> scheme.onPrimary
                else -> dayState.indicatorBarColor ?: scheme.primary
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .width(18.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(barColor)
                )
            }
        }
    }
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
    val eidColor = NimazColors.Gold500
    val holyColor = NimazColors.Success
    val fastColor = NimazColors.Purple

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
                    // Jan 20, 2026 is 1 Sha'ban 1447 — demo the dual-date
                    // month-start marker (emphasized Hijri day in the corner).
                    val isMonthStart = date == LocalDate.of(2026, 1, 20)
                    CalendarDayState(
                        indicatorColor = eventDays[date],
                        secondaryLabel = if (isMonthStart) "1" else null,
                        emphasizeSecondary = isMonthStart
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
                                day in missedDays -> NimazPalette.Red500
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
                    CalendarLegendItem(NimazPalette.Red500, "Missed"),
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

@Preview(showBackground = true, name = "NimazCalendar - prayer fill bars")
@Composable
private fun NimazCalendarFillBarPreview() {
    val month = YearMonth.of(2026, 8)
    NimazTheme {
        NimazCalendar(
            displayedMonth = month,
            selectedDate = month.atDay(13),
            onDateSelected = {},
            onPreviousMonth = {},
            onNextMonth = {},
            selectionStyle = SelectionStyle.BORDER,
            dayStateProvider = { date ->
                // A repeating 5,4,0,5,3,5,2 pattern so every bar length is visible at once.
                val prayed = listOf(5, 4, 0, 5, 3, 5, 2)[date.dayOfMonth % 7]
                val barColor = if (prayed == 5) {
                    NimazColors.StatusColors.Prayed
                } else {
                    NimazColors.StatusColors.Partial
                }
                when {
                    // Days 1-13: bar only, sweeping through every fraction the pattern
                    // produces.
                    date.dayOfMonth <= 13 -> CalendarDayState(
                        indicatorBar = prayed / 5f,
                        indicatorBarColor = barColor
                    )
                    // Days 14-27: bar AND dot together — the combined case that must
                    // read as two distinct marks with clear space between them, not a
                    // smudge.
                    date.dayOfMonth <= 27 -> CalendarDayState(
                        indicatorBar = prayed / 5f,
                        indicatorBarColor = barColor,
                        indicatorColor = if (prayed == 0) {
                            NimazColors.StatusColors.Missed
                        } else {
                            NimazColors.StatusColors.Prayed
                        }
                    )
                    // The rest of the month: no indicators at all, the untouched default.
                    else -> CalendarDayState()
                }
            }
        )
    }
}
