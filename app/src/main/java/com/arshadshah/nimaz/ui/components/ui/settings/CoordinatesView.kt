package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun CoordinatesView() {
    val sharedPreferences = PrivateSharedPreferences(LocalContext.current)
    val latitude = sharedPreferences.getDataDouble("latitude", 53.3498)
    val longitude = sharedPreferences.getDataDouble("longitude", -6.2603)

    //round the latitude and longitude to 4 decimal places
    val latitudeRounded = String.format("%.4f", latitude)
    val longitudeRounded = String.format("%.4f", longitude)

    SettingsMenuLink(
        title = { Text(text = "Latitude") },
        subtitle = { Text(text = latitudeRounded) },
        onClick = {})
    SettingsMenuLink(
        title = { Text(text = "Longitude") },
        subtitle = { Text(text = longitudeRounded) },
        onClick = {})
}