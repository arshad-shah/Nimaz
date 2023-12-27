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
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class MissedPrayerReciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (!LocalDataStore.isInitialized()) {
            LocalDataStore.init(context)
            Log.d(
                AppConstants.MISSED_PRAYER_RECEIVER_TAG,
                "onReceived:  called and local data store initialized"
            )
        }

        if (!FirebaseLogger.isInitialized()) {
            FirebaseLogger.init()
            Log.d(
                AppConstants.MISSED_PRAYER_RECEIVER_TAG,
                "onReceived:  called and firebase logger initialized"
            )
        }


        val mapForLogging = mapOf(
            "MissedPrayerReciever" to "MissedPrayerReciever called"
        )
        FirebaseLogger.logEvent("Missed Prayer Receiver", mapForLogging)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var amountMissed = 0
                val nameOfMissedPrayers = mutableListOf<String>()
                val tracker =
                    LocalDataStore.getDataStore().getTrackerForDate(LocalDate.now())

                Log.d(
                    AppConstants.MISSED_PRAYER_RECEIVER_TAG,
                    "onReceived:  called and tracker is $tracker"
                )


                if (!tracker.fajr) {
                    amountMissed++
                    nameOfMissedPrayers.add("Fajr")
                }
                if (!tracker.dhuhr) {
                    amountMissed++
                    nameOfMissedPrayers.add("Dhuhr")
                }
                if (!tracker.asr) {
                    amountMissed++
                    nameOfMissedPrayers.add("Asr")
                }
                if (!tracker.maghrib) {
                    amountMissed++
                    nameOfMissedPrayers.add("Maghrib")
                }
                if (!tracker.isha) {
                    amountMissed++
                    nameOfMissedPrayers.add("Isha")
                }


                val title = if (amountMissed > 0) "$amountMissed prayers missed"
                else "Well Done!"

                //get time left to complete missed prayers
                val timeLeft =
                    LocalTime.now().until(LocalTime.of(23, 59, 59), ChronoUnit.MINUTES)

                //create message
                val message =
                    if (amountMissed > 0) "You missed ${nameOfMissedPrayers.joinToString(", ")} today," +
                            "\n there is still $timeLeft minutes left to complete ${if (amountMissed > 1) "them" else "it"}"
                    else "You completed all your prayers today. Keep it up!"

                NotificationHelper().createNotificationForMissedPrayer(
                    context,
                    AppConstants.CHANNEL_MISSED_PRAYER_ID,
                    AppConstants.MISSED_PRAYER_NOTIFY_ID,
                    title,
                    message
                )

            } catch (e: Exception) {
                Log.e(
                    AppConstants.MISSED_PRAYER_RECEIVER_TAG,
                    "Error in BootReciever: ${e.message}"
                )
                val mapForLoggingError = mapOf(
                    "MissedPrayerReciever" to "Error in BootReciever: ${e.message}"
                )
                FirebaseLogger.logEvent("MissedPrayerReceiver", mapForLoggingError)
            }
        }

        Log.d(AppConstants.MISSED_PRAYER_RECEIVER_TAG, "onReceive:  called and finished")
    }
}