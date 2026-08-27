package com.arshadshah.nimaz.presentation.components.atoms

import com.arshadshah.nimaz.core.common.CountdownParts
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * How a countdown reads — the rendering half of the app's one countdown arithmetic.
 *
 * `CountdownParts` is deliberately pure numbers so it can never hardcode English unit suffixes;
 * this is where those numbers become words, and the `when` that does it picks a *different string
 * resource per lead unit*. That is the whole design: "2h 14m" once the target is hours away, "14m
 * 09s" inside the hour, "09s" at the end — and dropping to a single format would either print
 * "0h 0m 09s" on the final approach or "2h" with no minutes when it matters most.
 *
 * The `elapsed` arm outranks all of it, because a countdown that has run out must say so rather
 * than sit at zero: the prayer time has arrived, and "00s" reads as a stuck timer.
 */
@RunWith(RobolectricTestRunner::class)
class NimazCountdownTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    /** Every rendering in one composition — a rule takes one `setContent` (#604). */
    private fun render(vararg cases: Pair<CountdownParts, Boolean>): List<String> {
        val out = MutableList(cases.size) { "" }
        composeRule.setThemedContent {
            cases.forEachIndexed { index, (parts, showSeconds) ->
                out[index] = countdownText(parts, showSeconds = showSeconds)
            }
        }
        composeRule.waitForIdle()
        return out
    }

    @Test
    fun `an elapsed countdown says the moment has come`() {
        // Outranks every other arm, including `showSeconds`. A countdown that renders "00s"
        // instead reads as a stopped clock at exactly the moment the prayer time arrives.
        val (withSeconds, withoutSeconds) = render(
            CountdownParts.ZERO to true,
            CountdownParts.ZERO to false,
        )

        assertThat(withSeconds).isNotEmpty()
        assertThat(withSeconds).isEqualTo(withoutSeconds)
        assertThat(withSeconds).doesNotContain("0")
    }

    @Test
    fun `each lead unit gets its own format`() {
        // Three resources, picked by which unit is the coarsest non-zero one. Collapsing them into
        // one gives "0h 0m 09s" on the final approach.
        val (hours, minutes, seconds) = render(
            CountdownParts(2, 14, 9, elapsed = false) to true,
            CountdownParts(0, 14, 9, elapsed = false) to true,
            CountdownParts(0, 0, 9, elapsed = false) to true,
        )

        assertThat(hours).contains("2")
        assertThat(hours).contains("14")
        assertThat(hours).contains("9")

        assertThat(minutes).contains("14")
        assertThat(minutes).contains("9")
        assertThat(minutes).doesNotContain("0h")

        assertThat(seconds).contains("9")
        assertThat(seconds).isNotEqualTo(minutes)
    }

    @Test
    fun `hiding seconds drops to two units and then to one`() {
        // The coarse rendering used by cards that do not tick every second — and the reason
        // `rememberCountdownTo` has to be told, or the seconds digit sits still for a minute and
        // then jumps.
        val (hours, minutes, secondsOnly) = render(
            CountdownParts(2, 14, 9, elapsed = false) to false,
            CountdownParts(0, 14, 9, elapsed = false) to false,
            CountdownParts(0, 0, 9, elapsed = false) to false,
        )

        assertThat(hours).contains("2")
        assertThat(hours).contains("14")
        assertThat(minutes).contains("14")
        // Under a minute with seconds hidden there is nothing left but the minute figure, which is
        // zero — the alternative would be a blank countdown.
        assertThat(secondsOnly).isNotEmpty()
    }

    @Test
    fun `the lead unit is the coarsest non-zero field`() {
        // The property the renderer's `when` is keyed on. An hours field of zero must not select
        // the hours format, or every countdown under an hour reads "0h".
        assertThat(CountdownParts(1, 0, 0, elapsed = false).leadUnit)
            .isEqualTo(com.arshadshah.nimaz.core.common.CountdownUnit.HOURS)
        assertThat(CountdownParts(0, 1, 0, elapsed = false).leadUnit)
            .isEqualTo(com.arshadshah.nimaz.core.common.CountdownUnit.MINUTES)
        assertThat(CountdownParts(0, 0, 1, elapsed = false).leadUnit)
            .isEqualTo(com.arshadshah.nimaz.core.common.CountdownUnit.SECONDS)
    }
}
