package com.arshadshah.nimaz.utils

import android.content.Context
import com.arshadshah.nimaz.utils.location.LocationFinder
import com.arshadshah.nimaz.utils.location.LocationFinderAuto

class Location
{

	//function to get automatic location
	fun getAutomaticLocation(context : Context)
	{
		//get laitude and longitude from private shared preferences
		val sharedPreferences = PrivateSharedPreferences(context)
		//use locationfinderauto
		val locationFinderAuto = LocationFinderAuto()
		//get the location
		locationFinderAuto.getLocations(context , requestCode = 1)

		val latitude = sharedPreferences.getDataDouble("latitude" , 53.3498)
		val longitude = sharedPreferences.getDataDouble("longitude" , - 6.2603)
		//get the location name
		val locationFinder = LocationFinder()
		locationFinder.findCityName(context , latitude = latitude , longitude = longitude)
	}

	fun getManualLocation(name : String , context : Context)
	{
		//use locationfinderauto
		val locationFinderManual = LocationFinder()
		//get the location
		locationFinderManual.findLongAndLan(context = context , name = name)
	}
}