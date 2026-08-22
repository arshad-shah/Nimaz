package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.foundation.geometry.cardinalDirection
import com.arshadshah.nimaz.presentation.foundation.geometry.compassDegrees
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The primary instruction line above the compass dial. Large heading + small
 * subtitle that together tell the user exactly what to do right now.
 */
@Composable
fun QiblaInstructionRow(
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    currentAzimuth: Float,
    isCompassReady: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            !isCompassReady -> {
                Text(
                    text = stringResource(R.string.finding_direction),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            isFacingQibla -> {
                Text(
                    text = stringResource(R.string.facing_qibla),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = QiblaGreen,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.hold_this_direction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                val degrees = abs(rotationToQibla).roundToInt()
                val turnText = if (rotationToQibla > 0)
                    stringResource(R.string.turn_right_format, degrees)
                else
                    stringResource(R.string.turn_left_format, degrees)
                Text(
                    text = turnText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                // Normalised, not raw: `currentAzimuth` is the unwrapped dial value, which
                // runs negative and past 360 so the needle can animate across the seam. This
                // line printed it as it stood -- "You are facing -67 NW".
                val az = compassDegrees(currentAzimuth)
                val card = cardinalDirection(currentAzimuth)
                Text(
                    text = stringResource(R.string.you_are_facing_format, az, card),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Instruction - Seeking")
@Composable
private fun QiblaInstructionRowSeekingPreview() {
    NimazTheme {
        QiblaInstructionRow(
            isFacingQibla = false,
            rotationToQibla = 34f,
            currentAzimuth = 212f,
            isCompassReady = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Instruction - Facing")
@Composable
private fun QiblaInstructionRowFacingPreview() {
    NimazTheme {
        QiblaInstructionRow(
            isFacingQibla = true,
            rotationToQibla = 0f,
            currentAzimuth = 118f,
            isCompassReady = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Instruction - Not ready")
@Composable
private fun QiblaInstructionRowNotReadyPreview() {
    NimazTheme {
        QiblaInstructionRow(
            isFacingQibla = false,
            rotationToQibla = 0f,
            currentAzimuth = 0f,
            isCompassReady = false,
        )
    }
}
