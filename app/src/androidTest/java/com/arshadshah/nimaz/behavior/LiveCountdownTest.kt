package com.arshadshah.nimaz.behavior

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * The regression test for the bug that shipped: **timers stopped updating in real time.**
 *
 * ## Why this test deliberately avoids `ComposeTestRule`
 *
 * The obvious version of this test — drive the app with a Compose rule, read the countdown, sleep,
 * read again — **cannot work, and silently reports the app as broken.** A Compose test rule puts
 * the composition's frame clock under test control. The shared ticker still fires and still writes
 * new state, but no frame is ever produced, so nothing recomposes and the semantics tree the test
 * reads never changes. `Thread.sleep` advances wall time; it does not advance the harness's frame
 * clock. The first version of this test failed for exactly that reason while the app itself was
 * ticking correctly — verified by watching the same build on the same emulator by hand.
 *
 * So this drives the app through **UI Automator**, which reads the real rendered window and has no
 * control over Compose's clock whatsoever. That is the only way to assert "the UI actually changes
 * as real seconds pass", which is precisely the property that broke.
 *
 * ## Why the JVM suite can never cover this
 *
 * Every Robolectric component test pins `mainClock.autoAdvance = false` and asserts the first
 * frame. A countdown that renders once and then freezes forever passes all of them. This failure
 * class only exists against a real frame clock, a real lifecycle and real elapsed time.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LiveCountdownTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var settings: SettingsRepository

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            // Straight to Home, with a fixed mid-latitude location so prayer times always resolve.
            // Without a location the hero has no next-prayer instant and renders no countdown.
            settings.setOnboardingCompleted(true)
            settings.updateLocation(LONDON_LAT, LONDON_LON, "London")
        }
    }

    @Test
    fun homeNextPrayerCountdownAdvancesInRealTime() {
        launchApp()

        val first = awaitCountdownText()
        assertNotNull("No countdown rendered on Home", first)

        // Real elapsed time — the whole point of this test.
        Thread.sleep(3_000)

        val second = countdownText()
        assertNotEquals(
            "Home countdown did not advance in 3s of real time (was '$first', still '$second'). " +
                "Either the shared ticker has stopped, or this countdown displays seconds while " +
                "ticking at minute resolution — the defect that shipped.",
            first,
            second,
        )
    }

    /**
     * The ticker is lifecycle-scoped (`repeatOnLifecycle(STARTED)`), so it is suspended whenever the
     * app is backgrounded. If it failed to restart on resume, every countdown in the app would be
     * frozen on return — indistinguishable, to a user, from the original bug.
     */
    @Test
    fun countdownKeepsTickingAfterBackgroundingAndResuming() {
        launchApp()
        awaitCountdownText()

        device.pressHome()
        device.wait(Until.gone(By.text(COUNTDOWN_PATTERN)), 3_000)
        Thread.sleep(1_000)

        launchApp()
        val afterResume = awaitCountdownText()
        assertNotNull("Countdown missing after returning to the app", afterResume)

        Thread.sleep(3_000)

        assertNotEquals(
            "Countdown froze after backgrounding and resuming (stuck at '$afterResume'). " +
                "repeatOnLifecycle did not restart the ticker.",
            afterResume,
            countdownText(),
        )
    }

    private fun launchApp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), LAUNCH_TIMEOUT_MS)
    }

    /** Wait for the countdown to appear, then return its text. */
    private fun awaitCountdownText(): String? {
        device.wait(Until.hasObject(By.text(COUNTDOWN_PATTERN)), LAUNCH_TIMEOUT_MS)
        return countdownText()
    }

    /**
     * The hero's countdown, read straight off the rendered window.
     *
     * Matched by shape ("1 h 13 m 30 s") rather than by testTag: UI Automator cannot see Compose
     * test tags unless the app opts into `testTagsAsResourceId`, and making a production semantics
     * change purely to satisfy a test is the wrong trade. The pattern is the countdown_hms string
     * resource's shape, so a format change fails this loudly rather than silently.
     */
    private fun countdownText(): String? =
        device.findObject(By.text(COUNTDOWN_PATTERN))?.text

    private companion object {
        const val LONDON_LAT = 51.5074
        const val LONDON_LON = -0.1278
        const val LAUNCH_TIMEOUT_MS = 20_000L

        /** Matches `countdown_hms` / `countdown_ms`: "1 h 13 m 30 s" or "13 m 30 s". */
        val COUNTDOWN_PATTERN: Pattern =
            Pattern.compile("""(\d+\s*h\s*)?\d+\s*m\s*\d+\s*s""")
    }
}
