package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AdhanReciever : BroadcastReceiver()
{

	override fun onReceive(context : Context , intent : Intent)
	{
		if (! FirebaseLogger.isInitialized())
		{
			FirebaseLogger.init()
			Log.d(AppConstants.ADHAN_RECEIVER_TAG , "Firebase logger initialized")
		}
		// When the reciever is called, it will send a notification to the user
		//send notification
		// This method is called when the BroadcastReceiver is receiving an Intent broadcast.
		val title = intent.extras !!.getString("title").toString()
		val CHANNEL_ID = intent.extras !!.getString("channelid").toString()
		val Notify_Id = intent.extras !!.getInt("notifyid")
		val Time_of_alarm = intent.extras !!.getLong("time")

		val current_time = System.currentTimeMillis()
		val diff = current_time - Time_of_alarm
		//two minutes grace period
		val graceP = 2 * 60 * 1000

		Log.d(AppConstants.ADHAN_RECEIVER_TAG , "Alarm for $title is being executed!")
		// check if it is time to notify
		//diff in 1 until graceP
		if (diff in 1 until graceP || title == "Test Adhan")
		{
			Log.d(AppConstants.ADHAN_RECEIVER_TAG , "Notification for $title is being executed!")
			when (title)
			{
				"Test Adhan" ->
				{
					Toasty.info(context , "Test Adhan is being executed!").show()
				}

				"Sunrise" , "شروق" ->
				{
					Toasty.info(context , "The Sun is rising!").show()
				}

				else ->
				{
					Toasty.info(context , "Time to pray $title").show()
				}
			}
			NotificationHelper().createNotification(
					 context ,
					 CHANNEL_ID ,
					 title ,
					 Notify_Id ,
					 Time_of_alarm
												   )
			Toasty.info(context , "Time to pray $title").show()

			Log.d(AppConstants.ADHAN_RECEIVER_TAG , "Alarm for $title is Successfully executed!")
		} // end of if
		else
		{
			Log.d(
					 AppConstants.ADHAN_RECEIVER_TAG ,
					 "Notification for $title is not executed! The time has passed"
				 )
		}

		if (title == "Ishaa")
		{
			Log.d(AppConstants.ADHAN_RECEIVER_TAG , "Past Isha, Re-creating alarms for tomorrow")
			val sharedPreferences = PrivateSharedPreferences(context)
			CoroutineScope(Dispatchers.IO).launch {
				try
				{
					val repository = PrayerTimesRepository.getPrayerTimes(
							 context ,
							 LocalDate.now().plusDays(1).toString()
																		 )
					//check if the isha time is past 11 pm
					//if it is set the isha time to 30 mins after maghrib
					val ishaTime = repository.data?.isha?.toLocalTime()?.hour
					val newIshaTime = if (ishaTime !! >= 22)
					{
						repository.data.maghrib?.plusMinutes(60)
					} else
					{
						repository.data.isha
					}
					sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , false)
					val alarmLock =
						sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK , false)
					if (! alarmLock)
					{
						CreateAlarms().exact(
								 context ,
								 repository.data.fajr !! ,
								 repository.data.sunrise !! ,
								 repository.data.dhuhr !! ,
								 repository.data.asr !! ,
								 repository.data.maghrib !! ,
								 newIshaTime !!
											)
						sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , true)
					}
				} catch (e : Exception)
				{
					Log.e(AppConstants.ADHAN_RECEIVER_TAG , "Error in AdhanReciever: ${e.message}")
					val mapForLoggingError = mapOf(
							 "Error" to "Error in AdhanReciever: ${e.message}"
												  )
					FirebaseLogger.logEvent(AppConstants.ADHAN_RECEIVER_TAG , mapForLoggingError)
				}
			}
		}
	}
}