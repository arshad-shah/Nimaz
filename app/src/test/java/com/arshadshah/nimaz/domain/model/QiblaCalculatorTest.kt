package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [QiblaCalculator] — great-circle bearing and distance to the
 * Kaaba plus the compass-rotation helpers. The expected bearings below are the
 * well-known Qibla directions for major cities; the distance assertions use a
 * deterministic one-degree offset so the haversine result can be checked
 * against the analytic value (≈111.19 km per degree of latitude).
 */
class QiblaCalculatorTest {

    private val kaabaLat = 21.4225
    private val kaabaLon = 39.8262

    // ── Bearing ─────────────────────────────────────────────────────

    @Test
    fun `bearing from London points roughly south-east toward Mecca`() {
        val direction = QiblaCalculator.calculateQiblaDirection(51.5074, -0.1278)
        assertThat(direction.bearing).isWithin(1.0).of(119.0)
    }

    @Test
    fun `bearing from New York points roughly north-east toward Mecca`() {
        val direction = QiblaCalculator.calculateQiblaDirection(40.7128, -74.0060)
        assertThat(direction.bearing).isWithin(1.5).of(58.5)
    }

    @Test
    fun `bearing is always normalized to the 0 to 360 range`() {
        // A southern-hemisphere, eastern location (Jakarta) exercises the
        // (bearing + 360) % 360 normalization.
        val direction = QiblaCalculator.calculateQiblaDirection(-6.2088, 106.8456)
        assertThat(direction.bearing).isAtLeast(0.0)
        assertThat(direction.bearing).isLessThan(360.0)
    }

    @Test
    fun `bearing and distance are near zero at the Kaaba itself`() {
        val direction = QiblaCalculator.calculateQiblaDirection(kaabaLat, kaabaLon)
        assertThat(direction.distance).isWithin(1e-6).of(0.0)
        assertThat(direction.bearing).isWithin(1e-6).of(0.0)
    }

    // ── Distance ────────────────────────────────────────────────────

    @Test
    fun `distance for a one-degree latitude offset matches the analytic value`() {
        // One degree of latitude along the same meridian ≈ π/180 * 6371 km.
        val expectedKm = Math.toRadians(1.0) * 6371.0
        val direction = QiblaCalculator.calculateQiblaDirection(kaabaLat + 1.0, kaabaLon)
        assertThat(direction.distance).isWithin(0.5).of(expectedKm)
    }

    @Test
    fun `calculateDistanceToMecca returns the same value in meters`() {
        val lat = kaabaLat + 1.0
        val lon = kaabaLon
        val km = QiblaCalculator.calculateQiblaDirection(lat, lon).distance
        val meters = QiblaCalculator.calculateDistanceToMecca(lat, lon)
        assertThat(meters).isWithin(1e-3).of(km * 1000.0)
    }

    // ── Qibla rotation angle ────────────────────────────────────────

    @Test
    fun `qibla angle is zero when heading already faces the bearing`() {
        assertThat(QiblaCalculator.calculateQiblaAngle(119f, 119.0)).isWithin(1e-4f).of(0f)
    }

    @Test
    fun `qibla angle equals the bearing when heading is north`() {
        assertThat(QiblaCalculator.calculateQiblaAngle(0f, 119.0)).isWithin(1e-4f).of(119f)
    }

    @Test
    fun `qibla angle wraps into 0 to 360 for negative differences`() {
        // 10 - 350 = -340 -> +360 = 20
        assertThat(QiblaCalculator.calculateQiblaAngle(350f, 10.0)).isWithin(1e-4f).of(20f)
    }

    // ── Cardinal direction buckets ──────────────────────────────────

    @Test
    fun `cardinal direction maps the eight compass octants`() {
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
    fun `cardinal direction wraps around north at the 337_5 boundary`() {
        assertThat(QiblaCalculator.getCardinalDirection(337.5)).isEqualTo("N")
        assertThat(QiblaCalculator.getCardinalDirection(359.9)).isEqualTo("N")
        assertThat(QiblaCalculator.getCardinalDirection(22.4)).isEqualTo("N")
        assertThat(QiblaCalculator.getCardinalDirection(22.5)).isEqualTo("NE")
    }
}
