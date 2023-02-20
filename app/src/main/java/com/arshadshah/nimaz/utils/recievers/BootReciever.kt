package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import java.time.LocalDateTime

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

			val sharedPreferences = PrivateSharedPreferences(context)

			val fajr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.FAJR , LocalDateTime.now().toString()))
			val sunrise =
				LocalDateTime.parse(sharedPreferences.getData(AppConstants.SUNRISE , LocalDateTime.now().toString()))
			val dhuhr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.DHUHR , LocalDateTime.now().toString()))
			val asr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ASR , LocalDateTime.now().toString()))
			val maghrib =
				LocalDateTime.parse(sharedPreferences.getData(AppConstants.MAGHRIB , LocalDateTime.now().toString()))
			val isha = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ISHA , LocalDateTime.now().toString()))

			CreateAlarms().exact(context , fajr , sunrise , dhuhr , asr , maghrib , isha)

			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Alarms Reset after BootUp!")

		}
	}
}