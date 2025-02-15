package com.arshadshah.nimaz.libs.prayertimes.calculationClasses

import com.arshadshah.nimaz.libs.prayertimes.enums.CalculationMethod
import com.arshadshah.nimaz.libs.prayertimes.enums.HighLatitudeRule
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationParametersTest {

    //variables
    private val fajrAngle = 18.0
    private val ishaAngle = 17.0
    private val method = CalculationMethod.OTHER
    private val highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT

    @Test
    fun getHighLatitudeRule() {
        val calculationParameters =
            CalculationParameters(
                fajrAngle,
                ishaAngle,
                method
            )
        assertEquals(highLatitudeRule, calculationParameters.highLatitudeRule)
    }

    @Test
    fun setHighLatitudeRule() {
        val calculationParameters =
            CalculationParameters(
                fajrAngle,
                ishaAngle,
                method
            )
        calculationParameters.highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        assertEquals(HighLatitudeRule.SEVENTH_OF_THE_NIGHT, calculationParameters.highLatitudeRule)
    }

    @Test
    fun nightPortions() {
        val calculationParameters =
            CalculationParameters(
                fajrAngle,
                ishaAngle,
                method
            )
        calculationParameters.highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        val nightPortions = calculationParameters.nightPortions()
        assertEquals(1.0 / 2.0, nightPortions.fajr, 0.01)

        calculationParameters.highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        val nightPortions2 = calculationParameters.nightPortions()
        assertEquals(1.0 / 7.0, nightPortions2.fajr,0.01)

        calculationParameters.highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE
        val nightPortions3 = calculationParameters.nightPortions()
        assertEquals(fajrAngle / 60.0, nightPortions3.fajr,0.01)
    }

    @Test
    fun getFajrAngle() {
        val calculationParameters =
            CalculationParameters(
                fajrAngle,
                ishaAngle,
                method
            )
        assertEquals(fajrAngle, calculationParameters.fajrAngle,0.01)
    }

    @Test
    fun setFajrAngle() {
        val calculationParameters =
            CalculationParameters(
                fajrAngle,
                ishaAngle,
                method
            )
        calculationParameters.fajrAngle = 19.0
        assertEquals(19.0, calculationParameters.fajrAngle,0.01)
    }

    @Test
    fun getIshaAngle() {
        val calculationParameters =
            CalculationParameters(
                fajrAngle,
                ishaAngle,
                method
            )
        assertEquals(ishaAngle, calculationParameters.ishaAngle,0.01)
    }

    @Test
    fun setIshaAngle() {
        val calculationParameters =
            CalculationParameters(
                fajrAngle,
                ishaAngle,
                method
            )
        calculationParameters.ishaAngle = 18.0
        assertEquals(18.0, calculationParameters.ishaAngle,0.01)
    }
}