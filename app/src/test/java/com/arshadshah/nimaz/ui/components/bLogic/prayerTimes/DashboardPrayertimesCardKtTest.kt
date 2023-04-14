package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.ui.components.dashboard.getTimerText
import org.junit.Test

class DashboardPrayertimesCardKtTest
{
	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsMoreThanOne()
	{
		val timeToNextPrayer = CountDownTime(2 , 0 , 0)
		val result = getTimerText(timeToNextPrayer)
		assert(result == "2 hours Left")
	}

	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsOne()
	{
		val timeToNextPrayer = CountDownTime(1 , 0 , 0)
		val result = getTimerText(timeToNextPrayer)
		assert(result == "1 hour Left")
	}

	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsOneAndMinutesIsMoreThanOne()
	{
		val timeToNextPrayer = CountDownTime(1 , 2 , 0)
		val result = getTimerText(timeToNextPrayer)
		assert(result == "1 hour 2 minutes Left")
	}

	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsOneAndMinutesIsOne()
	{
		val timeToNextPrayer = CountDownTime(1 , 1 , 0)
		val result = getTimerText(timeToNextPrayer)
		assert(result == "1 hour 1 minute Left")
	}

	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsOneAndMinutesIsOneAndSecondsIsMoreThanOne()
	{
		val timeToNextPrayer = CountDownTime(1 , 1 , 2)
		val result = getTimerText(timeToNextPrayer)
		assert(result == "1 hour 1 minute 2 seconds Left")
	}

	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsOneAndMinutesIsOneAndSecondsIsOne()
	{
		val timeToNextPrayer = CountDownTime(1 , 1 , 1)
		val result = getTimerText(timeToNextPrayer)
		assert(result == "1 hour 1 minute 1 second Left")
	}

	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsZeroAndMinutesIsMoreThanOne()
	{
		val timeToNextPrayer = CountDownTime(0 , 2 , 0)
		val result = getTimerText(timeToNextPrayer)
		println(result)
		assert(result == "2 minutes Left")
	}

	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsZeroAndMinutesIsOne()
	{
		val timeToNextPrayer = CountDownTime(0 , 1 , 0)
		val result = getTimerText(timeToNextPrayer)
		println(result)
		assert(result == "1 minute Left")
	}

	@Test
	fun checkIfFunctionReturnsCorrectTextWhenHoursIsZeroAndMinutesIsOneAndSecondsIsMoreThanOne()
	{
		val timeToNextPrayer = CountDownTime(0 , 1 , 2)
		val result = getTimerText(timeToNextPrayer)
		println(result)
		assert(result == "1 minute 2 seconds Left")
	}
}