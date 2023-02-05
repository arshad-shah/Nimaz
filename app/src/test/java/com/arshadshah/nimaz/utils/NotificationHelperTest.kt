package com.arshadshah.nimaz.utils

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest
{

	private lateinit var context : Context
	private lateinit var notificationManager : NotificationManager

	@Before
	fun setUp()
	{
		context = ApplicationProvider.getApplicationContext()
		notificationManager =
			context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	@After
	fun tearDown()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			notificationManager.deleteNotificationChannel("test_channel_id")
		}
	}

	@Test
	fun testCreateNotificationChannel()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			val notificationHelper = NotificationHelper()
			notificationHelper.createNotificationChannel(
					context ,
					NotificationManager.IMPORTANCE_DEFAULT ,
					false ,
					"test_channel_name" ,
					"test_channel_description" ,
					"test_channel_id" ,
					"test_sound" ,
														)
			val channel = notificationManager.getNotificationChannel("test_channel_id")
			assertNotNull(channel)
			assertEquals("test_channel_name" , channel.name)
			assertEquals("test_channel_description" , channel.description)
			assertFalse(channel.canShowBadge())
			assertEquals(Uri.parse("test_sound") , channel.sound.path)
			assertTrue(channel.shouldVibrate())
			assertArrayEquals(
					longArrayOf(100 , 200 , 300 , 400 , 500 , 400 , 300 , 200 , 400) ,
					channel.vibrationPattern
							 )
		}
	}

	@Test
	fun testNotificationChannelSilent()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			val notificationHelper = NotificationHelper()
			notificationHelper.notificationChannelSilent(
					context ,
					NotificationManager.IMPORTANCE_DEFAULT ,
					false ,
					"test_channel_name" ,
					"test_channel_description" ,
					"test_channel_id"
														)
			val channel = notificationManager.getNotificationChannel("test_channel_id")
			assertNotNull(channel)
			assertEquals("test_channel_name" , channel.name)
			assertEquals("test_channel_description" , channel.description)
			assertFalse(channel.canShowBadge())
			assertEquals(Settings.System.DEFAULT_NOTIFICATION_URI , channel.sound.path)
			assertFalse(channel.shouldVibrate())
			assertArrayEquals(longArrayOf(0) , channel.vibrationPattern)
		}
	}
}


