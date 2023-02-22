package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

@Composable
fun FastTrackerCard(
	showDateSelector : State<Boolean> ,
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
	dateState : State<String> ,
	isFastingToday : MutableState<Boolean>
				   )
{
	if(showDateSelector.value)
	{
		DateSelector(
				handleEvent = handleEvent
					)
	}
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(4.dp)
				) {
			Row(
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth(),
					horizontalArrangement = Arrangement.Start ,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				ToggleableItem(
						text = if (isFastingToday.value) "Fasting" else "Not Fasting"  ,
						checked = isFastingToday.value ,
						onCheckedChange = {
							isFastingToday.value = !isFastingToday.value
							handleEvent(TrackerViewModel.TrackerEvent.UPDATE_FAST_TRACKER(
									FastTracker(
											date = dateState.value ,
											isFasting = isFastingToday.value
											   )
																						 ))
						} ,
						modifier = Modifier
							.padding(8.dp)
							.placeholder(
									visible = false ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										) ,
						showDateSelector = showDateSelector.value
							  )
			}
	}
}