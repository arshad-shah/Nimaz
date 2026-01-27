package com.arshadshah.nimaz.presentation.screens.qibla

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.QiblaEvent
import com.arshadshah.nimaz.presentation.viewmodel.QiblaViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onNavigateBack: () -> Unit,
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val state by viewModel.qiblaState.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val animatedRotation by animateFloatAsState(
        targetValue = state.rotationToQibla,
        animationSpec = tween(300),
        label = "compass_rotation"
    )

    // Gold color used throughout (matches HTML --gold-500)
    val goldColor = Color(0xFFEAB308)
    val greenColor = Color(0xFF22C55E)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Qibla",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(QiblaEvent.ShowLocationPicker) }) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Location Info
                state.qiblaInfo?.let { info ->
                    Text(
                        text = info.locationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.4f", info.latitude)}\u00B0 N, ${
                            String.format("%.4f", abs(info.longitude))
                        }\u00B0 ${if (info.longitude < 0) "W" else "E"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Compass
                Box(
                    modifier = Modifier.size(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer compass ring and inner ring
                    CompassRings(modifier = Modifier.fillMaxSize())

                    // Direction markers (N, E, S, W) - static
                    DirectionMarkers(modifier = Modifier.fillMaxSize())

                    // Compass dial with ticks and qibla arrow - rotates with compass
                    CompassDial(
                        azimuth = state.compassData.azimuth,
                        qiblaBearing = state.qiblaDirection?.bearing?.toFloat() ?: 0f,
                        isFacingQibla = state.isFacingQibla,
                        goldColor = goldColor,
                        modifier = Modifier
                            .size(250.dp)
                            .rotate(-state.compassData.azimuth)
                    )

                    // Center dot
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Qibla Direction Info
                state.qiblaInfo?.let { info ->
                    Text(
                        text = "${info.direction.bearing.toInt()}\u00B0",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = goldColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${getCompassDirection(info.direction.bearing)} \u2022 Qibla Direction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Distance to Mecca pill
                    Surface(
                        shape = RoundedCornerShape(25.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${String.format("%,d", (info.distanceToMecca / 1000).toInt())} km to Mecca",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Calibration Warning
                if (state.needsCalibration) {
                    CalibrationWarning()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Accuracy Bar
                AccuracyBar(
                    accuracy = state.compassData.accuracy,
                    greenColor = greenColor,
                    onCalibrate = { viewModel.onEvent(QiblaEvent.StartCompass) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CompassRings(modifier: Modifier = Modifier) {
    val outerRingColor = MaterialTheme.colorScheme.outlineVariant
    val innerRingColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2

        // Outer ring
        drawCircle(
            color = outerRingColor,
            radius = outerRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Inner ring
        drawCircle(
            color = innerRingColor,
            radius = outerRadius - 10.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun DirectionMarkers(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        val directions = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        directions.forEach { (label, angleDeg) ->
            val isNorth = label == "N"
            val textColor = if (isNorth) onBackgroundColor else onSurfaceVariantColor
            val style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            val textResult = textMeasurer.measure(label, style)
            val angle = Math.toRadians(angleDeg.toDouble())
            val markerRadius = radius - 20.dp.toPx()
            val x = center.x + (markerRadius * sin(angle)).toFloat() - textResult.size.width / 2
            val y = center.y - (markerRadius * cos(angle)).toFloat() - textResult.size.height / 2

            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(x, y)
            )
        }
    }
}

@Composable
private fun CompassDial(
    azimuth: Float,
    qiblaBearing: Float,
    isFacingQibla: Boolean,
    goldColor: Color,
    modifier: Modifier = Modifier
) {
    val dialBackground = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainer
        )
    )
    val tickColorMajor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val tickColorMinor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val arrowColor = if (isFacingQibla) Color(0xFF22C55E) else goldColor
    val arrowGlow = arrowColor.copy(alpha = 0.5f)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Dial background
        drawCircle(
            brush = dialBackground,
            radius = radius,
            center = center
        )

        // Degree ticks
        for (i in 0 until 360 step 5) {
            val isMajor = i % 30 == 0
            val tickLength = if (isMajor) 15.dp.toPx() else 8.dp.toPx()
            val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
            val tickColor = if (isMajor) tickColorMajor else tickColorMinor
            val angle = Math.toRadians(i.toDouble())
            val startR = radius - tickLength
            val endR = radius

            drawLine(
                color = tickColor,
                start = Offset(
                    center.x + (startR * sin(angle)).toFloat(),
                    center.y - (startR * cos(angle)).toFloat()
                ),
                end = Offset(
                    center.x + (endR * sin(angle)).toFloat(),
                    center.y - (endR * cos(angle)).toFloat()
                ),
                strokeWidth = tickWidth
            )
        }

        // Qibla arrow
        val qiblaAngleRad = Math.toRadians(qiblaBearing.toDouble())
        val arrowLength = radius - 30.dp.toPx()
        val arrowBaseWidth = 15.dp.toPx()

        val tipX = center.x + (arrowLength * sin(qiblaAngleRad)).toFloat()
        val tipY = center.y - (arrowLength * cos(qiblaAngleRad)).toFloat()

        // Arrow triangle
        val perpAngle = qiblaAngleRad + Math.PI / 2
        val baseLeftX = center.x + (arrowBaseWidth * sin(perpAngle)).toFloat()
        val baseLeftY = center.y - (arrowBaseWidth * cos(perpAngle)).toFloat()
        val baseRightX = center.x - (arrowBaseWidth * sin(perpAngle)).toFloat()
        val baseRightY = center.y + (arrowBaseWidth * cos(perpAngle)).toFloat()

        val arrowPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseLeftX, baseLeftY)
            lineTo(baseRightX, baseRightY)
            close()
        }

        drawPath(
            path = arrowPath,
            color = arrowColor
        )

        // Kaaba icon at tip (small square)
        val kaabaSize = 16.dp.toPx()
        val kaabaOffset = 10.dp.toPx()
        val kaabaCenterX = center.x + ((arrowLength + kaabaOffset) * sin(qiblaAngleRad)).toFloat()
        val kaabaCenterY = center.y - ((arrowLength + kaabaOffset) * cos(qiblaAngleRad)).toFloat()

        // Simple kaaba representation - small outlined rect
        drawRect(
            color = arrowColor,
            topLeft = Offset(kaabaCenterX - kaabaSize / 2, kaabaCenterY - kaabaSize / 2),
            size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize),
            style = Stroke(width = 1.5f.dp.toPx())
        )
    }
}

@Composable
private fun CalibrationWarning(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Calibration Needed",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Move your phone in a figure-8 pattern",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccuracyBar(
    accuracy: CompassAccuracy,
    greenColor: Color,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (accuracy) {
        CompassAccuracy.HIGH -> "High Accuracy" to greenColor
        CompassAccuracy.MEDIUM -> "Medium Accuracy" to Color(0xFFFACC15)
        CompassAccuracy.LOW -> "Low Accuracy" to MaterialTheme.colorScheme.error
        CompassAccuracy.UNRELIABLE -> "Unreliable" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accuracy icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Accuracy info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Compass Accuracy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }

            // Calibrate button
            Button(
                onClick = onCalibrate,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Calibrate",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun getCompassDirection(bearing: Double): String {
    return when {
        bearing < 22.5 || bearing >= 337.5 -> "North"
        bearing < 67.5 -> "Northeast"
        bearing < 112.5 -> "East"
        bearing < 157.5 -> "Southeast"
        bearing < 202.5 -> "South"
        bearing < 247.5 -> "Southwest"
        bearing < 292.5 -> "West"
        else -> "Northwest"
    }
}
