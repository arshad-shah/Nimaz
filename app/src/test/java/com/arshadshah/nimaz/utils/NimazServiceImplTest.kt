package com.arshadshah.nimaz.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.api.NimazServicesImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NimazServiceImplTest
{

	//set up the test
	private val nimazServicesImpl = NimazServicesImpl

	@Test
	fun testLogin() {
		runBlocking {
			val username = AppConstants.USER_USERNAME
			val password = AppConstants.USER_PASSWORD
			val loginResponse = nimazServicesImpl.login(username, password)
			assertNotNull(loginResponse)
			assertTrue(loginResponse.token.length > 100)
			//check the content of the response to be alphanumeric with some special characters
			val regex = Regex("[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+")
			assertTrue(loginResponse.token.matches(regex))
		}
	}

	@Test
	fun testGetPrayerTimes() {
		runBlocking {
			val mapOfParams = mutableMapOf<String, String>()
			mapOfParams["latitude"] = "33.6844"
			mapOfParams["longitude"] = "73.0479"
			mapOfParams["date"] = "2023-03-01T00:14:00"
			mapOfParams["fajrAngle"] = "18"
			mapOfParams["ishaAngle"] = "18"
			mapOfParams["ishaInterval"] = "0"
			mapOfParams["method"] = "MWL"
			mapOfParams["madhab"] = "SHAFI"
			mapOfParams["highLatitudeRule"] = "MIDDLE_OF_THE_NIGHT"
			mapOfParams["fajrAdjustment"] = "0"
			mapOfParams["sunriseAdjustment"] = "0"
			mapOfParams["dhuhrAdjustment"] = "0"
			mapOfParams["asrAdjustment"] = "0"
			mapOfParams["maghribAdjustment"] = "0"
			mapOfParams["ishaAdjustment"] = "0"
			mapOfParams["ishaInterval"] = "0"
			val prayerTimeResponse = nimazServicesImpl.getPrayerTimesMonthlyCustom(mapOfParams)
			println(prayerTimeResponse)
			assertNotNull(prayerTimeResponse)
			// additional tests to check the content of the response
			//check that the response is a list of 31 items
			assertTrue(prayerTimeResponse.size == 31)
		}
	}

	@Test
	fun testGetSurahs() {
		runBlocking {
			val surahResponseList = nimazServicesImpl.getSurahs()
			assertNotNull(surahResponseList)
			assertTrue(surahResponseList.isNotEmpty())
			// additional tests to check the content of the response
		}
	}

	@Test
	fun testGetJuzs() {
		runBlocking {
			val juzResponseList = nimazServicesImpl.getJuzs()
			assertNotNull(juzResponseList)
			assertTrue(juzResponseList.isNotEmpty())
			// additional tests to check the content of the response
		}
	}
}