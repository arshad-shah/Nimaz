package com.arshadshah.nimaz.widget.work

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.arshadshah.nimaz.widget.core.WidgetWorkReceiver
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidgetReceiver
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidgetReceiver
import com.arshadshah.nimaz.widget.khatam.KhatamWidgetReceiver
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidgetReceiver
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidgetReceiver
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWidgetReceiver
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Each receiver's three abstract hooks, exercised through the real lifecycle.
 *
 * The base class decides *when* to arm and cancel; these six one-line overrides decide *what*.
 * A receiver pointed at another widget's worker would arm the wrong refresh — the widget it
 * belongs to would simply never update, with nothing failing anywhere.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetReceiverWiringTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val receivers: List<Pair<WidgetWorkReceiver, String>> = listOf(
        HijriDateWidgetReceiver() to "HijriDateWorker",
        HijriCalendarWidgetReceiver() to "HijriCalendarWorker",
        KhatamWidgetReceiver() to "KhatamWorker",
        NextPrayerWidgetReceiver() to "NextPrayerWorker",
        PrayerTimesWidgetReceiver() to "PrayerTimesWorker",
        PrayerTrackerWidgetReceiver() to "PrayerTrackerWorker",
    )

    @Before
    fun initWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    /** Work enqueued under [name], whatever state it has reached. */
    private fun enqueued(name: String) = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(name).get()

    /** Work still pending under [name] — what a cancel has to clear. */
    private fun live(name: String) = enqueued(name).filter { !it.state.isFinished }

    @Test
    fun `placing a widget arms its own worker, both periodic and one-shot`() {
        receivers.forEach { (receiver, worker) ->
            receiver.onEnabled(context)

            assertThat(live(worker)).hasSize(1)
            // The one-shot runs under the test executor as soon as it is enqueued, so its
            // presence — not its liveness — is what says the receiver asked for it.
            assertThat(enqueued("${worker}OneTime")).isNotEmpty()
        }
    }

    @Test
    fun `removing the last instance cancels that widget's work`() {
        receivers.forEach { (receiver, worker) ->
            receiver.onEnabled(context)

            receiver.onDisabled(context)

            assertThat(live(worker)).isEmpty()
            assertThat(live("${worker}OneTime")).isEmpty()
        }
    }

    /**
     * Only the two countdown widgets drive the per-minute alarm; the other four must not arm it,
     * or a home screen with just a Khatam widget ticks every minute for nothing.
     */
    @Test
    fun `only the countdown widgets arm the per-minute tick`() {
        val alarms = { shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms }

        listOf(
            HijriDateWidgetReceiver(),
            HijriCalendarWidgetReceiver(),
            KhatamWidgetReceiver(),
            PrayerTrackerWidgetReceiver(),
        ).forEach { it.onEnabled(context) }
        assertThat(alarms()).isEmpty()

        NextPrayerWidgetReceiver().onEnabled(context)

        assertThat(alarms()).isNotEmpty()
    }

    @Test
    fun `the prayer-times widget also arms the tick`() {
        PrayerTimesWidgetReceiver().onEnabled(context)

        assertThat(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms)
            .isNotEmpty()
    }

    /**
     * Removing a countdown widget while the other is still placed must leave the shared tick
     * running — they use one request code, so a plain cancel would freeze the survivor.
     */
    @Test
    fun `removing one countdown widget does not stop the shared tick`() {
        val alarms = { shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms }
        shadowOf(android.appwidget.AppWidgetManager.getInstance(context)).bindAppWidgetId(
            3,
            android.content.ComponentName(context, PrayerTimesWidgetReceiver::class.java),
        )
        NextPrayerWidgetReceiver().onEnabled(context)
        assertThat(alarms()).isNotEmpty()

        NextPrayerWidgetReceiver().onDisabled(context)

        assertThat(alarms()).isNotEmpty()
    }

    @Test
    fun `every receiver exposes the widget it draws`() {
        assertThat(receivers.map { it.first.glanceAppWidget::class.simpleName })
            .containsExactly(
                "HijriDateWidget",
                "HijriCalendarWidget",
                "KhatamWidget",
                "NextPrayerWidget",
                "PrayerTimesWidget",
                "PrayerTrackerWidget",
            )
    }
}
