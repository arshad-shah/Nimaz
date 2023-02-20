package com.arshadshah.nimaz.ui.components.ui.intro

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.SettingsSwitch

@SuppressLint("BatteryLife")
@Composable
fun BatteryExemptionUI()
{
	val context = LocalContext.current

	val viewModel = viewModel(key = "SettingsViewModel", initializer = { SettingsViewModel(context) }, viewModelStoreOwner = context as ComponentActivity)
	val isBatteryExempt = remember {
		viewModel.isBatteryExempt
	}.collectAsState()

	val lifecycle = LocalLifecycleOwner.current.lifecycle

	//battery optimization exemption
	val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
	val isChecked =
		remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }

	//the state of the switch
	val state = rememberPreferenceBooleanSettingState(
			AppConstants.BATTERY_OPTIMIZATION ,
			isChecked.value
													 )

	DisposableEffect(lifecycle) {
		val observer = LifecycleEventObserver { _ , event ->
			when (event)
			{
				Lifecycle.Event.ON_RESUME ->
				{
					viewModel.handleEvent(
							SettingsViewModel.SettingsEvent.BatteryExempt(
									powerManager.isIgnoringBatteryOptimizations(
											context.packageName
																			   )
																		 )
										 )
					state.value = powerManager.isIgnoringBatteryOptimizations(context.packageName)
					isChecked.value = powerManager.isIgnoringBatteryOptimizations(context.packageName)
				}

				else ->
				{

				}
			}
		}

		lifecycle.addObserver(observer)
		onDispose {
			lifecycle.removeObserver(observer)
		}
	}


	SettingsSwitch(
			state = state ,
			onCheckedChange = {
				if (it)
				{
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
				}
			} ,
			title = {
				Text(text = "Battery Optimization")
			} ,
			subtitle = {
				//if the permission is granted, show a checkmark and text saying "Allowed"
				if (isBatteryExempt.value)
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