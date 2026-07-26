package com.arshadshah.nimaz.behavior

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.presentation.components.organisms.HomeCountdownTestTag
import com.arshadshah.nimaz.support.BaseAppTest
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The regression test for the bug that shipped: **timers stopped updating in real time.**
 *
 * ## Why this needs to be an instrumented test
 *
 * Nothing in the JVM suite could have caught it. Every Robolectric component test pins
 * `mainClock.autoAdvance = false` and asserts the *first frame*, so a countdown that renders once
 * and then freezes passes them all. The failure only exists against a real frame clock, a real
 * lifecycle and real elapsed time — which is exactly what an instrumented test has.
 *
 * ## What it actually asserts
 *
 * Read the Home hero's next-prayer countdown, let real seconds pass, read it again, and require
 * that the text changed. No mocking, no virtual clock: if the shared ticker stops, or a countdown
 * that displays seconds is wired to a minute-resolution tick (the actual defect — the seconds digit
 * sat still for 60s and then jumped), this fails.
 *
 * The countdown shows seconds whatever the distance to the next prayer, so this holds at any time
 * of day. That property is the fix, and this is the test that pins it.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LiveCountdownTest : BaseAppTest() {

    override suspend fun seedState() {
        super.seedState()
        // A fixed mid-latitude location so prayer times always resolve. Without a location the
        // hero has no next-prayer instant and renders no countdown at all.
        settings.updateLocation(LONDON_LAT, LONDON_LON, "London")
    }

    @Test
    fun homeNextPrayerCountdownAdvancesInRealTime() {
        launchApp()
        waitForCountdown()

        val first = countdownText()
        assertTrue("Countdown rendered empty", first.isNotBlank())

        // Real elapsed time — the whole point. Two-plus seconds so a 1 Hz ticker must move even if
        // the first read landed just before a boundary.
        Thread.sleep(2_500)
        compose.waitForIdle()

        val second = countdownText()
        assertNotEquals(
            "Home countdown did not advance in 2.5s of real time (was '$first', still '$second'). " +
                "The shared ticker has stopped, or this countdown shows seconds while ticking " +
                "at minute resolution.",
            first,
            second,
        )
    }

    /**
     * And it must keep ticking after the app has been backgrounded and resumed — the ticker is
     * lifecycle-scoped (`repeatOnLifecycle(STARTED)`), so a broken restart would leave every
     * countdown in the app frozen on return, which looks identical to the original bug.
     */
    @Test
    fun countdownResumesTickingAfterBackgrounding() {
        val scenario = launchApp()
        waitForCountdown()

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        Thread.sleep(500)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.waitForIdle()
        waitForCountdown()

        val afterResume = countdownText()
        Thread.sleep(2_500)
        compose.waitForIdle()

        assertNotEquals(
            "Countdown froze after backgrounding and resuming (stuck at '$afterResume')",
            afterResume,
            countdownText(),
        )
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForCountdown() {
        compose.waitUntilAtLeastOneExists(hasTestTag(HomeCountdownTestTag), 10_000)
    }

    /** The countdown's rendered text, read straight out of the semantics tree. */
    private fun countdownText(): String =
        compose.onNodeWithTag(HomeCountdownTestTag, useUnmergedTree = true).textValue()

    private fun SemanticsNodeInteraction.textValue(): String =
        fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString("") { it.text }

    private companion object {
        const val LONDON_LAT = 51.5074
        const val LONDON_LON = -0.1278
    }
}
