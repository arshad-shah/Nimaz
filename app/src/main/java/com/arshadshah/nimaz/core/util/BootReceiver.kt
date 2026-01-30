package com.arshadshah.nimaz.core.util

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * BroadcastReceiver that handles prayer notifications, boot events, and daily summaries.
 * Features enhanced notification layouts with Islamic greetings and motivational messages.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var prayerNotificationScheduler: PrayerNotificationScheduler

    @Inject
    lateinit var prayerRepository: PrayerRepository

    @Inject
    lateinit var adhanAudioManager: AdhanAudioManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                reschedulePrayerNotifications()
            }

            PrayerNotificationScheduler.ACTION_MIDNIGHT_RESCHEDULE -> {
                markMissedPrayersAndReschedule()
            }

            PrayerNotificationScheduler.ACTION_PRAYER_NOTIFICATION -> {
                handlePrayerNotification(context, intent)
            }

            PrayerNotificationScheduler.ACTION_DAILY_SUMMARY -> {
                handleDailySummary(context)
            }
        }
    }

    private fun reschedulePrayerNotifications() {
        scope.launch {
            try {
                val prefs = preferencesDataStore.userPreferences.first()
                val notificationsEnabled = prefs.prayerNotificationsEnabled
                val latitude = prefs.latitude
                val longitude = prefs.longitude

                val enabledPrayers = buildEnabledPrayersSet()
                val preReminderEnabled = preferencesDataStore.showReminderBefore.first()
                val preReminderMinutes = preferencesDataStore.notificationReminderMinutes.first()

                prayerNotificationScheduler.scheduleTodaysPrayerNotifications(
                    latitude = latitude,
                    longitude = longitude,
                    notificationsEnabled = notificationsEnabled,
                    enabledPrayers = enabledPrayers,
                    preReminderEnabled = preReminderEnabled,
                    preReminderMinutes = preReminderMinutes
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun markMissedPrayersAndReschedule() {
        scope.launch {
            try {
                prayerRepository.markPastPrayersAsMissed()

                val prefs = preferencesDataStore.userPreferences.first()
                val notificationsEnabled = prefs.prayerNotificationsEnabled
                val latitude = prefs.latitude
                val longitude = prefs.longitude

                val enabledPrayers = buildEnabledPrayersSet()
                val preReminderEnabled = preferencesDataStore.showReminderBefore.first()
                val preReminderMinutes = preferencesDataStore.notificationReminderMinutes.first()

                prayerNotificationScheduler.scheduleTodaysPrayerNotifications(
                    latitude = latitude,
                    longitude = longitude,
                    notificationsEnabled = notificationsEnabled,
                    enabledPrayers = enabledPrayers,
                    preReminderEnabled = preReminderEnabled,
                    preReminderMinutes = preReminderMinutes
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun buildEnabledPrayersSet(): Set<com.arshadshah.nimaz.domain.model.PrayerType> {
        return buildSet {
            if (preferencesDataStore.fajrNotificationEnabled.first()) add(com.arshadshah.nimaz.domain.model.PrayerType.FAJR)
            if (preferencesDataStore.sunriseNotificationEnabled.first()) add(com.arshadshah.nimaz.domain.model.PrayerType.SUNRISE)
            if (preferencesDataStore.dhuhrNotificationEnabled.first()) add(com.arshadshah.nimaz.domain.model.PrayerType.DHUHR)
            if (preferencesDataStore.asrNotificationEnabled.first()) add(com.arshadshah.nimaz.domain.model.PrayerType.ASR)
            if (preferencesDataStore.maghribNotificationEnabled.first()) add(com.arshadshah.nimaz.domain.model.PrayerType.MAGHRIB)
            if (preferencesDataStore.ishaNotificationEnabled.first()) add(com.arshadshah.nimaz.domain.model.PrayerType.ISHA)
        }
    }

    private fun handlePrayerNotification(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TIME) ?: ""
        val prayerType = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TYPE) ?: ""
        val isPreReminder = intent.getBooleanExtra(PrayerNotificationScheduler.EXTRA_IS_PRE_REMINDER, false)

        val isFajr = prayerType.equals("FAJR", ignoreCase = true)
        val isSunrise = prayerType.equals("SUNRISE", ignoreCase = true)

        scope.launch {
            try {
                val prayerNotificationEnabled = isPrayerNotificationEnabled(prayerType)
                if (!prayerNotificationEnabled) return@launch

                val vibrationEnabled = preferencesDataStore.notificationVibration.first()

                if (isPreReminder) {
                    val reminderMinutes = preferencesDataStore.notificationReminderMinutes.first()
                    showEnhancedPreReminderNotification(context, prayerName, prayerType, reminderMinutes, vibrationEnabled)
                    return@launch
                }

                val globalAdhanEnabled = preferencesDataStore.adhanEnabled.first()
                val prayerAdhanEnabled = preferencesDataStore.isAdhanEnabledForPrayer(prayerType).first()
                val selectedAdhan = preferencesDataStore.selectedAdhanSound.first()

                val respectDnd = preferencesDataStore.adhanRespectDnd.first()
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val isDndActive = notificationManager.currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                val dndBlocksAdhan = respectDnd && isDndActive

                val shouldPlayAdhan = globalAdhanEnabled && prayerAdhanEnabled && !isSunrise && !dndBlocksAdhan
                val shouldPlayBeep = globalAdhanEnabled && isSunrise && !dndBlocksAdhan

                // Get notification content for merging into adhan service notification
                val notifTitle = NotificationContentHelper.getPrayerTitle(prayerType)
                val notifMessage = NotificationContentHelper.getPrayerMessage(prayerType, prayerTime)
                val notifColor = getPrayerColor(prayerType)

                if (shouldPlayAdhan) {
                    val adhanSound = AdhanSound.fromName(selectedAdhan)
                    // Always check both variants so either can serve as fallback
                    val hasAdhan = adhanAudioManager.isDownloaded(adhanSound, true) ||
                            adhanAudioManager.isDownloaded(adhanSound, false)

                    if (hasAdhan) {
                        // Adhan service notification serves as both prayer + adhan notification
                        AdhanPlaybackService.playAdhan(
                            context = context,
                            adhanSound = adhanSound,
                            isFajr = isFajr,
                            prayerName = prayerName,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            notificationTitle = notifTitle,
                            notificationMessage = notifMessage,
                            notificationColor = notifColor
                        )
                    } else {
                        // Adhan file not available, show standalone notification
                        showEnhancedPrayerNotification(
                            context = context,
                            prayerName = prayerName,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            adhanEnabled = false,
                            vibrationEnabled = vibrationEnabled
                        )
                    }
                } else if (shouldPlayBeep) {
                    val beepSound = AdhanSound.SIMPLE_BEEP
                    if (adhanAudioManager.isDownloaded(beepSound, false)) {
                        AdhanPlaybackService.playAdhan(
                            context = context,
                            adhanSound = beepSound,
                            isFajr = false,
                            prayerName = prayerName,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            notificationTitle = notifTitle,
                            notificationMessage = notifMessage,
                            notificationColor = notifColor
                        )
                    } else {
                        showEnhancedPrayerNotification(
                            context = context,
                            prayerName = prayerName,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            adhanEnabled = false,
                            vibrationEnabled = vibrationEnabled
                        )
                    }
                } else {
                    // No adhan - show standalone prayer notification
                    showEnhancedPrayerNotification(
                        context = context,
                        prayerName = prayerName,
                        prayerType = prayerType,
                        prayerTime = prayerTime,
                        adhanEnabled = false,
                        vibrationEnabled = vibrationEnabled
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showEnhancedPrayerNotification(context, prayerName, prayerType, prayerTime, false, true)
            }
        }
    }

    private suspend fun isPrayerNotificationEnabled(prayerType: String): Boolean {
        return when (prayerType.uppercase()) {
            "FAJR" -> preferencesDataStore.fajrNotificationEnabled.first()
            "SUNRISE" -> preferencesDataStore.sunriseNotificationEnabled.first()
            "DHUHR" -> preferencesDataStore.dhuhrNotificationEnabled.first()
            "ASR" -> preferencesDataStore.asrNotificationEnabled.first()
            "MAGHRIB" -> preferencesDataStore.maghribNotificationEnabled.first()
            "ISHA" -> preferencesDataStore.ishaNotificationEnabled.first()
            else -> true
        }
    }

    /**
     * Show an enhanced pre-reminder notification with motivational content.
     */
    private fun showEnhancedPreReminderNotification(
        context: Context,
        prayerName: String,
        prayerType: String,
        minutesBefore: Int,
        vibrationEnabled: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openPendingIntent = mainIntent?.let {
            PendingIntent.getActivity(
                context,
                (prayerName + "_reminder").hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val title = NotificationContentHelper.getPreReminderTitle(prayerName, minutesBefore)
        val message = NotificationContentHelper.getPreReminderMessage(prayerName)
        val bigText = "$message\n\n${NotificationContentHelper.getTimeBasedGreeting()}"

        val notification = NotificationCompat.Builder(context, PrayerNotificationScheduler.CHANNEL_ID_PRAYER)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                if (!vibrationEnabled) {
                    setVibrate(longArrayOf(0L))
                }
            }
            .build()

        notificationManager.notify((prayerName + "_reminder").hashCode(), notification)
    }

    /**
     * Show an enhanced prayer notification with Islamic greetings and motivational messages.
     */
    private fun showEnhancedPrayerNotification(
        context: Context,
        prayerName: String,
        prayerType: String,
        prayerTime: String,
        adhanEnabled: Boolean = false,
        vibrationEnabled: Boolean = true
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create intent to open app and stop adhan
        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(EXTRA_STOP_ADHAN, true)
        }
        val openPendingIntent = mainIntent?.let {
            PendingIntent.getActivity(
                context,
                prayerName.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Create intent to stop adhan when notification is dismissed
        val dismissIntent = Intent(context, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_STOP
        }
        val dismissPendingIntent = PendingIntent.getService(
            context,
            prayerName.hashCode() + 1000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get enhanced content
        val title = NotificationContentHelper.getPrayerTitle(prayerType)
        val shortMessage = NotificationContentHelper.getShortMessage(prayerType)
        val fullMessage = NotificationContentHelper.getPrayerMessage(prayerType, prayerTime)

        // Build the big text with formatted time if available
        // Don't say "Prayer time" for sunrise since it's not a prayer
        val isSunrise = prayerType.equals("SUNRISE", ignoreCase = true)
        val timeDisplay = if (prayerTime.isNotEmpty()) {
            if (isSunrise) "\n\nSunrise: $prayerTime" else "\n\nPrayer time: $prayerTime"
        } else ""
        val bigText = "$fullMessage$timeDisplay"

        val channelId = if (adhanEnabled) {
            PrayerNotificationScheduler.CHANNEL_ID_ADHAN
        } else {
            PrayerNotificationScheduler.CHANNEL_ID_PRAYER
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle(title)
            .setContentText(shortMessage)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(bigText)
                .setBigContentTitle(title))
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColorized(true)
            .setColor(getPrayerColor(prayerType))
            .apply {
                if (!vibrationEnabled) {
                    setVibrate(longArrayOf(0L))
                }
                // Add action to mark prayer as done (optional future feature)
                // addAction(R.drawable.ic_check, "Prayed", markPrayedIntent)
            }
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }

    /**
     * Handle the daily summary notification at 11 PM.
     */
    private fun handleDailySummary(context: Context) {
        scope.launch {
            try {
                // Check if notifications are enabled
                val prefs = preferencesDataStore.userPreferences.first()
                if (!prefs.prayerNotificationsEnabled) return@launch

                // Get today's prayer records
                val todayEpoch = LocalDate.now()
                    .atStartOfDay()
                    .toEpochSecond(ZoneOffset.UTC) * 1000

                val prayerRecords = prayerRepository.getPrayerRecordsForDate(todayEpoch).first()

                // Count prayed and missed (excluding Sunrise)
                val mainPrayers = prayerRecords.filter { it.prayerName != PrayerName.SUNRISE }
                val prayedCount = mainPrayers.count { it.status == PrayerStatus.PRAYED || it.status == PrayerStatus.LATE }
                val missedCount = mainPrayers.count { it.status == PrayerStatus.MISSED || it.status == PrayerStatus.NOT_PRAYED }
                val missedPrayers = mainPrayers
                    .filter { it.status == PrayerStatus.MISSED || it.status == PrayerStatus.NOT_PRAYED }
                    .map { it.prayerName.displayName() }

                // Get notification content
                val summaryContent = NotificationContentHelper.getDailySummaryContent(
                    prayedCount = prayedCount,
                    missedCount = missedCount,
                    missedPrayers = missedPrayers
                )

                showDailySummaryNotification(context, summaryContent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Show the daily summary notification.
     */
    private fun showDailySummaryNotification(
        context: Context,
        content: NotificationContentHelper.DailySummaryContent
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openPendingIntent = mainIntent?.let {
            PendingIntent.getActivity(
                context,
                "daily_summary".hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Choose color based on positive/negative outcome
        val notificationColor = if (content.isPositive) {
            0xFF4CAF50.toInt() // Green for positive
        } else {
            0xFFFF9800.toInt() // Orange for needs improvement
        }

        val notification = NotificationCompat.Builder(context, PrayerNotificationScheduler.CHANNEL_ID_DAILY_SUMMARY)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle(content.title)
            .setContentText(content.message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(content.bigText)
                .setBigContentTitle(content.title))
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setColorized(true)
            .setColor(notificationColor)
            .build()

        notificationManager.notify("daily_summary".hashCode(), notification)
    }

    /**
     * Get a color for the prayer notification based on prayer type.
     */
    private fun getPrayerColor(prayerType: String): Int {
        return when (prayerType.uppercase()) {
            "FAJR" -> 0xFF3F51B5.toInt()     // Indigo - dawn
            "SUNRISE" -> 0xFFFF9800.toInt()  // Orange - sun
            "DHUHR" -> 0xFF2196F3.toInt()    // Blue - midday sky
            "ASR" -> 0xFF009688.toInt()      // Teal - afternoon
            "MAGHRIB" -> 0xFFE91E63.toInt()  // Pink - sunset
            "ISHA" -> 0xFF673AB7.toInt()     // Deep Purple - night
            else -> 0xFF4CAF50.toInt()       // Green - default
        }
    }

    companion object {
        const val EXTRA_STOP_ADHAN = "stop_adhan"
    }
}
