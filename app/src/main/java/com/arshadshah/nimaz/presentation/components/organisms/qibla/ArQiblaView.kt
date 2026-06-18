package com.arshadshah.nimaz.presentation.components.organisms.qibla

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGold
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaStatusCapsule
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaAccuracyPill
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The AR Qibla view: a full-bleed back-camera feed with a canvas overlay that
 * points at the real-world Qibla, plus the shared [QiblaStatusCapsule] and
 * [QiblaAccuracyPill] anchored at the bottom. The intact top bar lives in the
 * screen, so this view owns only the camera area below it.
 *
 * The bottom HUD is width-capped and centered so it reads as a focused strip on
 * tablets rather than stretching edge-to-edge. A green border glow frames the
 * whole view while facing the Qibla.
 */
@Composable
fun ArQiblaView(
    azimuth: Float,
    qiblaInfo: QiblaInfo?,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    isCompassReady: Boolean,
    compassAccuracy: CompassAccuracy,
    modifier: Modifier = Modifier,
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

        // ──────────────────────────── Bottom HUD ────────────────────────────
        if (qiblaInfo != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .widthIn(max = 380.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QiblaStatusCapsule(
                    qiblaBearing = qiblaInfo.direction.bearing.roundToInt(),
                    isFacingQibla = isFacingQibla,
                    rotationToQibla = rotationToQibla,
                    isCompassReady = isCompassReady,
                    onCamera = true
                )
                QiblaAccuracyPill(accuracy = compassAccuracy)
            }
        }

        // ──────────────────────────── Facing Qibla border glow ───────────────────
        if (isFacingQibla) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            QiblaGreen.copy(alpha = 0.6f),
                            QiblaGreen.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    size = size,
                    style = Stroke(width = 4.dp.toPx())
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
    val arrowColor = if (isFacingQibla) QiblaGreen else QiblaGold

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
//  Qibla marker & edge arrow draw helpers
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

    // Qibla marker – diamond shape
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