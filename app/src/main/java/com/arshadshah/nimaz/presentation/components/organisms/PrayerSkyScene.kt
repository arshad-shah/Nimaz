package com.arshadshah.nimaz.presentation.components.organisms

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A living, time-aware "sky scene" for the prayer-times hero.
 *
 * The sky gradient, the sun's east→west arc and the moon's phase are all
 * derived continuously from [timeOfDay] (0f = midnight … 1f = next midnight),
 * so the sun sits exactly where it should at any moment, not just at the
 * prayer anchors. Clouds drift slowly across.
 *
 * Performance: the heavy work — gradients, blurred corona/rays, the moon's
 * [PathOperation] and soft terminator — is baked once into a small
 * [ImageBitmap] (via [drawWithCache], keyed on [timeOfDay]/[moonFraction]) and
 * only rebuilt when those change. The cloud sprite is baked once too; the only
 * per-frame work is blitting the two bitmaps, with the clouds offset by a
 * single animated float and recoloured by a [ColorFilter]. Bitmaps render at
 * [SPRITE_SCALE] of the view size to keep the footprint small.
 *
 * @param timeOfDay fraction through a 24h day, 0f→1f.
 * @param moonFraction synodic phase 0f→1f (see [MoonPhase]); used at night.
 * @param cloudsEnabled set false to freeze cloud motion (e.g. battery saver).
 */
@Composable
fun SkyBackground(
    timeOfDay: Float,
    moonFraction: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    cloudsEnabled: Boolean = true,
) {
    val drift = rememberInfiniteTransition(label = "sky")
    val cloudPhase by drift.animateFloat(
        initialValue = 0f,
        targetValue = if (cloudsEnabled) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(120_000, easing = LinearEasing), RepeatMode.Restart),
        label = "clouds",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawWithCache {
                val w = size.width.roundToInt().coerceAtLeast(1)
                val h = size.height.roundToInt().coerceAtLeast(1)
                val sw = (size.width * SPRITE_SCALE).roundToInt().coerceAtLeast(1)
                val sh = (size.height * SPRITE_SCALE).roundToInt().coerceAtLeast(1)

                // Baked once per (timeOfDay, moonFraction, size) change — not per frame.
                val scene = bakeLayer(sw, sh, this, layoutDirection) { drawScene(timeOfDay, moonFraction) }
                val clouds = bakeLayer(sw, sh, this, layoutDirection) { drawCloudLayer() }
                val cloudTint = ColorFilter.tint(sampleCloud(timeOfDay), BlendMode.Modulate)
                val full = IntSize(w, h)

                onDrawBehind {
                    val off = (cloudPhase * w).roundToInt()
                    drawImage(scene, dstOffset = IntOffset.Zero, dstSize = full)
                    drawImage(clouds, dstOffset = IntOffset(off, 0), dstSize = full, colorFilter = cloudTint)
                    drawImage(clouds, dstOffset = IntOffset(off - w, 0), dstSize = full, colorFilter = cloudTint)
                }
            },
    )
}

/**
 * The [SkyBackground] with a time + status label overlaid (top-left). Used by
 * the dedicated Prayer Times screen.
 */
@Composable
fun PrayerSkyScene(
    timeOfDay: Float,
    timeLabel: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
    moonFraction: Float = 0.5f,
    shape: Shape = RoundedCornerShape(20.dp),
    cloudsEnabled: Boolean = true,
) {
    Box(modifier = modifier) {
        SkyBackground(
            timeOfDay = timeOfDay,
            moonFraction = moonFraction,
            modifier = Modifier.matchParentSize(),
            shape = shape,
            cloudsEnabled = cloudsEnabled,
        )
        Column(modifier = Modifier.padding(16.dp)) {
            val shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 1f), blurRadius = 4f)
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.titleLarge.copy(shadow = shadow),
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium.copy(shadow = shadow),
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

private const val SPRITE_SCALE = 0.6f
private const val SUNRISE_T = 0.27f
private const val SUNSET_T = 0.80f

/**
 * Moon phase from the date, after Jean Meeus, *Astronomical Algorithms*.
 * A synodic approximation (accurate to ~a day) — ample for a UI moon.
 */
object MoonPhase {
    private const val SYNODIC_MONTH = 29.53058867
    private const val NEW_MOON_REFERENCE_JD = 2451550.1 // 2000-01-06, Meeus lunation 0

    /** Synodic phase 0f (new) → 0.5f (full) → 1f (new) for an epoch-millis instant. */
    fun fractionForEpochMillis(epochMillis: Long): Float {
        val julianDay = epochMillis / 86_400_000.0 + 2_440_587.5
        var age = (julianDay - NEW_MOON_REFERENCE_JD) % SYNODIC_MONTH
        if (age < 0) age += SYNODIC_MONTH
        return (age / SYNODIC_MONTH).toFloat()
    }

    /** Illuminated fraction 0f→1f from a synodic [fraction]. */
    fun illumination(fraction: Float): Float =
        ((1 - cos(2 * PI * fraction)) / 2).toFloat()
}

// ─────────────────────────────────────────────────────────────────────────
// Scene composition (baked)
// ─────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawScene(t: Float, moonFraction: Float) {
    drawRect(Brush.verticalGradient(*skyStops(t), startY = 0f, endY = size.height))

    val night = nightFactor(t)
    if (night > 0f) {
        drawNight(night)
        drawMoon(Offset(size.width * 0.72f, size.height * 0.32f), size.minDimension * 0.16f, moonFraction, alpha = night)
    }

    // Continuous sun: altitude is sin() of the day-fraction, so it rises from
    // and sinks below the horizon smoothly. Drawn (fading) whenever it's at or
    // just under the horizon — so Maghrib (sunset) shows it sitting on it.
    val td = (t - SUNRISE_T) / (SUNSET_T - SUNRISE_T)
    val alt = sin(PI * td).toFloat()
    if (alt > -0.18f) {
        val sunAlpha = if (alt >= 0f) 1f else ((alt + 0.18f) / 0.18f).coerceIn(0f, 1f)
        drawSunAt(td, alt, sunAlpha)
    }
}

/** How "night" it is: 1f deep night, 0f full day, ramped through dawn/dusk. */
private fun nightFactor(t: Float): Float = when {
    t < 0.20f -> 1f
    t < SUNRISE_T -> (SUNRISE_T - t) / (SUNRISE_T - 0.20f)
    t < SUNSET_T -> 0f
    t < 0.87f -> (t - SUNSET_T) / (0.87f - SUNSET_T)
    else -> 1f
}.coerceIn(0f, 1f)

private fun DrawScope.drawSunAt(td: Float, alt: Float, sunAlpha: Float) {
    val w = size.width
    val h = size.height
    val sunX = lerpF(0.16f, 0.84f, td.coerceIn(0f, 1f)) * w
    val horizonY = h * 0.92f
    val apexY = h * 0.16f
    val sunY = horizonY - alt * (horizonY - apexY) // alt<0 sinks it below the frame
    val radius = size.minDimension * 0.085f
    val warm = (1f - alt).coerceIn(0f, 1f) // warmer near the horizon

    val diskMid = lerp(Color(0xFFFFF1A8), Color(0xFFFFCF87), warm)
    val diskRim = lerp(Color(0xFFFACC15), Color(0xFFF97316), warm)
    val coronaIn = lerp(Color(0xFFFFF6C2), Color(0xFFFFE0A0), warm)

    drawSun(
        center = Offset(sunX, sunY),
        radius = radius,
        disk = listOf(0f to Color.White, 0.7f to diskMid, 0.95f to diskRim, 1f to diskRim.copy(alpha = 0f)),
        corona = listOf(
            0f to coronaIn.copy(alpha = 0.85f), 0.34f to coronaIn.copy(alpha = 0.32f),
            0.7f to coronaIn.copy(alpha = 0.1f), 1f to coronaIn.copy(alpha = 0f),
        ),
        coronaScale = 2.7f,
        rayColor = coronaIn, rayCount = 5, rayLengthFr = 0.5f + 0.15f * alt.coerceAtLeast(0f), rayAlpha = 0.15f,
        alpha = sunAlpha,
    )
}

private fun DrawScope.drawNight(nf: Float) {
    val w = size.width
    val h = size.height
    drawIntoCanvas { c ->
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color(0xFF7E8AD6).copy(alpha = 0.1f * nf).toArgb()
            maskFilter = BlurMaskFilter(size.minDimension * 0.08f, BlurMaskFilter.Blur.NORMAL)
        }
        c.nativeCanvas.save()
        c.nativeCanvas.rotate(-18f, w * 0.42f, h * 0.5f)
        c.nativeCanvas.drawOval(RectF(w * 0.42f - w * 0.6f, h * 0.5f - h * 0.13f, w * 0.42f + w * 0.6f, h * 0.5f + h * 0.13f), paint)
        c.nativeCanvas.restore()
    }
    val stars = listOf(
        Triple(0.11f, 0.20f, 1.4f), Triple(0.26f, 0.14f, 1f), Triple(0.36f, 0.32f, 1.1f),
        Triple(0.83f, 0.18f, 1.3f), Triple(0.92f, 0.38f, 1f), Triple(0.55f, 0.21f, 1f),
        Triple(0.16f, 0.56f, 0.9f), Triple(0.46f, 0.48f, 1f),
    )
    stars.forEach { (fx, fy, r) ->
        drawCircle(Color.White.copy(alpha = 0.8f * nf), radius = r, center = Offset(w * fx, h * fy))
    }
    // one sparkle
    val sx = w * 0.42f
    val sy = h * 0.62f
    val sr = size.minDimension * 0.018f
    val sc = Color.White.copy(alpha = 0.85f * nf)
    drawLine(sc, Offset(sx - sr, sy), Offset(sx + sr, sy), strokeWidth = 1f)
    drawLine(sc, Offset(sx, sy - sr), Offset(sx, sy + sr), strokeWidth = 1f)
}

private fun DrawScope.drawCloudLayer() {
    // White→grey luminance so a per-phase ColorFilter.tint shades them correctly.
    val w = size.width
    val h = size.height
    drawCloud(w * 0.25f, h * 0.36f, scale(), Color.White, Color(0xFFAFAFAF), 0.95f)
    drawCloud(w * 0.6f, h * 0.24f, scale() * 0.8f, Color.White, Color(0xFFAFAFAF), 0.9f)
    drawCloud(w * 0.78f, h * 0.5f, scale() * 0.7f, Color.White, Color(0xFFAFAFAF), 0.85f)
}

// ─────────────────────────────────────────────────────────────────────────
// Time → colour sampling
// ─────────────────────────────────────────────────────────────────────────

private class SkyKey(val t: Float, val sky: List<Color>, val cloud: Color)

private val SKY_KEYS = listOf(
    SkyKey(0.00f, listOf(Color(0xFF03060F), Color(0xFF0A0F26), Color(0xFF141A38), Color(0xFF1B1F4A), Color(0xFF33285E)), Color(0xFF2A2F52)),
    SkyKey(0.20f, listOf(Color(0xFF060A1C), Color(0xFF16204A), Color(0xFF3B3270), Color(0xFF8A4F6E), Color(0xFFD08A5E)), Color(0xFF7A5A72)),
    SkyKey(0.28f, listOf(Color(0xFF2B3A8C), Color(0xFF7C6AB0), Color(0xFFE59AB0), Color(0xFFFBB778), Color(0xFFFFE0A3)), Color(0xFFFCE0CE)),
    SkyKey(0.50f, listOf(Color(0xFF0A2E7A), Color(0xFF1E62D6), Color(0xFF4F9BF5), Color(0xFFBFE0FB), Color(0xFFEAF6FF)), Color(0xFFF2F7FF)),
    SkyKey(0.67f, listOf(Color(0xFF15407F), Color(0xFF3E78C9), Color(0xFF8FB6E8), Color(0xFFF2D9A8), Color(0xFFFBE3B0)), Color(0xFFFBEBCF)),
    SkyKey(0.80f, listOf(Color(0xFF241056), Color(0xFF7A1E83), Color(0xFFD6356B), Color(0xFFF9733A), Color(0xFFFBD34D)), Color(0xFFF2B488)),
    SkyKey(0.87f, listOf(Color(0xFF04060F), Color(0xFF0E1330), Color(0xFF241A45), Color(0xFF33285E), Color(0xFF3A2A55)), Color(0xFF3A3F66)),
    SkyKey(1.00f, listOf(Color(0xFF03060F), Color(0xFF0A0F26), Color(0xFF141A38), Color(0xFF1B1F4A), Color(0xFF33285E)), Color(0xFF2A2F52)),
)

private fun bracket(t: Float): Triple<SkyKey, SkyKey, Float> {
    val tt = t.coerceIn(0f, 1f)
    for (i in 0 until SKY_KEYS.size - 1) {
        val a = SKY_KEYS[i]
        val b = SKY_KEYS[i + 1]
        if (tt in a.t..b.t) {
            val f = if (b.t > a.t) (tt - a.t) / (b.t - a.t) else 0f
            return Triple(a, b, f)
        }
    }
    return Triple(SKY_KEYS.last(), SKY_KEYS.last(), 0f)
}

private fun skyStops(t: Float): Array<Pair<Float, Color>> {
    val (a, b, f) = bracket(t)
    val c = List(5) { lerp(a.sky[it], b.sky[it], f) }
    return arrayOf(0f to c[0], 0.4f to c[1], 0.66f to c[2], 0.88f to c[3], 1f to c[4])
}

private fun sampleCloud(t: Float): Color {
    val (a, b, f) = bracket(t)
    return lerp(a.cloud, b.cloud, f)
}

private fun lerpF(a: Float, b: Float, f: Float): Float = a + (b - a) * f

// ─────────────────────────────────────────────────────────────────────────
// Drawing primitives
// ─────────────────────────────────────────────────────────────────────────

/** Renders [block] into a fresh [ImageBitmap] of the given pixel size. */
private fun bakeLayer(wPx: Int, hPx: Int, density: Density, ld: LayoutDirection, block: DrawScope.() -> Unit): ImageBitmap {
    val image = ImageBitmap(wPx, hPx)
    CanvasDrawScope().draw(density, ld, Canvas(image), Size(wPx.toFloat(), hPx.toFloat()), block)
    return image
}

private fun DrawScope.scale(): Float = size.width / 360f

private fun DrawScope.drawSun(
    center: Offset,
    radius: Float,
    disk: List<Pair<Float, Color>>,
    corona: List<Pair<Float, Color>>,
    coronaScale: Float,
    rayColor: Color,
    rayCount: Int,
    rayLengthFr: Float,
    rayAlpha: Float,
    alpha: Float = 1f,
) {
    if (rayCount > 0 && rayAlpha * alpha > 0.01f) {
        val rayLength = size.minDimension * rayLengthFr
        drawIntoCanvas { c ->
            val paint = Paint().apply {
                isAntiAlias = true
                color = rayColor.copy(alpha = rayAlpha * alpha).toArgb()
                maskFilter = BlurMaskFilter(radius * 0.7f, BlurMaskFilter.Blur.NORMAL)
            }
            val wb = radius * 0.42f
            val wt = radius * 0.9f
            val shaft = android.graphics.Path().apply {
                moveTo(center.x - wb, center.y)
                lineTo(center.x + wb, center.y)
                lineTo(center.x + wt, center.y - rayLength)
                lineTo(center.x - wt, center.y - rayLength)
                close()
            }
            for (i in 0 until rayCount) {
                c.nativeCanvas.save()
                c.nativeCanvas.rotate(i * 360f / rayCount + 12f, center.x, center.y)
                c.nativeCanvas.drawPath(shaft, paint)
                c.nativeCanvas.restore()
            }
        }
    }
    drawCircle(Brush.radialGradient(*fade(corona, alpha), center = center, radius = radius * coronaScale), radius = radius * coronaScale, center = center)
    drawCircle(Brush.radialGradient(*fade(disk, alpha), center = center, radius = radius), radius = radius, center = center)
}

private fun fade(stops: List<Pair<Float, Color>>, alpha: Float): Array<Pair<Float, Color>> =
    stops.map { it.first to it.second.copy(alpha = it.second.alpha * alpha) }.toTypedArray()

private fun DrawScope.drawCloud(cx: Float, cy: Float, s: Float, top: Color, bottom: Color, alpha: Float) {
    drawIntoCanvas { c ->
        val paint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(0f, cy - 18f * s, 0f, cy + 16f * s, top.toArgb(), bottom.toArgb(), Shader.TileMode.CLAMP)
            maskFilter = BlurMaskFilter(2f * s + 1f, BlurMaskFilter.Blur.NORMAL)
            this.alpha = (alpha * 255).toInt()
        }
        val dir = android.graphics.Path.Direction.CW
        val p = android.graphics.Path().apply {
            addOval(RectF(cx - 30f * s, cy - 12f * s, cx + 30f * s, cy + 12f * s), dir)
            addCircle(cx - 22f * s, cy - 2f * s, 14f * s, dir)
            addCircle(cx - 6f * s, cy - 12f * s, 16f * s, dir)
            addCircle(cx + 12f * s, cy - 8f * s, 13f * s, dir)
            addCircle(cx + 24f * s, cy - 1f * s, 15f * s, dir)
            addRoundRect(RectF(cx - 44f * s, cy, cx + 44f * s, cy + 16f * s), 8f * s, 8f * s, dir)
        }
        c.nativeCanvas.drawPath(p, paint)
    }
}

private fun DrawScope.drawMoon(center: Offset, radius: Float, fraction: Float, alpha: Float) {
    val cx = center.x
    val cy = center.y
    val r = radius

    drawCircle(Brush.radialGradient(0f to Color(0xFFC7D2FE).copy(alpha = 0.38f * alpha), 1f to Color(0x00C7D2FE), center = center, radius = r * 1.5f), radius = r * 1.5f, center = center)
    drawCircle(Brush.radialGradient(0f to Color(0xFF262C4C), 1f to Color(0xFF10142C), center = Offset(cx, cy - r * 0.05f), radius = r), radius = r, center = center)

    val rg = r * 1.06f
    val cosv = cos(2 * PI * fraction).toFloat()
    val rx = rg * abs(cosv)
    val waxing = fraction < 0.5f
    val crescent = cosv > 0f
    val lit = litRegion(cx, cy, rg, rx, waxing, crescent)

    clipPath(circlePath(cx, cy, r)) {
        drawIntoCanvas { c ->
            val paint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    cx - r * 0.3f, cy - r * 0.34f, r * 1.4f,
                    intArrayOf(Color(0xFFFFFFFF).toArgb(), Color(0xFFE9EDF6).toArgb(), Color(0xFFC5CCDE).toArgb(), Color(0xFFAAB2CC).toArgb()),
                    floatArrayOf(0f, 0.55f, 0.85f, 1f),
                    Shader.TileMode.CLAMP,
                )
                maskFilter = BlurMaskFilter(r * 0.12f, BlurMaskFilter.Blur.NORMAL)
                this.alpha = (alpha * 255).toInt()
            }
            c.nativeCanvas.drawPath(lit.asAndroidPath(), paint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Moon geometry
// ─────────────────────────────────────────────────────────────────────────

private fun circlePath(cx: Float, cy: Float, r: Float): Path =
    Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }

private fun ovalPath(cx: Float, cy: Float, rx: Float, ry: Float): Path =
    Path().apply { addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }

private fun rectPath(rect: Rect): Path = Path().apply { addRect(rect) }

private fun combine(a: Path, b: Path, op: PathOperation): Path =
    Path().apply { this.op(a, b, op) }

private fun litRegion(cx: Float, cy: Float, r: Float, rx: Float, waxing: Boolean, crescent: Boolean): Path {
    val rightRect = rectPath(Rect(cx, cy - r, cx + r, cy + r))
    val leftRect = rectPath(Rect(cx - r, cy - r, cx, cy + r))
    val disc = circlePath(cx, cy, r)
    val ell = ovalPath(cx, cy, rx, r)

    val rightHalfDisc = combine(disc, rightRect, PathOperation.Intersect)
    val leftHalfDisc = combine(disc, leftRect, PathOperation.Intersect)
    val rightHalfEll = combine(ell, rightRect, PathOperation.Intersect)
    val leftHalfEll = combine(ell, leftRect, PathOperation.Intersect)

    return when {
        waxing && crescent -> combine(rightHalfDisc, rightHalfEll, PathOperation.Difference)
        waxing && !crescent -> combine(rightHalfDisc, leftHalfEll, PathOperation.Union)
        !waxing && crescent -> combine(leftHalfDisc, leftHalfEll, PathOperation.Difference)
        else -> combine(leftHalfDisc, rightHalfEll, PathOperation.Union)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Fajr")
@Composable
private fun PrayerSkyScene_Fajr_Preview() {
    NimazTheme { PrayerSkyScene(0.22f, "5:23 AM", "First light · Sunrise in 1h 22m", scenePreviewModifier(), moonFraction = 0.92f) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Sunrise")
@Composable
private fun PrayerSkyScene_Sunrise_Preview() {
    NimazTheme { PrayerSkyScene(0.29f, "6:45 AM", "Sunrise · Dhuhr in 6h 30m", scenePreviewModifier()) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Mid-morning")
@Composable
private fun PrayerSkyScene_MidMorning_Preview() {
    NimazTheme { PrayerSkyScene(0.4f, "9:32 AM", "Dhuhr in 3h 43m", scenePreviewModifier()) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Dhuhr")
@Composable
private fun PrayerSkyScene_Dhuhr_Preview() {
    NimazTheme { PrayerSkyScene(0.5f, "1:15 PM", "Dhuhr · Asr in 3h 15m", scenePreviewModifier()) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Asr")
@Composable
private fun PrayerSkyScene_Asr_Preview() {
    NimazTheme { PrayerSkyScene(0.67f, "4:30 PM", "Asr · Maghrib in 3h 41m", scenePreviewModifier()) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Maghrib")
@Composable
private fun PrayerSkyScene_Maghrib_Preview() {
    NimazTheme { PrayerSkyScene(0.8f, "8:11 PM", "Maghrib · Isha in 1h 28m", scenePreviewModifier()) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Isha")
@Composable
private fun PrayerSkyScene_Isha_Preview() {
    NimazTheme { PrayerSkyScene(0.92f, "9:39 PM", "Isha · Fajr in 6h 04m", scenePreviewModifier(), moonFraction = 0.62f) }
}

private fun scenePreviewModifier(): Modifier =
    Modifier.fillMaxWidth().height(200.dp).padding(16.dp)
