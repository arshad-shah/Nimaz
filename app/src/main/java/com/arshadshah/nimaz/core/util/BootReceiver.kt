package com.arshadshah.nimaz.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

                prayerNotificationScheduler.scheduleTodaysPrayerNotifications(
                    latitude = latitude,
                    longitude = longitude,
                    notificationsEnabled = notificationsEnabled
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

                prayerNotificationScheduler.scheduleTodaysPrayerNotifications(
                    latitude = latitude,
                    longitude = longitude,
                    notificationsEnabled = notificationsEnabled
                )
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }

    private fun handlePrayerNotification(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TIME) ?: ""

        // Show the notification
        showPrayerNotification(context, prayerName, prayerTime)
    }

    private fun showPrayerNotification(context: Context, prayerName: String, prayerTime: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create intent to open app when notification is tapped
        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = mainIntent?.let {
            android.app.PendingIntent.getActivity(
                context,
                0,
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = android.app.Notification.Builder(context, PrayerNotificationScheduler.CHANNEL_ID_PRAYER)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon
            .setContentTitle("$prayerName Time")
            .setContentText("It's time for $prayerName prayer")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }
}
