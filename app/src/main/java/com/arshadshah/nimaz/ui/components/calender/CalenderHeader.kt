package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import io.github.boguszpawlowski.composecalendar.header.MonthState
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KFunction1

@Composable
fun CalenderHeader(
    monthState: MonthState,
    handleEvents: KFunction1<LocalDate, Unit>
) {
    val currentMonthYear = monthState.currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val hijriDate = HijrahDate.from(monthState.currentMonth.atDay(1))
    val hijriFormatted = getFormattedHijriDate(YearMonth.from(hijriDate))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationIconButton(
                iconId = R.drawable.angle_left_icon,
                description = "Previous Month",
                onClick = { changeMonth(monthState, -1, handleEvents) }
            )

            MonthYearDisplay(
                currentMonthYear = currentMonthYear,
                hijriFormatted = hijriFormatted,
                monthState = monthState,
                handleEvents = handleEvents
            )

            NavigationIconButton(
                iconId = R.drawable.angle_right_icon,
                description = "Next Month",
                onClick = { changeMonth(monthState, 1, handleEvents) }
            )
        }
    }
}

@Composable
private fun MonthYearDisplay(
    currentMonthYear: String,
    hijriFormatted: String,
    monthState: MonthState,
    handleEvents: KFunction1<LocalDate, Unit>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clip(MaterialTheme.shapes.medium)
            .clickable { navigateToCurrentMonth(monthState, handleEvents) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (monthState.currentMonth == YearMonth.now()) {
            CurrentDayIndicator()
        } else {
            MonthNavigationIndicator(monthState)
        }

        Text(
            text = currentMonthYear,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.primary
            ),
            maxLines = 1
        )

        Text(
            text = hijriFormatted,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.secondary
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun CurrentDayIndicator() {
    Text(
        text = "Today",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun MonthNavigationIndicator(monthState: MonthState) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            val (icon, text) = if (monthState.currentMonth.isAfter(YearMonth.now())) {
                Pair(R.drawable.angle_small_left_icon, "Previous")
            } else {
                Pair(R.drawable.angle_small_right_icon, "Next")
            }

            Icon(
                painter = painterResource(id = icon),
                contentDescription = "$text Month",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = "Return to Today",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    )
}

@Composable
private fun NavigationIconButton(
    iconId: Int,
    description: String,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = description,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun navigateToCurrentMonth(
    monthState: MonthState,
    handleEvents: KFunction1<LocalDate, Unit>
) {
    val yearMonthAtToday = YearMonth.now()
    monthState.currentMonth = yearMonthAtToday
    handleEvents(LocalDate.now())
}

private fun changeMonth(
    monthState: MonthState,
    monthsToAdd: Long,
    handleEvents: KFunction1<LocalDate, Unit>
) {
    monthState.currentMonth = monthState.currentMonth.plusMonths(monthsToAdd)
    val date = monthState.currentMonth.atDay(1)
    handleEvents(date)
}

fun getFormattedHijriDate(yearMonth: YearMonth): String {
    val formatterHijriMonth = DateTimeFormatter.ofPattern("MMMM")
    val uniqueHijriMonths = linkedSetOf<String>()

    // Check the start, middle, and end of the month for Hijri months
    val daysToCheck = listOf(1, yearMonth.lengthOfMonth() / 2, yearMonth.lengthOfMonth())
    for (day in daysToCheck) {
        val date = LocalDate.of(yearMonth.year, yearMonth.month, day)
        val hijriDateForDay = HijrahDate.from(date)
        uniqueHijriMonths.add(hijriDateForDay.format(formatterHijriMonth))
    }

    val hijriDate = HijrahDate.from(yearMonth.atDay(1))
    val hijriFormattedYear = hijriDate.format(DateTimeFormatter.ofPattern("yyyy"))

    return uniqueHijriMonths.joinToString(" / ") + " $hijriFormattedYear"
}
