package com.arshadshah.nimaz.core.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler.Companion.MIDNIGHT_REQUEST_CODE
import com.arshadshah.nimaz.domain.model.PrayerType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for prayer time notifications.
 * Handles scheduling, rescheduling, and cancellation of prayer notification alarms.
 */
@Singleton
class PrayerNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerTimeCalculator: PrayerTimeCalculator
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID_PRAYER = "prayer_notifications"
        const val CHANNEL_ID_ADHAN = "adhan_notifications"

        const val ACTION_PRAYER_NOTIFICATION = "com.arshadshah.nimaz.PRAYER_NOTIFICATION"
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_TIME = "prayer_time"

        // Request codes for different prayers (use prayer ordinal * 10 for different notification types)
        private const val REQUEST_CODE_BASE = 1000

        const val ACTION_MIDNIGHT_RESCHEDULE = "com.arshadshah.nimaz.MIDNIGHT_RESCHEDULE"
        private const val MIDNIGHT_REQUEST_CODE = 9999
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Prayer time notification channel
            val prayerChannel = NotificationChannel(
                CHANNEL_ID_PRAYER,
                "Prayer Time Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for prayer times"
                enableVibration(true)
                enableLights(true)
            }

            // Adhan notification channel (higher priority)
            val adhanChannel = NotificationChannel(
                CHANNEL_ID_ADHAN,
                "Adhan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Adhan sound notifications"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(listOf(prayerChannel, adhanChannel))
        }
    }

    /**
     * Schedule notifications for all prayers today.
     * This should be called after boot, when settings change, or at midnight.
     */
    fun scheduleTodaysPrayerNotifications(
        latitude: Double,
        longitude: Double,
        notificationsEnabled: Boolean
    ) {
        if (!notificationsEnabled) {
            cancelAllPrayerNotifications()
            return
        }

        if (latitude == 0.0 && longitude == 0.0) {
            return // No location set
        }

        val prayerTimes = prayerTimeCalculator.getPrayerTimes(latitude, longitude, LocalDate.now())
        val now = LocalDateTime.now()

        prayerTimes.forEach { prayerTime ->
            // Skip Sunrise as it's not a prayer
            if (prayerTime.type == PrayerType.SUNRISE) return@forEach

            val prayerLocalDateTime = prayerTime.time.toLocalDateTime(ZoneOffset.systemDefault() as ZoneOffset)

            // Only schedule if prayer time is in the future
            if (prayerLocalDateTime.isAfter(now)) {
                schedulePrayerNotification(prayerTime.type, prayerLocalDateTime)
            }
        }

        // Schedule midnight reschedule for tomorrow
        scheduleMidnightReschedule()
    }

    /**
     * Schedule a single prayer notification.
     */
    private fun schedulePrayerNotification(
        prayerType: PrayerType,
        prayerTime: LocalDateTime
    ) {
        val intent = Intent(ACTION_PRAYER_NOTIFICATION).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(EXTRA_PRAYER_NAME, prayerType.displayName)
            putExtra(EXTRA_PRAYER_TIME, prayerTime.toString())
        }

        val requestCode = REQUEST_CODE_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMillis = prayerTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Use setExactAndAllowWhileIdle for precise timing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancel notification for a specific prayer.
     */
    fun cancelPrayerNotification(prayerType: PrayerType) {
        val intent = Intent(ACTION_PRAYER_NOTIFICATION).apply {
            setPackage(context.packageName)
        }

        val requestCode = REQUEST_CODE_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Cancel all scheduled prayer notifications.
     */
    fun cancelAllPrayerNotifications() {
        PrayerType.entries.forEach { prayerType ->
            cancelPrayerNotification(prayerType)
        }
        cancelMidnightReschedule()
    }

    /**
     * Schedule a midnight alarm to reschedule tomorrow's prayers.
     */
    private fun scheduleMidnightReschedule() {
        val intent = Intent(ACTION_MIDNIGHT_RESCHEDULE).apply {
            setPackage(context.packageName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 12:01 AM tomorrow
        val tomorrow = LocalDate.now().plusDays(1)
        val midnight = tomorrow.atTime(0, 1)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                midnight,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                midnight,
                pendingIntent
            )
        }
    }

    private fun cancelMidnightReschedule() {
        val intent = Intent(ACTION_MIDNIGHT_RESCHEDULE).apply {
            setPackage(context.packageName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    // Extension to convert kotlinx.datetime.Instant to LocalDateTime
    private fun kotlinx.datetime.Instant.toLocalDateTime(zone: java.time.ZoneOffset): LocalDateTime {
        return java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}
