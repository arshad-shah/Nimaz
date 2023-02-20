package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.utils.network.PrayerTimesParamMapper


@Composable
fun LocationSettings(){

	val context = LocalContext.current
	val viewModel = viewModel(key = "SettingsViewModel", initializer = { SettingsViewModel(context) }, viewModelStoreOwner = context as ComponentActivity)
	val viewModelPrayerTimes = viewModel(key = "PrayerTimesViewModel", initializer = { PrayerTimesViewModel() }, viewModelStoreOwner = LocalContext.current as ComponentActivity)
	val isLocationAuto = remember {
		viewModel.isLocationAuto
	}.collectAsState()

	val locationNameState = remember {
		viewModel.locationName
	}.collectAsState()

	val latitudeState = remember {
		viewModel.latitude
	}.collectAsState()

	val longitudeState = remember {
		viewModel.longitude
	}.collectAsState()

	val isLocationNameLoading = remember {
		viewModel.isLocationNameLoading
	}.collectAsState()

	//if the changes are made to the location settings, then the prayer times are updated
	//					viewModelPrayerTimes.handleEvent(
	//							context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
	//							PrayerTimesParamMapper.getParams(context)
	//																							  )
	//													)
	LaunchedEffect(key1 = locationNameState.value , key2 = latitudeState.value , key3 = longitudeState.value) {
		viewModelPrayerTimes.handleEvent(
				context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
				PrayerTimesParamMapper.getParams(context)
																	  )
										)
	}

	val storage =
		rememberPreferenceBooleanSettingState(AppConstants.LOCATION_TYPE , true)
	storage.value = isLocationAuto.value

	SettingsGroup(title = { Text(text = "Location") }) {
		ElevatedCard(
				modifier = Modifier
					.padding(8.dp)
					.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
					.fillMaxWidth()
					) {
			SettingsSwitch(
					state = storage ,
					icon = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.marker_icon) ,
								contentDescription = "Location"
							)
					} ,
					title = {
						if (storage.value)
						{
							Text(text = "Automatic")
						} else
						{
							Text(text = "Manual")
						}
					} ,
					subtitle = {
						if (storage.value)
						{
							if(isLocationNameLoading.value)
							{
								Text(text = "Loading...")
							}else{
								Text(text = locationNameState.value)
							}
						}
					},
					onCheckedChange = {
						storage.value = it
						viewModel.handleEvent(
								SettingsViewModel.SettingsEvent.LocationToggle(context, it)
											 )
					}
						  )
		}
		if (! storage.value)
		{
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				ManualLocationInput(
						handleSettingEvents = viewModel::handleEvent ,
						locationNameState = locationNameState ,
								   )
			}
			CoordinatesView(
					longitudeState = longitudeState ,
					latitudeState = latitudeState ,
						   )
		}
	}
}