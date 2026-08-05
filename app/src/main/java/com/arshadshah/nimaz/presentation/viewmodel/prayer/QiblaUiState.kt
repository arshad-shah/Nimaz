package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.domain.model.CompassData
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.QiblaDirection
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.presentation.viewmodel.UiError

data class QiblaUiState(
    val qiblaDirection: QiblaDirection? = null,
    val qiblaInfo: QiblaInfo? = null,
    val compassData: CompassData = CompassData(),
    val currentLocation: Location? = null,
    val isCompassReady: Boolean = false,
    val needsCalibration: Boolean = false,
    val isFacingQibla: Boolean = false,
    val rotationToQibla: Float = 0f,
    /**
     * Local magnetic declination in degrees, east-positive, for the current location.
     *
     * The qibla bearing is measured from true north and the magnetometer from magnetic north;
     * this is the difference between them. Zero until a location is known.
     */
    val magneticDeclination: Float = 0f,
    /** Cumulative unwrapped azimuth — use for smooth rotation animation */
    val animatedAzimuth: Float = 0f,
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val showLocationPicker: Boolean = false,
    val showCalibrationDialog: Boolean = false,
    val isArMode: Boolean = false
)

data class QiblaSettingsUiState(
    val trueNorthMode: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val qiblaThreshold: Float = 5f
)
