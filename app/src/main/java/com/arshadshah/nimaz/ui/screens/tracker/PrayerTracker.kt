package com.arshadshah.nimaz.ui.screens.tracker

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme
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

@Composable
fun PrayerTracker()
{

	val showTracker = rememberSaveable { mutableStateOf(false) }
	val dateSelected = rememberSaveable { mutableStateOf("") }
	Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(8.dp) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
		  ) {
		ElevatedCard(
				modifier = Modifier
					.fillMaxWidth()
					) {
			SelectableCalendar(
					dayContent = {
						PrayerTrackerDay(dayState = it)
					} ,
					weekHeader = { weekState ->
						PrayerTrackerWeekHeader(weekState = weekState)
					} ,
					monthContainer = {
						PrayerTrackerMonth(monthState = it)
					} ,
					monthHeader = { monthState ->
						PrayerTrackerHeader(monthState = monthState)
					} ,
					calendarState = rememberSelectableCalendarState() ,
							  )
		}

		if (showTracker.value)
		{
			PrayerTrackerTabs(paddingValues = PaddingValues(16.dp) , dateSelected = dateSelected)
		}
	}
}

//composable for the header
@Composable
fun PrayerTrackerHeader(monthState : MonthState)
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
			}
			showCurrentMonth.value = false
			inCurrentMonth.value = true
		}
	}

	ElevatedCard(
			modifier = Modifier
				.clickable(
						enabled = ! inCurrentMonth.value ,
						  ) {
					showCurrentMonth.value = true
				} ,
			elevation = CardDefaults.elevatedCardElevation(
					defaultElevation = 4.dp ,
														  )
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			//left arrow
			IconButton(
					onClick = {
						monthState.currentMonth = monthState.currentMonth.minusMonths(1)
						inCurrentMonth.value = false
					} ,
					  ) {
				Icon(
						modifier = Modifier.size(48.dp) ,
						painter = painterResource(id = R.drawable.angle_small_left_icon) ,
						contentDescription = "Previous Month"
					)
			}
			Column(
					horizontalAlignment = Alignment.CenterHorizontally ,
					verticalArrangement = Arrangement.Center
				  ) {
				Text(
						text = currentMonthYear ,
						style = MaterialTheme.typography.titleLarge ,
						maxLines = 1 ,
						modifier = Modifier.padding(8.dp)
					)

				Text(
						text = hijriFormated ,
						style = MaterialTheme.typography.bodySmall ,
						maxLines = 1 ,
						modifier = Modifier.padding(8.dp)
					)
			}

			//right arrow
			IconButton(
					onClick = {
						monthState.currentMonth = monthState.currentMonth.plusMonths(1)
						inCurrentMonth.value = false
					} ,
					  ) {
				Icon(
						modifier = Modifier.size(48.dp) ,
						painter = painterResource(id = R.drawable.angle_small_right_icon) ,
						contentDescription = "Next Month"
					)
			}
		}
	}
}

@Composable
fun PrayerTrackerWeekHeader(weekState : List<DayOfWeek>)
{
	ElevatedCard(
			modifier = Modifier.padding(vertical = 8.dp) ,
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp) ,
				horizontalArrangement = Arrangement.Center
		   ) {
			weekState.forEach { dayOfWeek ->
				Text(
						text = dayOfWeek.name.substring(0 , 3) ,
						style = MaterialTheme.typography.titleSmall ,
						maxLines = 1 ,
						overflow = TextOverflow.Ellipsis ,
						textAlign = TextAlign.Center ,
						modifier = Modifier
							.weight(1f)
					)
			}
		}
	}
}

@Composable
fun PrayerTrackerMonth(monthState : @Composable (PaddingValues) -> Unit)
{
	ElevatedCard {
		monthState(PaddingValues(8.dp))
	}
}

@Composable
fun PrayerTrackerDay(
	dayState : DayState<DynamicSelectionState> ,
					)
{
	//get the day for the hijri calendar
	val hijriDay = HijrahDate.from(dayState.date)
	val today = dayState.isCurrentDay

	val hasDescription = remember { mutableStateOf(false) }
//	val highlighted =  remember { mutableStateOf(dayState.selectionState.isDateSelected(dayState.date)) }

	val hijriMonth = hijriDay[ChronoField.MONTH_OF_YEAR]
	val hijriDayOfMonth = hijriDay[ChronoField.DAY_OF_MONTH]
	//check if today is an important day and if so, display the description of the day and highlight the day
	val importantDay = isImportantDay(hijriDayOfMonth , hijriMonth)
	ElevatedCard(
			modifier = Modifier
				.padding(4.dp)
				.alpha(if (dayState.isFromCurrentMonth) 1f else 0.5f)
				) {

		Column(
				modifier = Modifier
					.border(
							width = 1.dp ,
							color = if (today) MaterialTheme.colorScheme.primary else Color.Transparent ,
							shape = MaterialTheme.shapes.medium
						   )
					.clickable(
							enabled = dayState.isFromCurrentMonth ,
							  ) {
						when (importantDay.first)
						{
							false -> if (dayState.isFromCurrentMonth)
							{
								dayState.selectionState.onDateSelected(dayState.date)
							}

							else ->
							{
								//show the description of the day
								hasDescription.value = ! hasDescription.value
							}
						}
					} ,
				horizontalAlignment = Alignment.CenterHorizontally
			  ) {
			Text(
					text = dayState.date.dayOfMonth.toString() ,
					style = MaterialTheme.typography.titleSmall ,
					maxLines = 1 ,
					overflow = TextOverflow.Ellipsis ,
					color = when (importantDay.first)
					{
						false -> if (today) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
						else -> MaterialTheme.colorScheme.primary
					}
				)
			Divider(
					modifier = Modifier
						.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
				   )
			Text(
					//put a letter scissor ha in front of the day to show that it is a hijri day
					text = "ه" + hijriDay[ChronoField.DAY_OF_MONTH].toString() ,
					style = MaterialTheme.typography.titleSmall ,
					maxLines = 1 ,
					overflow = TextOverflow.Ellipsis ,
					color = when (importantDay.first)
					{
						false -> if (today) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
						else -> MaterialTheme.colorScheme.primary
					}
				)
		}

	}

	if (hasDescription.value)
	{
		Popup(
				alignment = Alignment.TopCenter ,
				offset = IntOffset(0 , - 130) ,
			 ) {
			ElevatedCard(
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

//composable to show two tabs for the prayer tracker one for current day and one for the fasting tracker
@Composable
fun PrayerTrackerTabs(paddingValues : PaddingValues , dateSelected : MutableState<String>)
{

	val (selectedTab , setSelectedTab) = rememberSaveable { mutableStateOf(0) }
	val titles = listOf("Prayer")
	ElevatedCard(
			modifier = Modifier.padding(paddingValues) ,
				) {
		Column(modifier = Modifier.padding(paddingValues)) {
			Text(text = "Date: ${dateSelected.value}" , style = MaterialTheme.typography.titleSmall)
			TabRow(selectedTabIndex = selectedTab) {
				titles.forEachIndexed { index , title ->
					Tab(
							selected = selectedTab == index ,
							onClick = { setSelectedTab(index) } ,
							text = {
								Text(
										text = title ,
										maxLines = 2 ,
										overflow = TextOverflow.Ellipsis ,
										style = MaterialTheme.typography.titleSmall
									)
							}
					   )
				}
			}
			when (selectedTab)
			{
				0 ->
				{
					PrayerTrackerList()
				}
			}
		}
	}
}

//Prayer tracker list
@Composable
fun PrayerTrackerList()
{
	//a list of toggleable items
	val items = listOf(
			"Fajr" ,
			"Sunrise" ,
			"Zuhr" ,
			"Asr" ,
			"Maghrib" ,
			"Isha"
					  )

	//a list of booleans to keep track of the state of the toggleable items
	val checkedState =
		remember { mutableStateListOf(false , false , false , false , false , false) }

	//the list of items
	Column {
		items.forEachIndexed { index , item ->
			//the toggleable item
			Row(
					modifier = Modifier.fillMaxWidth() ,
					horizontalArrangement = Arrangement.SpaceBetween ,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				RadioButton(selected = checkedState[index] , onClick = {
					checkedState[index] = ! checkedState[index]
				})
				Text(text = item)
			}
		}
	}
}

@Preview
@Composable
fun PrayerTrackerPreview()
{
	NimazTheme {
		PrayerTracker()
	}
}