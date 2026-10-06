package com.arshadshah.rail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlin.math.floor
import kotlin.math.max

/**
 * The bridge between "something that scrolls" and the scrollbar.
 *
 * The scrollbar only needs to know four things: how far we've scrolled, how long the
 * whole content is, how much of it fits on screen, and how to jump somewhere.
 * All numbers are pixels along the scroll direction.
 *
 * Implement this yourself to plug in anything (a custom pager, a WebView, etc).
 */
@Stable
interface ScrollbarAdapter {
    /** Pixels scrolled from the very start. */
    val scrollOffset: Double

    /** Full length of the content. For lazy lists this is a best guess. */
    val contentSize: Double

    /** Length of the part you can see. */
    val viewportSize: Double

    /** True while the content is moving (finger, fling, or code). */
    val isScrollInProgress: Boolean

    /** True when the list starts at the bottom/end (e.g. chat screens with reverseLayout). */
    val isReversed: Boolean get() = false

    /** Jump straight to [offset] pixels from the start. */
    suspend fun scrollTo(offset: Double)
}

/** The furthest you can scroll. */
val ScrollbarAdapter.maxScrollOffset: Double
    get() {
        val content = contentSize
        val viewport = viewportSize
        return if (content.isFinite() && viewport.isFinite() && viewport >= 0.0) max(0.0, content - viewport) else 0.0
    }

// ---------------------------------------------------------------------------
// Plain Column/Row with Modifier.verticalScroll / horizontalScroll
// ---------------------------------------------------------------------------

internal class ScrollStateAdapter(private val state: ScrollState, override val isReversed: Boolean = false) : ScrollbarAdapter {
    // Before the first layout, maxValue is Int.MAX_VALUE. Treat that as "nothing to scroll".
    private val safeMax get() = if (state.maxValue == Int.MAX_VALUE) 0 else state.maxValue

    override val scrollOffset get() = state.value.toDouble()
    override val viewportSize get() = state.viewportSize.toDouble()
    override val contentSize get() = safeMax.toDouble() + state.viewportSize.toDouble()
    override val isScrollInProgress get() = state.isScrollInProgress
    override suspend fun scrollTo(offset: Double) { state.scrollTo(offset.toInt()) }
}

// ---------------------------------------------------------------------------
// LazyColumn / LazyRow
//
// A lazy list only knows the size of the items on screen, so we take their
// average size and pretend every item is that size. Good enough for a smooth
// thumb, and we pin the thumb to the exact ends so it never looks "off".
// ---------------------------------------------------------------------------

internal class LazyListAdapter(private val state: LazyListState) : ScrollbarAdapter {
    private val info get() = state.layoutInfo
    private val spacing get() = info.mainAxisItemSpacing.toDouble()

    /** Average item size on screen, plus the gap after it. */
    private val itemStep: Double
        get() {
            val items = info.visibleItemsInfo
            if (items.isEmpty()) return 0.0
            return items.sumOf { it.size }.toDouble() / items.size + spacing
        }

    override val isReversed get() = info.reverseLayout
    override val isScrollInProgress get() = state.isScrollInProgress
    override val viewportSize get() = (info.viewportEndOffset - info.viewportStartOffset).toDouble()

    override val contentSize: Double
        get() {
            val count = info.totalItemsCount
            if (count == 0) return 0.0
            return itemStep * count - spacing + info.beforeContentPadding + info.afterContentPadding
        }

    override val scrollOffset: Double
        get() {
            // Pin to the real ends so the guesswork never shows at the top or bottom.
            if (!state.canScrollBackward) return 0.0
            if (!state.canScrollForward) return maxScrollOffset
            val first = info.visibleItemsInfo.firstOrNull() ?: return 0.0
            return first.index * itemStep - first.offset
        }

    override suspend fun scrollTo(offset: Double) {
        val step = itemStep
        if (step <= 0.0) return
        val target = if (offset.isNaN()) 0.0 else offset.coerceIn(0.0, maxScrollOffset)
        if (target >= maxScrollOffset && maxScrollOffset > 0.0) {
            state.scrollToItem((info.totalItemsCount - 1).coerceAtLeast(0), Int.MAX_VALUE)
            return
        }
        val index = floor(target / step).toInt().coerceIn(0, max(0, info.totalItemsCount - 1))
        state.scrollToItem(index, (target - index * step).toInt())
    }
}

// ---------------------------------------------------------------------------
// LazyVerticalGrid / LazyHorizontalGrid
//
// Same idea as lists, but we work in lines (rows for a vertical grid).
// ---------------------------------------------------------------------------

internal class LazyGridAdapter(private val state: LazyGridState) : ScrollbarAdapter {
    private val info get() = state.layoutInfo
    private val vertical get() = info.orientation == Orientation.Vertical
    private val spacing get() = info.mainAxisItemSpacing.toDouble()

    private fun lineOf(i: LazyGridItemInfo) = if (vertical) i.row else i.column
    private fun sizeOf(i: LazyGridItemInfo) = if (vertical) i.size.height else i.size.width
    private fun startOf(i: LazyGridItemInfo) = if (vertical) i.offset.y else i.offset.x

    private val visibleLines: Map<Int, List<LazyGridItemInfo>>
        get() = info.visibleItemsInfo.filter { lineOf(it) >= 0 }.groupBy(::lineOf)

    /** How many items sit side by side, judged from the fullest line on screen. */
    private val itemsPerLine: Int
        get() {
            val lines = visibleLines
            val occupied = lines.values.maxOfOrNull { it.size } ?: 1
            // A viewport can contain only a partial final line. Uniform spans let
            // us recover the actual cross-axis count from logical coordinates.
            val inferred = lines.values.flatten().mapNotNull { item ->
                val line = lineOf(item)
                val crossSlot = if (vertical) item.column else item.row
                if (line > 0 && crossSlot >= 0) (item.index - crossSlot) / line else null
            }.maxOrNull() ?: 1
            return maxOf(occupied, inferred, 1)
        }

    /** Average line height (or width), plus the gap after it. */
    private val lineStep: Double
        get() {
            val lines = visibleLines
            if (lines.isEmpty()) return 0.0
            val total = lines.values.sumOf { line -> line.maxOf(::sizeOf) }
            return total.toDouble() / lines.size + spacing
        }

    private val lineCount get() = (info.totalItemsCount + itemsPerLine - 1) / itemsPerLine

    override val isReversed get() = info.reverseLayout
    override val isScrollInProgress get() = state.isScrollInProgress
    override val viewportSize get() = (info.viewportEndOffset - info.viewportStartOffset).toDouble()

    override val contentSize: Double
        get() {
            if (info.totalItemsCount == 0) return 0.0
            return lineStep * lineCount - spacing + info.beforeContentPadding + info.afterContentPadding
        }

    override val scrollOffset: Double
        get() {
            if (!state.canScrollBackward) return 0.0
            if (!state.canScrollForward) return maxScrollOffset
            val (firstLine, items) = visibleLines.minByOrNull { it.key } ?: return 0.0
            return firstLine * lineStep - items.minOf(::startOf)
        }

    override suspend fun scrollTo(offset: Double) {
        val step = lineStep
        if (step <= 0.0) return
        val target = if (offset.isNaN()) 0.0 else offset.coerceIn(0.0, maxScrollOffset)
        if (target >= maxScrollOffset && maxScrollOffset > 0.0) {
            state.scrollToItem((info.totalItemsCount - 1).coerceAtLeast(0), Int.MAX_VALUE)
            return
        }
        val line = floor(target / step).toInt()
        val index = (line * itemsPerLine).coerceIn(0, max(0, info.totalItemsCount - 1))
        state.scrollToItem(index, (target - line * step).toInt())
    }
}

// ---------------------------------------------------------------------------
// remember helpers
// ---------------------------------------------------------------------------

@Composable
fun rememberScrollbarAdapter(state: ScrollState): ScrollbarAdapter =
    rememberScrollbarAdapter(state, reverseScrolling = false)

/** Pass the same reverseScrolling value used by verticalScroll or horizontalScroll. */
@Composable
fun rememberScrollbarAdapter(state: ScrollState, reverseScrolling: Boolean): ScrollbarAdapter =
    remember(state, reverseScrolling) { ScrollStateAdapter(state, reverseScrolling) }

@Composable
fun rememberScrollbarAdapter(state: LazyListState): ScrollbarAdapter =
    remember(state) { LazyListAdapter(state) }

@Composable
fun rememberScrollbarAdapter(state: LazyGridState): ScrollbarAdapter =
    remember(state) { LazyGridAdapter(state) }
