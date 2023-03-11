package com.arshadshah.nimaz.utils.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

object AutoLocationUtils
{

	//single instance of location client
	private var fusedLocationProviderClient : FusedLocationProviderClient? = null

	fun init(context : Context)
	{
		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
		Log.d("Nimaz: Location" , "Location Client initialized")
	}

	private const val ONE_MINUTE = 60 * 1000

	val locationRequest : LocationRequest = LocationRequest.Builder(
			Priority.PRIORITY_HIGH_ACCURACY ,
			ONE_MINUTE.toLong()
																   ).build()

	//a single instance of location callback
	private val locationCallback = object : LocationCallback()
	{
		override fun onLocationResult(p0 : LocationResult)
		{
			super.onLocationResult(p0)
			locationRequest
			for (location in p0.locations)
			{
				setLocationData(location)
				Log.d(
						"Nimaz: Location Auto" ,
						"Latitude: ${location.latitude} Longitude: ${location.longitude}"
					 )
			}
		}
	}

	//a callback to set the location data that is accessed by the viewmodel to get the location
	private var setLocationData : (Location) -> Unit = {}

	fun setLocationDataCallback(callback : (Location) -> Unit)
	{
		setLocationData = callback
	}


	@SuppressLint("MissingPermission")
	fun startLocationUpdates()
	{
		fusedLocationProviderClient?.requestLocationUpdates(
				locationRequest ,
				locationCallback ,
				Looper.getMainLooper()
														  )
		Log.w("Nimaz: Location" , "Location Updates Started")
	}

	fun stopLocationUpdates()
	{
		fusedLocationProviderClient?.removeLocationUpdates(
				locationCallback
														 )?.addOnCompleteListener {
			Log.w("Nimaz: Location" , "Location Updates Stopped")
		}
		//set client to null
		fusedLocationProviderClient = null
	}
}