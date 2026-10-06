package com.arshadshah.rail

import kotlin.math.roundToInt

internal data class ThumbGeometry(val length: Float, val travel: Float, val canScroll: Boolean)

internal fun thumbGeometry(track: Float, padding: Float, minimum: Float, maximum: Float, content: Double, viewport: Double): ThumbGeometry {
    if (!track.isFinite() || !padding.isFinite()) return ThumbGeometry(0f, 0f, false)
    val usable = (track - 2 * padding).coerceAtLeast(0f)
    val canScroll = usable > 0f && content.isFinite() && viewport.isFinite() && viewport > 0 && content - viewport > 1.0
    val raw = if (canScroll) (usable * viewport / content).toFloat() else usable
    val length = raw.coerceIn(minOf(minimum, usable), minOf(maximum, usable)).roundToInt().toFloat().coerceIn(minOf(minimum, usable), minOf(maximum, usable))
    return ThumbGeometry(length, (usable - length).coerceAtLeast(0f), canScroll)
}

internal fun thumbPosition(offset: Double, maximum: Double, travel: Float, reversed: Boolean): Float {
    if (!maximum.isFinite() || maximum <= 0 || !travel.isFinite() || travel <= 0 || !offset.isFinite()) return 0f
    val fraction = (offset / maximum).toFloat().coerceIn(0f, 1f)
    return (if (reversed) 1f - fraction else fraction) * travel
}

internal fun scrollPosition(position: Float, maximum: Double, travel: Float, reversed: Boolean): Double {
    if (!position.isFinite() || !maximum.isFinite() || maximum <= 0 || !travel.isFinite() || travel <= 0f) return 0.0
    val fraction = (position / travel).coerceIn(0f, 1f)
    return (if (reversed) 1f - fraction else fraction).toDouble() * maximum
}
