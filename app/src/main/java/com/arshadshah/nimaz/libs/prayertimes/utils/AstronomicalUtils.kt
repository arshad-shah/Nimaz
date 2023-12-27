package com.arshadshah.nimaz.libs.prayertimes.utils

internal object AstronomicalUtils {
    /*
* Interpolation of a value given equidistant previous and next values and a
* factor equal to the fraction of the interpolated point's time over the time
* between values.
*/
    /**
     * Interpolation of a value given equidistant previous and next values and a
     * factor equal to the fraction of the interpolated point's time over the time
     * between values.
     * Not Accounting for angle unwinding
     * @param value the value
     * @param prevValue the previous value
     * @param nextValue the next value
     * @param factor  the factor
     * @return the interpolated value
     */
    fun interpolate(value: Double, prevValue: Double, nextValue: Double, factor: Double): Double {
        /* Equation from Astronomical Algorithms page 24 */
        val term1 = value - prevValue
        val term2 = nextValue - value
        val term3 = term2 - term1
        return value + factor / 2 * (term1 + term2 + factor * term3)
    }

    /**
     * Interpolation of a value given equidistant previous and next values and a
     * factor equal to the fraction of the interpolated point's time over the time
     * accounting for angle unwinding
     *
     * @param value the value
     * @param prevValue the previous value
     * @param nextValue the next value
     * @param factor  the factor
     * @return interpolated angle
     */
    fun interpolateAngles(
        value: Double,
        prevValue: Double,
        nextValue: Double,
        factor: Double
    ): Double {
        /* Equation from Astronomical Algorithms page 24 */
        val term1 = DoubleUtil.unwindAngle(value - prevValue)
        val term2 = DoubleUtil.unwindAngle(nextValue - value)
        val term3 = term2 - term1
        return value + factor / 2 * (term1 + term2 + factor * term3)
    }
}