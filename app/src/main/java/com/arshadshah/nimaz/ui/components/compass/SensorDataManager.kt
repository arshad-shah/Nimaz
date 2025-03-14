package com.arshadshah.nimaz.ui.components.compass

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SensorDataManager(context: Context) : SensorEventListener {

    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    fun init(context: Context) {
        val packageManager = context.packageManager
        //check if device has sensors
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) && packageManager.hasSystemFeature(
                PackageManager.FEATURE_SENSOR_COMPASS
            )
        ) {
            Log.d("SensorDataManager", "init")
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        } else {

            Log.d("SensorDataManager", "No sensors")
            Toasty.error(context, "No sensors found on device", Toasty.LENGTH_LONG).show()
        }
    }

    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    val data: Channel<SensorData> = Channel(Channel.UNLIMITED)

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor === accelerometer) {
            lastAccelerometer = event!!.values.clone()
            lastAccelerometerSet = true
        } else if (event!!.sensor === magnetometer) {
            lastMagnetometer = event!!.values.clone()
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)

            if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
                val orientation = FloatArray(3)

                SensorManager.getOrientation(r, orientation)

                data.trySend(
                    SensorData(
                        roll = orientation[2],
                        pitch = orientation[1],
                        yaw = orientation[0],
                    )
                )

            } // inner if
        } // outerif
    } // end of onsensor changed

    fun cancel() {
        Log.d("SensorDataManager", "cancel")
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        Log.d("SensorDataManager", "onAccuracyChanged")
        Log.d("SensorDataManager", "sensor: $sensor")
        Log.d("SensorDataManager", "accuracy: $accuracy")
    }
}

data class SensorData(
    val roll: Float,
    val pitch: Float,
    val yaw: Float,
)


@Composable
fun rememberSensorData(
    context: Context,
    scope: CoroutineScope
): SensorData? {
    var sensorData by remember { mutableStateOf<SensorData?>(null) }

    DisposableEffect(Unit) {
        val dataManager = SensorDataManager(context)
        dataManager.init(context)

        val job = scope.launch {
            dataManager.data
                .receiveAsFlow()
                .collect { sensorData = it }
        }

        onDispose {
            dataManager.cancel()
            job.cancel()
        }
    }

    return sensorData
}