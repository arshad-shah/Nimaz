package com.arshadshah.nimaz.testing

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location

/**
 * A [Location] with fourteen fields filled in, of which a settings test cares about four.
 *
 * The location screen and the settings ViewModel both handle lists of these, and spelling the
 * other ten out per test file is how one of them ends up with `isFavorite = true` by accident in
 * a test about something else.
 */
fun testLocation(
    id: Long = 1L,
    name: String = "London",
    latitude: Double = 51.5074,
    longitude: Double = -0.1278,
    country: String? = "United Kingdom",
    city: String? = name,
    isCurrentLocation: Boolean = false,
    isFavorite: Boolean = false,
    timezone: String = "Europe/London",
    calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    asrCalculation: AsrCalculation = AsrCalculation.STANDARD,
    highLatitudeRule: HighLatitudeRule? = null,
    fajrAngle: Double? = null,
    ishaAngle: Double? = null,
) = Location(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    timezone = timezone,
    country = country,
    city = city,
    isCurrentLocation = isCurrentLocation,
    isFavorite = isFavorite,
    calculationMethod = calculationMethod,
    asrCalculation = asrCalculation,
    highLatitudeRule = highLatitudeRule,
    fajrAngle = fajrAngle,
    ishaAngle = ishaAngle,
)
