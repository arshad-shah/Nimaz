package com.arshadshah.nimaz.ui.components.ui.intro

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.ManualLocationInput
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsSwitch
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.FeatureThatRequiresLocationPermission
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationScreenUI()
{
	val context = LocalContext.current
	val sharedpref = PrivateSharedPreferences(context)
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

	//a manual location page
	val prayerTimesViewModel = PrayerTimesViewModel()
	val sharedPreferences = PrivateSharedPreferences(context)
	//values for coordinates that are mutable
	val longitude =
		remember { mutableStateOf(sharedPreferences.getDataDouble(AppConstants.LONGITUDE , 0.0)) }
	val latitude =
		remember { mutableStateOf(sharedPreferences.getDataDouble(AppConstants.LATITUDE , 0.0)) }
	val locationName = remember {
		mutableStateOf(
				sharedPreferences.getData(
						AppConstants.LOCATION_INPUT ,
						"Abbeyleix"
										 )
					  )
	}

	val locationFoundCallbackManual =
		{ latitudeValue : Double , longitudeValue : Double , name : String ->
			longitude.value = longitudeValue
			latitude.value = latitudeValue
			locationName.value = name
			sharedPreferences.saveData(AppConstants.LOCATION_INPUT , name)
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
			state = state ,
			onCheckedChange = {
				if (it)
				{
					if (locationPermissionState.allPermissionsGranted)
					{
						sharedpref.saveDataBoolean(AppConstants.LOCATION_TYPE , true)
					} else
					{
						locationPermissionState.launchMultiplePermissionRequest()
						sharedpref.saveDataBoolean(AppConstants.LOCATION_TYPE , true)
					}
				} else
				{
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
					//if its unchecked, then we need to remove the location permission
					//and remove the value from the shared preferences
					sharedpref.removeData(AppConstants.LOCATION_TYPE)
				}
			} ,
			title = {
				Text(text = if (! checked.value) "Location" else "Allow Auto Location")
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
						Text(text = "Allowed")
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
						Text(text = "Auto Location not allowed")
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
				reloadPrayerTimes = prayerTimesViewModel::handleEvent ,
				locationFoundCallbackManual = locationFoundCallbackManual
						   )
	}
}


@Preview
@Composable
fun LocationScreenUIPreview()
{
	LocationScreenUI()
}