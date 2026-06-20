package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The festive, fully Canvas-drawn backdrop for the Qaida lesson-complete moment
 * — no emoji, no images. Four layers stack into a celebratory "burst" that the
 * earned stars sit on top of:
 *
 * 1. a soft **gold halo** that gently pulses (the warm glow),
 * 2. a slowly rotating **sunburst** of alternating long/short gold rays,
 * 3. a static **eight-point Islamic star** (khatim) outline in teal, tying the
 *    moment to the app's Islamic-art language, and
 * 4. a scatter of **twinkling four-point sparkles** that fade in and out on
 *    their own phases.
 *
 * All motion runs off a single [rememberInfiniteTransition]; set [play] = false
 * (or rely on previews, which render a still frame) for a static rendering.
 * Colours default to the theme gold ([MaterialTheme.colorScheme.secondary]) and
 * teal ([MaterialTheme.colorScheme.primary]).
 */
@Composable
fun QaidaCelebrationBurst(
    modifier: Modifier = Modifier,
    play: Boolean = true,
    gold: Color = MaterialTheme.colorScheme.secondary,
    teal: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "celebration")
    val rayAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (play) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "rays",
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (play) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )
    val haloPulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (play) 1.08f else 0.92f,
        animationSpec = infiniteRepeatable(
            tween(1_900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "halo",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f

        // 1. Soft pulsing gold halo.
        val haloR = r * haloPulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    gold.copy(alpha = 0.38f),
                    gold.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = haloR,
            ),
            radius = haloR,
            center = center,
        )

        // 2. Rotating sunburst — alternating long/short rays.
        rotate(degrees = rayAngle, pivot = center) {
            val rays = 16
            for (i in 0 until rays) {
                val major = i % 2 == 0
                val a = (i * (360f / rays)) * (PI.toFloat() / 180f)
                val inner = r * 0.32f
                val outer = if (major) r * 0.95f else r * 0.66f
                drawLine(
                    color = gold.copy(alpha = if (major) 0.55f else 0.22f),
                    start = Offset(center.x + inner * sin(a), center.y - inner * cos(a)),
                    end = Offset(center.x + outer * sin(a), center.y - outer * cos(a)),
                    strokeWidth = if (major) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        // 3. Static eight-point Islamic star (two overlaid squares) outline.
        val starR = r * 0.52f
        drawPath(
            path = eightPointStarPath(center, starR),
            color = teal.copy(alpha = 0.40f),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        // 4. Twinkling four-point sparkles at fixed normalized positions.
        for (s in SPARKLES) {
            val phase = (twinkle + s.phase) % 1f
            val tw = sin(phase * PI.toFloat()).coerceAtLeast(0f) // 0→1→0
            val pos = Offset(center.x + s.dx * r, center.y + s.dy * r)
            drawSparkle(
                center = pos,
                radius = s.scale * r * (0.55f + 0.45f * tw),
                color = gold.copy(alpha = 0.25f + 0.65f * tw),
            )
        }
    }
}

/** A fixed sparkle placement: normalized offset from centre, base scale, twinkle phase. */
private data class Sparkle(val dx: Float, val dy: Float, val scale: Float, val phase: Float)

private val SPARKLES = listOf(
    Sparkle(-0.70f, -0.55f, 0.10f, 0.00f),
    Sparkle(0.66f, -0.60f, 0.13f, 0.30f),
    Sparkle(0.80f, 0.18f, 0.09f, 0.55f),
    Sparkle(-0.78f, 0.30f, 0.11f, 0.70f),
    Sparkle(0.30f, 0.78f, 0.08f, 0.15f),
    Sparkle(-0.34f, 0.74f, 0.12f, 0.45f),
    Sparkle(0.05f, -0.82f, 0.09f, 0.85f),
)

/**
 * Draws a four-point sparkle (two perpendicular tapered spikes meeting at
 * [center]) — the classic "twinkle" glyph, hand-built so it carries the accent
 * colour and animates, with no emoji or asset.
 */
private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val waist = radius * 0.18f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)        // top
        lineTo(center.x + waist, center.y - waist)
        lineTo(center.x + radius, center.y)        // right
        lineTo(center.x + waist, center.y + waist)
        lineTo(center.x, center.y + radius)        // bottom
        lineTo(center.x - waist, center.y + waist)
        lineTo(center.x - radius, center.y)        // left
        lineTo(center.x - waist, center.y - waist)
        close()
    }
    drawPath(path, color = color)
}

/**
 * Builds an eight-point star (khatim) as two overlaid squares rotated 45° from
 * each other, expressed as a single star polygon with [outerRadius] points and
 * inner valleys at ~0.72× the radius.
 */
private fun eightPointStarPath(center: Offset, outerRadius: Float): Path {
    val innerRadius = outerRadius * 0.72f
    val points = 8
    return Path().apply {
        for (i in 0 until points * 2) {
            val isOuter = i % 2 == 0
            val rr = if (isOuter) outerRadius else innerRadius
            val a = (i * (180f / points)) * (PI.toFloat() / 180f)
            val x = center.x + rr * sin(a)
            val y = center.y - rr * cos(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

@Preview(showBackground = true, widthDp = 220, heightDp = 220, name = "Celebration Burst")
@Composable
private fun QaidaCelebrationBurstPreview() {
    NimazTheme {
        QaidaCelebrationBurst(modifier = Modifier.size(200.dp), play = false)
    }
}

@Preview(
    showBackground = true, widthDp = 220, heightDp = 220, name = "Celebration Burst - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QaidaCelebrationBurstDarkPreview() {
    NimazTheme {
        QaidaCelebrationBurst(modifier = Modifier.size(200.dp), play = false)
    }
}
