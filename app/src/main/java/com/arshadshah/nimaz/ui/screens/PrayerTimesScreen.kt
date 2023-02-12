package com.arshadshah.nimaz.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DatesContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.LocationTimeContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.PrayerTimesList

@Composable
fun PrayerTimesScreen(
	paddingValues : PaddingValues ,
	viewModel : PrayerTimesViewModel = PrayerTimesViewModel() ,
					 )
{
	val context = LocalContext.current

	// Collecting the state of the view model
	val state by remember { viewModel.prayerTimesState }.collectAsState()
	val locationState by remember { viewModel.locationState }.collectAsState()
	val timer = viewModel.timer

	val currentPrayerName = remember {
		mutableStateOf("Loading...")
	}

	//reload the data when the screen is resumed
	LaunchedEffect(Unit) {
		viewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.RELOAD)
	}

	//log all the states
	Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG , "state: $state")
	Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.padding(8.dp) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.SpaceEvenly
		  ) {
		// Calling the LocationTimeContainer composable
		LocationTimeContainer(
				currentPrayerName = currentPrayerName ,
				locationState = locationState ,
							 )

		// Calling the DatesContainer composable
		DatesContainer()

		// Calling the PrayerTimesList composable
		PrayerTimesList(
				state = state ,
				timer = timer ,
				handleEvent = viewModel::handleEvent ,
				currentPrayerName = currentPrayerName ,
					   )
	}
}