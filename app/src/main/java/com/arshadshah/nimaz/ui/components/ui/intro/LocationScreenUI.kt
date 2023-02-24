package com.arshadshah.nimaz.ui.components.ui.intro

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.ManualLocationInput
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsSwitch
import com.arshadshah.nimaz.utils.location.FeatureThatRequiresLocationPermission
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationScreenUI()
{
	val context = LocalContext.current
	val viewModel = viewModel(key = "SettingsViewModel", initializer = { SettingsViewModel(context) }, viewModelStoreOwner = context as ComponentActivity)
	val locationNameState = remember {
		viewModel.locationName
	}.collectAsState()

	//location permission state
	val locationPermissionState = rememberMultiplePermissionsState(
			permissions = listOf(
					Manifest.permission.ACCESS_COARSE_LOCATION ,
					Manifest.permission.ACCESS_FINE_LOCATION
								)
																  )
	//the state of the switch
	val state =
		rememberPreferenceBooleanSettingState(
				AppConstants.LOCATION_TYPE ,
				locationPermissionState.allPermissionsGranted
											 )

	val checked =
		remember { mutableStateOf(locationPermissionState.allPermissionsGranted) }
	//call FeatureThatRequiresLocationPermission() when the switch is checked
	if (checked.value)
	{
		FeatureThatRequiresLocationPermission(locationPermissionState , checked)
	}

	//a laucnhed affect to check if the user has granted the notification permission
	LaunchedEffect(locationPermissionState.allPermissionsGranted) {
		if (locationPermissionState.allPermissionsGranted)
		{
			//if the user has granted the notification permission then set the state of the switch to true
			state.value = true
			//set the isChecked to true
			checked.value = true
			PrayerTimesViewModel.PrayerTimesEvent.RELOAD
		} else
		{
			//if the user has not granted the notification permission then set the state of the switch to false
			state.value = false
			//set the isChecked to false
			checked.value = false
		}
	}

	SettingsSwitch(
			modifier = Modifier.testTag("LocationSwitch") ,
			state = state ,
			onCheckedChange = {
				if (it)
				{
					if (locationPermissionState.allPermissionsGranted)
					{
						viewModel.handleEvent(SettingsViewModel.SettingsEvent.LocationToggle(context,it))
					} else
					{
						viewModel.handleEvent(SettingsViewModel.SettingsEvent.LocationToggle(context,it))
						locationPermissionState.launchMultiplePermissionRequest()
					}
				} else
				{
					viewModel.handleEvent(SettingsViewModel.SettingsEvent.LocationToggle(context,it))
					Toasty.info(
							context ,
							"Please disable location permission for Nimaz in \n Permissions -> Location -> Don't Allow" ,
							Toasty.LENGTH_LONG
							   ).show()
					//send the user to the location settings of the app
					val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
					with(intent) {
						data = Uri.fromParts("package" , context.packageName , null)
						addCategory(Intent.CATEGORY_DEFAULT)
						addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
						addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
					}

					context.startActivity(intent)
				}
			} ,
			title = {
				Text(text = "Enable Auto Location")
			} ,
			subtitle = {
				//if the permission is granted, show a checkmark and text saying "Allowed"
				if (checked.value)
				{
					Row(
							verticalAlignment = Alignment.CenterVertically
					   ) {
						Icon(
								imageVector = Icons.Filled.CheckCircle ,
								contentDescription = "Location Allowed"
							)
						Text(text = "Enabled")
					}
				} else
				{
					//if the permission is not granted, show a notification icon and text saying "Not Allowed"
					Row(
							verticalAlignment = Alignment.CenterVertically
					   ) {
						Icon(
								imageVector = Icons.Filled.Close ,
								contentDescription = "Location Not Allowed"
							)
						Text(text = "Disabled")
					}
				}
			} ,
			icon = {
				Icon(
						imageVector = Icons.Filled.LocationOn ,
						contentDescription = "Location"
					)
			}
				  )

	//if checked, then show the manual location page
	if (! checked.value)
	{
		ManualLocationInput(
				handleSettingEvents = viewModel::handleEvent,
				locationNameState = locationNameState,
						   )
	}
}


@Preview
@Composable
fun LocationScreenUIPreview()
{
	LocationScreenUI()
}