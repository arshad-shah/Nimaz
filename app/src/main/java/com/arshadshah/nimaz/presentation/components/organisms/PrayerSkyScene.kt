package com.arshadshah.nimaz.presentation.components.organisms

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.GlassBackdrop
import com.arshadshah.nimaz.presentation.components.atoms.GlassIconButton
import com.arshadshah.nimaz.presentation.components.atoms.GlassPill
import com.arshadshah.nimaz.presentation.components.atoms.GlassPillTone
import com.arshadshah.nimaz.presentation.components.atoms.glassBackdropSource
import com.arshadshah.nimaz.presentation.components.atoms.rememberGlassBackdrop
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.SkyColors
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
 * The sun is locked to the *real* sun: [sunriseFraction]/[sunsetFraction]
 * (today's sunrise/sunset, each as a fraction of the day) time-warp the scene
 * so daybreak and dusk land on the user's actual sun times for their location
 * and season — not fixed clock anchors. With the defaults the scene falls back
 * to a generic ~6:30am/~7:12pm day.
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
 * @param sunriseFraction today's sunrise as a fraction of the day (0f→1f).
 * @param sunsetFraction today's sunset (Maghrib) as a fraction of the day.
 */
@Composable
fun SkyBackground(
    timeOfDay: Float,
    moonFraction: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    cloudsEnabled: Boolean = true,
    sunriseFraction: Float = SUNRISE_T,
    sunsetFraction: Float = SUNSET_T,
) {
    // Cloud drift, advanced on the frame clock.
    //
    // History, because this has moved twice. It began as `rememberInfiniteTransition` +
    // `animateFloat`, which requests a frame *every frame, forever* for as long as Home is
    // composed — and ran even with `cloudsEnabled` false, animating 0f → 0f. That was replaced
    // by a `delay(1s)` loop advancing the phase in 120 discrete steps, on the reasoning that
    // steps that small would be invisible.
    //
    // They are not. A step is `width / 120` — about 9px on a 1080px-wide scene — and one 9px
    // jump per second is not drift, it is a slideshow. Motion is either per-frame or it reads
    // as broken; there is no cheap middle.
    //
    // So: back to the frame clock, but keeping the one thing the delay loop got right — when
    // `cloudsEnabled` is false nothing is started at all, rather than an animation running to
    // move nothing. `withInfiniteAnimationFrameNanos` also yields to `InfiniteAnimationPolicy`,
    // so Robolectric and instrumented tests are not held awake by it.
    //
    // The per-frame cost is what it always was: recomputing one float and blitting two already
    // baked bitmaps.
    var cloudPhase by remember { mutableFloatStateOf(0f) }
    if (cloudsEnabled) {
        LaunchedEffect(Unit) {
            var previousFrame = 0L
            while (true) {
                withInfiniteAnimationFrameNanos { frameNanos ->
                    // The first frame only establishes a baseline — a delta measured against
                    // zero would be the time since the process booted, and would slam the
                    // phase to an arbitrary value on the very first frame.
                    if (previousFrame != 0L) {
                        val elapsedMs = (frameNanos - previousFrame) / NANOS_PER_MS
                        cloudPhase = (cloudPhase + elapsedMs / CLOUD_CYCLE_MS) % 1f
                    }
                    previousFrame = frameNanos
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .drawWithCache {
                val w = size.width.roundToInt().coerceAtLeast(1)
                val h = size.height.roundToInt().coerceAtLeast(1)
                val sw = (size.width * SPRITE_SCALE).roundToInt().coerceAtLeast(1)
                val sh = (size.height * SPRITE_SCALE).roundToInt().coerceAtLeast(1)

                // Warp real time so the actual sunrise/sunset land on the
                // scene's canonical anchors — the sun, day/night ramp and sky
                // gradient then all track the real sun, not fixed clock times.
                val warped = remapDayFraction(timeOfDay, sunriseFraction, sunsetFraction)

                // Baked once per (warped, moonFraction, size) change — not per frame.
                val scene =
                    bakeLayer(sw, sh, this, layoutDirection) { drawScene(warped, moonFraction) }
                val clouds = bakeLayer(sw, sh, this, layoutDirection) { drawCloudLayer() }
                val cloudTint = ColorFilter.tint(sampleCloud(warped), BlendMode.Modulate)
                val full = IntSize(w, h)

                onDrawBehind {
                    drawImage(scene, dstOffset = IntOffset.Zero, dstSize = full)

                    // Translated by a float rather than offset by an `IntOffset`. The drift is
                    // roughly a sixth of a pixel per frame, so rounding the destination to whole
                    // pixels would hold the clouds still for six frames and then jump them one —
                    // a smaller version of the very stutter the frame clock above is fixing.
                    // A float translate lets the layer land between pixels and be filtered there.
                    translate(left = cloudPhase * w) {
                        drawImage(
                            clouds,
                            dstOffset = IntOffset.Zero,
                            dstSize = full,
                            colorFilter = cloudTint
                        )
                        // The trailing copy, so the band wraps seamlessly instead of showing a
                        // clear gap as the first one leaves.
                        drawImage(
                            clouds,
                            dstOffset = IntOffset(-w, 0),
                            dstSize = full,
                            colorFilter = cloudTint
                        )
                    }
                }
            },
    )
}

/**
 * The [SkyBackground] with a time + status label overlaid (top-left). Used by
 * the dedicated Prayer Times screen.
 *
 * When [locationName], [onBack] and [onSettings] are all supplied, the scene also
 * renders a pill-based glass topbar (back + settings + location) above the labels,
 * sharing this scene's single glass backdrop so every pill frosts the same sky.
 * In that mode the overlay is padded below the status bar, so callers should grow
 * the scene's height by the status-bar inset to let the sky reach the very top
 * (edge-to-edge). See [PrayerSkyTopBar].
 *
 * @param locationName when non-null (with [onBack]/[onSettings]), shown in the topbar's location pill.
 * @param onBack invoked by the topbar's back pill; enables the topbar when set.
 * @param onSettings invoked by the topbar's settings pill; enables the topbar when set.
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
    sunriseFraction: Float = SUNRISE_T,
    sunsetFraction: Float = SUNSET_T,
    locationName: String? = null,
    onBack: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    val backdrop = rememberGlassBackdrop()
    val showTopBar = locationName != null && onBack != null && onSettings != null
    // Only reserve the status-bar band when the topbar is present (edge-to-edge).
    val statusBarTop = if (showTopBar) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }
    Box(modifier = modifier) {
        SkyBackground(
            timeOfDay = timeOfDay,
            moonFraction = moonFraction,
            modifier = Modifier
                .matchParentSize()
                .glassBackdropSource(backdrop),
            shape = shape,
            cloudsEnabled = cloudsEnabled,
            sunriseFraction = sunriseFraction,
            sunsetFraction = sunsetFraction,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarTop)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showTopBar) {
                PrayerSkyTopBar(
                    locationName = locationName!!,
                    onBackClick = onBack!!,
                    onSettingsClick = onSettings!!,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            GlassPill(
                text = timeLabel,
                style = MaterialTheme.typography.titleLarge.copy(
                    shadow = GlassTextShadow,
                    fontWeight = FontWeight.Bold,
                ),
                tone = GlassPillTone.Solid,
                backdrop = backdrop,
            )
            GlassPill(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium.copy(shadow = GlassTextShadow),
                backdrop = backdrop,
            )
        }
    }
}

/**
 * The pill-based glass topbar overlaid on [PrayerSkyScene]: back + settings
 * circles on one row, then the location pill on its own line. It draws **no**
 * background of its own — it sits on the scene's sky and shares the scene's
 * [backdrop] so all three pills frost the same continuous sky.
 */
@Composable
private fun PrayerSkyTopBar(
    locationName: String,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Navigation actions at the two edges.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBackClick,
                backdrop = backdrop,
            )
            GlassIconButton(
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                onClick = onSettingsClick,
                backdrop = backdrop,
            )
        }

        Spacer(modifier = Modifier.height(NimazSpacing.Medium))

        // The location, on its own line so long names have room.
        GlassPill(
            text = locationName,
            leadingIcon = Icons.Filled.Place,
            tone = GlassPillTone.Solid,
            tint = Color.White,
            backdrop = backdrop,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                shadow = GlassTextShadow,
            ),
            modifier = Modifier.widthIn(max = LocationPillMaxWidth),
        )
    }
}

/** Keeps a long city name on one line and lets it ellipsise past this width. */
private val LocationPillMaxWidth = 260.dp

/** A soft drop shadow so overlaid glass text stays crisp over bright sky. */
private val GlassTextShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 4f)

/** How long the cloud band takes to travel its own width once. */
private const val CLOUD_CYCLE_MS = 120_000f

/** Frame timestamps arrive in nanoseconds; the cycle above is in milliseconds. */
private const val NANOS_PER_MS = 1_000_000f

private const val SPRITE_SCALE = 0.6f
private const val SUNRISE_T = 0.27f
private const val SUNSET_T = 0.80f

/**
 * Piecewise-linear time-warp from a real day-fraction [t] onto the scene's
 * canonical timeline, so the real [sunrise]/[sunset] (each 0f→1f of the day)
 * land exactly on [SUNRISE_T]/[SUNSET_T]. Midnight stays at 0f/1f. This keeps
 * the sun arc, the day/night ramp and the sky gradient locked to the user's
 * actual sun — the sun only dips below the horizon at real Maghrib, not at a
 * fixed 7:12pm. Degenerate or polar inputs are clamped to a sane day.
 */
internal fun remapDayFraction(t: Float, sunrise: Float, sunset: Float): Float {
    val tt = t.coerceIn(0f, 1f)
    val sr = sunrise.coerceIn(0.02f, 0.96f)
    val ss = sunset.coerceIn(sr + 0.02f, 0.98f)
    return when {
        tt <= sr -> lerpF(0f, SUNRISE_T, tt / sr)
        tt <= ss -> lerpF(SUNRISE_T, SUNSET_T, (tt - sr) / (ss - sr))
        else -> lerpF(SUNSET_T, 1f, (tt - ss) / (1f - ss))
    }
}

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
        drawMoon(
            Offset(size.width * 0.72f, size.height * 0.44f),
            size.minDimension * 0.16f,
            moonFraction,
            alpha = night
        )
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
    val horizonY = h * 0.86f
    val apexY = h * 0.30f
    val sunY = horizonY - alt * (horizonY - apexY) // alt<0 sinks it below the frame
    val radius = size.minDimension * 0.085f
    val warm = (1f - alt).coerceIn(0f, 1f) // warmer near the horizon

    val diskMid = lerp(SkyColors.SunDiskCoreDay, SkyColors.SunDiskCoreWarm, warm)
    val diskRim = lerp(SkyColors.SunDiskRimDay, SkyColors.SunDiskRimWarm, warm)
    val coronaIn = lerp(SkyColors.SunCoronaDay, SkyColors.SunCoronaWarm, warm)

    drawSun(
        center = Offset(sunX, sunY),
        radius = radius,
        disk = listOf(
            0f to Color.White,
            0.7f to diskMid,
            0.95f to diskRim,
            1f to diskRim.copy(alpha = 0f)
        ),
        corona = listOf(
            0f to coronaIn.copy(alpha = 0.85f), 0.34f to coronaIn.copy(alpha = 0.32f),
            0.7f to coronaIn.copy(alpha = 0.1f), 1f to coronaIn.copy(alpha = 0f),
        ),
        coronaScale = 2.7f,
        rayColor = coronaIn,
        rayCount = 5,
        rayLengthFr = 0.5f + 0.15f * alt.coerceAtLeast(0f),
        rayAlpha = 0.15f,
        alpha = sunAlpha,
    )
}

private fun DrawScope.drawNight(nf: Float) {
    val w = size.width
    val h = size.height
    drawIntoCanvas { c ->
        val paint = Paint().apply {
            isAntiAlias = true
            color = SkyColors.NightAurora.copy(alpha = 0.1f * nf).toArgb()
            maskFilter = BlurMaskFilter(size.minDimension * 0.08f, BlurMaskFilter.Blur.NORMAL)
        }
        c.nativeCanvas.save()
        c.nativeCanvas.rotate(-18f, w * 0.42f, h * 0.5f)
        c.nativeCanvas.drawOval(
            RectF(
                w * 0.42f - w * 0.6f,
                h * 0.5f - h * 0.13f,
                w * 0.42f + w * 0.6f,
                h * 0.5f + h * 0.13f
            ), paint
        )
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
    drawCloud(
        w * 0.25f,
        h * 0.49f,
        scale(),
        Color.White,
        SkyColors.CloudShadow,
        0.95f,
        CloudShape.Classic
    )
    drawCloud(
        w * 0.6f,
        h * 0.37f,
        scale() * 0.8f,
        Color.White,
        SkyColors.CloudShadow,
        0.9f,
        CloudShape.Puffy
    )
    drawCloud(
        w * 0.78f,
        h * 0.63f,
        scale() * 0.7f,
        Color.White,
        SkyColors.CloudShadow,
        0.85f,
        CloudShape.Wide
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Time → colour sampling
// ─────────────────────────────────────────────────────────────────────────

private class SkyKey(val t: Float, val sky: List<Color>, val cloud: Color)

private val SKY_KEYS = listOf(
    SkyKey(
        0.00f,
        listOf(
            SkyColors.MidnightZenith,
            SkyColors.MidnightUpper,
            SkyColors.MidnightMid,
            SkyColors.MidnightLower,
            SkyColors.MidnightHorizon
        ),
        SkyColors.MidnightCloud
    ),
    SkyKey(
        0.20f,
        listOf(
            SkyColors.PreDawnZenith,
            SkyColors.PreDawnUpper,
            SkyColors.PreDawnMid,
            SkyColors.PreDawnLower,
            SkyColors.PreDawnHorizon
        ),
        SkyColors.PreDawnCloud
    ),
    SkyKey(
        0.28f,
        listOf(
            SkyColors.SunriseZenith,
            SkyColors.SunriseUpper,
            SkyColors.SunriseMid,
            SkyColors.SunriseLower,
            SkyColors.SunriseHorizon
        ),
        SkyColors.SunriseCloud
    ),
    SkyKey(
        0.50f,
        listOf(
            SkyColors.MiddayZenith,
            SkyColors.MiddayUpper,
            SkyColors.MiddayMid,
            SkyColors.MiddayLower,
            SkyColors.MiddayHorizon
        ),
        SkyColors.MiddayCloud
    ),
    SkyKey(
        0.67f,
        listOf(
            SkyColors.AfternoonZenith,
            SkyColors.AfternoonUpper,
            SkyColors.AfternoonMid,
            SkyColors.AfternoonLower,
            SkyColors.AfternoonHorizon
        ),
        SkyColors.AfternoonCloud
    ),
    SkyKey(
        0.80f,
        listOf(
            SkyColors.SunsetZenith,
            SkyColors.SunsetUpper,
            SkyColors.SunsetMid,
            SkyColors.SunsetLower,
            SkyColors.SunsetHorizon
        ),
        SkyColors.SunsetCloud
    ),
    SkyKey(
        0.87f,
        listOf(
            SkyColors.DuskZenith,
            SkyColors.DuskUpper,
            SkyColors.DuskMid,
            SkyColors.MidnightHorizon,
            SkyColors.DuskHorizon
        ),
        SkyColors.DuskCloud
    ),
    SkyKey(
        1.00f,
        listOf(
            SkyColors.MidnightZenith,
            SkyColors.MidnightUpper,
            SkyColors.MidnightMid,
            SkyColors.MidnightLower,
            SkyColors.MidnightHorizon
        ),
        SkyColors.MidnightCloud
    ),
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
private fun bakeLayer(
    wPx: Int,
    hPx: Int,
    density: Density,
    ld: LayoutDirection,
    block: DrawScope.() -> Unit
): ImageBitmap {
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
    drawCircle(
        Brush.radialGradient(
            *fade(corona, alpha),
            center = center,
            radius = radius * coronaScale
        ), radius = radius * coronaScale, center = center
    )
    drawCircle(
        Brush.radialGradient(*fade(disk, alpha), center = center, radius = radius),
        radius = radius,
        center = center
    )
}

private fun fade(stops: List<Pair<Float, Color>>, alpha: Float): Array<Pair<Float, Color>> =
    stops.map { it.first to it.second.copy(alpha = it.second.alpha * alpha) }.toTypedArray()

/**
 * Silhouette variants for [drawCloud]. All follow the same recipe — a base oval,
 * a row of puff circles for the bumpy crown, and a flat-bottomed slab — so they
 * read as one family while no two clouds look identical.
 */
private enum class CloudShape { Classic, Puffy, Wide }

private fun DrawScope.drawCloud(
    cx: Float,
    cy: Float,
    s: Float,
    top: Color,
    bottom: Color,
    alpha: Float,
    shape: CloudShape = CloudShape.Classic
) {
    drawIntoCanvas { c ->
        val paint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f,
                cy - 18f * s,
                0f,
                cy + 16f * s,
                top.toArgb(),
                bottom.toArgb(),
                Shader.TileMode.CLAMP
            )
            maskFilter = BlurMaskFilter(2f * s + 1f, BlurMaskFilter.Blur.NORMAL)
            this.alpha = (alpha * 255).toInt()
        }
        val dir = android.graphics.Path.Direction.CW
        val p = android.graphics.Path().apply {
            when (shape) {
                CloudShape.Classic -> {
                    addOval(RectF(cx - 30f * s, cy - 12f * s, cx + 30f * s, cy + 12f * s), dir)
                    addCircle(cx - 22f * s, cy - 2f * s, 14f * s, dir)
                    addCircle(cx - 6f * s, cy - 12f * s, 16f * s, dir)
                    addCircle(cx + 12f * s, cy - 8f * s, 13f * s, dir)
                    addCircle(cx + 24f * s, cy - 1f * s, 15f * s, dir)
                    addRoundRect(
                        RectF(cx - 44f * s, cy, cx + 44f * s, cy + 16f * s),
                        8f * s,
                        8f * s,
                        dir
                    )
                }
                // Rounder and taller — a fifth puff lifts the crown into a dome.
                CloudShape.Puffy -> {
                    addOval(RectF(cx - 28f * s, cy - 10f * s, cx + 28f * s, cy + 12f * s), dir)
                    addCircle(cx - 20f * s, cy - 4f * s, 13f * s, dir)
                    addCircle(cx - 8f * s, cy - 14f * s, 17f * s, dir)
                    addCircle(cx + 6f * s, cy - 16f * s, 16f * s, dir)
                    addCircle(cx + 18f * s, cy - 9f * s, 14f * s, dir)
                    addCircle(cx + 26f * s, cy - 2f * s, 12f * s, dir)
                    addRoundRect(
                        RectF(cx - 40f * s, cy + 2f * s, cx + 40f * s, cy + 16f * s),
                        9f * s,
                        9f * s,
                        dir
                    )
                }
                // Stretched and flat — a low, drifting wisp with smaller bumps.
                CloudShape.Wide -> {
                    addOval(RectF(cx - 38f * s, cy - 8f * s, cx + 38f * s, cy + 10f * s), dir)
                    addCircle(cx - 28f * s, cy - 2f * s, 11f * s, dir)
                    addCircle(cx - 12f * s, cy - 9f * s, 14f * s, dir)
                    addCircle(cx + 4f * s, cy - 10f * s, 14f * s, dir)
                    addCircle(cx + 20f * s, cy - 6f * s, 12f * s, dir)
                    addCircle(cx + 30f * s, cy - 1f * s, 10f * s, dir)
                    addRoundRect(
                        RectF(cx - 50f * s, cy, cx + 50f * s, cy + 14f * s),
                        7f * s,
                        7f * s,
                        dir
                    )
                }
            }
        }
        c.nativeCanvas.drawPath(p, paint)
    }
}

private fun DrawScope.drawMoon(center: Offset, radius: Float, fraction: Float, alpha: Float) {
    val cx = center.x
    val cy = center.y
    val r = radius

    drawCircle(
        Brush.radialGradient(
            0f to SkyColors.MoonGlow.copy(alpha = 0.38f * alpha),
            1f to SkyColors.MoonGlowTransparent,
            center = center,
            radius = r * 1.5f
        ), radius = r * 1.5f, center = center
    )
    drawCircle(
        Brush.radialGradient(
            0f to SkyColors.MoonDiscTop,
            1f to SkyColors.MoonDiscBottom,
            center = Offset(cx, cy - r * 0.05f),
            radius = r
        ), radius = r, center = center
    )

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
                    intArrayOf(
                        NimazPalette.White.toArgb(),
                        SkyColors.MoonLitHighlight.toArgb(),
                        SkyColors.MoonLitMid.toArgb(),
                        SkyColors.MoonLitEdge.toArgb()
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
// Moon geometry
// ─────────────────────────────────────────────────────────────────────────

private fun circlePath(cx: Float, cy: Float, r: Float): Path =
    Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }

private fun ovalPath(cx: Float, cy: Float, rx: Float, ry: Float): Path =
    Path().apply { addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }

private fun rectPath(rect: Rect): Path = Path().apply { addRect(rect) }

private fun combine(a: Path, b: Path, op: PathOperation): Path =
    Path().apply { this.op(a, b, op) }

private fun litRegion(
    cx: Float,
    cy: Float,
    r: Float,
    rx: Float,
    waxing: Boolean,
    crescent: Boolean
): Path {
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
    NimazTheme {
        PrayerSkyScene(
            0.22f,
            "5:23 AM",
            "First light · Sunrise in 1h 22m",
            scenePreviewModifier(),
            moonFraction = 0.92f
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Sunrise")
@Composable
private fun PrayerSkyScene_Sunrise_Preview() {
    NimazTheme {
        PrayerSkyScene(
            0.29f,
            "6:45 AM",
            "Sunrise · Dhuhr in 6h 30m",
            scenePreviewModifier()
        )
    }
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
    NimazTheme {
        PrayerSkyScene(
            0.67f,
            "4:30 PM",
            "Asr · Maghrib in 3h 41m",
            scenePreviewModifier()
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Maghrib")
@Composable
private fun PrayerSkyScene_Maghrib_Preview() {
    NimazTheme {
        PrayerSkyScene(
            0.8f,
            "8:11 PM",
            "Maghrib · Isha in 1h 28m",
            scenePreviewModifier()
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 220, name = "Living · Isha")
@Composable
private fun PrayerSkyScene_Isha_Preview() {
    NimazTheme {
        PrayerSkyScene(
            0.92f,
            "9:39 PM",
            "Isha · Fajr in 6h 04m",
            scenePreviewModifier(),
            moonFraction = 0.62f,
            shape = RoundedCornerShape(
                bottomStart = 26.dp,
                bottomEnd = 26.dp
            )
        )
    }
}

// The shipped configuration — sky + glass topbar (back · settings · location)
// over the same continuous sky. Status-bar inset is 0 in previews, so the band
// above the pills only appears on-device.

@Preview(showBackground = true, widthDp = 380, heightDp = 320, name = "Hero + topbar · Sunrise")
@Composable
private fun PrayerSkyScene_TopBar_Sunrise_Preview() {
    NimazTheme {
        PrayerSkyScene(
            timeOfDay = 0.29f,
            timeLabel = "6:45 AM",
            statusLabel = "Sunrise · Dhuhr in 6h 30m",
            locationName = "Manchester, UK",
            onBack = {},
            onSettings = {},
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            modifier = heroPreviewModifier(),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 320, name = "Hero + topbar · Dhuhr")
@Composable
private fun PrayerSkyScene_TopBar_Dhuhr_Preview() {
    NimazTheme {
        PrayerSkyScene(
            timeOfDay = 0.5f,
            timeLabel = "1:15 PM",
            statusLabel = "Dhuhr · Asr in 3h 15m",
            locationName = "Makkah",
            onBack = {},
            onSettings = {},
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            modifier = heroPreviewModifier(),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 320, name = "Hero + topbar · Isha")
@Composable
private fun PrayerSkyScene_TopBar_Isha_Preview() {
    NimazTheme {
        PrayerSkyScene(
            timeOfDay = 0.92f,
            timeLabel = "9:39 PM",
            statusLabel = "Isha · Fajr in 6h 04m",
            moonFraction = 0.62f,
            locationName = "Kuala Lumpur",
            onBack = {},
            onSettings = {},
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            modifier = heroPreviewModifier(),
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 380,
    heightDp = 320,
    name = "Hero + topbar · long location"
)
@Composable
private fun PrayerSkyScene_TopBar_LongLocation_Preview() {
    NimazTheme {
        PrayerSkyScene(
            timeOfDay = 0.8f,
            timeLabel = "8:11 PM",
            statusLabel = "Maghrib · Isha in 1h 28m",
            locationName = "Makkah al-Mukarramah, Saudi Arabia",
            onBack = {},
            onSettings = {},
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            modifier = heroPreviewModifier(),
        )
    }
}

private fun heroPreviewModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .height(320.dp)

private fun scenePreviewModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .height(200.dp)
        .padding(16.dp)
