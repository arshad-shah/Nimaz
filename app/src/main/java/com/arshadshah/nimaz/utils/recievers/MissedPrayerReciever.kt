package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class MissedPrayerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var localDataStore: DataStore

    @Inject
    lateinit var firebaseLogger: FirebaseLogger

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val mapForLogging = mapOf(
            "MissedPrayerReceiver" to "MissedPrayerReceiver called"
        )
        firebaseLogger.logEvent("Missed Prayer Receiver", mapForLogging)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var amountMissed = 0
                val nameOfMissedPrayers = mutableListOf<String>()
                val tracker = localDataStore.getTrackerForDate(LocalDate.now())

                Log.d(
                    AppConstants.MISSED_PRAYER_RECEIVER_TAG,
                    "onReceived: called and tracker is $tracker"
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

                val timeLeft =
                    LocalTime.now().until(LocalTime.of(23, 59, 59), ChronoUnit.MINUTES)

                val message =
                    if (amountMissed > 0) "You missed ${nameOfMissedPrayers.joinToString(", ")} today," +
                            "\n there is still $timeLeft minutes left to complete ${if (amountMissed > 1) "them" else "it"}"
                    else "You completed all your prayers today. Keep it up!"

                notificationHelper.createNotificationForMissedPrayer(
                    context,
                    AppConstants.CHANNEL_MISSED_PRAYER_ID,
                    AppConstants.MISSED_PRAYER_NOTIFY_ID,
                    title,
                    message
                )

            } catch (e: Exception) {
                Log.e(
                    AppConstants.MISSED_PRAYER_RECEIVER_TAG,
                    "Error in BootReceiver: ${e.message}"
                )
                val mapForLoggingError = mapOf(
                    "MissedPrayerReceiver" to "Error in BootReceiver: ${e.message}"
                )
                firebaseLogger.logEvent("MissedPrayerReceiver", mapForLoggingError)
            }
        }

        Log.d(AppConstants.MISSED_PRAYER_RECEIVER_TAG, "onReceive: called and finished")
    }
}