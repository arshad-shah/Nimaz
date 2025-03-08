package com.arshadshah.nimaz.libs.prayertimes.calculationClasses

import com.arshadshah.nimaz.libs.prayertimes.utils.CalenderUtils
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

class SeasonalAdjustments {
    private val calenderUtils =
        CalenderUtils()

    fun seasonAdjustedMorningTwilight(
        latitude: Double,
        day: Int,
        year: Int,
        sunrise: LocalDateTime?
    ): LocalDateTime {
        val a = 75 + 28.65 / 55.0 * abs(latitude)
        val b = 75 + 19.44 / 55.0 * abs(latitude)
        val c = 75 + 32.74 / 55.0 * abs(latitude)
        val d = 75 + 48.10 / 55.0 * abs(latitude)
        val adjustment: Double
        val dyy = daysSinceSolstice(day, year, latitude)
        adjustment = when {
            dyy < 91 -> {
                a + (b - a) / 91.0 * dyy
            }

            dyy < 137 -> {
                b + (c - b) / 46.0 * (dyy - 91)
            }

            dyy < 183 -> {
                c + (d - c) / 46.0 * (dyy - 137)
            }

            dyy < 229 -> {
                d + (c - d) / 46.0 * (dyy - 183)
            }

            dyy < 275 -> {
                c + (b - c) / 46.0 * (dyy - 229)
            }

            else -> {
                b + (a - b) / 91.0 * (dyy - 275)
            }
        }
        return sunrise!!.plus(-(adjustment * 60.0).roundToInt().toLong(), ChronoUnit.SECONDS)
    }

    fun seasonAdjustedEveningTwilight(
        latitude: Double,
        day: Int,
        year: Int,
        sunset: LocalDateTime?
    ): LocalDateTime {
        val a = 75 + 25.60 / 55.0 * abs(latitude)
        val b = 75 + 2.050 / 55.0 * abs(latitude)
        val c = 75 - 9.210 / 55.0 * abs(latitude)
        val d = 75 + 6.140 / 55.0 * abs(latitude)
        val adjustment: Double
        val dyy = daysSinceSolstice(day, year, latitude)
        adjustment = when {
            dyy < 91 -> {
                a + (b - a) / 91.0 * dyy
            }

            dyy < 137 -> {
                b + (c - b) / 46.0 * (dyy - 91)
            }

            dyy < 183 -> {
                c + (d - c) / 46.0 * (dyy - 137)
            }

            dyy < 229 -> {
                d + (c - d) / 46.0 * (dyy - 183)
            }

            dyy < 275 -> {
                c + (b - c) / 46.0 * (dyy - 229)
            }

            else -> {
                b + (a - b) / 91.0 * (dyy - 275)
            }
        }
        return sunset!!.plus((adjustment * 60.0).roundToInt().toLong(), ChronoUnit.SECONDS)
    }

    private fun daysSinceSolstice(dayOfYear: Int, year: Int, latitude: Double): Int {
        var daysSinceSolistice: Int
        val northernOffset = 10
        val isLeapYear = calenderUtils.isLeapYear(year)
        val southernOffset = if (isLeapYear) 173 else 172
        val daysInYear = if (isLeapYear) 366 else 365
        if (latitude >= 0) {
            daysSinceSolistice = dayOfYear + northernOffset
            if (daysSinceSolistice >= daysInYear) {
                daysSinceSolistice -= daysInYear
            }
        } else {
            daysSinceSolistice = dayOfYear - southernOffset
            if (daysSinceSolistice < 0) {
                daysSinceSolistice += daysInYear
            }
        }
        return daysSinceSolistice
    }
}