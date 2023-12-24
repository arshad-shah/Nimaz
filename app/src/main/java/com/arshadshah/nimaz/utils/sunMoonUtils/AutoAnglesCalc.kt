package com.arshadshah.nimaz.utils.sunMoonUtils

import android.content.Context
import android.util.Log
import com.arshadshah.nimaz.utils.sunMoonUtils.sun.SunAngles
import java.util.Date
import kotlin.math.roundToInt

class AutoAnglesCalc {

    fun calculateFajrAngle(context: Context, latitude: Double, longitude: Double): Int {

        val timesFromNew = SunAngles.getTimes(Date(), latitude, longitude)
        val sunPositionAtFajrNew =
            SunAngles.getPosition(timesFromNew["nauticalDawn"]!!, latitude, longitude)
        val altitudeInDegreesFajrNew =
            Math.toDegrees(sunPositionAtFajrNew["altitude"]!!).roundToInt()
        //all the times
        Log.d("AutoAnglesCalc", "times: $timesFromNew")

        return (altitudeInDegreesFajrNew - 3) * -1
    }

    fun calculateIshaaAngle(context: Context, latitude: Double, longitude: Double): Int {
        val timesFromNew = SunAngles.getTimes(Date(), latitude, longitude)
        val sunPositionAtIsaaNew =
            SunAngles.getPosition(timesFromNew["nauticalDusk"]!!, latitude, longitude)
        val altitudeInDegreesIsaaNew =
            Math.toDegrees(sunPositionAtIsaaNew["altitude"]!!).roundToInt()
        return (altitudeInDegreesIsaaNew - 3) * -1
    }


}