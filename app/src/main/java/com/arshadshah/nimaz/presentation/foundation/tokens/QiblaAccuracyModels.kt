package com.arshadshah.nimaz.presentation.foundation.tokens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.theme.NimazColors

/** Resolved presentation data for a [CompassAccuracy] level. */
data class AccuracyVisuals(val label: String, val color: Color, val hint: String)

@Composable
internal fun accuracyVisuals(accuracy: CompassAccuracy): AccuracyVisuals = when (accuracy) {
    CompassAccuracy.HIGH -> AccuracyVisuals(
        stringResource(R.string.accuracy_high),
        QiblaGreen,
        stringResource(R.string.accuracy_high_hint)
    )

    CompassAccuracy.MEDIUM -> AccuracyVisuals(
        stringResource(R.string.accuracy_medium),
        NimazColors.Gold400,
        stringResource(R.string.accuracy_medium_hint)
    )

    CompassAccuracy.LOW -> AccuracyVisuals(
        stringResource(R.string.accuracy_low),
        androidx.compose.material3.MaterialTheme.colorScheme.error,
        stringResource(R.string.accuracy_low_hint)
    )

    CompassAccuracy.UNRELIABLE -> AccuracyVisuals(
        stringResource(R.string.accuracy_unreliable),
        androidx.compose.material3.MaterialTheme.colorScheme.error,
        stringResource(R.string.accuracy_unreliable_hint)
    )
}

internal fun needsCalibration(accuracy: CompassAccuracy) =
    accuracy == CompassAccuracy.LOW || accuracy == CompassAccuracy.UNRELIABLE
