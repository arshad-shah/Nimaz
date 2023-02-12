package com.arshadshah.nimaz.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
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
		}
	}

	@Test
	fun testGetPrayerTimes() {
		runBlocking {
			val mapOfParams = mapOf("param1" to "value1", "param2" to "value2")
			val prayerTimeResponse = nimazServicesImpl.getPrayerTimes(mapOfParams)
			assertNotNull(prayerTimeResponse)
			// additional tests to check the content of the response
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