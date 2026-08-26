package com.arshadshah.nimaz.presentation.foundation.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The heading readout, which had been printing the dial's internal value.
 *
 * The compass screen keeps its azimuth **unwrapped** — accumulated across the 359°→0° seam so
 * the needle animates the short way instead of unwinding through 358 degrees — so the number is
 * routinely negative and, after a few turns in one hand, well past 360. Fed straight to the
 * readout it produced "You are facing −67° NW": the letters right, because
 * [cardinalDirection] normalises for itself, and the number one no compass shows.
 */
class CompassDegreesTest {

    @Test
    fun `a heading already in range is unchanged`() {
        assertThat(compassDegrees(113f)).isEqualTo(113)
    }

    @Test
    fun `a negative heading reads as its compass equivalent`() {
        // The reported case, from the screenshot: -67 is 293, west-north-west.
        assertThat(compassDegrees(-67f)).isEqualTo(293)
        assertThat(cardinalDirection(-67f)).isEqualTo("NW")
    }

    @Test
    fun `a heading wound past a full turn comes back round`() {
        assertThat(compassDegrees(421f)).isEqualTo(61)
        assertThat(compassDegrees(-421f)).isEqualTo(299)
    }

    @Test
    fun `several turns in one hand still read as one heading`() {
        assertThat(compassDegrees(1080f)).isEqualTo(0)
        assertThat(compassDegrees(-1080f)).isEqualTo(0)
    }

    @Test
    fun `just short of north reads as north, never as 360`() {
        // Rounds first, then wraps. The other order gives 360°, which is a degree that does not
        // appear on a compass rose.
        assertThat(compassDegrees(359.7f)).isEqualTo(0)
        assertThat(compassDegrees(-0.4f)).isEqualTo(0)
    }
}
