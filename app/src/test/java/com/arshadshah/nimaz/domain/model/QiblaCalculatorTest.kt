package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The qibla bearing and the phone's compass are measured from **different norths**.
 *
 * `calculateQiblaDirection` returns a great-circle bearing from **true** north.
 * `SensorManager.getOrientation` returns an azimuth from **magnetic** north. The two differ by
 * the local magnetic declination — near zero across western Europe, but roughly -13° in New
 * York, +15° in Seattle and Anchorage, +20° in Auckland and -25° in Cape Town.
 *
 * `QiblaViewModel.updateCompassData` subtracted one from the other directly, so the arrow was
 * off by exactly the local declination, and the "you are facing the qibla" confirmation — whose
 * threshold is **5°** — fired while the reader was up to 25° off, and refused to fire when they
 * were correct. A `trueNorthMode` flag was on the settings state, defaulted to `true` and
 * settable from the UI, but nothing ever read it: the correction was designed and never wired.
 *
 * So the correction lives here, as arithmetic that can be checked without a magnetometer.
 */
class QiblaCalculatorTest {

    // Declinations are approximate 2026-epoch values; the maths is what is under test, not WMM.
    private val newYork = Triple(40.7128, -74.0060, -13.0f)
    private val london = Triple(51.5074, -0.1278, 0.5f)
    private val jakarta = Triple(-6.2088, 106.8456, 0.5f)

    @Test
    fun `a magnetic reading is turned into a true-north one by adding the declination`() {
        // Magnetic north in New York sits ~13° west of true north, so a phone reading 0°
        // magnetic is actually pointing 13° west of true north — that is 347°.
        assertThat(QiblaCalculator.trueAzimuth(magneticAzimuth = 0f, declination = -13f))
            .isWithin(TOLERANCE).of(347f)

        assertThat(QiblaCalculator.trueAzimuth(magneticAzimuth = 10f, declination = 15f))
            .isWithin(TOLERANCE).of(25f)
    }

    @Test
    fun `true azimuth stays inside a single turn`() {
        assertThat(QiblaCalculator.trueAzimuth(355f, 20f)).isWithin(TOLERANCE).of(15f)
        assertThat(QiblaCalculator.trueAzimuth(5f, -20f)).isWithin(TOLERANCE).of(345f)
        assertThat(QiblaCalculator.trueAzimuth(0f, 0f)).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `the rotation to the qibla accounts for declination`() {
        val (lat, lon, declination) = newYork
        val bearing = QiblaCalculator.calculateQiblaDirection(lat, lon).bearing.toFloat()

        // A phone whose *magnetic* reading equals the true qibla bearing is NOT facing the
        // qibla — it is off by the declination, which is the whole defect.
        val rotation = QiblaCalculator.rotationToQibla(
            qiblaBearing = bearing,
            magneticAzimuth = bearing,
            declination = declination
        )
        assertThat(rotation).isWithin(TOLERANCE).of(-declination)

        // Turning to the magnetic heading that *corresponds* to the true bearing lands on zero.
        val corrected = QiblaCalculator.rotationToQibla(
            qiblaBearing = bearing,
            magneticAzimuth = bearing - declination,
            declination = declination
        )
        assertThat(corrected).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `the rotation is the shorter way round, never the long way`() {
        // 350° to 10° is a 20° turn right, not a 340° turn left.
        assertThat(QiblaCalculator.rotationToQibla(10f, 350f, 0f)).isWithin(TOLERANCE).of(20f)
        assertThat(QiblaCalculator.rotationToQibla(350f, 10f, 0f)).isWithin(TOLERANCE).of(-20f)

        listOf(0f, 45f, 90f, 179f, 180f, 181f, 270f, 359f).forEach { azimuth ->
            listOf(0f, 30f, 120f, 200f, 300f).forEach { bearing ->
                val rotation = QiblaCalculator.rotationToQibla(bearing, azimuth, 0f)
                assertThat(rotation).isAtMost(180f)
                assertThat(rotation).isGreaterThan(-180f)
            }
        }
    }

    @Test
    fun `zero declination leaves the reading alone`() {
        // Most of western Europe, where the defect was invisible.
        val (lat, lon, _) = london
        val bearing = QiblaCalculator.calculateQiblaDirection(lat, lon).bearing.toFloat()

        assertThat(QiblaCalculator.rotationToQibla(bearing, bearing, 0f))
            .isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `known qibla bearings are still correct`() {
        // Sanity anchors for the underlying great-circle bearing, which the correction wraps.
        // London faces roughly east-southeast; Jakarta faces roughly west-northwest.
        val (londonLat, londonLon, _) = london
        assertThat(QiblaCalculator.calculateQiblaDirection(londonLat, londonLon).bearing)
            .isWithin(1.0).of(118.99)

        val (jakartaLat, jakartaLon, _) = jakarta
        assertThat(QiblaCalculator.calculateQiblaDirection(jakartaLat, jakartaLon).bearing)
            .isWithin(1.0).of(295.15)

        val (nyLat, nyLon, _) = newYork
        assertThat(QiblaCalculator.calculateQiblaDirection(nyLat, nyLon).bearing)
            .isWithin(1.0).of(58.48)
    }

    @Test
    fun `standing on the Kaaba does not produce a NaN bearing`() {
        val direction = QiblaCalculator.calculateQiblaDirection(21.4225, 39.8262)

        assertThat(direction.bearing.isNaN()).isFalse()
        assertThat(direction.distance).isWithin(0.5).of(0.0)
    }

    @Test
    fun `distance to Mecca is the great-circle distance, in metres`() {
        // London → Mecca is ~4,800 km. Note the unit: `QiblaDirection.distance` is kilometres
        // but `calculateDistanceToMecca` multiplies by 1000, so the two disagree. Nothing
        // displays either today, so this pins the behaviour rather than asserting a preference.
        val (lat, lon, _) = london
        assertThat(QiblaCalculator.calculateDistanceToMecca(lat, lon))
            .isWithin(50_000.0).of(4_790_000.0)
        assertThat(QiblaCalculator.calculateQiblaDirection(lat, lon).distance)
            .isWithin(50.0).of(4_790.0)
    }

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
