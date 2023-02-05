package com.arshadshah.nimaz.utils

import kotlin.math.*

class Qibla {
	//an object that holds the latitude and longitude of Makkah
	private object MAKKAH {
		val latitude = 21.4225241
		val longitude = 39.8261818
	}
	fun calculateQiblaDirection(latitude: Double, longitude: Double) : Double
	{
		// Equation from "Spherical Trigonometry For the use of colleges and schools" page 50
		val longitudeDelta =
			Math.toRadians(MAKKAH.longitude) - Math.toRadians(longitude)
		val latitudeRadians = Math.toRadians(latitude)
		val term1 = sin(longitudeDelta)
		val term2 = cos(latitudeRadians) * tan(Math.toRadians(MAKKAH.latitude))
		val term3 = sin(latitudeRadians) * cos(longitudeDelta)
		val angle = atan2(term1 , term2 - term3)
		val angleDegrees = Math.toDegrees(angle)
		return angleDegrees - 360.0 * floor(angleDegrees / 360.0)
	}
}