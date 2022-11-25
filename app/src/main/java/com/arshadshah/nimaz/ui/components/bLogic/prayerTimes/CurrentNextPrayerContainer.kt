package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.CountTimeViewModel
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.CurrentNextPrayerContainerUI
import java.time.LocalDateTime

@Composable
fun CurrentNextPrayerContainer(state: State<PrayerTimesViewModel.PrayerTimesListState>) {

    val context = LocalContext.current

    when (val prayerTimesListState = state.value) {
        is PrayerTimesViewModel.PrayerTimesListState.Loading -> {

        }
        is PrayerTimesViewModel.PrayerTimesListState.Success -> {
            val timeToNextPrayerLong = prayerTimesListState.prayerTimes?.nextPrayer?.time?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            val currentTime = LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val difference = timeToNextPrayerLong?.minus(currentTime)
            val  countDownTimerViewModel = CountTimeViewModel()
            difference?.let { countDownTimerViewModel.startTimer(context, it) }
            val timeToNextPrayerString = countDownTimerViewModel.timer
            prayerTimesListState.prayerTimes?.currentPrayer?.let { CurrentNextPrayerContainerUI(it.name, state = timeToNextPrayerString) }
        }
        is PrayerTimesViewModel.PrayerTimesListState.Error -> {

        }
    }
}