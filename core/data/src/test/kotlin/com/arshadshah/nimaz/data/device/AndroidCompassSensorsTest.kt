package com.arshadshah.nimaz.data.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager

/**
 * The qibla needle's input.
 *
 * Two sensors feed one heading, and neither the screen nor a user can tell a wrong heading from
 * a right one — a compass that is confidently 40° out looks exactly like a compass that works.
 * What is pinned here:
 *
 *  - **the filter is seeded, not started from zero.** A low-pass filter at 0.97 needs about a
 *    hundred samples to converge, so an unseeded one publishes headings derived from a vector
 *    of zeroes. That is what used to make the screen say "ready" and then visibly sweep the
 *    needle in from a meaningless direction. One sample is a worse estimate than a hundred, but
 *    it is an estimate *of the right thing*;
 *  - **nothing is published until both sensors have reported.** A rotation matrix built from a
 *    half-filled vector is not an error — it is a number, and it is wrong;
 *  - **accuracy comes from the magnetometer only.** The accelerometer reports its own accuracy
 *    constantly, and letting it through would show "calibrate your phone" at random;
 *  - **the listener's lifetime is the collection's.** It is unregistered on cancellation, which
 *    is what the ViewModel's `onCleared()` used to have to remember.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidCompassSensorsTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var shadowSensors: ShadowSensorManager
    private lateinit var sensors: AndroidCompassSensors

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shadowSensors = Shadow.extract(sensorManager)
        shadowSensors.addSensor(Sensor.TYPE_ACCELEROMETER, accelerometer())
        shadowSensors.addSensor(Sensor.TYPE_MAGNETIC_FIELD, magnetometer())
        sensors = AndroidCompassSensors(context)
    }

    // ── the low-pass filter, on its own ───────────────────────────────────────

    @Test
    fun `a seeded filter takes the first sample at face value`() {
        val filtered = FloatArray(3)

        smoothInto(filtered, floatArrayOf(0f, 0f, 9.81f), seed = true)

        assertThat(filtered.toList()).containsExactly(0f, 0f, 9.81f).inOrder()
    }

    @Test
    fun `an unseeded filter barely moves, which is why the first sample must seed it`() {
        val filtered = FloatArray(3)

        smoothInto(filtered, floatArrayOf(0f, 0f, 9.81f), seed = false)

        // 3% of one sample. A hundred of these to converge is the whole reason `seed` exists.
        assertThat(filtered[2]).isWithin(0.001f).of(9.81f * 0.03f)
    }

    @Test
    fun `subsequent samples are smoothed towards the new reading, not snapped to it`() {
        val filtered = floatArrayOf(0f, 0f, 10f)

        smoothInto(filtered, floatArrayOf(0f, 0f, 0f), seed = false)

        assertThat(filtered[2]).isGreaterThan(9f)
        assertThat(filtered[2]).isLessThan(10f)
    }

    // ── availability ──────────────────────────────────────────────────────────

    @Test
    fun `a phone with both sensors reports a compass`() {
        assertThat(sensors.isAvailable).isTrue()
    }

    @Test
    fun `a phone with no magnetometer reports no compass`() {
        shadowSensors.removeSensor(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!)

        assertThat(AndroidCompassSensors(context).isAvailable).isFalse()
    }

    @Test
    fun `a phone with no accelerometer reports no compass`() {
        shadowSensors.removeSensor(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!)

        assertThat(AndroidCompassSensors(context).isAvailable).isFalse()
    }

    // ── the heading ───────────────────────────────────────────────────────────

    @Test
    fun `nothing is published until both sensors have reported`() = runTest {
        sensors.orientation().test {
            sendAccelerometer(0f, 0f, 9.81f)

            // A rotation matrix from a half-filled vector is a number, and it is the wrong one.
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `a phone lying flat and facing north reads about zero degrees`() = runTest {
        sensors.orientation().test {
            sendAccelerometer(0f, 0f, 9.81f)
            sendMagnetometer(0f, 40f, 0f)

            val heading = awaitItem()
            assertThat(heading.azimuthDegrees).isWithin(1f).of(0f)
            assertThat(heading.pitchDegrees).isWithin(1f).of(0f)
            cancel()
        }
    }

    @Test
    fun `the first sample seeds the filter, so the first heading is already usable`() = runTest {
        sensors.orientation().test {
            sendAccelerometer(0f, 0f, 9.81f)
            // A field pointing along -x is 90° away from one along +y.
            sendMagnetometer(-40f, 0f, 0f)

            // Unseeded, the first heading would be derived from a near-zero vector and land
            // nowhere near the right quadrant.
            assertThat(awaitItem().azimuthDegrees).isWithin(2f).of(90f)
            cancel()
        }
    }

    @Test
    fun `an azimuth is normalised into zero to three sixty, never negative`() = runTest {
        sensors.orientation().test {
            sendAccelerometer(0f, 0f, 9.81f)
            sendMagnetometer(40f, 0f, 0f)

            val heading = awaitItem()
            assertThat(heading.azimuthDegrees).isAtLeast(0f)
            assertThat(heading.azimuthDegrees).isLessThan(360f)
            assertThat(heading.azimuthDegrees).isWithin(2f).of(270f)
            cancel()
        }
    }

    @Test
    fun `a reading with no usable rotation matrix publishes nothing`() = runTest {
        sensors.orientation().test {
            // Gravity and the field pointing the same way: the cross product is degenerate and
            // `getRotationMatrix` refuses. Publishing anyway would swing the needle at random.
            sendAccelerometer(0f, 0f, 9.81f)
            sendMagnetometer(0f, 0f, 40f)

            expectNoEvents()
            cancel()
        }
    }

    // ── accuracy ──────────────────────────────────────────────────────────────

    @Test
    fun `each magnetometer accuracy maps to its own level`() = runTest {
        val expected = mapOf(
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH to CompassAccuracy.HIGH,
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM to CompassAccuracy.MEDIUM,
            SensorManager.SENSOR_STATUS_ACCURACY_LOW to CompassAccuracy.LOW,
            SensorManager.SENSOR_STATUS_UNRELIABLE to CompassAccuracy.UNRELIABLE,
        )

        expected.forEach { (status, level) ->
            sensors.orientation().test {
                shadowSensors.sendSensorEventToListeners(
                    ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_MAGNETIC_FIELD)
                )
                listeners().forEach { it.onAccuracyChanged(magnetometer(), status) }
                sendAccelerometer(0f, 0f, 9.81f)
                sendMagnetometer(0f, 40f, 0f)

                assertThat(awaitItem().accuracy).isEqualTo(level)
                cancel()
            }
        }
    }

    @Test
    fun `the accelerometer's accuracy is ignored`() = runTest {
        sensors.orientation().test {
            listeners().forEach {
                it.onAccuracyChanged(magnetometer(), SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
                // The accelerometer reports constantly; letting it through would show
                // "calibrate your phone" at random.
                it.onAccuracyChanged(accelerometer(), SensorManager.SENSOR_STATUS_UNRELIABLE)
            }
            sendAccelerometer(0f, 0f, 9.81f)
            sendMagnetometer(0f, 40f, 0f)

            assertThat(awaitItem().accuracy).isEqualTo(CompassAccuracy.HIGH)
            cancel()
        }
    }

    @Test
    fun `a sensor the compass does not use changes nothing`() = runTest {
        sensors.orientation().test {
            shadowSensors.sendSensorEventToListeners(
                ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_GYROSCOPE)
            )

            expectNoEvents()
            cancel()
        }
    }

    // ── the listener's lifetime ───────────────────────────────────────────────

    @Test
    fun `both sensors are registered while collecting and released when it ends`() = runTest {
        assertThat(shadowSensors.hasListener(null)).isFalse()

        sensors.orientation().test {
            sendAccelerometer(0f, 0f, 9.81f)
            sendMagnetometer(0f, 40f, 0f)
            awaitItem()
            assertThat(listeners()).isNotEmpty()
            cancel()
        }

        // The ViewModel's onCleared() used to have to remember this.
        assertThat(listeners()).isEmpty()
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun listeners() = shadowSensors.listeners.toList()

    private fun sendAccelerometer(x: Float, y: Float, z: Float) =
        shadowSensors.sendSensorEventToListeners(
            ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_ACCELEROMETER)
                .also { setValues(it, x, y, z) }
        )

    private fun sendMagnetometer(x: Float, y: Float, z: Float) =
        shadowSensors.sendSensorEventToListeners(
            ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_MAGNETIC_FIELD)
                .also { setValues(it, x, y, z) }
        )

    private fun setValues(event: android.hardware.SensorEvent, x: Float, y: Float, z: Float) {
        event.values[0] = x
        event.values[1] = y
        event.values[2] = z
    }

    private fun accelerometer(): Sensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)

    private fun magnetometer(): Sensor = ShadowSensor.newInstance(Sensor.TYPE_MAGNETIC_FIELD)
}
