package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import es.dmoral.toasty.Toasty
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

@Composable
fun PrayerTracker(paddingValues : PaddingValues)
{
	val mutableDate = remember { mutableStateOf(LocalDate.now()) }
	val (selectedTab , setSelectedTab) = rememberSaveable { mutableStateOf(0) }
	val titles = listOf("Prayer")

	val viewModel = viewModel(initializer = { TrackerViewModel() }, viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)

	LaunchedEffect(key1 = "getTrackerForDate") {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(mutableDate.value.toString()))
	}

	val stateOfTrackerForToday = remember {
		viewModel.trackerState
	}.collectAsState()

	val dateState = remember {
		viewModel.dateState
	}.collectAsState()

	val fajrState = remember {
		viewModel.fajrState
	}.collectAsState()

	val zuhrState = remember {
		viewModel.zuhrState
	}.collectAsState()

	val asrState = remember {
		viewModel.asrState
	}.collectAsState()

	val maghribState = remember {
		viewModel.maghribState
	}.collectAsState()

	val ishaState = remember {
		viewModel.ishaState
	}.collectAsState()



	Column(modifier = Modifier.padding(paddingValues) , horizontalAlignment = Alignment.CenterHorizontally) {

	ElevatedCard(
			modifier = Modifier.padding(top = 0.dp , bottom = 8.dp , start = 8.dp , end = 8.dp) ,
				) {
		Column {
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
					PrayerTrackerList(
							viewModel::onEvent ,
							stateOfTrackerForToday.value,
							mutableDate,
							fajrState.value,
							zuhrState.value,
							asrState.value,
							maghribState.value,
							ishaState.value
									 )
				}
			}
		}
	}
	}
}

@Composable
fun DateSelector(
	date : MutableState<LocalDate> ,
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
				 )
{

	val hijrahDate = remember { mutableStateOf(HijrahDate.from(date.value)) }
	val newDay = remember { mutableStateOf(date.value.dayOfMonth) }
	val newMonth = remember { mutableStateOf(date.value.monthValue) }
	val newYear = remember { mutableStateOf(date.value.year) }
	ElevatedCard {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(4.dp) ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			IconButton(onClick = {
				date.value = date.value.minusDays(1)
				handleEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(date.value.toString()))
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
							newDay.value = date.value.dayOfMonth
							newMonth.value = date.value.monthValue
							newYear.value = date.value.year
							hijrahDate.value = HijrahDate.from(date.value)
						}
					,
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				  ) {
				//if its today show today else show the today dimmed with a symbol to show which way to go to get to today
				if (date.value == LocalDate.now())
				{
					Text(
							text = "Today" ,
							style = MaterialTheme.typography.titleSmall
						)
				}
				else
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
									style = MaterialTheme.typography.titleSmall,
									modifier = Modifier
										.padding(start = 4.dp)
										.alpha(0.5f)
								)
						}
						else
						{
							Text(
									text = "Today" ,
									style = MaterialTheme.typography.titleSmall,
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
						style = MaterialTheme.typography.titleSmall
					)
				Text(
						text = hijrahDate.value.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
						style = MaterialTheme.typography.titleSmall
					)
			}
			IconButton(onClick = {
				date.value = date.value.plusDays(1)
				handleEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(date.value.toString()))
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

//Prayer tracker list
@Composable
fun PrayerTrackerList(
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
	stateOfTrackerForToday : TrackerViewModel.TrackerState ,
	dateMutable : MutableState<LocalDate> ,
	fajrState : Boolean ,
	zuharState : Boolean ,
	asrState : Boolean ,
	maghribState : Boolean ,
	ishaState : Boolean ,
					 )
{
	val context = LocalContext.current
	//a list of toggleable items
	val items = listOf("Fajr" , "Dhuhr" , "Asr" , "Maghrib" , "Isha")

	val state = stateOfTrackerForToday as TrackerViewModel.TrackerState

	//a list of booleans to keep track of the state of the toggleable items
	val fajrChecked = remember { mutableStateOf(false) }
	val zuhrChecked = remember { mutableStateOf(false) }
	val asrChecked = remember { mutableStateOf(false) }
	val maghribChecked = remember { mutableStateOf(false) }
	val ishaChecked = remember { mutableStateOf(false) }

		when (state)
		{
			is TrackerViewModel.TrackerState.Loading ->
			{
				Log.d("Tracker" , "Loading")
				PrayerTrackerListItems(
						items = items ,
						loading = true ,
						fajrChecked = fajrChecked ,
						zuhrChecked = zuhrChecked ,
						asrChecked = asrChecked ,
						maghribChecked = maghribChecked ,
						ishaChecked = ishaChecked ,
						dateMutable = dateMutable ,
						handleEvent = handleEvent,
									  )
			}
			is TrackerViewModel.TrackerState.Tracker ->
			{
				fajrChecked.value = fajrState
				zuhrChecked.value = zuharState
				asrChecked.value = asrState
				maghribChecked.value = maghribState
				ishaChecked.value = ishaState
				Log.d("Tracker" , "Loaded")
				PrayerTrackerListItems(
						items = items ,
						loading = false ,
						fajrChecked = fajrChecked ,
						zuhrChecked = zuhrChecked ,
						asrChecked = asrChecked ,
						maghribChecked = maghribChecked ,
						ishaChecked = ishaChecked ,
						dateMutable = dateMutable ,
						handleEvent = handleEvent ,
									  )
			}
			is TrackerViewModel.TrackerState.Error ->
			{
				Toasty.error(
						context,
						state.message ,
						Toast.LENGTH_SHORT ,
						true
							).show()
			}
		}
}

@Composable
fun PrayerTrackerListItems(
	items : List<String> ,
	loading : Boolean ,
	fajrChecked : MutableState<Boolean> ,
	zuhrChecked : MutableState<Boolean> ,
	asrChecked : MutableState<Boolean> ,
	maghribChecked : MutableState<Boolean> ,
	ishaChecked : MutableState<Boolean> ,
	dateMutable : MutableState<LocalDate> ,
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
						  )
{
	DateSelector(
			date = dateMutable ,
			handleEvent = handleEvent
				)
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(4.dp) ,
				) {
		items.forEachIndexed { index , item ->
			//if not the first item add a divider
			if (index != 0)
			{
				Divider(
						color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f) ,
						thickness = 1.dp
					   )
			}
			//the toggleable item
			ToggleableItem(
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp)
						.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									) ,
					text = item ,
					checked = when (item)
					{
						"Fajr" -> fajrChecked.value
						"Dhuhr" -> zuhrChecked.value
						"Asr" -> asrChecked.value
						"Maghrib" -> maghribChecked.value
						"Isha" -> ishaChecked.value
						else -> false
					} ,
					onCheckedChange = {
						when (item)
						{
							"Fajr" -> fajrChecked.value = it
							"Dhuhr" -> zuhrChecked.value = it
							"Asr" -> asrChecked.value = it
							"Maghrib" -> maghribChecked.value = it
							"Isha" -> ishaChecked.value = it
						}
						handleEvent(
								TrackerViewModel.TrackerEvent.UPDATE_TRACKER(
										PrayerTracker(
												fajr = fajrChecked.value ,
												dhuhr = zuhrChecked.value ,
												asr = asrChecked.value ,
												maghrib = maghribChecked.value ,
												isha = ishaChecked.value ,
												date = dateMutable.value.toString()
													 )
																			)
								   )
					}
						  )
		}
	}

}

@Composable
fun ToggleableItem(
	text : String ,
	checked : Boolean ,
	onCheckedChange : (Boolean) -> Unit ,
	modifier : Modifier ,
				  )
{
	Row(
			modifier = modifier ,
			verticalAlignment = Alignment.CenterVertically
	   ) {
		//a icon button to toggle the state of the toggleable item
		IconButton(
				modifier = Modifier.size(48.dp).border(
						1.dp ,
						MaterialTheme.colorScheme.primary ,
						CircleShape
													  ) ,
				onClick = {
			onCheckedChange(!checked)
		}) {
			if (!checked){
				Icon(
						painter = painterResource(id = R.drawable.cross_icon),
						contentDescription = "Close" ,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(24.dp)
						)
			}else{
				Icon(
						painter = painterResource(id = R.drawable.check_icon),
						contentDescription = "Check" ,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(24.dp)
						)
			}
		}


		Text(
				text = text ,
				modifier = Modifier.padding(start = 16.dp) ,
				style = MaterialTheme.typography.bodyLarge
			)
	}
}


@Preview
@Composable
fun PrayerTrackerNewPreview()
{
	PrayerTracker()
}
