package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [QiblaCalculator] — the pure great-circle math that points a
 * user toward the Kaaba. A bug here sends worshippers the wrong way, so the
 * bearing/distance formulas are verified against independently-known values.
 */
class QiblaCalculatorTest {

    // Kaaba coordinates the calculator targets internally.
    private val kaabaLat = 21.4225
    private val kaabaLng = 39.8262

    // Tolerances: bearings within 2°, distances within 2% of reference values.
    private val bearingTolerance = 2.0

    // ── calculateQiblaDirection: known cities ───────────────────────────────

    @Test
    fun `Qibla bearing from New York is roughly north-east`() {
        // Independently-known Qibla from NYC (40.7128, -74.0060) ≈ 58°.
        val direction = QiblaCalculator.calculateQiblaDirection(40.7128, -74.0060)
        assertThat(direction.bearing).isWithin(bearingTolerance).of(58.48)
        assertThat(QiblaCalculator.getCardinalDirection(direction.bearing)).isEqualTo("NE")
    }

    @Test
    fun `Qibla bearing from London is roughly south-east`() {
        // Known Qibla from London (51.5074, -0.1278) ≈ 119°.
        val direction = QiblaCalculator.calculateQiblaDirection(51.5074, -0.1278)
        assertThat(direction.bearing).isWithin(bearingTolerance).of(118.99)
        assertThat(QiblaCalculator.getCardinalDirection(direction.bearing)).isEqualTo("SE")
    }

    @Test
    fun `Qibla bearing from Jakarta is roughly west-north-west`() {
        // Known Qibla from Jakarta (-6.2, 106.8) ≈ 295°.
        val direction = QiblaCalculator.calculateQiblaDirection(-6.2, 106.8)
        assertThat(direction.bearing).isWithin(bearingTolerance).of(295.15)
        assertThat(QiblaCalculator.getCardinalDirection(direction.bearing)).isEqualTo("NW")
    }

    @Test
    fun `bearing is always normalized to 0-360 range`() {
        val samples = listOf(
            40.7128 to -74.0060,
            51.5074 to -0.1278,
            -33.87 to 151.21,
            35.68 to 139.69,
            -22.9 to -43.2,
            0.0 to 0.0
        )
        for ((lat, lng) in samples) {
            val bearing = QiblaCalculator.calculateQiblaDirection(lat, lng).bearing
            assertThat(bearing).isAtLeast(0.0)
            assertThat(bearing).isLessThan(360.0)
        }
    }

    @Test
    fun `direction echoes back the user coordinates`() {
        val direction = QiblaCalculator.calculateQiblaDirection(12.34, 56.78)
        assertThat(direction.userLatitude).isEqualTo(12.34)
        assertThat(direction.userLongitude).isEqualTo(56.78)
    }

    // ── Distance ────────────────────────────────────────────────────────────

    @Test
    fun `distance from the Kaaba to itself is zero`() {
        val direction = QiblaCalculator.calculateQiblaDirection(kaabaLat, kaabaLng)
        assertThat(direction.distance).isWithin(0.001).of(0.0)
    }

    @Test
    fun `distance from London to Kaaba is around 4800 km`() {
        val direction = QiblaCalculator.calculateQiblaDirection(51.5074, -0.1278)
        // Great-circle London → Makkah ≈ 4,800 km.
        assertThat(direction.distance).isWithin(150.0).of(4800.0)
    }

    @Test
    fun `distance from New York to Kaaba is around 10300 km`() {
        val direction = QiblaCalculator.calculateQiblaDirection(40.7128, -74.0060)
        assertThat(direction.distance).isWithin(200.0).of(10300.0)
    }

    @Test
    fun `calculateDistanceToMecca returns the same distance in meters`() {
        val km = QiblaCalculator.calculateQiblaDirection(51.5074, -0.1278).distance
        val meters = QiblaCalculator.calculateDistanceToMecca(51.5074, -0.1278)
        assertThat(meters).isWithin(0.001).of(km * 1000)
    }

    // ── calculateQiblaAngle ─────────────────────────────────────────────────

    @Test
    fun `qibla angle is the difference between bearing and heading`() {
        assertThat(QiblaCalculator.calculateQiblaAngle(compassHeading = 0f, qiblaBearing = 90.0))
            .isWithin(0.001f).of(90f)
    }

    @Test
    fun `qibla angle is zero when heading matches bearing`() {
        assertThat(QiblaCalculator.calculateQiblaAngle(compassHeading = 119f, qiblaBearing = 119.0))
            .isWithin(0.001f).of(0f)
    }

    @Test
    fun `qibla angle wraps into 0-360 when heading exceeds bearing`() {
        // 10 - 350 = -340, normalized to 20.
        assertThat(QiblaCalculator.calculateQiblaAngle(compassHeading = 350f, qiblaBearing = 10.0))
            .isWithin(0.001f).of(20f)
    }

    @Test
    fun `qibla angle is never negative and below 360`() {
        for (heading in 0..359) {
            for (bearing in listOf(0.0, 58.0, 119.0, 295.0, 359.0)) {
                val angle = QiblaCalculator.calculateQiblaAngle(heading.toFloat(), bearing)
                assertThat(angle).isAtLeast(0f)
                assertThat(angle).isLessThan(360f)
            }
        }
    }

    // ── getCardinalDirection boundaries ─────────────────────────────────────

    @Test
    fun `cardinal direction maps each 45 degree sector`() {
        assertThat(QiblaCalculator.getCardinalDirection(0.0)).isEqualTo("N")
        assertThat(QiblaCalculator.getCardinalDirection(45.0)).isEqualTo("NE")
        assertThat(QiblaCalculator.getCardinalDirection(90.0)).isEqualTo("E")
        assertThat(QiblaCalculator.getCardinalDirection(135.0)).isEqualTo("SE")
        assertThat(QiblaCalculator.getCardinalDirection(180.0)).isEqualTo("S")
        assertThat(QiblaCalculator.getCardinalDirection(225.0)).isEqualTo("SW")
        assertThat(QiblaCalculator.getCardinalDirection(270.0)).isEqualTo("W")
        assertThat(QiblaCalculator.getCardinalDirection(315.0)).isEqualTo("NW")
    }

    @Test
    fun `cardinal direction handles sector boundaries and wrap-around`() {
        // North wraps around 360/0.
        assertThat(QiblaCalculator.getCardinalDirection(337.5)).isEqualTo("N")
        assertThat(QiblaCalculator.getCardinalDirection(359.9)).isEqualTo("N")
        assertThat(QiblaCalculator.getCardinalDirection(22.4)).isEqualTo("N")
        // Lower boundary of NE sector.
        assertThat(QiblaCalculator.getCardinalDirection(22.5)).isEqualTo("NE")
    }
}
