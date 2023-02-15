package com.arshadshah.nimaz.utils.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import com.arshadshah.nimaz.R
import com.google.accompanist.permissions.*


object PermissionUtils
{

	fun askAccessLocationPermission(activity : ComponentActivity , requestId : Int)
	{
		//ask for permission
		if (! checkAccessLocationGranted(activity.applicationContext))
		{
			//permission not granted
			if (activity.shouldShowRequestPermissionRationale(
						Manifest.permission.ACCESS_COARSE_LOCATION
															 )
			)
			{
				//show dialog
				showPermissionDialog(activity.applicationContext , requestId)
			} else
			{
				//ask for permission
				activity.requestPermissions(
						arrayOf(
								Manifest.permission.ACCESS_COARSE_LOCATION
							   ) ,
						requestId
										   )
			}
		}
	}

	fun checkAccessLocationGranted(context : Context) : Boolean
	{
		var isGranted = false

		val coarsePerms = ContextCompat
			.checkSelfPermission(
					context ,
					Manifest.permission.ACCESS_COARSE_LOCATION
								) == PackageManager.PERMISSION_GRANTED

		if (coarsePerms)
		{
			isGranted = true
		}

		return isGranted
	}

	fun isLocationEnabled(context : Context) : Boolean
	{
		val locationManager : LocationManager =
			context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
				|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
	}

	fun showGPSNotEnabledDialog(context : Context)
	{
		AlertDialog.Builder(context)
			.setTitle(context.getString(R.string.gps_not_enabled))
			.setMessage(context.getString(R.string.required_for_this_app))
			.setCancelable(false)
			.setPositiveButton(context.getString(R.string.enable_now)) { _ , _ ->
				context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
			}
			.setNegativeButton(context.getString(R.string.cancel)) { dialog , _ ->
				dialog.dismiss()
			}
			.show()
	}


	private fun showPermissionDialog(context : Context , requestId : Int)
	{
		//show toast that location access is not granted
		Toast.makeText(context , "Location access is not granted" , Toast.LENGTH_SHORT).show()

		AlertDialog.Builder(context)
			.setTitle(context.getString(R.string.permission_required))
			.setMessage(context.getString(R.string.Location_required_for_this_app))
			.setPositiveButton(context.getString(R.string.AllowPerms)) { _ , _ ->
				//ask for permission
				ActivityCompat.requestPermissions(
						context as AppCompatActivity ,
						arrayOf(
								Manifest.permission.ACCESS_FINE_LOCATION ,
								Manifest.permission.ACCESS_COARSE_LOCATION
							   ) ,
						requestId
												 )
			}
			.setNegativeButton(context.getString(R.string.cancel)) { dialog , _ ->
				dialog.dismiss()
			}
			.show()
	}
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FeatureThatRequiresLocationPermission(
	locationPermissionState : MultiplePermissionsState ,
	checked : MutableState<Boolean> ,
										 )
{

	val descToShow = remember { mutableStateOf("") }
	val showRationale = remember { mutableStateOf(false) }
	//check if location permission is granted
	if (locationPermissionState.allPermissionsGranted)
	{
		checked.value = true
	} else
	{
		if (locationPermissionState.shouldShowRationale)
		{
			showRationale.value = true
			descToShow.value =
				"Location permission is required to get accurate prayer times. Please allow location permission."

		} else
		{
			showRationale.value = true
			descToShow.value =
				"Location permission is required to get accurate prayer times the system will revert to using manual location without updates."
		}
	}

	if (showRationale.value)
	{
		//permission not granted
		//show dialog
		AlertDialog(
				onDismissRequest = {
					locationPermissionState.launchMultiplePermissionRequest()
					showRationale.value = false
								   } ,
				title = { Text(text = "Location Permission Required") } ,
				text = { Text(text = descToShow.value) } ,
				confirmButton = {
					Button(onClick = {
						locationPermissionState.launchMultiplePermissionRequest()
						showRationale.value = false
					}) {
						Text(text = "Allow")
					}
				} ,
				dismissButton = {
					Button(onClick = {
						locationPermissionState.launchMultiplePermissionRequest()
						showRationale.value = false
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
			showRationale.value = false
			descToShow.value =
				"Notification permission is required to deliver adhan notifications, Nimaz will not be able to show adhan notifications if it is denied."
		}
	}

	if (showRationale.value)
	{
		//permission not granted
		//show dialog
		AlertDialog(
				onDismissRequest = { notificationPermissionState.launchPermissionRequest() } ,
				title = { Text(text = "Notification Permission Required") } ,
				text = { Text(text = descToShow.value) } ,
				confirmButton = {
					Button(onClick = { notificationPermissionState.launchPermissionRequest() }) {
						Text(text = "Allow")
					}
				} ,
				dismissButton = {
					Button(onClick = { notificationPermissionState.launchPermissionRequest() }) {
						Text(text = "Cancel")
					}
				}
				   )
	}

}