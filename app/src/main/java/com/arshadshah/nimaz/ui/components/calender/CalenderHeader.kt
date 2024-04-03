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
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
    ){
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarNavigationButton(
                iconId = R.drawable.angle_left_icon,
                description = "Previous Month",
                onClick = { changeMonth(monthState, -1, handleEvents) }
            )

            CalendarMonthDisplay(
                currentMonthYear = currentMonthYear,
                hijriFormatted = hijriFormatted,
                monthState = monthState,
                handleEvents = handleEvents
            )

            CalendarNavigationButton(
                iconId = R.drawable.angle_right_icon,
                description = "Next Month",
                onClick = { changeMonth(monthState, 1, handleEvents) }
            )
        }
    }
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


@Composable
private fun CalendarMonthDisplay(
    currentMonthYear: String,
    hijriFormatted: String,
    monthState: MonthState,
    handleEvents: KFunction1<LocalDate, Unit>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clip(MaterialTheme.shapes.medium)
            .clickable { navigateToCurrentMonth(monthState, handleEvents) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (monthState.currentMonth == YearMonth.now()) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "Today",
                style = MaterialTheme.typography.titleSmall
            )
        } else {
            TodayIndicator(monthState)
        }

        Text(
            text = currentMonthYear,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            modifier = Modifier.padding(4.dp)
        )

        Text(
            text = hijriFormatted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
private fun TodayIndicator(monthState: MonthState) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconId = if (monthState.currentMonth.isAfter(YearMonth.now()))
            R.drawable.angle_small_left_icon
        else
            R.drawable.angle_small_right_icon

        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = iconId),
            contentDescription = if (monthState.currentMonth.isAfter(YearMonth.now())) "Previous Day" else "Next Day",
            tint = MaterialTheme.colorScheme.onPrimary
        )

        Text(
            text = "Today",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun CalendarNavigationButton(iconId: Int, description: String, onClick: () -> Unit) {
    FilledIconButton(onClick = onClick) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = iconId),
            contentDescription = description
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