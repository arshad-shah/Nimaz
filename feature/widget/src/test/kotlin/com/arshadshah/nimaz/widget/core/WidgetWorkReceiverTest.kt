package com.arshadshah.nimaz.widget.core

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidget
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The shared receiver lifecycle. `onUpdate` re-arming everything is the recovery channel for a
 * schedule the app lost — a force-stop drops WorkManager jobs, and AlarmManager alarms do not
 * survive a reboot at all. A receiver that only armed in `onEnabled` left those widgets dead
 * until they were removed and placed again.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetWorkReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class RecordingReceiver : WidgetWorkReceiver() {
        val calls = mutableListOf<String>()
        override val glanceAppWidget: GlanceAppWidget = HijriDateWidget()
        override fun enqueueWork(context: Context, force: Boolean) {
            calls += "enqueue(force=$force)"
        }

        override fun refreshNow(context: Context) {
            calls += "refreshNow"
        }

        override fun cancelWork(context: Context) {
            calls += "cancel"
        }

        override fun onWidgetPresent(context: Context) {
            calls += "present"
        }

        override fun onWidgetAbsent(context: Context) {
            calls += "absent"
        }
    }

    /** Placing the first instance forces a restart so the widget is not blank for a period. */
    @Test
    fun `onEnabled forces the schedule and refreshes straight away`() {
        val receiver = RecordingReceiver()

        receiver.onEnabled(context)

        assertThat(receiver.calls)
            .containsExactly("enqueue(force=true)", "refreshNow", "present")
            .inOrder()
    }

    /**
     * `onUpdate` arrives on boot, after a package update and every `updatePeriodMillis`, whatever
     * state WorkManager is in. It re-arms without forcing, so it is idempotent.
     */
    @Test
    fun `onUpdate re-arms without forcing and still refreshes`() {
        val receiver = RecordingReceiver()

        receiver.onUpdate(context, mockk<AppWidgetManager>(relaxed = true), intArrayOf(1, 2))

        assertThat(receiver.calls)
            .containsExactly("enqueue(force=false)", "refreshNow", "present")
            .inOrder()
    }

    @Test
    fun `onDisabled cancels the work and reports the widget gone`() {
        val receiver = RecordingReceiver()

        receiver.onDisabled(context)

        assertThat(receiver.calls).containsExactly("cancel", "absent").inOrder()
    }

    /** The two hooks are optional — a receiver with nothing extra to do must still work. */
    @Test
    fun `a receiver that overrides neither hook runs the lifecycle unchanged`() {
        val calls = mutableListOf<String>()
        val receiver = object : WidgetWorkReceiver() {
            override val glanceAppWidget: GlanceAppWidget = HijriDateWidget()
            override fun enqueueWork(context: Context, force: Boolean) {
                calls += "enqueue"
            }

            override fun refreshNow(context: Context) {
                calls += "refreshNow"
            }

            override fun cancelWork(context: Context) {
                calls += "cancel"
            }
        }

        receiver.onEnabled(context)
        receiver.onDisabled(context)

        assertThat(calls).containsExactly("enqueue", "refreshNow", "cancel").inOrder()
    }
}
