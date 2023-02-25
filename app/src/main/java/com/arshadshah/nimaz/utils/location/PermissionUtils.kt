package com.arshadshah.nimaz.utils.location

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.BooleanPreferenceSettingValueState
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FeatureThatRequiresLocationPermission(
	locationPermissionState : MultiplePermissionsState ,
	checked : BooleanPreferenceSettingValueState ,
										 )
{

	val descToShow = remember { mutableStateOf("") }
	val showDialog = remember { mutableStateOf(false) }
	//check if location permission is granted
	if (locationPermissionState.allPermissionsGranted)
	{
		checked.value = true
	} else
	{
		if (locationPermissionState.shouldShowRationale)
		{
			showDialog.value = true
			descToShow.value =
				"Location permission is required to get accurate prayer times. Please allow location permission."

		} else
		{
			showDialog.value = true
			descToShow.value =
				"Location permission is required to get accurate prayer times, Nimaz will revert to manual location."
		}
	}

	if (showDialog.value)
	{
		//permission not granted
		//show dialog
		AlertDialog(
				onDismissRequest = {
					showDialog.value = false
				} ,
				title = { Text(text = "Location Permission Required") } ,
				text = { Text(text = descToShow.value) } ,
				confirmButton = {
					Button(onClick = {
						showDialog.value = false
						locationPermissionState.launchMultiplePermissionRequest()
					}) {
						Text(text = "Grant Permission")
					}
				} ,
				dismissButton = {
					Button(onClick = {
						showDialog.value = false
					}) {
						Text(text = "Cancel")
					}
				}
				   )
	}
}

//feature that requires notification permission
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FeatureThatRequiresNotificationPermission(
	notificationPermissionState : PermissionState ,
	isChecked : MutableState<Boolean> ,
											 )
{

	val descToShow = remember { mutableStateOf("") }
	val showRationale = remember { mutableStateOf(false) }
	//check if notification permission is granted
	if (notificationPermissionState.status.isGranted)
	{
		isChecked.value = true
	} else
	{
		//rationale
		if (notificationPermissionState.status.shouldShowRationale)
		{
			showRationale.value = true
			descToShow.value =
				"Notification permission is required to deliver adhan notifications. Please allow notification permission."
		} else
		{
			showRationale.value = true
			descToShow.value =
				"Notification permission is required to deliver adhan notifications, Nimaz will not be able to show adhan notifications if it is denied."
		}
	}

	if (showRationale.value)
	{
		//permission not granted
		//show dialog
		AlertDialog(
				onDismissRequest = {
					showRationale.value = false } ,
				title = { Text(text = "Notification Permission Required") } ,
				text = { Text(text = descToShow.value) } ,
				confirmButton = {
					Button(onClick = { notificationPermissionState.launchPermissionRequest() }) {
						Text(text = "Allow")
					}
				} ,
				dismissButton = {
					Button(onClick = { showRationale.value = false }) {
						Text(text = "Cancel")
					}
				}
				   )
	}

}