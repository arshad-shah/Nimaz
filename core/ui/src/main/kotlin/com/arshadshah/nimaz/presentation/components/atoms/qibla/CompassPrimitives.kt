package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.CompassArtColors
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette
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
val QiblaGold = NimazColors.Gold500

/** Accent used when the user is facing the Qibla. */
val QiblaGreen = NimazColors.Success

/** Red used to mark North across the compass. */
val CompassNorthColor = NimazPalette.Red500

/**
 * Shared Kaaba glyph — a small isometric cube with the dark kiswah body and a
 * coloured hizam band + door. Reused by the compass dial (at the needle tip) and
 * the AR view (at the beam head). Pass [color] = [QiblaGold] while seeking or
 * [QiblaGreen] when facing; [size] is the glyph's bounding width in px and
 * [center] its centre. Set [glow] for a soft radial halo behind it.
 *
 * Drawn upright in the current draw frame, so inside the rotating dial it turns
 * with the dial (like the N/E/S/W letters) and in the AR overlay it stays
 * screen-upright.
 */
fun DrawScope.drawKaabaGlyph(
    center: Offset,
    size: Float,
    color: Color,
    glow: Boolean = true,
) {
    val s = size / 64f

    // glyph art lives in a 64-unit box; its visual centre is ~(36, 32)
    fun p(x: Float, y: Float) = Offset(center.x + (x - 36f) * s, center.y + (y - 32f) * s)
    fun quad(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }

    if (glow) {
        val r = size * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.55f), Color.Transparent),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )
    }

    val bandDark = lerp(color, Color.Black, 0.28f)
    // cube faces (constant dark kiswah)
    drawPath(
        quad(p(20f, 18f), p(34f, 11f), p(52f, 16f), p(38f, 23f)),
        CompassArtColors.NeedleTop
    ) // top
    drawPath(
        quad(p(38f, 23f), p(52f, 16f), p(52f, 46f), p(38f, 53f)),
        CompassArtColors.NeedleSide
    ) // side
    drawPath(
        quad(p(20f, 18f), p(38f, 23f), p(38f, 53f), p(20f, 48f)),
        CompassArtColors.NeedleFront
    ) // front
    // hizam band (front + side) + door, in the accent colour
    drawPath(
        quad(p(20f, 26f), p(38f, 31f), p(38f, 38f), p(20f, 33f)),
        color
    )             // band front
    drawPath(
        quad(p(38f, 31f), p(52f, 24f), p(52f, 31f), p(38f, 38f)),
        bandDark
    )          // band side
    drawPath(quad(p(26f, 38f), p(33f, 40f), p(33f, 50f), p(26f, 48f)), color)             // door
}

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

/**
 * Cardinal direction labels (N/E/S/W) drawn around the fixed dial face. All
 * labels share the neutral on-surface tone — "north is red" now belongs to the
 * compass needle, like a real compass whose printed letters are plain and only
 * the needle's north tip is coloured. North stays a touch larger/bolder purely
 * for orientation.
 */
@Composable
fun DirectionMarkers(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        val directions = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        directions.forEach { (label, angleDeg) ->
            val isNorth = label == "N"
            val style = TextStyle(
                fontSize = if (isNorth) 18.sp else 16.sp,
                fontWeight = if (isNorth) FontWeight.Bold else FontWeight.SemiBold,
                color = labelColor
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

/**
 * Static dial face: the radial background + the ring of degree ticks (every 5°,
 * major every 30°). This layer never rotates — it is the fixed compass card that
 * gives the eye a stable frame of reference. The needles ([CompassNeedles]) and
 * the Kaaba marker are drawn on top as separate, rotating layers.
 */
@Composable
fun CompassDialFace(modifier: Modifier = Modifier) {
    val dialBackground = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
    // Bright majors + clearly-visible medium-grey minors, so the full ring of
    // 5° notches reads against the dark dial (matches the agreed prototype).
    val tickColorMajor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    val tickColorMinor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)

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
    }
}

/**
 * Draws a single compass hand pointing at [angleDeg] (0° = straight up, growing
 * clockwise): a long [frontLength] blade in [frontColor] and a shorter
 * [backLength] counterweight tail in [backColor], meeting at [halfWidth]-wide
 * shoulders over [center]. Used for both the red north needle and the gold
 * Qibla needle.
 */
private fun DrawScope.drawNeedle(
    center: Offset,
    angleDeg: Float,
    frontLength: Float,
    backLength: Float,
    halfWidth: Float,
    frontColor: Color,
    backColor: Color,
) {
    val angleRad = Math.toRadians(angleDeg.toDouble())
    val perpAngle = angleRad + Math.PI / 2

    val shoulderLeftX = center.x + (halfWidth * sin(perpAngle)).toFloat()
    val shoulderLeftY = center.y - (halfWidth * cos(perpAngle)).toFloat()
    val shoulderRightX = center.x - (halfWidth * sin(perpAngle)).toFloat()
    val shoulderRightY = center.y + (halfWidth * cos(perpAngle)).toFloat()

    val frontTipX = center.x + (frontLength * sin(angleRad)).toFloat()
    val frontTipY = center.y - (frontLength * cos(angleRad)).toFloat()
    val backTipX = center.x - (backLength * sin(angleRad)).toFloat()
    val backTipY = center.y + (backLength * cos(angleRad)).toFloat()

    drawPath(
        path = Path().apply {
            moveTo(frontTipX, frontTipY)
            lineTo(shoulderLeftX, shoulderLeftY)
            lineTo(shoulderRightX, shoulderRightY)
            close()
        },
        color = frontColor
    )
    drawPath(
        path = Path().apply {
            moveTo(backTipX, backTipY)
            lineTo(shoulderLeftX, shoulderLeftY)
            lineTo(shoulderRightX, shoulderRightY)
            close()
        },
        color = backColor
    )
}

/**
 * The two spinning needles that pivot on the center pin over the fixed dial,
 * like a real compass. [qiblaScreenAngle] and [northScreenAngle] are already in
 * screen space (0° = the top "AIM" notch, growing clockwise) — the caller bakes
 * in the device heading, so this layer itself never needs the parent to rotate.
 *
 * - **Gold Qibla needle** (the hero): a long blade toward the Qibla carrying the
 *   Kaaba glyph at its tip; turns green when [isFacingQibla].
 * - **Red north needle** (secondary): a thinner, quieter hand pointing at real
 *   north, drawn first so the gold needle sits on top.
 *
 * The pivot pin is drawn last so it caps where both needles cross the center.
 */
@Composable
fun CompassNeedles(
    qiblaScreenAngle: Float,
    northScreenAngle: Float,
    isFacingQibla: Boolean,
    modifier: Modifier = Modifier,
    goldColor: Color = QiblaGold,
) {
    val arrowColor = if (isFacingQibla) QiblaGreen else goldColor
    // Colors read here; MaterialTheme can't be touched inside DrawScope.
    val qiblaTailColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val northTailColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val pinColor = MaterialTheme.colorScheme.outline
    val pinHighlightColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Red north needle — secondary, thinner; drawn under the gold needle.
        drawNeedle(
            center = center,
            angleDeg = northScreenAngle,
            frontLength = radius * 0.72f,
            backLength = radius * 0.56f,
            halfWidth = 4.dp.toPx(),
            frontColor = CompassNorthColor,
            backColor = northTailColor
        )

        // Gold Qibla needle — the hero hand.
        val frontLength = radius - 34.dp.toPx()
        val backLength = radius * 0.40f
        val halfWidth = 9.dp.toPx()
        drawNeedle(
            center = center,
            angleDeg = qiblaScreenAngle,
            frontLength = frontLength,
            backLength = backLength,
            halfWidth = halfWidth,
            frontColor = arrowColor,
            backColor = qiblaTailColor
        )

        // Kaaba glyph at the Qibla needle's front tip (shared with the AR view).
        val qiblaAngleRad = Math.toRadians(qiblaScreenAngle.toDouble())
        val kaabaSize = 36.dp.toPx()
        val kaabaOffset = 13.dp.toPx()
        val kaabaCenterX = center.x + ((frontLength + kaabaOffset) * sin(qiblaAngleRad)).toFloat()
        val kaabaCenterY = center.y - ((frontLength + kaabaOffset) * cos(qiblaAngleRad)).toFloat()
        // Orient the glyph along the needle so it stays "square" to the arrow at
        // any heading instead of staying axis-aligned to the screen frame.
        rotate(degrees = qiblaScreenAngle, pivot = Offset(kaabaCenterX, kaabaCenterY)) {
            drawKaabaGlyph(
                center = Offset(kaabaCenterX, kaabaCenterY),
                size = kaabaSize,
                color = arrowColor,
                glow = isFacingQibla
            )
        }

        // Pivot pin — both needles turn through this, like a real compass.
        drawCircle(color = pinColor, radius = halfWidth * 1.5f, center = center)
        drawCircle(color = pinHighlightColor, radius = halfWidth * 0.65f, center = center)
    }
}

/**
 * Static "lubber line" notch pinned to the top of the compass — the neutral
 * reference that marks where the phone is aimed. You're facing the Qibla when
 * the gold Kaaba needle swings up under this notch. Neutral on purpose: red now
 * belongs to the north needle, and the top no longer means "north."
 */
@Composable
fun CompassLubberNotch(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val notchColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val triSize = 16.dp.toPx()
        val path = Path().apply {
            moveTo(cx, triSize + 2.dp.toPx())
            lineTo(cx - triSize / 2, 2.dp.toPx())
            lineTo(cx + triSize / 2, 2.dp.toPx())
            close()
        }
        drawPath(path = path, color = notchColor)

        val style = TextStyle(
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = notchColor
        )
        val textResult = textMeasurer.measure("AIM", style)
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(cx - textResult.size.width / 2, triSize + 5.dp.toPx())
        )
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
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .background(
                if (isFacingQibla) greenColor
                else MaterialTheme.colorScheme.outline
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isFacingQibla) {
            NimazIcon(
                imageVector = Icons.Default.Mosque,
                contentDescription = null,
                tint = Color.White,
                size = NimazIconSize.SMALL
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

