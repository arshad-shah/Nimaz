package com.arshadshah.nimaz.presentation.foundation.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sqrt

/**
 * The arc's shape is a closed form, not a hand-drawn curve, so it can be pinned exactly: solar
 * noon is midway between sunrise and sunset by definition, which makes a cosine through both
 * crossings the only curve that is 1 at the apex and 0 at both horizons.
 */
class SolarArcGeometryTest {

    // Dublin, early September.
    private val sunrise = 0.27f
    private val sunset = 0.80f
    private val dhuhr = (sunrise + sunset) / 2f

    @Test
    fun `the apex is exactly one at solar noon`() {
        assertThat(solarAltitude(dhuhr, sunrise, sunset)).isWithin(1e-4f).of(1f)
    }

    @Test
    fun `the curve is zero at both horizon crossings`() {
        assertThat(solarAltitude(sunrise, sunrise, sunset)).isWithin(1e-4f).of(0f)
        assertThat(solarAltitude(sunset, sunrise, sunset)).isWithin(1e-4f).of(0f)
    }

    @Test
    fun `midnight is below the horizon`() {
        assertThat(solarAltitude(0f, sunrise, sunset)).isLessThan(0f)
    }

    @Test
    fun `the curve is symmetric about solar noon`() {
        val before = solarAltitude(dhuhr - 0.1f, sunrise, sunset)
        val after = solarAltitude(dhuhr + 0.1f, sunrise, sunset)
        assertThat(before).isWithin(1e-4f).of(after)
    }

    /**
     * The real seasonal property. The apex is normalised to 1 in every season, so what a short
     * day changes is the *night*: Dublin in December troughs near -2.6, in June near -0.2.
     */
    @Test
    fun `a short winter day troughs far deeper than a long summer one`() {
        val summer = solarAltitude(0f, sunriseFraction = 0.20f, sunsetFraction = 0.90f)
        val winter = solarAltitude(0f, sunriseFraction = 0.35f, sunsetFraction = 0.70f)
        assertThat(winter).isLessThan(summer)
        assertThat(summer).isWithin(0.01f).of(-0.23f)
        assertThat(winter).isWithin(0.01f).of(-2.64f)
    }

    @Test
    fun `a full day of daylight never goes below the horizon`() {
        // Polar summer: the crossings meet at midnight.
        val values = (0..20).map { solarAltitude(it / 20f, 0f, 1f) }
        assertThat(values.min()).isAtLeast(-1e-4f)
    }

    @Test
    fun `the drawn curve never leaves minus one to one`() {
        // December's raw trough is -2.64; the drawn one must be clamped.
        val values = (0..100).map { drawnAltitude(it / 100f, 0.35f, 0.70f) }
        assertThat(values.min()).isAtLeast(-1f)
        assertThat(values.max()).isAtMost(1f)
    }

    @Test
    fun `night is compressed rather than drawn at full depth`() {
        val raw = solarAltitude(0f, sunrise, sunset)
        val drawn = drawnAltitude(0f, sunrise, sunset)
        assertThat(drawn).isGreaterThan(raw)
        assertThat(drawn).isLessThan(0f)
        // A soft knee: the root of the depth, not the depth.
        assertThat(drawn).isWithin(1e-4f).of(-sqrt(-raw) * NightCompression)
    }

    /**
     * Why the knee is a root rather than a constant multiplier. A linear compression cannot serve
     * both ends of the year: any k that keeps December's -2.64 inside the band flattens June's
     * -0.23 to nothing. Both must stay visible, and winter must stay the deeper of the two.
     */
    @Test
    fun `both a shallow summer night and a deep winter one stay legible`() {
        val summer = drawnAltitude(0f, sunriseFraction = 0.20f, sunsetFraction = 0.90f)
        val winter = drawnAltitude(0f, sunriseFraction = 0.35f, sunsetFraction = 0.70f)

        assertThat(winter).isLessThan(summer)   // winter is still the deeper night
        assertThat(summer).isLessThan(-0.15f)   // and summer has not been flattened away
        assertThat(winter).isGreaterThan(-1f)   // while winter no longer bottoms out at the clamp
    }

    @Test
    fun `daylight is not compressed`() {
        assertThat(drawnAltitude(dhuhr, sunrise, sunset)).isWithin(1e-4f).of(1f)
    }

    @Test
    fun `a sunset at or before sunrise gives a flat curve rather than throwing`() {
        assertThat(solarAltitude(0.5f, 0.6f, 0.6f)).isEqualTo(0f)
        assertThat(solarAltitude(0.5f, 0.8f, 0.2f)).isEqualTo(0f)
    }

    @Test
    fun `out of range fractions give a flat curve`() {
        assertThat(solarAltitude(0.5f, -0.2f, 0.8f)).isEqualTo(0f)
        assertThat(solarAltitude(0.5f, 0.2f, 1.4f)).isEqualTo(0f)
    }

    @Test
    fun `NaN and infinity give a flat curve rather than throwing`() {
        assertThat(solarAltitude(Float.NaN, sunrise, sunset)).isEqualTo(0f)
        assertThat(solarAltitude(0.5f, Float.NaN, sunset)).isEqualTo(0f)
        assertThat(solarAltitude(0.5f, sunrise, Float.POSITIVE_INFINITY)).isEqualTo(0f)
        assertThat(drawnAltitude(Float.NaN, sunrise, sunset)).isEqualTo(0f)
    }
}
