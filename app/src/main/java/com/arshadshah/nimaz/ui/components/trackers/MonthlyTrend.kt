package com.arshadshah.nimaz.ui.components.trackers

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun PrayerTrackerGrid()
{
	val viewModelTracker = viewModel(
			key = AppConstants.TRACKING_VIEWMODEL_KEY ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
									)
	LaunchedEffect(Unit) {
		viewModelTracker.onEvent(
				TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
						LocalDate.now().toString()
																	)
								)
	}
	val progressForMonth = remember {
		viewModelTracker.progressForMonth
	}.collectAsState()
	val dateState = remember {
		viewModelTracker.dateState
	}.collectAsState()

	val currentDate = LocalDate.now()
	val yearMonth = YearMonth.of(currentDate.year , currentDate.month)
	val daysInMonth = yearMonth.lengthOfMonth()
	//Bit of a hack to get the day number to align
	val prayers = listOf("Fajr" , "Dhuhr" , "Asr" , "Maghrib" , "Isha")

	val userSelectedDate = LocalDate.parse(dateState.value)

	// a grid of 6 rows 1 for the number of the day and 5 for the prayers
	// amount of days in the month columns + 1 for the name of the prayer
	Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp , vertical = 8.dp) ,
			verticalArrangement = Arrangement.Center ,
			horizontalAlignment = Alignment.Start
		  ) {
		prayers.forEach { prayer ->
			Row(
					modifier = Modifier
						.fillMaxWidth() ,
					horizontalArrangement = Arrangement.SpaceBetween ,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				// Render the name of the prayer on the left
				//if its Maghri1 then it must be transparent
				Text(
						text = prayers[prayers.indexOf(prayer)] ,
						style = MaterialTheme.typography.labelSmall ,
						modifier = Modifier
							.width(40.dp) ,
					)

				// Render the small boxes (dots) for each day of the month
				for (i in 0 until daysInMonth)
				{
					val date = yearMonth.atDay(i + 1)
					val prayerTracker = progressForMonth.value.find { it.date == date.toString() }
					val isHighlighted = prayerTracker != null && prayerTracker.isPrayerCompleted(
							prayers[prayers.indexOf(prayer)]
																								)
					val isMenstrauting = prayerTracker?.isMenstruating ?: false

					Box(
							modifier = Modifier
								.size(8.dp)
								//if the day is today then add border
								.border(
										width = 1.dp ,
										color = when (date)
										{
											currentDate ->
											{
												MaterialTheme.colorScheme.tertiary
											}

											userSelectedDate ->
											{
												MaterialTheme.colorScheme.onSecondaryContainer
											}

											else ->
											{
												if (isHighlighted && !isMenstrauting)
												{
													MaterialTheme.colorScheme.primary
												} else if(isMenstrauting){
													//pink
													Color(0xFFE91E63)
												}else
												{
													Color.Gray
												}
											}
										} ,
										shape = CircleShape
									   )
								.background(
										color =
										when (date)
										{
											currentDate ->
											{
												if (isHighlighted)
												{
													MaterialTheme.colorScheme.primary
												} else if(isMenstrauting){
													//pink
													Color(0xFFE91E63)
												}else
												{
													Color.Gray
												}
											}

											userSelectedDate ->
											{
												if (isHighlighted)
												{
													MaterialTheme.colorScheme.primary
												} else if(isMenstrauting){
													//pink
													Color(
															0xFFE91E63
														 )
												} else
												{
													MaterialTheme.colorScheme.secondaryContainer
												}
											}

											else ->
											{
												if (isHighlighted)
												{
													MaterialTheme.colorScheme.primary
												} else if(isMenstrauting){
													//pink
													Color(0xFFE91E63)
												}else
												{
													Color.Gray
												}
											}
										} ,
										shape = CircleShape
										   )
					   )
				}
			}
		}
	}
}

// Extension function to check if a prayer is completed for a specific day
fun PrayerTracker.isPrayerCompleted(prayer : String) : Boolean
{
	return when (prayer)
	{
		"Fajr" -> fajr
		"Dhuhr" -> dhuhr
		"Asr" -> asr
		"Maghrib" -> maghrib
		"Isha" -> isha
		else -> false
	}
}