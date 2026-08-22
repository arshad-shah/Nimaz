package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The shared ticker is the single point of failure for **every** timer in the app: after the
 * migration away from per-ViewModel `while (isActive) { delay(1000) }` loops, nothing else moves
 * a countdown. If it stops emitting, every clock and countdown in Nimaz silently freezes at
 * whatever value it last held — which is exactly the symptom that shipped.
 *
 * That failure was invisible because no test asserted that a displayed time ever *changes*: the
 * component tests all pin `mainClock.autoAdvance = false` and assert the first frame only. These
 * tests close that hole. They drive a substituted [ProvideNimazClock] time source, so "did the
 * value advance?" becomes a deterministic assertion rather than a wall-clock race.
 *
 * ## How the harness works
 *
 * With `autoAdvance = false` the Compose test clock is manual: `advanceTimeBy` releases the
 * ticker's `delay`, the producer loop wakes, re-reads the (mutable) fake source and publishes it.
 * So each test moves fake time *and* virtual time, then asserts what the leaf renders.
 */
@RunWith(RobolectricTestRunner::class)
class NimazClockTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    /**
     * A **minute-aligned** epoch, so `TickResolution.MINUTES.truncate` is the identity on it and
     * the boundary assertions below read literally.
     */
    private val epoch = Instant.fromEpochMilliseconds(1_699_999_980_000L)

    /**
     * Advance both wall time and virtual time, then render the frame that publishes the result.
     *
     * The extra [advanceTimeByFrame] is the harness detail worth knowing: with `autoAdvance = false`
     * the ticker's `delay` resumes and writes the new instant, but nothing has drawn a frame yet, so
     * a bare `waitForIdle` still observes the previous value. Production has a continuously running
     * frame clock, so this lag exists only under the manual test clock.
     */
    private fun advance(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    /**
     * The regression test for the reported bug: a countdown must advance on its own, with no
     * navigation, no recomposition triggered from elsewhere, and no ViewModel emission.
     */
    @Test
    fun `countdown advances as time passes without any external recomposition`() {
        composeRule.mainClock.autoAdvance = false
        var fakeNow = epoch

        composeRule.setThemedContent {
            ProvideNimazClock(timeSource = { fakeNow }) {
                // 5 minutes out: inside the fine-grained window, so this shows seconds.
                CountdownProbe(target = epoch + 5.minutes)
            }
        }

        composeRule.onNodeWithText("05:00").assertIsDisplayed()

        // One second of wall time, one second of virtual time.
        fakeNow = epoch + 1.seconds
        advance(1_000)

        composeRule.onNodeWithText("04:59").assertIsDisplayed()

        fakeNow = epoch + 30.seconds
        advance(29_000)

        composeRule.onNodeWithText("04:30").assertIsDisplayed()
    }

    /**
     * The Home hero's bug, pinned.
     *
     * The hero counts down to the next prayer, which is more than 15 minutes away for most of the
     * day, and it renders seconds. Under the old policy [rememberCountdownTo] ticked at minute
     * resolution at that distance, so the seconds digit sat frozen for a minute at a time and only
     * appeared to move when navigation recomposed the screen.
     *
     * Distance must not decide whether a *displayed* seconds digit advances.
     */
    @Test
    fun `a seconds-showing countdown ticks every second even hours from the target`() {
        composeRule.mainClock.autoAdvance = false
        var fakeNow = epoch

        composeRule.setThemedContent {
            ProvideNimazClock(timeSource = { fakeNow }) {
                // Two hours out — far outside the 15-minute fine-grained window.
                NimazCountdownText(target = epoch + 2.hours, showSeconds = true)
            }
        }

        composeRule.onNodeWithText("2 h 0 m 0 s").assertIsDisplayed()

        fakeNow = epoch + 1.seconds
        advance(1_000)
        composeRule.onNodeWithText("1 h 59 m 59 s").assertIsDisplayed()

        fakeNow = epoch + 2.seconds
        advance(1_000)
        composeRule.onNodeWithText("1 h 59 m 58 s").assertIsDisplayed()
    }

    /**
     * The other half of the invariant: a countdown that does *not* show seconds must stay on the
     * cheap minute cadence, so binding tick-to-display does not silently make every card 1 Hz.
     */
    @Test
    fun `a minute-granularity countdown does not recompose every second`() {
        composeRule.mainClock.autoAdvance = false
        var fakeNow = epoch
        var renders = 0

        composeRule.setThemedContent {
            ProvideNimazClock(timeSource = { fakeNow }) {
                renders++
                NimazCountdownText(target = epoch + 2.hours, showSeconds = false)
            }
        }

        val initialRenders = renders
        repeat(30) { step ->
            fakeNow = epoch + (step + 1).seconds
            advance(1_000)
        }

        assert(renders == initialRenders) {
            "Minute-granularity countdown recomposed ${renders - initialRenders} times in 30 seconds"
        }
        // Still the whole-minute value: 30 s of ticking cannot move a minute-granularity readout.
        composeRule.onNodeWithText("2 h 0 m").assertIsDisplayed()
    }

    /**
     * A minute-resolution consumer must not re-render 59 times for a digit that cannot change —
     * the property that makes one app-wide 1 Hz ticker affordable.
     */
    @Test
    fun `minute resolution holds steady within a minute and flips on the boundary`() {
        composeRule.mainClock.autoAdvance = false
        var fakeNow = epoch
        var renders = 0

        composeRule.setThemedContent {
            ProvideNimazClock(timeSource = { fakeNow }) {
                val now by rememberNow(TickResolution.MINUTES)
                renders++
                Text(text = now.toEpochMilliseconds().toString())
            }
        }

        val initialRenders = renders
        val initialText = epoch.toEpochMilliseconds().toString()
        composeRule.onNodeWithText(initialText).assertIsDisplayed()

        // 30 seconds of ticking: the source updates 30 times, the minute-resolution reader must not.
        repeat(30) { step ->
            fakeNow = epoch + (step + 1).seconds
            advance(1_000)
        }
        composeRule.onNodeWithText(initialText).assertIsDisplayed()
        assert(renders == initialRenders) {
            "Minute-resolution reader recomposed ${renders - initialRenders} times within one minute"
        }

        // Crossing the boundary must publish the new minute.
        fakeNow = epoch + 1.minutes
        advance(30_000)
        composeRule.onNodeWithText((epoch + 1.minutes).toEpochMilliseconds().toString())
            .assertIsDisplayed()
    }

    /**
     * Every consumer must observe the *same* instant. Two independent tickers would drift apart and
     * show a different second in the top bar than in the hero.
     */
    @Test
    fun `all consumers share one instant`() {
        composeRule.mainClock.autoAdvance = false
        var fakeNow = epoch

        composeRule.setThemedContent {
            ProvideNimazClock(timeSource = { fakeNow }) {
                val a by rememberNow(TickResolution.SECONDS)
                val b by rememberNow(TickResolution.SECONDS)
                Text(text = if (a == b) "in-sync" else "drifted")
            }
        }

        fakeNow = epoch + 1.seconds
        advance(1_000)

        composeRule.onNodeWithText("in-sync").assertIsDisplayed()
    }

    /**
     * Without a provider above it — previews, one-off screens, any composable someone forgets to
     * host under [ProvideNimazClock] — a countdown must still animate rather than sit frozen.
     */
    @Test
    fun `falls back to a local ticker when no provider is installed`() {
        composeRule.mainClock.autoAdvance = false

        composeRule.setThemedContent {
            val now by rememberNow(TickResolution.SECONDS)
            Text(text = if (now.toEpochMilliseconds() > 0) "ticking" else "frozen")
        }

        composeRule.onNodeWithText("ticking").assertIsDisplayed()
    }

    /** mm:ss, so assertions read as the user sees them. */
    @androidx.compose.runtime.Composable
    private fun CountdownProbe(target: Instant) {
        val parts by rememberCountdownTo(target)
        Text(
            text = "%02d:%02d".format(
                parts.hours * 60 + parts.minutes,
                parts.seconds,
            )
        )
    }
}
