package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.PrayerTimeCardToday

@Composable
fun TodayScreen(paddingValues : PaddingValues)
{
	val context = LocalContext.current

	// Initalising the view model
	val viewModel = PrayerTimesViewModel(context)

	val timerState = viewModel.timer
	val prayerTimesState = remember { viewModel.prayerTimesState }.collectAsState()


	Column(modifier = Modifier.padding(paddingValues)) {
		Text(text = "Today")
		PrayerTimeCardToday(state = timerState,prayerTimesState = prayerTimesState,viewModel = viewModel)
	}
}