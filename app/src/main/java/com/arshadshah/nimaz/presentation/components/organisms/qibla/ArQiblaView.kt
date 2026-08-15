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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGold
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaArOverlay
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaFactsRow
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaInstructionRow
import kotlin.math.abs

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
    animatedAzimuth: Float,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
        QiblaArOverlay(
            qiblaInfo = qiblaInfo,
            isFacingQibla = isFacingQibla,
            rotationToQibla = rotationToQibla,
            compassAccuracy = compassAccuracy,
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
                text = if (turnRight) stringResource(R.string.qibla_turn_right_hint)
                else stringResource(R.string.qibla_turn_left_hint),
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
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .widthIn(max = 380.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QiblaFactsRow(
                    qiblaBearing = qiblaInfo.direction.bearing.toFloat(),
                    currentAzimuth = animatedAzimuth,
                    modifier = Modifier.fillMaxWidth(),
                )
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

