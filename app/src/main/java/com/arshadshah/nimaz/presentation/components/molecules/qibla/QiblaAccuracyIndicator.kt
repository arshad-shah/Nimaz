package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.theme.CompassArtColors
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/** Resolved presentation data for a [CompassAccuracy] level. */
private data class AccuracyVisuals(val label: String, val color: Color, val hint: String)

@Composable
private fun accuracyVisuals(accuracy: CompassAccuracy): AccuracyVisuals = when (accuracy) {
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
        MaterialTheme.colorScheme.error,
        stringResource(R.string.accuracy_low_hint)
    )

    CompassAccuracy.UNRELIABLE -> AccuracyVisuals(
        stringResource(R.string.accuracy_unreliable),
        MaterialTheme.colorScheme.error,
        stringResource(R.string.accuracy_unreliable_hint)
    )
}

private fun needsCalibration(accuracy: CompassAccuracy) =
    accuracy == CompassAccuracy.LOW || accuracy == CompassAccuracy.UNRELIABLE

/**
 * Full compass-accuracy card: label, segmented strength meter, hint and a
 * Calibrate action when accuracy is poor. Used in the themed compass view.
 */
@Composable
fun QiblaAccuracyBar(
    accuracy: CompassAccuracy,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val levels = listOf(
        CompassAccuracy.UNRELIABLE,
        CompassAccuracy.LOW,
        CompassAccuracy.MEDIUM,
        CompassAccuracy.HIGH
    )
    val activeIndex = levels.indexOf(accuracy)
    val (label, color, hint) = accuracyVisuals(accuracy)
    val calibrate = needsCalibration(accuracy)

    NimazCard(
        modifier = modifier,
        tone = if (calibrate) NimazCardTone.ERROR else NimazCardTone.MUTED,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.compass_accuracy),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                levels.forEachIndexed { index, _ ->
                    val segmentColor = if (index <= activeIndex) color
                    else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(segmentColor)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NimazIcon(
                    imageVector = if (calibrate) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    iconSize = 14.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (calibrate) {
                    NimazButton(
                        text = stringResource(R.string.calibrate),
                        onClick = onCalibrate,
                        variant = NimazButtonVariant.FILLED,
                        size = NimazButtonSize.SMALL
                    )
                }
            }
        }
    }
}

/**
 * Compact accuracy pill for the AR camera overlay: a dark translucent chip with
 * an accent icon + accuracy label. Mirrors [QiblaAccuracyBar] data in a smaller
 * footprint that reads over the camera feed.
 */
@Composable
fun QiblaAccuracyPill(
    accuracy: CompassAccuracy,
    modifier: Modifier = Modifier,
) {
    val (label, color, _) = accuracyVisuals(accuracy)
    val calibrate = needsCalibration(accuracy)
    val shape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier
            .clip(shape)
            .background(CompassArtColors.DialBackground.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazIcon(
            imageVector = if (calibrate) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = color,
            size = NimazIconSize.SMALL
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "· ${stringResource(R.string.compass_accuracy)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

// Previews

@Preview(showBackground = true, widthDp = 400, name = "Accuracy Bar - High")
@Composable
private fun AccuracyBarHighPreview() {
    NimazTheme {
        QiblaAccuracyBar(
            accuracy = CompassAccuracy.HIGH,
            onCalibrate = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Accuracy Bar - Low")
@Composable
private fun AccuracyBarLowPreview() {
    NimazTheme {
        QiblaAccuracyBar(
            accuracy = CompassAccuracy.LOW,
            onCalibrate = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Accuracy Bar - Unreliable")
@Composable
private fun AccuracyBarUnreliablePreview() {
    NimazTheme {
        QiblaAccuracyBar(
            accuracy = CompassAccuracy.UNRELIABLE,
            onCalibrate = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222222, name = "Accuracy Pill (AR)")
@Composable
private fun AccuracyPillPreview() {
    NimazTheme {
        QiblaAccuracyPill(accuracy = CompassAccuracy.HIGH, modifier = Modifier.padding(16.dp))
    }
}
