package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class MissedPrayerReciever : BroadcastReceiver()
{

	override fun onReceive(context : Context , intent : Intent)
	{

		if (! LocalDataStore.isInitialized())
		{
			LocalDataStore.init(context)
			Log.d(
					AppConstants.MISSED_PRAYER_RECEIVER_TAG ,
					"onReceived:  called and local data store initialized"
				 )
		}

		if (! FirebaseLogger.isInitialized())
		{
			FirebaseLogger.init()
			Log.d(
					AppConstants.MISSED_PRAYER_RECEIVER_TAG ,
					"onReceived:  called and firebase logger initialized"
				 )
		}


		val mapForLogging = mapOf(
				"MissedPrayerReciever" to "MissedPrayerReciever called"
								 )
		FirebaseLogger.logEvent("Missed Prayer Receiver" , mapForLogging)
		CoroutineScope(Dispatchers.IO).launch {
			try
			{
				var amountMissed = 0
				val tracker =
					LocalDataStore.getDataStore().getTrackerForDate(LocalDate.now().toString())
				if (! tracker.fajr)
				{
					amountMissed += 1
				} else if (! tracker.dhuhr)
				{
					amountMissed += 1
				} else if (! tracker.asr)
				{
					amountMissed += 1
				} else if (! tracker.maghrib)
				{
					amountMissed += 1
				} else if (! tracker.isha)
				{
					amountMissed += 1
				}
				val title = if (amountMissed > 0) "$amountMissed prayers missed"
				else "Well Done!"

				//message
				val message =
					if (amountMissed > 0) "You missed $amountMissed prayers today. Please try to pray them before the next day."
					else "You completed all your prayers today. Keep it up!"

				NotificationHelper().createNotificationForMissedPrayer(
						context ,
						AppConstants.CHANNEL_MISSED_PRAYER_ID ,
						AppConstants.MISSED_PRAYER_NOTIFY_ID ,
						title ,
						message
																	  )

			} catch (e : Exception)
			{
				Log.e(
						AppConstants.MISSED_PRAYER_RECEIVER_TAG ,
						"Error in BootReciever: ${e.message}"
					 )
				val mapForLoggingError = mapOf(
						"MissedPrayerReciever" to "Error in BootReciever: ${e.message}"
											  )
				FirebaseLogger.logEvent("Missed Prayer Receiver" , mapForLoggingError)
			}
		}

		Log.d(AppConstants.MISSED_PRAYER_RECEIVER_TAG , "onReceive:  called and finished")
	}
}