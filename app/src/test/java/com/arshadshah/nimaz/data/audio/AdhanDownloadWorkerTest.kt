package com.arshadshah.nimaz.data.audio

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The background half of the adhan download.
 *
 * `AdhanDownloadService` cannot be started from the background on Android 12+, so this worker is
 * what actually runs on the paths that matter most — app initialization and a prayer-notification
 * broadcast finding a missing file. It was at 31%: only its `enqueue` helper had ever run.
 *
 * Its retry rule is the interesting part. A transient failure has to come back; a permanent one
 * must not retry forever, because WorkManager's backoff would keep waking the device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AdhanDownloadWorkerTest {

    private lateinit var context: Context
    private lateinit var audioManager: AdhanAudioManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        audioManager = mockk(relaxed = true)
        every { audioManager.isDownloaded(any(), any()) } returns false
        coEvery { audioManager.downloadAdhan(any(), any(), any(), any()) } returns true
    }

    private fun worker(
        sound: AdhanSound? = null,
        runAttemptCount: Int = 0,
    ): AdhanDownloadWorker {
        val data = Data.Builder().apply {
            if (sound != null) putString(AdhanDownloadWorker.KEY_ADHAN_SOUND, sound.name)
        }.build()
        return TestListenableWorkerBuilder<AdhanDownloadWorker>(context)
            .setInputData(data)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters,
                ): ListenableWorker = AdhanDownloadWorker(appContext, workerParameters, audioManager)
            })
            .build()
    }

    @Test
    fun `with no sound named it fetches the default`() {
        // The app-init path enqueues with no sound at all; guessing wrong there means the
        // reader's chosen adhan is never fetched and every prayer beeps.
        val result = runBlocking { worker().doWork() }

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify { audioManager.downloadAdhan(AdhanSound.MISHARY, false, any(), any()) }
        coVerify { audioManager.downloadAdhan(AdhanSound.MISHARY, true, any(), any()) }
    }

    @Test
    fun `a named sound is the one fetched`() {
        val sound = AdhanSound.entries.first { it != AdhanSound.MISHARY }

        runBlocking { worker(sound = sound).doWork() }

        coVerify { audioManager.downloadAdhan(sound, false, any(), any()) }
        coVerify { audioManager.downloadAdhan(sound, true, any(), any()) }
    }

    @Test
    fun `an unrecognised sound name falls back rather than failing the job`() {
        // Names are persisted. A build that drops a muezzin must not leave the reader with no
        // audio at all.
        val data = Data.Builder()
            .putString(AdhanDownloadWorker.KEY_ADHAN_SOUND, "A_MUEZZIN_WE_REMOVED")
            .build()
        val worker = TestListenableWorkerBuilder<AdhanDownloadWorker>(context)
            .setInputData(data)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters,
                ): ListenableWorker = AdhanDownloadWorker(appContext, workerParameters, audioManager)
            })
            .build()

        val result = runBlocking { worker.doWork() }

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `the beep is generated when it is not already there`() {
        // It is the fallback the whole notification path relies on: without it a missing adhan
        // variant means the prayer passes in silence.
        runBlocking { worker().doWork() }

        coVerify { audioManager.downloadAdhan(AdhanSound.SIMPLE_BEEP, false, any(), any()) }
    }

    @Test
    fun `an existing beep is not regenerated`() {
        every { audioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false) } returns true

        runBlocking { worker().doWork() }

        coVerify(exactly = 0) {
            audioManager.downloadAdhan(AdhanSound.SIMPLE_BEEP, false, any(), any())
        }
    }

    @Test
    fun `a failure on an early attempt asks to be retried`() {
        coEvery { audioManager.cleanupTempFiles() } throws java.io.IOException("no network")

        val result = runBlocking { worker(runAttemptCount = 0).doWork() }

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `a failure after the retry budget gives up rather than waking the device forever`() {
        // WorkManager backs off exponentially, so an unbounded retry on a permanently broken
        // URL is a battery drain the reader cannot see or stop.
        coEvery { audioManager.cleanupTempFiles() } throws java.io.IOException("no network")

        val result = runBlocking { worker(runAttemptCount = 3).doWork() }

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `one variant failing does not abort the other`() {
        // Downloads are tolerated per variant: a bad Fajr URL must not cost the regular adhan.
        coEvery { audioManager.downloadAdhan(any(), eq(false), any(), any()) } returns true
        coEvery { audioManager.downloadAdhan(any(), eq(true), any(), any()) } returns false

        val result = runBlocking { worker().doWork() }

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify { audioManager.downloadAdhan(AdhanSound.MISHARY, false, any(), any()) }
    }
}
