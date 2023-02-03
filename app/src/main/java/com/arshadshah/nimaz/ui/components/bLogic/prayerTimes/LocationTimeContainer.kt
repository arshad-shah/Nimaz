package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.LocationTimeContainerUI
import es.dmoral.toasty.Toasty

@Composable
fun LocationTimeContainer(
	state : State<PrayerTimesViewModel.LocationState> ,
	prayerTimesState : State<PrayerTimesViewModel.PrayerTimesState> ,
						 )
{
	when (val locationState = state.value)
	{
		is PrayerTimesViewModel.LocationState.Loading ->
		{
			LocationTimeContainerUI(
					location = "Loading..." ,
					prayerTimesState = prayerTimesState ,
								   )
		}

		is PrayerTimesViewModel.LocationState.Success ->
		{
			LocationTimeContainerUI(
					location = locationState.location ,
					prayerTimesState = prayerTimesState ,
								   )
		}

		is PrayerTimesViewModel.LocationState.Error ->
		{
			LocationTimeContainerUI(
					location = "Error" ,
					prayerTimesState = prayerTimesState ,
								   )
			Toasty.error(LocalContext.current , locationState.errorMessage).show()
		}
	}
}