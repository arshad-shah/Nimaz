package com.arshadshah.nimaz.ui.components.calender

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import io.github.boguszpawlowski.composecalendar.header.MonthState
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KFunction1

//composable for the header
@Composable
fun CalenderHeader(
	monthState : MonthState ,
	handleEvents : KFunction1<TrackerViewModel.TrackerEvent , Unit>
				  )
{
	val currentMonth = monthState.currentMonth
	val currentYear = monthState.currentMonth.year

	val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
	//sentence case the month name
	val currentMonthYear = currentMonth.format(formatter)

	val dateToBeUsedToGetHijriDate = LocalDate.of(currentYear , currentMonth.month , 1)
	//go through all the days in the month and get the hijri month for each day
	//add the hijri month to a list
	val formatterHijriMonth = DateTimeFormatter.ofPattern("MMMM")
	val listOfUniqueHijriMonths = mutableListOf<String>()
	//process this in a separate thread
	for (i in 1 .. currentMonth.lengthOfMonth())
	{
		val date = LocalDate.of(currentYear , currentMonth.month , i)
		val hijriDateForDay = HijrahDate.from(date)
		val formattedHijriDate = hijriDateForDay.format(formatterHijriMonth)
		if (! listOfUniqueHijriMonths.contains(formattedHijriDate))
		{
			listOfUniqueHijriMonths.add(formattedHijriDate)
		}
	}

	val hijriDate = HijrahDate.from(dateToBeUsedToGetHijriDate)
	val formatterHijriYear = DateTimeFormatter.ofPattern("yyyy")
	val hijriFormatedYear = hijriDate.format(formatterHijriYear)

	//create a string of the hijri months and year
	val hijriFormated = listOfUniqueHijriMonths.joinToString(" / ") + " $hijriFormatedYear"

	//a toggle to move user back to month with current date
	val showCurrentMonth = remember {
		mutableStateOf(false)
	}

	val inCurrentMonth = remember {
		mutableStateOf(true)
	}

	LaunchedEffect(showCurrentMonth.value) {
		if (showCurrentMonth.value)
		{
			val currentYearMonth = YearMonth.now()
			val currentMonth = monthState.currentMonth
			if (currentYearMonth != currentMonth)
			{
				monthState.currentMonth = currentYearMonth
				handleEvents(
						TrackerViewModel.TrackerEvent.SET_DATE(
								LocalDate.now().toString()
															  )
							)
				handleEvents(
						TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
								LocalDate.now().toString()
																		  )
							)
				handleEvents(
						TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
								LocalDate.now().toString()
																			   )
							)
				handleEvents(
						TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
								LocalDate.now().toString()
																			)
							)
				handleEvents(
						TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(
								LocalDate.now().toString()
																				 )
							)

			} else
			{
				handleEvents(
						TrackerViewModel.TrackerEvent.SET_DATE(
								LocalDate.now().toString()
															  )
							)
				handleEvents(
						TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
								LocalDate.now().toString()
																		  )
							)
				handleEvents(
						TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
								LocalDate.now().toString()
																			   )
							)
			}
			showCurrentMonth.value = false
			inCurrentMonth.value = true
		}
	}

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			elevation = CardDefaults.elevatedCardElevation(
					defaultElevation = 4.dp ,
														  )
				) {
		Row(
				modifier = Modifier
					.padding(horizontal = 14.dp)
					.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			//left arrow
			FilledIconButton(
					modifier = Modifier
						.size(52.dp) ,
					onClick = {
						monthState.currentMonth = monthState.currentMonth.minusMonths(1)
						//get a date in the new month
						val date = monthState.currentMonth.atDay(1)
						handleEvents(TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(date.toString()))
						handleEvents(
								TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(
										date.toString()
																						 )
									)
						inCurrentMonth.value = false
					} ,
							) {
				Icon(
						modifier = Modifier.size(24.dp) ,
						painter = painterResource(id = R.drawable.angle_left_icon) ,
						contentDescription = "Previous Month"
					)
			}
			Column(
					modifier = Modifier
						.clickable {
							showCurrentMonth.value = true
						} ,
					horizontalAlignment = Alignment.CenterHorizontally ,
					verticalArrangement = Arrangement.Center
				  ) {
				//if its today show today else show the today dimmed with a symbol to show which way to go to get to today
				if (monthState.currentMonth == YearMonth.now())
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
						if (monthState.currentMonth.isAfter(YearMonth.now()))
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
						text = currentMonthYear ,
						style = MaterialTheme.typography.titleLarge ,
						maxLines = 1 ,
						modifier = Modifier.padding(4.dp)
					)

				Text(
						text = hijriFormated ,
						style = MaterialTheme.typography.bodySmall ,
						maxLines = 1 ,
						modifier = Modifier.padding(4.dp)
					)
			}

			//right arrow
			FilledIconButton(
					modifier = Modifier.size(52.dp) ,
					onClick = {
						monthState.currentMonth = monthState.currentMonth.plusMonths(1)
						//get a date in the new month
						val date = monthState.currentMonth.atDay(1)
						handleEvents(TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(date.toString()))
						handleEvents(
								TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(
										date.toString()
																						 )
									)
						inCurrentMonth.value = false
					} ,
							) {
				Icon(
						modifier = Modifier.size(24.dp) ,
						painter = painterResource(id = R.drawable.angle_right_icon) ,
						contentDescription = "Next Month"
					)
			}
		}
	}
}