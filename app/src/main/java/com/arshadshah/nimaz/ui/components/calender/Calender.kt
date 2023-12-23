package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import kotlin.reflect.KFunction1

@Composable
fun Calender(
    handleEvents: KFunction1<TrackerViewModel.TrackerEvent, Unit>,
    progressForMonth: State<MutableList<PrayerTracker>>,
    fastProgressForMonth: State<MutableList<FastTracker>>,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp),
        ),
        modifier = Modifier
            .fillMaxWidth()
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
            calendarState = rememberSelectableCalendarState()
        )
    }
}