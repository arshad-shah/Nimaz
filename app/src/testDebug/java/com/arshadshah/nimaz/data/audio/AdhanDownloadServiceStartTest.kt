package com.arshadshah.nimaz.data.audio

import android.app.ForegroundServiceStartNotAllowedException
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
 * Reproduces and guards against two distinct Android 12+ production crashes from
 * starting the adhan download foreground service from the background:
 *
 *   android.app.ForegroundServiceStartNotAllowedException
 *   android.app.ForegroundServiceDidNotStartInTimeException
 *
 * The first is thrown synchronously at the call site; the second is delivered
 * asynchronously on the main thread (when startForeground() is not reached
 * within the system deadline during a congested cold start) and cannot be
 * caught. The start helpers therefore (a) only start the foreground service when
 * the app is in the foreground, and (b) route automatic/background downloads to
 * a WorkManager job that can run in the background.
 */
@RunWith(RobolectricTestRunner::class)
class AdhanDownloadServiceStartTest {

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
            canStartForeground = true,
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
            canStartForeground = true,
            start = { started = true },
            fallback = { fallbackInvoked = true }
        )

        assertThat(started).isTrue()
        assertThat(fallbackInvoked).isFalse()
    }

    @Test
    fun `falls back without starting foreground service when app is not in foreground`() {
        var started = false
        var fallbackInvoked = false

        // Guards against ForegroundServiceDidNotStartInTimeException: when the
        // app is not in the foreground we must not even attempt the foreground
        // start, since that exception is async and cannot be caught.
        AdhanDownloadService.startServiceWithFallback(
            canStartForeground = false,
            start = { started = true },
            fallback = { fallbackInvoked = true }
        )

        assertThat(started).isFalse()
        assertThat(fallbackInvoked).isTrue()
    }

    // ── End-to-end through the public entry points ──────────────────────

    @Test
    fun `downloadDefault always enqueues background work`() {
        mockkObject(AdhanDownloadWorker.Companion)
        every { AdhanDownloadWorker.enqueue(any(), any()) } returns Unit

        val context = RuntimeEnvironment.getApplication()

        // The automatic default download never uses a foreground service.
        AdhanDownloadService.downloadDefault(context)

        // Default download => null sound.
        verify { AdhanDownloadWorker.enqueue(context, null) }
    }
}
