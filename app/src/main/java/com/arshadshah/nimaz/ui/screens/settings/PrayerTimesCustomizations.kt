package com.arshadshah.nimaz.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsList
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun PrayerTimesCustomizations(paddingValues: PaddingValues) {
    val sharedPreferences = PrivateSharedPreferences(LocalContext.current)
    val fajrAngle: String = sharedPreferences.getData("fajr_angle", "14.0")
    val ishaAngle: String = sharedPreferences.getData("isha_angle", "14.0")
    val calculationMethod: String =
        sharedPreferences.getData("calculation_method", "IRELAND")
    val madhab: String = sharedPreferences.getData("madhab", "HANAFI")
    val highLatitudeRule: String =
        sharedPreferences.getData("high_latitude_rule", "TWILIGHT_ANGLE")
    val fajrAdjustment: String = sharedPreferences.getData("fajr_adjustment", "0")
    val dhuhrAdjustment: String = sharedPreferences.getData("dhuhr_adjustment", "0")
    val asrAdjustment: String = sharedPreferences.getData("asr_adjustment", "0")
    val maghribAdjustment: String = sharedPreferences.getData("maghrib_adjustment", "0")
    val ishaAdjustment: String = sharedPreferences.getData("isha_adjustment", "0")


    val mapOfMethods = AppConstants.getMethods()
    val mapOfMadhabs = AppConstants.getMadhabs()
    val mapOfHighLatitudeRules = AppConstants.getHighLatitudeRules()

    val calculationMethodState =
        rememberPreferenceStringSettingState("calculation_method", "IRELAND", sharedPreferences)
    val madhabState = rememberPreferenceStringSettingState("madhab", "HANAFI", sharedPreferences)
    val highLatitudeRuleState = rememberPreferenceStringSettingState(
        "high_latitude_rule",
        "TWILIGHT_ANGLE",
        sharedPreferences
    )
    val fajrAngleState =
        rememberPreferenceStringSettingState("fajr_angle", "14.0", sharedPreferences)
    val ishaAngleState =
        rememberPreferenceStringSettingState("isha_angle", "14.0", sharedPreferences)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState(), true)
            .padding(paddingValues)
    ) {
        SettingsGroup(title = {
            Text(text = "Prayer Parameters")
        }) {
            SettingsList(
                title = {
                    Text(text = "Calculation Method")
                },
                subtitle = {
                    Text(text = calculationMethod)
                },
                items = mapOfMethods,
                useSelectedValueAsSubtitle = true,
                valueState = calculationMethodState,
            )
            Divider(color = MaterialTheme.colorScheme.outline)
            SettingsList(
                title = {
                    Text(text = "Madhab")
                },
                subtitle = {
                    Text(text = madhab)
                },
                items = mapOfMadhabs,
                useSelectedValueAsSubtitle = true,
                valueState = madhabState,
            )
            Divider(color = MaterialTheme.colorScheme.outline)
            SettingsList(
                title = {
                    Text(text = "High Latitude Rule")
                },
                subtitle = {
                    Text(text = highLatitudeRule)
                },
                items = mapOfHighLatitudeRules,
                useSelectedValueAsSubtitle = true,
                valueState = highLatitudeRuleState,
            )
        }

        SettingsGroup(title = {
            Text(text = "Prayer Angles")
        }) {
            SettingsList(
                title = {
                    Text(text = "Fajr Angle")
                },
                subtitle = {
                    Text(text = fajrAngle)
                },
                //list between -25 and 25 of strings
                items = (0..50).map { (it - 25).toString() },
                useSelectedValueAsSubtitle = true,
                valueState = fajrAngleState,
            )
            Divider(color = MaterialTheme.colorScheme.outline)
            SettingsList(
                title = {
                    Text(text = "Isha Angle")
                },
                subtitle = {
                    Text(text = ishaAngle)
                },
                //list between -25 and 25 of strings
                items = (0..50).map { (it - 25).toString() },
                useSelectedValueAsSubtitle = true,
                valueState = ishaAngleState,
            )
        }



        SettingsGroup(
            title = {
                Text(text = "Prayer Time")
            },
            content = {
                PrayerTimesCustomizationsLink(title = "Fajr", fajrAdjustment)
                Divider(color = MaterialTheme.colorScheme.outline)
                PrayerTimesCustomizationsLink(title = "Dhuhr", dhuhrAdjustment)
                Divider(color = MaterialTheme.colorScheme.outline)
                PrayerTimesCustomizationsLink(title = "Asr", asrAdjustment)
                Divider(color = MaterialTheme.colorScheme.outline)
                PrayerTimesCustomizationsLink(
                    title = "Maghrib",
                    maghribAdjustment.toString()
                )
                Divider(color = MaterialTheme.colorScheme.outline)
                PrayerTimesCustomizationsLink(title = "Isha", ishaAdjustment)
            }
        )
    }
}

//a composable using the SettingsMenuLink that opens a dialog to change the prayer times
@Composable
fun PrayerTimesCustomizationsLink(title: String, subtitle: String) {
    val correctedTitle = title.replaceFirstChar { it.lowercase() }
    val adjustmentString = "_adjustment"
    val storage = rememberPreferenceStringSettingState(
        key = "$correctedTitle$adjustmentString",
        defaultValue = "0"
    )
    SettingsList(
        title = {
            Text(text = title)
        },
        subtitle = {
            Text(text = subtitle)
        },
        items = (0..120).toList().map { it - 60 }.map { it.toString() },
        useSelectedValueAsSubtitle = true,
        valueState = storage,
    )
}