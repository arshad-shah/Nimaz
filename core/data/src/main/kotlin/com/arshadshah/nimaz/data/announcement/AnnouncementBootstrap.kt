package com.arshadshah.nimaz.data.announcement

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot per-launch setup for FCM announcements: creates the dedicated
 * low-importance "Updates & Announcements" channel (kept strictly separate
 * from the prayer/adhan channels) and (re-)subscribes to the announcements
 * topic. Both operations are idempotent; failures are logged, never thrown.
 */
@Singleton
class AnnouncementBootstrap @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun initialize() {
        createChannel()
        subscribeToTopic()
    }

    private fun createChannel() {
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Updates & Announcements",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Feature announcements, changelog highlights and policy updates"
            }
            manager.createNotificationChannel(channel)
        }.onFailure { e ->
            AppAnalytics.logError("announcements", e.javaClass.simpleName, e.message)
        }
    }

    private fun subscribeToTopic() {
        // No-op on builds without google-services.json (Firebase never initializes).
        if (FirebaseApp.getApps(context).isEmpty()) return
        runCatching {
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
                .addOnFailureListener { e ->
                    AppAnalytics.logError("announcements", e.javaClass.simpleName, e.message)
                }
        }.onFailure { e ->
            AppAnalytics.logError("announcements", e.javaClass.simpleName, e.message)
        }
    }

    companion object {
        /** Must match the manifest's default_notification_channel_id meta-data. */
        const val CHANNEL_ID = "nimaz_announcements"
        const val TOPIC = "announcements"
    }
}
