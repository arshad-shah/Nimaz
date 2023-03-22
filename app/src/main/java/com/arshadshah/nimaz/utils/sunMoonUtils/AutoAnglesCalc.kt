package com.arshadshah.nimaz.utils.sunMoonUtils

import android.content.Context
import android.util.Log
import kotlin.math.roundToInt

class AutoAnglesCalc
{
	private lateinit var sunCalc: SunMoonCalc
	fun calculateFajrAngle(context: Context , latitude: Double , longitude: Double): Int{
		sunCalc = SunMoonCalc(latitude , longitude, context)
		val times = sunCalc.getTimes()
		val sunPositionAtFajr = sunCalc.getSunPositionForDate(times.nightEnd)

		Log.d("Nimaz: time sunrise" , times.sunrise.toString())
		Log.d("Nimaz: time sunriseEnd" , times.sunriseEnd.toString())
		Log.d("Nimaz: time goldenHour" , times.goldenHour.toString())
		Log.d("Nimaz: time goldenHourEnd" , times.goldenHourEnd.toString())
		Log.d("Nimaz: time solarNoon" , times.solarNoon.toString())
		Log.d("Nimaz: time sunsetStart" , times.sunsetStart.toString())
		Log.d("Nimaz: time sunset" , times.sunset.toString())
		Log.d("Nimaz: time dusk" , times.dusk.toString())
		Log.d("Nimaz: time nauticalDusk" , times.nauticalDusk.toString())
		Log.d("Nimaz: time night" , times.night.toString())
		Log.d("Nimaz: time nightEnd" , times.nightEnd.toString())
		Log.d("Nimaz: time nadir" , times.nadir.toString())
		Log.d("Nimaz: time nauticalDawn" , times.nauticalDawn.toString())
		Log.d("Nimaz: time dawn" , times.dawn.toString())



		val altitudeInDegreesFajr = Math.toDegrees(sunPositionAtFajr.altitude).roundToInt()
		return altitudeInDegreesFajr
	}

	fun calculateIshaaAngle(context: Context , latitude: Double , longitude: Double): Int{
		sunCalc = SunMoonCalc(latitude , longitude, context)
		val times = sunCalc.getTimes()
		val sunPositionAtIshaa = sunCalc.getSunPositionForDate(times.dusk)

		val altitudeInDegreesIshaa = Math.toDegrees(sunPositionAtIshaa.altitude).roundToInt()
		return altitudeInDegreesIshaa
	}
}