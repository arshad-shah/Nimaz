package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.QiblaCalculator
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaBearingText
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaFacingLabel
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGold
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaTurnHint
import com.arshadshah.nimaz.presentation.theme.CompassArtColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.roundToInt

/**
 * The single bearing/status element shared by both the compass and AR views.
 *
 * It adapts to three situations:
 *  - **Facing Qibla** → solid green pill, "Facing Qibla · 119° SE"
 *  - **Seeking** (compass ready, not yet aligned) → gold pill with a turn arrow,
 *    "↻ Turn right 12° · 119° SE"
 *  - **Not ready** → just the bearing, "119° SE"
 *
 * Set [onCamera] when drawn over the AR camera feed: it swaps the light themed
 * surface for a dark translucent one so the text stays legible.
 */
@Composable
fun QiblaStatusCapsule(
    qiblaBearing: Int,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    isCompassReady: Boolean,
    modifier: Modifier = Modifier,
    onCamera: Boolean = false,
) {
    val cardinal = QiblaCalculator.getCardinalDirection(qiblaBearing.toDouble())
    val shape = RoundedCornerShape(percent = 50)

    val containerModifier = when {
        isFacingQibla -> Modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(NimazPalette.Green600, QiblaGreen)))

        onCamera -> Modifier
            .clip(shape)
            .background(CompassArtColors.GoldCapsuleBackground.copy(alpha = 0.82f))
            .border(1.dp, QiblaGold.copy(alpha = 0.4f), shape)

        else -> Modifier
            .clip(shape)
            .background(QiblaGold.copy(alpha = 0.12f))
            .border(1.dp, QiblaGold.copy(alpha = 0.4f), shape)
    }

    val bearingColor = when {
        isFacingQibla || onCamera -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val hintColor = when {
        isFacingQibla -> Color.White
        onCamera -> Color.White.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dividerColor = if (isFacingQibla || onCamera) Color.White.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    val showLeft = isFacingQibla || isCompassReady
    val turnRight = rotationToQibla > 0

    Box(
        modifier = modifier
            .then(containerModifier)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showLeft) {
                if (isFacingQibla) {
                    QiblaFacingLabel(color = hintColor)
                } else {
                    QiblaTurnHint(
                        turnRight = turnRight,
                        degrees = rotationToQibla.roundToInt(),
                        color = hintColor,
                    )
                }

                Spacer(Modifier.width(13.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(dividerColor)
                )
                Spacer(Modifier.width(13.dp))
            }

            QiblaBearingText(
                bearing = qiblaBearing,
                cardinal = cardinal,
                color = bearingColor,
            )
        }
    }
}

// Previews

@Preview(showBackground = true, name = "Capsule - Seeking")
@Composable
private fun CapsuleSeekingPreview() {
    NimazTheme {
        QiblaStatusCapsule(
            qiblaBearing = 119,
            isFacingQibla = false,
            rotationToQibla = 12f,
            isCompassReady = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Capsule - Facing")
@Composable
private fun CapsuleFacingPreview() {
    NimazTheme {
        QiblaStatusCapsule(
            qiblaBearing = 119,
            isFacingQibla = true,
            rotationToQibla = 0f,
            isCompassReady = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Capsule - Not ready")
@Composable
private fun CapsuleNotReadyPreview() {
    NimazTheme {
        QiblaStatusCapsule(
            qiblaBearing = 119,
            isFacingQibla = false,
            rotationToQibla = 0f,
            isCompassReady = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF222222,
    name = "Capsule - On camera (seeking)"
)
@Composable
private fun CapsuleOnCameraPreview() {
    NimazTheme {
        QiblaStatusCapsule(
            qiblaBearing = 119,
            isFacingQibla = false,
            rotationToQibla = -8f,
            isCompassReady = true,
            onCamera = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
