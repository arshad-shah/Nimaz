package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.CurrentNextPrayerContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DatesContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.LocationTimeContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.PrayerTimesList

@Composable
fun PrayerTimesScreen()
{
	Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(8.dp)
				.wrapContentSize(Alignment.Center) ,
		  ) {
		val context = LocalContext.current

		val viewModel = PrayerTimesViewModel(context)

		val state = viewModel.prayerTimesState.collectAsState()


		val locationState = viewModel.location.collectAsState()

		val timerState = viewModel.timer


		LocationTimeContainer(state = locationState)
		DatesContainer()
		CurrentNextPrayerContainer(state = state , timerState = timerState , viewModel = viewModel)
		PrayerTimesList(state = state)
	}
}