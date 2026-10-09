package com.arshadshah.rail

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * One line of a lazy layout as it sits on screen: a list item, or a whole grid row (column for
 * a horizontal grid). Offsets and sizes are pixels along the scroll direction, in the lazy
 * layout's own coordinates (the first item's start is 0 when nothing is scrolled).
 */
internal class VisibleLine(
    val line: Int,
    val firstIndex: Int,
    val lastIndex: Int,
    val start: Int,
    val extent: Int,
)

/** What a lazy layout reported for one frame, stripped of Compose types so it can be tested on a plain JVM. */
internal class LazyFrame(
    val totalItems: Int,
    /** Ascending by [VisibleLine.line], one entry per line. */
    val lines: List<VisibleLine>,
    val viewportStart: Int,
    val viewportEnd: Int,
    val beforePadding: Int,
    val afterPadding: Int,
    val spacing: Int,
    /** Size across the scroll direction. A change (rotation, resize) makes every cached size stale. */
    val crossAxisSize: Int,
    val canScrollBackward: Boolean,
    val canScrollForward: Boolean,
)

/** Where to send a lazy layout: [index] with [offset] pixels scrolled past its start. */
internal data class LazySeek(val index: Int, val offset: Int)

/**
 * Turns a stream of lazy-layout frames into stable scrollbar numbers.
 *
 * A lazy layout only measures what is on screen, so the total length is a guess. Guessing from
 * the items on screen alone (an average of whatever is visible) makes the guess swing whenever a
 * short section header and a tall card trade places, and the thumb jumps and resizes with it.
 * This keeps three things steady instead:
 *
 * - **Every line it has ever measured is remembered**, in a Fenwick tree, so the estimate only
 *   ever gets better. Unmeasured lines are assumed to be the average of the measured ones.
 * - **The position is anchored on the last line on screen.** The lines between it and the first
 *   one are measured, so the start reads exactly 0 and the end exactly 1 without any pinning
 *   jump. (The first line is not used: a pinned sticky header reports itself there, at the top,
 *   while its index lags behind the content scrolling under it.)
 * - **The thumb moves by what the content actually moved.** Between two frames that share a line
 *   the pixel delta is exact, and the thumb's distance to the end it is heading for shrinks by the
 *   same ratio as the content's. Any disagreement with the fresh estimate is therefore folded in
 *   gradually instead of as a jump, the thumb never moves while the content is still, and it still
 *   lands exactly on the ends.
 *
 * [seek] inverts the same mapping, so grabbing the thumb and nudging it moves the content by a
 * nudge rather than snapping it to where the raw estimate thinks the thumb should be.
 */
internal class LazyScrollEstimator(private val denseLimit: Int = 1 shl 17) {
    // ---- per-line cache --------------------------------------------------------
    private var dense = true
    private var extents = IntArray(0) // -1 = not measured
    private var firsts = IntArray(0) // -1 = not seen
    private var sumTree = DoubleArray(0)
    private var countTree = IntArray(0)
    private val sparse = HashMap<Int, Int>()
    private var knownSum = 0.0
    private var knownCount = 0
    private var priorAverage = 0.0
    private var priorWeight = 0
    private var cachedTotal = -1
    private var cachedCross = -1

    // ---- last frame ------------------------------------------------------------
    private var frame: LazyFrame? = null
    private var previousStarts: Map<Int, Int>? = null
    private var lineCount = 0
    private var itemsPerLine = 1.0
    private var estimate = 0.0

    /** Where the thumb is, from 0 to 1. */
    var fraction = 0.0
        private set
    var contentSize = 0.0
        private set
    var viewportSize = 0.0
        private set
    val maxScrollOffset get() = max(0.0, contentSize - viewportSize)
    val scrollOffset get() = fraction * maxScrollOffset

    fun update(next: LazyFrame) {
        val previous = frame
        frame = next
        viewportSize = (next.viewportEnd - next.viewportStart).toDouble()
        if (next.totalItems <= 0 || next.lines.isEmpty()) {
            contentSize = 0.0; fraction = 0.0; estimate = 0.0; lineCount = 0
            previousStarts = null
            return
        }

        val stale = next.totalItems != cachedTotal || next.crossAxisSize != cachedCross ||
            next.lines.any { known(it.line) && firstOf(it.line) != it.firstIndex }
        if (stale) {
            // Same items re-flowed into other lines keep a useful average; a new cross-axis size does not.
            reset(next.totalItems, keepAverage = cachedTotal >= 0 && next.crossAxisSize == cachedCross)
            cachedTotal = next.totalItems
            cachedCross = next.crossAxisSize
        }
        next.lines.forEach(::record)

        val last = next.lines.last()
        itemsPerLine = when {
            last.line > 0 -> last.firstIndex.toDouble() / last.line
            else -> max(1, next.lines.maxOf { it.lastIndex - it.firstIndex + 1 }).toDouble()
        }.coerceAtLeast(1e-9)
        val remainingItems = next.totalItems - 1 - last.lastIndex
        lineCount = last.line + 1 + if (remainingItems > 0) ceil(remainingItems / itemsPerLine).toInt() else 0
        lineCount = lineCount.coerceAtMost(if (dense) extents.size else Int.MAX_VALUE)

        contentSize = (next.beforePadding + offsetOfLine(lineCount) - next.spacing + next.afterPadding).coerceAtLeast(0.0)
        val maximum = maxScrollOffset
        estimate = if (maximum > 0.0) {
            val scrolled = offsetOfLine(last.line) - last.start + next.beforePadding + next.viewportStart
            (scrolled / maximum).coerceIn(0.0, 1.0)
        } else 0.0

        val starts = next.lines.associate { it.line to it.start }
        val shared = if (stale || previous == null) null else next.lines.lastOrNull { previousStarts?.containsKey(it.line) == true }
        fraction = when {
            maximum <= 0.0 -> 0.0
            shared == null -> estimate
            else -> {
                // Shrink the thumb's distance to the end it is heading for by the same ratio the
                // content's distance to that end just shrank. In sync, that is a plain move by
                // `move`; out of sync, the gap closes as the end nears, and is gone at the end.
                val move = (previousStarts!!.getValue(shared.line) - shared.start) / maximum
                when {
                    move > 0.0 -> {
                        val before = 1.0 - estimate + move
                        if (before <= 0.0) 1.0 else 1.0 - (1.0 - fraction) * (1.0 - estimate) / before
                    }
                    move < 0.0 -> {
                        val before = estimate - move
                        if (before <= 0.0) 0.0 else fraction * estimate / before
                    }
                    else -> fraction
                }
            }
        }.coerceIn(0.0, 1.0)
        if (maximum > 0.0) {
            if (!next.canScrollBackward) fraction = 0.0
            if (!next.canScrollForward) fraction = 1.0
        }
        previousStarts = starts
    }

    /** Where [offset] (pixels along the thumb's scale, 0..[maxScrollOffset]) lives in the layout. */
    fun seek(offset: Double): LazySeek? {
        val f = frame ?: return null
        if (f.totalItems <= 0 || f.lines.isEmpty()) return null
        val maximum = maxScrollOffset
        val target = if (offset.isNaN()) 0.0 else offset.coerceIn(0.0, maximum)
        if (maximum > 0.0 && target >= maximum) return LazySeek(f.totalItems - 1, Int.MAX_VALUE)
        if (target <= 0.0) return LazySeek(0, 0)

        // The thumb shows `fraction`; the estimate says the content is at `estimate`. Carry their
        // gap along, shrinking it to nothing at whichever end the target heads towards.
        val shown = fraction * maximum
        val gap = (estimate - fraction) * maximum
        val weight = if (target >= shown) {
            if (maximum - shown > 0.0) (maximum - target) / (maximum - shown) else 0.0
        } else {
            if (shown > 0.0) target / shown else 0.0
        }
        val scrolled = (target + gap * weight.coerceIn(0.0, 1.0) - f.beforePadding - f.viewportStart).coerceAtLeast(0.0)

        // Largest line whose start is at or before `scrolled`.
        var low = 0
        var high = max(0, lineCount - 1)
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (offsetOfLine(mid) <= scrolled) low = mid else high = mid - 1
        }
        val index = (if (known(low)) firstOf(low) else (low * itemsPerLine).toInt()).coerceIn(0, f.totalItems - 1)
        return LazySeek(index, (scrolled - offsetOfLine(low)).toInt().coerceAtLeast(0))
    }

    // ---- cache ---------------------------------------------------------------------

    private val average: Double
        get() {
            val weight = knownCount + priorWeight
            return if (weight == 0) 0.0 else (knownSum + priorAverage * priorWeight) / weight
        }

    /** Distance from the first line's start to [line]'s start, spacing included. */
    private fun offsetOfLine(line: Int): Double {
        val f = frame ?: return 0.0
        if (line <= 0) return 0.0
        var sum = 0.0
        var count = 0
        if (dense) {
            var i = min(line, extents.size)
            while (i > 0) { sum += sumTree[i - 1]; count += countTree[i - 1]; i -= i and -i }
        }
        return sum + (line - count) * average + line.toDouble() * f.spacing
    }

    private fun known(line: Int) = dense && line in firsts.indices && firsts[line] >= 0

    private fun firstOf(line: Int) = firsts[line]

    private fun record(line: VisibleLine) {
        val l = line.line
        if (l < 0) return
        if (!dense) {
            val old = sparse.put(l, line.extent)
            if (old == null) { knownCount++; knownSum += line.extent } else knownSum += line.extent - old
            return
        }
        if (l >= extents.size) return
        firsts[l] = line.firstIndex
        val old = extents[l]
        if (old == line.extent) return
        extents[l] = line.extent
        if (old < 0) { knownCount++; knownSum += line.extent; add(l, line.extent.toDouble(), 1) }
        else { knownSum += line.extent - old; add(l, (line.extent - old).toDouble(), 0) }
    }

    private fun add(line: Int, value: Double, count: Int) {
        var i = line + 1
        while (i <= sumTree.size) { sumTree[i - 1] += value; countTree[i - 1] += count; i += i and -i }
    }

    private fun reset(total: Int, keepAverage: Boolean) {
        if (keepAverage && knownCount > 0) {
            priorAverage = average
            priorWeight = min(knownCount + priorWeight, PRIOR_WEIGHT)
        } else {
            priorAverage = 0.0
            priorWeight = 0
        }
        knownSum = 0.0
        knownCount = 0
        sparse.clear()
        dense = total <= denseLimit
        val size = if (dense) total else 0
        extents = IntArray(size) { -1 }
        firsts = IntArray(size) { -1 }
        sumTree = DoubleArray(size)
        countTree = IntArray(size)
        previousStarts = null
    }

    private companion object {
        /** How many lines' worth of say the old average keeps after the data changes. */
        const val PRIOR_WEIGHT = 16
    }
}
