package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.LocationTimeContainerUI

@Composable
fun LocationTimeContainer(
	state : State<PrayerTimesViewModel.LocationState> ,
						 )
{
	when (val locationState = state.value)
	{
		is PrayerTimesViewModel.LocationState.Loading ->
		{
			LocationTimeContainerUI(
					location = "Loading..." ,
								   )
		}

		is PrayerTimesViewModel.LocationState.Success ->
		{
			LocationTimeContainerUI(
					location = locationState.location ,
								   )
		}

		is PrayerTimesViewModel.LocationState.Error ->
		{
			LocationTimeContainerUI(
					location = "Error" ,
								   )
		}
	}
}