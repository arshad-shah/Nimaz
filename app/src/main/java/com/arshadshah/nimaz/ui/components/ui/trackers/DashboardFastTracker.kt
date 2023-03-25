package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import java.time.LocalDate

@Composable
fun DashboardFastTracker()
{
	val mutableDate = remember { mutableStateOf(LocalDate.now()) }

	val viewModelTracker = viewModel(
			key = "TrackerViewModel" ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
									)
	LaunchedEffect(key1 = "getTrackerForDate") {
		viewModelTracker.onEvent(TrackerViewModel.TrackerEvent.SHOW_DATE_SELECTOR(false))
		viewModelTracker.onEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(mutableDate.value.toString()))
	}
	val dateState = remember {
		viewModelTracker.dateState
	}.collectAsState()
	val isFasting = remember {
		viewModelTracker.isFasting
	}.collectAsState()

	val isFastingToday = remember { mutableStateOf(false) }

	isFastingToday.value = isFasting.value
	FastTrackerCard(
			dateState = dateState ,
			isFastingToday = isFastingToday ,
			handleEvent = viewModelTracker::onEvent
				   )
}