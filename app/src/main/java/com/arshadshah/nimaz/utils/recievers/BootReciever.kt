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
			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Boot Completed")
			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Resetting Alarms after BootUp!")


			//TODO: get these from the database instead of shared preferences
			//and fix the 00:00 as it will crash app due to being unparsable
			val sharedPreferences = PrivateSharedPreferences(context)

			val fajr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.FAJR , "00:00"))
			val sunrise =
				LocalDateTime.parse(sharedPreferences.getData(AppConstants.SUNRISE , "00:00"))
			val dhuhr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.DHUHR , "00:00"))
			val asr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ASR , "00:00"))
			val maghrib =
				LocalDateTime.parse(sharedPreferences.getData(AppConstants.MAGHRIB , "00:00"))
			val isha = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ISHA , "00:00"))

			CreateAlarms().exact(context , fajr , sunrise , dhuhr , asr , maghrib , isha)

			Log.d(AppConstants.BOOT_RECEIVER_TAG , "Alarms Reset after BootUp!")

		}
	}
}