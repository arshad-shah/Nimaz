package com.arshadshah.nimaz.work

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.arshadshah.nimaz.data.audio.AdhanDownloadWorker
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWorker
import com.arshadshah.nimaz.widget.hijridate.HijriDateWorker
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWorker
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWorker
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWorker
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Drives every background [androidx.work.CoroutineWorker] the app ships through its
 * `doWork()` once, built by the real [HiltWorkerFactory].
 *
 * What this proves:
 *  - Each `@HiltWorker` can actually be instantiated by the factory — i.e. its
 *    `@AssistedInject` dependencies (PrayerTimeCalculator, PreferencesDataStore,
 *    PrayerDao, AdhanAudioManager) resolve from the graph. A broken binding surfaces
 *    here rather than silently on a user's device when a widget refresh fires.
 *  - Running with a seeded location, no worker fails. We assert the result is not a
 *    [ListenableWorker.Result.Failure] (Success or a transient Retry are both fine).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WidgetWorkersTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settings: SettingsRepository

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        hiltRule.inject()
        // Initialize WorkManager for tests with the Hilt factory so any internal
        // WorkManager.getInstance(...) calls inside a worker resolve correctly.
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        // Give the prayer-time workers real coordinates to compute from.
        runBlocking { settings.updateLocation(21.4225, 39.8262, "Makkah") }
    }

    private inline fun <reified T : ListenableWorker> runWorker(): ListenableWorker.Result {
        val worker = TestListenableWorkerBuilder<T>(context)
            .setWorkerFactory(workerFactory)
            .build()
        return runBlocking { worker.doWork() }
    }

    private fun assertNotFailure(result: ListenableWorker.Result) {
        assertThat(result).isNotInstanceOf(ListenableWorker.Result.Failure::class.java)
    }

    @Test
    fun nextPrayerWorker_runs() = assertNotFailure(runWorker<NextPrayerWorker>())

    @Test
    fun prayerTimesWorker_runs() = assertNotFailure(runWorker<PrayerTimesWorker>())

    @Test
    fun prayerTrackerWorker_runs() = assertNotFailure(runWorker<PrayerTrackerWorker>())

    @Test
    fun hijriDateWorker_runs() = assertNotFailure(runWorker<HijriDateWorker>())

    @Test
    fun hijriCalendarWorker_runs() = assertNotFailure(runWorker<HijriCalendarWorker>())

    @Test
    fun adhanDownloadWorker_isConstructableByFactory() {
        // The download worker reaches out to the network, which may be unavailable on
        // a CI emulator — a real Failure/Retry is legitimate there. So we only assert
        // the factory can build it and `doWork()` completes (returns a result rather
        // than throwing), which is what exercises the Hilt @AssistedInject wiring.
        val result = runWorker<AdhanDownloadWorker>()
        assertThat(result).isNotNull()
    }
}
