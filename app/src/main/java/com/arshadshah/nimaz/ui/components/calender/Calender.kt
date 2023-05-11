package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
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
	handleEvents : KFunction1<TrackerViewModel.TrackerEvent , Unit> ,
	progressForMonth : State<MutableList<PrayerTracker>> ,
	fastProgressForMonth : State<MutableList<FastTracker>>
			)
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge.copy(
					bottomStart = CornerSize(8.dp) ,
					bottomEnd = CornerSize(8.dp)
														) ,
			modifier = Modifier
				.fillMaxWidth()
				) {
		SelectableCalendar(
				horizontalSwipeEnabled = false ,
				dayContent = {
					CalenderDay(
							dayState = it ,
							handleEvents = handleEvents ,
							progressForMonth ,
							fastProgressForMonth
							   )
				} ,
				daysOfWeekHeader = { weekState ->
					CalenderWeekHeader(
							weekState = weekState,)
				} ,
				monthContainer = {
					CalenderMonth(monthState = it)
				} ,
				monthHeader = { monthState ->
					CalenderHeader(monthState = monthState, handleEvents = handleEvents)
				} ,
				calendarState = rememberSelectableCalendarState()
						  )
	}
}