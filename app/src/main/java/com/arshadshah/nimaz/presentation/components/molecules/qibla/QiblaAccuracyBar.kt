package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.foundation.tokens.accuracyVisuals
import com.arshadshah.nimaz.presentation.foundation.tokens.needsCalibration
import com.arshadshah.nimaz.presentation.theme.NimazTheme

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
        tone = if (calibrate) NimazTone.ERROR else NimazTone.MUTED,
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
