package com.arshadshah.nimaz.ui.screens.tracker

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_CALENDER
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.ui.components.calender.Calender
import com.arshadshah.nimaz.ui.components.dashboard.DashboardFastTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayerTracker
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalenderScreen(paddingValues : PaddingValues)
{

	val mutableDate = remember { mutableStateOf(LocalDate.now()) }

	val viewModel = viewModel(
			key = TRACKING_VIEWMODEL_KEY ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	//call this effect only once
	LaunchedEffect(Unit) {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(mutableDate.value.toString()))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(mutableDate.value.toString()))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(mutableDate.value.toString()))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(mutableDate.value.toString()))
	}

	val dateState = remember {
		viewModel.dateState
	}.collectAsState()

	val progressForMonth = remember {
		viewModel.progressForMonth
	}.collectAsState()

	val fastProgressForMonth = remember {
		viewModel.fastProgressForMonth
	}.collectAsState()

	LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.testTag(TEST_TAG_CALENDER) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Top
			  ) {
		item {
			Calender(
					handleEvents = viewModel::onEvent ,
					progressForMonth = progressForMonth ,
					fastProgressForMonth = fastProgressForMonth
					)
		}
		item {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(top = 8.dp)
						.fillMaxWidth()
						) {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									start =
									24.dp , end = 24.dp , top = 12.dp , bottom = 8.dp
									) ,
						horizontalArrangement = Arrangement.SpaceBetween ,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					Text(
							text = "Trackers" , style = MaterialTheme.typography.titleMedium
						)
					Text(
							text = LocalDate.parse(dateState.value)
								.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
							style = MaterialTheme.typography.titleMedium
						)
				}
				DashboardPrayerTracker()

				DashboardFastTracker()
			}
		}
	}
}