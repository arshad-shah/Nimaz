package com.arshadshah.nimaz.presentation.foundation.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.CompassArtColors
import java.util.Locale
import kotlin.math.abs

/**
 * Pure draw-scope geometry helpers shared by Qibla screens. No Composable
 * state, no theme reads — safe to call from any [DrawScope].
 */

/**
 * Shared Kaaba glyph — a small isometric cube with the dark kiswah body and a
 * coloured hizam band + door. Reused by the compass dial (at the needle tip) and
 * the AR view (at the beam head). Pass [color] = QiblaGold while seeking or
 * QiblaGreen when facing; [size] is the glyph's bounding width in px and
 * [center] its centre. Set [glow] for a soft radial halo behind it.
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

/** The vertical beam of light rising to the Kaaba glyph at the Qibla's position. */
internal fun DrawScope.drawBeam(x: Float, color: Color, isFacing: Boolean) {
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
internal fun DrawScope.drawArcToKaaba(pointRight: Boolean, color: Color) {
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
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )

    drawKaabaGlyph(
        center = Offset(kx, ky),
        size = 44.dp.toPx(),
        color = color,
        glow = true
    )
}

/** Formats a coordinate pair like "51.5074°N, 0.1278°W". */
fun formatCoordinates(latitude: Double, longitude: Double): String {
    val ns = if (latitude >= 0) "N" else "S"
    val ew = if (longitude >= 0) "E" else "W"
    val locale = Locale.getDefault()
    val lat = String.format(locale, "%.4f", abs(latitude))
    val lon = String.format(locale, "%.4f", abs(longitude))
    return "$lat°$ns, $lon°$ew"
}
