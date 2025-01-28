package com.arshadshah.nimaz.utils.alarms

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.recievers.AdhanReceiver
import com.arshadshah.nimaz.utils.recievers.MissedPrayerReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateAlarms @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val sharedPreferences: PrivateSharedPreferences,
    private val alarms: Alarms
) {

    fun exact(
        context: Context,
        fajr: LocalDateTime,
        sunrise: LocalDateTime,
        dhuhr: LocalDateTime,
        asr: LocalDateTime,
        maghrib: LocalDateTime,
        ishaa: LocalDateTime,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            //check if all the channels have been created
            val channelLock = sharedPreferences.getDataBoolean(AppConstants.CHANNEL_LOCK, false)
            if (!channelLock) {
                createAllNotificationChannels(context)
                sharedPreferences.saveDataBoolean(AppConstants.CHANNEL_LOCK, true)
            } else {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                //find out which of the channels are not created
                if (
                    notificationManager.getNotificationChannel(AppConstants.FAJR_CHANNEL_ID) == null ||
                    notificationManager.getNotificationChannel(AppConstants.SUNRISE_CHANNEL_ID) == null ||
                    notificationManager.getNotificationChannel(AppConstants.DHUHR_CHANNEL_ID) == null ||
                    notificationManager.getNotificationChannel(AppConstants.ASR_CHANNEL_ID) == null ||
                    notificationManager.getNotificationChannel(AppConstants.MAGHRIB_CHANNEL_ID) == null ||
                    notificationManager.getNotificationChannel(AppConstants.ISHA_CHANNEL_ID) == null ||
                    notificationManager.getNotificationChannel(AppConstants.CHANNEL_MISSED_PRAYER_ID) == null
                ) {
                    createAllNotificationChannels(context)
                }
            }

            val timeZone = ZoneId.systemDefault()
            val fajrTime = fajr.atZone(timeZone).toInstant().toEpochMilli()
            val sunriseTime = sunrise.atZone(timeZone).toInstant().toEpochMilli()
            val dhuhrTime = dhuhr.atZone(timeZone).toInstant().toEpochMilli()
            val asrTime = asr.atZone(timeZone).toInstant().toEpochMilli()
            val maghribTime = maghrib.atZone(timeZone).toInstant().toEpochMilli()
            val ishaaTime = ishaa.atZone(timeZone).toInstant().toEpochMilli()

            scheduleAlarms(
                context,
                fajrTime,
                sunriseTime,
                dhuhrTime,
                asrTime,
                maghribTime,
                ishaaTime
            )
        }
    }

    fun createPendingIntent(
        context: Context,
        requestCode: Int,
        notifyId: Int,
        timeToNotify: Long,
        title: String,
        channelid: String,
    ): PendingIntent {
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            putExtra("notifyid", notifyId)
            putExtra("time", timeToNotify)
            putExtra("title", title)
            putExtra("channelid", channelid)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun scheduleAlarms(
        context: Context,
        fajr: Long,
        sunrise: Long,
        dhuhr: Long,
        asr: Long,
        maghrib: Long,
        ishaa: Long,
    ) {
        with(AppConstants) {
            val pendingIntent1 = createPendingIntent(
                context, FAJR_PI_REQUEST_CODE, FAJR_NOTIFY_ID,
                fajr, CHANNEL_FAJR, FAJR_CHANNEL_ID
            )
            val pendingIntent2 = createPendingIntent(
                context, SUNRISE_PI_REQUEST_CODE, SUNRISE_NOTIFY_ID,
                sunrise, CHANNEL_SUNRISE, SUNRISE_CHANNEL_ID
            )
            val pendingIntent3 = createPendingIntent(
                context, DHUHR_PI_REQUEST_CODE, DHUHR_NOTIFY_ID,
                dhuhr, CHANNEL_ZUHAR, DHUHR_CHANNEL_ID
            )
            val pendingIntent4 = createPendingIntent(
                context, ASR_PI_REQUEST_CODE, ASR_NOTIFY_ID,
                asr, CHANNEL_ASAR, ASR_CHANNEL_ID
            )
            val pendingIntent5 = createPendingIntent(
                context, MAGHRIB_PI_REQUEST_CODE, MAGHRIB_NOTIFY_ID,
                maghrib, CHANNEL_MAGHRIB, MAGHRIB_CHANNEL_ID
            )
            val pendingIntent6 = createPendingIntent(
                context, ISHA_PI_REQUEST_CODE, ISHA_NOTIFY_ID,
                ishaa, CHANNEL_ISHAA, ISHA_CHANNEL_ID
            )

            // Set the alarms
            alarms.setExactAlarm(context, fajr, pendingIntent1)
            alarms.setExactAlarm(context, sunrise, pendingIntent2)
            alarms.setExactAlarm(context, dhuhr, pendingIntent3)
            alarms.setExactAlarm(context, asr, pendingIntent4)
            alarms.setExactAlarm(context, maghrib, pendingIntent5)
            alarms.setExactAlarm(context, ishaa, pendingIntent6)

            val prayerCompletedIntent = Intent(context, MissedPrayerReceiver::class.java)
            val prayerCompletedPendingIntent = PendingIntent.getBroadcast(
                context,
                PRAYER_COMPLETED_PENDING_INTENT_REQUEST_CODE,
                prayerCompletedIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val calendarPrayerCompleted = Calendar.getInstance().apply {
                if (get(Calendar.HOUR_OF_DAY) >= 23) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            alarms.setExactAlarm(
                context,
                calendarPrayerCompleted.timeInMillis,
                prayerCompletedPendingIntent,
            )
        }
    }

    fun createAllNotificationChannels(context: Context) {
        val fajrAdhan = "android.resource://${context.packageName}/${R.raw.fajr}"
        val zuharAdhan = "android.resource://${context.packageName}/${R.raw.zuhar}"
        val asarAdhan = "android.resource://${context.packageName}/${R.raw.asar}"
        val maghribAdhan = "android.resource://${context.packageName}/${R.raw.maghrib}"
        val ishaaAdhan = "android.resource://${context.packageName}/${R.raw.ishaa}"

        with(AppConstants) {
            //create notification channels
            //fajr
            notificationHelper.createNotificationChannel(
                context, NotificationManager.IMPORTANCE_MAX, true,
                CHANNEL_FAJR, CHANNEL_DESC_FAJR, FAJR_CHANNEL_ID, fajrAdhan
            )
            //sunrise
            notificationHelper.notificationChannelSilent(
                context, NotificationManager.IMPORTANCE_DEFAULT, false,
                CHANNEL_SUNRISE, CHANNEL_DESC_SUNRISE, SUNRISE_CHANNEL_ID
            )
            //zuhar
            notificationHelper.createNotificationChannel(
                context, NotificationManager.IMPORTANCE_MAX, true,
                CHANNEL_ZUHAR, CHANNEL_DESC_ZUHAR, DHUHR_CHANNEL_ID, zuharAdhan
            )
            //asar
            notificationHelper.createNotificationChannel(
                context, NotificationManager.IMPORTANCE_MAX, true,
                CHANNEL_ASAR, CHANNEL_DESC_ASAR, ASR_CHANNEL_ID, asarAdhan
            )
            //maghrib
            notificationHelper.createNotificationChannel(
                context, NotificationManager.IMPORTANCE_MAX, true,
                CHANNEL_MAGHRIB, CHANNEL_DESC_MAGHRIB, MAGHRIB_CHANNEL_ID, maghribAdhan
            )
            //ishaa
            notificationHelper.createNotificationChannel(
                context, NotificationManager.IMPORTANCE_MAX, true,
                CHANNEL_ISHAA, CHANNEL_DESC_ISHAA, ISHA_CHANNEL_ID, ishaaAdhan
            )
            //missed Prayer
            notificationHelper.notificationChannelSilent(
                context, NotificationManager.IMPORTANCE_MAX, true,
                CHANNEL_MISSED_PRAYER, CHANNEL_DESC_MISSED_PRAYER, CHANNEL_MISSED_PRAYER_ID
            )
        }
    }
}