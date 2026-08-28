package com.arshadshah.nimaz.data.audio

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.NimazChannels
import com.arshadshah.nimaz.testing.TestEntryPointApplication
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

/**
 * The foreground service that fetches the adhan audio.
 *
 * What it produces is a **notification** — the progress bar, the step counter, and the three
 * different endings (both variants, one variant, neither). None of that had ever run: the
 * service is `@AndroidEntryPoint`, so `onCreate` demanded a Hilt application and the class sat
 * at 13% with only its companion's fallback logic covered.
 *
 * The endings matter because they are what a user sees when the download half-works. "Downloaded"
 * when only the regular variant arrived means silence at Fajr, and nothing else reports it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestEntryPointApplication::class, sdk = [34])
class AdhanDownloadServiceTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AdhanAudioManager

    /** Which (sound, isFajr) pairs are on disk right now. */
    private val onDisk = mutableSetOf<Pair<AdhanSound, Boolean>>()

    /** Which downloads this test lets succeed. Replaced per test. */
    private var downloadSucceeds: (Pair<AdhanSound, Boolean>) -> Boolean = { true }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        onDisk.clear()
        downloadSucceeds = { true }
        audioManager = mockk(relaxed = true)
        // Model the disk rather than stubbing the two calls independently: the service
        // deliberately re-asks `isDownloaded` after a "successful" download, so a pair of
        // stubs that disagree makes every success test read as a failure.
        every { audioManager.isDownloaded(any(), any()) } answers {
            (firstArg<AdhanSound>() to secondArg<Boolean>()) in onDisk
        }
        coEvery { audioManager.downloadAdhan(any(), any(), any(), any()) } answers {
            val key = firstArg<AdhanSound>() to secondArg<Boolean>()
            if (downloadSucceeds(key)) { onDisk += key; true } else false
        }
        TestEntryPointApplication.Injector.reset()
        TestEntryPointApplication.Injector.adhanDownload = { it.adhanAudioManager = audioManager }
    }

    private fun start(intent: Intent?): ServiceController<AdhanDownloadService> =
        Robolectric.buildService(AdhanDownloadService::class.java, intent)
            .create()
            .startCommand(0, 0)

    private fun downloadIntent(sound: AdhanSound?) =
        Intent(context, AdhanDownloadService::class.java).apply {
            action = AdhanDownloadService.ACTION_DOWNLOAD_SELECTED
            sound?.let { putExtra(AdhanDownloadService.EXTRA_ADHAN_SOUND, it.name) }
        }

    private fun latestText(): String {
        val n = shadowOf(notificationManager).getNotification(AdhanDownloadService.NOTIFICATION_ID)
            ?: throw AssertionError("the download service posted no notification")
        return (n.extras.getString(android.app.Notification.EXTRA_TITLE).orEmpty() + " " +
            n.extras.getString(android.app.Notification.EXTRA_TEXT).orEmpty())
    }

    // ── The channel and the foreground promise ──────────────────────────────────

    @Test
    fun `the channel exists before the first notification is posted`() {
        // A foreground service that posts to a channel that does not exist is killed by the
        // system for not calling startForeground in time — with no useful message anywhere.
        Robolectric.buildService(AdhanDownloadService::class.java).create()

        assertThat(notificationManager.notificationChannels.map { it.id })
            .contains(NimazChannels.ADHAN_DOWNLOAD)
    }

    @Test
    fun `it goes foreground immediately, before it knows what it was asked to download`() {
        // The deadline is on `onStartCommand`, not on the download, so the indeterminate
        // "preparing" notification has to be posted before the action is even read.
        start(downloadIntent(AdhanSound.MISHARY))

        assertThat(shadowOf(notificationManager).allNotifications).isNotEmpty()
    }

    // ── Routing ─────────────────────────────────────────────────────────────────

    @Test
    fun `an unknown action stops the service rather than downloading something`() {
        val controller = start(
            Intent(context, AdhanDownloadService::class.java).apply { action = "nonsense" }
        )

        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `a selected download with no sound named stops rather than guessing`() {
        val controller = start(downloadIntent(sound = null))

        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `the default download fetches Mishary and guarantees the beep`() {
        // The beep is the fallback the notification path relies on: without it a missing adhan
        // variant means the prayer passes in silence.
        start(
            Intent(context, AdhanDownloadService::class.java).apply {
                action = AdhanDownloadService.ACTION_DOWNLOAD_DEFAULT
            }
        )
        awaitNotificationContaining("Adhan ready")

        coVerify { audioManager.downloadAdhan(AdhanSound.MISHARY, false, any(), any()) }
        coVerify { audioManager.downloadAdhan(AdhanSound.MISHARY, true, any(), any()) }
        coVerify { audioManager.downloadAdhan(AdhanSound.SIMPLE_BEEP, false, any(), any()) }
    }

    // ── Both variants, and what the user is told about them ─────────────────────

    @Test
    fun `both variants downloading reports completion`() {
        downloadSucceeds = { true }

        start(downloadIntent(AdhanSound.MISHARY))
        awaitNotificationContaining("Adhan ready")

        assertThat(latestText()).contains("Mishary")
    }

    @Test
    fun `only one variant downloading is reported as partial, not as success`() {
        // The regression this guards is silence at Fajr with a green tick in settings.
        // The Fajr variant is the one that fails, which is the case that ends in silence at
        // the one prayer the user is least likely to be awake to notice.
        downloadSucceeds = { (_, isFajr) -> !isFajr }

        start(downloadIntent(AdhanSound.MISHARY))
        awaitNotificationContaining("partially downloaded")

        assertThat(latestText()).contains("Some")
    }

    @Test
    fun `neither variant downloading is reported as a failure`() {
        downloadSucceeds = { false }

        start(downloadIntent(AdhanSound.MISHARY))
        awaitNotificationContaining("download failed")

        assertThat(latestText()).contains("internet connection")
    }

    @Test
    fun `a download that reports success but leaves no valid file is treated as a failure`() {
        // The downloader returning true while the file is truncated is a real outcome — a
        // captive-portal HTML body saved as an mp3. Believing it means silence at prayer time.
        coEvery { audioManager.downloadAdhan(any(), any(), any(), any()) } returns true
        // …but nothing lands on disk, so `isDownloaded` keeps saying no.

        start(downloadIntent(AdhanSound.MISHARY))
        awaitNotificationContaining("download failed")

        // isDownloaded stays false after the "successful" download, so neither variant counts.
        assertThat(latestText()).contains("internet connection")
    }

    @Test
    fun `an already-downloaded sound is not fetched again`() {
        onDisk += AdhanSound.MISHARY to false
        onDisk += AdhanSound.MISHARY to true
        onDisk += AdhanSound.SIMPLE_BEEP to false

        start(downloadIntent(AdhanSound.MISHARY))
        awaitNotificationContaining("Adhan ready")

        coVerify(exactly = 0) { audioManager.downloadAdhan(any(), any(), any(), any()) }
    }

    @Test
    fun `a thrown downloader is reported as a failure rather than crashing the service`() {
        // A service that throws out of a coroutine takes the process with it — from a
        // background start the user sees the app "close by itself".
        coEvery { audioManager.cleanupTempFiles() } throws IllegalStateException("disk gone")

        val controller = start(downloadIntent(AdhanSound.MISHARY))
        awaitNotificationContaining("download failed")

        assertThat(latestText()).contains("internet connection")
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    // ── The fallback decision ───────────────────────────────────────────────────

    @Test
    fun `a background trigger never attempts a foreground start`() {
        var started = false
        var fellBack = false

        AdhanDownloadService.startServiceWithFallback(
            canStartForeground = false,
            start = { started = true },
            fallback = { fellBack = true },
        )

        assertThat(started).isFalse()
        assertThat(fellBack).isTrue()
    }

    @Test
    fun `a rejected foreground start falls back instead of propagating`() {
        var fellBack = false

        AdhanDownloadService.startServiceWithFallback(
            canStartForeground = true,
            start = { throw IllegalStateException("not allowed from background") },
            fallback = { fellBack = true },
        )

        assertThat(fellBack).isTrue()
    }

    private fun awaitNotificationContaining(fragment: String) {
        repeat(300) {
            val n = shadowOf(notificationManager)
                .getNotification(AdhanDownloadService.NOTIFICATION_ID)
            val text = (n?.extras?.getString(android.app.Notification.EXTRA_TITLE).orEmpty() + " " +
                n?.extras?.getString(android.app.Notification.EXTRA_TEXT).orEmpty())
            if (text.contains(fragment)) return
            Thread.sleep(10)
        }
        throw AssertionError(
            "no notification containing \"$fragment\" within 3s; last was \"${
                runCatching { latestText() }.getOrDefault("<none>")
            }\""
        )
    }
}
