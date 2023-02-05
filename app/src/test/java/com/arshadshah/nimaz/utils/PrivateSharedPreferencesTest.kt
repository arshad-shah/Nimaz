package com.arshadshah.nimaz.utils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivateSharedPreferencesTest
{

	private lateinit var sharedPreferences : PrivateSharedPreferences

	@Before
	fun setUp()
	{
		val context = InstrumentationRegistry.getInstrumentation().context
		sharedPreferences = PrivateSharedPreferences(context)
		sharedPreferences.clearData()
	}

	@Test
	fun testSaveData()
	{
		sharedPreferences.saveData("key" , "value")
		assertEquals("value" , sharedPreferences.getData("key" , "default"))
	}

	@Test
	fun testSaveDataBoolean()
	{
		sharedPreferences.saveDataBoolean("key" , true)
		assertTrue(sharedPreferences.getDataBoolean("key" , false))
	}

	@Test
	fun testSaveDataInt()
	{
		sharedPreferences.saveDataInt("key" , 1)
		assertEquals(1 , sharedPreferences.getDataInt("key"))
	}

	@Test
	fun testSaveDataLong()
	{
		sharedPreferences.saveDataLong("key" , 1L)
		assertEquals(1L , sharedPreferences.getDataLong("key"))
	}

	@Test
	fun testSaveDataFloat()
	{
		sharedPreferences.saveDataFloat("key" , 1f)
		assertEquals(1f , sharedPreferences.getDataFloat("key") , 0f)
	}

	@Test
	fun testSaveDataDouble()
	{
		sharedPreferences.saveDataDouble("key" , 1.0)
		assertEquals(1.0 , sharedPreferences.getDataDouble("key" , 0.0) , 0.0)
	}

//	@Test
//	fun testSaveIntSet()
//	{
//		sharedPreferences.saveIntSet("key" , setOf(1 , 2 , 3))
//		assertEquals("[1, 2, 3]" , sharedPreferences.getIntSet("key" , ""))
//	}

	@Test
	fun testContainsData()
	{
		sharedPreferences.saveData("key" , "value")
		assertTrue(sharedPreferences.containsData("key"))
		assertFalse(sharedPreferences.containsData("non_existent_key"))
	}

	@Test
	fun testRemoveData()
	{
		sharedPreferences.saveData("key" , "value")
		sharedPreferences.removeData("key")
		assertFalse(sharedPreferences.containsData("key"))
	}

	@Test
	fun testClearData()
	{
		sharedPreferences.saveData("key" , "value")
		sharedPreferences.clearData()
		assertFalse(sharedPreferences.containsData("key"))
	}
}
