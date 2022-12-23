package com.arshadshah.nimaz.ui.components.bLogic.compass

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.channels.Channel

class SensorDataManager(context: Context): SensorEventListener
{
	private lateinit var accelerometer: Sensor
	private lateinit var magnetometer: Sensor
	private val sensorManager by lazy {
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	}

	fun init() {
		Log.d("SensorDataManager" , "init")
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
		sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
	}

	private var currentDegree = 0.0f
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

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

data class SensorData(
	val roll: Float,
	val pitch: Float,
	val yaw: Float,
					 )