package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
			shape = MaterialTheme.shapes.extraLarge ,
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(4.dp) ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			IconButton(onClick = {
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
						painter = painterResource(id = R.drawable.angle_small_left_icon) ,
						contentDescription = "Previous Day" ,
						tint = MaterialTheme.colorScheme.primary
					)
			}
			Column(
					modifier = Modifier
						.padding(4.dp)
						//a click to get user back to today
						.clickable {
							date.value = LocalDate.now()
							handleEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(date.value.toString()))
							handleEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(date.value.toString()))
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
							text = "Today" ,
							style = MaterialTheme.typography.titleSmall
						)
				} else
				{
					Row(
							horizontalArrangement = Arrangement.Center ,
							verticalAlignment = Alignment.CenterVertically
					   ) {
						if (date.value.isAfter(LocalDate.now()))
						{
							Icon(
									modifier = Modifier.size(16.dp) ,
									painter = painterResource(id = R.drawable.angle_small_left_icon) ,
									contentDescription = "Previous Day" ,
									tint = MaterialTheme.colorScheme.primary
								)
							Text(
									text = "Today" ,
									style = MaterialTheme.typography.titleSmall ,
									modifier = Modifier
										.padding(start = 4.dp)
										.alpha(0.5f)
								)
						} else
						{
							Text(
									text = "Today" ,
									style = MaterialTheme.typography.titleSmall ,
									modifier = Modifier
										.padding(start = 4.dp)
										.alpha(0.5f)
								)
							Icon(
									modifier = Modifier.size(16.dp) ,
									painter = painterResource(id = R.drawable.angle_small_right_icon) ,
									contentDescription = "Next Day" ,
									tint = MaterialTheme.colorScheme.primary
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
			IconButton(onClick = {
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
						painter = painterResource(id = R.drawable.angle_small_right_icon) ,
						contentDescription = "Next Day" ,
						tint = MaterialTheme.colorScheme.primary
					)
			}
		}
	}
}