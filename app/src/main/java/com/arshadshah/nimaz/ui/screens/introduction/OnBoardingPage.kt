package com.arshadshah.nimaz.ui.screens.introduction

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.activities.Introduction
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.ui.icons.Prayer
import com.arshadshah.nimaz.ui.components.ui.intro.LocationScreenUI
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsSwitch
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.utils.location.FeatureThatRequiresNotificationPermission
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import compose.icons.FeatherIcons
import compose.icons.feathericons.*

sealed class OnBoardingPage(
	val image : ImageVector ,
	val title : String ,
	val description : String ,
	val extra : @Composable () -> Unit = {} ,
						   )
{

	object First : OnBoardingPage(
			image = Icons.Prayer ,
			title = "Assalamu alaikum" ,
			description = "Nimaz is a muslim lifestyle companion app that helps you keep track of your daily prayers." ,
								 )

	object Second : OnBoardingPage(
			image = FeatherIcons.Clock ,
			title = "Prayer Times" ,
			description = "Accurate prayer times for your location, Adhan notifications, and more." ,
								  )

	object Third : OnBoardingPage(
			image = FeatherIcons.BookOpen ,
			title = "Quran" ,
			description = "Quran with urdu and english translations." ,
								 )

	//the Notification permission page
	@OptIn(ExperimentalPermissionsApi::class)
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	object Fourth : OnBoardingPage(
			image = FeatherIcons.Bell ,
			title = "Notifications" ,
			description = "Enable Notifications for Nimaz to get Prayer alerts in the form of Adhan." ,
			extra = {
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
								isChecked.value = false
								//if its unchecked, then we need to remove the notification permission
								//and remove the value from the shared preferences
								sharedpref.removeData(AppConstants.NOTIFICATION_ALLOWED)
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
								  )

	//the location permission page
	@OptIn(ExperimentalPermissionsApi::class)
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	object Fifth : OnBoardingPage(
			image = FeatherIcons.MapPin ,
			title = "Location" ,
			description = "Nimaz needs your location to get accurate prayer times. You can also use manual location." ,
			extra = {
				LocationScreenUI()
			}
								 )

	//a page to ask for the battery optimization exemption
	@SuppressLint("BatteryLife")
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	object Sixth : OnBoardingPage(
			image = FeatherIcons.Battery ,
			title = "Battery Optimization" ,
			description = "Nimaz needs to be exempted from battery optimization to show adhan notifications." ,
			extra = {
				val context = LocalContext.current
				//get shared preference
				val sharedpref = PrivateSharedPreferences(context)

				//battery optimization exemption
				val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
				val isChecked = remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }
				//the state of the switch
				val state = rememberPreferenceBooleanSettingState(
						AppConstants.BATTERY_OPTIMIZATION ,
						isChecked.value
																 )

				//a laucnhed affect to check if the user has granted the notification permission
				LaunchedEffect(isChecked.value) {
					if (isChecked.value)
					{
						//if the user has granted the notification permission then set the state of the switch to true
						state.value = true
						//set the isChecked to true
						isChecked.value = true
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
								//if the switch is checked, then we need to ask for the battery optimization exemption
								//and save the value in the shared preferences
								sharedpref.saveDataBoolean(AppConstants.BATTERY_OPTIMIZATION , true)
								val intent = Intent()
								intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
								intent.data = Uri.parse("package:" + context.packageName)
								context.startActivity(intent)
							} else
							{
								//if its unchecked, then we need to remove the battery optimization exemption
								//and remove the value from the shared preferences
								sharedpref.removeData(AppConstants.BATTERY_OPTIMIZATION)
							}
						} ,
						title = {
							Text(text = "Allow Battery Optimization")
						} ,
						subtitle = {
							//if the permission is granted, show a checkmark and text saying "Allowed"
							if (isChecked.value)
							{
								Row {
									Icon(
											imageVector = Icons.Filled.CheckCircle ,
											contentDescription = "Battery Optimization Allowed"
										)
									Text(text = "Allowed")
								}
							} else
							{
								//if the permission is not granted, show a notification icon and text saying "Not Allowed"
								Row {
									Icon(
											imageVector = Icons.Filled.Close ,
											contentDescription = "Battery Optimization Not Allowed"
										)
									Text(text = "Not Allowed")
								}
							}
						} ,
						icon = {
							Icon(
									imageVector = FeatherIcons.Battery ,
									contentDescription = "Battery Optimization"
								)
						}
							  )
			}
								 )

	object Seventh : OnBoardingPage(
			image = FeatherIcons.CheckCircle ,
			title = "Onboarding Complete" ,
			description = "You are all set to use Nimaz. You can always change these settings later. I hope Nimaz helps you in your daily life and Kindly keep me and my family in your prayers." ,
			extra = {
				val context = LocalContext.current
				//a button to navigate to the main screen
				Button(
						onClick = {
							val sharedPref = PrivateSharedPreferences(context)
							sharedPref.saveDataBoolean(AppConstants.IS_FIRST_INSTALL , false)
							context.startActivity(Intent(context , MainActivity::class.java))
							//remove the activity from the back stack
							(context as Introduction).finish()
						} ,
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp)
					  ) {
					Text(text = "Get Started")
				}
			}
								   )
}