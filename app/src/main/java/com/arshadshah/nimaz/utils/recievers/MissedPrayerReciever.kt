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
        // Log receiver trigger
        firebaseLogger.logEvent(
            "missed_prayer_check_started",
            mapOf("time" to LocalTime.now().toString())
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val tracker = localDataStore.getTrackerForDate(today)

                Log.d(
                    AppConstants.MISSED_PRAYER_RECEIVER_TAG,
                    "onReceived: called and tracker is $tracker"
                )

                // Track missed prayers
                var amountMissed = 0
                val nameOfMissedPrayers = mutableListOf<String>()

                // Check each prayer status
                val prayerStatus = mutableMapOf<String, Boolean>()

                // Fajr status
                prayerStatus["fajr"] = tracker.fajr
                if (!tracker.fajr) {
                    amountMissed++
                    nameOfMissedPrayers.add("Fajr")
                }

                // Dhuhr status
                prayerStatus["dhuhr"] = tracker.dhuhr
                if (!tracker.dhuhr) {
                    amountMissed++
                    nameOfMissedPrayers.add("Dhuhr")
                }

                // Asr status
                prayerStatus["asr"] = tracker.asr
                if (!tracker.asr) {
                    amountMissed++
                    nameOfMissedPrayers.add("Asr")
                }

                // Maghrib status
                prayerStatus["maghrib"] = tracker.maghrib
                if (!tracker.maghrib) {
                    amountMissed++
                    nameOfMissedPrayers.add("Maghrib")
                }

                // Isha status
                prayerStatus["isha"] = tracker.isha
                if (!tracker.isha) {
                    amountMissed++
                    nameOfMissedPrayers.add("Isha")
                }

                // Log prayer completion status
                firebaseLogger.logEvent(
                    "daily_prayer_completion_status",
                    mapOf(
                        "date" to today.toString(),
                        "total_completed" to (5 - amountMissed),
                        "total_missed" to amountMissed,
                        "completion_percentage" to ((5 - amountMissed) * 20),
                        "missed_prayers" to if (nameOfMissedPrayers.isEmpty()) "none" else nameOfMissedPrayers.joinToString(
                            ","
                        )
                    )
                )

                // Prepare notification content
                val title = if (amountMissed > 0) "$amountMissed prayers missed" else "Well Done!"
                val timeLeft = LocalTime.now().until(LocalTime.of(23, 59, 59), ChronoUnit.MINUTES)
                val message =
                    if (amountMissed > 0) "You missed ${nameOfMissedPrayers.joinToString(", ")} today," +
                            "\n there is still $timeLeft minutes left to complete ${if (amountMissed > 1) "them" else "it"}"
                    else "You completed all your prayers today. Keep it up!"

                // Create notification
                notificationHelper.createNotificationForMissedPrayer(
                    context,
                    AppConstants.CHANNEL_MISSED_PRAYER_ID,
                    AppConstants.MISSED_PRAYER_NOTIFY_ID,
                    title,
                    message
                )

                // Log notification created
                firebaseLogger.logEvent(
                    "missed_prayer_notification_sent",
                    mapOf(
                        "title" to title,
                        "missed_count" to amountMissed,
                        "achievement" to if (amountMissed == 0) "all_completed" else "partial_completion"
                    )
                )

            } catch (e: Exception) {
                Log.e(
                    AppConstants.MISSED_PRAYER_RECEIVER_TAG,
                    "Error in MissedPrayerReceiver: ${e.message}"
                )

                // Log error
                firebaseLogger.logError(
                    "missed_prayer_receiver_error",
                    e.message ?: "Unknown error in MissedPrayerReceiver",
                    mapOf("error_type" to e.javaClass.simpleName)
                )
            }
        }

        Log.d(AppConstants.MISSED_PRAYER_RECEIVER_TAG, "onReceive: called and finished")
    }
}