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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Single day cell for calendar displays.
 */
@Composable
fun CalendarDay(
    dayNumber: Int,
    modifier: Modifier = Modifier,
    hijriDay: Int? = null,
    isSelected: Boolean = false,
    isToday: Boolean = false,
    isCurrentMonth: Boolean = true,
    hasEvent: Boolean = false,
    isFastingDay: Boolean = false,
    fastingStatus: FastingDayStatus? = null,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        isToday && !isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gregorian day
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )

            // Hijri day (if provided)
            if (hijriDay != null) {
                Text(
                    text = hijriDay.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (isCurrentMonth) 0.7f else 0.4f
                        )
                    }
                )
            }

            // Event/fasting indicator
            if (hasEvent || fastingStatus != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (hasEvent) {
                        DayIndicatorDot(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                    if (fastingStatus != null) {
                        DayIndicatorDot(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                fastingStatus.color
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Small indicator dot for events/fasting.
 */
@Composable
private fun DayIndicatorDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Fasting day status.
 */
enum class FastingDayStatus(val color: Color) {
    FASTED(NimazColors.FastingColors.Fasted),
    NOT_FASTED(NimazColors.FastingColors.NotFasted),
    MAKEUP(NimazColors.FastingColors.Makeup),
    EXEMPTED(NimazColors.FastingColors.Exempted)
}

/**
 * Week header row.
 */
@Composable
fun CalendarWeekHeader(
    modifier: Modifier = Modifier,
    weekDays: List<String> = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    highlightFriday: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDays.forEach { day ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (highlightFriday && day == "Fri") FontWeight.Bold else FontWeight.Normal,
                    color = if (highlightFriday && day == "Fri") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Month header with navigation.
 */
@Composable
fun CalendarMonthHeader(
    monthName: String,
    year: Int,
    modifier: Modifier = Modifier,
    hijriMonth: String? = null,
    hijriYear: Int? = null,
    onPreviousMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$monthName $year",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (hijriMonth != null && hijriYear != null) {
            Text(
                text = "$hijriMonth $hijriYear AH",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Calendar day cell for use in calendar grids.
 * Displays both Gregorian and Hijri day numbers with event indicators.
 */
@Composable
fun CalendarDayCell(
    gregorianDay: Int,
    hijriDay: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    hasEvent: Boolean,
    isHoliday: Boolean,
    isFastingDay: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        isHoliday && isCurrentMonth -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        isHoliday -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        isToday && !isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gregorian day
            Text(
                text = gregorianDay.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )

            // Hijri day
            Text(
                text = hijriDay.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = if (isCurrentMonth) 0.7f else 0.4f
                    )
                }
            )

            // Event/fasting indicators
            if (hasEvent || isFastingDay) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (hasEvent) {
                        DayIndicatorDot(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else if (isHoliday) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                    if (isFastingDay) {
                        DayIndicatorDot(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                NimazColors.FastingColors.Fasted
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
private fun CalendarDayDefaultPreview() {
    NimazTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CalendarDay(
                dayNumber = 15,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDaySelectedPreview() {
    NimazTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CalendarDay(
                dayNumber = 10,
                hijriDay = 17,
                isSelected = true,
                modifier = Modifier.size(48.dp),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayTodayPreview() {
    NimazTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CalendarDay(
                dayNumber = 22,
                hijriDay = 1,
                isToday = true,
                hasEvent = true,
                modifier = Modifier.size(48.dp),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayFastingPreview() {
    NimazTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CalendarDay(
                dayNumber = 5,
                hijriDay = 12,
                hasEvent = true,
                fastingStatus = FastingDayStatus.FASTED,
                modifier = Modifier.size(48.dp),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayNotCurrentMonthPreview() {
    NimazTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CalendarDay(
                dayNumber = 29,
                isCurrentMonth = false,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarWeekHeaderPreview() {
    NimazTheme {
        CalendarWeekHeader(
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarMonthHeaderPreview() {
    NimazTheme {
        CalendarMonthHeader(
            monthName = "January",
            year = 2026,
            hijriMonth = "Rajab",
            hijriYear = 1447,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayCellPreview() {
    NimazTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CalendarDayCell(
                gregorianDay = 15,
                hijriDay = 22,
                isToday = true,
                isSelected = false,
                isCurrentMonth = true,
                hasEvent = true,
                isHoliday = false,
                isFastingDay = true,
                onClick = {},
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayCellSelectedPreview() {
    NimazTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CalendarDayCell(
                gregorianDay = 25,
                hijriDay = 3,
                isToday = false,
                isSelected = true,
                isCurrentMonth = true,
                hasEvent = false,
                isHoliday = true,
                isFastingDay = false,
                onClick = {},
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
