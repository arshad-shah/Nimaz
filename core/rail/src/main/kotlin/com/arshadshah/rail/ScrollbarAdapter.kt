package com.arshadshah.rail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
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
// LazyColumn / LazyRow / LazyVerticalGrid / LazyHorizontalGrid
//
// A lazy layout only knows the size of what is on screen, so the total is a
// guess. [LazyScrollEstimator] remembers every line it has measured and moves
// the thumb by what the content really moved, so headers and tall cards trading
// places do not make the thumb jump or resize. See that class for the details.
// ---------------------------------------------------------------------------

/** Shared plumbing: turn each new layout into a [LazyFrame] once, then answer from the estimator. */
internal abstract class LazyLayoutAdapter : ScrollbarAdapter {
    private val estimator = LazyScrollEstimator()
    private var seen: Any? = null

    /** The layout info object; a new one arrives with every layout pass. */
    protected abstract val layout: Any
    protected abstract fun frame(): LazyFrame
    protected abstract suspend fun scrollToItem(index: Int, offset: Int)

    // Reading `layout` here is what makes snapshot observers (the thumb's derived state)
    // re-run when the list moves. Feeding the same frame twice is a no-op.
    private fun synced(): LazyScrollEstimator {
        val current = layout
        if (current !== seen) {
            seen = current
            estimator.update(frame())
        }
        return estimator
    }

    override val scrollOffset get() = synced().scrollOffset
    override val contentSize get() = synced().contentSize
    override val viewportSize get() = synced().viewportSize

    override suspend fun scrollTo(offset: Double) {
        val seek = synced().seek(offset) ?: return
        scrollToItem(seek.index, seek.offset)
    }
}

internal class LazyListAdapter(private val state: LazyListState) : LazyLayoutAdapter() {
    override val layout get() = state.layoutInfo
    override val isReversed get() = state.layoutInfo.reverseLayout
    override val isScrollInProgress get() = state.isScrollInProgress

    override fun frame(): LazyFrame {
        val info = state.layoutInfo
        // Each item is its own line. Sorted, because a pinned sticky header may be listed out of order.
        val lines = info.visibleItemsInfo
            .map { VisibleLine(it.index, it.index, it.index, it.offset, it.size) }
            .sortedBy { it.line }
            .distinctBy { it.line }
        val cross = if (info.orientation == Orientation.Vertical) info.viewportSize.width else info.viewportSize.height
        return LazyFrame(
            totalItems = info.totalItemsCount,
            lines = lines,
            viewportStart = info.viewportStartOffset,
            viewportEnd = info.viewportEndOffset,
            beforePadding = info.beforeContentPadding,
            afterPadding = info.afterContentPadding,
            spacing = info.mainAxisItemSpacing,
            crossAxisSize = cross,
            canScrollBackward = state.canScrollBackward,
            canScrollForward = state.canScrollForward,
        )
    }

    override suspend fun scrollToItem(index: Int, offset: Int) = state.scrollToItem(index, offset)
}

internal class LazyGridAdapter(private val state: LazyGridState) : LazyLayoutAdapter() {
    override val layout get() = state.layoutInfo
    override val isReversed get() = state.layoutInfo.reverseLayout
    override val isScrollInProgress get() = state.isScrollInProgress

    override fun frame(): LazyFrame {
        val info = state.layoutInfo
        val vertical = info.orientation == Orientation.Vertical
        fun lineOf(i: LazyGridItemInfo) = if (vertical) i.row else i.column
        val lines = info.visibleItemsInfo
            .filter { lineOf(it) >= 0 }
            .groupBy(::lineOf)
            .map { (line, items) ->
                VisibleLine(
                    line = line,
                    firstIndex = items.minOf { it.index },
                    lastIndex = items.maxOf { it.index },
                    start = items.minOf { if (vertical) it.offset.y else it.offset.x },
                    extent = items.maxOf { if (vertical) it.size.height else it.size.width },
                )
            }
            .sortedBy { it.line }
        return LazyFrame(
            totalItems = info.totalItemsCount,
            lines = lines,
            viewportStart = info.viewportStartOffset,
            viewportEnd = info.viewportEndOffset,
            beforePadding = info.beforeContentPadding,
            afterPadding = info.afterContentPadding,
            spacing = info.mainAxisItemSpacing,
            crossAxisSize = if (vertical) info.viewportSize.width else info.viewportSize.height,
            canScrollBackward = state.canScrollBackward,
            canScrollForward = state.canScrollForward,
        )
    }

    override suspend fun scrollToItem(index: Int, offset: Int) = state.scrollToItem(index, offset)
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
