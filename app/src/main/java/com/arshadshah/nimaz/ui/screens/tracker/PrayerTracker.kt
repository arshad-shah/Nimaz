package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_PRAYER_TRACKER
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import com.arshadshah.nimaz.ui.components.ui.trackers.FastTrackerCard
import com.arshadshah.nimaz.ui.components.ui.trackers.PrayerTrackerListItems
import es.dmoral.toasty.Toasty


@Composable
fun PrayerTracker(paddingValues : PaddingValues , isIntegrated : Boolean = false)
{
	val viewModel = viewModel(
			key = TRACKING_VIEWMODEL_KEY ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
							 )

	val dateState = remember {
		viewModel.dateState
	}.collectAsState()

	LaunchedEffect(key1 = "getTrackerForDate") {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.SHOW_DATE_SELECTOR(! isIntegrated))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(dateState.value))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(dateState.value))
	}

	val stateOfTrackerForToday = remember {
		viewModel.trackerState
	}.collectAsState()

	val showDateSelector = remember {
		viewModel.showDateSelector
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

	val progressState = remember {
		viewModel.progressState
	}.collectAsState()

	val isFasting = remember {
		viewModel.isFasting
	}.collectAsState()

	val fastingState = remember {
		viewModel.fastTrackerState
	}.collectAsState()

	Column(
			modifier = Modifier
				.padding(paddingValues)
				.testTag(TEST_TAG_PRAYER_TRACKER) ,
			horizontalAlignment = Alignment.CenterHorizontally
		  ) {

		ElevatedCard(
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = Modifier.padding(
						top = 4.dp ,
						bottom = 8.dp ,
						start = 0.dp ,
						end = 0.dp
										   ) ,
					) {
			Column {
				PrayerTrackerList(
						viewModel::onEvent ,
						stateOfTrackerForToday.value ,
						fajrState.value ,
						zuhrState.value ,
						asrState.value ,
						maghribState.value ,
						ishaState.value ,
						showDateSelector ,
						dateState ,
						progressState
								 )
				Fasting(
						viewModel::onEvent ,
						dateState ,
						isFasting.value ,
						fastingState.value
					   )
			}
		}
	}
}

@Composable
fun Fasting(
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
	dateState : State<String> ,
	isFasting : Boolean ,
	fastingState : TrackerViewModel.FastTrackerState ,
		   )
{

	val state = fastingState
	val isFastingToday = remember { mutableStateOf(false) }
	when (state)
	{
		is TrackerViewModel.FastTrackerState.Loading ->
		{
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(start = 6.dp , end = 6.dp , top = 4.dp , bottom = 4.dp) ,
					horizontalAlignment = Alignment.CenterHorizontally ,
					verticalArrangement = Arrangement.Center
				  ) {
				CircularProgressIndicator()
			}
		}

		is TrackerViewModel.FastTrackerState.Tracker ->
		{
			isFastingToday.value = isFasting
			FastTrackerCard(
					handleEvent = handleEvent ,
					dateState = dateState ,
					isFastingToday = isFastingToday
						   )

		}

		is TrackerViewModel.FastTrackerState.Error ->
		{
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(start = 6.dp , end = 6.dp , top = 4.dp , bottom = 4.dp) ,
					horizontalAlignment = Alignment.CenterHorizontally ,
					verticalArrangement = Arrangement.Center
				  ) {
				Text(text = "Error" , style = MaterialTheme.typography.titleMedium)
			}
		}

	}
}

//Prayer tracker list
@Composable
fun PrayerTrackerList(
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
	stateOfTrackerForToday : TrackerViewModel.TrackerState ,
	fajrState : Boolean ,
	zuharState : Boolean ,
	asrState : Boolean ,
	maghribState : Boolean ,
	ishaState : Boolean ,
	showDateSelector : State<Boolean> ,
	dateState : State<String> ,
	progressState : State<Int> ,
					 )
{
	val context = LocalContext.current
	//a list of toggleable items
	val items = listOf("Fajr" , "Dhuhr" , "Asr" , "Maghrib" , "Isha")

	val state = stateOfTrackerForToday

	//a list of booleans to keep track of the state of the toggleable items
	val fajrChecked = remember { mutableStateOf(false) }
	val zuhrChecked = remember { mutableStateOf(false) }
	val asrChecked = remember { mutableStateOf(false) }
	val maghribChecked = remember { mutableStateOf(false) }
	val ishaChecked = remember { mutableStateOf(false) }
	val progress = remember { mutableStateOf(0f) }

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
					handleEvent = handleEvent ,
					showDateSelector = showDateSelector ,
					dateState = dateState ,
					progress = progress
								  )
		}

		is TrackerViewModel.TrackerState.Tracker ->
		{
			fajrChecked.value = fajrState
			zuhrChecked.value = zuharState
			asrChecked.value = asrState
			maghribChecked.value = maghribState
			ishaChecked.value = ishaState
			progress.value = progressState.value.toFloat()
			Log.d("Tracker" , "Loaded")
			PrayerTrackerListItems(
					items = items ,
					loading = false ,
					fajrChecked = fajrChecked ,
					zuhrChecked = zuhrChecked ,
					asrChecked = asrChecked ,
					maghribChecked = maghribChecked ,
					ishaChecked = ishaChecked ,
					handleEvent = handleEvent ,
					showDateSelector = showDateSelector ,
					dateState = dateState ,
					progress = progress
								  )
		}

		is TrackerViewModel.TrackerState.Error ->
		{
			Toasty.error(
					context ,
					state.message ,
					Toast.LENGTH_SHORT ,
					true
						).show()
		}

		else ->
		{
		}
	}
}