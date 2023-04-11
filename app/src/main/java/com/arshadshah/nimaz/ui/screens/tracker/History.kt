package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import com.arshadshah.nimaz.ui.components.ProgressBarCustom
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.LocalDataStore
import java.time.LocalDate

@Composable
fun History()
{
	val viewModel = viewModel(
			key = TRACKING_VIEWMODEL_KEY ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

	LaunchedEffect(key1 = "getTrackerForDate") {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_ALL_TRACKERS)
	}

	val allTracers = remember {
		viewModel.allTrackers
	}.collectAsState()

	//filter the data and get tracker for today
	val trackerForToday = allTracers.value.filter { it.date == LocalDate.now().toString() }
	Log.d("trackerForToday" , trackerForToday.toString())

	Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 6.dp , end = 6.dp , top = 4.dp , bottom = 4.dp) ,
			horizontalAlignment = Alignment.CenterHorizontally
		  ) {
		if (trackerForToday.isNotEmpty())
		{
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth()
						) {
				Row(
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth() ,
						horizontalArrangement = Arrangement.SpaceEvenly ,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					Column {
						Text(
								text = "Your Progress Today" ,
								style = MaterialTheme.typography.titleLarge
							)
						Text(
								text = "${getCompletedPrayers(trackerForToday[0])} of 5 Completed" ,
								style = MaterialTheme.typography.titleSmall
							)
					}
					ProgressBarCustom(
							progress = trackerForToday[0].progress.toFloat() ,
							radius = 50.dp ,
									 )
				}
			}
		}
	}
}

//function to get completed prayers from a tracker
fun getCompletedPrayers(tracker : PrayerTracker) : Int
{
	var completedPrayers = 0
	if (tracker.fajr) completedPrayers ++
	if (tracker.dhuhr) completedPrayers ++
	if (tracker.asr) completedPrayers ++
	if (tracker.maghrib) completedPrayers ++
	if (tracker.isha) completedPrayers ++
	return completedPrayers
}

//preview
@Preview
@Composable
fun HistoryPreview()
{
	LocalDataStore.init(LocalContext.current)
	NimazTheme {
		History()
	}
}