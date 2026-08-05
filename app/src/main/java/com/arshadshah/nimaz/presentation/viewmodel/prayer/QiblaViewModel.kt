package com.arshadshah.nimaz.presentation.viewmodel.prayer

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.CompassData
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.QiblaCalculator
import com.arshadshah.nimaz.domain.model.QiblaDirection
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.domain.model.isLocationSet
import com.arshadshah.nimaz.domain.repository.settings.LocationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class QiblaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationSettings: LocationSettings,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _qiblaState = MutableStateFlow(QiblaUiState())
    val qiblaState: StateFlow<QiblaUiState> = _qiblaState.asStateFlow()

    private val _settingsState = MutableStateFlow(QiblaSettingsUiState())
    val settingsState: StateFlow<QiblaSettingsUiState> = _settingsState.asStateFlow()

    // Sensor management
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Low-pass filtered sensor arrays
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false

    // Azimuth unwrapping state
    private var prevRawAzimuth = 0f
    private var cumulativeAzimuth = 0f

    // Track previous facing state for haptic
    private var wasFacingQibla = false

    private val sensorListener = object : SensorEventListener {
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

            if (hasGravity && hasMagnetic) {
                val rotationMatrix = FloatArray(9)
                val inclinationMatrix = FloatArray(9)
                if (SensorManager.getRotationMatrix(
                        rotationMatrix,
                        inclinationMatrix,
                        gravity,
                        geomagnetic
                    )
                ) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthDeg =
                        ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360) % 360)
                    val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

                    // Unwrap azimuth to avoid 360->0 snap
                    var delta = azimuthDeg - prevRawAzimuth
                    if (delta > 180) delta -= 360
                    if (delta < -180) delta += 360
                    prevRawAzimuth = azimuthDeg
                    cumulativeAzimuth += delta

                    updateCompassData(azimuthDeg, pitchDeg, rollDeg, cumulativeAzimuth)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                val compassAccuracy = when (accuracy) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
                    else -> CompassAccuracy.UNRELIABLE
                }
                updateAccuracy(compassAccuracy)
            }
        }
    }

    init {
        observeLocation()
    }

    override fun onCleared() {
        super.onCleared()
        unregisterSensors()
    }

    private fun registerSensors() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(sensorListener)
    }

    fun onEvent(event: QiblaEvent) {
        when (event) {
            is QiblaEvent.UpdateAccuracy -> updateAccuracy(event.accuracy)
            is QiblaEvent.SetLocation -> setLocation(event.location)
            is QiblaEvent.SetTrueNorthMode -> _settingsState.update { it.copy(trueNorthMode = event.enabled) }
            is QiblaEvent.SetVibrationEnabled -> _settingsState.update { it.copy(vibrationEnabled = event.enabled) }
            is QiblaEvent.SetSoundEnabled -> _settingsState.update { it.copy(soundEnabled = event.enabled) }
            is QiblaEvent.SetQiblaThreshold -> _settingsState.update { it.copy(qiblaThreshold = event.threshold) }
            QiblaEvent.RefreshLocation -> { /* Location is observed reactively */
            }

            QiblaEvent.ShowLocationPicker -> _qiblaState.update { it.copy(showLocationPicker = true) }
            QiblaEvent.HideLocationPicker -> _qiblaState.update { it.copy(showLocationPicker = false) }
            QiblaEvent.ShowCalibrationDialog -> _qiblaState.update { it.copy(showCalibrationDialog = true) }
            QiblaEvent.DismissCalibrationDialog -> _qiblaState.update {
                it.copy(
                    showCalibrationDialog = false
                )
            }

            QiblaEvent.StartCompass -> {
                telemetry.featureUsed(AppAnalytics.Feature.QIBLA, "start_compass")
                resetSensorState()
                registerSensors()
            }

            QiblaEvent.StopCompass -> unregisterSensors()
            is QiblaEvent.SetArMode -> {
                telemetry.featureUsed(
                    AppAnalytics.Feature.QIBLA,
                    if (event.enabled) "ar_on" else "ar_off"
                )
                _qiblaState.update { it.copy(isArMode = event.enabled) }
            }
        }
    }

    /**
     * The last accuracy reported to analytics, so only changes are recorded. Deliberately not
     * reset by [resetSensorState]: re-entering the screen with the same uncalibrated
     * magnetometer is the same fact, not a new one.
     */
    private var lastReportedAccuracy: CompassAccuracy? = null

    private fun resetSensorState() {
        gravity.fill(0f)
        geomagnetic.fill(0f)
        hasGravity = false
        hasMagnetic = false
        prevRawAzimuth = 0f
        cumulativeAzimuth = 0f
        // Reset with the rest of the sensor state. Left set, the rising-edge test below never
        // fired for a user who left the screen facing the qibla and came back still facing it —
        // no confirmation haptic until they turned away and back again.
        wasFacingQibla = false
        _qiblaState.update { it.copy(isCompassReady = false, animatedAzimuth = 0f) }
    }

    private fun observeLocation() {
        launchSafely(telemetry, AppAnalytics.Feature.QIBLA, "observe_location") {
            combine(
                locationSettings.latitude,
                locationSettings.longitude,
                locationSettings.locationName
            ) { lat, lng, name ->
                Triple(lat, lng, name)
            }.collect { (lat, lng, name) ->
                if (isLocationSet(lat, lng)) {
                    setLocationFromCoords(lat, lng, name.ifEmpty { "Current Location" })
                } else {
                    _qiblaState.update {
                        it.copy(
                            error = "No location set. Please set your location in settings.",
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /**
     * The local magnetic declination, east-positive, from the World Magnetic Model Android
     * ships. Altitude is passed as 0: the field varies by well under a tenth of a degree over
     * any altitude a phone reaches, and the location source has no elevation.
     */
    private fun declinationAt(latitude: Double, longitude: Double): Float =
        runCatching {
            GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination
        }.getOrDefault(0f)

    private fun setLocationFromCoords(latitude: Double, longitude: Double, locationName: String) {
        launchSafely(telemetry, AppAnalytics.Feature.QIBLA, "set_location_from_coords") {
            try {
                val qiblaDirection = QiblaCalculator.calculateQiblaDirection(latitude, longitude)
                val declination = declinationAt(latitude, longitude)
                val qiblaInfo = QiblaInfo(
                    direction = qiblaDirection,
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    distanceToMecca = QiblaCalculator.calculateDistanceToMecca(latitude, longitude)
                )
                _qiblaState.update {
                    it.copy(
                        qiblaDirection = qiblaDirection,
                        qiblaInfo = qiblaInfo,
                        magneticDeclination = declination,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                telemetry.failure(AppAnalytics.Feature.QIBLA, "calculate_direction", e)
                _qiblaState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun setLocation(location: Location) {
        _qiblaState.update { it.copy(currentLocation = location, isLoading = true) }

        launchSafely(
            telemetry,
            AppAnalytics.Feature.QIBLA,
            "set_location",
            onFailure = { _qiblaState.update { it.copy(isLoading = false) } },
        ) {
            try {
                val qiblaDirection = QiblaCalculator.calculateQiblaDirection(
                    location.latitude, location.longitude
                )
                val declination = declinationAt(location.latitude, location.longitude)
                val qiblaInfo = QiblaInfo(
                    direction = qiblaDirection,
                    locationName = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    distanceToMecca = QiblaCalculator.calculateDistanceToMecca(
                        location.latitude, location.longitude
                    )
                )
                _qiblaState.update {
                    it.copy(
                        qiblaDirection = qiblaDirection,
                        qiblaInfo = qiblaInfo,
                        magneticDeclination = declination,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                telemetry.failure(AppAnalytics.Feature.QIBLA, "set_location", e)
                _qiblaState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun updateCompassData(
        azimuth: Float,
        pitch: Float,
        roll: Float,
        unwrappedAzimuth: Float
    ) {
        val normalizedAzimuth = (azimuth + 360) % 360

        // The magnetometer reads from magnetic north; the qibla bearing is from true north.
        // Correcting here rather than only in `rotationToQibla` is what makes the *drawn*
        // needle right: `QiblaCompassWidget` computes its screen angles straight off
        // `animatedAzimuth` (`qiblaBearing - animatedAzimuth`, `-animatedAzimuth`), so an
        // uncorrected azimuth points the qibla needle — and the "N" needle — at magnetic north.
        // With `trueNorthMode` off the reading is left raw, which is what a paper compass held
        // next to the phone would show.
        val declination = if (_settingsState.value.trueNorthMode) {
            _qiblaState.value.magneticDeclination
        } else {
            0f
        }
        val headingFromTrueNorth = QiblaCalculator.trueAzimuth(normalizedAzimuth, declination)
        // The unwrap runs on deltas, so shifting the accumulated value by a constant keeps it
        // continuous — no 360° snap is reintroduced.
        val unwrappedFromTrueNorth = unwrappedAzimuth + declination

        val compassData = CompassData(
            azimuth = headingFromTrueNorth,
            pitch = pitch,
            roll = roll,
            accuracy = _qiblaState.value.compassData.accuracy,
            timestamp = System.currentTimeMillis()
        )

        val qiblaDirection = _qiblaState.value.qiblaDirection
        if (qiblaDirection == null) {
            // Still update compass rotation even without qibla direction
            _qiblaState.update {
                it.copy(
                    compassData = compassData,
                    isCompassReady = true,
                    animatedAzimuth = unwrappedFromTrueNorth
                )
            }
            return
        }

        val threshold = _settingsState.value.qiblaThreshold
        val qiblaBearing = qiblaDirection.bearing.toFloat()
        val rotationToQibla =
            QiblaCalculator.rotationToQibla(qiblaBearing, normalizedAzimuth, declination)

        val isFacingQibla = abs(rotationToQibla) <= threshold

        if (isFacingQibla && !wasFacingQibla && _settingsState.value.vibrationEnabled) {
            triggerHaptic()
        }
        wasFacingQibla = isFacingQibla

        _qiblaState.update {
            it.copy(
                compassData = compassData,
                rotationToQibla = rotationToQibla,
                isFacingQibla = isFacingQibla,
                isCompassReady = true,
                animatedAzimuth = unwrappedFromTrueNorth
            )
        }
    }

    /**
     * Track the magnetometer's reported accuracy, and report each **transition**.
     *
     * "How many users have an uncalibrated magnetometer" is exactly the population-level
     * question `AppAnalytics`' KDoc describes, and it was answerable from nothing: the compass
     * shows a calibration prompt and the app had no idea how often. It matters more here than
     * a usual usage counter, because a qibla needle drawn from an unreliable sensor is
     * confidently wrong rather than visibly broken.
     *
     * Transitions only. `SensorManager` re-delivers the same accuracy on every reading, so
     * logging each callback would emit tens of events a second for as long as the screen is
     * open — the firehose problem, at sensor rate.
     */
    private fun updateAccuracy(accuracy: CompassAccuracy) {
        val needsCalibration =
            accuracy == CompassAccuracy.LOW || accuracy == CompassAccuracy.UNRELIABLE
        if (accuracy != lastReportedAccuracy) {
            lastReportedAccuracy = accuracy
            telemetry.featureUsed(
                AppAnalytics.Feature.QIBLA,
                "accuracy_" + accuracy.name.lowercase(),
            )
        }
        _qiblaState.update {
            it.copy(
                compassData = it.compassData.copy(accuracy = accuracy),
                needsCalibration = needsCalibration
            )
        }
    }

    private fun triggerHaptic() {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

/** Low-pass coefficient for the accelerometer and magnetometer vectors. */
private const val SMOOTHING = 0.97f

/**
 * Low-pass filter, seeded from the first sample.
 *
 * The vectors start as all-zero and `resetSensorState()` re-zeroes them on every
 * `StartCompass`, while `SMOOTHING` is 0.97 — so the first sample contributed **3%** of its
 * value and the filtered vector needed ~100 samples to reach 95% of the truth. At
 * `SENSOR_DELAY_GAME` (~20 ms) that is **about two seconds**, and `isCompassReady` is
 * published on the *first* successful `getRotationMatrix`. So the screen said ready and the
 * needle swept in from a meaningless heading — every time, because the screen's
 * `DisposableEffect` stops and starts the compass on each entry.
 *
 * Seeding costs nothing: one sample is a worse estimate than a hundred, but it is an
 * estimate *of the right thing*, which zero is not.
 */
internal fun smoothInto(filtered: FloatArray, sample: FloatArray, seed: Boolean) {
    for (i in filtered.indices) {
        filtered[i] =
            if (seed) sample[i]
            else SMOOTHING * filtered[i] + (1 - SMOOTHING) * sample[i]
    }
}
