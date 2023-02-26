package com.arshadshah.nimaz.ui.screens.settings

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.FAJR_ANGLE
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsList
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsMenuLink
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsNumberPickerDialog
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.PrayerTimesParamMapper.getParams

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PrayerTimesCustomizations(paddingValues : PaddingValues)
{
	val context = LocalContext.current
	val sharedPreferences = PrivateSharedPreferences(context)

	val viewModel = viewModel(key = "PrayerTimesViewModel", initializer = { PrayerTimesViewModel() }, viewModelStoreOwner = LocalContext.current as ComponentActivity)
	val settingViewModel = viewModel(key = "SettingViewModel", initializer = { SettingsViewModel(context) }, viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)


	LaunchedEffect(Unit){
		settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.LoadSettings)
	}

	val mapOfMethods = AppConstants.getMethods()
	val mapOfMadhabs = AppConstants.getAsrJuristic()
	val mapOfHighLatitudeRules = AppConstants.getHighLatitudes()

	val calculationMethodState =
		rememberPreferenceStringSettingState(
				AppConstants.CALCULATION_METHOD ,
				"IRELAND" ,
				sharedPreferences
											)
	val madhabState =
		rememberPreferenceStringSettingState(AppConstants.MADHAB , "SHAFI" , sharedPreferences)
	val highLatitudeRuleState = rememberPreferenceStringSettingState(
			AppConstants.HIGH_LATITUDE_RULE ,
			"MIDDLE_OF_THE_NIGHT" ,
			sharedPreferences
																	)
	val fajrAngleState =
		rememberPreferenceStringSettingState(FAJR_ANGLE , "18" , sharedPreferences)
	val ishaAngleState =
		rememberPreferenceStringSettingState(AppConstants.ISHA_ANGLE , "18" , sharedPreferences)

	//isha interval
	val ishaIntervalState =
		rememberPreferenceStringSettingState(AppConstants.ISHA_INTERVAL , "0" , sharedPreferences)

	val fajrAdjustment =
		rememberPreferenceStringSettingState(AppConstants.FAJR_ADJUSTMENT , "0" , sharedPreferences)
	val sunriseAdjustment = rememberPreferenceStringSettingState(
			AppConstants.SUNRISE_ADJUSTMENT ,
			"0" ,
			sharedPreferences
																)
	val dhuhrAdjustment = rememberPreferenceStringSettingState(
			AppConstants.DHUHR_ADJUSTMENT ,
			"0" ,
			sharedPreferences
															  )
	val asrAdjustment =
		rememberPreferenceStringSettingState(AppConstants.ASR_ADJUSTMENT , "0" , sharedPreferences)
	val maghribAdjustment = rememberPreferenceStringSettingState(
			AppConstants.MAGHRIB_ADJUSTMENT ,
			"0" ,
			sharedPreferences
																)
	val ishaAdjustment =
		rememberPreferenceStringSettingState(AppConstants.ISHA_ADJUSTMENT , "0" , sharedPreferences)


	val ishaaAngleVisible = remember {
		settingViewModel.ishaAngleVisibility
	}.collectAsState()

	val ishaInterval = remember {
		settingViewModel.ishaInterval
	}.collectAsState()

	val calculationMethod = remember {
		settingViewModel.calculationMethod
	}.collectAsState()

	val madhab = remember {
		settingViewModel.madhab
	}.collectAsState()

	val highLatitudeRule = remember {
		settingViewModel.highLatitude
	}.collectAsState()

	val fajrAngle = remember {
		settingViewModel.fajrAngle
	}.collectAsState()

	val ishaAngle = remember {
		settingViewModel.ishaAngle
	}.collectAsState()

	val fajrAdjustmentValue = remember {
		settingViewModel.fajrOffset
	}.collectAsState()

	val sunriseAdjustmentValue = remember {
		settingViewModel.sunriseOffset
	}.collectAsState()

	val dhuhrAdjustmentValue = remember {
		settingViewModel.dhuhrOffset
	}.collectAsState()

	val asrAdjustmentValue = remember {
		settingViewModel.asrOffset
	}.collectAsState()

	val maghribAdjustmentValue = remember {
		settingViewModel.maghribOffset
	}.collectAsState()

	val ishaAdjustmentValue = remember {
		settingViewModel.ishaOffset
	}.collectAsState()

	val isLoading = remember {
		settingViewModel.isLoading
	}.collectAsState()

	calculationMethodState.value = calculationMethod.value
	madhabState.value = madhab.value
	highLatitudeRuleState.value = highLatitudeRule.value
	fajrAngleState.value = fajrAngle.value
	ishaAngleState.value = ishaAngle.value
	ishaIntervalState.value = ishaInterval.value
	fajrAdjustment.value = fajrAdjustmentValue.value
	sunriseAdjustment.value = sunriseAdjustmentValue.value
	dhuhrAdjustment.value = dhuhrAdjustmentValue.value
	asrAdjustment.value = asrAdjustmentValue.value
	maghribAdjustment.value = maghribAdjustmentValue.value
	ishaAdjustment.value = ishaAdjustmentValue.value

	Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState() , true)
				.padding(paddingValues)
				.testTag(AppConstants.TEST_TAG_PRAYER_TIMES_CUSTOMIZATION)
		  ) {
		SettingsGroup(title = {
			Text(text = "Prayer Parameters")
		}) {
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
						onChange = { method: String ->
							settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.CalculationMethod(method))
							settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.UpdateSettings(method))
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
						} ,
						height = 500.dp
							)
			}
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsList(
						valueState = madhabState ,
						title = {
							Text(text = "Madhab")
						} ,
						description = {
							Text(text = "The madhab used to calculate the asr prayer times.")
						} ,
						items = mapOfMadhabs ,
						subtitle = {
							Text(text = madhabState.value)
						} ,
						height = 300.dp
							) { madhab : String ->
					settingViewModel.handleEvent(
							SettingsViewModel.SettingsEvent.Madhab(
									madhab
																			 )
												)
					viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
					viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
				}
			}
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsList(
						valueState = highLatitudeRuleState ,
						title = {
							Text(text = "High Latitude Rule")
						} ,
						description = {
							Text(text = "The high latitude rule used to calculate the prayer times.")
						} ,
						items = mapOfHighLatitudeRules ,
						subtitle = {
							Text(text = highLatitudeRuleState.value)
						} ,
						height = 350.dp
							) { highLatRule : String ->
					settingViewModel.handleEvent(
							SettingsViewModel.SettingsEvent.HighLatitude(
									highLatRule
																			 )
												)
					viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
					viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
				}
			}
		}

		SettingsGroup(title = {
			Text(text = "Prayer Angles")
		}) {

			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsNumberPickerDialog(
						title = {
							Text(text = "Fajr Angle")
						} ,
						subtitle = {
							Text(text = fajrAngleState.value)
						} ,
						description = {
							Text(text = "The angle of the sun at which the Fajr prayer begins")
						} ,
						items = (0 .. 50).map { (it - 25) } ,
						valueState = fajrAngleState ,
										  ) { angle : Int ->
					settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.FajrAngle(angle.toString()))
					viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
					viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
				}
			}
			if (ishaaAngleVisible.value)
			{
				ElevatedCard(
						modifier = Modifier
							.padding(8.dp)
							.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
							.fillMaxWidth()
							) {
					SettingsNumberPickerDialog(
							title = {
								Text(text = "Isha Angle")
							} ,
							description = {
								Text(text = "The angle of the sun at which the Isha prayer begins")
							} ,
							items = (0 .. 50).map { (it - 25) } ,
							subtitle = {
								Text(text = ishaAngleState.value)
							} ,
							valueState = ishaAngleState ,
											  ) { angle : Int ->
						settingViewModel.handleEvent(
								SettingsViewModel.SettingsEvent.IshaAngle(
										angle.toString()
																													)
													)
						viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
						viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
					}
				}
			} else
			{
				ElevatedCard(
						modifier = Modifier
							.padding(8.dp)
							.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
							.fillMaxWidth()
							) {
					SettingsMenuLink(
							modifier = Modifier.padding(8.dp) ,
							title = { Text(text = "Isha Interval of ${ishaIntervalState.value} Minutes") } ,
							subtitle = { Text(text = "The interval of time after Maghrib at which the Isha prayer begins") } ,
							onClick = { } ,
									)
				}
			}
		}



		SettingsGroup(
				title = {
					Text(text = "Prayer Time")
				} ,
				content = {
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								icon = {
									Image(
											modifier = Modifier
												.size(48.dp),
											painter = painterResource(id = R.drawable.fajr_icon) ,
										  contentDescription = "Fajr Time"
										)
								},
								title = {
									Text(text = "Fajr Time")
								} ,
								description = {
									Text(text = "Adjust the time of the Fajr prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								subtitle = {
									Text(text = fajrAdjustment.value)
								} ,
								valueState = fajrAdjustment ,
												  ) { adjustment : Int ->
							settingViewModel.handleEvent(
									SettingsViewModel.SettingsEvent.FajrOffset(
											adjustment.toString()
																														)
														)
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
						}
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								icon = {
									Image(
											modifier = Modifier
												.size(48.dp),
											painter = painterResource(id = R.drawable.sunrise_icon) ,
											contentDescription = "Fajr Time"
										 )
								},
								title = {
									Text(text = "Sunrise Time")
								} ,
								description = {
									Text(text = "Adjust the time of the Sunrise prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								subtitle = {
									Text(text = sunriseAdjustment.value)
								} ,
								valueState = sunriseAdjustment ,
												  ) { adjustment : Int ->
							settingViewModel.handleEvent(
									SettingsViewModel.SettingsEvent.SunriseOffset(
											adjustment.toString()
																														)
														)
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
						}
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								icon = {
									Image(
											modifier = Modifier
												.size(48.dp),
											painter = painterResource(id = R.drawable.dhuhr_icon) ,
											contentDescription = "Dhuhr Time"
										 )
								},
								title = {
									Text(text = "Dhuhr Time")
								} ,
								description = {
									Text(text = "Adjust the time of the Dhuhr prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								subtitle = {
									Text(text = dhuhrAdjustment.value)
								} ,
								valueState = dhuhrAdjustment ,
												  ) { adjustment : Int ->
							settingViewModel.handleEvent(
									SettingsViewModel.SettingsEvent.DhuhrOffset(
											adjustment.toString()
																														)
														)
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
						}
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								icon = {
									Image(
											modifier = Modifier
												.size(48.dp),
											painter = painterResource(id = R.drawable.asr_icon) ,
											contentDescription = "Asr Time"
										 )
								},
								title = {
									Text(text = "Asr Time")
								} ,
								description = {
									Text(text = "Adjust the time of the Asr prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								subtitle = {
									Text(text = asrAdjustment.value)
								} ,
								valueState = asrAdjustment ,
												  ) { adjustment : Int ->
							settingViewModel.handleEvent(
									SettingsViewModel.SettingsEvent.AsrOffset(
											adjustment.toString()
																														)
														)
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
						}
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								icon = {
									Image(
											modifier = Modifier
												.size(48.dp),
											painter = painterResource(id = R.drawable.maghrib_icon) ,
											contentDescription = "Maghrib Time"
										 )
								},
								title = {
									Text(text = "Maghrib Time")
								} ,
								description = {
									Text(text = "Adjust the time of the Maghrib prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								subtitle = {
									Text(text = maghribAdjustment.value)
								} ,
								valueState = maghribAdjustment ,
												  ) { adjustment : Int ->
							settingViewModel.handleEvent(
									SettingsViewModel.SettingsEvent.MaghribOffset(
											adjustment.toString()
																														)
														)
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
						}
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								icon = {
									Image(
											modifier = Modifier
												.size(48.dp),
											painter = painterResource(id = R.drawable.isha_icon) ,
											contentDescription = "Isha Time"
										 )
								},
								title = {
									Text(text = "Isha Time")
								} ,
								description = {
									Text(text = "Adjust the time of the Isha prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								subtitle = {
									Text(text = ishaAdjustment.value)
								} ,
								valueState = ishaAdjustment ,
												  ) { adjustment : Int ->
							settingViewModel.handleEvent(
									SettingsViewModel.SettingsEvent.IshaOffset(
											adjustment.toString()
																														)
														)
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(getParams(context)))
							viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(context))
						}
					}
				}
					 )
	}
}
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview()
{
	NimazTheme {
		PrayerTimesCustomizations(paddingValues = PaddingValues(16.dp))
	}
}