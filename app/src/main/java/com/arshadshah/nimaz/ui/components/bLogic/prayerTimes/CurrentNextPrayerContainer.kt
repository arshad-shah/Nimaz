package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.CurrentNextPrayerContainerUI
import java.time.LocalDateTime

@Composable
fun CurrentNextPrayerContainer(
	state : State<PrayerTimesViewModel.PrayerTimesState> ,
	timerState : LiveData<CountDownTime> ,
	viewModel : PrayerTimesViewModel ,
							  )
{

	val context = LocalContext.current

	when (val prayerTimesListState = state.value)
	{
		is PrayerTimesViewModel.PrayerTimesState.Success ->
		{
			val prayerTimes = prayerTimesListState.prayerTimes
			val timeToNextPrayerLong =
				prayerTimes?.nextPrayer?.time?.atZone(java.time.ZoneId.systemDefault())?.toInstant()
					?.toEpochMilli()
			val currentTime =
				LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
					.toEpochMilli()
			val difference = timeToNextPrayerLong?.minus(currentTime)
			viewModel.startTimer(context , difference !!)
			CurrentNextPrayerContainerUI(prayerTimes.nextPrayer.name , timerState)
		}

		is PrayerTimesViewModel.PrayerTimesState.Error ->
		{
			CurrentNextPrayerContainerUI(nextPrayerName = "Error" , timerState)
		}

		is PrayerTimesViewModel.PrayerTimesState.Loading ->
		{
			CurrentNextPrayerContainerUI("Loading..." , timerState)
		}
	}
}