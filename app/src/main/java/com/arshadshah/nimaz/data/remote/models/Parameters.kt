package com.arshadshah.nimaz.data.remote.models

import com.arshadshah.nimaz.libs.prayertimes.enums.CalculationMethod
import com.arshadshah.nimaz.libs.prayertimes.enums.HighLatitudeRule
import com.arshadshah.nimaz.libs.prayertimes.enums.Madhab
import java.time.LocalDateTime

data class Parameters(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var date: String = LocalDateTime.now().toString(),
    var fajrAngle: Double = 18.0,
    var ishaAngle: Double = 18.0,
    var method: CalculationMethod =
        CalculationMethod.MWL,
    var madhab: Madhab =
        Madhab.SHAFI,
    var highLatitudeRule: HighLatitudeRule =
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
    var fajrAdjustment: Int = 0,
    var sunriseAdjustment: Int = 0,
    var dhuhrAdjustment: Int = 0,
    var asrAdjustment: Int = 0,
    var maghribAdjustment: Int = 0,
    var ishaAdjustment: Int = 0,
    var ishaInterval: Int = 0,
)