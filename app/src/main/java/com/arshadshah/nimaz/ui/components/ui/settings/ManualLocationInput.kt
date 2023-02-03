package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import compose.icons.FeatherIcons
import compose.icons.feathericons.Edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationInput(locationFoundCallbackManual : (Double , Double , String) -> Unit)
{

	val context = LocalContext.current
	//get laitude and longitude from private shared preferences
	val sharedPreferences = PrivateSharedPreferences(context)
	val cityName = rememberPreferenceStringSettingState(
			key = AppConstants.LOCATION_INPUT ,
			defaultValue = "Abbeyleix" ,
			sharedPreferences
													   )
	val showDialog = remember { mutableStateOf(false) }
	//show manual location input
	//onclick open dialog
	SettingsMenuLink(
			title = { Text(text = "Edit Location") } ,
			subtitle = { Text(text = cityName.value) } ,
			onClick = {
				showDialog.value = true
			} ,
			icon = { Icon(imageVector = FeatherIcons.Edit , contentDescription = "Location") }
					)

	if (! showDialog.value) return
	//text input field
	//open dialog
	AlertDialog(
			onDismissRequest = {
			} ,
			title = { Text(text = "Edit Location") } ,
			text = {
				OutlinedTextField(
						value = cityName.value ,
						onValueChange = { cityName.value = it } ,
						label = { Text(text = "Location") } ,
						singleLine = true ,
						modifier = Modifier.fillMaxWidth()
								 )
			} ,
			confirmButton = {
				Button(onClick = {
					//get Manual location
					Location().getManualLocation(
							name = cityName.value ,
							context = context ,
							locationFoundCallbackManual = locationFoundCallbackManual
												)
					showDialog.value = false
				}) { Text(text = "Confirm") }
			} ,
			dismissButton = {
				TextButton(onClick = {
					showDialog.value = false

				}) { Text(text = "Cancel") }
			} ,
			   )

}