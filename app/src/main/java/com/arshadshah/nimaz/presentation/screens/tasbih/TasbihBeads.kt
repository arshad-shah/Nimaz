package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Hand-drawn tasbih (misbaha) counter. The full loop is hidden — only a visible
 * **strand** that arches gently upward and runs **edge to edge** is drawn. Beads
 * bunch in from one edge and out the other, with a **wide gap** at the apex
 * holding one loose bead. **Tap**, or **flick that bead across the gap**, advances
 * the count; the strand slides by one and a fresh bead drops into the gap. The
 * **imame** (lap marker) returns to the gap once per [targetCount].
 *
 * Controlled: [count] is the source of truth (from the screen / view-model) and
 * each gesture calls [onIncrement]; the strand animates to follow the count.
 * The look is supplied by a pluggable [design] (see [BeadDesigns]).
 */
@Composable
fun TasbihBeads(
    count: Int,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    targetCount: Int = 33,
    design: BeadDesign = BeadDesigns.Default,
    leftHanded: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val beadCount = targetCount.coerceAtLeast(1) // imame marks each full target

    // Visual position; follows [count] except while a drag is controlling it.
    val pos = remember { Animatable(count.toFloat()) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(count) {
        if (!dragging) {
            if (abs(count - pos.value) > 1.5f) pos.snapTo(count.toFloat())
            else pos.animateTo(count.toFloat(), tween(340)) // tap glide
        }
    }

    Canvas(
        modifier = modifier.pointerInput(design, leftHanded) {
            val g = buildStrand(size.width.toFloat(), size.height.toFloat(), design, leftHanded)
            // One gesture = one bead. We track the crossing synchronously (dragD)
            // so even a fast flick is decided correctly, and increment exactly once.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val base = count.toFloat()
                var dragD = 0f
                var moved = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    val delta = change.positionChange()
                    val proj = (delta.x * g.dirX + delta.y * g.dirY) / g.pack
                    if (proj != 0f) {
                        dragD = (dragD + proj).coerceIn(0f, 1f)
                        if (dragD > 0.04f) {
                            moved = true
                            dragging = true
                            change.consume()
                            scope.launch { pos.snapTo(base + dragD) }
                        }
                    }
                }
                dragging = false
                when {
                    !moved -> onIncrement()                  // tap → glide one bead
                    dragD > 0.5f -> onIncrement()            // flick crossed → advance one
                    else -> scope.launch { pos.animateTo(base, tween(170)) } // settle back
                }
            }
        }
    ) {
        drawStrand(pos.value, beadCount, design, leftHanded)
    }
}

/**
 * Visible-strand geometry: an upward-arching quadratic curve, sampled to an
 * arc-length table so beads can be placed by distance along the curve (and the
 * cord drawn as the real curve). Distances beyond the ends extrapolate along the
 * end tangents, so beads continue off-screen and the loop reads as continuous.
 */
private class Strand(
    private val xs: FloatArray,
    private val ys: FloatArray,
    private val cum: FloatArray,
    private val startDir: Offset,
    private val endDir: Offset,
    val total: Float,
    val pack: Float,
    val beadR: Float,
    val gapTop: Float,
    val gapBottom: Float,
    val dirX: Float,
    val dirY: Float,
) {
    fun point(d: Float): Offset {
        if (d <= 0f) return Offset(xs.first() + startDir.x * d, ys.first() + startDir.y * d)
        if (d >= total) {
            val e = d - total
            return Offset(xs.last() + endDir.x * e, ys.last() + endDir.y * e)
        }
        // locate segment whose cumulative length brackets d
        var i = 1
        while (i < cum.size && cum[i] < d) i++
        val segLen = cum[i] - cum[i - 1]
        val f = if (segLen > 0f) (d - cum[i - 1]) / segLen else 0f
        return Offset(lerp(xs[i - 1], xs[i], f), lerp(ys[i - 1], ys[i], f))
    }

    /** The cord, drawn slightly past both ends so it disappears off-screen. */
    fun cordPath(): Path = Path().apply {
        val s = point(-beadR * 2f)
        moveTo(s.x, s.y)
        for (i in xs.indices) lineTo(xs[i], ys[i])
        val e = point(total + beadR * 2f)
        lineTo(e.x, e.y)
    }
}

private fun buildStrand(w: Float, h: Float, design: BeadDesign, mirrored: Boolean = false): Strand {
    // Diagonal strand, edge to edge: beads advance TOP → BOTTOM as the count rises.
    // Right-handed (default) runs top-right → bottom-left (down-and-left); mirror
    // for left-handed (top-left → bottom-right). A gentle bow keeps it organic.
    fun px(x: Float) = if (mirrored) w - x else x
    val p0 = Offset(px(w * 0.82f), h * 0.05f) // start: top edge
    val p1 = Offset(px(w * 0.42f), h * 0.46f) // control: gentle bow
    val p2 = Offset(px(w * 0.16f), h * 0.95f) // end: bottom edge

    val n = 48
    val xs = FloatArray(n + 1)
    val ys = FloatArray(n + 1)
    val cum = FloatArray(n + 1)
    for (i in 0..n) {
        val t = i.toFloat() / n
        val u = 1f - t
        xs[i] = u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x
        ys[i] = u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y
        cum[i] = if (i == 0) 0f else cum[i - 1] + hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1])
    }
    val total = cum[n]

    fun normalize(o: Offset): Offset {
        val m = hypot(o.x, o.y)
        return if (m > 0f) Offset(o.x / m, o.y / m) else Offset(1f, 0f)
    }
    // tangents = derivative of the quadratic at the ends
    val startDir = normalize(Offset(2f * (p1.x - p0.x), 2f * (p1.y - p0.y)))
    val endDir = normalize(Offset(2f * (p2.x - p1.x), 2f * (p2.y - p1.y)))
    val midDir = normalize(Offset(2f * (p2.x - p0.x), 2f * (p2.y - p0.y))) // ~tangent at apex

    val beadR = minOf(w, h) * design.beadFraction
    val pack = beadR * design.pack
    val gap = pack * design.gapBeads
    val center = total * 0.5f
    return Strand(
        xs, ys, cum, startDir, endDir,
        total = total,
        pack = pack,
        beadR = beadR,
        gapTop = center - gap / 2f,
        gapBottom = center + gap / 2f,
        dirX = midDir.x,
        dirY = midDir.y,
    )
}

private fun DrawScope.drawStrand(pos: Float, beadCount: Int, design: BeadDesign, mirrored: Boolean) {
    val g = buildStrand(size.width, size.height, design, mirrored)
    val a0 = floor(pos).toInt()
    val frac = pos - a0
    // The imame (jade leader bead) marks each completed lap. For tiny targets every
    // bead would be a lap-end, flooding the strand green and hiding the design — so
    // only show it when the target is large enough to read as a sparse marker.
    fun imame(rank: Int) = beadCount >= 7 && ((rank % beadCount) + beadCount) % beadCount == 0

    drawPath(g.cordPath(), color = design.cord, style = Stroke(width = g.beadR * 0.16f, cap = StrokeCap.Round))

    // Lower bunch — counted beads, packed below the gap, drifting along with frac.
    var k = 0
    while (true) {
        val d = g.gapBottom + (k + frac) * g.pack
        if (d > g.total + g.beadR * 2f) break
        design.drawBead(this, g.point(d), g.beadR, if (imame(a0 - 1 - k)) design.imame else design.resting)
        k++
    }

    // Upper bunch — upcoming beads, packed above the gap, drifting along with frac.
    var m = 1
    while (true) {
        val d = g.gapTop - (m - frac) * g.pack
        if (d < -g.beadR * 2f) break
        design.drawBead(this, g.point(d), g.beadR, if (imame(a0 + m)) design.imame else design.resting)
        m++
    }

    // Loose bead crossing the gap — resting at both ends, warming gold mid-crossing,
    // with a soft glow as it travels the wide gap.
    val activeD = lerp(g.gapTop, g.gapBottom, frac)
    val t = sin(frac * Math.PI).toFloat()
    val center = g.point(activeD)
    if (t > 0.02f) {
        drawCircle(
            color = design.active[1].copy(alpha = 0.22f * t),
            radius = g.beadR * 1.9f,
            center = center
        )
    }
    // The crossing bead always warms to gold (the active highlight) — never green,
    // even when it is the imame; the imame still reads green at rest in the bunch.
    val activeColors = List(3) { i -> blend(design.resting[i], design.active[i], t) }
    design.drawBead(this, center, g.beadR * (1f + 0.16f * t), activeColors)
}

private fun blend(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

// Preview

@Preview(name = "Tasbih Beads", widthDp = 320, heightDp = 300, showBackground = true, backgroundColor = 0xFF0B100E)
@Composable
private fun TasbihBeadsPreview() {
    var count by remember { mutableIntStateOf(7) }
    Box(modifier = Modifier.background(Color(0xFF0B100E)).size(320.dp, 300.dp)) {
        TasbihBeads(
            count = count,
            onIncrement = { count++ },
            targetCount = 33,
            design = BeadDesigns.Wood,
            modifier = Modifier.fillMaxSize()
        )
    }
}
