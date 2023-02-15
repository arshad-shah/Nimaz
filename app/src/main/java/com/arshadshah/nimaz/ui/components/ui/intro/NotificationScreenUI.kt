package com.arshadshah.nimaz.ui.components.ui.intro

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsSwitch
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.utils.location.FeatureThatRequiresNotificationPermission
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationScreenUI()
{
	val context = LocalContext.current
	//get shared preference
	val sharedpref = PrivateSharedPreferences(context)
	//notification permission state
	val notificationPermissionState =
		rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

	val isChecked =
		remember { mutableStateOf(notificationPermissionState.status.isGranted) }

	//the state of the switch
	val state =
		rememberPreferenceBooleanSettingState(
				AppConstants.NOTIFICATION_ALLOWED ,
				notificationPermissionState.status.isGranted
											 )

	if (isChecked.value)
	{
		FeatureThatRequiresNotificationPermission(
				notificationPermissionState ,
				isChecked
												 )
	}

	val lifecycle = LocalLifecycleOwner.current.lifecycle
	lifecycle.addObserver(LifecycleEventObserver { _, event ->
		if (event == Lifecycle.Event.ON_RESUME)
		{
			isChecked.value = notificationPermissionState.status.isGranted
		}
	})
	//a laucnhed affect to check if the user has granted the notification permission
	LaunchedEffect(notificationPermissionState.status.isGranted) {
		if (notificationPermissionState.status.isGranted)
		{
			//if the user has granted the notification permission then set the state of the switch to true
			state.value = true
			//set the isChecked to true
			isChecked.value = true

			val sharedPreferences = PrivateSharedPreferences(context)
			val channelLock =
				sharedPreferences.getDataBoolean(AppConstants.CHANNEL_LOCK , false)
			if (! channelLock)
			{
				CreateAlarms().createAllNotificationChannels(context)
				sharedPreferences.saveDataBoolean(AppConstants.CHANNEL_LOCK , true)
			}

			sharedpref.saveDataBoolean(AppConstants.NOTIFICATION_ALLOWED , true)
		} else
		{
			//if the user has not granted the notification permission then set the state of the switch to false
			state.value = false
			//set the isChecked to false
			isChecked.value = false
		}
	}

	SettingsSwitch(
			state = state ,
			onCheckedChange = {
				if (it)
				{
					//if its android 13 or above then check if the notification permission is granted else take the user to the notification settings
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
					{
						if (notificationPermissionState.status.isGranted)
						{
							//if the permission is granted, then save the value in the shared preferences
							sharedpref.saveDataBoolean(
									AppConstants.NOTIFICATION_ALLOWED ,
									true
													  )
						} else
						{
							notificationPermissionState.launchPermissionRequest()
						}
					} else
					{
						//take the user to the notification settings
						val intent = Intent()
						intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
						intent.putExtra(
								"android.provider.extra.APP_PACKAGE" ,
								context.packageName
									   )
						context.startActivity(intent)
					}
				} else
				{
					//if its unchecked, then we need to remove the notification permission
					//and remove the value from the shared preferences
					sharedpref.removeData(AppConstants.NOTIFICATION_ALLOWED)
					//take the user to the notification settings
					val intent = Intent()
					intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
					intent.putExtra(
							"android.provider.extra.APP_PACKAGE" ,
							context.packageName
								   )
					context.startActivity(intent)
				}
			} ,
			title = {
				Text(text = "Allow Notifications")
			} ,
			subtitle = {
				//if the permission is granted, show a checkmark and text saying "Allowed"
				if (isChecked.value)
				{
					Row {
						Icon(
								imageVector = Icons.Filled.CheckCircle ,
								contentDescription = "Notifications Allowed"
							)
						Text(text = "Allowed")
					}
				} else
				{
					//if the permission is not granted, show a notification icon and text saying "Not Allowed"
					Row {
						Icon(
								imageVector = Icons.Filled.Close ,
								contentDescription = "Notifications Not Allowed"
							)
						Text(text = "Not Allowed")
					}
				}
			} ,
			icon = {
				Icon(
						imageVector = Icons.Filled.Notifications ,
						contentDescription = "Notifications"
					)
			}
				  )
}