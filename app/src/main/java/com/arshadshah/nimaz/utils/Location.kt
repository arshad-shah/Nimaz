package com.arshadshah.nimaz.utils

import android.content.Context
import com.arshadshah.nimaz.constants.AppConstants.AUTO_LOCATION_PERMISSION_REQUEST_CODE
import com.arshadshah.nimaz.utils.location.LocationFinder
import com.arshadshah.nimaz.utils.location.LocationFinderAuto

class Location
{

	//function to get automatic location
	fun getAutomaticLocation(
		context : Context ,
		listener : (Latitude : Double , Longitude : Double) -> Unit ,
							)
	{
		//use locationfinderauto
		val locationFinderAuto = LocationFinderAuto()
		//get the location
		locationFinderAuto.getLocations(
				context ,
				requestCode = AUTO_LOCATION_PERMISSION_REQUEST_CODE ,
				listener = listener
									   )
	}

	fun getManualLocation(
		name : String ,
		context : Context ,
		locationFoundCallbackManual : (Double , Double , String) -> Unit ,
						 )
	{
		//use locationfinderauto
		val locationFinderManual = LocationFinder()
		//get the location
		locationFinderManual.findLongAndLan(
				context = context ,
				name = name ,
				locationFoundCallbackManual = locationFoundCallbackManual
										   )
	}
}