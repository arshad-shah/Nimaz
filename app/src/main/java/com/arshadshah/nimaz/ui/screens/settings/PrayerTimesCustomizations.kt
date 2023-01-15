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
	val fajrAngle : String = sharedPreferences.getData("fajr_angle" , "14.0")
	val ishaAngle : String = sharedPreferences.getData("isha_angle" , "14.0")
	val calculationMethod : String =
		sharedPreferences.getData("calculation_method" , "IRELAND")
	val madhab : String = sharedPreferences.getData("madhab" , "HANAFI")
	val highLatitudeRule : String =
		sharedPreferences.getData("high_latitude_rule" , "TWILIGHT_ANGLE")
	val fajrAdjustment : String = sharedPreferences.getData("fajr_adjustment" , "0")
	val sunriseAdjustment : String = sharedPreferences.getData("sunrise_adjustment" , "0")
	val dhuhrAdjustment : String = sharedPreferences.getData("dhuhr_adjustment" , "0")
	val asrAdjustment : String = sharedPreferences.getData("asr_adjustment" , "0")
	val maghribAdjustment : String = sharedPreferences.getData("maghrib_adjustment" , "0")
	val ishaAdjustment : String = sharedPreferences.getData("isha_adjustment" , "0")

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
		rememberPreferenceStringSettingState("fajr_angle" , "14.0")
	val ishaAngleState =
		rememberPreferenceStringSettingState("isha_angle" , "14.0")

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
							Text(text = calculationMethod)
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
							Text(text = madhab)
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
							Text(text = highLatitudeRule)
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
							Text(text = fajrAngle)
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
							Text(text = ishaAngle)
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
						PrayerTimesCustomizationsLink(title = "Fajr" , fajrAdjustment)
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						PrayerTimesCustomizationsLink(title = "Sunrise" , sunriseAdjustment)
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						PrayerTimesCustomizationsLink(title = "Dhuhr" , dhuhrAdjustment)
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						PrayerTimesCustomizationsLink(title = "Asr" , asrAdjustment)
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						PrayerTimesCustomizationsLink(
								title = "Maghrib" ,
								maghribAdjustment
													 )
					}
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								) {
						PrayerTimesCustomizationsLink(title = "Isha" , ishaAdjustment)
					}
				}
					 )
	}
}

//a composable that opens a dialog to change the prayer times
@Composable
fun PrayerTimesCustomizationsLink(title : String , subtitle : String)
{
	val correctedTitle = title.replaceFirstChar { it.lowercase() }
	val adjustmentString = "_adjustment"
	val storage = rememberPreferenceStringSettingState(
			key = "$correctedTitle$adjustmentString" ,
			defaultValue = "0"
													  )
	SettingsNumberPickerDialog(
			title = {
				Text(text = title)
			} ,
			subtitle = {
				Text(text = subtitle)
			} ,
			description = {
				//if title is "Sunrise" then the description is "The time of sunrise"
				if (title == "Sunrise")
				{
					Text(text = "The time of sunrise")
				} else
				{
					Text(text = "Adjust the time of the $title prayer")
				}
			} ,
			items = (0 .. 120).map { (it - 60) } ,
			valueState = storage ,
							  )
}