package com.arshadshah.nimaz.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.*
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.LocationFinderAuto

@Composable
fun SettingsScreen(
	onNavigateToPrayerTimeCustomizationScreen : () -> Unit ,
	paddingValues : PaddingValues ,
				  )
{
	val sharedPreferences = PrivateSharedPreferences(LocalContext.current)
	val locationFinderAuto = LocationFinderAuto()
	val cityname =
		rememberPreferenceStringSettingState(key = "location_input" , defaultValue = "Abbeyleix")

	Column(modifier = Modifier.verticalScroll(rememberScrollState() , true)) {
		SettingsGroup(title = { Text(text = "Location") }) {
			val storage =
				rememberPreferenceBooleanSettingState("location_auto" , true , sharedPreferences)
			SettingsSwitch(
					state = storage ,
					icon = {
						Icon(
								imageVector = Icons.Outlined.LocationOn ,
								contentDescription = "Clear"
							)
					} ,
					title = {
						if (storage.value)
						{
							Text(text = "Automatic")
							//if the location city name is not null, then run the code
							if (cityname.value != "")
							{
								Location().getAutomaticLocation(LocalContext.current)
							}
						} else
						{
							Text(text = "Manual")
							locationFinderAuto.stopLocationUpdates()
						}
					} ,
					subtitle = {
						if (storage.value)
						{
							Text(text = cityname.value)
						}
					} ,
					onCheckedChange = {
						storage.value = it
					}
						  )
			if (! storage.value)
			{
				ManualLocationInput()
				CoordinatesView()
			}
		}
		ElevatedCard(
				modifier = Modifier
					.padding(8.dp)
					.shadow(4.dp , clip = true , shape = CardDefaults.elevatedShape)
					.fillMaxWidth()
					) {
			SettingsMenuLink(
					title = { Text(text = "Prayer Times Adjustments") } ,
					onClick = onNavigateToPrayerTimeCustomizationScreen ,
					icon = {
						Icon(
								imageVector =
								//get the icon from the resources
								ImageVector.vectorResource(id = R.drawable.ic_clock) ,
								contentDescription = "Clock"
							)
					} ,
							)
		}
	}
}