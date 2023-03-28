package com.arshadshah.nimaz.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DatesContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.LocationTimeContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.PrayerTimesList
import com.arshadshah.nimaz.utils.network.PrayerTimesParamMapper

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PrayerTimesScreen(
	paddingValues : PaddingValues ,
	onNavigateToTracker : () -> Unit ,
					 )
{
	val context = LocalContext.current

	val viewModel = viewModel(
			key = "PrayerTimesViewModel" ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
							 )
	val settingViewModel = viewModel(
			key = "SettingViewModel" ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
									)

	//reload the data when the screen is resumed
	LaunchedEffect(Unit) {
		settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.LoadLocation(context))
		viewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.RELOAD)
	}

	// Collecting the state of the view model
	val state by remember { viewModel.prayerTimesState }.collectAsState()
	val locationState = remember { settingViewModel.locationName }.collectAsState()
	val latitude = remember { settingViewModel.latitude }.collectAsState()
	val longitude = remember { settingViewModel.longitude }.collectAsState()

	val isLoading = remember {
		viewModel.isLoading
	}.collectAsState()

	val fajrTime = remember {
		viewModel.fajrTime
	}.collectAsState()

	val sunriseTime = remember {
		viewModel.sunriseTime
	}.collectAsState()

	val dhuhrTime = remember {
		viewModel.dhuhrTime
	}.collectAsState()

	val asrTime = remember {
		viewModel.asrTime
	}.collectAsState()

	val maghribTime = remember {
		viewModel.maghribTime
	}.collectAsState()

	val ishaTime = remember {
		viewModel.ishaTime
	}.collectAsState()
	LaunchedEffect(locationState.value, latitude.value, longitude.value) {
		viewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.SET_LOADING(true))
			//update the prayer times
			viewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
				PrayerTimesParamMapper.getParams(context)
																				  )
							 )
		viewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.SET_LOADING(false))
		viewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.REFRESH(false))
	}
	val currentPrayerName = remember {
		viewModel.currentPrayerName
	}.collectAsState()

	//log all the states
	Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG , "state: $state")
	Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG , "locationState: $locationState")
	Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG , "currentPrayerName: $currentPrayerName")

		LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(paddingValues)
					.padding(8.dp)
					.testTag(AppConstants.TEST_TAG_PRAYER_TIMES) ,
				horizontalAlignment = Alignment.CenterHorizontally ,
				verticalArrangement = Arrangement.SpaceEvenly
				  ) {
			item {
				// Calling the LocationTimeContainer composable
				LocationTimeContainer(
						currentPrayerName = currentPrayerName ,
						locationState = locationState ,
						handleEvent = settingViewModel::handleEvent ,
						isLoading = isLoading
									 )

				// Calling the DatesContainer composable
				DatesContainer(onNavigateToTracker = onNavigateToTracker)

				// Calling the PrayerTimesList composable
				PrayerTimesList()
			}
		}
}