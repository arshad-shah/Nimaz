package com.arshadshah.nimaz.ui.components.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.ui.components.calender.CalendarDay
import com.arshadshah.nimaz.ui.components.calender.CalendarHeader
import com.arshadshah.nimaz.ui.components.calender.CalendarWeekHeader
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun Calendar(
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    trackers: List<LocalPrayersTracker>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    isMenstruatingProvider: (LocalDate) -> Boolean,
    isFastingProvider: (LocalDate) -> Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val locale = remember { Locale.getDefault() }
    val weekFields = remember(locale) { WeekFields.of(locale) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with month navigation
        CalendarHeader(
            currentMonth = currentMonth,
            onMonthChange = onMonthChanged,
            onTodayClick = { onDateSelected(LocalDate.now()) }
        )

        // Week days header
        CalendarWeekHeader(locale = locale)

        // Days grid
        val days = remember(currentMonth, weekFields) {
            generateDaysForMonth(currentMonth, weekFields)
        }

        val trackersMap = remember(trackers) {
            trackers.associateBy { it.date }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(max(100.dp, 400.dp)),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(days, key = { it.date.toString() }) { dayInfo ->
                CalendarDay(
                    date = dayInfo.date,
                    isSelected = dayInfo.date == selectedDate,
                    isToday = dayInfo.date == LocalDate.now(),
                    isFromCurrentMonth = dayInfo.isFromCurrentMonth,
                    tracker = trackersMap[dayInfo.date],
                    isMenstruating = isMenstruatingProvider(dayInfo.date),
                    isFasting = isFastingProvider(dayInfo.date),
                    onDateClick = onDateSelected,
                )
            }
        }
    }
}

private data class DayInfo(
    val date: LocalDate,
    val isFromCurrentMonth: Boolean
)

private fun generateDaysForMonth(
    yearMonth: YearMonth,
    weekFields: WeekFields
): List<DayInfo> {
    val firstDay = yearMonth.atDay(1)
    val lastDay = yearMonth.atEndOfMonth()

    // Find the first day of the week containing the first day of the month
    val firstDayOfGrid = firstDay.with(weekFields.dayOfWeek(), 1)

    // Calculate days needed to complete the grid
    val daysAfter = 7 - lastDay.get(weekFields.dayOfWeek())
    val lastDayOfGrid = lastDay.plusDays(daysAfter.toLong())

    return (0..ChronoUnit.DAYS.between(firstDayOfGrid, lastDayOfGrid))
        .map { day ->
            val currentDay = firstDayOfGrid.plusDays(day)
            DayInfo(
                date = currentDay,
                isFromCurrentMonth = currentDay.month == yearMonth.month
            )
        }
}

@Composable
fun rememberCalendarState(
    initialDate: LocalDate = LocalDate.now(),
    initialMonth: YearMonth = YearMonth.from(initialDate)
): CalendarState {
    return remember {
        CalendarState(initialDate, initialMonth)
    }
}

class CalendarState(
    initialDate: LocalDate,
    initialMonth: YearMonth
) {
    var selectedDate by mutableStateOf(initialDate)
        private set

    var currentMonth by mutableStateOf(initialMonth)
        private set

    fun onDateSelected(date: LocalDate) {
        selectedDate = date
        if (date.month != currentMonth.month) {
            currentMonth = YearMonth.from(date)
        }
    }

    fun onMonthChanged(month: YearMonth) {
        currentMonth = month
    }
}