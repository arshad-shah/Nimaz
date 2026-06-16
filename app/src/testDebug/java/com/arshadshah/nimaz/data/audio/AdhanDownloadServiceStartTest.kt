package com.arshadshah.nimaz.data.audio

import android.app.ForegroundServiceStartNotAllowedException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Reproduces and guards against the production crash:
 *
 *   android.app.ForegroundServiceStartNotAllowedException: startForegroundService()
 *   not allowed due to mAllowStartForeground false
 *
 * The app initializer (and the boot receiver) trigger adhan downloads from a
 * background context. On Android 12+ starting a foreground service from the
 * background is forbidden and throws, which previously aborted the download and
 * was reported as a non-fatal crash. The start helpers must instead fall back to
 * a WorkManager job that can run in the background.
 */
@RunWith(RobolectricTestRunner::class)
class AdhanDownloadServiceStartTest {

    /**
     * A context that always refuses to start a (foreground) service, exactly as
     * the framework does when the process is in the background on Android 12+.
     */
    private class BackgroundRestrictedContext(base: Context) : ContextWrapper(base) {
        override fun startForegroundService(service: Intent): ComponentName? {
            throw ForegroundServiceStartNotAllowedException(
                "startForegroundService() not allowed due to mAllowStartForeground false"
            )
        }

        override fun startService(service: Intent): ComponentName? {
            throw ForegroundServiceStartNotAllowedException(
                "startService() not allowed due to mAllowStartForeground false"
            )
        }
    }

    @After
    fun tearDown() {
        unmockkObject(AdhanDownloadWorker.Companion)
    }

    // ── Core fallback logic ─────────────────────────────────────────────

    @Test
    fun `falls back when foreground start throws and does not propagate`() {
        var fallbackInvoked = false

        // Must not propagate ForegroundServiceStartNotAllowedException.
        AdhanDownloadService.startServiceWithFallback(
            start = {
                throw ForegroundServiceStartNotAllowedException(
                    "not allowed due to mAllowStartForeground false"
                )
            },
            fallback = { fallbackInvoked = true }
        )

        assertThat(fallbackInvoked).isTrue()
    }

    @Test
    fun `does not invoke fallback when foreground start succeeds`() {
        var started = false
        var fallbackInvoked = false

        AdhanDownloadService.startServiceWithFallback(
            start = { started = true },
            fallback = { fallbackInvoked = true }
        )

        assertThat(started).isTrue()
        assertThat(fallbackInvoked).isFalse()
    }

    // ── End-to-end through the public entry points ──────────────────────

    @Test
    fun `downloadDefault enqueues background work when foreground start is disallowed`() {
        mockkObject(AdhanDownloadWorker.Companion)
        every { AdhanDownloadWorker.enqueue(any(), any()) } returns Unit

        val context = BackgroundRestrictedContext(RuntimeEnvironment.getApplication())

        // Must not crash.
        AdhanDownloadService.downloadDefault(context)

        // Default download => null sound.
        verify { AdhanDownloadWorker.enqueue(context, null) }
    }

    @Test
    fun `downloadSelected enqueues background work for the chosen sound when foreground start is disallowed`() {
        mockkObject(AdhanDownloadWorker.Companion)
        every { AdhanDownloadWorker.enqueue(any(), any()) } returns Unit

        val context = BackgroundRestrictedContext(RuntimeEnvironment.getApplication())

        AdhanDownloadService.downloadSelected(context, AdhanSound.MAKKAH)

        verify { AdhanDownloadWorker.enqueue(context, AdhanSound.MAKKAH) }
    }
}
