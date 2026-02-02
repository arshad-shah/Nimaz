package com.arshadshah.nimaz.presentation.screens.qibla

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaInfo
import kotlin.math.abs

// -- Colour palette --
private val GoldColor = Color(0xFFEAB308)
private val GoldDark = Color(0xFFCA9A06)
private val GreenColor = Color(0xFF22C55E)
private val GreenDark = Color(0xFF16A34A)
private val TealColor = Color(0xFF0D9488)
private val TealDark = Color(0xFF115E59)

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
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.fillMaxSize()) {
        // ──────────────────────────── Camera Preview ────────────────────────────
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

        // ──────────────────────────── AR Overlay ────────────────────────────
        ArOverlay(
            azimuth = azimuth,
            qiblaInfo = qiblaInfo,
            isFacingQibla = isFacingQibla,
            rotationToQibla = rotationToQibla,
            textMeasurer = textMeasurer,
            modifier = Modifier.fillMaxSize()
        )

        // ──────────────────────────── Top HUD ────────────────────────────
        TopHud(
            azimuth = azimuth,
            qiblaInfo = qiblaInfo,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 24.dp, end = 24.dp)
        )

        // ──────────────────────────── Center – Facing Qibla ────────────────────────────
        FacingQiblaIndicator(
            visible = isFacingQibla,
            modifier = Modifier.align(Alignment.Center)
        )

        // Turn direction hint (when NOT facing Qibla)
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

        // ──────────────────────────── Bottom HUD ────────────────────────────
        BottomHud(
            qiblaInfo = qiblaInfo,
            compassAccuracy = compassAccuracy,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        )

        // ──────────────────────────── Facing Qibla border glow ────────────────────────────
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

// =====================================================================================
//  Top HUD – gradient teal card with compass icon, heading, and Qibla bearing
// =====================================================================================
@Composable
private fun TopHud(
    azimuth: Float,
    qiblaInfo: QiblaInfo?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(TealDark, TealColor)
                )
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Small compass circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
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
                        fontWeight = FontWeight.SemiBold,
                        color = GoldColor
                    )
                }
            }
        }
    }
}

// =====================================================================================
//  Center indicator – pulsing rings, radial glow, mosque icon, badge
// =====================================================================================
@Composable
private fun FacingQiblaIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    // Pulsing ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "qibla_pulse")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 300),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    // Spring scale for the badge
    val badgeScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badge_scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )),
        exit = fadeOut() + scaleOut(animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing concentric rings + mosque icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Three pulsing rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxRadius = size.minDimension / 2

                    // Ring 3 (outermost)
                    drawCircle(
                        color = GreenColor.copy(alpha = (1f - ring3) * 0.25f),
                        radius = maxRadius * ring3,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Ring 2
                    drawCircle(
                        color = GreenColor.copy(alpha = (1f - ring2) * 0.35f),
                        radius = maxRadius * ring2,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Ring 1 (innermost)
                    drawCircle(
                        color = GreenColor.copy(alpha = (1f - ring1) * 0.45f),
                        radius = maxRadius * ring1,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Radial gradient glow behind icon
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GreenColor.copy(alpha = 0.35f),
                                GreenColor.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * 0.55f
                        ),
                        radius = maxRadius * 0.55f,
                        center = center
                    )
                }

                // Green circle with mosque icon (64dp)
                Surface(
                    shape = CircleShape,
                    color = GreenColor.copy(alpha = 0.25f),
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = "Facing Qibla",
                            tint = GreenColor,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // "Facing Qibla" badge – gradient green card
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GreenDark, GreenColor)
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Facing Qibla",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// =====================================================================================
//  Bottom HUD – two info pills side-by-side + optional location name pill
// =====================================================================================
@Composable
private fun BottomHud(
    qiblaInfo: QiblaInfo?,
    compassAccuracy: CompassAccuracy,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Two pills side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            // Distance pill
            qiblaInfo?.let { info ->
                InfoPill(
                    icon = Icons.Default.LocationOn,
                    iconTint = GoldColor,
                    value = "${String.format("%,d", (info.distanceToMecca / 1000).toInt())} km",
                    label = "to Mecca",
                    modifier = Modifier.weight(1f)
                )
            }

            // Accuracy pill
            val (accLabel, accColor, accIcon) = remember(compassAccuracy) {
                when (compassAccuracy) {
                    CompassAccuracy.HIGH -> Triple("High", GreenColor, Icons.Default.CheckCircle)
                    CompassAccuracy.MEDIUM -> Triple("Medium", Color(0xFFFACC15), Icons.Default.CheckCircle)
                    CompassAccuracy.LOW -> Triple("Low", Color(0xFFEF4444), Icons.Default.Warning)
                    CompassAccuracy.UNRELIABLE -> Triple("Unreliable", Color(0xFFEF4444), Icons.Default.Warning)
                }
            }
            InfoPill(
                icon = accIcon,
                iconTint = accColor,
                value = accLabel,
                label = "Compass",
                modifier = if (qiblaInfo != null) Modifier.weight(1f) else Modifier
            )
        }

        // Location name pill (if available)
        qiblaInfo?.let { info ->
            if (info.locationName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.60f),
                                    Color.Black.copy(alpha = 0.40f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = info.locationName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.65f),
                        Color.Black.copy(alpha = 0.40f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// =====================================================================================
//  AR Overlay – Canvas drawing: Qibla marker, edge arrows, compass strip
// =====================================================================================
@Composable
private fun ArOverlay(
    azimuth: Float,
    qiblaInfo: QiblaInfo?,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier
) {
    val arrowColor = if (isFacingQibla) GreenColor else GoldColor

    Canvas(modifier = modifier) {
        if (qiblaInfo == null) return@Canvas

        val centerX = size.width / 2
        val centerY = size.height / 2

        // Calculate horizontal offset based on rotation to Qibla
        val horizontalFov = 60f
        val pixelsPerDegree = size.width / horizontalFov
        val offsetX = rotationToQibla * pixelsPerDegree

        val arrowX = centerX + offsetX
        val isOnScreen = arrowX > -50f && arrowX < size.width + 50f

        if (isOnScreen) {
            drawQiblaMarker(
                x = arrowX.coerceIn(40f, size.width - 40f),
                y = centerY,
                color = arrowColor,
                isFacingQibla = isFacingQibla
            )
        } else {
            val edgeX = if (rotationToQibla > 0) size.width - 30f else 30f
            drawEdgeArrow(
                x = edgeX,
                y = centerY,
                pointRight = rotationToQibla > 0,
                color = arrowColor
            )
        }

        // Compass strip
        drawCompassStrip(
            azimuth = azimuth,
            qiblaBearing = qiblaInfo.direction.bearing.toFloat(),
            y = size.height - 120.dp.toPx(),
            arrowColor = arrowColor,
            textMeasurer = textMeasurer
        )
    }
}

// =====================================================================================
//  Qibla marker & edge arrow draw helpers (unchanged logic)
// =====================================================================================
private fun DrawScope.drawQiblaMarker(
    x: Float,
    y: Float,
    color: Color,
    isFacingQibla: Boolean
) {
    val markerSize = if (isFacingQibla) 32.dp.toPx() else 24.dp.toPx()

    if (isFacingQibla) {
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = markerSize * 2,
            center = Offset(x, y)
        )
    }

    val arrowPath = Path().apply {
        moveTo(x, y - markerSize)
        lineTo(x - markerSize * 0.6f, y + markerSize * 0.3f)
        lineTo(x, y)
        lineTo(x + markerSize * 0.6f, y + markerSize * 0.3f)
        close()
    }
    drawPath(path = arrowPath, color = color)

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

// =====================================================================================
//  Compass strip – wider ticks, cardinal labels, gold diamond Qibla marker
// =====================================================================================
private fun DrawScope.drawCompassStrip(
    azimuth: Float,
    qiblaBearing: Float,
    y: Float,
    arrowColor: Color,
    textMeasurer: TextMeasurer
) {
    val stripHeight = 2.dp.toPx()
    val centerX = size.width / 2
    val pixelsPerDegree = size.width / 60f // 60-degree visible range

    // Thin horizontal base line
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = stripHeight
    )

    val visibleRange = 30f
    val cardinalLabels = mapOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")

    for (deg in 0 until 360 step 15) {
        var diff = deg - azimuth
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        if (abs(diff) <= visibleRange) {
            val tickX = centerX + diff * pixelsPerDegree
            val isMajor = deg % 90 == 0
            // Wider ticks: major 12dp, minor 5dp
            val tickHeight = if (isMajor) 12.dp.toPx() else 5.dp.toPx()

            drawLine(
                color = Color.White.copy(alpha = if (isMajor) 0.8f else 0.3f),
                start = Offset(tickX, y - tickHeight),
                end = Offset(tickX, y + tickHeight),
                strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()
            )

            // Cardinal direction labels near major ticks
            cardinalLabels[deg]?.let { label ->
                val textStyle = TextStyle(
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                val measured = textMeasurer.measure(label, textStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        tickX - measured.size.width / 2f,
                        y + tickHeight + 4.dp.toPx()
                    )
                )
            }
        }
    }

    // Qibla marker – gold diamond shape
    var qiblaDiff = qiblaBearing - azimuth
    if (qiblaDiff > 180) qiblaDiff -= 360
    if (qiblaDiff < -180) qiblaDiff += 360

    if (abs(qiblaDiff) <= visibleRange) {
        val qiblaX = centerX + qiblaDiff * pixelsPerDegree
        val diamondHalf = 7.dp.toPx()
        val diamondTop = y - diamondHalf - 6.dp.toPx()

        val diamondPath = Path().apply {
            moveTo(qiblaX, diamondTop - diamondHalf)         // top vertex
            lineTo(qiblaX + diamondHalf, diamondTop)          // right vertex
            lineTo(qiblaX, diamondTop + diamondHalf)          // bottom vertex
            lineTo(qiblaX - diamondHalf, diamondTop)          // left vertex
            close()
        }
        drawPath(path = diamondPath, color = arrowColor)

        // Small connecting line from diamond to strip
        drawLine(
            color = arrowColor.copy(alpha = 0.6f),
            start = Offset(qiblaX, diamondTop + diamondHalf),
            end = Offset(qiblaX, y - 2.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
    }

    // Center tick (current heading)
    drawLine(
        color = Color.White,
        start = Offset(centerX, y - 14.dp.toPx()),
        end = Offset(centerX, y + 14.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )
}

// =====================================================================================
//  Helper – cardinal direction label from bearing
// =====================================================================================
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
