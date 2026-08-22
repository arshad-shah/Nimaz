package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.abs

/**
 * A gold arc sweeping from the AIM notch (12 o'clock) toward the Qibla needle,
 * showing the user exactly how far and which way to turn. The arc animates
 * smoothly as the device rotates and vanishes once the user is aligned.
 */
@Composable
fun CompassTurnArc(
    rotationToQibla: Float,
    isFacingQibla: Boolean,
    modifier: Modifier = Modifier,
) {
    val sweep by animateFloatAsState(
        targetValue = if (isFacingQibla || abs(rotationToQibla) < 2f) 0f else rotationToQibla,
        animationSpec = tween(120),
        label = "turn_arc_sweep"
    )

    Canvas(modifier = modifier) {
        if (abs(sweep) < 1f) return@Canvas

        val radius = size.minDimension / 2f
        // Arc sits just inside the outer ring, between the two decorative circles.
        val arcRadius = radius - 6.dp.toPx()
        val strokeWidth = 3.dp.toPx()
        val inset = radius - arcRadius
        val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = QiblaGold.copy(alpha = 0.85f),
            startAngle = -90f,          // 12 o'clock in Android canvas coords
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Turn Arc - Right 45°")
@Composable
private fun CompassTurnArcRightPreview() {
    NimazTheme {
        CompassTurnArc(
            rotationToQibla = 45f,
            isFacingQibla = false,
            modifier = Modifier.size(280.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Turn Arc - Left 120°")
@Composable
private fun CompassTurnArcLeftPreview() {
    NimazTheme {
        CompassTurnArc(
            rotationToQibla = -120f,
            isFacingQibla = false,
            modifier = Modifier.size(280.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Turn Arc - Facing (hidden)")
@Composable
private fun CompassTurnArcFacingPreview() {
    NimazTheme {
        CompassTurnArc(
            rotationToQibla = 0f,
            isFacingQibla = true,
            modifier = Modifier.size(280.dp)
        )
    }
}
