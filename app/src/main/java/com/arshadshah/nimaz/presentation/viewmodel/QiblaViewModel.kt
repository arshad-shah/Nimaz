package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.CompassData
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.QiblaCalculator
import com.arshadshah.nimaz.domain.model.QiblaDirection
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isLoading: Boolean = true,
    val error: String? = null,
    val showLocationPicker: Boolean = false
)

data class QiblaSettingsUiState(
    val trueNorthMode: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val qiblaThreshold: Float = 5f // Degrees tolerance for "facing Qibla"
)

sealed interface QiblaEvent {
    data class UpdateCompassData(val azimuth: Float, val pitch: Float, val roll: Float) : QiblaEvent
    data class UpdateAccuracy(val accuracy: CompassAccuracy) : QiblaEvent
    data class SetLocation(val location: Location) : QiblaEvent
    data class SetTrueNorthMode(val enabled: Boolean) : QiblaEvent
    data class SetVibrationEnabled(val enabled: Boolean) : QiblaEvent
    data class SetSoundEnabled(val enabled: Boolean) : QiblaEvent
    data class SetQiblaThreshold(val threshold: Float) : QiblaEvent
    data object RefreshLocation : QiblaEvent
    data object ShowLocationPicker : QiblaEvent
    data object HideLocationPicker : QiblaEvent
    data object StartCompass : QiblaEvent
    data object StopCompass : QiblaEvent
}

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository
) : ViewModel() {

    private val _qiblaState = MutableStateFlow(QiblaUiState())
    val qiblaState: StateFlow<QiblaUiState> = _qiblaState.asStateFlow()

    private val _settingsState = MutableStateFlow(QiblaSettingsUiState())
    val settingsState: StateFlow<QiblaSettingsUiState> = _settingsState.asStateFlow()

    init {
        loadCurrentLocation()
    }

    fun onEvent(event: QiblaEvent) {
        when (event) {
            is QiblaEvent.UpdateCompassData -> updateCompassData(event.azimuth, event.pitch, event.roll)
            is QiblaEvent.UpdateAccuracy -> updateAccuracy(event.accuracy)
            is QiblaEvent.SetLocation -> setLocation(event.location)
            is QiblaEvent.SetTrueNorthMode -> _settingsState.update { it.copy(trueNorthMode = event.enabled) }
            is QiblaEvent.SetVibrationEnabled -> _settingsState.update { it.copy(vibrationEnabled = event.enabled) }
            is QiblaEvent.SetSoundEnabled -> _settingsState.update { it.copy(soundEnabled = event.enabled) }
            is QiblaEvent.SetQiblaThreshold -> _settingsState.update { it.copy(qiblaThreshold = event.threshold) }
            QiblaEvent.RefreshLocation -> loadCurrentLocation()
            QiblaEvent.ShowLocationPicker -> _qiblaState.update { it.copy(showLocationPicker = true) }
            QiblaEvent.HideLocationPicker -> _qiblaState.update { it.copy(showLocationPicker = false) }
            QiblaEvent.StartCompass -> startCompass()
            QiblaEvent.StopCompass -> stopCompass()
        }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            prayerRepository.getCurrentLocation().collect { location ->
                location?.let {
                    setLocation(it)
                }
            }
        }
    }

    private fun setLocation(location: Location) {
        _qiblaState.update { it.copy(currentLocation = location, isLoading = true) }

        viewModelScope.launch {
            try {
                // Calculate Qibla direction
                val qiblaDirection = QiblaCalculator.calculateQiblaDirection(
                    location.latitude,
                    location.longitude
                )

                val qiblaInfo = QiblaInfo(
                    direction = qiblaDirection,
                    locationName = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    distanceToMecca = QiblaCalculator.calculateDistanceToMecca(
                        location.latitude,
                        location.longitude
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
                _qiblaState.update {
                    it.copy(error = e.message, isLoading = false)
                }
            }
        }
    }

    private fun updateCompassData(azimuth: Float, pitch: Float, roll: Float) {
        val qiblaDirection = _qiblaState.value.qiblaDirection ?: return
        val threshold = _settingsState.value.qiblaThreshold

        // Normalize azimuth to 0-360
        val normalizedAzimuth = (azimuth + 360) % 360

        // Calculate rotation needed to face Qibla
        val qiblaBearing = qiblaDirection.bearing.toFloat()
        var rotationToQibla = qiblaBearing - normalizedAzimuth

        // Normalize to -180 to 180
        if (rotationToQibla > 180) rotationToQibla -= 360
        if (rotationToQibla < -180) rotationToQibla += 360

        // Check if facing Qibla within threshold
        val isFacingQibla = abs(rotationToQibla) <= threshold

        val compassData = CompassData(
            azimuth = normalizedAzimuth,
            pitch = pitch,
            roll = roll,
            accuracy = _qiblaState.value.compassData.accuracy,
            timestamp = System.currentTimeMillis()
        )

        _qiblaState.update {
            it.copy(
                compassData = compassData,
                rotationToQibla = rotationToQibla,
                isFacingQibla = isFacingQibla,
                isCompassReady = true
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

    private fun startCompass() {
        _qiblaState.update { it.copy(isCompassReady = false) }
        // The actual compass sensor registration would be handled by the UI layer
        // This just updates the state to indicate compass is starting
    }

    private fun stopCompass() {
        _qiblaState.update { it.copy(isCompassReady = false) }
        // The actual compass sensor unregistration would be handled by the UI layer
    }
}
