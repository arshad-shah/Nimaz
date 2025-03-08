package com.arshadshah.nimaz.libs.prayertimes.utils

import com.arshadshah.nimaz.libs.prayertimes.enums.CalculationMethod
import com.arshadshah.nimaz.libs.prayertimes.enums.Madhab
import com.arshadshah.nimaz.libs.prayertimes.enums.ShadowLength
import org.junit.Assert.*
import org.junit.Test

internal class ParametersUtilsTest {

    @Test
    fun getParametersForMethod() {
        val params1 = ParametersUtils.getParametersForMethod(
            CalculationMethod.MWL)
        val params2 = ParametersUtils.getParametersForMethod(
            CalculationMethod.EGYPTIAN)
        val params3 = ParametersUtils.getParametersForMethod(
            CalculationMethod.KARACHI)

        assertEquals(18.0, params1.fajrAngle, 0.01)
        assertEquals(17.0, params1.ishaAngle,0.01)
        assertEquals(CalculationMethod.MWL, params1.method)

        assertEquals(19.5, params2.fajrAngle,0.01)
        assertEquals(17.5, params2.ishaAngle,0.01)
        assertEquals(CalculationMethod.EGYPTIAN, params2.method)

        assertEquals(18.0, params3.fajrAngle,0.01)
        assertEquals(18.0, params3.ishaAngle,0.01)
        assertEquals(CalculationMethod.KARACHI, params3.method)

    }

    @Test
    fun getShadowLengthForMadhab() {
        val shadow1 = ParametersUtils.getShadowLengthForMadhab(
            Madhab.SHAFI)
        val shadow2 = ParametersUtils.getShadowLengthForMadhab(
            Madhab.HANAFI)
        val shadow3 = ParametersUtils.getShadowLengthForMadhab(
            Madhab.SHAFI)
        val shadow4 = ParametersUtils.getShadowLengthForMadhab(
            Madhab.HANAFI)
        val shadow5 = ParametersUtils.getShadowLengthForMadhab(
            Madhab.SHAFI)
        val shadow6 = ParametersUtils.getShadowLengthForMadhab(
            Madhab.HANAFI)

        assertEquals(ShadowLength.SINGLE, shadow1)
        assertEquals(ShadowLength.DOUBLE, shadow2)
        assertEquals(ShadowLength.SINGLE, shadow3)
        assertEquals(ShadowLength.DOUBLE, shadow4)
        assertEquals(ShadowLength.SINGLE, shadow5)
        assertEquals(ShadowLength.DOUBLE, shadow6)
    }
}