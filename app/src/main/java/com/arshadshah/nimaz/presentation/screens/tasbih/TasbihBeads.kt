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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * Hand-drawn tasbih (misbaha) counter. The full loop is hidden — only the
 * visible diagonal **strand** is drawn: beads bunch in from the top edge and out
 * the bottom edge, with a **gap** in the middle holding one loose bead. **Tap**,
 * or **flick that bead down across the gap**, advances the count; the strand
 * rotates down by one and a fresh bead drops into the gap. One flick = one bead.
 * The **imame** (green) leader bead returns to the gap once per [targetCount].
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
) {
    val scope = rememberCoroutineScope()
    val beadCount = if (targetCount in 5..99) targetCount else 33

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
        modifier = modifier.pointerInput(design) {
            val g = buildStrand(size.width.toFloat(), size.height.toFloat(), design)
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
        drawStrand(pos.value, beadCount, design)
    }
}

/** Visible-strand geometry: a straight diagonal line top-right → bottom-left. */
private class Strand(
    val ax: Float, val ay: Float,
    val dirX: Float, val dirY: Float,
    val total: Float,
    val pack: Float,
    val beadR: Float,
    val gapTop: Float,
    val gapBottom: Float,
) {
    fun point(d: Float) = Offset(ax + dirX * d, ay + dirY * d)
}

private fun buildStrand(w: Float, h: Float, design: BeadDesign): Strand {
    val ax = w * 0.70f; val ay = 0f
    val exX = w * 0.30f
    val dx = exX - ax; val dy = h - ay
    val total = hypot(dx, dy)
    val beadR = minOf(w, h) * design.beadFraction
    val pack = beadR * design.pack
    val gap = pack * design.gapBeads
    val center = total * 0.5f
    return Strand(ax, ay, dx / total, dy / total, total, pack, beadR, center - gap / 2f, center + gap / 2f)
}

private fun DrawScope.drawStrand(pos: Float, beadCount: Int, design: BeadDesign) {
    val g = buildStrand(size.width, size.height, design)
    val a0 = floor(pos).toInt()
    val frac = pos - a0
    fun imame(rank: Int) = ((rank % beadCount) + beadCount) % beadCount == 0

    drawLine(design.cord, g.point(-g.beadR), g.point(g.total + g.beadR), strokeWidth = g.beadR * 0.18f)

    // Bottom bunch — counted beads, packed below the gap, drifting down with frac.
    var k = 0
    while (true) {
        val d = g.gapBottom + (k + frac) * g.pack
        if (d > g.total + g.beadR) break
        design.drawBead(this, g.point(d), g.beadR, if (imame(a0 - 1 - k)) design.imame else design.wood)
        k++
    }

    // Top bunch — upcoming beads, packed above the gap, drifting down with frac.
    var m = 1
    while (true) {
        val d = g.gapTop - (m - frac) * g.pack
        if (d < -g.beadR) break
        design.drawBead(this, g.point(d), g.beadR, if (imame(a0 + m)) design.imame else design.wood)
        m++
    }

    // Loose bead crossing the gap — wood at both ends, warming gold mid-crossing.
    val activeD = lerp(g.gapTop, g.gapBottom, frac)
    val t = sin(frac * Math.PI).toFloat()
    val activeColors = if (imame(a0)) design.imame else List(3) { i ->
        blend(design.wood[i], design.gold[i], t)
    }
    design.drawBead(this, g.point(activeD), g.beadR * (1f + 0.14f * t), activeColors)
}

private fun blend(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

// Preview

@Preview(name = "Tasbih Beads", widthDp = 320, heightDp = 560, showBackground = true, backgroundColor = 0xFF12151D)
@Composable
private fun TasbihBeadsPreview() {
    var count by remember { mutableIntStateOf(7) }
    Box(modifier = Modifier.background(Color(0xFF12151D)).size(320.dp, 560.dp)) {
        TasbihBeads(
            count = count,
            onIncrement = { count++ },
            targetCount = 33,
            modifier = Modifier.fillMaxSize()
        )
    }
}
