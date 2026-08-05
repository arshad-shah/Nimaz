package com.arshadshah.nimaz.data.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.repository.CompassOrientation
import com.arshadshah.nimaz.domain.repository.CompassSensors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Low-pass coefficient for the accelerometer and magnetometer vectors. */
private const val SMOOTHING = 0.97f

/**
 * Smooths [sample] into [filtered] in place.
 *
 * With [seed] the sample is taken at face value. An unseeded filter starts at zero and needs
 * roughly a hundred samples to converge, which is what used to publish `isCompassReady` on the
 * first successful `getRotationMatrix` — the screen said ready and the needle then swept in
 * from a meaningless heading. One sample is a worse estimate than a hundred, but it is an
 * estimate *of the right thing*, which zero is not.
 */
internal fun smoothInto(filtered: FloatArray, sample: FloatArray, seed: Boolean) {
    for (i in filtered.indices) {
        filtered[i] =
            if (seed) sample[i]
            else SMOOTHING * filtered[i] + (1 - SMOOTHING) * sample[i]
    }
}

@Singleton
class AndroidCompassSensors @Inject constructor(
    @ApplicationContext private val context: Context
) : CompassSensors {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val magnetometer: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    override val isAvailable: Boolean
        get() = accelerometer != null && magnetometer != null

    override fun orientation(): Flow<CompassOrientation> = callbackFlow {
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnetic = false
        var accuracy = CompassAccuracy.UNRELIABLE

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        smoothInto(gravity, event.values, seed = !hasGravity)
                        hasGravity = true
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        smoothInto(geomagnetic, event.values, seed = !hasMagnetic)
                        hasMagnetic = true
                    }
                }
                if (!hasGravity || !hasMagnetic) return

                val rotation = FloatArray(9)
                val inclination = FloatArray(9)
                if (!SensorManager.getRotationMatrix(rotation, inclination, gravity, geomagnetic)) {
                    return
                }
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotation, orientation)
                trySend(
                    CompassOrientation(
                        azimuthDegrees =
                            ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360) % 360),
                        pitchDegrees = Math.toDegrees(orientation[1].toDouble()).toFloat(),
                        rollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat(),
                        accuracy = accuracy,
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor, value: Int) {
                if (sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
                accuracy = when (value) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
                    else -> CompassAccuracy.UNRELIABLE
                }
            }
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // The listener's lifetime is the collection's. Cancelling the collect unregisters,
        // which is what the ViewModel's onCleared() used to have to remember to do.
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
