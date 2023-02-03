package com.arshadshah.nimaz.utils.alarms

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.recievers.AdhanReciever
import com.arshadshah.nimaz.utils.recievers.ResetAdhansReciever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class CreateAlarms
{

	// notification channelid
	val FAJR_CHANNEL_ID = "fajr_channel_id"
	val SUNRISE_CHANNEL_ID = "sunrise_channel_id"
	val ZUHAR_CHANNEL_ID = "zuhar_channel_id"
	val ASAR_CHANNEL_ID = "asar_channel_id"
	val MAGHRIB_CHANNEL_ID = "maghrib_channel_id"
	val ISHAA_CHANNEL_ID = "ishaa_channel_id"


	//notification id
	val fajrNotifyId = 2000
	val sunriseNotifyId = 2001
	val zuharNotifyId = 2002
	val asarNotifyId = 2003
	val maghribNotifyId = 2004
	val ishaaNotifyId = 2005

	val fajrPIRequestCode = 1000
	val sunrisePIRequestCode = 1001
	val dhuhrPIRequestCode = 1002
	val asrPIRequestCode = 1003
	val maghribPIRequestCode = 1004
	val ishaPIRequestCode = 1005


	// notification title
	lateinit var channelFajr : String
	lateinit var channelSunrise : String
	lateinit var channelZuhar : String
	lateinit var channelAsar : String
	lateinit var channelMaghrib : String
	lateinit var channelIshaa : String


	fun exact(
		context : Context ,
		fajr : LocalDateTime ,
		sunrise : LocalDateTime ,
		dhuhr : LocalDateTime ,
		asr : LocalDateTime ,
		maghrib : LocalDateTime ,
		ishaa : LocalDateTime ,
			 )
	{
		channelFajr = context.getString(R.string.fajr)
		channelSunrise = context.getString(R.string.sunrise)
		channelZuhar = context.getString(R.string.zuhar)
		channelAsar = context.getString(R.string.asar)
		channelMaghrib = context.getString(R.string.maghrib)
		channelIshaa = context.getString(R.string.ishaa)
		CoroutineScope(Dispatchers.IO).launch {
			val sharedPreferences = PrivateSharedPreferences(context)
			val channelLock = sharedPreferences.getDataBoolean(AppConstants.CHANNEL_LOCK, false)
			if (! channelLock)
			{
				createAllNotificationChannels(context)
				sharedPreferences.saveDataBoolean(AppConstants.CHANNEL_LOCK, true)
			}
			//convert the local date time to milliseconds
			val fajrTime = fajr.toInstant(ZoneOffset.UTC).toEpochMilli()
			val sunriseTime = sunrise.toInstant(ZoneOffset.UTC).toEpochMilli()
			val dhuhrTime = dhuhr.toInstant(ZoneOffset.UTC).toEpochMilli()
			val asrTime = asr.toInstant(ZoneOffset.UTC).toEpochMilli()
			val maghribTime = maghrib.toInstant(ZoneOffset.UTC).toEpochMilli()
			val ishaaTime = ishaa.toInstant(ZoneOffset.UTC).toEpochMilli()

			//alarm lock
			val oneOClock = GregorianCalendar.getInstance().apply {
				set(Calendar.HOUR_OF_DAY , 1)
				set(Calendar.MINUTE , 0)
				set(Calendar.SECOND , 0)
				set(Calendar.MILLISECOND , 0)
			}

			val current_time = System.currentTimeMillis()
			if (current_time > oneOClock.timeInMillis)
			{
				scheduleAlarms(
						context ,
						fajrTime ,
						sunriseTime ,
						dhuhrTime ,
						asrTime ,
						maghribTime ,
						ishaaTime
							  )
				//reset alarms
				resetAlarms(
						context ,
						fajrTime ,
						sunriseTime ,
						dhuhrTime ,
						asrTime ,
						maghribTime ,
						ishaaTime
						   )
			} else
			{
				//reset alarms
				resetAlarms(
						context ,
						fajrTime ,
						sunriseTime ,
						dhuhrTime ,
						asrTime ,
						maghribTime ,
						ishaaTime
						   )
			}
		}
	}

	fun createPendingIntent(
		context : Context ,
		requestCode : Int ,
		notifyId : Int ,
		timeToNotify : Long ,
		title : String ,
		channelid : String ,
						   ) : PendingIntent
	{
		val intent = Intent(context , AdhanReciever::class.java).apply {
			putExtra("notifyid" , notifyId)
			putExtra("time" , timeToNotify)
			putExtra("title" , title)
			putExtra("channelid" , channelid)
		}
		return PendingIntent.getBroadcast(
				context ,
				requestCode ,
				intent ,
				PendingIntent.FLAG_IMMUTABLE
										 )
	}

	fun scheduleAlarms(
		context : Context ,
		fajr : Long ,
		sunrise : Long ,
		dhuhr : Long ,
		asr : Long ,
		maghrib : Long ,
		ishaa : Long ,
					  )
	{

		// Set up the pending intents for each alarm
		val pendingIntent1 = createPendingIntent(
				context ,
				fajrPIRequestCode ,
				fajrNotifyId ,
				fajr ,
				channelFajr ,
				FAJR_CHANNEL_ID
												)
		val pendingIntent2 = createPendingIntent(
				context ,
				sunrisePIRequestCode ,
				sunriseNotifyId ,
				sunrise ,
				channelSunrise ,
				SUNRISE_CHANNEL_ID
												)
		val pendingIntent3 = createPendingIntent(
				context ,
				dhuhrPIRequestCode ,
				zuharNotifyId ,
				dhuhr ,
				channelZuhar ,
				ZUHAR_CHANNEL_ID
												)
		val pendingIntent4 = createPendingIntent(
				context ,
				asrPIRequestCode ,
				asarNotifyId ,
				asr ,
				channelAsar ,
				ASAR_CHANNEL_ID
												)
		val pendingIntent5 = createPendingIntent(
				context ,
				maghribPIRequestCode ,
				maghribNotifyId ,
				maghrib ,
				channelMaghrib ,
				MAGHRIB_CHANNEL_ID
												)
		val pendingIntent6 = createPendingIntent(
				context ,
				ishaPIRequestCode ,
				ishaaNotifyId ,
				ishaa ,
				channelIshaa ,
				ISHAA_CHANNEL_ID
												)


		// Set the alarms
		val alarms = Alarms()
		alarms.setExactAlarm(context , fajr , pendingIntent1)
		alarms.setExactAlarm(context , sunrise , pendingIntent2)
		alarms.setExactAlarm(context , dhuhr , pendingIntent3)
		alarms.setExactAlarm(context , asr , pendingIntent4)
		alarms.setExactAlarm(context , maghrib , pendingIntent5)
		alarms.setExactAlarm(context , ishaa , pendingIntent6)
	}

	/**
	 * reset the onetime exact alarms
	 * @param context the context of the Application
	 * @param fajrAlarm time in milliseconds for fajr alarm
	 * @param zuharAlarm time in milliseconds for zuhar alarm
	 * @param asarAlarm time in milliseconds for asar alarm
	 * @param maghribAlarm time in milliseconds for maghrib alarm
	 * @param ishaaAlarm time in milliseconds for ishaaAlarm
	 * */
	fun resetAlarms(
		context : Context ,
		fajrAlarm : Long ,
		sunriseAlarm : Long ,
		zuharAlarm : Long ,
		asarAlarm : Long ,
		maghribAlarm : Long ,
		ishaaAlarm : Long ,
				   )
	{
		//recreate all alarms
		val resetIntent =
			Intent(context , ResetAdhansReciever::class.java).apply {
				putExtra("fajrTime" , fajrAlarm)
				putExtra("sunriseTime" , sunriseAlarm)
				putExtra("zuharTime" , zuharAlarm)
				putExtra("asarTime" , asarAlarm)
				putExtra("maghribTime" , maghribAlarm)
				putExtra("ishaaTime" , ishaaAlarm)
			}
		val resetPendingIntent = PendingIntent.getBroadcast(
				context , 7 , resetIntent ,
				PendingIntent.FLAG_IMMUTABLE
														   )
		Alarms().setAlarm(context , resetPendingIntent)
	}

	fun createAllNotificationChannels(context : Context)
	{
		channelFajr = context.getString(R.string.fajr)
		channelSunrise = context.getString(R.string.sunrise)
		channelZuhar = context.getString(R.string.zuhar)
		channelAsar = context.getString(R.string.asar)
		channelMaghrib = context.getString(R.string.maghrib)
		channelIshaa = context.getString(R.string.ishaa)
		// notification description
		val descFajr = "Fajr Prayer Notification"
		val descSunrise = "Sunrise Notification"
		val descZuhar = "Zuhar Prayer Notification"
		val descAsar = "Asar Prayer Notification"
		val descMaghrib = "Maghrib Prayer Notification"
		val descIshaa = "Ishaa Prayer Notification"

		val fajrAdhan = "android.resource://" + context.packageName + "/" + R.raw.fajr
		val zuharAdhan = "android.resource://" + context.packageName + "/" + R.raw.zuhar
		val asarAdhan = "android.resource://" + context.packageName + "/" + R.raw.asar
		val maghribAdhan = "android.resource://" + context.packageName + "/" + R.raw.maghrib
		val ishaaAdhan = "android.resource://" + context.packageName + "/" + R.raw.ishaa
		//create notification channels
		val notificationHelper = NotificationHelper()
		//fajr
		notificationHelper.createNotificationChannel(
				context ,
				NotificationManager.IMPORTANCE_MAX ,
				true ,
				channelFajr ,
				descFajr ,
				FAJR_CHANNEL_ID ,
				fajrAdhan
													)
		//sunrise
		notificationHelper.notificationChannelSilent(
				context ,
				NotificationManager.IMPORTANCE_DEFAULT ,
				false ,
				channelSunrise ,
				descSunrise ,
				SUNRISE_CHANNEL_ID
													)
		//zuhar
		notificationHelper.createNotificationChannel(
				context ,
				NotificationManager.IMPORTANCE_MAX ,
				true ,
				channelZuhar ,
				descZuhar ,
				ZUHAR_CHANNEL_ID ,
				zuharAdhan
													)
		//asar
		notificationHelper.createNotificationChannel(
				context ,
				NotificationManager.IMPORTANCE_MAX ,
				true ,
				channelAsar ,
				descAsar ,
				ASAR_CHANNEL_ID ,
				asarAdhan
													)
		//maghrib
		notificationHelper.createNotificationChannel(
				context ,
				NotificationManager.IMPORTANCE_MAX ,
				true ,
				channelMaghrib ,
				descMaghrib ,
				MAGHRIB_CHANNEL_ID ,
				maghribAdhan
													)
		//ishaa
		notificationHelper.createNotificationChannel(
				context ,
				NotificationManager.IMPORTANCE_MAX ,
				true ,
				channelIshaa ,
				descIshaa ,
				ISHAA_CHANNEL_ID ,
				ishaaAdhan
													)

	}
}