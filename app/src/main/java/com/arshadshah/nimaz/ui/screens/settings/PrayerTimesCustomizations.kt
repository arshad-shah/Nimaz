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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsList
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsNumberPickerDialog
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun PrayerTimesCustomizations(paddingValues : PaddingValues)
{
	val sharedPreferences = PrivateSharedPreferences(LocalContext.current)

	val mapOfMethods = AppConstants.getMethods()
	val mapOfMadhabs = AppConstants.getAsrJuristic()
	val mapOfHighLatitudeRules = AppConstants.getHighLatitudes()

	val calculationMethodState =
		rememberPreferenceStringSettingState("calculation_method" , "IRELAND")
	val madhabState = rememberPreferenceStringSettingState("madhab" , "HANAFI")
	val highLatitudeRuleState = rememberPreferenceStringSettingState(
			"high_latitude_rule" ,
			"TWILIGHT_ANGLE"
																	)
	val fajrAngleState =
		rememberPreferenceStringSettingState("fajr_angle" , "14")
	val ishaAngleState =
		rememberPreferenceStringSettingState("isha_angle" , "14")

	val fajrAdjustment = rememberPreferenceStringSettingState(
		key = "fajr_adjustment" ,
		defaultValue = "0"
	)
	val sunriseAdjustment = rememberPreferenceStringSettingState(
		key = "sunrise_adjustment" ,
		defaultValue = "0"
	)
	val dhuhrAdjustment = rememberPreferenceStringSettingState(key = "dhuhr_adjustment" , defaultValue = "0")
	val asrAdjustment = rememberPreferenceStringSettingState(key = "asr_adjustment" , defaultValue = "0")
	val maghribAdjustment = rememberPreferenceStringSettingState(
		key = "maghrib_adjustment" ,
		defaultValue = "0"
	)
	val ishaAdjustment = rememberPreferenceStringSettingState(key = "isha_adjustment" , defaultValue = "0")

	//call this : sharedPreferences.saveDataBoolean("recalculate_prayer_times" , true)
	//whenever a setting is changed to recalculate the prayer times
	LaunchedEffect(calculationMethodState.value,
				   madhabState.value,
				   highLatitudeRuleState.value,
				   fajrAngleState.value,
				   ishaAngleState.value,
				   fajrAdjustment.value,
				   sunriseAdjustment.value,
				   dhuhrAdjustment.value,
				   asrAdjustment.value,
				   maghribAdjustment.value,
				   ishaAdjustment.value
				  )
	{
		sharedPreferences.saveDataBoolean("recalculate_prayer_times" , true)
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