package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonthYear by remember(currentMonth) {
        derivedStateOf {
            currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }
    }

    val hijriInfo by remember(currentMonth) {
        derivedStateOf {
            getHijriMonthInfo(currentMonth)
        }
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationButton(
                        onClick = { onMonthChange(currentMonth.minusMonths(1)) },
                        icon = R.drawable.angle_left_icon,
                        contentDescription = "Previous Month"
                    )

                    MonthYearDisplay(
                        currentMonthYear = currentMonthYear,
                        hijriInfo = hijriInfo,
                        isCurrentMonth = currentMonth == YearMonth.now(),
                        showPastIcon = currentMonth.isBefore(YearMonth.now()),
                        showFutureIcon = currentMonth.isAfter(YearMonth.now()),
                        onTodayClick = onTodayClick
                    )

                    NavigationButton(
                        onClick = { onMonthChange(currentMonth.plusMonths(1)) },
                        icon = R.drawable.angle_right_icon,
                        contentDescription = "Next Month"
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationButton(
    onClick: () -> Unit,
    icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(id = icon),
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun MonthYearDisplay(
    currentMonthYear: String,
    hijriInfo: String,
    isCurrentMonth: Boolean,
    showPastIcon: Boolean,
    showFutureIcon: Boolean,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onTodayClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        MonthIndicator(
            isCurrentMonth = isCurrentMonth,
            showPastIcon = showPastIcon,
            showFutureIcon = showFutureIcon
        )

        Text(
            text = currentMonthYear,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = hijriInfo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MonthIndicator(
    isCurrentMonth: Boolean,
    showPastIcon: Boolean,
    showFutureIcon: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isCurrentMonth)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showPastIcon) {
                NavigationIcon(
                    icon = R.drawable.angle_small_left_icon,
                    isCurrentMonth = isCurrentMonth
                )
            }

            Text(
                text = if (isCurrentMonth) "Current Month" else "Return to Current Month",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrentMonth)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (showFutureIcon) {
                NavigationIcon(
                    icon = R.drawable.angle_small_right_icon,
                    isCurrentMonth = isCurrentMonth
                )
            }
        }
    }
}

@Composable
private fun NavigationIcon(
    icon: Int,
    isCurrentMonth: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        modifier = modifier.size(12.dp),
        painter = painterResource(id = icon),
        contentDescription = null,
        tint = if (isCurrentMonth)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.primary
    )
}

private fun getHijriMonthInfo(yearMonth: YearMonth): String {
    val formatterHijriMonth = DateTimeFormatter.ofPattern("MMMM")
    val uniqueHijriMonths = linkedSetOf<String>()

    // Check start, middle, and end of month for Hijri months
    val daysToCheck = listOf(1, yearMonth.lengthOfMonth() / 2, yearMonth.lengthOfMonth())

    for (day in daysToCheck) {
        val date = yearMonth.atDay(day)
        val hijriDate = HijrahDate.from(date)
        uniqueHijriMonths.add(hijriDate.format(formatterHijriMonth))
    }

    val hijriYear = HijrahDate.from(yearMonth.atDay(1))
        .format(DateTimeFormatter.ofPattern("yyyy"))

    return uniqueHijriMonths.joinToString(" / ") + " $hijriYear"
}