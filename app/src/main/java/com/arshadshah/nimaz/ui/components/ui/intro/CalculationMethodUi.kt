package com.arshadshah.nimaz.ui.components.ui.intro

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.AUTO_PARAMETERS
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsList
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsSwitch
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.sunMoonUtils.AutoAnglesCalc

@Composable
fun CalculationMethodUI()
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "PrayerTimesViewModel" ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = context as ComponentActivity
							 )
	val settingViewModel = viewModel(
			key = "SettingViewModel" ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
									)
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
	val autoParams = remember{
		settingViewModel.autoParams
	}.collectAsState()
	val state =
		rememberPreferenceBooleanSettingState(
				AUTO_PARAMETERS,
				false
											 )
	state.value = autoParams.value

	val mapOfMethods = AppConstants.getMethods()
	val calculationMethodState =
		rememberPreferenceStringSettingState(
				AppConstants.CALCULATION_METHOD ,
				"MWL"
											)
	val latitude = remember{
		settingViewModel.latitude
	}.collectAsState()
	val longitude = remember{
		settingViewModel.longitude
	}.collectAsState()

	val sharedPreferences = remember{
		settingViewModel.sharedPreferences
	}

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth()
				) {
		SettingsSwitch(
				state = state ,
				title = {
					if (state.value)
					{
						Text(text = "Auto Calculation")
					}
					else
					{
						Text(text = "Manual Calculation")
					}
				},
				onCheckedChange = {
					if (it){
						settingViewModel.handleEvent(
								SettingsViewModel.SettingsEvent.AutoParameters(
										true
																			  )
													)
					}else{
						settingViewModel.handleEvent(
								SettingsViewModel.SettingsEvent.AutoParameters(
										false
																			  )
													)
					}

					//set method to other
					settingViewModel.handleEvent(
							SettingsViewModel.SettingsEvent.CalculationMethod(
									"OTHER"
																			 )
												)
					//set fajr angle
					settingViewModel.handleEvent(
							SettingsViewModel.SettingsEvent.FajrAngle(
									AutoAnglesCalc().calculateFajrAngle(context, latitude.value, longitude.value).toString()
																	 )
												)
					//set ishaa angle
					settingViewModel.handleEvent(
							SettingsViewModel.SettingsEvent.IshaAngle(
									AutoAnglesCalc().calculateIshaaAngle(context, latitude.value, longitude.value).toString()
																	  )
												)
					//set high latitude method
					settingViewModel.handleEvent(
							SettingsViewModel.SettingsEvent.HighLatitude(
									"TWILIGHT_ANGLE"
																			  )
												)
					viewModel.handleEvent(
							context ,
							PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
									PrayerTimesParamMapper.getParams(context)
																					)
										 )
					viewModel.handleEvent(
							context ,
							PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
									context
																			   )
										 )
					PrivateSharedPreferences(context).saveDataBoolean(AppConstants.ALARM_LOCK , false)
				}
					  )
	}
	AnimatedVisibility(
			visible = !state.value,
			enter = expandVertically() ,
			exit = shrinkVertically()
					  ) {
		ElevatedCard(
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					) {
			SettingsList(
					title = {
						Text(text = "Calculation Method")
					} ,
					subtitle = {
						Text(text = calculationMethodState.value)
					} ,
					description = {
						Text(text = "The method used to calculate the prayer times.")
					} ,
					items = mapOfMethods ,
					valueState = calculationMethodState ,
					onChange = { method : String ->
						settingViewModel.handleEvent(
								SettingsViewModel.SettingsEvent.CalculationMethod(
										method
																				 )
													)
						settingViewModel.handleEvent(
								SettingsViewModel.SettingsEvent.UpdateSettings(
										method
																			  )
													)
						viewModel.handleEvent(
								context ,
								PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
										PrayerTimesParamMapper.getParams(context)
																						)
											 )
						viewModel.handleEvent(
								context ,
								PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
										context
																				   )
											 )
						PrivateSharedPreferences(context).saveDataBoolean(AppConstants.ALARM_LOCK , false)
					} ,
					height = 500.dp
						)
		}
	}
}