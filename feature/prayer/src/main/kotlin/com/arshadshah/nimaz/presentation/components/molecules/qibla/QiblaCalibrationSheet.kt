package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CalibrationStep
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bottom sheet for compass calibration: an animated figure-8 gesture guide,
 * a live accuracy meter, numbered steps, and a close button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCalibrationSheet(
    accuracy: CompassAccuracy,
    onDismiss: () -> Unit,
) {
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.calibrate_compass),
        subtitle = stringResource(R.string.calibration_sheet_subtitle),
        icon = Icons.Default.Refresh,
        onClose = onDismiss,
        footer = {
            NimazButton(
                text = stringResource(R.string.got_it),
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    ) {
        // Figure-8 animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Figure8Animation(
                modifier = Modifier.size(180.dp, 96.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Accuracy meter
        AccuracyMeter(accuracy = accuracy)

        Spacer(Modifier.height(4.dp))

        // Current accuracy label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.accuracy_now_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val accuracyColor = when (accuracy) {
                CompassAccuracy.UNRELIABLE, CompassAccuracy.LOW -> MaterialTheme.colorScheme.error
                CompassAccuracy.MEDIUM -> MaterialTheme.colorScheme.secondary
                CompassAccuracy.HIGH -> QiblaGreen
            }
            Text(
                text = accuracy.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accuracyColor,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Steps
        CalibrationStep(number = "1", text = stringResource(R.string.calibration_step_1))
        CalibrationStep(number = "2", text = stringResource(R.string.calibration_step_2))
        CalibrationStep(number = "3", text = stringResource(R.string.calibration_step_3))

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Figure8Animation(modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val accentColor = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "fig8")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "trace",
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val a = size.width * 0.46f

        // Build lemniscate path (parametric: t = 0..2π)
        val steps = 200
        val fullPath = Path()
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2f * PI.toFloat()
            val denom = 1f + sin(t) * sin(t)
            val x = cx + a * cos(t) / denom
            val y = cy + a * sin(t) * cos(t) / denom
            if (i == 0) fullPath.moveTo(x, y) else fullPath.lineTo(x, y)
        }
        fullPath.close()

        // Draw background track
        drawPath(fullPath, color = trackColor, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))

        // Draw animated segment
        val measure = PathMeasure()
        measure.setPath(fullPath, true)
        val totalLength = measure.length
        val segLen = totalLength * 0.12f
        val startDist = (progress * totalLength) % totalLength
        val endDist = startDist + segLen

        val segment = Path()
        if (endDist <= totalLength) {
            measure.getSegment(startDist, endDist, segment, true)
        } else {
            // Wrap around
            measure.getSegment(startDist, totalLength, segment, true)
            val wrap = Path()
            measure.getSegment(0f, endDist - totalLength, wrap, true)
            segment.addPath(wrap)
        }
        drawPath(segment, color = accentColor, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun AccuracyMeter(accuracy: CompassAccuracy, modifier: Modifier = Modifier) {
    val segmentColors = listOf(
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.secondary,
        QiblaGreen,
    )
    val dimColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val shape = RoundedCornerShape(99.dp)

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(4) { i ->
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .background(
                        color = if (i <= accuracy.ordinal) segmentColors[i] else dimColor,
                        shape = shape,
                    )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, name = "Calibration Sheet - Low")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QiblaCalibrationSheetLowPreview() {
    NimazTheme {
        QiblaCalibrationSheet(accuracy = CompassAccuracy.LOW, onDismiss = {})
    }
}
