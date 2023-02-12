package com.arshadshah.nimaz.utils

import org.junit.Test
import kotlin.math.abs

class QiblaTest
{

	private val qibla = Qibla()

	//test data from https://www.qiblafinder.org/

	@Test
	fun testQiblaDirection_ForAbbeyleixIreland() {
		val latitude = 52.9184
		val longitude = -7.3552
		val direction = qibla.calculateQiblaDirection(latitude, longitude)
		//round to 2 decimal places
		val roundedDirection = (direction * 100).toInt() / 100.0
		assert(abs(roundedDirection - 112.62) < 0.01)
	}

	@Test
	fun testQiblaDirection_ForRussia() {
		val latitude = 68.0584
		val longitude = 166.4454
		val direction = qibla.calculateQiblaDirection(latitude, longitude)
		val roundedDirection = (direction * 100).toInt() / 100.0
		assert(abs(roundedDirection - 311.09) < 0.01)
	}

	@Test
	fun testQiblaDirection_ForLondon() {
		val latitude = 51.5085
		val longitude = -0.1257
		val direction = qibla.calculateQiblaDirection(latitude, longitude)
		val roundedDirection = (direction * 100).toInt() / 100.0
		assert(abs(roundedDirection - 118.99) < 0.01)
	}

	@Test
	fun testQiblaDirection_ForMakkahAbrajAlBait() {
		val latitude = 21.4347
		val longitude = 39.8538
		val direction = qibla.calculateQiblaDirection(latitude, longitude)
		val roundedDirection = (direction * 100).toInt() / 100.0
		println("direction: $roundedDirection")
		assert(abs(roundedDirection - 244.66) < 0.01)
	}

	@Test
	fun testQiblaDirection_ForNewYork() {
		val latitude = 40.7128
		val longitude = -74.0060
		val direction = qibla.calculateQiblaDirection(latitude, longitude)
		val roundedDirection = (direction * 100).toInt() / 100.0
		println("direction: $roundedDirection")
		assert(abs(roundedDirection - 58.48) < 0.01)
	}

	@Test
	fun testQiblaDirection_ForSydney() {
		val latitude = -33.8688
		val longitude = 151.2093
		val direction = qibla.calculateQiblaDirection(latitude, longitude)
		val roundedDirection = (direction * 100).toInt() / 100.0
		println("direction: $roundedDirection")
		assert(abs(roundedDirection - 277.49) < 0.01)
	}
}