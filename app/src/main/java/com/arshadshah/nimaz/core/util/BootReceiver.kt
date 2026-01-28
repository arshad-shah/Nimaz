package com.arshadshah.nimaz.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that reschedules prayer notifications after device boot.
 * Also handles midnight reschedule events for daily prayer notification updates.
 */
@AndroidEntryPoint
class  BootReceiver : BroadcastReceiver() {

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
                // Device has booted, reschedule all prayer notifications
                reschedulePrayerNotifications()
            }

            PrayerNotificationScheduler.ACTION_MIDNIGHT_RESCHEDULE -> {
                // Midnight reschedule - mark yesterday's unprayed prayers as missed
                // and schedule today's new prayer notifications
                markMissedPrayersAndReschedule()
            }

            PrayerNotificationScheduler.ACTION_PRAYER_NOTIFICATION -> {
                // Prayer time has arrived - show notification
                handlePrayerNotification(context, intent)
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

                // Build enabled prayers set
                val enabledPrayers = buildEnabledPrayersSet()

                // Get pre-reminder settings
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
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }

    private fun markMissedPrayersAndReschedule() {
        scope.launch {
            try {
                // Mark any past pending/not_prayed prayers as missed
                prayerRepository.markPastPrayersAsMissed()

                // Then reschedule today's notifications
                val prefs = preferencesDataStore.userPreferences.first()
                val notificationsEnabled = prefs.prayerNotificationsEnabled
                val latitude = prefs.latitude
                val longitude = prefs.longitude

                // Build enabled prayers set
                val enabledPrayers = buildEnabledPrayersSet()

                // Get pre-reminder settings
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
                // Log error but don't crash
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

        // Check if this is Fajr prayer (uses special adhan with "prayer is better than sleep")
        val isFajr = prayerType.equals("FAJR", ignoreCase = true)
        val isSunrise = prayerType.equals("SUNRISE", ignoreCase = true)

        scope.launch {
            try {
                // First check if this prayer's notification is enabled
                val prayerNotificationEnabled = isPrayerNotificationEnabled(prayerType)
                if (!prayerNotificationEnabled) {
                    return@launch // Don't show notification if this prayer is disabled
                }

                // Get vibration setting
                val vibrationEnabled = preferencesDataStore.notificationVibration.first()

                // For pre-reminders, show a simpler notification without adhan
                if (isPreReminder) {
                    val reminderMinutes = preferencesDataStore.notificationReminderMinutes.first()
                    showPreReminderNotification(context, prayerName, reminderMinutes, vibrationEnabled)
                    return@launch
                }

                // Check global adhan setting AND per-prayer adhan setting
                val globalAdhanEnabled = preferencesDataStore.adhanEnabled.first()
                val prayerAdhanEnabled = preferencesDataStore.isAdhanEnabledForPrayer(prayerType).first()
                val selectedAdhan = preferencesDataStore.selectedAdhanSound.first()

                // Adhan plays only if both global AND per-prayer settings are enabled
                // Sunrise never gets full adhan - only beep
                val shouldPlayAdhan = globalAdhanEnabled && prayerAdhanEnabled && !isSunrise
                val shouldPlayBeep = globalAdhanEnabled && isSunrise

                // Show the notification
                showPrayerNotification(context, prayerName, prayerTime, shouldPlayAdhan || shouldPlayBeep, vibrationEnabled)

                if (shouldPlayAdhan) {
                    // Play full adhan for regular prayers
                    val adhanSound = AdhanSound.fromName(selectedAdhan)
                    val hasAdhan = adhanAudioManager.isDownloaded(adhanSound, isFajr) ||
                            adhanAudioManager.isDownloaded(adhanSound, false)

                    if (hasAdhan) {
                        AdhanPlaybackService.playAdhan(
                            context = context,
                            adhanSound = adhanSound,
                            isFajr = isFajr,
                            prayerName = prayerName
                        )
                    }
                } else if (shouldPlayBeep) {
                    // Play only beep for sunrise
                    val beepSound = AdhanSound.SIMPLE_BEEP
                    if (adhanAudioManager.isDownloaded(beepSound, false)) {
                        AdhanPlaybackService.playAdhan(
                            context = context,
                            adhanSound = beepSound,
                            isFajr = false,
                            prayerName = prayerName
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: show notification without adhan
                showPrayerNotification(context, prayerName, prayerTime, false, true)
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

    private fun showPreReminderNotification(context: Context, prayerName: String, minutesBefore: Int, vibrationEnabled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openPendingIntent = mainIntent?.let {
            android.app.PendingIntent.getActivity(
                context,
                (prayerName + "_reminder").hashCode(),
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = android.app.Notification.Builder(context, PrayerNotificationScheduler.CHANNEL_ID_PRAYER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$prayerName in $minutesBefore minutes")
            .setContentText("Prepare for $prayerName prayer")
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .apply {
                if (!vibrationEnabled) {
                    setVibrate(longArrayOf(0L)) // No vibration
                }
            }
            .build()

        notificationManager.notify((prayerName + "_reminder").hashCode(), notification)
    }

    private fun showPrayerNotification(context: Context, prayerName: String, prayerTime: String, adhanEnabled: Boolean = false, vibrationEnabled: Boolean = true) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create intent to open app and stop adhan when notification is tapped
        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            // Add action to stop adhan when app is opened
            putExtra(EXTRA_STOP_ADHAN, true)
        }
        val openPendingIntent = mainIntent?.let {
            android.app.PendingIntent.getActivity(
                context,
                prayerName.hashCode(),
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Create intent to stop adhan when notification is dismissed
        val dismissIntent = Intent(context, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_STOP
        }
        val dismissPendingIntent = android.app.PendingIntent.getService(
            context,
            prayerName.hashCode() + 1000,
            dismissIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Use adhan channel if adhan is enabled, otherwise use regular prayer channel
        val channelId = if (adhanEnabled) {
            PrayerNotificationScheduler.CHANNEL_ID_ADHAN
        } else {
            PrayerNotificationScheduler.CHANNEL_ID_PRAYER
        }

        val notification = android.app.Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon
            .setContentTitle("$prayerName Time")
            .setContentText("It's time for $prayerName prayer")
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(dismissPendingIntent) // Stop adhan when notification is dismissed
            .apply {
                if (!vibrationEnabled) {
                    setVibrate(longArrayOf(0L)) // No vibration
                }
            }
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }

    companion object {
        const val EXTRA_STOP_ADHAN = "stop_adhan"
    }
}
