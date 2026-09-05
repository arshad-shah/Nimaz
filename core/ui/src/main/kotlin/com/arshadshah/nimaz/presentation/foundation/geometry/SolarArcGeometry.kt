package com.arshadshah.nimaz.presentation.foundation.geometry

import kotlin.math.PI
import kotlin.math.cos

/**
 * The shape of the sun's day, from sunrise and sunset alone.
 *
 * Solar altitude is sinusoidal in the hour angle, and solar noon is midway between sunrise and
 * sunset *by definition*. So a cosine centred on that midpoint, scaled to pass through zero at
 * both crossings, is the exact curve — with no latitude, declination or date arithmetic.
 *
 * This is deliberately **suggestive, not simulated**: true Fajr and Isha depend on twilight
 * angle, and the real solar path changes shape with latitude in ways this does not model. It
 * draws a diagram of why the prayer times are when they are. It is not an ephemeris, and nothing
 * that needs an accurate altitude may call it.
 */

private const val TwoPi = (2.0 * PI).toFloat()

/** Below this the curve is degenerate (no daylight) and there is nothing to draw. */
private const val Epsilon = 1e-6f

/**
 * How far a below-horizon altitude is knocked back before it is drawn.
 *
 * A drawing decision, not astronomy. The apex is normalised to 1 in every season, so a short day
 * troughs *much* deeper than a long one — Dublin in December reaches -2.64 against June's -0.23.
 * Drawn at full depth a December night would be more than twice the visual weight of the day and
 * would leave the card, so night is compressed and then clamped to -1.
 */
const val NightCompression = 0.45f

/**
 * Normalised solar altitude at day-fraction [t] (0f = 00:00, 1f = 24:00).
 *
 * Returns 1f at solar noon, 0f at [sunriseFraction] and [sunsetFraction], and negative between
 * sunset and the next sunrise. Unbounded below — see [drawnAltitude] for the drawable form.
 *
 * Every degenerate input returns a flat zero curve rather than throwing: an arc must never be the
 * thing that crashes the screen.
 */
fun solarAltitude(t: Float, sunriseFraction: Float, sunsetFraction: Float): Float {
    if (!t.isFinite() || !sunriseFraction.isFinite() || !sunsetFraction.isFinite()) return 0f
    if (sunriseFraction < 0f || sunriseFraction > 1f) return 0f
    if (sunsetFraction < 0f || sunsetFraction > 1f) return 0f
    if (sunsetFraction <= sunriseFraction) return 0f

    val dhuhr = (sunriseFraction + sunsetFraction) / 2f
    // cos is even, so this is cos(2pi * halfDayLength) either way round.
    val c = cos(TwoPi * (sunriseFraction - dhuhr))
    val denominator = 1f - c
    if (denominator < Epsilon) return 0f

    val amplitude = 1f / denominator
    val offset = -c / denominator
    return amplitude * cos(TwoPi * (t - dhuhr)) + offset
}

/**
 * [solarAltitude] mapped into the `-1f..1f` band the arc is drawn in: daylight untouched, night
 * compressed by [NightCompression] and clamped.
 */
fun drawnAltitude(t: Float, sunriseFraction: Float, sunsetFraction: Float): Float {
    val raw = solarAltitude(t, sunriseFraction, sunsetFraction)
    return if (raw >= 0f) raw.coerceAtMost(1f) else (raw * NightCompression).coerceAtLeast(-1f)
}
