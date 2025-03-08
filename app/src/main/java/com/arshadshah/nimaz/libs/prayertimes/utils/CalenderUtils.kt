package com.arshadshah.nimaz.libs.prayertimes.utils

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoField

class CalenderUtils {

    /**
     * To check if a year is a leap year or not
     * @param year the year to check
     * @return true if the year is a leap year, false otherwise
     * */
    fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && year % 100 != 0 || year % 400 == 0
    }

    /**
     * Date and time with a rounded minute
     * This returns a date with the seconds rounded and added to the minute
     * @param date the date and time to be rounded
     * @return the date and time with 0 seconds and minutes including rounded seconds
     */
    fun roundedMinute(date: LocalDateTime): LocalDateTime {
        val localTimeHour = date.get(ChronoField.HOUR_OF_DAY)
        val localTimeMinute = LocalTime.from(date).get(ChronoField.MINUTE_OF_HOUR)
        val localTimeSecond = LocalTime.from(date).get(ChronoField.SECOND_OF_MINUTE)
        val localTimeAndDate = LocalDateTime.of(
            date.year,
            date.month,
            date.dayOfMonth,
            localTimeHour,
            localTimeMinute,
            localTimeSecond
        )
        val roundedSeconds = localTimeAndDate.second / 60.0
        val roundedMinutes = localTimeAndDate.minute + roundedSeconds
        return localTimeAndDate.withMinute(roundedMinutes.toInt()).withSecond(0)

    }


    /**
     * Gets a date for the particular date
     * @param year the year
     * @param month the month
     * @param day the day
     * @return the date with a time set to 00:00:00 at utc
     */
    fun resolveTime(year: Int, month: Int, day: Int): LocalDateTime {
        return LocalDateTime.of(year, month, day, 0, 0, 0)
    }

    //convert localDate to milliseconds
    fun toMillis(date: LocalDateTime): Long {
        return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}