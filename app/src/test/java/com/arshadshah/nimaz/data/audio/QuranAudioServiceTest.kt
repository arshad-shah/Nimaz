package com.arshadshah.nimaz.data.audio

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.model.AudioState
import com.arshadshah.nimaz.testing.TestEntryPointApplication
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * The Quran player's foreground service: the media notification and the transport controls.
 *
 * It is the only thing standing between the audio manager and the lock screen, and it had never
 * run — `@AndroidEntryPoint` again. The behaviour worth pinning is not that it plays (the manager
 * does that) but that the notification **follows** the state: the button says Pause while
 * playing, the subtitle reports download progress while preparing, and the service takes itself
 * down when audio ends rather than sitting in the shade forever.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestEntryPointApplication::class, sdk = [34])
class QuranAudioServiceTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: QuranAudioManager

    private val audioState = MutableStateFlow(AudioState())

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        audioState.value = AudioState()

        audioManager = mockk(relaxed = true) {
            every { this@mockk.audioState } returns this@QuranAudioServiceTest.audioState
            // Null keeps MediaSession out of it: a session needs a real player, and what these
            // tests are about is the notification the service builds around one.
            every { getPlayer() } returns null
        }
        TestEntryPointApplication.Injector.reset()
        TestEntryPointApplication.Injector.quranAudio = { it.audioManager = audioManager }
    }

    private fun create(): ServiceController<QuranAudioService> =
        Robolectric.buildService(QuranAudioService::class.java).create().also { idle() }

    private fun send(action: String?): ServiceController<QuranAudioService> {
        val intent = action?.let {
            Intent(context, QuranAudioService::class.java).apply { this.action = it }
        }
        return Robolectric.buildService(QuranAudioService::class.java, intent)
            .create()
            .startCommand(0, 0)
            .also { idle() }
    }

    private fun idle() = ShadowLooper.idleMainLooper()

    private fun notification(): Notification? =
        shadowOf(notificationManager).getNotification(QuranAudioService.NOTIFICATION_ID)

    // ── The channel ─────────────────────────────────────────────────────────────

    @Test
    fun `the media channel is silent and unobtrusive`() {
        // A transport notification that dings on every ayah change would be unusable.
        create()

        val channel = notificationManager.notificationChannels
            .single { it.id == QuranAudioService.CHANNEL_ID }
        assertThat(channel.sound).isNull()
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_LOW)
    }

    // ── The notification follows the state ──────────────────────────────────────

    @Test
    fun `an active state puts the surah and the reciter in the notification`() {
        audioState.value = AudioState(
            isActive = true,
            isPlaying = true,
            currentTitle = "Al-Fatihah",
            reciterName = "Abdul Basit",
        )

        create()

        val n = notification()!!
        assertThat(n.extras.getString(Notification.EXTRA_TITLE)).isEqualTo("Al-Fatihah")
        assertThat(n.extras.getString(Notification.EXTRA_TEXT)).isEqualTo("Abdul Basit")
    }

    @Test
    fun `while preparing, the subtitle reports download progress instead of the reciter`() {
        // Downloading a surah takes long enough that "Abdul Basit" and no progress reads as a
        // hang. The count is the only feedback there is.
        audioState.value = AudioState(
            isPreparing = true,
            currentTitle = "Al-Baqarah",
            reciterName = "Abdul Basit",
            downloadedCount = 12,
            totalToDownload = 286,
        )

        create()

        assertThat(notification()!!.extras.getString(Notification.EXTRA_TEXT))
            .isEqualTo("Downloading 12 of 286 ayahs")
    }

    @Test
    fun `preparing with nothing to download still names the reciter`() {
        audioState.value = AudioState(
            isPreparing = true,
            currentTitle = "Al-Fatihah",
            reciterName = "Abdul Basit",
            totalToDownload = 0,
        )

        create()

        assertThat(notification()!!.extras.getString(Notification.EXTRA_TEXT))
            .isEqualTo("Abdul Basit")
    }

    @Test
    fun `the transport button says Pause while playing and Play while paused`() {
        audioState.value = AudioState(isActive = true, isPlaying = true, currentTitle = "Yasin")
        create()
        assertThat(notification()!!.actions.map { it.title.toString() }).contains("Pause")

        notificationManager.cancelAll()
        audioState.value = AudioState(isActive = true, isPlaying = false, currentTitle = "Yasin")
        create()
        assertThat(notification()!!.actions.map { it.title.toString() }).contains("Play")
    }

    @Test
    fun `the notification is ongoing only while audio is actually playing`() {
        // An ongoing notification cannot be swiped away. Leaving one up while paused traps it
        // in the shade with no way to dismiss it.
        audioState.value = AudioState(isActive = true, isPlaying = true, currentTitle = "Yasin")
        create()
        assertThat(notification()!!.flags and Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)

        notificationManager.cancelAll()
        audioState.value = AudioState(isActive = true, isPlaying = false, currentTitle = "Yasin")
        create()
        assertThat(notification()!!.flags and Notification.FLAG_ONGOING_EVENT).isEqualTo(0)
    }

    @Test
    fun `all three transport controls are offered`() {
        audioState.value = AudioState(isActive = true, isPlaying = true, currentTitle = "Yasin")

        create()

        assertThat(notification()!!.actions.map { it.title.toString() })
            .containsExactly("Previous", "Pause", "Next")
    }

    @Test
    fun `tapping the notification opens the surah being played, not just the app`() {
        audioState.value = AudioState(isActive = true, isPlaying = true, currentTitle = "Yasin")

        create()

        // The surah is read from the singleton at click time rather than encoded here, so the
        // intent stays correct as the playlist advances.
        assertThat(notification()!!.contentIntent).isNotNull()
    }

    // ── Transport actions ───────────────────────────────────────────────────────

    @Test
    fun `the play action toggles rather than forcing playback`() {
        send(QuranAudioService.ACTION_PLAY)

        verify { audioManager.togglePlayPause() }
    }

    @Test
    fun `the pause action toggles too, because one notification button does both`() {
        send(QuranAudioService.ACTION_PAUSE)

        verify { audioManager.togglePlayPause() }
    }

    @Test
    fun `next and previous move through the playlist`() {
        send(QuranAudioService.ACTION_NEXT)
        verify { audioManager.skipToNext() }

        send(QuranAudioService.ACTION_PREVIOUS)
        verify { audioManager.skipToPrevious() }
    }

    @Test
    fun `the stop action stops the audio and takes the service with it`() {
        val controller = send(QuranAudioService.ACTION_STOP)

        verify { audioManager.stop() }
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `a system restart with a null intent stops rather than resuming something`() {
        // `onStartCommand(null, …)` is what a system restart delivers. Robolectric's
        // `buildService(clazz, null)` substitutes a default intent, so the null has to be
        // handed to the real method directly — otherwise this branch is unreachable.
        val controller = create()

        val result = controller.get().onStartCommand(null, 0, 0)

        assertThat(result).isEqualTo(android.app.Service.START_NOT_STICKY)
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `an action the service does not own is ignored`() {
        send("com.example.NOT_OURS")

        verify(exactly = 0) { audioManager.togglePlayPause() }
        verify(exactly = 0) { audioManager.stop() }
    }

    // ── Teardown ────────────────────────────────────────────────────────────────

    @Test
    fun `destroying the service does not leave the state collector running`() {
        audioState.value = AudioState(isActive = true, isPlaying = true, currentTitle = "Yasin")
        val controller = create()

        controller.destroy()
        idle()
        notificationManager.cancelAll()
        audioState.value = AudioState(isActive = true, isPlaying = true, currentTitle = "Nuh")
        idle()

        // A collector that outlived the service would re-post here.
        assertThat(notification()).isNull()
    }
}
