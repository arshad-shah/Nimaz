package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel

@Composable
fun PrayerTimeCardToday(
	state : LiveData<CountDownTime> ,
	prayerTimesState : State<PrayerTimesViewModel.PrayerTimesState> ,
	viewModel : PrayerTimesViewModel
					   )
{
	CurrentNextPrayerContainer(state = prayerTimesState , timerState = state , viewModel = viewModel)
}