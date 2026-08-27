package com.arshadshah.nimaz.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidgetReceiver
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidgetReceiver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The one-minute tick that keeps the two countdown widgets moving.
 *
 * Both countdown receivers share a request code, so the alarm they arm is the *same* alarm. A
 * plain `cancel` from one receiver's `onDisabled` therefore used to stop the tick belonging to
 * the widget that stayed — its countdown visibly froze, which reads as "the widget sometimes
 * doesn't work". [WidgetUpdateScheduler.cancelIfUnused] is the fix, and these pin it.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetUpdateSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun scheduledAlarms() = shadowOf(alarmManager).scheduledAlarms

    /** Put a widget on the home screen, the way the launcher does. */
    private fun place(receiver: Class<*>, id: Int) {
        shadowOf(AppWidgetManager.getInstance(context))
            .bindAppWidgetId(id, ComponentName(context, receiver))
    }

    @Suppress("DEPRECATION") // ShadowAlarmManager exposes the interval only through this field.
    @Test
    fun `scheduling arms a repeating tick`() {
        WidgetUpdateScheduler.schedule(context)

        assertThat(scheduledAlarms()).isNotEmpty()
        assertThat(scheduledAlarms().first().interval).isEqualTo(60_000L)
    }

    /**
     * `onUpdate` re-arms on every system broadcast. `FLAG_UPDATE_CURRENT` reuses the one
     * PendingIntent, so re-arming must not stack a second alarm.
     */
    @Test
    fun `re-arming repeatedly never stacks a second alarm`() {
        WidgetUpdateScheduler.schedule(context)
        WidgetUpdateScheduler.schedule(context)
        WidgetUpdateScheduler.schedule(context)

        assertThat(scheduledAlarms()).hasSize(1)
    }

    @Test
    fun `cancelling removes the tick`() {
        WidgetUpdateScheduler.schedule(context)

        WidgetUpdateScheduler.cancel(context)

        assertThat(scheduledAlarms()).isEmpty()
    }

    /**
     * The regression: with no countdown widget placed, `cancelIfUnused` stops the tick. With one
     * still placed it must not — that widget's countdown depends on it.
     */
    @Test
    fun `cancelIfUnused stops the tick only when no countdown widget is left`() {
        WidgetUpdateScheduler.schedule(context)

        WidgetUpdateScheduler.cancelIfUnused(context)
        assertThat(scheduledAlarms()).isEmpty()

        place(PrayerTimesWidgetReceiver::class.java, id = 11)
        WidgetUpdateScheduler.schedule(context)

        WidgetUpdateScheduler.cancelIfUnused(context)

        assertThat(scheduledAlarms()).isNotEmpty()
    }

    /** Boot and package updates go through `ensureScheduled`, which arms only if it needs to. */
    @Test
    fun `ensureScheduled arms nothing when no countdown widget is placed`() {
        WidgetUpdateScheduler.ensureScheduled(context)

        assertThat(scheduledAlarms()).isEmpty()
    }

    @Test
    fun `ensureScheduled arms the tick once a countdown widget is placed`() {
        place(NextPrayerWidgetReceiver::class.java, id = 7)

        WidgetUpdateScheduler.ensureScheduled(context)

        assertThat(scheduledAlarms()).isNotEmpty()
    }

    /**
     * The countdown a widget renders. A target already behind the clock, or one that was never
     * set, must produce the em dash rather than a negative duration.
     */
    @Test
    fun `computeCountdown returns an em dash for an unset target`() {
        assertThat(WidgetUpdateScheduler.computeCountdown(0L)).isEqualTo("—")
        assertThat(WidgetUpdateScheduler.computeCountdown(-1L)).isEqualTo("—")
    }

    @Test
    fun `computeCountdown formats the gap to a future instant`() {
        val inTwoHours = System.currentTimeMillis() + 2 * 3_600_000 + 30 * 60_000

        val countdown = WidgetUpdateScheduler.computeCountdown(inTwoHours)

        assertThat(countdown).contains("2h")
        assertThat(countdown).doesNotContain("-")
    }
}
