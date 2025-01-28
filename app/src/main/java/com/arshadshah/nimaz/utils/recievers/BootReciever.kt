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

            val mapForLogging = mapOf(
                "BootReceiver" to "Boot Completed resetting Alarms"
            )
            firebaseLogger.logEvent("Boot Receiver", mapForLogging)

            val sharedPreferences = PrivateSharedPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = prayerTimesRepository.getPrayerTimes(context)
                    val ishaTime = repository.data?.isha?.toLocalTime()?.hour
                    val newIshaTime = if (ishaTime!! >= 22) {
                        repository.data.maghrib?.plusMinutes(60)
                    } else {
                        repository.data.isha
                    }

                    sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, false)
                    val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK, false)

                    if (!alarmLock) {
                        createAlarms.exact(
                            context,
                            repository.data.fajr!!,
                            repository.data.sunrise!!,
                            repository.data.dhuhr!!,
                            repository.data.asr!!,
                            repository.data.maghrib!!,
                            newIshaTime!!
                        )
                        sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, true)
                    }
                } catch (e: Exception) {
                    Log.e(AppConstants.BOOT_RECEIVER_TAG, "Error in BootReceiver: ${e.message}")
                    val mapForLoggingError = mapOf(
                        "BootReceiver" to "Error in BootReceiver: ${e.message}"
                    )
                    firebaseLogger.logEvent("Boot Receiver", mapForLoggingError)
                }
            }

            Log.d(AppConstants.BOOT_RECEIVER_TAG, "Alarms Reset after BootUp!")
        }
    }
}