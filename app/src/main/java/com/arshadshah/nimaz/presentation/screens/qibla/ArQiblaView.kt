package com.arshadshah.nimaz.presentation.screens.qibla

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaInfo
import kotlin.math.abs

private val GoldColor = Color(0xFFEAB308)
private val GreenColor = Color(0xFF22C55E)

@Composable
fun ArQiblaView(
    azimuth: Float,
    qiblaInfo: QiblaInfo?,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    isCompassReady: Boolean,
    compassAccuracy: CompassAccuracy,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        val previewView = remember {
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

        DisposableEffect(lifecycleOwner) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            var cameraProvider: ProcessCameraProvider? = null

            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (_: Exception) {
                    // Camera may not be available
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))

            onDispose {
                cameraProvider?.unbindAll()
            }
        }

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // AR Overlay
        ArOverlay(
            azimuth = azimuth,
            qiblaInfo = qiblaInfo,
            isFacingQibla = isFacingQibla,
            rotationToQibla = rotationToQibla,
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD - Heading
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${azimuth.toInt()}\u00B0 ${getCardinalDirection(azimuth.toDouble())}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    qiblaInfo?.let { info ->
                        Text(
                            text = "Qibla ${info.direction.bearing.toInt()}\u00B0 ${getCardinalDirection(info.direction.bearing)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldColor
                        )
                    }
                }
            }
        }

        // Center - Facing Qibla indicator
        AnimatedVisibility(
            visible = isFacingQibla,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = GreenColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = "Facing Qibla",
                            tint = GreenColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GreenColor.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "Facing Qibla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Turn direction hint (when not facing Qibla)
        AnimatedVisibility(
            visible = !isFacingQibla && isCompassReady,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 120.dp)
        ) {
            val turnRight = rotationToQibla > 0
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (turnRight)
                            Icons.AutoMirrored.Filled.RotateRight
                        else
                            Icons.AutoMirrored.Filled.RotateLeft,
                        contentDescription = null,
                        tint = GoldColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Turn ${if (turnRight) "right" else "left"} ${abs(rotationToQibla).toInt()}\u00B0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // Bottom HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Distance pill
            qiblaInfo?.let { info ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GoldColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${String.format("%,d", (info.distanceToMecca / 1000).toInt())} km to Mecca",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Accuracy pill
            ArAccuracyPill(accuracy = compassAccuracy)
        }

        // Facing Qibla green border glow
        if (isFacingQibla) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GreenColor.copy(alpha = 0.6f),
                            GreenColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    size = size,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

@Composable
private fun ArOverlay(
    azimuth: Float,
    qiblaInfo: QiblaInfo?,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    modifier: Modifier = Modifier
) {
    val arrowColor = if (isFacingQibla) GreenColor else GoldColor

    Canvas(modifier = modifier) {
        if (qiblaInfo == null) return@Canvas

        val centerX = size.width / 2
        val centerY = size.height / 2

        // Calculate horizontal offset based on rotation to Qibla
        // Map the rotation angle to screen position
        // Assuming ~60° horizontal FOV for the camera
        val horizontalFov = 60f
        val pixelsPerDegree = size.width / horizontalFov
        val offsetX = rotationToQibla * pixelsPerDegree

        val arrowX = centerX + offsetX
        val isOnScreen = arrowX > -50f && arrowX < size.width + 50f

        if (isOnScreen) {
            // Draw Qibla indicator on screen
            drawQiblaMarker(
                x = arrowX.coerceIn(40f, size.width - 40f),
                y = centerY,
                color = arrowColor,
                isFacingQibla = isFacingQibla
            )
        } else {
            // Draw edge indicator
            val edgeX = if (rotationToQibla > 0) size.width - 30f else 30f
            drawEdgeArrow(
                x = edgeX,
                y = centerY,
                pointRight = rotationToQibla > 0,
                color = arrowColor
            )
        }

        // Compass strip at the bottom of the overlay
        drawCompassStrip(
            azimuth = azimuth,
            qiblaBearing = qiblaInfo.direction.bearing.toFloat(),
            y = size.height - 120.dp.toPx(),
            arrowColor = arrowColor
        )
    }
}

private fun DrawScope.drawQiblaMarker(
    x: Float,
    y: Float,
    color: Color,
    isFacingQibla: Boolean
) {
    val markerSize = if (isFacingQibla) 32.dp.toPx() else 24.dp.toPx()

    // Outer glow
    if (isFacingQibla) {
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = markerSize * 2,
            center = Offset(x, y)
        )
    }

    // Arrow pointing down (towards the ground / Qibla direction)
    val arrowPath = Path().apply {
        moveTo(x, y - markerSize)
        lineTo(x - markerSize * 0.6f, y + markerSize * 0.3f)
        lineTo(x, y)
        lineTo(x + markerSize * 0.6f, y + markerSize * 0.3f)
        close()
    }
    drawPath(path = arrowPath, color = color)

    // Kaaba square at the tip
    val kaabaSize = 12.dp.toPx()
    drawRect(
        color = color,
        topLeft = Offset(x - kaabaSize / 2, y - markerSize - kaabaSize - 4.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawEdgeArrow(
    x: Float,
    y: Float,
    pointRight: Boolean,
    color: Color
) {
    val arrowSize = 20.dp.toPx()
    val chevronPath = Path().apply {
        if (pointRight) {
            moveTo(x - arrowSize / 2, y - arrowSize)
            lineTo(x + arrowSize / 2, y)
            lineTo(x - arrowSize / 2, y + arrowSize)
        } else {
            moveTo(x + arrowSize / 2, y - arrowSize)
            lineTo(x - arrowSize / 2, y)
            lineTo(x + arrowSize / 2, y + arrowSize)
        }
    }
    drawPath(
        path = chevronPath,
        color = color,
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawCompassStrip(
    azimuth: Float,
    qiblaBearing: Float,
    y: Float,
    arrowColor: Color
) {
    val stripHeight = 2.dp.toPx()
    val centerX = size.width / 2
    val pixelsPerDegree = size.width / 60f // 60° visible range

    // Draw thin horizontal line
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = stripHeight
    )

    // Draw tick marks for cardinal directions within visible range
    val visibleRange = 30f // ±30° from center
    for (deg in 0 until 360 step 15) {
        var diff = deg - azimuth
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        if (abs(diff) <= visibleRange) {
            val tickX = centerX + diff * pixelsPerDegree
            val isMajor = deg % 90 == 0
            val tickHeight = if (isMajor) 10.dp.toPx() else 5.dp.toPx()

            drawLine(
                color = Color.White.copy(alpha = if (isMajor) 0.7f else 0.3f),
                start = Offset(tickX, y - tickHeight),
                end = Offset(tickX, y + tickHeight),
                strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
            )
        }
    }

    // Draw Qibla marker on strip
    var qiblaDiff = qiblaBearing - azimuth
    if (qiblaDiff > 180) qiblaDiff -= 360
    if (qiblaDiff < -180) qiblaDiff += 360

    if (abs(qiblaDiff) <= visibleRange) {
        val qiblaX = centerX + qiblaDiff * pixelsPerDegree
        val markerSize = 6.dp.toPx()
        val markerPath = Path().apply {
            moveTo(qiblaX, y - markerSize - 4.dp.toPx())
            lineTo(qiblaX - markerSize, y - markerSize - 4.dp.toPx() - markerSize)
            lineTo(qiblaX + markerSize, y - markerSize - 4.dp.toPx() - markerSize)
            close()
        }
        drawPath(path = markerPath, color = arrowColor)
    }

    // Center tick (current heading)
    drawLine(
        color = Color.White,
        start = Offset(centerX, y - 14.dp.toPx()),
        end = Offset(centerX, y + 14.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )
}

@Composable
private fun ArAccuracyPill(
    accuracy: CompassAccuracy
) {
    val (label, color) = when (accuracy) {
        CompassAccuracy.HIGH -> "High" to GreenColor
        CompassAccuracy.MEDIUM -> "Medium" to Color(0xFFFACC15)
        CompassAccuracy.LOW -> "Low" to Color(0xFFEF4444)
        CompassAccuracy.UNRELIABLE -> "Unreliable" to Color(0xFFEF4444)
    }

    val needsCalibration = accuracy == CompassAccuracy.LOW || accuracy == CompassAccuracy.UNRELIABLE

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (needsCalibration) Icons.Default.Warning
                else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Accuracy: $label",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

private fun getCardinalDirection(bearing: Double): String {
    return when {
        bearing < 22.5 || bearing >= 337.5 -> "N"
        bearing < 67.5 -> "NE"
        bearing < 112.5 -> "E"
        bearing < 157.5 -> "SE"
        bearing < 202.5 -> "S"
        bearing < 247.5 -> "SW"
        bearing < 292.5 -> "W"
        else -> "NW"
    }
}
