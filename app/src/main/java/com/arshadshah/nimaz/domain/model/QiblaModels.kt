package com.arshadshah.nimaz.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class QiblaDirection(
    val bearing: Double,      // Degrees from North (0-360)
    val distance: Double,     // Distance to Kaaba in kilometers
    val userLatitude: Double,
    val userLongitude: Double
)

data class CompassData(
    val azimuth: Float = 0f,       // Device heading in degrees (0-360)
    val pitch: Float = 0f,         // Device tilt forward/backward
    val roll: Float = 0f,          // Device tilt left/right
    val accuracy: CompassAccuracy = CompassAccuracy.MEDIUM,
    val timestamp: Long = System.currentTimeMillis()
)

enum class CompassAccuracy {
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH
}

data class QiblaInfo(
    val direction: QiblaDirection,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val distanceToMecca: Double,
    val compass: CompassData? = null,
    val qiblaAngle: Float = 0f,    // Angle to rotate for Qibla (compass heading - qibla bearing)
    val isCalibrationNeeded: Boolean = false
)

object QiblaCalculator {
    // Kaaba coordinates (Mecca, Saudi Arabia)
    private const val KAABA_LATITUDE = 21.4225
    private const val KAABA_LONGITUDE = 39.8262

    // Earth's radius in kilometers
    private const val EARTH_RADIUS_KM = 6371.0

    fun calculateQiblaDirection(userLatitude: Double, userLongitude: Double): QiblaDirection {
        val bearing = calculateBearing(userLatitude, userLongitude)
        val distance = calculateDistance(userLatitude, userLongitude)

        return QiblaDirection(
            bearing = bearing,
            distance = distance,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )
    }

    private fun calculateBearing(userLat: Double, userLon: Double): Double {
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val lon1 = Math.toRadians(userLon)
        val lon2 = Math.toRadians(KAABA_LONGITUDE)

        val dLon = lon2 - lon1

        val x = sin(dLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var bearing = Math.toDegrees(atan2(x, y))

        // Normalize to 0-360
        bearing = (bearing + 360) % 360

        return bearing
    }

    private fun calculateDistance(userLat: Double, userLon: Double): Double {
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val lon1 = Math.toRadians(userLon)
        val lon2 = Math.toRadians(KAABA_LONGITUDE)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        // Haversine formula
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

    fun calculateDistanceToMecca(userLat: Double, userLon: Double): Double {
        return calculateDistance(userLat, userLon) * 1000 // Return in meters
    }

    fun calculateQiblaAngle(compassHeading: Float, qiblaBearing: Double): Float {
        // Calculate the angle to rotate the Qibla indicator
        var angle = (qiblaBearing - compassHeading).toFloat()

        // Normalize to 0-360
        while (angle < 0) angle += 360f
        while (angle >= 360) angle -= 360f

        return angle
    }

    fun getCardinalDirection(bearing: Double): String {
        return when {
            bearing >= 337.5 || bearing < 22.5 -> "N"
            bearing >= 22.5 && bearing < 67.5 -> "NE"
            bearing >= 67.5 && bearing < 112.5 -> "E"
            bearing >= 112.5 && bearing < 157.5 -> "SE"
            bearing >= 157.5 && bearing < 202.5 -> "S"
            bearing >= 202.5 && bearing < 247.5 -> "SW"
            bearing >= 247.5 && bearing < 292.5 -> "W"
            bearing >= 292.5 && bearing < 337.5 -> "NW"
            else -> "N"
        }
    }
}
