package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import dagger.hilt.android.AndroidEntryPoint
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AdhanReceiver : BroadcastReceiver() {

    @Inject
    lateinit var firebaseLogger: FirebaseLogger

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var prayerTimesRepository: PrayerTimesRepository

    @Inject
    lateinit var createAlarms: CreateAlarms

    @Inject
    lateinit var sharedPreferences: PrivateSharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        // Extract intent extras
        val title = intent.extras?.getString("title") ?: "Unknown"
        val channelId = intent.extras?.getString("channelid") ?: "default_channel"
        val notifyId = intent.extras?.getInt("notifyid") ?: 0
        val timeOfAlarm = intent.extras?.getLong("time") ?: 0L

        // Log prayer notification received event
        firebaseLogger.logEvent(
            "prayer_notification_received",
            mapOf(
                "prayer_name" to title,
                "scheduled_time" to timeOfAlarm
            )
        )

        Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Alarm for $title is being executed!")

        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - timeOfAlarm
        val gracePeriod = 2 * 60 * 1000 // two minutes grace period

        if (timeDifference in 1 until gracePeriod || title == "Test Adhan") {
            Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Notification for $title is being executed!")

            // Create the notification
            notificationHelper.createNotification(
                context,
                channelId,
                title,
                notifyId,
                timeOfAlarm
            )

            // Log successful notification
            firebaseLogger.logEvent(
                "prayer_notification_displayed",
                mapOf(
                    "prayer_name" to title,
                    "prayer_type" to getPrayerType(title)
                )
            )

            when (title) {
                "Test Adhan" -> {
                    Toasty.info(context, "Test Adhan is being executed!").show()
                }
                "Sunrise", "شروق" -> {
                    Toasty.info(context, "The Sun is rising!").show()
                }
                else -> {
                    Toasty.info(context, "Time to pray $title").show()
                }
            }

            Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Alarm for $title is Successfully executed!")
        } else {
            // Log missed notification due to timing
            firebaseLogger.logEvent(
                "prayer_notification_missed",
                mapOf(
                    "prayer_name" to title,
                    "time_difference_minutes" to (timeDifference / (60 * 1000))
                )
            )

            Log.d(
                AppConstants.ADHAN_RECEIVER_TAG,
                "Notification for $title is not executed! The time has passed"
            )
        }

        // Check if we need to reset alarms for tomorrow after Isha
        if (title == "Ishaa") {
            Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Past Isha, Re-creating alarms for tomorrow")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val tomorrowDate = LocalDate.now().plusDays(1)

                    val repository = prayerTimesRepository.getPrayerTimes(
                        context,
                        tomorrowDate.toString()
                    )

                    // Calculate adjusted Isha time if needed
                    val ishaTime = repository.data?.isha?.toLocalTime()?.hour
                    val newIshaTime = if (ishaTime != null && ishaTime >= 22) {
                        // Log Isha time adjustment
                        firebaseLogger.logEvent(
                            "isha_time_adjusted",
                            mapOf(
                                "original_hour" to ishaTime,
                                "adjustment" to "maghrib_plus_30min"
                            )
                        )
                        repository.data.maghrib?.plusMinutes(30)
                    } else {
                        repository.data?.isha
                    }

                    // Check alarm lock status
                    sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, false)
                    val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK, false)

                    if (!alarmLock) {
                        // Set up alarms for tomorrow
                        repository.data?.let { data ->
                            if (data.fajr != null && data.sunrise != null && data.dhuhr != null &&
                                data.asr != null && data.maghrib != null && newIshaTime != null) {

                                createAlarms.exact(
                                    context,
                                    data.fajr!!,
                                    data.sunrise!!,
                                    data.dhuhr!!,
                                    data.asr!!,
                                    data.maghrib!!,
                                    newIshaTime
                                )

                                sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, true)

                                // Log successful alarms creation
                                firebaseLogger.logEvent(
                                    "next_day_alarms_created",
                                    mapOf("date" to tomorrowDate.toString())
                                )
                            } else {
                                // Log missing prayer times error
                                firebaseLogger.logError(
                                    "prayer_times_missing",
                                    "Some prayer times are null, cannot create alarms",
                                    null
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log error
                    Log.e(AppConstants.ADHAN_RECEIVER_TAG, "Error in AdhanReceiver: ${e.message}")

                    firebaseLogger.logError(
                        "adhan_receiver_error",
                        e.message ?: "Unknown error",
                        mapOf("prayer_name" to title)
                    )
                }
            }
        }
    }

    /**
     * Helper function to categorize prayer type for analytics
     */
    private fun getPrayerType(prayerName: String): String {
        return when (prayerName) {
            "Fajr", "فجر" -> "obligatory"
            "Sunrise", "شروق" -> "non_prayer"
            "Dhuhr", "ظهر" -> "obligatory"
            "Asr", "عصر" -> "obligatory"
            "Maghrib", "مغرب" -> "obligatory"
            "Ishaa", "عشاء" -> "obligatory"
            "Test Adhan" -> "test"
            else -> "unknown"
        }
    }
}