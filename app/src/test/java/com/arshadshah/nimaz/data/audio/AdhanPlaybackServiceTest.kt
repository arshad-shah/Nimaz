package com.arshadshah.nimaz.data.audio

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.NimazChannels
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.testing.TestEntryPointApplication
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

/**
 * The service that actually makes the sound at prayer time.
 *
 * Its whole job is file selection and a foreground notification, and both had been unreachable:
 * `@AndroidEntryPoint` made `onCreate` demand a Hilt application, so the class reported 6% with
 * only its companion's intent builders covered.
 *
 * The file selection is the interesting half. **A missing adhan must never fall back to the other
 * variant** — playing the Fajr adhan at Dhuhr is wrong, not merely surprising — so the fallback is
 * the beep or nothing. That rule is one `if` and no test had ever run it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestEntryPointApplication::class, sdk = [34])
class AdhanPlaybackServiceTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var adhanDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        adhanDir = File(context.filesDir, "adhan").apply { mkdirs() }
        adhanDir.listFiles()?.forEach { it.delete() }

        TestEntryPointApplication.Injector.reset()
        TestEntryPointApplication.Injector.adhanPlayback = {
            it.adhanAudioManager = mockk(relaxed = true)
        }
    }

    @After
    fun tearDown() {
        adhanDir.listFiles()?.forEach { it.delete() }
    }

    /** A file the service will accept: anything over 1 KB. */
    private fun writeAudio(name: String, bytes: Int = 2_048) {
        File(adhanDir, name).writeBytes(ByteArray(bytes))
    }

    private fun play(
        sound: AdhanSound = AdhanSound.MISHARY,
        isFajr: Boolean = false,
        prayerName: String = "Dhuhr",
        title: String = "",
        message: String = "",
        color: Int = 0,
    ): ServiceController<AdhanPlaybackService> {
        val intent = Intent(context, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_PLAY
            putExtra(AdhanPlaybackService.EXTRA_ADHAN_SOUND, sound.name)
            putExtra(AdhanPlaybackService.EXTRA_IS_FAJR, isFajr)
            putExtra(AdhanPlaybackService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(AdhanPlaybackService.EXTRA_NOTIFICATION_TITLE, title)
            putExtra(AdhanPlaybackService.EXTRA_NOTIFICATION_MESSAGE, message)
            putExtra(AdhanPlaybackService.EXTRA_NOTIFICATION_COLOR, color)
        }
        return Robolectric.buildService(AdhanPlaybackService::class.java, intent)
            .create()
            .startCommand(0, 0)
    }

    private fun notificationFor(prayerName: String): Notification? =
        shadowOf(notificationManager).getNotification(prayerName.hashCode())

    // ── The channel ─────────────────────────────────────────────────────────────

    @Test
    fun `the playback channel is silent, because the audio is the sound`() {
        // A channel with a sound of its own would play a notification tone over the adhan.
        Robolectric.buildService(AdhanPlaybackService::class.java).create()

        val channel = notificationManager.notificationChannels
            .single { it.id == NimazChannels.ADHAN_PLAYBACK }
        assertThat(channel.sound).isNull()
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_LOW)
    }

    // ── File selection ──────────────────────────────────────────────────────────

    @Test
    fun `the requested adhan plays when its file is there`() {
        writeAudio(AdhanSound.MISHARY.getFileName(false))

        play(sound = AdhanSound.MISHARY, isFajr = false, prayerName = "Dhuhr")

        assertThat(notificationFor("Dhuhr")).isNotNull()
    }

    @Test
    fun `a missing adhan falls back to the beep, never to the other variant`() {
        // Playing the Fajr adhan at Dhuhr is a religious-content error, not a UX one. The
        // regular file exists here and must still not be chosen for a Fajr request.
        writeAudio(AdhanSound.MISHARY.getFileName(false))
        writeAudio(AdhanSound.SIMPLE_BEEP.getFileName(false))

        play(sound = AdhanSound.MISHARY, isFajr = true, prayerName = "Fajr")

        // Something played — the beep — and the service did not give up.
        assertThat(notificationFor("Fajr")).isNotNull()
    }

    @Test
    fun `a truncated file is treated as missing`() {
        // A captive-portal HTML body saved as an mp3 is a real outcome of a failed download,
        // and it is not silent — it is a crash inside the player.
        writeAudio(AdhanSound.MISHARY.getFileName(false), bytes = 100)
        writeAudio(AdhanSound.SIMPLE_BEEP.getFileName(false))

        play(sound = AdhanSound.MISHARY, prayerName = "Asr")

        assertThat(notificationFor("Asr")).isNotNull()
    }

    @Test
    fun `no adhan and no beep stops the service instead of pretending to play`() {
        val controller = play(sound = AdhanSound.MISHARY, prayerName = "Isha")

        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
        assertThat(notificationFor("Isha")).isNull()
    }

    // ── The merged notification ─────────────────────────────────────────────────

    @Test
    fun `the playback notification carries the prayer notification's own text`() {
        // The service notification *is* the prayer notification — the receiver hands its title
        // and body over so the shade shows one entry rather than two.
        writeAudio(AdhanSound.MISHARY.getFileName(false))

        play(prayerName = "Dhuhr", title = "Dhuhr · 12:30", message = "Time to pray")

        val notification = notificationFor("Dhuhr")!!
        assertThat(notification.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("Dhuhr · 12:30")
        assertThat(notification.extras.getString(Notification.EXTRA_TEXT))
            .isEqualTo("Time to pray")
    }

    @Test
    fun `an empty title falls back to something rather than showing a blank notification`() {
        writeAudio(AdhanSound.MISHARY.getFileName(false))

        play(prayerName = "Maghrib", title = "", message = "")

        val notification = notificationFor("Maghrib")!!
        assertThat(notification.extras.getString(Notification.EXTRA_TITLE)).contains("Maghrib")
        assertThat(notification.extras.getString(Notification.EXTRA_TEXT)).isNotEmpty()
    }

    @Test
    fun `it posts on the adhan channel, so the vibration preference still applies`() {
        writeAudio(AdhanSound.MISHARY.getFileName(false))

        play(prayerName = "Asr")

        assertThat(notificationFor("Asr")!!.channelId)
            .isEqualTo(NimazChannels.ADHAN)
    }

    @Test
    fun `a colour is only applied when one was supplied`() {
        writeAudio(AdhanSound.MISHARY.getFileName(false))

        play(prayerName = "Fajr", color = 0xFF3F51B5.toInt())
        assertThat(notificationFor("Fajr")!!.color).isEqualTo(0xFF3F51B5.toInt())

        notificationManager.cancelAll()
        play(prayerName = "Isha", color = 0)
        assertThat(notificationFor("Isha")!!.color).isEqualTo(0)
    }

    @Test
    fun `the notification offers a way to stop the adhan`() {
        // Without it a user in a meeting has to force-stop the app.
        writeAudio(AdhanSound.MISHARY.getFileName(false))

        play(prayerName = "Dhuhr")

        val notification = notificationFor("Dhuhr")!!
        assertThat(notification.actions).isNotEmpty()
        assertThat(notification.deleteIntent).isNotNull()
    }

    // ── Stopping ────────────────────────────────────────────────────────────────

    @Test
    fun `the stop action stops the service`() {
        val controller = Robolectric.buildService(
            AdhanPlaybackService::class.java,
            Intent(context, AdhanPlaybackService::class.java).apply {
                action = AdhanPlaybackService.ACTION_STOP
            },
        ).create().startCommand(0, 0)

        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `a restart with a null intent stops rather than playing something arbitrary`() {
        // START_NOT_STICKY makes this unlikely, not impossible; playing "whatever was last
        // requested" hours later is the failure being avoided.
        val controller = Robolectric.buildService(AdhanPlaybackService::class.java)
            .create()
            .startCommand(0, 0)

        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `destroying the service releases everything it held`() {
        writeAudio(AdhanSound.MISHARY.getFileName(false))
        val controller = play(prayerName = "Dhuhr")

        controller.destroy()

        // The assertion that matters is that this does not throw: the wake lock, the audio
        // focus request and the player are all released on a path that runs from onDestroy.
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isFalse()
    }

    // ── The intent builders ─────────────────────────────────────────────────────

    @Test
    fun `playAdhan starts a foreground service carrying everything the notification needs`() {
        AdhanPlaybackService.playAdhan(
            context = context,
            adhanSound = AdhanSound.MISHARY,
            isFajr = true,
            prayerName = "Fajr",
            prayerType = "FAJR",
            prayerTime = "05:30",
            notificationTitle = "Fajr · 05:30",
            notificationMessage = "Time to pray",
            notificationColor = 1,
        )

        val intent = shadowOf(context as Application).nextStartedService
        assertThat(intent.action).isEqualTo(AdhanPlaybackService.ACTION_PLAY)
        assertThat(intent.getStringExtra(AdhanPlaybackService.EXTRA_ADHAN_SOUND))
            .isEqualTo(AdhanSound.MISHARY.name)
        assertThat(intent.getBooleanExtra(AdhanPlaybackService.EXTRA_IS_FAJR, false)).isTrue()
        assertThat(intent.getStringExtra(AdhanPlaybackService.EXTRA_NOTIFICATION_TITLE))
            .isEqualTo("Fajr · 05:30")
    }

    @Test
    fun `stopAdhan asks the running service to stop`() {
        AdhanPlaybackService.stopAdhan(context)

        assertThat(shadowOf(context as Application).nextStartedService.action)
            .isEqualTo(AdhanPlaybackService.ACTION_STOP)
    }
}
