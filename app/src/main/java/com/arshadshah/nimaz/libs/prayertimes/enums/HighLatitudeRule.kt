package com.arshadshah.nimaz.libs.prayertimes.enums

enum class HighLatitudeRule {
    /**
     * Fajr will never be earlier than the middle of the night, and Isha will never
     * be later than the middle of the night.
     */
    MIDDLE_OF_THE_NIGHT,

    /**
     * Fajr will never be earlier than the beginning of the last seventh of the
     * night, and Isha will never be later than the end of hte first seventh of the
     * night.
     */
    SEVENTH_OF_THE_NIGHT,

    /**
     * Similar to [HighLatitudeRule.SEVENTH_OF_THE_NIGHT], but instead of
     * 1/7th, the faction of the night used is fajrAngle / 60 and ishaAngle/60.
     */
    TWILIGHT_ANGLE
}