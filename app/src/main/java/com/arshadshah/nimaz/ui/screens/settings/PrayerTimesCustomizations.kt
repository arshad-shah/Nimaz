package com.arshadshah.nimaz.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.FAJR_ANGLE
import com.arshadshah.nimaz.constants.AppConstants.getDefaultParametersForMethod
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsList
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsMenuLink
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsNumberPickerDialog
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import java.time.LocalDateTime

@Composable
fun PrayerTimesCustomizations(paddingValues : PaddingValues)
{
	val context = LocalContext.current
	val sharedPreferences = PrivateSharedPreferences(context)

	val mapOfMethods = AppConstants.getMethods()
	val mapOfMadhabs = AppConstants.getAsrJuristic()
	val mapOfHighLatitudeRules = AppConstants.getHighLatitudes()
	val defaultValuesForMethod = remember {
		mutableStateOf(getDefaultParametersForMethod("IRELAND"))
	}

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
		mutableStateOf(true)
	}

	//if the calculation method is MAKKAH, QATAR, or GULF then the Isha angle is not visible
	if (calculationMethodState.value == "MAKKAH" || calculationMethodState.value == "QATAR" || calculationMethodState.value == "GULF")
	{
		ishaAngleState.value = "0"
		ishaaAngleVisible.value = false
	}
	else
	{
		ishaaAngleVisible.value = true
	}

	//whenever the calculation method is changed, the default values for that method are loaded and the values are set to the state
	SideEffect {

	}
	LaunchedEffect(calculationMethodState.value)
	{
		defaultValuesForMethod.value = getDefaultParametersForMethod(calculationMethodState.value)
		fajrAngleState.value = defaultValuesForMethod.value["fajrAngle"].toString()
		ishaAngleState.value = defaultValuesForMethod.value["ishaAngle"].toString()
		madhabState.value = defaultValuesForMethod.value["madhab"].toString()
		ishaIntervalState.value = defaultValuesForMethod.value["ishaInterval"].toString()
		highLatitudeRuleState.value = defaultValuesForMethod.value["highLatitudeRule"].toString()
		fajrAdjustment.value = defaultValuesForMethod.value["fajrAdjustment"].toString()
		sunriseAdjustment.value = defaultValuesForMethod.value["sunriseAdjustment"].toString()
		dhuhrAdjustment.value = defaultValuesForMethod.value["dhuhrAdjustment"].toString()
		asrAdjustment.value = defaultValuesForMethod.value["asrAdjustment"].toString()
		maghribAdjustment.value = defaultValuesForMethod.value["maghribAdjustment"].toString()
		ishaAdjustment.value = defaultValuesForMethod.value["ishaAdjustment"].toString()
	}

	LaunchedEffect(
			calculationMethodState.value ,
			madhabState.value ,
			highLatitudeRuleState.value ,
			fajrAngleState.value ,
			ishaAngleState.value ,
			fajrAdjustment.value ,
			sunriseAdjustment.value ,
			dhuhrAdjustment.value ,
			asrAdjustment.value ,
			maghribAdjustment.value ,
			ishaAdjustment.value
				  )
	{
		val prayerTimesViewModel = PrayerTimesViewModel()
		val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
		val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
		//then pass it to the UPDATE_PRAYERTIMES event
		val mapOfParams = mutableMapOf<String , String>()
		mapOfParams["latitude"] = latitude.toString()
		mapOfParams["longitude"] = longitude.toString()
		mapOfParams["date"] = LocalDateTime.now().toString()
		mapOfParams["fajrAngle"] = fajrAngleState.value
		mapOfParams["ishaAngle"] = ishaAngleState.value
		mapOfParams["ishaInterval"] = ishaIntervalState.value
		mapOfParams["method"] = calculationMethodState.value
		mapOfParams["madhab"] = madhabState.value
		mapOfParams["highLatitudeRule"] = highLatitudeRuleState.value
		mapOfParams["fajrAdjustment"] = fajrAdjustment.value
		mapOfParams["sunriseAdjustment"] = sunriseAdjustment.value
		mapOfParams["dhuhrAdjustment"] = dhuhrAdjustment.value
		mapOfParams["asrAdjustment"] = asrAdjustment.value
		mapOfParams["maghribAdjustment"] = maghribAdjustment.value
		mapOfParams["ishaAdjustment"] = ishaAdjustment.value
		//then pass it to the UPDATE_PRAYERTIMES event
		prayerTimesViewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(mapOfParams))
	}

	Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState() , true)
				.padding(paddingValues)
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
						title = {
							Text(text = "Madhab")
						} ,
						subtitle = {
							Text(text = madhabState.value)
						} ,
						description = {
							Text(text = "The madhab used to calculate the asr prayer times.")
						} ,
						items = mapOfMadhabs ,
						valueState = madhabState ,
						height = 300.dp
							)
			}
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsList(
						title = {
							Text(text = "High Latitude Rule")
						} ,
						subtitle = {
							Text(text = highLatitudeRuleState.value)
						} ,
						description = {
							Text(text = "The high latitude rule used to calculate the prayer times.")
						} ,
						items = mapOfHighLatitudeRules ,
						valueState = highLatitudeRuleState ,
						height = 350.dp
							)
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
										  )
			}
			if(ishaaAngleVisible.value){
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
							subtitle = {
								Text(text = ishaAngleState.value)
							} ,
							description = {
								Text(text = "The angle of the sun at which the Isha prayer begins")
							} ,
							items = (0 .. 50).map { (it - 25) } ,
							valueState = ishaAngleState ,
											  )
				}
			}else{
				ElevatedCard(
						modifier = Modifier
							.padding(8.dp)
							.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
							.fillMaxWidth()
							) {
					SettingsMenuLink(
							modifier = Modifier.padding(8.dp) ,
							title = {Text(text = "Isha Interval of ${ishaIntervalState.value} Minutes")} ,
							subtitle = {Text(text = "The interval of time after Maghrib at which the Isha prayer begins")} ,
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
								title = {
									Text(text = "Fajr Time")
								} ,
								subtitle = {
									Text(text = fajrAdjustment.value)
								} ,
								description = {
									Text(text = "Adjust the time of the Fajr prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								valueState = fajrAdjustment ,
												  )
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								title = {
									Text(text = "Sunrise Time")
								} ,
								subtitle = {
									Text(text = sunriseAdjustment.value)
								} ,
								description = {
									Text(text = "Adjust the time of the Sunrise prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								valueState = sunriseAdjustment ,
												  )
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								title = {
									Text(text = "Dhuhr Time")
								} ,
								subtitle = {
									Text(text = dhuhrAdjustment.value)
								} ,
								description = {
									Text(text = "Adjust the time of the Dhuhr prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								valueState = dhuhrAdjustment ,
												  )
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								title = {
									Text(text = "Asr Time")
								} ,
								subtitle = {
									Text(text = asrAdjustment.value)
								} ,
								description = {
									Text(text = "Adjust the time of the Asr prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								valueState = asrAdjustment ,
												  )
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								title = {
									Text(text = "Maghrib Time")
								} ,
								subtitle = {
									Text(text = maghribAdjustment.value)
								} ,
								description = {
									Text(text = "Adjust the time of the Maghrib prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								valueState = maghribAdjustment ,
												  )
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						SettingsNumberPickerDialog(
								title = {
									Text(text = "Isha Time")
								} ,
								subtitle = {
									Text(text = ishaAdjustment.value)
								} ,
								description = {
									Text(text = "Adjust the time of the Isha prayer")
								} ,
								items = (0 .. 120).map { (it - 60) } ,
								valueState = ishaAdjustment ,
												  )
					}
				}
					 )
	}
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
	NimazTheme {
		PrayerTimesCustomizations(paddingValues = PaddingValues(16.dp))
	}
}