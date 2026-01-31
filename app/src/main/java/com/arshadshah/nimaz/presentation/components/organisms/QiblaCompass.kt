package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaDirection
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full Qibla compass with direction and calibration info.
 */
@Composable
fun QiblaCompass(
    qiblaInfo: QiblaInfo,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    locationName: String? = null
) {
    val animatedQiblaAngle by animateFloatAsState(
        targetValue = qiblaInfo.qiblaAngle,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "qibla_rotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Qibla Direction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (locationName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Compass
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Compass face
                CompassFace(
                    compassHeading = qiblaInfo.compass?.azimuth ?: 0f,
                    modifier = Modifier.fillMaxSize()
                )

                // Qibla indicator
                QiblaIndicator(
                    angle = animatedQiblaAngle,
                    modifier = Modifier.fillMaxSize()
                )

                // Center Kaaba icon
                KaabaCenter(isAligned = isQiblaAligned(qiblaInfo.qiblaAngle))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Direction info
            DirectionInfo(
                bearing = qiblaInfo.direction.bearing,
                distance = qiblaInfo.direction.distance
            )

            // Calibration warning
            AnimatedVisibility(
                visible = qiblaInfo.isCalibrationNeeded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CalibrationWarning(
                    accuracy = qiblaInfo.compass?.accuracy ?: CompassAccuracy.UNRELIABLE,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

/**
 * Compass face with cardinal directions and degree markings.
 */
@Composable
private fun CompassFace(
    compassHeading: Float,
    modifier: Modifier = Modifier
) {
    val animatedHeading by animateFloatAsState(
        targetValue = -compassHeading,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "compass_rotation"
    )

    Canvas(modifier = modifier.rotate(animatedHeading)) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2 - 20.dp.toPx()
        val innerRadius = outerRadius - 30.dp.toPx()

        // Outer circle
        drawCircle(
            color = Color.Gray.copy(alpha = 0.1f),
            radius = outerRadius,
            center = center
        )

        // Inner circle
        drawCircle(
            color = Color.Gray.copy(alpha = 0.05f),
            radius = innerRadius,
            center = center
        )

        // Degree markings
        for (i in 0 until 360 step 10) {
            val angle = Math.toRadians(i.toDouble() - 90)
            val isCardinal = i % 90 == 0
            val isMajor = i % 30 == 0

            val startRadius = if (isCardinal) innerRadius - 15.dp.toPx()
                else if (isMajor) innerRadius - 10.dp.toPx()
                else innerRadius - 5.dp.toPx()

            val startX = center.x + startRadius * cos(angle).toFloat()
            val startY = center.y + startRadius * sin(angle).toFloat()
            val endX = center.x + innerRadius * cos(angle).toFloat()
            val endY = center.y + innerRadius * sin(angle).toFloat()

            drawLine(
                color = if (isCardinal) Color.Black else Color.Gray,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (isCardinal) 3.dp.toPx() else 1.dp.toPx()
            )
        }

        // Cardinal direction labels
        val cardinals = listOf(
            Triple(0, "N", Color(0xFFD32F2F)),
            Triple(90, "E", Color.Gray),
            Triple(180, "S", Color.Gray),
            Triple(270, "W", Color.Gray)
        )

        cardinals.forEach { (degree, _, _) ->
            val angle = Math.toRadians(degree.toDouble() - 90)
            val labelRadius = innerRadius - 35.dp.toPx()

            val x = center.x + labelRadius * cos(angle).toFloat()
            val y = center.y + labelRadius * sin(angle).toFloat()

            // Draw colored circle for N
            if (degree == 0) {
                drawCircle(
                    color = Color(0xFFD32F2F),
                    radius = 8.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Qibla direction indicator arrow.
 */
@Composable
private fun QiblaIndicator(
    angle: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val isAligned = isQiblaAligned(angle)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.rotate(angle)) {
        val center = Offset(size.width / 2, size.height / 2)
        val arrowLength = size.minDimension / 2 - 60.dp.toPx()

        // Glow effect when aligned
        if (isAligned) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = pulseAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = 100.dp.toPx()
                ),
                radius = 100.dp.toPx(),
                center = center
            )
        }

        // Qibla arrow
        val arrowPath = Path().apply {
            // Arrow point
            moveTo(center.x, center.y - arrowLength)
            // Right side
            lineTo(center.x + 20.dp.toPx(), center.y - arrowLength + 40.dp.toPx())
            // Inner right
            lineTo(center.x + 8.dp.toPx(), center.y - arrowLength + 35.dp.toPx())
            // Right tail
            lineTo(center.x + 8.dp.toPx(), center.y + 40.dp.toPx())
            // Bottom center
            lineTo(center.x, center.y + 50.dp.toPx())
            // Left tail
            lineTo(center.x - 8.dp.toPx(), center.y + 40.dp.toPx())
            // Inner left
            lineTo(center.x - 8.dp.toPx(), center.y - arrowLength + 35.dp.toPx())
            // Left side
            lineTo(center.x - 20.dp.toPx(), center.y - arrowLength + 40.dp.toPx())
            close()
        }

        // Arrow fill
        drawPath(
            path = arrowPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (isAligned) primaryColor else secondaryColor,
                    if (isAligned) primaryColor else secondaryColor
                )
            )
        )

        // Arrow outline
        drawPath(
            path = arrowPath,
            color = Color.White.copy(alpha = 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Center Kaaba icon.
 */
@Composable
private fun KaabaCenter(
    isAligned: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isAligned) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Kaaba representation
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color.Black)
        )
    }
}

/**
 * Direction information display.
 */
@Composable
private fun DirectionInfo(
    bearing: Double,
    distance: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DirectionInfoItem(
            value = "${bearing.toInt()}°",
            label = "Bearing",
            icon = Icons.Default.Navigation
        )
        DirectionInfoItem(
            value = "${String.format("%.0f", distance)} km",
            label = "Distance to Mecca",
            icon = Icons.Default.LocationOn
        )
    }
}

@Composable
private fun DirectionInfoItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Calibration warning banner.
 */
@Composable
private fun CalibrationWarning(
    accuracy: CompassAccuracy,
    modifier: Modifier = Modifier
) {
    val warningColor = when (accuracy) {
        CompassAccuracy.UNRELIABLE -> MaterialTheme.colorScheme.error
        CompassAccuracy.LOW -> NimazColors.FastingColors.Makeup
        else -> NimazColors.StatusColors.Pending
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = warningColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = warningColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Calibration Needed",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = warningColor
                )
                Text(
                    text = "Move your phone in a figure-8 pattern to calibrate the compass",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact Qibla indicator for home screen.
 */
@Composable
fun CompactQiblaIndicator(
    qiblaAngle: Float,
    bearing: Double,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val animatedAngle by animateFloatAsState(
        targetValue = qiblaAngle,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "compact_qibla_rotation"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini compass
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Qibla direction",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(animatedAngle)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Qibla",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${bearing.toInt()}° from North",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Check if device is aligned with Qibla (within 5 degrees).
 */
private fun isQiblaAligned(angle: Float): Boolean {
    val normalizedAngle = ((angle % 360) + 360) % 360
    return normalizedAngle < 5 || normalizedAngle > 355
}

@Preview(showBackground = true)
@Composable
private fun QiblaCompassPreview() {
    NimazTheme {
        QiblaCompass(
            qiblaInfo = QiblaInfo(
                direction = QiblaDirection(
                    bearing = 136.5,
                    distance = 5200.0,
                    userLatitude = 53.35,
                    userLongitude = -6.26
                ),
                locationName = "Dublin",
                latitude = 53.35,
                longitude = -6.26,
                distanceToMecca = 5200.0,
                compass = null,
                qiblaAngle = 136.5f,
                isCalibrationNeeded = false
            ),
            locationName = "Dublin, Ireland",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompactQiblaIndicatorPreview() {
    NimazTheme {
        CompactQiblaIndicator(
            qiblaAngle = 136.5f,
            bearing = 136.5,
            modifier = Modifier.padding(16.dp)
        )
    }
}
