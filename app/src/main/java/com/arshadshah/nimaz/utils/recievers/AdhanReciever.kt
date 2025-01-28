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
        val title = intent.extras!!.getString("title").toString()
        val CHANNEL_ID = intent.extras!!.getString("channelid").toString()
        val Notify_Id = intent.extras!!.getInt("notifyid")
        val Time_of_alarm = intent.extras!!.getLong("time")

        val current_time = System.currentTimeMillis()
        val diff = current_time - Time_of_alarm
        val graceP = 2 * 60 * 1000 // two minutes grace period

        Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Alarm for $title is being executed!")

        if (diff in 1 until graceP || title == "Test Adhan") {
            Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Notification for $title is being executed!")

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

            notificationHelper.createNotification(
                context,
                CHANNEL_ID,
                title,
                Notify_Id,
                Time_of_alarm
            )
            Toasty.info(context, "Time to pray $title").show()

            Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Alarm for $title is Successfully executed!")
        } else {
            Log.d(
                AppConstants.ADHAN_RECEIVER_TAG,
                "Notification for $title is not executed! The time has passed"
            )
        }

        if (title == "Ishaa") {
            Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Past Isha, Re-creating alarms for tomorrow")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = prayerTimesRepository.getPrayerTimes(
                        context,
                        LocalDate.now().plusDays(1).toString()
                    )

                    val ishaTime = repository.data?.isha?.toLocalTime()?.hour
                    val newIshaTime = if (ishaTime!! >= 22) {
                        repository.data.maghrib?.plusMinutes(30)
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
                    Log.e(AppConstants.ADHAN_RECEIVER_TAG, "Error in AdhanReceiver: ${e.message}")
                    val mapForLoggingError = mapOf(
                        "Error" to "Error in AdhanReceiver: ${e.message}"
                    )
                    firebaseLogger.logEvent(AppConstants.ADHAN_RECEIVER_TAG, mapForLoggingError)
                }
            }
        }
    }
}