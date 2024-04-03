package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import java.time.LocalDate
import kotlin.reflect.KFunction1

@Composable
fun Calender(
    handleEvents: KFunction1<LocalDate, Unit>,
    progressForMonth: State<List<LocalPrayersTracker>>,
    fastProgressForMonth: State<List<LocalFastTracker>>,
) {
    SelectableCalendar(
        horizontalSwipeEnabled = false,
        dayContent = { state ->
            CalenderDay(
                dayState = state,
                handleEvents = handleEvents,
                progressForMonth,
                fastProgressForMonth
            )
        },
        daysOfWeekHeader = { weekState ->
            CalenderWeekHeader(
                weekState = weekState,
            )
        },
        monthContainer = {
            CalenderMonth(monthState = it)
        },
        monthHeader = { monthState ->
            CalenderHeader(monthState = monthState, handleEvents = handleEvents)
        },
    )
}