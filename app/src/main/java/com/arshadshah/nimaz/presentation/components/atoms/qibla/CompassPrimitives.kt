package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual primitives for the Qibla compass. These atoms draw a single layer of
 * the compass each (rings, ticks + arrow, cardinal labels, north pointer,
 * center dot, glow) and are composed into the full widget by the
 * QiblaCompassWidget organism. Keeping them separate lets each layer be
 * previewed in isolation; the "Full Compass Dial" previews below show them
 * assembled, the way the organism stacks them.
 */

/** Accent used for the Qibla arrow / highlights. */
val QiblaGold = Color(0xFFEAB308)

/** Accent used when the user is facing the Qibla. */
val QiblaGreen = Color(0xFF22C55E)

/** Red used to mark North across the compass. */
val CompassNorthColor = Color(0xFFEF4444)

/** Outer + inner decorative rings of the compass. */
@Composable
fun CompassRings(modifier: Modifier = Modifier) {
    val outerRingColor = MaterialTheme.colorScheme.outlineVariant
    val innerRingColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2

        drawCircle(
            color = outerRingColor,
            radius = outerRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = innerRingColor,
            radius = outerRadius - 10.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/** Cardinal direction labels (N/E/S/W) drawn around the dial. */
@Composable
fun DirectionMarkers(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val northColor = CompassNorthColor
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        val directions = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        directions.forEach { (label, angleDeg) ->
            val isNorth = label == "N"
            val textColor = if (isNorth) northColor else onSurfaceVariantColor
            val style = TextStyle(
                fontSize = if (isNorth) 16.sp else 14.sp,
                fontWeight = if (isNorth) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor
            )
            val textResult = textMeasurer.measure(label, style)
            val angle = Math.toRadians(angleDeg.toDouble())
            val markerRadius = radius - 38.dp.toPx()
            val x = center.x + (markerRadius * sin(angle)).toFloat() - textResult.size.width / 2
            val y = center.y - (markerRadius * cos(angle)).toFloat() - textResult.size.height / 2

            drawText(textLayoutResult = textResult, topLeft = Offset(x, y))
        }
    }
}

/** Dial face with tick marks and the Qibla arrow + Kaaba marker at its tip. */
@Composable
fun CompassDial(
    qiblaBearing: Float,
    isFacingQibla: Boolean,
    modifier: Modifier = Modifier,
    goldColor: Color = QiblaGold,
) {
    val dialBackground = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainer
        )
    )
    val tickColorMajor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val tickColorMinor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val arrowColor = if (isFacingQibla) QiblaGreen else goldColor
    // Needle tail + pivot pin colors (read here; can't touch MaterialTheme inside DrawScope)
    val tailColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val pinColor = MaterialTheme.colorScheme.outline
    val pinHighlightColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        drawCircle(brush = dialBackground, radius = radius, center = center)

        for (i in 0 until 360 step 5) {
            val isMajor = i % 30 == 0
            val tickLength = if (isMajor) 15.dp.toPx() else 8.dp.toPx()
            val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
            val tickColor = if (isMajor) tickColorMajor else tickColorMinor
            val angle = Math.toRadians(i.toDouble())
            val startR = radius - tickLength

            drawLine(
                color = tickColor,
                start = Offset(
                    center.x + (startR * sin(angle)).toFloat(),
                    center.y - (startR * cos(angle)).toFloat()
                ),
                end = Offset(
                    center.x + (radius * sin(angle)).toFloat(),
                    center.y - (radius * cos(angle)).toFloat()
                ),
                strokeWidth = tickWidth
            )
        }

        // Qibla needle — a real compass hand: a long front blade toward the
        // Qibla and a shorter tail behind, both meeting at the center where a
        // pivot pin passes through (drawn last so it sits on top of the blades).
        val qiblaAngleRad = Math.toRadians(qiblaBearing.toDouble())
        val perpAngle = qiblaAngleRad + Math.PI / 2

        val frontLength = radius - 34.dp.toPx()
        val backLength = radius * 0.40f
        val halfWidth = 9.dp.toPx()

        // Blade shoulder points (the widest part of the needle, at the center)
        val shoulderLeftX = center.x + (halfWidth * sin(perpAngle)).toFloat()
        val shoulderLeftY = center.y - (halfWidth * cos(perpAngle)).toFloat()
        val shoulderRightX = center.x - (halfWidth * sin(perpAngle)).toFloat()
        val shoulderRightY = center.y + (halfWidth * cos(perpAngle)).toFloat()

        val frontTipX = center.x + (frontLength * sin(qiblaAngleRad)).toFloat()
        val frontTipY = center.y - (frontLength * cos(qiblaAngleRad)).toFloat()
        val backTipX = center.x - (backLength * sin(qiblaAngleRad)).toFloat()
        val backTipY = center.y + (backLength * cos(qiblaAngleRad)).toFloat()

        // Front blade (points at the Qibla)
        drawPath(
            path = Path().apply {
                moveTo(frontTipX, frontTipY)
                lineTo(shoulderLeftX, shoulderLeftY)
                lineTo(shoulderRightX, shoulderRightY)
                close()
            },
            color = arrowColor
        )
        // Back blade (counterweight tail)
        drawPath(
            path = Path().apply {
                moveTo(backTipX, backTipY)
                lineTo(shoulderLeftX, shoulderLeftY)
                lineTo(shoulderRightX, shoulderRightY)
                close()
            },
            color = tailColor
        )

        // Kaaba icon at the needle's front tip
        val kaabaSize = 16.dp.toPx()
        val kaabaOffset = 10.dp.toPx()
        val kaabaCenterX = center.x + ((frontLength + kaabaOffset) * sin(qiblaAngleRad)).toFloat()
        val kaabaCenterY = center.y - ((frontLength + kaabaOffset) * cos(qiblaAngleRad)).toFloat()
        drawRect(
            color = arrowColor,
            topLeft = Offset(kaabaCenterX - kaabaSize / 2, kaabaCenterY - kaabaSize / 2),
            size = Size(kaabaSize, kaabaSize),
            style = Stroke(width = 1.5f.dp.toPx())
        )

        // Pivot pin — the needle pivots through this, like a real compass
        drawCircle(color = pinColor, radius = halfWidth * 1.5f, center = center)
        drawCircle(color = pinHighlightColor, radius = halfWidth * 0.65f, center = center)
    }
}

/** Static red triangle pinned to the top of the compass marking device North. */
@Composable
fun CompassNorthIndicator(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, 0f)
        val triSize = 16.dp.toPx()
        val path = Path().apply {
            moveTo(center.x, triSize + 2.dp.toPx())
            lineTo(center.x - triSize / 2, 2.dp.toPx())
            lineTo(center.x + triSize / 2, 2.dp.toPx())
            close()
        }
        drawPath(path = path, color = CompassNorthColor)
    }
}

/** Center hub of the compass; grows and shows a Mosque glyph when facing Qibla. */
@Composable
fun CompassCenterDot(
    isFacingQibla: Boolean,
    modifier: Modifier = Modifier,
    greenColor: Color = QiblaGreen,
) {
    Box(
        modifier = modifier
            .size(if (isFacingQibla) 28.dp else 20.dp)
            .clip(CircleShape)
            .background(
                if (isFacingQibla) greenColor
                else MaterialTheme.colorScheme.outline
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isFacingQibla) {
            Icon(
                imageVector = Icons.Default.Mosque,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Soft green halo shown over the dial when the user is facing the Qibla. */
@Composable
fun CompassFacingGlow(
    visible: Boolean,
    modifier: Modifier = Modifier,
    greenColor: Color = QiblaGreen,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Canvas(modifier = modifier) {
            drawCircle(
                color = greenColor.copy(alpha = 0.15f),
                radius = size.minDimension / 2
            )
        }
    }
}

// Previews — individual atom layers

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Rings")
@Composable
private fun CompassRingsPreview() {
    NimazTheme {
        CompassRings(modifier = Modifier.size(280.dp))
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Dial")
@Composable
private fun CompassDialPreview() {
    NimazTheme {
        CompassDial(
            qiblaBearing = 45f,
            isFacingQibla = false,
            modifier = Modifier.size(280.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Compass Dial - Facing")
@Composable
private fun CompassDialFacingPreview() {
    NimazTheme {
        CompassDial(
            qiblaBearing = 0f,
            isFacingQibla = true,
            modifier = Modifier.size(280.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300, name = "Direction Markers")
@Composable
private fun DirectionMarkersPreview() {
    NimazTheme {
        DirectionMarkers(modifier = Modifier.size(280.dp))
    }
}

@Preview(showBackground = true, name = "North Indicator")
@Composable
private fun CompassNorthIndicatorPreview() {
    NimazTheme {
        CompassNorthIndicator(modifier = Modifier.size(280.dp))
    }
}

@Preview(showBackground = true, name = "Center Dot")
@Composable
private fun CompassCenterDotPreview() {
    NimazTheme {
        CompassCenterDot(isFacingQibla = false, modifier = Modifier)
    }
}

@Preview(showBackground = true, name = "Center Dot - Facing")
@Composable
private fun CompassCenterDotFacingPreview() {
    NimazTheme {
        CompassCenterDot(isFacingQibla = true, modifier = Modifier)
    }
}

// Previews — full compass dial, assembled from the atoms above
// (mirrors how QiblaCompassWidget stacks the layers).

/** Preview-only assembly so the atom layers can be seen built up together. */
@Composable
private fun AssembledCompassDial(
    qiblaBearing: Float,
    isFacingQibla: Boolean,
    azimuth: Float,
    size: androidx.compose.ui.unit.Dp = 280.dp,
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        CompassRings(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .size(size - 20.dp)
                .rotate(-azimuth),
            contentAlignment = Alignment.Center
        ) {
            CompassDial(
                qiblaBearing = qiblaBearing,
                isFacingQibla = isFacingQibla,
                modifier = Modifier.size(size - 50.dp)
            )
            DirectionMarkers(modifier = Modifier.fillMaxSize())
        }

        CompassCenterDot(isFacingQibla = isFacingQibla)

        CompassFacingGlow(visible = isFacingQibla, modifier = Modifier.size(size - 20.dp))

        CompassNorthIndicator(modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320, name = "Full Compass Dial")
@Composable
private fun FullCompassDialPreview() {
    NimazTheme {
        AssembledCompassDial(qiblaBearing = 119f, isFacingQibla = false, azimuth = 30f)
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320, name = "Full Compass Dial - Facing")
@Composable
private fun FullCompassDialFacingPreview() {
    NimazTheme {
        AssembledCompassDial(qiblaBearing = 0f, isFacingQibla = true, azimuth = 0f)
    }
}
