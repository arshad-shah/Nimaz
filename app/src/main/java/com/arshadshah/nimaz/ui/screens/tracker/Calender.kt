package com.arshadshah.nimaz.ui.screens.tracker

import androidx.activity.ComponentActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_CALENDER
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardFastTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayerTracker
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.header.MonthState
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.reflect.KFunction1

@Composable
fun Calender(paddingValues : PaddingValues)
{

	val mutableDate = remember { mutableStateOf(LocalDate.now()) }

	val viewModel = viewModel(
			key = TRACKING_VIEWMODEL_KEY ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	//call this effect only once
	LaunchedEffect(Unit) {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(mutableDate.value.toString()))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(mutableDate.value.toString()))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(mutableDate.value.toString()))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(mutableDate.value.toString()))
	}

	val dateState = remember {
		viewModel.dateState
	}.collectAsState()

	val progressForMonth = remember {
		viewModel.progressForMonth
	}.collectAsState()

	val fastProgressForMonth = remember {
		viewModel.fastProgressForMonth
	}.collectAsState()

	LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.testTag(TEST_TAG_CALENDER) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Top
			  ) {
		item {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.fillMaxWidth()
						) {
				SelectableCalendar(
						horizontalSwipeEnabled = false ,
						dayContent = {
							CalenderDay(
									dayState = it ,
									handleEvents = viewModel::onEvent ,
									progressForMonth ,
									fastProgressForMonth
									   )
						} ,
						daysOfWeekHeader = { weekState ->
							CalenderWeekHeader(weekState = weekState)
						} ,
						monthContainer = {
							CalenderMonth(monthState = it)
						} ,
						monthHeader = { monthState ->
							CalenderHeader(monthState = monthState)
						} ,
						calendarState = rememberSelectableCalendarState()
								  )
			}
		}
		item {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(top = 8.dp)
						.fillMaxWidth()
						) {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									start =
									24.dp , end = 24.dp , top = 12.dp , bottom = 8.dp
									) ,
						horizontalArrangement = Arrangement.SpaceBetween ,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					Text(
							text = "Trackers" , style = MaterialTheme.typography.titleMedium
						)
					Text(
							text = LocalDate.parse(dateState.value)
								.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
							style = MaterialTheme.typography.titleMedium
						)
				}
				DashboardPrayerTracker(
						onNavigateToTracker = {}
									  )

				DashboardFastTracker()
			}
		}
	}
}

//composable for the header
@Composable
fun CalenderHeader(monthState : MonthState)
{
	val viewModel = viewModel(
			key = TRACKING_VIEWMODEL_KEY ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
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
				viewModel.onEvent(
						TrackerViewModel.TrackerEvent.SET_DATE(
								LocalDate.now().toString()
															  )
								 )
				viewModel.onEvent(
						TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
								LocalDate.now().toString()
																		  )
								 )
				viewModel.onEvent(
						TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
								LocalDate.now().toString()
																			   )
								 )
				viewModel.onEvent(
						TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
								LocalDate.now().toString()
																			)
								 )
				viewModel.onEvent(
						TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(
								LocalDate.now().toString()
																				 )
								 )

			} else
			{
				viewModel.onEvent(
						TrackerViewModel.TrackerEvent.SET_DATE(
								LocalDate.now().toString()
															  )
								 )
				viewModel.onEvent(
						TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
								LocalDate.now().toString()
																		  )
								 )
				viewModel.onEvent(
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
			modifier = Modifier
				.clickable {
					showCurrentMonth.value = true
				} ,
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
						viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(date.toString()))
						viewModel.onEvent(
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
							horizontalArrangement = Arrangement.Center ,
							verticalAlignment = Alignment.CenterVertically
					   ) {
						if (monthState.currentMonth.isAfter(YearMonth.now()))
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
										.padding(start = 4.dp , top = 4.dp , bottom = 4.dp)
										.alpha(0.5f)
								)
						} else
						{
							Text(
									text = "Today" ,
									style = MaterialTheme.typography.titleSmall ,
									modifier = Modifier
										.padding(start = 4.dp , top = 4.dp , bottom = 4.dp)
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
						viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(date.toString()))
						viewModel.onEvent(
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

@Composable
fun CalenderWeekHeader(weekState : List<DayOfWeek>)
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier.padding(top = 8.dp) ,
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 4.dp , vertical = 8.dp) ,
				horizontalArrangement = Arrangement.Center
		   ) {
			weekState.forEach { dayOfWeek ->
				Text(
						text = dayOfWeek.name.substring(0 , 3) ,
						style = MaterialTheme.typography.titleMedium ,
						color = if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
						{
							MaterialTheme.colorScheme.error
						} else
						{
							MaterialTheme.colorScheme.onSurface
						} ,
						maxLines = 1 ,
						overflow = TextOverflow.Ellipsis ,
						textAlign = TextAlign.Center ,
						modifier = Modifier
							.weight(1f)
							.padding(4.dp)
					)
			}
		}
	}
}

@Composable
fun CalenderMonth(monthState : @Composable (PaddingValues) -> Unit)
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
				) {
		monthState(PaddingValues(0.dp))
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalenderDay(
	dayState : DayState<DynamicSelectionState> ,
	handleEvents : KFunction1<TrackerViewModel.TrackerEvent , Unit> ,
	progressForMonth : State<MutableList<PrayerTracker>> ,
	fastProgressForMonth : State<MutableList<FastTracker>> ,
			   )
{

	//get the day for the hijri calendar
	val hijriDay = HijrahDate.from(dayState.date)
	val currentDate = dayState.date
	val today = dayState.isCurrentDay

	val isSelectedDay = dayState.selectionState.isDateSelected(currentDate)

	val hasDescription = remember { mutableStateOf(false) }

	val hijriMonth = hijriDay[ChronoField.MONTH_OF_YEAR]
	val hijriDayOfMonth = hijriDay[ChronoField.DAY_OF_MONTH]
	//check if today is an important day and if so, display the description of the day and highlight the day
	val importantDay = isImportantDay(hijriDayOfMonth , hijriMonth)

	//find todays tracker in the list of trackers from progressForMonth
	val todaysTracker = progressForMonth.value.find { it.date == currentDate.toString() }
	val todaysFastTracker = fastProgressForMonth.value.find { it.date == currentDate.toString() }
	ElevatedCard(
			shape = MaterialTheme.shapes.large ,
			elevation = CardDefaults.elevatedCardElevation(
					defaultElevation = if (dayState.isFromCurrentMonth) 2.dp else 0.dp ,
														  ) ,
			modifier = Modifier
				.padding(2.dp)
				.alpha(if (dayState.isFromCurrentMonth) 1f else 0.2f)
				.border(
						width = if (isSelectedDay || today) 2.dp else 0.dp ,
						color = when (importantDay.first)
						{
							false -> if (isSelectedDay && ! today) MaterialTheme.colorScheme.tertiary.copy(
									alpha = 0.5f
																										  )
							else if (today) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
							else MaterialTheme.colorScheme.surface

							true -> if (isSelectedDay && ! today) MaterialTheme.colorScheme.tertiary.copy(
									alpha = 0.5f
																										 )
							else if (today) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
							else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
						} ,
						shape = MaterialTheme.shapes.large
					   ) ,
			colors = CardDefaults.elevatedCardColors(
					containerColor = when (importantDay.first)
					{
						false -> if (isSelectedDay && ! today) MaterialTheme.colorScheme.tertiaryContainer
						else if (today) MaterialTheme.colorScheme.secondaryContainer
						else MaterialTheme.colorScheme.surface

						true -> if (isSelectedDay && ! today) MaterialTheme.colorScheme.tertiaryContainer
						else if (today) MaterialTheme.colorScheme.surface
						else MaterialTheme.colorScheme.primaryContainer
					}
													)
				) {

		Column(
				modifier = Modifier
					.combinedClickable(
							enabled = dayState.isFromCurrentMonth ,
							onClick = {
								dayState.selectionState.onDateSelected(dayState.date)
								handleEvents(
										TrackerViewModel.TrackerEvent.SET_DATE(
												dayState.date.toString()
																			  )
											)
								handleEvents(
										TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
												dayState.date.toString()
																						  )
											)
								handleEvents(
										TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
												dayState.date.toString()
																							   )
											)
								handleEvents(
										TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
												dayState.date.toString()
																							)
											)
								handleEvents(
										TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(
												dayState.date.toString()
																								 )
											)
							} ,
							onLongClick = {
								if (importantDay.first)
								{
									hasDescription.value = ! hasDescription.value
								}
							}
									  ) ,
				horizontalAlignment = Alignment.CenterHorizontally
			  ) {
			Text(
					text = dayState.date.dayOfMonth.toString() ,
					style = MaterialTheme.typography.titleMedium ,
					maxLines = 1 ,
					overflow = TextOverflow.Ellipsis ,
					modifier = Modifier.padding(horizontal = 6.dp , vertical = 4.dp) ,
					color = when (importantDay.first)
					{
						false -> if (isSelectedDay && ! today) MaterialTheme.colorScheme.onTertiaryContainer
						else if (today) MaterialTheme.colorScheme.onSecondaryContainer
						else MaterialTheme.colorScheme.onSurface

						true -> if (isSelectedDay && ! today) MaterialTheme.colorScheme.onTertiaryContainer
						else if (today) MaterialTheme.colorScheme.onSurface
						else MaterialTheme.colorScheme.onPrimaryContainer
					}
				)
			Row(
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth() ,
					horizontalArrangement = Arrangement.SpaceEvenly
			   ) {
				//fajr
				Box(
						modifier = Modifier
							.size(4.dp)
							.background(
									color = if (todaysTracker?.fajr == true) MaterialTheme.colorScheme.primary
									else Color.Transparent ,
									shape = CircleShape
									   )
				   )
				//dhuhr
				Box(
						modifier = Modifier
							.size(4.dp)
							.background(
									color = if (todaysTracker?.dhuhr == true) MaterialTheme.colorScheme.primary
									else Color.Transparent ,
									shape = CircleShape
									   )
				   )
				//asr
				Box(
						modifier = Modifier
							.size(4.dp)
							.background(
									color = if (todaysTracker?.asr == true) MaterialTheme.colorScheme.primary
									else Color.Transparent ,
									shape = CircleShape
									   )
				   )
				//maghrib
				Box(
						modifier = Modifier
							.size(4.dp)
							.background(
									color = if (todaysTracker?.maghrib == true) MaterialTheme.colorScheme.primary
									else Color.Transparent ,
									shape = CircleShape
									   )
				   )
				//isha
				Box(
						modifier = Modifier
							.size(4.dp)
							.background(
									color = if (todaysTracker?.isha == true) MaterialTheme.colorScheme.primary
									else Color.Transparent ,
									shape = CircleShape
									   )
				   )
			}
			Row(
					modifier = Modifier ,
					horizontalArrangement = Arrangement.SpaceEvenly ,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				Text(
						//put a letter scissor ha in front of the day to show that it is a hijri day
						text = "ه" + hijriDay[ChronoField.DAY_OF_MONTH].toString() ,
						style = MaterialTheme.typography.titleMedium ,
						maxLines = 1 ,
						overflow = TextOverflow.Ellipsis ,
						modifier = Modifier
							.padding(vertical = 4.dp , horizontal = 3.dp) ,
						color = when (importantDay.first)
						{
							false -> if (isSelectedDay && ! today) MaterialTheme.colorScheme.onTertiaryContainer
							else if (today) MaterialTheme.colorScheme.onSecondaryContainer
							else MaterialTheme.colorScheme.onSurface

							true -> if (isSelectedDay && ! today) MaterialTheme.colorScheme.onTertiaryContainer
							else if (today) MaterialTheme.colorScheme.onSurface
							else MaterialTheme.colorScheme.onTertiaryContainer
						}
					)
				Box(
						modifier = Modifier
							.size(6.dp)
							.background(
									color = if (todaysFastTracker?.isFasting == true) MaterialTheme.colorScheme.error
									else Color.Transparent ,
									shape = CircleShape
									   )
				   )
			}
		}

	}

	if (hasDescription.value)
	{
		Popup(
				alignment = Alignment.TopCenter ,
				offset = IntOffset(0 , - 120) ,
				onDismissRequest = { hasDescription.value = false }
			 ) {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.clickable {
							hasDescription.value = ! hasDescription.value
						} ,
						) {
				Text(
						text = importantDay.second ,
						style = MaterialTheme.typography.bodyMedium ,
						modifier = Modifier.padding(8.dp)
					)
			}
		}
	}
}

//function to check if a day is an important day
fun isImportantDay(day : Int , month : Int) : Pair<Boolean , String>
{
	val importantDays = mapOf(
			7 to mapOf(27 to "Al Isra Wal Mi'raj") ,
			8 to mapOf(15 to "Shab-e-Barat") ,
			9 to mapOf(
					1 to "Ramadan Starts (estimated)" ,
					2 to "Ramadan day 2" ,
					3 to "Ramadan day 3" ,
					4 to "Ramadan day 4" ,
					5 to "Ramadan day 5" ,
					6 to "Ramadan day 6" ,
					7 to "Ramadan day 7" ,
					8 to "Ramadan day 8" ,
					9 to "Ramadan day 9" ,
					10 to "Ramadan day 10" ,
					11 to "Ramadan day 11" ,
					12 to "Ramadan day 12" ,
					13 to "Ramadan day 13" ,
					14 to "Ramadan day 14" ,
					15 to "Ramadan day 15" ,
					16 to "Ramadan day 16" ,
					17 to "Ramadan day 17" ,
					18 to "Ramadan day 18" ,
					19 to "Ramadan day 19" ,
					20 to "Ramadan day 20" ,
					21 to "Ramadan day 21" ,
					22 to "Ramadan day 22" ,
					23 to "Ramadan day 23" ,
					24 to "Ramadan day 24" ,
					25 to "Ramadan day 25" ,
					26 to "Ramadan day 26" ,
					27 to "Laylatul Qadr" ,
					28 to "Laylatul Qadr" ,
					29 to "Laylatul Qadr" ,
					30 to "Laylatul Qadr"
					  ) ,
			10 to mapOf(1 to "Eid Al Fitr") ,
			12 to mapOf(
					1 to "Start of Dul Hijjah" ,
					2 to "Dul Hijjah 2" ,
					3 to "Dul Hijjah 3" ,
					4 to "Dul Hijjah 4" ,
					5 to "Dul Hijjah 5" ,
					6 to "Dul Hijjah 6" ,
					7 to "Dul Hijjah 7" ,
					8 to "Hajj Day 1" ,
					9 to "Day of Arafah" ,
					10 to "Eid Al Adha"
					   ) ,
			1 to mapOf(1 to "Islamic New Year" , 10 to "Day of Ashura") ,
			3 to mapOf(12 to "Mawlid An Nabawi")
							 )
	return if (importantDays[month]?.contains(day) == true)
	{
		//structure of the map is month (key) -> day (key) -> description (value)
		Pair(true , importantDays[month] !![day] ?: "")
	} else
	{
		Pair(false , "")
	}
}

@Preview
@Composable
fun PrayerTrackerPreview()
{
	NimazTheme(darkTheme = true) {
		Calender(PaddingValues(8.dp))
	}
}