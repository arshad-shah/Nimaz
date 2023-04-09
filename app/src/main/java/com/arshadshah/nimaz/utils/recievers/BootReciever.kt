package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReciever : BroadcastReceiver()
{

	override fun onReceive(context : Context , intent : Intent)
	{
		if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) ||
			intent.action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)
		)
		{
			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Boot Completed or Locked Boot Completed!")
			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Resetting Alarms after BootUp!")

			if (! FirebaseLogger.isInitialized())
			{
				FirebaseLogger.init()
				Log.d(
						AppConstants.BOOT_RECEIVER_TAG ,
						"onReceived:  called and firebase logger initialized"
					 )
			}

			val mapForLogging = mapOf(
					"BootReciever" to "Boot Completed resetting Alarms"
									 )
			FirebaseLogger.logEvent("Boot Receiver" , mapForLogging)

			val sharedPreferences = PrivateSharedPreferences(context)
			CoroutineScope(Dispatchers.IO).launch {
				try
				{
					val repository = PrayerTimesRepository.getPrayerTimes(context)
					sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , false)
					val alarmLock =
						sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK , false)
					if (! alarmLock)
					{
						CreateAlarms().exact(
								context ,
								repository.data?.fajr !! ,
								repository.data.sunrise !! ,
								repository.data.dhuhr !! ,
								repository.data.asr !! ,
								repository.data.maghrib !! ,
								repository.data.isha !!
											)
						sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , true)
					}
				} catch (e : Exception)
				{
					Log.e(AppConstants.BOOT_RECEIVER_TAG , "Error in BootReciever: ${e.message}")
					val mapForLoggingError = mapOf(
							"BootReciever" to "Error in BootReciever: ${e.message}"
												  )
					FirebaseLogger.logEvent("Boot Receiver" , mapForLoggingError)
				}
			}

			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Alarms Reset after BootUp!")

		}
	}
}