package com.arshadshah.nimaz.ui.screens.tracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.header.MonthState
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PrayerTracker() {

	val showTracker = rememberSaveable { mutableStateOf(false) }
	val dateSelected = rememberSaveable { mutableStateOf("") }
	Column {
		SelectableCalendar(
				weekHeader = {weekState ->
					PrayerTrackerWeekHeader(weekState = weekState)
				},
				monthContainer = {
					ElevatedCard(
							elevation = CardDefaults.elevatedCardElevation(
									defaultElevation = 4.dp,
																		  )
								) {
						it(PaddingValues(3.dp))
					}
				},
				monthHeader = { monthState ->
					PrayerTrackerHeader(monthState = monthState)
				},
				calendarState = rememberSelectableCalendarState(
						confirmSelectionChange = {
							//strip the [] from the date
							val date = it.toString().replace("[", "").replace("]", "")
							if (date.isEmpty()){
								showTracker.value = false
							}else{
								val dateObject = LocalDate.parse(date)
								val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
								val formattedDate = dateObject.format(formatter)
								dateSelected.value = formattedDate
								showTracker.value = dateSelected.value.isNotEmpty()
							}
							true }
															   ),
						  )

		if (showTracker.value){
			PrayerTrackerTabs(paddingValues = PaddingValues(16.dp),dateSelected = dateSelected)
		}
	}
}

//composable for the header
@Composable
fun PrayerTrackerHeader(monthState : MonthState) {
	val currentMonth = monthState.currentMonth
	val currentYear = monthState.currentMonth.year
	//sentence case the month name
	val currentMonthName = currentMonth.month.name.substring(0, 1)
		.uppercase(Locale.ROOT) + currentMonth.month.name.substring(1).lowercase(
			Locale.ROOT
																				)

	ElevatedCard(
			modifier = Modifier
				.padding(8.dp),
			elevation = CardDefaults.elevatedCardElevation(
					defaultElevation = 4.dp,
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
					onClick = { monthState.currentMonth = monthState.currentMonth.minusMonths(1) },
					modifier = Modifier.padding(8.dp)
					  ) {
				Icon(
						imageVector = FeatherIcons.ArrowLeft ,
						contentDescription = "Previous Month"
					)
			}
				Text(
						text = "$currentMonthName $currentYear",
						style = MaterialTheme.typography.titleMedium ,
						maxLines = 1 ,
						modifier = Modifier.padding(8.dp)
					)

			//right arrow
			IconButton(
					onClick = { monthState.currentMonth = monthState.currentMonth.plusMonths(1) },
					modifier = Modifier.padding(8.dp)
					  ) {
				Icon(
						imageVector = FeatherIcons.ArrowRight ,
						contentDescription = "Next Month"
					)
			}
		}
	}
}

@Composable
fun PrayerTrackerWeekHeader(weekState : List<DayOfWeek>){
	ElevatedCard {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp) ,
				horizontalArrangement = Arrangement.SpaceEvenly
		   ) {
			weekState.forEach { dayOfWeek ->
				Text(
						text = dayOfWeek.name.substring(0, 3),
						style = MaterialTheme.typography.titleSmall ,
						maxLines = 1 ,
						overflow = TextOverflow.Ellipsis ,
						modifier = Modifier
							.weight(1f)
							.padding(8.dp)
					)
			}
		}
	}
}

//composable to show two tabs for the prayer tracker one for current day and one for the fasting tracker
@Composable
fun PrayerTrackerTabs(paddingValues : PaddingValues , dateSelected : MutableState<String>) {

	val (selectedTab , setSelectedTab) = rememberSaveable { mutableStateOf(0) }
	val titles = listOf("Prayer")
	ElevatedCard(
			modifier = Modifier.padding(paddingValues) ,
				) {
		Column(modifier = Modifier.padding(paddingValues)) {
			Text(text = "Date: ${dateSelected.value}", style = MaterialTheme.typography.titleSmall)
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
fun PrayerTrackerList() {
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
	val checkedState = remember { mutableStateListOf(false , false , false , false , false , false) }

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
					checkedState[index] = !checkedState[index]
				})
				Text(text = item)
			}
		}
	}
}

@Preview
@Composable
fun PrayerTrackerPreview() {
	NimazTheme {
		PrayerTracker()
	}
}