package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
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
			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Boot Completed or Locked Boot Completed!")
			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Resetting Alarms after BootUp!")

			runBlocking {
				val repository = PrayerTimesRepository.getPrayerTimesForWidget(context)
				CreateAlarms().exact(context , repository.data?.fajr!!, repository.data.sunrise!! , repository.data.dhuhr!! , repository.data.asr!! , repository.data.maghrib!! , repository.data.isha!!)
			}

			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Alarms Reset after BootUp!")

		}
	}
}