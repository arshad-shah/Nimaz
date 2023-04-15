package com.arshadshah.nimaz.utils.sunMoonUtils

import com.arshadshah.nimaz.utils.sunMoonUtils.utils.MathUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class MathUtilsTest
{

	@Test
	fun calculateJulianDay() {
		val julianDay1 = MathUtils.toJulian(LocalDateTime.parse("2019-01-01T00:00:00"))
		assertEquals(2458484.5, julianDay1, 0.0001)

		val julianDay2 = MathUtils.toJulian(LocalDateTime.parse("2019-02-01T00:00:00"))
		assertEquals(2458515.5, julianDay2, 0.0001)

		val julianDay3 = MathUtils.toJulian(LocalDateTime.parse("2019-03-01T00:00:00"))
		assertEquals(2458543.5, julianDay3, 0.0001)

		val julianDay4 = MathUtils.toJulian(LocalDateTime.parse("2019-04-01T00:00:00"))
		assertEquals(2458574.5, julianDay4, 0.0001)
	}


	@Test
	fun fromJulianTest() {
		val julian = 2458484.5
		val date = LocalDateTime.parse("2019-01-01T00:00:00")
		assertEquals(date, MathUtils.fromJulian(julian))
	}

	@Test
	fun toDaysTest() {
		val date = LocalDateTime.parse("2021-08-16T12:00:00")
		assertEquals(7898.0 , MathUtils.toDays(date) , 0.0001)
	}


}