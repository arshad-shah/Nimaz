package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.foundation.tokens.accuracyVisuals
import com.arshadshah.nimaz.presentation.foundation.tokens.needsCalibration
import com.arshadshah.nimaz.presentation.theme.CompassArtColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

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

@Preview(showBackground = true, backgroundColor = 0xFF222222, name = "Accuracy Pill (AR)")
@Composable
private fun AccuracyPillPreview() {
    NimazTheme {
        QiblaAccuracyPill(accuracy = CompassAccuracy.HIGH, modifier = Modifier.padding(16.dp))
    }
}
