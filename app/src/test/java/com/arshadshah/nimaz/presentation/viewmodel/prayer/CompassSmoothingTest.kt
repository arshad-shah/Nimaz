package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * How long the qibla needle takes to mean anything.
 *
 * `QiblaViewModel` itself cannot be constructed in a JVM test — `SensorManager`, `Vibrator`,
 * `Context` — and `updateCompassData` is reachable only through a `SensorEvent`, which is not
 * constructible either. The filter is the whole of the defect though, and it is pure, so it is
 * lifted out and tested directly rather than left untestable until the #360 seam lands.
 */
class CompassSmoothingTest {

    /** A phone lying flat: gravity straight down. */
    private val truth = floatArrayOf(0f, 0f, 9.81f)

    @Test
    fun `the first sample is taken at face value`() {
        val filtered = FloatArray(3)

        smoothInto(filtered, truth, seed = true)

        // Unseeded, `0.97 * 0 + 0.03 * 9.81` = 0.29 — three per cent of the truth, published
        // as ready.
        assertThat(filtered[2]).isEqualTo(9.81f)
    }

    @Test
    fun `an unseeded filter is nowhere near the truth after one sample`() {
        val filtered = FloatArray(3)

        smoothInto(filtered, truth, seed = false)

        // The behaviour that shipped, stated so the seeding above has something to be measured
        // against.
        assertThat(filtered[2]).isLessThan(0.5f)
    }

    @Test
    fun `an unseeded filter needs about a hundred samples to converge`() {
        val filtered = FloatArray(3)
        var samples = 0
        while (abs(filtered[2] - truth[2]) > truth[2] * 0.05f) {
            smoothInto(filtered, truth, seed = false)
            samples++
        }

        // At SENSOR_DELAY_GAME (~20 ms) that is roughly two seconds of the needle sweeping in
        // from a heading that means nothing — repeated on every entry, because the screen's
        // DisposableEffect stops and starts the compass each time.
        assertThat(samples).isGreaterThan(90)
    }

    @Test
    fun `a seeded filter is within tolerance immediately`() {
        val filtered = FloatArray(3)

        smoothInto(filtered, truth, seed = true)

        assertThat(abs(filtered[2] - truth[2])).isLessThan(truth[2] * 0.05f)
    }

    @Test
    fun `later samples are still smoothed, not replaced`() {
        val filtered = FloatArray(3)
        smoothInto(filtered, truth, seed = true)

        // A single noisy reading must not throw the needle: that is what the filter is for, and
        // seeding must not turn it into a pass-through.
        smoothInto(filtered, floatArrayOf(0f, 0f, 50f), seed = false)

        // 0.97 × 9.81 + 0.03 × 50 ≈ 11.02: the spike moves the estimate by about 1.2, not to 50.
        assertThat(filtered[2]).isLessThan(12f)
        assertThat(filtered[2]).isGreaterThan(9.81f)
    }

    @Test
    fun `every axis is filtered`() {
        val filtered = FloatArray(3)

        smoothInto(filtered, floatArrayOf(1f, 2f, 3f), seed = true)

        assertThat(filtered.toList()).containsExactly(1f, 2f, 3f).inOrder()
    }
}
