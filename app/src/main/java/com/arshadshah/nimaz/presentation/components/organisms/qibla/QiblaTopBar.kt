package com.arshadshah.nimaz.presentation.components.organisms.qibla

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Qibla screen top bar. Shows the screen title, active location + accuracy
 * status dot, a camera-mode toggle icon, and a calibrate icon. The segmented
 * Compass / AR tab has been replaced by the camera icon so the bar stays compact
 * and identical across modes.
 */
@Composable
fun QiblaTopBar(
    locationName: String?,
    accuracy: CompassAccuracy,
    isArMode: Boolean,
    onCameraToggle: () -> Unit,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accuracyDotColor = when (accuracy) {
        CompassAccuracy.UNRELIABLE, CompassAccuracy.LOW -> MaterialTheme.colorScheme.error
        CompassAccuracy.MEDIUM -> MaterialTheme.colorScheme.secondary
        CompassAccuracy.HIGH -> QiblaGreen
    }
    val accuracyLabel = accuracy.name.lowercase().replaceFirstChar { it.uppercase() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Title + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.guide_qibla_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (!locationName.isNullOrBlank()) {
                            Text(
                                text = locationName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accuracyDotColor)
                        )
                        Text(
                            text = accuracyLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Camera toggle — highlighted when AR mode is active
                NimazIconButton(
                    icon = Icons.Default.CameraAlt,
                    contentDescription = stringResource(
                        if (isArMode) R.string.back_to_compass else R.string.point_with_camera
                    ),
                    onClick = onCameraToggle,
                    modifier = if (isArMode)
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    else Modifier,
                )

                // Calibrate
                NimazIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.calibrate_compass),
                    onClick = onCalibrate,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Qibla Top Bar - High Accuracy")
@Composable
private fun QiblaTopBarHighPreview() {
    NimazTheme {
        QiblaTopBar(
            locationName = "Abbeyleix, Co. Laois",
            accuracy = CompassAccuracy.HIGH,
            isArMode = false,
            onCameraToggle = {},
            onCalibrate = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Qibla Top Bar - AR Mode + Low Accuracy")
@Composable
private fun QiblaTopBarArLowPreview() {
    NimazTheme {
        QiblaTopBar(
            locationName = "London, UK",
            accuracy = CompassAccuracy.LOW,
            isArMode = true,
            onCameraToggle = {},
            onCalibrate = {},
        )
    }
}
