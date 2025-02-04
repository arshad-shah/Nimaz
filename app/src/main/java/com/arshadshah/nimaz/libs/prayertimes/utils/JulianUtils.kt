package com.arshadshah.nimaz.libs.prayertimes.utils

/**
 * A class that calculates the julian day of current date
 * takes in Year int, Month int, Day int
 * */
internal object JulianUtils {

    /**
     * Calculates the julian day of current date
     * */
    fun calculateJulianDay(year: Int, month: Int, day: Int): Double {
        val yearChecked = if (month == 1 || month == 2) year - 1 else year
        val monthChecked = if (month == 1 || month == 2) month + 12 else month
        val B = -13 // this is same for 3 more centuries so we dont have to calculate this
        val term1 = (365.25 * yearChecked).toInt()
        val term2 = (30.6001 * (monthChecked + 1)).toInt()
        val term3 = day + B
        val term4 = 1720994.5   //365.25*4716-1524.5

        return term1 + term2 + term3 + term4
    }

    //calculate julian century
    fun calculateJulianCentury(julianDayVal: Double): Double {
        return (julianDayVal - 2451545.0) / 36525.0
    }
}