package com.arshadshah.nimaz.utils.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.core.app.ComponentActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.android.gms.location.*

class LocationFinderAuto
{

	var fusedLocationProviderClient : FusedLocationProviderClient? = null
	var locationCallback : LocationCallback? = null

	/**
	 * make the location callback object to be given to fusedLocationProviderClient
	 */
	private fun getLocationCallback(
		context : Context ,
		listener : (Latitude : Double , Longitude : Double) -> Unit
								   )
	{
		//get the preferences file for the app
		val sharedPreferences = PrivateSharedPreferences(context)
		locationCallback = object : LocationCallback()
		{
			override fun onLocationResult(p0 : LocationResult)
			{
				for (location in p0.locations)
				{
					//get the latitude and longitude
					val latitude = location.latitude
					val longitude = location.longitude
					//save the latitude and longitude to the shared preferences
					sharedPreferences.saveDataDouble(AppConstants.LATITUDE , latitude)
					sharedPreferences.saveDataDouble(AppConstants.LONGITUDE , longitude)
					//call the listener
					listener(latitude , longitude)
				}
			}
		}
	}


	@SuppressLint("MissingPermission")
	private fun setUpLocationListener(
		context : Context ,
		listener : (Latitude : Double , Longitude : Double) -> Unit
									 )
	{
		getLocationCallback(context, listener)
		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
		val locationRequest =
			LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY , 72000).build()
		fusedLocationProviderClient !!.requestLocationUpdates(
				locationRequest ,
				locationCallback !! ,
				Looper.getMainLooper()
															 )
	}

	fun getLocations(
		context : Context ,
		requestCode : Int ,
		listener : (Latitude : Double , Longitude : Double) -> Unit
					)
	{
		when
		{
			PermissionUtils.checkAccessLocationGranted(context) ->
			{
				when
				{
					PermissionUtils.isLocationEnabled(context) ->
					{
						setUpLocationListener(context, listener)
					}

					else ->
					{
						PermissionUtils.showGPSNotEnabledDialog(context)
					}
				}
			}

			else ->
			{
				PermissionUtils.askAccessLocationPermission(
						context as ComponentActivity ,
						requestCode
														   )
			}
		}
	}

	fun stopLocationUpdates()
	{
		fusedLocationProviderClient?.removeLocationUpdates(locationCallback !!)
	}
}