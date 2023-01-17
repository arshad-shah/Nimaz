package com.arshadshah.nimaz.ui.screens.introduction

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.ui.icons.Prayer
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsSwitch
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
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
			title = "Nimaz" ,
			description = "Nimaz is a muslim lifestyle companion app that helps you keep track of your daily prayers." ,
								 )

	object Second : OnBoardingPage(
			image = FeatherIcons.Clock ,
			title = "Prayer Times" ,
			description = "Accurate prayer times for your location. You can also customize the prayer times." ,
								  )

	object Third : OnBoardingPage(
			image = FeatherIcons.Compass ,
			title = "Qibla Direction" ,
			description = "Accurately find the direction of the Qibla with Nimaz." ,
								 )

	object Fourth : OnBoardingPage(
			image = FeatherIcons.BookOpen ,
			title = "Quran" ,
			description = "Read the Quran in Arabic with urdu and english translations." ,
								  )

	//the Notification permission page
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	object Fifth : OnBoardingPage(
			image = FeatherIcons.BookOpen ,
			title = "Notifications" ,
			description = "Nimaz can send you notifications for your prayers. You can also customize the notifications." ,
			extra = {
				val context = LocalContext.current
				//ask for notification permission
				val permissionGranted = ActivityCompat.checkSelfPermission(
						context ,
						Manifest.permission.ACCESS_NOTIFICATION_POLICY
																		  ) == PackageManager.PERMISSION_GRANTED
				//get shared preference
				val sharedpref = PrivateSharedPreferences(context)
				//the state of the switch
				val state =
					rememberPreferenceBooleanSettingState("notificationAllowed" , permissionGranted)
				SettingsSwitch(
						state = state ,
						onCheckedChange = {
							if (it)
							{
								ActivityCompat.requestPermissions(
										context as Activity ,
										arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY) ,
										1
																 )
							} else
							{
								//if its unchecked, then we need to remove the notification permission
								//and remove the value from the shared preferences
								sharedpref.removeData("notificationAllowed")
							}
						} ,
						title = {
							Text(text = "Allow Notifications")
						} ,
						subtitle = {
							//if the permission is granted, show a checkmark and text saying "Allowed"
							if (permissionGranted)
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
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	object Sixth : OnBoardingPage(
			image = FeatherIcons.MapPin ,
			title = "Location" ,
			description = "Nimaz can use your location to get accurate prayer times. You can also use manual location." ,
			extra = {
				val context = LocalContext.current
				//ask for location permission
				val permissionGranted = ActivityCompat.checkSelfPermission(
						context ,
						Manifest.permission.ACCESS_FINE_LOCATION
																		  ) == PackageManager.PERMISSION_GRANTED ||
						ActivityCompat.checkSelfPermission(
								context ,
								Manifest.permission.ACCESS_COARSE_LOCATION
														  ) == PackageManager.PERMISSION_GRANTED
				//get shared preference
				val sharedpref = PrivateSharedPreferences(context)
				//the state of the switch
				val state =
					rememberPreferenceBooleanSettingState("location_auto" , permissionGranted)
				SettingsSwitch(
						state = state ,
						onCheckedChange = {
							if (it)
							{
								ActivityCompat.requestPermissions(
										context as Activity ,
										arrayOf(
												Manifest.permission.ACCESS_FINE_LOCATION ,
												Manifest.permission.ACCESS_COARSE_LOCATION
											   ) ,
										1
																 )
							} else
							{
								//if its unchecked, then we need to remove the location permission
								//and remove the value from the shared preferences
								sharedpref.removeData("locationAllowed")
							}
						} ,
						title = {
							Text(text = "Allow Auto Location")
						} ,
						subtitle = {
							//if the permission is granted, show a checkmark and text saying "Allowed"
							if (permissionGranted)
							{
								Row {
									Icon(
											imageVector = Icons.Filled.CheckCircle ,
											contentDescription = "Location Allowed"
										)
									Text(text = "Allowed")
								}
							} else
							{
								//if the permission is not granted, show a notification icon and text saying "Not Allowed"
								Row {
									Icon(
											imageVector = Icons.Filled.Close ,
											contentDescription = "Location Not Allowed"
										)
									Text(text = "Not Allowed")
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
			}
								 )

	//a page to ask for the battery optimization exemption
	@SuppressLint("BatteryLife")
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	object Seventh : OnBoardingPage(
			image = FeatherIcons.Battery ,
			title = "Battery Optimization" ,
			description = "Nimaz needs to be exempted from battery optimization to work properly." ,
			extra = {
				val context = LocalContext.current
				//get shared preference
				val sharedpref = PrivateSharedPreferences(context)

				//battery optimization exemption
				val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
				val isbatteryOptimizationExempted =
					powerManager.isIgnoringBatteryOptimizations(context.packageName)
				//the state of the switch
				val state = rememberPreferenceBooleanSettingState(
						"battery_optimization" ,
						isbatteryOptimizationExempted
																 )
				SettingsSwitch(
						state = state ,
						onCheckedChange = {
							if (it)
							{
								//if the switch is checked, then we need to ask for the battery optimization exemption
								//and save the value in the shared preferences
								sharedpref.saveDataBoolean("battery_optimization" , true)
								val intent = Intent()
								intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
								intent.data = Uri.parse("package:" + context.packageName)
								context.startActivity(intent)
							} else
							{
								//if its unchecked, then we need to remove the battery optimization exemption
								//and remove the value from the shared preferences
								sharedpref.removeData("battery_optimization")
							}
						} ,
						title = {
							Text(text = "Allow Battery Optimization")
						} ,
						subtitle = {
							//if the permission is granted, show a checkmark and text saying "Allowed"
							if (isbatteryOptimizationExempted)
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
}