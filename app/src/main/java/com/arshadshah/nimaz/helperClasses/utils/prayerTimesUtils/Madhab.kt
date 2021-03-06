package com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils

import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.ShadowLength

/**
 * Madhab for determining how Asr is calculated
 */
enum class Madhab {

    /**
     * Shafi Madhab
     */
    SHAFI,

    /**
     * Hanafi Madhab
     */
    HANAFI;

    val shadowLength: ShadowLength
        get() = when (this) {
            SHAFI -> {
                ShadowLength.SINGLE
            }

            HANAFI -> {
                ShadowLength.DOUBLE
            }

            else -> {
                throw IllegalArgumentException("Invalid Madhab")
            }
        }
}