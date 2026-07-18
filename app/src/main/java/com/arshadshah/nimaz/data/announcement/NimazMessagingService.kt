package com.arshadshah.nimaz.data.announcement

import android.util.Log
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Receives FCM announcement messages (topic broadcast, sent from the Firebase
 * console). For notification+data messages this callback fires only while the
 * app is in the FOREGROUND — when backgrounded/killed the OS posts the tray
 * notification itself (on the channel named in the manifest meta-data) and the
 * tap intent is handled by MainActivity instead.
 *
 * Parse-and-write only: FCM allows a short execution window here, so no
 * network or heavy work. No system notification is posted for foreground
 * receipt — the Home banner is the surface.
 */
@AndroidEntryPoint
class NimazMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var repository: AnnouncementRepository

    @Inject
    lateinit var mapper: AnnouncementPayloadMapper

    override fun onMessageReceived(message: RemoteMessage) {
        // This runs headless: an uncaught throw here is invisible, so report it.
        try {
            val announcement = mapper.fromPayload(message.data) ?: return
            // Blocking keeps the write inside the callback's execution window;
            // a DataStore edit is a fast local disk write.
            runBlocking { repository.setAnnouncement(announcement) }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            AppAnalytics.logError("announcements", e.javaClass.simpleName, e.message)
        }
    }

    override fun onNewToken(token: String) {
        // Topic broadcast only — tokens are never stored or sent anywhere.
        Log.d(TAG, "FCM registration token rotated")
    }

    private companion object {
        const val TAG = "NimazMessagingService"
    }
}
