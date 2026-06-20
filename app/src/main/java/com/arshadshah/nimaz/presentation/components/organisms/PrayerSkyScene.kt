package com.arshadshah.nimaz.presentation.components.organisms

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * A time-aware "sky scene" for the prayer-times experience.
 *
 * There is no horizon and no markers — the sky itself tells the time. Each
 * [SkyPhase] paints its own gradient, clouds, and celestial body: a crisp sun
 * with a diffused corona + soft light shafts by day, and a real-phase moon at
 * Fajr (a dim setting crescent) and Isha (full night).
 *
 * The moon is dynamic: pass [moonPhaseFraction] (0f = new … 0.5f = full … →
 * new), which [MoonPhase] derives from the date, and the renderer draws the
 * correct illuminated shape for the real sky.
 *
 * @param phase which part of the day to paint.
 * @param timeLabel large overlay label, e.g. "3:42 PM".
 * @param statusLabel secondary overlay line, e.g. "Asr in 1h 12m".
 * @param moonPhaseFraction synodic phase 0f→1f; only used by FAJR and ISHA.
 */
@Composable
fun PrayerSkyScene(
    phase: SkyPhase,
    timeLabel: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
    moonPhaseFraction: Float = 0.18f,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(20.dp))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (phase) {
                SkyPhase.FAJR -> drawFajr(moonPhaseFraction)
                SkyPhase.SUNRISE -> drawSunrise()
                SkyPhase.DHUHR -> drawDhuhr()
                SkyPhase.ASR -> drawAsr()
                SkyPhase.MAGHRIB -> drawMaghrib()
                SkyPhase.ISHA -> drawIsha(moonPhaseFraction)
            }
        }

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

/** One sky per prayer. */
enum class SkyPhase { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

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
// Phases
// ─────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawFajr(moonFraction: Float) {
    drawSky(
        0f to Color(0xFF060A1C), 0.45f to Color(0xFF16204A),
        0.74f to Color(0xFF3B3270), 0.9f to Color(0xFF8A4F6E), 1f to Color(0xFFD08A5E),
    )
    val w = size.width
    val h = size.height
    // first light creeping up from below
    val glowC = Offset(w * 0.5f, h * 1.02f)
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFFFFC98A).copy(alpha = 0.5f), 1f to Color(0x00FFC98A),
            center = glowC, radius = w * 0.7f,
        ),
        radius = w * 0.7f, center = glowC,
    )
    star(w * 0.14f, h * 0.16f, 1.3f, 0.5f)
    star(w * 0.34f, h * 0.12f, 1f, 0.35f)
    star(w * 0.66f, h * 0.18f, 1f, 0.4f)
    drawCloud(w * 0.30f, h * 0.78f, scale(), Color(0xFF6A4A63), Color(0xFFC98A66), 0.8f)
    drawMoon(Offset(w * 0.8f, h * 0.26f), size.minDimension * 0.10f, moonFraction, alpha = 0.9f)
}

private fun DrawScope.drawSunrise() {
    drawSky(
        0f to Color(0xFF2B3A8C), 0.4f to Color(0xFF7C6AB0),
        0.66f to Color(0xFFE59AB0), 0.86f to Color(0xFFFBB778), 1f to Color(0xFFFFE0A3),
    )
    val w = size.width
    val h = size.height
    drawCloud(w * 0.55f, h * 0.32f, scale(), Color(0xFFFFF3E6), Color(0xFFF0A98F), 0.9f)
    drawCloud(w * 0.82f, h * 0.5f, scale() * 0.8f, Color(0xFFFFF6EC), Color(0xFFE89FB0), 0.85f)
    drawSun(
        center = Offset(w * 0.2f, h * 0.72f), radius = size.minDimension * 0.085f,
        disk = listOf(0f to Color(0xFFFFFEF5), 0.7f to Color(0xFFFFD9A0), 0.95f to Color(0xFFFBA76A), 1f to Color(0x00FBA76A)),
        corona = listOf(0f to Color(0xFFFFE9C2).copy(alpha = 0.85f), 0.6f to Color(0xFFFFD9A0).copy(alpha = 0.18f), 1f to Color(0x00FFD9A0)),
        coronaScale = 2.7f,
        rayColor = Color(0xFFFFE9C2), rayCount = 2, rayLengthFr = 0.62f, rayAlpha = 0.14f,
    )
}

private fun DrawScope.drawDhuhr() {
    drawSky(
        0f to Color(0xFF0A2E7A), 0.35f to Color(0xFF1E62D6),
        0.66f to Color(0xFF4F9BF5), 0.9f to Color(0xFFBFE0FB), 1f to Color(0xFFEAF6FF),
    )
    val w = size.width
    val h = size.height
    drawCloud(w * 0.22f, h * 0.7f, scale(), Color(0xFFFFFFFF), Color(0xFFC6DBF2), 0.92f)
    drawCloud(w * 0.8f, h * 0.8f, scale() * 0.85f, Color(0xFFFFFFFF), Color(0xFFC6DBF2), 0.82f)
    drawSun(
        center = Offset(w * 0.5f, h * 0.3f), radius = size.minDimension * 0.075f,
        disk = listOf(0f to Color(0xFFFFFFFF), 0.7f to Color(0xFFFFF1A8), 0.95f to Color(0xFFFACC15), 1f to Color(0x00FACC15)),
        corona = listOf(0f to Color(0xFFFFF6C2).copy(alpha = 0.85f), 0.32f to Color(0xFFFDE047).copy(alpha = 0.4f), 0.66f to Color(0xFFFDE047).copy(alpha = 0.12f), 1f to Color(0x00FDE047)),
        coronaScale = 2.7f,
        rayColor = Color(0xFFFFF3B8), rayCount = 5, rayLengthFr = 0.6f, rayAlpha = 0.18f,
    )
}

private fun DrawScope.drawAsr() {
    drawSky(
        0f to Color(0xFF15407F), 0.4f to Color(0xFF3E78C9),
        0.68f to Color(0xFF8FB6E8), 0.88f to Color(0xFFF2D9A8), 1f to Color(0xFFFBE3B0),
    )
    val w = size.width
    val h = size.height
    drawCloud(w * 0.26f, h * 0.72f, scale(), Color(0xFFFFFDF5), Color(0xFFE3C79E), 0.9f)
    drawCloud(w * 0.5f, h * 0.82f, scale() * 0.78f, Color(0xFFFFFDF5), Color(0xFFE3C79E), 0.78f)
    drawSun(
        center = Offset(w * 0.7f, h * 0.46f), radius = size.minDimension * 0.08f,
        disk = listOf(0f to Color(0xFFFFFDF0), 0.7f to Color(0xFFFFE08A), 0.95f to Color(0xFFF4B23C), 1f to Color(0x00F4B23C)),
        corona = listOf(0f to Color(0xFFFFE9B0).copy(alpha = 0.8f), 0.6f to Color(0xFFFFE9B0).copy(alpha = 0.16f), 1f to Color(0x00FFE9B0)),
        coronaScale = 2.6f,
        rayColor = Color(0xFFFFE9B0), rayCount = 4, rayLengthFr = 0.55f, rayAlpha = 0.16f,
    )
}

private fun DrawScope.drawMaghrib() {
    drawSky(
        0f to Color(0xFF241056), 0.28f to Color(0xFF7A1E83),
        0.52f to Color(0xFFD6356B), 0.76f to Color(0xFFF9733A), 1f to Color(0xFFFBD34D),
    )
    val w = size.width
    val h = size.height
    // warm haze glowing where the sun sets (west/right)
    val hazeC = Offset(w * 0.8f, h * 0.95f)
    drawCircle(
        Brush.radialGradient(0f to Color(0xFFFFD27A).copy(alpha = 0.3f), 1f to Color(0x00FFD27A), center = hazeC, radius = w * 0.7f),
        radius = w * 0.7f, center = hazeC,
    )
    drawCloud(w * 0.22f, h * 0.44f, scale() * 0.9f, Color(0xFFFFD9A8), Color(0xFF9B2F63), 0.82f)
    drawCloud(w * 0.6f, h * 0.26f, scale() * 0.8f, Color(0xFFFFE3B0), Color(0xFF7A2A6E), 0.75f)
    drawSun(
        center = Offset(w * 0.82f, h * 0.74f), radius = size.minDimension * 0.09f,
        disk = listOf(0f to Color(0xFFFFFDF6), 0.7f to Color(0xFFFFC15E), 0.95f to Color(0xFFF97316), 1f to Color(0x00F97316)),
        corona = listOf(0f to Color(0xFFFFF4D6).copy(alpha = 0.9f), 0.3f to Color(0xFFFFD98A).copy(alpha = 0.5f), 0.62f to Color(0xFFFB923C).copy(alpha = 0.16f), 1f to Color(0x00FB923C)),
        coronaScale = 2.9f,
        rayColor = Color(0xFFFFE9B8), rayCount = 5, rayLengthFr = 0.7f, rayAlpha = 0.16f,
    )
}

private fun DrawScope.drawIsha(moonFraction: Float) {
    drawSky(
        0f to Color(0xFF03060F), 0.5f to Color(0xFF0E1330),
        0.82f to Color(0xFF1B1F4A), 1f to Color(0xFF33285E),
    )
    val w = size.width
    val h = size.height
    // galaxy band
    drawIntoCanvas { c ->
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color(0xFF7E8AD6).copy(alpha = 0.1f).toArgb()
            maskFilter = BlurMaskFilter(size.minDimension * 0.08f, BlurMaskFilter.Blur.NORMAL)
        }
        c.nativeCanvas.save()
        c.nativeCanvas.rotate(-18f, w * 0.42f, h * 0.5f)
        c.nativeCanvas.drawOval(
            RectF(w * 0.42f - w * 0.6f, h * 0.5f - h * 0.13f, w * 0.42f + w * 0.6f, h * 0.5f + h * 0.13f),
            paint,
        )
        c.nativeCanvas.restore()
    }
    star(w * 0.11f, h * 0.2f, 1.4f, 0.9f)
    star(w * 0.26f, h * 0.14f, 1f, 0.6f)
    star(w * 0.36f, h * 0.32f, 1.1f, 0.7f)
    star(w * 0.83f, h * 0.18f, 1.3f, 0.85f)
    star(w * 0.92f, h * 0.38f, 1f, 0.6f)
    star(w * 0.55f, h * 0.21f, 1f, 0.6f)
    star(w * 0.16f, h * 0.56f, 0.9f, 0.5f)
    // sparkle
    sparkle(w * 0.42f, h * 0.62f, size.minDimension * 0.018f)
    drawCloud(w * 0.26f, h * 0.78f, scale(), Color(0xFF3A3E66), Color(0xFF171A38), 0.7f)
    drawMoon(Offset(w * 0.73f, h * 0.36f), size.minDimension * 0.16f, moonFraction, alpha = 1f)
}

// ─────────────────────────────────────────────────────────────────────────
// Shared drawing helpers
// ─────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawSky(vararg stops: Pair<Float, Color>) {
    drawRect(Brush.verticalGradient(*stops, startY = 0f, endY = size.height))
}

/** A scale factor that maps the 360-wide reference design to the actual width. */
private fun DrawScope.scale(): Float = size.width / 360f

private fun DrawScope.star(x: Float, y: Float, radius: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha = alpha), radius = radius, center = Offset(x, y))
}

private fun DrawScope.sparkle(x: Float, y: Float, r: Float) {
    val c = Color.White.copy(alpha = 0.85f)
    drawLine(c, Offset(x - r, y), Offset(x + r, y), strokeWidth = 1f)
    drawLine(c, Offset(x, y - r), Offset(x, y + r), strokeWidth = 1f)
}

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
) {
    // diffused light shafts: wide, heavily blurred, low opacity
    if (rayCount > 0) {
        val rayLength = size.minDimension * rayLengthFr
        drawIntoCanvas { c ->
            val paint = Paint().apply {
                isAntiAlias = true
                color = rayColor.copy(alpha = rayAlpha).toArgb()
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
    // diffused corona
    drawCircle(
        Brush.radialGradient(*corona.toTypedArray(), center = center, radius = radius * coronaScale),
        radius = radius * coronaScale, center = center,
    )
    // crisp disk (gradient fades only at the very rim)
    drawCircle(
        Brush.radialGradient(*disk.toTypedArray(), center = center, radius = radius),
        radius = radius, center = center,
    )
}

private fun DrawScope.drawCloud(
    cx: Float,
    cy: Float,
    s: Float,
    top: Color,
    bottom: Color,
    alpha: Float,
) {
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

/**
 * The moon as a full sphere: a faint dark side (so the silhouette is always
 * round) with the illuminated region painted on top with spherical shading and
 * a soft terminator. The lit region is built with [PathOperation] and clipped
 * to the disc so the limb stays crisp while only the terminator is blurred.
 */
private fun DrawScope.drawMoon(center: Offset, radius: Float, fraction: Float, alpha: Float) {
    val cx = center.x
    val cy = center.y
    val r = radius

    // halo
    drawCircle(
        Brush.radialGradient(0f to Color(0xFFC7D2FE).copy(alpha = 0.38f * alpha), 1f to Color(0x00C7D2FE), center = center, radius = r * 1.5f),
        radius = r * 1.5f, center = center,
    )
    // faint dark side
    drawCircle(
        Brush.radialGradient(0f to Color(0xFF262C4C), 1f to Color(0xFF10142C), center = Offset(cx, cy - r * 0.05f), radius = r),
        radius = r, center = center,
    )

    // lit region geometry (limb slightly oversized so the blur doesn't soften it)
    val rg = r * 1.06f
    val cosv = cos(2 * PI * fraction).toFloat()
    val rx = rg * abs(cosv)
    val waxing = fraction < 0.5f
    val crescent = cosv > 0f // illuminated fraction < 0.5
    val lit = litRegion(cx, cy, rg, rx, waxing, crescent)

    clipPath(circlePath(cx, cy, r)) {
        drawIntoCanvas { c ->
            val paint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    cx - r * 0.3f, cy - r * 0.34f, r * 1.4f,
                    intArrayOf(
                        Color(0xFFFFFFFF).toArgb(), Color(0xFFE9EDF6).toArgb(),
                        Color(0xFFC5CCDE).toArgb(), Color(0xFFAAB2CC).toArgb(),
                    ),
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
// Geometry
// ─────────────────────────────────────────────────────────────────────────

private fun circlePath(cx: Float, cy: Float, r: Float): Path =
    Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }

private fun ovalPath(cx: Float, cy: Float, rx: Float, ry: Float): Path =
    Path().apply { addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }

private fun rectPath(rect: Rect): Path = Path().apply { addRect(rect) }

private fun combine(a: Path, b: Path, op: PathOperation): Path =
    Path().apply { this.op(a, b, op) }

/**
 * The illuminated region for a moon of radius [r] at ([cx],[cy]) with
 * terminator half-width [rx]. Built from a lit-side half-disc and a
 * terminator half-ellipse: subtracted for a crescent, unioned for a gibbous.
 */
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

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Sky · Fajr")
@Composable
private fun PrayerSkyScene_Fajr_Preview() {
    NimazTheme {
        PrayerSkyScene(SkyPhase.FAJR, "5:23 AM", "First light · Sunrise in 1h 22m", scenePreviewModifier(), moonPhaseFraction = 0.92f)
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Sky · Sunrise")
@Composable
private fun PrayerSkyScene_Sunrise_Preview() {
    NimazTheme {
        PrayerSkyScene(SkyPhase.SUNRISE, "6:45 AM", "Sunrise · Dhuhr in 6h 30m", scenePreviewModifier())
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Sky · Dhuhr")
@Composable
private fun PrayerSkyScene_Dhuhr_Preview() {
    NimazTheme {
        PrayerSkyScene(SkyPhase.DHUHR, "1:15 PM", "Dhuhr · Asr in 3h 15m", scenePreviewModifier())
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Sky · Asr")
@Composable
private fun PrayerSkyScene_Asr_Preview() {
    NimazTheme {
        PrayerSkyScene(SkyPhase.ASR, "4:30 PM", "Asr · Maghrib in 3h 41m", scenePreviewModifier())
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Sky · Maghrib")
@Composable
private fun PrayerSkyScene_Maghrib_Preview() {
    NimazTheme {
        PrayerSkyScene(SkyPhase.MAGHRIB, "8:11 PM", "Maghrib · Isha in 1h 28m", scenePreviewModifier())
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Sky · Isha")
@Composable
private fun PrayerSkyScene_Isha_Preview() {
    NimazTheme {
        PrayerSkyScene(SkyPhase.ISHA, "9:39 PM", "Isha · Fajr in 6h 04m", scenePreviewModifier(), moonPhaseFraction = 0.62f)
    }
}

@Composable
private fun scenePreviewModifier(): Modifier =
    Modifier.fillMaxWidth().height(200.dp).padding(16.dp)
