package com.arshadshah.nimaz.presentation.foundation.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared path builders for the Quran "manuscript" ornament language — the
 * shamsa medallion, the ogee cartouche panel, and the small bud/finial marks.
 *
 * These are the single source of geometry so every Quran ornament (the surah
 * header cartouche, the surah-list number medallion, page frames, …) is drawn
 * by the same code and can never drift apart. Keep them free of colour/size
 * policy — callers own the paint.
 */

private fun polar(center: Offset, radius: Float, degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    return Offset(
        center.x + radius * cos(rad).toFloat(),
        center.y + radius * sin(rad).toFloat(),
    )
}

/** Ring of [lobes] outward scallops around [c]; lobe apex lands ~ on radius [r]. */
internal fun scallopPath(c: Offset, r: Float, lobes: Int, anchor: Float, control: Float): Path {
    val step = 360f / lobes
    return Path().apply {
        val start = polar(c, r * anchor, -90f)
        moveTo(start.x, start.y)
        for (i in 0 until lobes) {
            val cp = polar(c, r * control, -90f + (i + 0.5f) * step)
            val p = polar(c, r * anchor, -90f + (i + 1f) * step)
            quadraticTo(cp.x, cp.y, p.x, p.y)
        }
        close()
    }
}

/** Ogee-pointed panel between x0..x1, y0..y1; [t] is the tip extension. */
internal fun cartouchePath(x0: Float, y0: Float, x1: Float, y1: Float, t: Float): Path {
    val cy = (y0 + y1) / 2f
    val bl = x0 + t
    val br = x1 - t
    val bow = (y1 - y0) * 0.30f
    return Path().apply {
        moveTo(bl, y0)
        lineTo(br, y0)
        cubicTo(br + t * 0.55f, y0, x1 - t * 0.25f, cy - bow, x1, cy)
        cubicTo(x1 - t * 0.25f, cy + bow, br + t * 0.55f, y1, br, y1)
        lineTo(bl, y1)
        cubicTo(bl - t * 0.55f, y1, x0 + t * 0.25f, cy + bow, x0, cy)
        cubicTo(x0 + t * 0.25f, cy - bow, bl - t * 0.55f, y0, bl, y0)
        close()
    }
}

/** Small solid diamond centred on [c] with half-diagonal [r] — the bud/finial mark. */
internal fun diamondPath(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r); lineTo(c.x + r, c.y)
    lineTo(c.x, c.y + r); lineTo(c.x - r, c.y)
    close()
}

/** Circle centred on [c] with radius [r]. */
internal fun circlePath(c: Offset, r: Float): Path = Path().apply {
    addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r))
}
