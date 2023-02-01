package com.arshadshah.nimaz.utils.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import java.util.*

/**
 * Finds the location from a string using Geocoder
 * @author Arshad Shah
 */
class LocationFinder
{

	// coordinates for the calculation of prayer time
	var latitudeValue = 0.0
	var longitudeValue = 0.0

	// name of city
	var cityName : String = " "


	/**
	 * Finds the longitude and latitude from city name
	 * @author Arshad Shah
	 * @param context context of the application
	 * @param name Name of the city
	 */
	fun findLongAndLan(
		context : Context ,
		name : String ,
		locationFoundCallbackManual : (Double , Double , String) -> Unit
					  )
	{
		val sharedPreferences = PrivateSharedPreferences(context)
		// city name
		if (name == "No Network")
		{
			val isNetworkAvailable = NetworkChecker().networkCheck(context)
			if (isNetworkAvailable)
			{
				val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498)
				val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
				findCityName(context , latitude , longitude , locationFoundCallbackManual)
			} else
			{
				sharedPreferences.saveData(AppConstants.LOCATION_INPUT , "No Network")
			}
		} else
		{
			val gcd = Geocoder(context , Locale.getDefault())

			val isNetworkAvailable = NetworkChecker().networkCheck(context)
			if (isNetworkAvailable)
			{
				try
				{
					val addresses : List<Address> =
						gcd.getFromLocationName(name , 1) as List<Address>
					if (addresses.isNotEmpty())
					{
						cityName = addresses[0].locality
						latitudeValue = addresses[0].latitude
						longitudeValue = addresses[0].longitude
						locationFoundCallbackManual(latitudeValue , longitudeValue , cityName)
						sharedPreferences.saveData(AppConstants.LOCATION_INPUT , cityName)
						sharedPreferences.saveDataDouble(AppConstants.LATITUDE , latitudeValue)
						sharedPreferences.saveDataDouble(AppConstants.LONGITUDE , longitudeValue)

						Log.i("Location" , "Location Found From value $cityName")
					} else
					{
						latitudeValue =
							sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
						longitudeValue =
							sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
						cityName =
							sharedPreferences.getData(AppConstants.LOCATION_INPUT , "Abbeyleix")
						Log.i("Location" , "Location Found From Storage $cityName")
					}
				} catch (e : Exception)
				{
					Log.e("Geocoder" , "Geocoder has failed")
					latitudeValue = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
					longitudeValue = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
					val cityNameFromStorage =
						sharedPreferences.getData(AppConstants.LOCATION_INPUT , "Abbeyleix")
					cityName = cityNameFromStorage
					Log.i("Location" , "Location Found From Storage $cityName")
				}
			} else
			{
				latitudeValue = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
				longitudeValue = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
				cityName = sharedPreferences.getData(AppConstants.LOCATION_INPUT , "Abbeyleix")
				Log.i("Location" , "Location Found From Storage $cityName")
			}
		}
	}


	/**
	 * Finds the longitude and latitude from city name
	 * @author Arshad Shah
	 * @param context context of the application
	 */
	fun findCityName(
		context : Context ,
		latitude : Double ,
		longitude : Double ,
		locationFoundCallbackManual : (Double , Double , String) -> Unit
					)
	{
		// city name
		val gcd = Geocoder(context , Locale.getDefault())
		val sharedPreferences = PrivateSharedPreferences(context)
		val isNetworkAvailable = NetworkChecker().networkCheck(context)
		if (isNetworkAvailable)
		{
			try
			{
				val addresses : List<Address> =
					gcd.getFromLocation(latitude , longitude , 1) as List<Address>
				if (addresses.isNotEmpty())
				{
					cityName = addresses[0].locality
					latitudeValue = latitude
					longitudeValue = longitude
					locationFoundCallbackManual(latitudeValue , longitudeValue , cityName)
					sharedPreferences.saveData(AppConstants.LOCATION_INPUT , cityName)

					Log.i("Location" , "Location Found From value $latitude, and $longitude")
				} else
				{
					latitudeValue = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
					longitudeValue = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
					cityName = sharedPreferences.getData(AppConstants.LOCATION_INPUT , "Abbeyleix")
					Log.i("Location" , "Location Found From Storage $cityName")
				}
			} catch (e : Exception)
			{
				Log.e("Geocoder" , "Geocoder has failed")
				latitudeValue = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
				longitudeValue = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
				val cityNameFromStorage =
					sharedPreferences.getData(AppConstants.LOCATION_INPUT , "Abbeyleix")

				cityName = cityNameFromStorage
				Log.i("Location" , "Location Found From value $latitude, and $longitude")
			}
		} else
		{
			latitudeValue = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
			longitudeValue = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
			cityName = sharedPreferences.getData(AppConstants.LOCATION_INPUT , "Abbeyleix")
			Log.i("Location" , "Location Found From Storage $cityName")
		}
	}
}
