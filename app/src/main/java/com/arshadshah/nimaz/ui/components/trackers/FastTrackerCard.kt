package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.ui.components.common.ToggleableItemColumn
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import es.dmoral.toasty.Toasty
import java.time.LocalDate

@Composable
fun FastTrackerCard(
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
	dateState : State<String> ,
	isFastingToday : MutableState<Boolean> ,
				   )
{
	val context = LocalContext.current
	val dateForTracker = LocalDate.parse(dateState.value)
	val isAfterToday = dateForTracker.isAfter(LocalDate.now())

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp) ,
				) {
		ToggleableItemColumn(
				text = if (dateForTracker.isBefore(LocalDate.now())) "Did not fast"
				else "Not Fasting" ,
				//if date state is in the fast then shoow the date
				// like this:
				// Fasted on 2021-09-01
				selectedText = if (dateForTracker.isBefore(LocalDate.now())) "Fasted on ${dateState.value}" else "Fasting Today" ,
				checked = isFastingToday.value ,
				onCheckedChange = {
					//if the date is after today then don't allow the user to change the value
					if (isAfterToday)
					{
						Toasty.info(
								context ,
								"Oops! you cant update the tracker for a date in the future" ,
								Toasty.LENGTH_SHORT ,
								true
								   ).show()
						return@ToggleableItemColumn
					}
					isFastingToday.value = ! isFastingToday.value
					handleEvent(
							TrackerViewModel.TrackerEvent.UPDATE_FAST_TRACKER(
									FastTracker(
											date = dateState.value ,
											isFasting = isFastingToday.value
											   )
																			 )
							   )
				} ,
				modifier = Modifier
					.padding(start = 16.dp , end = 16.dp , top = 8.dp , bottom = 8.dp)
					.fillMaxWidth()
					.placeholder(
							visible = false ,
							color = MaterialTheme.colorScheme.outline ,
							shape = RoundedCornerShape(4.dp) ,
							highlight = PlaceholderHighlight.shimmer(
									highlightColor = Color.White ,
																	)
								) ,
							)
	}
}