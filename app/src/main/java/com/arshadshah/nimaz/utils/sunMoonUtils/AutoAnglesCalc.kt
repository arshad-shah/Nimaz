package com.arshadshah.nimaz.utils.sunMoonUtils

import android.content.Context
import kotlin.math.roundToInt

class AutoAnglesCalc
{
	private lateinit var sunCalc: SunMoonCalc
	fun calculateFajrAngle(context: Context , latitude: Double , longitude: Double): Int{
		sunCalc = SunMoonCalc(latitude , longitude)
		val times = sunCalc.getTimes()
		val sunPositionAtFajr = sunCalc.getSunPositionForTime(times.nightEnd)

		val altitudeInDegreesFajr = Math.toDegrees(sunPositionAtFajr.altitude).roundToInt()
		return altitudeInDegreesFajr
	}

	fun calculateIshaaAngle(context: Context , latitude: Double , longitude: Double): Int{
		sunCalc = SunMoonCalc(latitude , longitude)
		val times = sunCalc.getTimes()
		val sunPositionAtIshaa = sunCalc.getSunPositionForTime(times.dusk)

		val altitudeInDegreesIshaa = Math.toDegrees(sunPositionAtIshaa.altitude).roundToInt()
		return altitudeInDegreesIshaa
	}




}