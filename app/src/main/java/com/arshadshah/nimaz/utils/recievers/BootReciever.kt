package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import kotlinx.coroutines.runBlocking

class BootReciever : BroadcastReceiver()
{

	override fun onReceive(context : Context , intent : Intent)
	{
		if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) ||
			intent.action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)
		)
		{
			Log.i(AppConstants.BOOT_RECEIVER_TAG , "Boot Completed or Locked Boot Completed!")
			Log.i(AppConstants.BOOT_RECEIVER_TAG , "Resetting Alarms after BootUp!")

			val sharedPreferences = PrivateSharedPreferences(context)

			runBlocking {
				val repository = PrayerTimesRepository.getPrayerTimes(context)
				sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , false)
				val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK , false)
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
			}

			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Alarms Reset after BootUp!")

		}
	}
}