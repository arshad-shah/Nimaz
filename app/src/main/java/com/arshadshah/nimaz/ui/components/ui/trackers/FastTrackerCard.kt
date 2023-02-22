package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
		Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 6.dp , end = 6.dp , top = 4.dp , bottom = 4.dp) ,
				horizontalAlignment = Alignment.CenterHorizontally ,
				verticalArrangement = Arrangement.Center
			  ) {
			if(!showDateSelector.value){
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									start = 6.dp ,
									end = 6.dp ,
									top = 4.dp ,
									bottom = 4.dp
									) ,
						horizontalArrangement = Arrangement.SpaceBetween ,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					Text(
							text = "Fasting" , style = MaterialTheme.typography.titleMedium
						)
					Text(
							text = LocalDate.parse(dateState.value)
								.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
							style = MaterialTheme.typography.titleMedium
						)
				}
			}
			Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(
								start = 6.dp ,
								end = 6.dp ,
								top = 4.dp ,
								bottom = 4.dp
								) ,
					horizontalArrangement = Arrangement.Start ,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				IconButton(
						modifier = Modifier
							.padding(vertical = 8.dp , horizontal = 4.dp)
							.size(32.dp)
							.border(1.dp , MaterialTheme.colorScheme.primary , CircleShape) ,
						onClick = {
							isFastingToday.value = !isFastingToday.value
							handleEvent(TrackerViewModel.TrackerEvent.UPDATE_FAST_TRACKER(
									FastTracker(
											date = dateState.value ,
											isFasting = isFastingToday.value
												)
										))
						}) {
					if (!isFastingToday.value){
						Icon(
								painter = painterResource(id = R.drawable.cross_icon) ,
								contentDescription = "Close" ,
								tint = MaterialTheme.colorScheme.primary ,
								modifier = Modifier.size(48.dp)
							)
					}else{
						Icon(
								painter = painterResource(id = R.drawable.check_icon) ,
								contentDescription = "Check" ,
								tint = MaterialTheme.colorScheme.primary ,
								modifier = Modifier.size(48.dp)
							)
					}
				}
				Text(
						text = if (isFastingToday.value) "Fasting" else "Not Fasting" ,
						style = MaterialTheme.typography.titleMedium,
						modifier = Modifier.padding(start = 16.dp)
					)
			}
		}
	}
}