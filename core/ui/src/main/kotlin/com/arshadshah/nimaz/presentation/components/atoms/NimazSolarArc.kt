package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.foundation.geometry.drawnAltitude
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.NimazToneColors
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * One marked point on a [NimazSolarArc].
 *
 * @param position day fraction in `0f..1f` — 0f is 00:00, 1f is 24:00.
 * @param label drawn above the point, or `null` for a bare dot (sunrise and Maghrib, whose times
 *   the card states anyway).
 * @param contentDescription required, not optional: the arc speaks as one sentence, and an
 *   unnamed dot contributes nothing a reader could act on.
 */
data class NimazSolarNode(
    val position: Float,
    val label: String? = null,
    val tone: NimazTone = NimazTone.ACCENT,
    val contentDescription: String,
)

object NimazSolarArcDefaults {
    /** Tall enough for the day limb, the night troughs and one row of labels. */
    val Height: Dp = 108.dp
}

/** Above this scale six labels collide in the width available, so they drop out. */
private const val LabelDropOutFontScale = 1.5f

/** How many samples the curve is drawn from. 96 is a point every 15 minutes. */
private const val CurveSamples = 96

private val CurveStroke = 3.dp
private val NodeRadius = 3.6.dp
private val SunRadius = 5.5.dp

/**
 * The sun's day as a curve, with the prayers marked where the sun actually puts them.
 *
 * Not a chart *about* the prayer times — a picture of *why they are when they are*. Dhuhr is the
 * apex because solar noon is the apex; sunrise and Maghrib are the horizon crossings; Fajr and
 * Isha sit below the line. See
 * [com.arshadshah.nimaz.presentation.foundation.geometry.solarAltitude] for the geometry, and for
 * why this is a diagram rather than a simulation.
 *
 * @param sunPosition where the sun is now, as a day fraction. `null` draws no sun — the correct
 *   rendering for any day that is not today, and most days a reader looks at are not today.
 * @param litSpan the prayer window the reader is currently inside, brightened along the curve.
 *   This is what makes a separate window band unnecessary.
 */
@Composable
fun NimazSolarArc(
    nodes: List<NimazSolarNode>,
    sunriseFraction: Float,
    sunsetFraction: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    sunPosition: Float? = null,
    litSpan: ClosedFloatingPointRange<Float>? = null,
    height: Dp = NimazSolarArcDefaults.Height,
) {
    val dayColor = NimazToneColors.foreground(NimazTone.ACCENT)
    val duskColor = NimazToneColors.foreground(NimazTone.WARNING)
    val mutedColor = NimazToneColors.foreground(NimazTone.MUTED)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val horizonColor = MaterialTheme.colorScheme.outlineVariant

    val showLabels = LocalDensity.current.fontScale <= LabelDropOutFontScale
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    val safeSun = sunPosition?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
    val safeSpan = litSpan
        ?.takeIf { it.start.isFinite() && it.endInclusive.isFinite() }
        ?.let { minOf(it.start, it.endInclusive)..maxOf(it.start, it.endInclusive) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            // One spoken sentence for the whole drawing. Six dots and four painted labels would
            // otherwise read as a pile of unlabelled nodes.
            .clearAndSetSemantics { this.contentDescription = contentDescription }
    ) {
        val horizonY = size.height * 0.62f
        val labelBand = if (showLabels) size.height * 0.16f else 0f
        val dayHeight = (horizonY - labelBand).coerceAtLeast(1f)
        val nightHeight = (size.height - horizonY).coerceAtLeast(1f)

        fun pointAt(t: Float): Offset {
            val h = drawnAltitude(t, sunriseFraction, sunsetFraction)
            val y = if (h >= 0f) horizonY - h * dayHeight else horizonY - h * nightHeight
            return Offset(t * size.width, y)
        }

        // The horizon: the line that makes Fajr and Isha legible as "before dawn" and "after
        // dusk" rather than as two dots that fell off the curve.
        drawLine(
            color = horizonColor,
            start = Offset(0f, horizonY),
            end = Offset(size.width, horizonY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
        )

        val solidStroke = Stroke(width = CurveStroke.toPx(), cap = StrokeCap.Round)

        // Night and day are two paths, so night can be dashed and muted while day is solid and
        // gradient-filled. Sampling both from the same function keeps them continuous.
        val nightPath = Path()
        val dayPath = Path()
        var nightStarted = false
        var dayStarted = false
        for (i in 0..CurveSamples) {
            val t = i / CurveSamples.toFloat()
            val p = pointAt(t)
            if (drawnAltitude(t, sunriseFraction, sunsetFraction) >= 0f) {
                if (dayStarted) {
                    dayPath.lineTo(p.x, p.y)
                } else {
                    dayPath.moveTo(p.x, p.y)
                    dayStarted = true
                }
                nightStarted = false
            } else {
                if (nightStarted) {
                    nightPath.lineTo(p.x, p.y)
                } else {
                    nightPath.moveTo(p.x, p.y)
                    nightStarted = true
                }
                dayStarted = false
            }
        }

        drawPath(
            path = nightPath,
            color = trackColor,
            style = Stroke(
                width = CurveStroke.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            ),
        )
        drawPath(
            path = dayPath,
            brush = Brush.horizontalGradient(listOf(dayColor, duskColor)),
            style = solidStroke,
        )

        // The current window, brightened over the top of the day limb.
        if (safeSpan != null) {
            val spanPath = Path()
            var started = false
            for (i in 0..CurveSamples) {
                val t = i / CurveSamples.toFloat()
                if (t in safeSpan) {
                    val p = pointAt(t)
                    if (started) {
                        spanPath.lineTo(p.x, p.y)
                    } else {
                        spanPath.moveTo(p.x, p.y)
                        started = true
                    }
                }
            }
            if (started) drawPath(path = spanPath, color = duskColor, style = solidStroke)
        }

        nodes.forEach { node ->
            val t = node.position.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: return@forEach
            val p = pointAt(t)
            drawCircle(
                color = toneColorFor(node.tone, dayColor, duskColor, mutedColor),
                radius = NodeRadius.toPx(),
                center = p,
            )
            val label = node.label
            if (showLabels && label != null) {
                val measured = measurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (p.x - measured.size.width / 2f)
                            .coerceIn(0f, (size.width - measured.size.width).coerceAtLeast(0f)),
                        y = p.y - measured.size.height - 6.dp.toPx(),
                    ),
                )
            }
        }

        if (safeSun != null) {
            val p = pointAt(safeSun)
            // A soft halo, then the disc: the halo is what stops the sun reading as a seventh
            // prayer marker.
            drawCircle(
                color = duskColor.copy(alpha = 0.25f),
                radius = SunRadius.toPx() * 2.2f,
                center = p,
            )
            drawCircle(color = duskColor, radius = SunRadius.toPx(), center = p)
        }
    }
}

/**
 * Tones resolved inside a draw scope would need a composable context there, so the three colours
 * the arc actually uses are resolved outside and mapped here.
 */
private fun toneColorFor(tone: NimazTone, day: Color, dusk: Color, muted: Color): Color =
    when (tone) {
        NimazTone.ACCENT, NimazTone.PROMINENT -> day
        NimazTone.WARNING, NimazTone.ERROR -> dusk
        else -> muted
    }

// ==================== PREVIEWS ====================

@Composable
private fun ShowcaseLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NimazSolarArcShowcase() {
    val september = listOf(
        NimazSolarNode(0.22f, "Fajr", NimazTone.MUTED, "Fajr at 05:12"),
        NimazSolarNode(0.27f, null, NimazTone.ACCENT, "Sunrise at 06:48"),
        NimazSolarNode(0.55f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr at 13:22"),
        NimazSolarNode(0.72f, "Asr", NimazTone.WARNING, "Asr at 17:13"),
        NimazSolarNode(0.80f, null, NimazTone.WARNING, "Maghrib at 20:04"),
        NimazSolarNode(0.90f, "Isha", NimazTone.MUTED, "Isha at 21:38"),
    )
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ShowcaseLabel("Today — inside Dhuhr's window")
        NimazSolarArc(
            september, 0.27f, 0.80f, "The sun's day",
            sunPosition = 0.62f, litSpan = 0.55f..0.72f,
        )

        ShowcaseLabel("Another day — no sun")
        NimazSolarArc(september, 0.27f, 0.80f, "The sun's day")

        ShowcaseLabel("December — a short day and a deep night")
        NimazSolarArc(september, 0.35f, 0.70f, "The sun's day", sunPosition = 0.5f)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazSolarArc — Light")
@Composable
private fun NimazSolarArcLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazSolarArcShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazSolarArc — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun NimazSolarArcDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazSolarArcShowcase() }
}
