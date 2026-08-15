package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGold
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaGreen
import com.arshadshah.nimaz.presentation.foundation.geometry.drawArcToKaaba
import com.arshadshah.nimaz.presentation.foundation.geometry.drawBeam
import kotlin.math.abs

private const val HALF_FOV = 30f

/**
 * AR overlay drawn on top of the camera feed: a vertical beam of light pointing
 * at the Qibla while it is on-screen, or a sweeping arc toward the edge when the
 * Qibla is outside the camera's field of view.
 *
 * Draw functions ([drawBeam], [drawArcToKaaba]) live in
 * `foundation.geometry.QiblaGeometry`.
 */
@Composable
fun QiblaArOverlay(
    qiblaInfo: QiblaInfo?,
    isFacingQibla: Boolean,
    rotationToQibla: Float,
    compassAccuracy: CompassAccuracy,
    modifier: Modifier = Modifier,
) {
    val lowAccuracy = compassAccuracy == CompassAccuracy.LOW ||
            compassAccuracy == CompassAccuracy.UNRELIABLE
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
                x = arrowX.coerceIn(48f, size.width - 48f),
                color = color,
                isFacing = isFacingQibla
            )
        } else {
            drawArcToKaaba(pointRight = rotationToQibla > 0, color = color)
        }
    }
}
