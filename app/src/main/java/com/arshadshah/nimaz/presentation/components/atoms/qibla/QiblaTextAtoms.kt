package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.abs

/**
 * Small text atoms shared by the Qibla screens. [QiblaLocationLabel] is used in
 * the top bar; [CalibrationStep] inside the calibration dialog.
 */

/** Formats a coordinate pair like "51.5074°N, 0.1278°W". */
fun formatCoordinates(latitude: Double, longitude: Double): String {
    val ns = if (latitude >= 0) "N" else "S"
    val ew = if (longitude >= 0) "E" else "W"
    return "${String.format("%.4f", abs(latitude))}°$ns, ${
        String.format(
            "%.4f",
            abs(longitude)
        )
    }°$ew"
}

/**
 * Location name + coordinates, stacked. Shown in the Qibla top bar.
 * Falls back to [fallbackTitle] when no location is available yet.
 */
@Composable
fun QiblaLocationLabel(
    locationName: String?,
    latitude: Double?,
    longitude: Double?,
    fallbackTitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = locationName?.takeIf { it.isNotBlank() } ?: fallbackTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (latitude != null && longitude != null) {
            Text(
                text = formatCoordinates(latitude, longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A numbered step row used in the calibration instructions. */
@Composable
fun CalibrationStep(
    number: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Previews

@Preview(showBackground = true, widthDp = 260, name = "Location Label")
@Composable
private fun QiblaLocationLabelPreview() {
    NimazTheme {
        QiblaLocationLabel(
            locationName = "London, UK",
            latitude = 51.5074,
            longitude = -0.1278,
            fallbackTitle = "Qibla",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 260, name = "Location Label - Fallback")
@Composable
private fun QiblaLocationLabelFallbackPreview() {
    NimazTheme {
        QiblaLocationLabel(
            locationName = null,
            latitude = null,
            longitude = null,
            fallbackTitle = "Qibla",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Calibration Step")
@Composable
private fun CalibrationStepPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalibrationStep("1", "Hold your phone away from metal objects and magnets")
            CalibrationStep("2", "Slowly move your phone in a figure-8 pattern")
        }
    }
}
