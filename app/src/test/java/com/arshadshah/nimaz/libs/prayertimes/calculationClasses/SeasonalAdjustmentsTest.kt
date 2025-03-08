package com.arshadshah.nimaz.libs.prayertimes.calculationClasses
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

internal class SeasonalAdjustmentsTest {
    val seasonalAdjustments =
        SeasonalAdjustments()

    //latitude of dublin
    val latitude = 53.3498

    //day of year
    val day = 1

    //year
    val year = 2020

    //sunrise
    val sunrise = LocalDateTime.of(2020, 1, 1, 8, 0, 0)

    //sunset
    val sunset = LocalDateTime.of(2020, 1, 1, 16, 0, 0)

    @Test
    fun seasonAdjustedMorningTwilight() {
        val result = seasonalAdjustments.seasonAdjustedMorningTwilight(latitude, day, year, sunrise)
        //convert result to string
        val resultString = result.toString()
        assertEquals("2020-01-01T06:18:17", resultString)
    }

    @Test
    fun seasonAdjustedEveningTwilight() {
        val result = seasonalAdjustments.seasonAdjustedEveningTwilight(latitude, day, year, sunset)
        //convert result to string
        val resultString = result.toString()
        assertEquals("2020-01-01T17:37:04", resultString)
    }
}