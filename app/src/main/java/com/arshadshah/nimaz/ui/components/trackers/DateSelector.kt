package com.arshadshah.nimaz.ui.components.trackers

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

@Composable
fun DateSelector(
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
				)
{
	val viewModel = viewModel(
			 key = AppConstants.TRACKING_VIEWMODEL_KEY ,
			 initializer = { TrackerViewModel() } ,
			 viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	val dateState = remember {
		viewModel.dateState
	}.collectAsState()
	val date = remember { mutableStateOf(LocalDate.parse(dateState.value)) }
	val hijrahDate = remember { mutableStateOf(HijrahDate.from(date.value)) }
	val newDay = remember { mutableStateOf(date.value.dayOfMonth) }
	val newMonth = remember { mutableStateOf(date.value.monthValue) }
	val newYear = remember { mutableStateOf(date.value.year) }
	ElevatedCard(
			 colors = CardDefaults.elevatedCardColors(
					  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp) ,
					  contentColor = MaterialTheme.colorScheme.onSurface ,
					  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
					  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
													 ) ,
			 shape = MaterialTheme.shapes.extraLarge ,
				) {
		Row(
				 modifier = Modifier
					 .fillMaxWidth()
					 .padding(horizontal = 16.dp , vertical = 8.dp) ,
				 horizontalArrangement = Arrangement.SpaceBetween ,
				 verticalAlignment = Alignment.CenterVertically
		   ) {
			FilledIconButton(
					 onClick = {
						 date.value = date.value.minusDays(1)
						 handleEvent(TrackerViewModel.TrackerEvent.SET_DATE(date.value.toString()))
						 handleEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(date.value.toString()))
						 handleEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(date.value.toString()))
						 newDay.value = date.value.dayOfMonth
						 newMonth.value = date.value.monthValue
						 newYear.value = date.value.year
						 hijrahDate.value = HijrahDate.from(date.value)
					 }) {
				Icon(
						 modifier = Modifier.size(24.dp) ,
						 painter = painterResource(id = R.drawable.angle_left_icon) ,
						 contentDescription = "Previous Day" ,
					)
			}
			Column(
					 modifier = Modifier
						 .padding(4.dp)
						 //a click to get user back to today
						 .clickable {
							 date.value = LocalDate.now()
							 handleEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(date.value.toString()))
							 handleEvent(
									  TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
											   date.value.toString()
																							 )
										)
							 newDay.value = date.value.dayOfMonth
							 newMonth.value = date.value.monthValue
							 newYear.value = date.value.year
							 hijrahDate.value = HijrahDate.from(date.value)
						 } ,
					 horizontalAlignment = Alignment.CenterHorizontally ,
					 verticalArrangement = Arrangement.Center
				  ) {
				//if its today show today else show the today dimmed with a symbol to show which way to go to get to today
				if (date.value == LocalDate.now())
				{
					Text(
							 modifier = Modifier
								 .padding(start = 4.dp , top = 4.dp , bottom = 4.dp) ,
							 text = "Today" ,
							 style = MaterialTheme.typography.titleSmall
						)
				} else
				{
					Row(
							 modifier = Modifier
								 .background(
										  color = MaterialTheme.colorScheme.primary ,
										  shape = MaterialTheme.shapes.small
											)
								 .padding(horizontal = 8.dp) ,
							 horizontalArrangement = Arrangement.Center ,
							 verticalAlignment = Alignment.CenterVertically
					   ) {
						if (date.value.isAfter(LocalDate.now()))
						{
							Icon(
									 modifier = Modifier.size(16.dp) ,
									 painter = painterResource(id = R.drawable.angle_small_left_icon) ,
									 contentDescription = "Previous Day" ,
									 tint = MaterialTheme.colorScheme.onPrimary
								)
							Text(
									 text = "Today" ,
									 style = MaterialTheme.typography.titleSmall ,
									 modifier = Modifier
										 .padding(start = 4.dp , top = 4.dp , bottom = 4.dp) ,
									 color = MaterialTheme.colorScheme.onPrimary
								)
						} else
						{
							Text(
									 text = "Today" ,
									 style = MaterialTheme.typography.titleSmall ,
									 modifier = Modifier
										 .padding(start = 4.dp , top = 4.dp , bottom = 4.dp) ,
									 color = MaterialTheme.colorScheme.onPrimary
								)
							Icon(
									 modifier = Modifier.size(16.dp) ,
									 painter = painterResource(id = R.drawable.angle_small_right_icon) ,
									 contentDescription = "Next Day" ,
									 tint = MaterialTheme.colorScheme.onPrimary

								)
						}
					}
				}
				Text(
						 text = date.value.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")) ,
						 style = MaterialTheme.typography.titleMedium
					)
				Text(
						 text = hijrahDate.value.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
						 style = MaterialTheme.typography.bodySmall
					)
			}
			FilledIconButton(
					 onClick = {
						 date.value = date.value.plusDays(1)
						 handleEvent(TrackerViewModel.TrackerEvent.SET_DATE(date.value.toString()))
						 handleEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(date.value.toString()))
						 handleEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(date.value.toString()))
						 newDay.value = date.value.dayOfMonth
						 newMonth.value = date.value.monthValue
						 newYear.value = date.value.year
						 hijrahDate.value = HijrahDate.from(date.value)
					 }) {
				Icon(
						 modifier = Modifier.size(24.dp) ,
						 painter = painterResource(id = R.drawable.angle_right_icon) ,
						 contentDescription = "Next Day" ,
					)
			}
		}
	}
}