package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationInput(
	handleSettingEvents : KFunction1<SettingsViewModel.SettingsEvent , Unit> ,
	locationNameState : State<String>
					   )
{

	val context = LocalContext.current
	val showDialog = remember { mutableStateOf(false) }
	val name = remember {
		mutableStateOf(locationNameState.value)
	}
	//show manual location input
	//onclick open dialog
	SettingsMenuLink(
			title = { Text(text = "Edit Location") } ,
			subtitle = { Text(text = locationNameState.value) } ,
			onClick = {
				showDialog.value = true
			} ,
			icon = {
				Icon(
						modifier = Modifier.size(24.dp) ,
						painter = painterResource(id = R.drawable.location_marker_edit_icon) ,
						contentDescription = "Location"
					)
			}
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
						value = name.value ,
						onValueChange = { name.value = it } ,
						label = { Text(text = "Location") } ,
						singleLine = true ,
						modifier = Modifier.fillMaxWidth()
								 )
			} ,
			confirmButton = {
				Button(onClick = {
					handleSettingEvents(
							SettingsViewModel.SettingsEvent.LocationInput(
									context ,
									name.value
																		 )
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