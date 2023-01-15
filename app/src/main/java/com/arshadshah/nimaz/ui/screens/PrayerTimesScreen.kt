package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DatesContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.FeatureCard
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.LocationTimeContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.PrayerTimesList

@Composable
fun PrayerTimesScreen(paddingValues : PaddingValues)
{
	val context = LocalContext.current

	// Initalising the view model
	val viewModel = PrayerTimesViewModel(context)

	// Collecting the state of the view model
	val state = remember { viewModel.prayerTimesState }.collectAsState()

	val locationState = remember { viewModel.location }.collectAsState()

	val timerState = viewModel.timer


	Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.padding(8.dp) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.SpaceEvenly
		  ) {
		// Calling the LocationTimeContainer composable
		LocationTimeContainer(state = locationState)

		// Calling the DatesContainer composable
		DatesContainer()

		// Calling the PrayerTimesList composable
		PrayerTimesList(
				state = state ,
				timerState = timerState ,
				viewModel = viewModel ,
				paddingValues = paddingValues
					   )

		FeatureCard()
	}
}

@Preview
@Composable
fun PrayerTimesScreenPreview()
{
	PrayerTimesScreen(paddingValues = PaddingValues(0.dp))
}