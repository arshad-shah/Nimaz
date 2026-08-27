package com.arshadshah.nimaz.data.device

import android.content.Context
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The tasbih counter's tick.
 *
 * Two independent settings, and the failure mode of getting them crossed is a phone that
 * buzzes in a mosque when the user turned buzzing off. The rest is resource handling: a
 * `ToneGenerator` is one audio resource per process — the class is a `@Singleton` for that
 * reason, since every Tasbih screen gets its own ViewModel — and both the constructor and every
 * use are wrapped, because a device with the audio resource already claimed throws rather than
 * returning null.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidCounterFeedbackTest {

    private lateinit var context: Context
    private lateinit var feedback: AndroidCounterFeedback

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        feedback = AndroidCounterFeedback(context)
    }

    @Test
    fun `a tick with vibration on buzzes once`() {
        feedback.tick(vibrate = true, sound = false)

        assertThat(shadowOf(vibrator()).milliseconds).isEqualTo(30L)
    }

    @Test
    fun `a tick with vibration off does not buzz`() {
        feedback.tick(vibrate = false, sound = true)

        assertThat(shadowOf(vibrator()).milliseconds).isEqualTo(0L)
    }

    @Test
    fun `a tick with neither does nothing and does not throw`() {
        feedback.tick(vibrate = false, sound = false)

        assertThat(shadowOf(vibrator()).milliseconds).isEqualTo(0L)
    }

    @Test
    fun `a tick with both is safe`() {
        feedback.tick(vibrate = true, sound = true)

        assertThat(shadowOf(vibrator()).milliseconds).isEqualTo(30L)
    }

    @Config(sdk = [30])
    @Test
    fun `a phone older than Android 12 reaches the vibrator the old way`() {
        // `VIBRATOR_MANAGER_SERVICE` does not exist before S, so the cast would throw and the
        // counter would crash on the first tap.
        AndroidCounterFeedback(context).tick(vibrate = true, sound = false)

        assertThat(shadowOf(vibrator()).milliseconds).isEqualTo(30L)
    }

    @Test
    fun `releasing twice is safe, so a second screen closing cannot crash`() {
        feedback.tick(vibrate = false, sound = true)

        feedback.release()
        feedback.release()

        // And a tick after release is a no-op rather than a use-after-free.
        feedback.tick(vibrate = false, sound = true)
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator = context.getSystemService(Vibrator::class.java)
}
