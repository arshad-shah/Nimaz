package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import com.arshadshah.nimaz.ui.screens.tracker.PrayerTrackerList

@Composable
fun DashboardPrayerTracker() {

	val viewModel = viewModel(key="TrackerViewModel",initializer = { TrackerViewModel() }, viewModelStoreOwner = LocalContext.current as ComponentActivity)

	val dateState = remember {
		viewModel.dateState
	}.collectAsState()

	LaunchedEffect(key1 = "getTrackerForDate") {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.SHOW_DATE_SELECTOR(false))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(dateState.value))
	}

	val stateOfTrackerForToday = remember {
		viewModel.trackerState
	}.collectAsState()

	val showDateSelector = remember {
		viewModel.showDateSelector
	}.collectAsState()

	val allTracker = remember {
		viewModel.allTrackers
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

	PrayerTrackerList(
			viewModel::onEvent ,
			stateOfTrackerForToday.value,
			fajrState.value,
			zuhrState.value,
			asrState.value,
			maghribState.value,
			ishaState.value,
			showDateSelector,
			dateState,
			progressState
					 )
}