package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var firebaseLogger: FirebaseLogger

    @Inject
    lateinit var createAlarms: CreateAlarms

    @Inject
    lateinit var prayerTimesRepository: PrayerTimesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) ||
            intent.action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        ) {
            Log.d(AppConstants.BOOT_RECEIVER_TAG, "Boot Completed or Locked Boot Completed!")
            Log.d(AppConstants.BOOT_RECEIVER_TAG, "Resetting Alarms after BootUp!")

            // Log boot completion event
            firebaseLogger.logEvent(
                "device_boot_completed",
                mapOf(
                    "action" to intent.action.toString(),
                    "timestamp" to System.currentTimeMillis()
                )
            )

            val sharedPreferences = PrivateSharedPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Log prayer times fetch attempt
                    firebaseLogger.logEvent("boot_prayer_times_fetch_started", null)

                    val repository = prayerTimesRepository.getPrayerTimes(context)

                    // Log prayer times availability
                    val hasPrayerTimes = repository.data != null
                    firebaseLogger.logEvent(
                        "boot_prayer_times_available",
                        mapOf("success" to hasPrayerTimes)
                    )

                    // Calculate Isha time adjustment if needed
                    val ishaTime = repository.data?.isha?.toLocalTime()?.hour
                    val newIshaTime = if (ishaTime != null && ishaTime >= 22) {
                        // Log Isha time adjustment
                        firebaseLogger.logEvent(
                            "boot_isha_time_adjusted",
                            mapOf(
                                "original_hour" to ishaTime,
                                "adjustment" to "maghrib_plus_60min"
                            )
                        )
                        repository.data.maghrib?.plusMinutes(60)
                    } else {
                        repository.data?.isha
                    }

                    // Reset alarm lock and create alarms
                    sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, false)
                    val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK, false)

                    if (!alarmLock) {
                        // Check if all prayer times are available
                        if (repository.data?.fajr != null &&
                            repository.data.sunrise != null &&
                            repository.data.dhuhr != null &&
                            repository.data.asr != null &&
                            repository.data.maghrib != null &&
                            newIshaTime != null) {

                            // Create alarms
                            createAlarms.exact(
                                context,
                                repository.data.fajr!!,
                                repository.data.sunrise!!,
                                repository.data.dhuhr!!,
                                repository.data.asr!!,
                                repository.data.maghrib!!,
                                newIshaTime
                            )

                            sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, true)

                            // Log successful alarm creation
                            firebaseLogger.logEvent(
                                "boot_alarms_created_successfully",
                                mapOf("prayer_count" to 6)
                            )
                        } else {
                            // Log missing prayer times
                            firebaseLogger.logError(
                                "boot_missing_prayer_times",
                                "Could not create alarms due to missing prayer times",
                                null
                            )
                        }
                    } else {
                        // Log skipped alarm creation
                        firebaseLogger.logEvent(
                            "boot_alarms_creation_skipped",
                            mapOf("reason" to "already_locked")
                        )
                    }
                } catch (e: Exception) {
                    Log.e(AppConstants.BOOT_RECEIVER_TAG, "Error in BootReceiver: ${e.message}")

                    // Log error details
                    firebaseLogger.logError(
                        "boot_receiver_error",
                        e.message ?: "Unknown error in BootReceiver",
                        mapOf("error_type" to e.javaClass.simpleName)
                    )
                }
            }

            Log.d(AppConstants.BOOT_RECEIVER_TAG, "Alarms Reset after BootUp!")
        } else {
            // Log unexpected intent
            firebaseLogger.logEvent(
                "boot_receiver_unexpected_intent",
                mapOf("action" to (intent.action ?: "null"))
            )
        }
    }
}