package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun CoordinatesView()
{
	val sharedPreferences = PrivateSharedPreferences(LocalContext.current)
	val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498)
	val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)

	//if a value was changed then set a flag to true so that prayer times can be recalculated
	//and the flag can be set to false again
	when
	{
		latitude != sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498) ->
		{
			sharedPreferences.saveDataBoolean(AppConstants.RECALCULATE_PRAYER_TIMES, true)
		}
		longitude != sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603) ->
		{
			sharedPreferences.saveDataBoolean(AppConstants.RECALCULATE_PRAYER_TIMES, true)
		}
	}

	//round the latitude and longitude to 4 decimal places
	val latitudeRounded = String.format("%.4f" , latitude)
	val longitudeRounded = String.format("%.4f" , longitude)

	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				.fillMaxWidth()
				) {
		SettingsMenuLink(
				title = { Text(text = "Latitude") } ,
				subtitle = { Text(text = latitudeRounded) } ,
				onClick = {})
	}
	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				.fillMaxWidth()
				) {
		SettingsMenuLink(
				title = { Text(text = "Longitude") } ,
				subtitle = { Text(text = longitudeRounded) } ,
				onClick = {})
	}
}