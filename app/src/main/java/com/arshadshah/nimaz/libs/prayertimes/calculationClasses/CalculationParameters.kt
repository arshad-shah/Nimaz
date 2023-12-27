package com.arshadshah.nimaz.libs.prayertimes.calculationClasses

import com.arshadshah.nimaz.libs.prayertimes.enums.CalculationMethod
import com.arshadshah.nimaz.libs.prayertimes.enums.HighLatitudeRule
import com.arshadshah.nimaz.libs.prayertimes.enums.Madhab
import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import com.arshadshah.nimaz.libs.prayertimes.objects.NightPortions
import com.arshadshah.nimaz.libs.prayertimes.objects.PrayerAdjustments

/**
 * Parameters used for PrayerTime calculation customization
 */
class CalculationParameters
/**
 * Generate CalculationParameters from angles
 *
 * @param fajrAngle the angle for calculating fajr
 * @param ishaAngle the angle for calculating isha
 */(
    /**
     * The angle of the sun used to calculate fajr
     */
    var fajrAngle: Double,
    /**
     * The angle of the sun used to calculate isha
     */
    var ishaAngle: Double
) {

    /**
     * The method used to do the calculation
     */
    @JvmField
    var method = CalculationMethod.OTHER

    /**
     * Minutes after Maghrib (if set, the time for Isha will be Maghrib plus
     * IshaInterval)
     */
    @JvmField
    var ishaInterval = 0

    /**
     * The madhab used to calculate Asr
     */
    @JvmField
    var madhab = Madhab.SHAFI

    /**
     * Rules for placing bounds on Fajr and Isha for high latitude areas
     */
    var highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT

    /**
     * Used to optionally add or subtract a set amount of time from each prayer time
     */
    @JvmField
    var adjustments = PrayerAdjustments()

    /**
     * Coordinates that this calculation covers
     */
    @JvmField
    var coordinates: Coordinates? = null

    /**
     * Generate CalculationParameters from fajr angle and isha interval
     *
     * @param fajrAngle    the angle for calculating fajr
     * @param ishaInterval the amount of time after maghrib to have isha
     */
    constructor(fajrAngle: Double, ishaInterval: Int) : this(fajrAngle, 0.0) {
        this.ishaInterval = ishaInterval
    }

    /**
     * Generate CalculationParameters from angles and a calculation method
     *
     * @param fajrAngle the angle for calculating fajr
     * @param ishaAngle the angle for calculating isha
     * @param method    the calculation method to use
     */
    constructor(
        fajrAngle: Double,
        ishaAngle: Double,
        method: CalculationMethod
    ) : this(
        fajrAngle,
        ishaAngle
    ) {
        this.method = method
    }

    /**
     * Generate CalculationParameters from fajr angle, isha interval, and
     * calculation method
     *
     * @param fajrAngle    the angle for calculating fajr
     * @param ishaInterval the amount of time after maghrib to have isha
     * @param method       the calculation method to use
     */
    constructor(
        fajrAngle: Double,
        ishaInterval: Int,
        method: CalculationMethod
    ) : this(
        fajrAngle,
        ishaInterval
    ) {
        this.method = method
    }

    /**
     * Set the method adjustments for the current calculation parameters
     *
     * @param adjustments the prayer adjustments
     * @return this calculation parameters instance
     */
    fun withAdjustments(adjustments: PrayerAdjustments): CalculationParameters {
        this.adjustments = adjustments
        return this
    }

    /**
     * Set the coordinates for the current calculation parameters
     *
     * @param coordinates the coordinates
     * @return this calculation parameters instance
     */
    fun withCoordinates(coordinates: Coordinates): CalculationParameters {
        this.coordinates = coordinates
        return this
    }

    /**
     * Set the madhab for the current calculation parameters
     *
     * @param madhab the madhab
     * @return this calculation parameters instance
     */
    fun withMadhab(madhab: Madhab): CalculationParameters {
        this.madhab = madhab
        return this
    }

    fun nightPortions(): NightPortions {
        return when (highLatitudeRule) {
            HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> {
                NightPortions(1.0 / 2.0, 1.0 / 2.0)
            }

            HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> {
                NightPortions(1.0 / 7.0, 1.0 / 7.0)
            }

            HighLatitudeRule.TWILIGHT_ANGLE -> {
                NightPortions(
                    fajrAngle / 60.0,
                    ishaAngle / 60.0
                )
            }
        }
    }
}