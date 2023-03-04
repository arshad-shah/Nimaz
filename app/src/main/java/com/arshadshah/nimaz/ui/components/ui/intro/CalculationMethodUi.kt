package com.arshadshah.nimaz.ui.components.ui.intro

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.arshadshah.nimaz.utils.network.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.sunMoonUtils.SunMoonCalc
import kotlin.math.roundToInt

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

	//sun position
	val sunCalc = SunMoonCalc(latitude.value , longitude.value, context)
	val times = sunCalc.getTimes()
	val sunPositionAtFajr = sunCalc.getSunPositionForDate(times.nauticalDawn)
	val sunPositionAtIshaa = sunCalc.getSunPositionForDate(times.nauticalDusk)

	val altitudeInDegreesFajr = Math.toDegrees(sunPositionAtFajr.altitude).roundToInt()
	val altitudeInDegreesIshaa = Math.toDegrees(sunPositionAtIshaa.altitude).roundToInt()

	ElevatedCard {
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
									altitudeInDegreesFajr.toString()
																	 )
												)
					//set ishaa angle
					settingViewModel.handleEvent(
							SettingsViewModel.SettingsEvent.IshaAngle(
									altitudeInDegreesIshaa.toString()
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
				}
					  )
	}
	AnimatedVisibility(
			visible = !state.value,
			enter = expandVertically() ,
			exit = shrinkVertically()
					  ) {
		ElevatedCard(
				modifier = Modifier
					.padding(8.dp)
					.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
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
					} ,
					height = 500.dp
						)
		}
	}
}