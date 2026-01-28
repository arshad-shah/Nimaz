package com.arshadshah.nimaz.presentation.viewmodel

import android.content.Context
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
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.CompassData
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.QiblaCalculator
import com.arshadshah.nimaz.domain.model.QiblaDirection
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class QiblaUiState(
    val qiblaDirection: QiblaDirection? = null,
    val qiblaInfo: QiblaInfo? = null,
    val compassData: CompassData = CompassData(),
    val currentLocation: Location? = null,
    val isCompassReady: Boolean = false,
    val needsCalibration: Boolean = false,
    val isFacingQibla: Boolean = false,
    val rotationToQibla: Float = 0f,
    /** Cumulative unwrapped azimuth — use for smooth rotation animation */
    val animatedAzimuth: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showLocationPicker: Boolean = false,
    val showCalibrationDialog: Boolean = false
)

data class QiblaSettingsUiState(
    val trueNorthMode: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val qiblaThreshold: Float = 5f
)

sealed interface QiblaEvent {
    data class UpdateAccuracy(val accuracy: CompassAccuracy) : QiblaEvent
    data class SetLocation(val location: Location) : QiblaEvent
    data class SetTrueNorthMode(val enabled: Boolean) : QiblaEvent
    data class SetVibrationEnabled(val enabled: Boolean) : QiblaEvent
    data class SetSoundEnabled(val enabled: Boolean) : QiblaEvent
    data class SetQiblaThreshold(val threshold: Float) : QiblaEvent
    data object RefreshLocation : QiblaEvent
    data object ShowLocationPicker : QiblaEvent
    data object HideLocationPicker : QiblaEvent
    data object ShowCalibrationDialog : QiblaEvent
    data object DismissCalibrationDialog : QiblaEvent
    data object StartCompass : QiblaEvent
    data object StopCompass : QiblaEvent
}

@HiltViewModel
class QiblaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerRepository: PrayerRepository,
    private val preferencesDataStore: PreferencesDataStore
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
            val alpha = 0.97f
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                    hasGravity = true
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                    geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                    geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
                    hasMagnetic = true
                }
            }

            if (hasGravity && hasMagnetic) {
                val rotationMatrix = FloatArray(9)
                val inclinationMatrix = FloatArray(9)
                if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthDeg = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360) % 360)
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
        loadCurrentLocation()
        registerSensors()
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
            QiblaEvent.RefreshLocation -> loadCurrentLocation()
            QiblaEvent.ShowLocationPicker -> _qiblaState.update { it.copy(showLocationPicker = true) }
            QiblaEvent.HideLocationPicker -> _qiblaState.update { it.copy(showLocationPicker = false) }
            QiblaEvent.ShowCalibrationDialog -> _qiblaState.update { it.copy(showCalibrationDialog = true) }
            QiblaEvent.DismissCalibrationDialog -> _qiblaState.update { it.copy(showCalibrationDialog = false) }
            QiblaEvent.StartCompass -> {
                resetSensorState()
                registerSensors()
            }
            QiblaEvent.StopCompass -> unregisterSensors()
        }
    }

    private fun resetSensorState() {
        gravity.fill(0f)
        geomagnetic.fill(0f)
        hasGravity = false
        hasMagnetic = false
        prevRawAzimuth = 0f
        cumulativeAzimuth = 0f
        _qiblaState.update { it.copy(isCompassReady = false, animatedAzimuth = 0f) }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            // Try DB location first
            val dbLocation = prayerRepository.getCurrentLocationSync()
            if (dbLocation != null) {
                setLocation(dbLocation)
            } else {
                // Fall back to DataStore lat/lng
                val lat = preferencesDataStore.latitude.first()
                val lng = preferencesDataStore.longitude.first()
                val name = preferencesDataStore.locationName.first()
                if (lat != 0.0 || lng != 0.0) {
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

    private fun setLocationFromCoords(latitude: Double, longitude: Double, locationName: String) {
        viewModelScope.launch {
            try {
                val qiblaDirection = QiblaCalculator.calculateQiblaDirection(latitude, longitude)
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
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _qiblaState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun setLocation(location: Location) {
        _qiblaState.update { it.copy(currentLocation = location, isLoading = true) }

        viewModelScope.launch {
            try {
                val qiblaDirection = QiblaCalculator.calculateQiblaDirection(
                    location.latitude, location.longitude
                )
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
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _qiblaState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun updateCompassData(azimuth: Float, pitch: Float, roll: Float, unwrappedAzimuth: Float) {
        val normalizedAzimuth = (azimuth + 360) % 360

        val compassData = CompassData(
            azimuth = normalizedAzimuth,
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
                    animatedAzimuth = unwrappedAzimuth
                )
            }
            return
        }

        val threshold = _settingsState.value.qiblaThreshold
        val qiblaBearing = qiblaDirection.bearing.toFloat()
        var rotationToQibla = qiblaBearing - normalizedAzimuth
        if (rotationToQibla > 180) rotationToQibla -= 360
        if (rotationToQibla < -180) rotationToQibla += 360

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
                animatedAzimuth = unwrappedAzimuth
            )
        }
    }

    private fun updateAccuracy(accuracy: CompassAccuracy) {
        val needsCalibration = accuracy == CompassAccuracy.LOW || accuracy == CompassAccuracy.UNRELIABLE
        _qiblaState.update {
            it.copy(
                compassData = it.compassData.copy(accuracy = accuracy),
                needsCalibration = needsCalibration
            )
        }
    }

    private fun triggerHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }
}
