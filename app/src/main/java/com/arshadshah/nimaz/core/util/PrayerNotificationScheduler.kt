package com.arshadshah.nimaz.core.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
        const val CHANNEL_ID_DAILY_SUMMARY = "daily_summary_notifications"

        const val ACTION_PRAYER_NOTIFICATION = "com.arshadshah.nimaz.PRAYER_NOTIFICATION"
        const val ACTION_DAILY_SUMMARY = "com.arshadshah.nimaz.DAILY_SUMMARY"
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_TIME = "prayer_time"
        const val EXTRA_IS_PRE_REMINDER = "is_pre_reminder"

        // Request codes for different prayers (use prayer ordinal * 10 for different notification types)
        private const val REQUEST_CODE_BASE = 1000
        private const val PRE_REMINDER_REQUEST_CODE_BASE = 2000
        private const val TEST_NOTIFICATION_ID = 8888
        private const val DAILY_SUMMARY_REQUEST_CODE = 8889

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
                description = "Notifications for prayer times with Islamic reminders"
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

            // Daily summary notification channel
            val dailySummaryChannel = NotificationChannel(
                CHANNEL_ID_DAILY_SUMMARY,
                "Daily Prayer Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nightly summary of your daily prayer completion"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(listOf(prayerChannel, adhanChannel, dailySummaryChannel))
        }
    }

    /**
     * Schedule notifications for today's prayers.
     * This should be called after boot, when settings change, or at midnight.
     *
     * @param enabledPrayers If provided, only schedule for these prayer types. If null, schedule all non-sunrise prayers.
     * @param preReminderEnabled If true, schedule pre-reminder notifications.
     * @param preReminderMinutes Minutes before prayer to show pre-reminder.
     */
    fun scheduleTodaysPrayerNotifications(
        latitude: Double,
        longitude: Double,
        notificationsEnabled: Boolean,
        enabledPrayers: Set<PrayerType>? = null,
        preReminderEnabled: Boolean = false,
        preReminderMinutes: Int = 15,
        calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation: AsrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule: HighLatitudeRule? = null,
        adjustments: Map<PrayerType, Int> = emptyMap()
    ) {
        if (!notificationsEnabled) {
            cancelAllPrayerNotifications()
            return
        }

        if (latitude == 0.0 && longitude == 0.0) {
            return // No location set
        }

        // Cancel all first, then reschedule only enabled ones
        PrayerType.entries.forEach {
            cancelPrayerNotification(it)
            cancelPreReminderNotification(it)
        }

        val prayerTimes = prayerTimeCalculator.getPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            date = LocalDate.now(),
            calculationMethod = calculationMethod,
            asrCalculation = asrCalculation,
            highLatitudeRule = highLatitudeRule,
            adjustments = adjustments
        )
        val now = LocalDateTime.now()

        prayerTimes.forEach { prayerTime ->
            // Skip Sunrise by default, or skip if not in enabledPrayers set
            if (enabledPrayers != null) {
                if (prayerTime.type !in enabledPrayers) return@forEach
            } else {
                if (prayerTime.type == PrayerType.SUNRISE) return@forEach
            }

            val prayerLocalDateTime = prayerTime.time.toLocalDateTime()

            // Only schedule if prayer time is in the future
            if (prayerLocalDateTime.isAfter(now)) {
                schedulePrayerNotification(prayerTime.type, prayerLocalDateTime)

                // Schedule pre-reminder if enabled (not for sunrise)
                if (preReminderEnabled && prayerTime.type != PrayerType.SUNRISE) {
                    val preReminderTime = prayerLocalDateTime.minusMinutes(preReminderMinutes.toLong())
                    if (preReminderTime.isAfter(now)) {
                        schedulePreReminderNotification(prayerTime.type, preReminderTime)
                    }
                }
            }
        }

        // Schedule midnight reschedule for tomorrow
        scheduleMidnightReschedule()

        // Schedule daily summary notification at 11 PM
        scheduleDailySummary()
    }

    /**
     * Schedule daily summary notification at 11 PM.
     * This shows a summary of prayers completed/missed for the day.
     */
    private fun scheduleDailySummary() {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_DAILY_SUMMARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 11:00 PM today (or tomorrow if already past 11 PM)
        val now = LocalDateTime.now()
        var summaryTime = LocalDate.now().atTime(23, 0) // 11:00 PM

        if (now.isAfter(summaryTime)) {
            // If it's already past 11 PM, schedule for tomorrow
            summaryTime = LocalDate.now().plusDays(1).atTime(23, 0)
        }

        val triggerTimeMillis = summaryTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

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
     * Cancel the daily summary notification alarm.
     */
    fun cancelDailySummary() {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_DAILY_SUMMARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Schedule a pre-reminder notification for a prayer.
     */
    private fun schedulePreReminderNotification(
        prayerType: PrayerType,
        reminderTime: LocalDateTime
    ) {
        // Use explicit intent for BootReceiver (required for Android 8.0+)
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_PRAYER_NOTIFICATION
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(EXTRA_PRAYER_NAME, prayerType.displayName)
            putExtra(EXTRA_PRAYER_TIME, reminderTime.toString())
            putExtra(EXTRA_IS_PRE_REMINDER, true)
        }

        val requestCode = PRE_REMINDER_REQUEST_CODE_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMillis = reminderTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

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
     * Cancel pre-reminder notification for a specific prayer.
     */
    private fun cancelPreReminderNotification(prayerType: PrayerType) {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_PRAYER_NOTIFICATION
        }

        val requestCode = PRE_REMINDER_REQUEST_CODE_BASE + prayerType.ordinal
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
     * Schedule a single prayer notification.
     */
    private fun schedulePrayerNotification(
        prayerType: PrayerType,
        prayerTime: LocalDateTime
    ) {
        // Use explicit intent for BootReceiver (required for Android 8.0+)
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_PRAYER_NOTIFICATION
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
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_PRAYER_NOTIFICATION
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
            cancelPreReminderNotification(prayerType)
        }
        cancelMidnightReschedule()
    }

    /**
     * Send an immediate test notification to verify notifications are working.
     */
    fun sendTestNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PRAYER)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle("Test Notification")
            .setContentText("Prayer notifications are working correctly")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TEST_NOTIFICATION_ID, notification)
    }

    /**
     * Send test notifications for all prayers to validate the notification system.
     * Uses explicit broadcasts to ensure BootReceiver receives them on Android 8.0+.
     */
    fun sendAllPrayerTestNotifications() {
        val prayers = listOf(
            PrayerType.FAJR to "05:30 AM",
            PrayerType.SUNRISE to "06:45 AM",
            PrayerType.DHUHR to "12:30 PM",
            PrayerType.ASR to "03:45 PM",
            PrayerType.MAGHRIB to "06:15 PM",
            PrayerType.ISHA to "07:45 PM"
        )

        prayers.forEach { (prayerType, time) ->
            // Create explicit intent for BootReceiver (required for Android 8.0+)
            val intent = Intent(context, BootReceiver::class.java).apply {
                action = ACTION_PRAYER_NOTIFICATION
                putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
                putExtra(EXTRA_PRAYER_NAME, prayerType.displayName)
                putExtra(EXTRA_PRAYER_TIME, time)
            }

            // Send explicit broadcast to trigger the full notification flow
            context.sendBroadcast(intent)
        }
    }

    /**
     * Schedule a midnight alarm to reschedule tomorrow's prayers.
     */
    private fun scheduleMidnightReschedule() {
        // Use explicit intent for BootReceiver (required for Android 8.0+)
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_RESCHEDULE
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
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_RESCHEDULE
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
    private fun kotlinx.datetime.Instant.toLocalDateTime(): LocalDateTime {
        return java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}
