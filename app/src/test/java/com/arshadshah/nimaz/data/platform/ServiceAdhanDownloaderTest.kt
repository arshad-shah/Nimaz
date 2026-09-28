package com.arshadshah.nimaz.data.platform

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanDownloadWorker
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests the real download entry point, including the service-to-worker fallback wiring. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServiceAdhanDownloaderTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private val request = slot<OneTimeWorkRequest>()
    private var importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND

    @Before
    fun setUp() {
        context = spyk(ApplicationProvider.getApplicationContext<Context>())
        workManager = mockk(relaxed = true)
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        every {
            workManager.enqueueUniqueWork(
                AdhanDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                capture(request),
            )
        } returns mockk(relaxed = true)
        mockkStatic(ActivityManager::class)
        every { ActivityManager.getMyMemoryState(any()) } answers {
            firstArg<ActivityManager.RunningAppProcessInfo>().importance = importance
        }
        every { context.startForegroundService(any()) } returns
            ComponentName(context, AdhanDownloadService::class.java)
    }

    @After
    fun tearDown() {
        unmockkStatic(ActivityManager::class)
        unmockkObject(WorkManager.Companion)
    }

    @Test
    fun `a foreground selection starts the service with the selected sound`() {
        val intent = slot<Intent>()
        every { context.startForegroundService(capture(intent)) } returns
            ComponentName(context, AdhanDownloadService::class.java)

        ServiceAdhanDownloader(context).download(AdhanSound.ABDUL_BASIT.name)

        assertThat(intent.captured.component?.className)
            .isEqualTo(AdhanDownloadService::class.java.name)
        assertThat(intent.captured.action).isEqualTo(AdhanDownloadService.ACTION_DOWNLOAD_SELECTED)
        assertThat(intent.captured.getStringExtra(AdhanDownloadService.EXTRA_ADHAN_SOUND))
            .isEqualTo(AdhanSound.ABDUL_BASIT.name)
        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `a background selection queues network work without starting a foreground service`() {
        importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND

        ServiceAdhanDownloader(context).download(AdhanSound.MAKKAH.name)

        assertQueuedSound(AdhanSound.MAKKAH.name)
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    @Test
    fun `a rejected foreground start queues the same sound for background download`() {
        every { context.startForegroundService(any()) } throws
            IllegalStateException("foreground service start rejected")

        ServiceAdhanDownloader(context).download(AdhanSound.ABDUL_BASIT.name)

        verify(exactly = 1) { context.startForegroundService(any()) }
        assertQueuedSound(AdhanSound.ABDUL_BASIT.name)
    }

    @Test
    fun `an obsolete persisted sound name falls back to Mishary`() {
        importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND

        ServiceAdhanDownloader(context).download("REMOVED_SOUND")

        assertQueuedSound(AdhanSound.MISHARY.name)
    }

    @Test
    fun `automatic default downloads always use background work even while foregrounded`() {
        AdhanDownloadService.downloadDefault(context)

        assertQueuedSound(null)
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    private fun assertQueuedSound(sound: String?) {
        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                AdhanDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
        }
        assertThat(request.captured.workSpec.workerClassName)
            .isEqualTo(AdhanDownloadWorker::class.java.name)
        assertThat(request.captured.workSpec.input.getString(AdhanDownloadWorker.KEY_ADHAN_SOUND))
            .isEqualTo(sound)
        assertThat(request.captured.workSpec.constraints.requiredNetworkType)
            .isEqualTo(NetworkType.CONNECTED)
    }
}
