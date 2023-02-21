package com.arshadshah.nimaz.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DashboardPrayertimesCard
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.RamadanCard
import com.arshadshah.nimaz.ui.screens.tracker.PrayerTracker
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.LocalDataStore
import java.time.LocalDate

@Composable
fun Dashboard() {

	val context = LocalContext.current
	LocalDataStore.init(context)
	val mutableDate = remember { mutableStateOf(LocalDate.now()) }

	val viewModelTracker = viewModel(initializer = { TrackerViewModel() }, viewModelStoreOwner = LocalContext.current as ComponentActivity)
	viewModelTracker.onEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(mutableDate.value.toString()))

	Column {
		DashboardPrayertimesCard()
		PrayerTracker(PaddingValues(top = 8.dp, bottom = 0.dp, start = 8.dp, end = 8.dp) , true)
		RamadanCard()
	}

}


@Preview
@Composable
fun DashboardPreview() {
	NimazTheme(
			darkTheme = true
			  ) {
		Dashboard()
	}
}