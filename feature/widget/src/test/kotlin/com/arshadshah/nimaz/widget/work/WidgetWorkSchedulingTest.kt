package com.arshadshah.nimaz.widget.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWorker
import com.arshadshah.nimaz.widget.hijridate.HijriDateWorker
import com.arshadshah.nimaz.widget.khatam.KhatamWorker
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWorker
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWorker
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWorker
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Every widget's refresh schedule, driven through the real `WidgetWork` helpers.
 *
 * A widget whose periodic work is never enqueued does not fail anywhere visible — it just stops
 * updating, and the reader sees stale prayer times. These assert the enqueue actually happened,
 * under the unique name the cancel path later looks for.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetWorkSchedulingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var workManager: WorkManager

    /** The six widgets, each as (unique periodic name, enqueue, refresh now, cancel). */
    private val widgets = listOf(
        Triple("HijriDateWorker", HijriDateWorker::enqueuePeriodicWork, HijriDateWorker::cancel)
            to HijriDateWorker::enqueueImmediateWork,
        Triple(
            "HijriCalendarWorker",
            HijriCalendarWorker::enqueuePeriodicWork,
            HijriCalendarWorker::cancel,
        ) to HijriCalendarWorker::enqueueImmediateWork,
        Triple("KhatamWorker", KhatamWorker::enqueuePeriodicWork, KhatamWorker::cancel)
            to KhatamWorker::enqueueImmediateWork,
        Triple("NextPrayerWorker", NextPrayerWorker::enqueuePeriodicWork, NextPrayerWorker::cancel)
            to NextPrayerWorker::enqueueImmediateWork,
        Triple(
            "PrayerTimesWorker",
            PrayerTimesWorker::enqueuePeriodicWork,
            PrayerTimesWorker::cancel,
        ) to PrayerTimesWorker::enqueueImmediateWork,
        Triple(
            "PrayerTrackerWorker",
            PrayerTrackerWorker::enqueuePeriodicWork,
            PrayerTrackerWorker::cancel,
        ) to PrayerTrackerWorker::enqueueImmediateWork,
    )

    @Before
    fun initWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    private fun infos(name: String): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(name).get()

    @Test
    fun `every widget enqueues periodic refresh under its own unique name`() {
        widgets.forEach { (spec, _) ->
            val (name, enqueue, _) = spec
            enqueue(context, false)

            assertThat(infos(name)).hasSize(1)
        }
    }

    @Test
    fun `every widget enqueues a one-shot refresh under its own one-time name`() {
        widgets.forEach { (spec, refreshNow) ->
            val (name, _, _) = spec
            refreshNow(context)

            assertThat(infos("${name}OneTime")).hasSize(1)
        }
    }

    /**
     * `onUpdate` re-arms on every system broadcast, so the un-forced path has to be idempotent —
     * `KEEP` must not stack a second schedule or reset the one already running.
     */
    @Test
    fun `re-arming without force keeps the schedule already running`() {
        HijriDateWorker.enqueuePeriodicWork(context, force = false)
        val original = infos("HijriDateWorker").single().id

        HijriDateWorker.enqueuePeriodicWork(context, force = false)

        assertThat(infos("HijriDateWorker").map { it.id }).containsExactly(original)
    }

    /**
     * Placing a widget forces a restart, so its first refresh is prompt rather than up to a
     * period away.
     */
    @Test
    fun `forcing replaces the schedule rather than keeping it`() {
        HijriDateWorker.enqueuePeriodicWork(context, force = false)
        val original = infos("HijriDateWorker").single().id

        HijriDateWorker.enqueuePeriodicWork(context, force = true)

        val after = infos("HijriDateWorker").filter { !it.state.isFinished }
        assertThat(after).hasSize(1)
        assertThat(after.single().id).isNotEqualTo(original)
    }

    /** Removing the last instance must cancel both the periodic job and any pending one-shot. */
    @Test
    fun `cancelling stops both the periodic and the one-shot work`() {
        widgets.forEach { (spec, refreshNow) ->
            val (name, enqueue, cancel) = spec
            enqueue(context, false)
            refreshNow(context)

            cancel(context)

            assertThat(infos(name).none { !it.state.isFinished }).isTrue()
            assertThat(infos("${name}OneTime").none { !it.state.isFinished }).isTrue()
        }
    }
}
