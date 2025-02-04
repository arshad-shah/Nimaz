package com.arshadshah.nimaz.libs.prayertimes.utils

import java.time.LocalDateTime
import kotlin.math.floor

class TimeUtils(
    private val hours: Int,
    private val minutes: Int,
    private val seconds: Int
) {

    //a function to return date for a time given in hours, minutes and seconds
    //it sets the calender date to date parameter and the time for that date to the time given in the constructor
    //allowing for setting time for the 6 points of day

    fun date(date: LocalDateTime): LocalDateTime {
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        val hour = hours
        val minute = minutes
        val second = seconds
        return try {
            LocalDateTime.of(year, month, day, hour, minute, second)
        } catch (e: Exception) {
            LocalDateTime.of(year, month, day, 0, 0, 0)
        }
    }

    companion object {

        @JvmStatic
        fun fromDouble(value: Double): TimeUtils? {
            if (java.lang.Double.isInfinite(value) || java.lang.Double.isNaN(value)) {
                return null
            }
            val hours = floor(value)
            val minutes = floor((value - hours) * 60.0)
            val seconds = floor((value - (hours + minutes / 60.0)) * 60 * 60)
            return TimeUtils(
                hours.toInt(),
                minutes.toInt(),
                seconds.toInt()
            )
        }
    }
}