package com.arshadshah.nimaz.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidget
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidget
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import android.os.Looper

/**
 * The per-minute tick that redraws the two countdown widgets.
 *
 * Both redraws are wrapped separately on purpose: one widget throwing must not stop the other
 * from updating, and neither may take the receiver down — a `BroadcastReceiver` that throws is an
 * ANR-adjacent crash on a path the user never triggered.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetTickReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val receiver = WidgetTickReceiver()
    private val action = "com.arshadshah.nimaz.ACTION_WIDGET_TICK"

    @Before
    fun stubGlance() {
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
    }

    @After
    fun tearDown() {
        context.unregisterReceiver(receiver)
        unmockkAll()
    }

    private fun tick() {
        context.sendBroadcast(Intent(action))
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `a tick redraws both countdown widgets`() {
        coEvery { any<GlanceAppWidget>().updateAll(any()) } just Runs

        tick()

        coVerify(timeout = 5_000) { any<NextPrayerWidget>().updateAll(any()) }
        coVerify(timeout = 5_000) { any<PrayerTimesWidget>().updateAll(any()) }
    }

    /**
     * The first widget failing must not cost the second its redraw — that is the whole reason
     * each `updateAll` sits in its own try.
     */
    @Test
    fun `one widget failing still lets the other redraw`() {
        coEvery { any<NextPrayerWidget>().updateAll(any()) } throws
            IllegalStateException("host busy")
        coEvery { any<PrayerTimesWidget>().updateAll(any()) } just Runs

        tick()

        coVerify(timeout = 5_000) { any<PrayerTimesWidget>().updateAll(any()) }
    }

    @Test
    fun `both widgets failing does not take the receiver down`() {
        coEvery { any<GlanceAppWidget>().updateAll(any()) } throws IllegalStateException("gone")

        tick()

        coVerify(timeout = 5_000) { any<PrayerTimesWidget>().updateAll(any()) }
    }
}
