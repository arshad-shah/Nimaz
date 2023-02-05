package com.arshadshah.nimaz.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DatesContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.LocationTimeContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.PrayerTimesList
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import es.dmoral.toasty.Toasty

@Composable
fun PrayerTimesScreen(
	paddingValues : PaddingValues ,
					 )
{
	val context = LocalContext.current

	// Initalising the view model
	val viewModel = PrayerTimesViewModel(context)


	// Collecting the state of the view model
	val state = remember { viewModel.prayerTimesState }.collectAsState()
	val timer = viewModel.timer

	//reload the data when the screen is resumed
	LaunchedEffect(Unit) {
		viewModel.reload(context)
	}
	//log all the states
	Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG , "state: ${state.value}")

	//if its not loaded yet, show a loading screen
	if (state.value.isLoading.value || state.value.prayerTimes.value == null)
	{
		CircularLoaderCard()
	} else if (state.value.error.value != null)
	{
		//if there is an error, show an error screen
		Toasty.error(context , state.value.error.value !!).show()
	} else
	{
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
					location = state.value.location ,
					currentTimeName = state.value.prayerTimes.value?.currentPrayer?.name
								 )

			// Calling the DatesContainer composable
			DatesContainer()

			// Calling the PrayerTimesList composable
			PrayerTimesList(
					prayerTimes = state.value.prayerTimes ,
					timerState = timer ,
					viewModel = viewModel ,
					paddingValues = paddingValues ,
						   )
		}
	}
}