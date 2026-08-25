package com.arshadshah.nimaz.data.announcement

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Per-launch setup for FCM announcements.
 *
 * Both halves are idempotent by design and neither may throw: this runs during app start, so an
 * exception here is a phone that will not open. The properties that matter:
 *
 *  - **announcements get their own low-importance channel**, kept strictly apart from the
 *    prayer and adhan channels. Reusing one of those would make a feature announcement play the
 *    adhan; giving it default importance would make it interrupt;
 *  - **the channel id must match the manifest's `default_notification_channel_id`.** When the
 *    app is killed the OS posts the tray notification itself and looks the channel up by that
 *    id — get it wrong and background announcements are silently dropped by the system;
 *  - **a build with no `google-services.json` must not try to subscribe.** Firebase never
 *    initialises there, and calling into it throws on the launch path.
 */
@RunWith(RobolectricTestRunner::class)
class AnnouncementBootstrapTest {

    private lateinit var context: Context
    private lateinit var bootstrap: AnnouncementBootstrap

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bootstrap = AnnouncementBootstrap(context)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `the announcements channel is created, low importance and separate`() {
        bootstrap.initialize()

        val channel = notificationManager()
            .getNotificationChannel(AnnouncementBootstrap.CHANNEL_ID)!!

        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_LOW)
        assertThat(channel.name.toString()).isEqualTo("Updates & Announcements")
        assertThat(channel.description).isNotEmpty()
        // The platform's own notification sound, not one of the app's adhan recordings: an
        // announcement must never play the call to prayer.
        assertThat(channel.sound?.toString().orEmpty()).doesNotContain(context.packageName)
    }

    @Test
    fun `the channel id is the one the manifest hands the OS`() {
        // The OS looks this up by id when it posts the tray notification itself; a mismatch
        // drops every background announcement with no error anywhere.
        assertThat(AnnouncementBootstrap.CHANNEL_ID).isEqualTo("nimaz_announcements")
        assertThat(AnnouncementBootstrap.TOPIC).isEqualTo("announcements")
    }

    @Test
    fun `initialising twice leaves one channel, not two`() {
        bootstrap.initialize()
        bootstrap.initialize()

        assertThat(
            shadowOf(notificationManager()).notificationChannels
                .count { (it as android.app.NotificationChannel).id == AnnouncementBootstrap.CHANNEL_ID }
        ).isEqualTo(1)
    }

    @Test
    fun `a build with no Firebase config does not reach for messaging`() {
        // `FirebaseApp.getApps` is empty on a build without google-services.json, which is the
        // guard: calling into FirebaseMessaging there throws on the launch path.
        mockkStatic(com.google.firebase.FirebaseApp::class)
        every { com.google.firebase.FirebaseApp.getApps(any()) } returns emptyList()
        mockkStatic(com.google.firebase.messaging.FirebaseMessaging::class)

        bootstrap.initialize()

        verify(exactly = 0) { com.google.firebase.messaging.FirebaseMessaging.getInstance() }
        assertThat(notificationManager().getNotificationChannel(AnnouncementBootstrap.CHANNEL_ID))
            .isNotNull()
    }

    @Test
    fun `a configured build subscribes to the announcements topic`() {
        mockkStatic(com.google.firebase.FirebaseApp::class)
        every { com.google.firebase.FirebaseApp.getApps(any()) } returns
            listOf(mockk(relaxed = true))
        mockkStatic(com.google.firebase.messaging.FirebaseMessaging::class)
        val messaging = mockk<com.google.firebase.messaging.FirebaseMessaging>(relaxed = true)
        every { com.google.firebase.messaging.FirebaseMessaging.getInstance() } returns messaging

        bootstrap.initialize()

        verify { messaging.subscribeToTopic(AnnouncementBootstrap.TOPIC) }
    }

    @Test
    fun `a messaging call that throws does not take the launch down with it`() {
        mockkStatic(com.google.firebase.FirebaseApp::class)
        every { com.google.firebase.FirebaseApp.getApps(any()) } returns
            listOf(mockk(relaxed = true))
        mockkStatic(com.google.firebase.messaging.FirebaseMessaging::class)
        every { com.google.firebase.messaging.FirebaseMessaging.getInstance() } throws
            IllegalStateException("Firebase not initialised")

        // Logged, never thrown: this runs on the path that opens the app.
        bootstrap.initialize()

        assertThat(notificationManager().getNotificationChannel(AnnouncementBootstrap.CHANNEL_ID))
            .isNotNull()
    }

    private fun notificationManager() =
        context.getSystemService(NotificationManager::class.java)
}
