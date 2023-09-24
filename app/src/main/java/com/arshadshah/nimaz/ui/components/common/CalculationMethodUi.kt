package com.arshadshah.nimaz.ui.components.common

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.AUTO_PARAMETERS
import com.arshadshah.nimaz.ui.components.settings.SettingsList
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.api.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.sunMoonUtils.AutoAnglesCalc
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel

@Composable
fun CalculationMethodUI()
{
	val context = LocalContext.current
	val viewModel = viewModel(
			 key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY ,
			 initializer = { PrayerTimesViewModel() } ,
			 viewModelStoreOwner = context as ComponentActivity
							 )
	val settingViewModel = viewModel(
			 key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
			 initializer = { SettingsViewModel(context) } ,
			 viewModelStoreOwner = context
									)
	val autoParams = remember {
		settingViewModel.autoParams
	}.collectAsState()
	val state =
		rememberPreferenceBooleanSettingState(
				 AUTO_PARAMETERS ,
				 false
											 )
	state.value = autoParams.value

	val mapOfMethods = AppConstants.getMethods()
	val calculationMethodState =
		rememberPreferenceStringSettingState(
				 AppConstants.CALCULATION_METHOD ,
				 "MWL"
											)
	val latitude = remember {
		settingViewModel.latitude
	}.collectAsState()
	val longitude = remember {
		settingViewModel.longitude
	}.collectAsState()
	ElevatedCard(
			 colors = CardDefaults.elevatedCardColors(
					  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 32.dp) ,
					  contentColor = MaterialTheme.colorScheme.onSurface ,
					  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
					  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
													 ) ,
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
					 } else
					 {
						 Text(text = "Manual Calculation")
					 }
				 } ,
				 subtitle = {
					 Text(text = "Auto angles are Experimental")
				 } ,
				 onCheckedChange = {
					 if (it)
					 {
						 settingViewModel.handleEvent(
								  SettingsViewModel.SettingsEvent.AutoParameters(
										   true
																				)
													 )
					 } else
					 {
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
									   AutoAnglesCalc().calculateFajrAngle(
												context ,
												latitude.value ,
												longitude.value
																		  ).toString()
																	   )
												 )
					 //set ishaa angle
					 settingViewModel.handleEvent(
							  SettingsViewModel.SettingsEvent.IshaAngle(
									   AutoAnglesCalc().calculateIshaaAngle(
												context ,
												latitude.value ,
												longitude.value
																		   ).toString()
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
					 PrivateSharedPreferences(context).saveDataBoolean(
							  AppConstants.ALARM_LOCK ,
							  false
																	  )
				 }
					  )
	}
	AnimatedVisibility(
			 visible = ! state.value ,
			 enter = expandVertically() ,
			 exit = shrinkVertically()
					  ) {
		ElevatedCard(
				 colors = CardDefaults.elevatedCardColors(
						  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
								   elevation = 32.dp
																							) ,
						  contentColor = MaterialTheme.colorScheme.onSurface ,
						  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
						  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
														 ) ,
				 shape = MaterialTheme.shapes.extraLarge ,
				 modifier = Modifier
					 .padding(8.dp)
					 .fillMaxWidth()
					) {
			SettingsList(
					 title = "Calculation Method" ,
					 subtitle = calculationMethodState.value ,
					 description = "The method used to calculate the prayer times." ,
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
						 PrivateSharedPreferences(context).saveDataBoolean(
								  AppConstants.ALARM_LOCK ,
								  false
																		  )
					 } ,
					 height = 300.dp
						)
		}
	}
}