package com.arshadshah.nimaz.data.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * How the app tells the home-screen widgets its data changed.
 *
 * A prayer marked in the app has to reach the tracker widget, and a settings change has to
 * reach all six — each as the widget's own one-time work, so a burst of changes collapses into
 * one refresh per widget rather than queueing a refresh per change.
 */
@RunWith(RobolectricTestRunner::class)
class WorkManagerWidgetRefresherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    private fun enqueued(name: String) = workManager.getWorkInfosForUniqueWork(name).get().size

    @Test
    fun `a tracker change refreshes the tracker widget alone`() {
        WorkManagerWidgetRefresher(context).refreshPrayerTracker()

        assertThat(enqueued("PrayerTrackerWorkerOneTime")).isEqualTo(1)
        assertThat(enqueued("NextPrayerWorkerOneTime")).isEqualTo(0)
    }

    @Test
    fun `refreshing all reaches every widget once`() {
        WorkManagerWidgetRefresher(context).refreshAll()

        listOf(
            "NextPrayerWorkerOneTime",
            "PrayerTimesWorkerOneTime",
            "PrayerTrackerWorkerOneTime",
            "HijriDateWorkerOneTime",
            "HijriCalendarWorkerOneTime",
            "KhatamWorkerOneTime",
        ).forEach { assertThat(enqueued(it)).isEqualTo(1) }
    }
}
