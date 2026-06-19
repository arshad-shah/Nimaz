package com.arshadshah.nimaz.presentation.components.organisms.qibla

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGold
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaStatusCapsule
import com.arshadshah.nimaz.presentation.components.atoms.qibla.drawKaabaGlyph
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaAccuracyPill
import kotlin.math.abs
import kotlin.math.roundToInt

/** Half of the assumed camera horizontal field of view, in degrees. Beyond this
 *  the Qibla is off the screen and we switch from the beam to the edge arrow. */
private const val HALF_FOV = 30f

/**
 * The AR Qibla view: a full-bleed back-camera feed with a "beam of light"
 * pointing at the real-world Qibla, topped by the shared Kaaba glyph (gold while
 * seeking, green when facing). When the Qibla is outside the camera's view the
 * beam is replaced by an arc that sweeps toward a Kaaba glyph at the edge, with a
 * bold "turn this way" instruction. The shared [QiblaStatusCapsule] and
 * [QiblaAccuracyPill] sit at the bottom; a green border frames the view on lock.
 *
 * There is no heading ruler — the beam shows direction and the capsule the turn,
 * so nothing is duplicated.
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

    val lowAccuracy = compassAccuracy == CompassAccuracy.LOW ||
        compassAccuracy == CompassAccuracy.UNRELIABLE
    val qiblaOffScreen = qiblaInfo != null && abs(rotationToQibla) > HALF_FOV
    val turnRight = rotationToQibla > 0

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
                    cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (_: Exception) {
                    // Camera may not be available
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))

            onDispose { cameraProvider?.unbindAll() }
        }

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // ──────────────────────────── AR Overlay (beam / arc + Kaaba) ────────────
        ArOverlay(
            qiblaInfo = qiblaInfo,
            isFacingQibla = isFacingQibla,
            rotationToQibla = rotationToQibla,
            lowAccuracy = lowAccuracy,
            modifier = Modifier.fillMaxSize()
        )

        // ──────────────────────── Off-screen instruction overlay ─────────────────
        if (qiblaOffScreen) {
            // Soft directional wash on the edge you must turn toward.
            Box(
                modifier = Modifier
                    .align(if (turnRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(110.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = if (turnRight)
                                listOf(Color.Transparent, QiblaGold.copy(alpha = 0.16f))
                            else
                                listOf(QiblaGold.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
            )
            Text(
                text = if (turnRight) "Turn right\nto find Qibla" else "Turn left\nto find Qibla",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                textAlign = if (turnRight) TextAlign.Start else TextAlign.End,
                modifier = Modifier
                    .align(if (turnRight) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 22.dp)
            )
        }

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

        // ──────────────────────── Facing Qibla border glow ───────────────────────
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
//  AR Overlay — the beam of light (or off-screen arc) crowned by the Kaaba glyph
// =====================================================================================
@Composable
private fun ArOverlay(
    qiblaInfo: QiblaInfo?,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    lowAccuracy: Boolean,
    modifier: Modifier = Modifier,
) {
    val base = if (isFacingQibla) QiblaGreen else QiblaGold
    val color = if (lowAccuracy) base.copy(alpha = 0.45f) else base

    Canvas(modifier = modifier) {
        if (qiblaInfo == null) return@Canvas

        val centerX = size.width / 2
        val pixelsPerDegree = size.width / (HALF_FOV * 2f)
        val arrowX = centerX + rotationToQibla * pixelsPerDegree
        val onScreen = abs(rotationToQibla) <= HALF_FOV

        if (onScreen) {
            drawBeam(
                x = arrowX.coerceIn(48.dp.toPx(), size.width - 48.dp.toPx()),
                color = color,
                isFacing = isFacingQibla
            )
        } else {
            drawArcToKaaba(pointRight = rotationToQibla > 0, color = color)
        }
    }
}

/** The vertical beam of light rising to the Kaaba glyph at the Qibla's position. */
private fun DrawScope.drawBeam(x: Float, color: Color, isFacing: Boolean) {
    val topY = size.height * 0.34f
    val botY = size.height * 0.80f
    val topHalf = 5.dp.toPx()
    val botHalf = (if (isFacing) 16f else 12f).dp.toPx()

    // Beam body — bright at the top (near the Kaaba), fading into the floor.
    drawPath(
        path = Path().apply {
            moveTo(x - botHalf, botY)
            lineTo(x - topHalf, topY)
            lineTo(x + topHalf, topY)
            lineTo(x + botHalf, botY)
            close()
        },
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = color.alpha * 0.55f), Color.Transparent),
            startY = topY,
            endY = botY
        )
    )

    // Base footprint — a soft glow pool where the beam meets the ground (facing).
    if (isFacing) {
        drawOval(
            color = color.copy(alpha = 0.22f),
            topLeft = Offset(x - botHalf * 3f, botY - 8.dp.toPx()),
            size = Size(botHalf * 6f, 16.dp.toPx())
        )
        drawOval(
            color = color.copy(alpha = 0.35f),
            topLeft = Offset(x - botHalf * 1.8f, botY - 5.dp.toPx()),
            size = Size(botHalf * 3.6f, 10.dp.toPx())
        )
    }

    // The Kaaba, crowning the beam.
    drawKaabaGlyph(
        center = Offset(x, topY),
        size = (if (isFacing) 46f else 40f).dp.toPx(),
        color = color,
        glow = true
    )
}

/** Off-screen indicator: an arc that sweeps the eye toward a Kaaba glyph hugging
 *  the edge you need to turn toward. */
private fun DrawScope.drawArcToKaaba(pointRight: Boolean, color: Color) {
    val edgeInset = 56.dp.toPx()
    val kx = if (pointRight) size.width - edgeInset else edgeInset
    val ky = size.height * 0.32f

    val startY = size.height * 0.42f
    val endY = size.height * 0.78f
    val bulge = if (pointRight) -84.dp.toPx() else 84.dp.toPx()

    drawPath(
        path = Path().apply {
            moveTo(kx, startY)
            quadraticTo(kx + bulge, (startY + endY) / 2f, kx, endY)
        },
        brush = Brush.verticalGradient(
            colors = listOf(color, color.copy(alpha = 0.05f)),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )

    drawKaabaGlyph(
        center = Offset(kx, ky),
        size = 44.dp.toPx(),
        color = color,
        glow = true
    )
}
