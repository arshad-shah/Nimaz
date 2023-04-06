package com.arshadshah.nimaz.utils.sunMoonUtils

import android.content.Context
import android.util.Log
import java.util.*
import kotlin.math.roundToInt

class AutoAnglesCalc
{

	fun calculateFajrAngle(context : Context , latitude : Double , longitude : Double) : Int
	{

		val timesFromNew = SunAngles.getTimes(Date() , latitude , longitude)
		val sunPositionAtFajrNew =
			SunAngles.getPosition(timesFromNew["nauticalDawn"] !! , latitude , longitude)
		val sunPositionAtIsaaNew =
			SunAngles.getPosition(timesFromNew["nauticalDusk"] !! , latitude , longitude)
		val altitudeInDegreesFajrNew =
			Math.toDegrees(sunPositionAtFajrNew["altitude"] !!).roundToInt()
		val altitudeInDegreesIsaaNew =
			Math.toDegrees(sunPositionAtIsaaNew["altitude"] !!).roundToInt()

		Log.d("AutoAnglesCalc" , "Altitude Fajr: ${altitudeInDegreesFajrNew - 3}")
		Log.d("AutoAnglesCalc" , "Altitude Isaa: ${altitudeInDegreesIsaaNew - 3}")
		Log.d("AutoAnglesCalc" , "timesForIsaa: ${timesFromNew["nauticalDusk"]}")
		Log.d("AutoAnglesCalc" , "timesForFajr: ${timesFromNew["nauticalDawn"]}")

		//all the times
		Log.d("AutoAnglesCalc" , "times: $timesFromNew")

		//make the angle into positive


		return (altitudeInDegreesFajrNew - 3) * - 1
	}

	fun calculateIshaaAngle(context : Context , latitude : Double , longitude : Double) : Int
	{
		val timesFromNew = SunAngles.getTimes(Date() , latitude , longitude)
		val sunPositionAtIsaaNew =
			SunAngles.getPosition(timesFromNew["nauticalDusk"] !! , latitude , longitude)
		val altitudeInDegreesIsaaNew =
			Math.toDegrees(sunPositionAtIsaaNew["altitude"] !!).roundToInt()
		return (altitudeInDegreesIsaaNew - 3) * - 1
	}


}