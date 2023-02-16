package com.arshadshah.nimaz.ui.components.ui.intro

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsSwitch
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@SuppressLint("BatteryLife")
@Composable
fun BatteryExemptionUI()
{
	val context = LocalContext.current
	//get shared preference
	val sharedpref = PrivateSharedPreferences(context)

	//battery optimization exemption
	val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
	val isChecked =
		remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }
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
					//navigate to the battery optimization settings
					val intent = Intent()
					intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
					context.startActivity(intent)
					//if its unchecked, then we need to remove the battery optimization exemption
					//and remove the value from the shared preferences
					sharedpref.removeData(AppConstants.BATTERY_OPTIMIZATION)
				}
			} ,
			title = {
				Text(text = "Battery Optimization")
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
						modifier = Modifier.size(24.dp) ,
						painter = painterResource(id = R.drawable.battery) ,
						contentDescription = "Battery Optimization"
					)
			}
				  )
}