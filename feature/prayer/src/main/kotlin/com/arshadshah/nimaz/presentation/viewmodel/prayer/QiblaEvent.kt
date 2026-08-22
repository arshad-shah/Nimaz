package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.Location

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
    data class SetArMode(val enabled: Boolean) : QiblaEvent
}
