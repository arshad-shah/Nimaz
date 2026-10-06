package com.arshadshah.rail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.roundToInt

/**
 * A vertical scrollbar. Place it along the end edge of your list
 * (or just use [ScrollbarBox], which does that for you).
 *
 * @param thumb draw your own thumb. Fills the thumb's area; leave null for the default pill.
 * @param label a bubble shown beside the thumb while dragging (e.g. "A", "B", "C" for contacts).
 *              Gets how far along the bar the thumb is, from 0 to 1.
 * @param contentDescription what accessibility services announce for the bar. Null uses Rail's
 *              own string, "Vertical scrollbar" / "Horizontal scrollbar", which ships translated
 *              and which an app can override like any library string resource.
 */
@Composable
fun VerticalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    style: ScrollbarStyle = ScrollbarStyle(),
    thumb: (@Composable (ThumbState) -> Unit)? = null,
    label: (@Composable (progress: Float) -> Unit)? = null,
    contentDescription: String? = null,
) = Scrollbar(adapter, Orientation.Vertical, modifier, style, thumb, label, descriptionOverride = contentDescription)

/** Same as [VerticalScrollbar], but for sideways scrolling. Place it along the bottom. */
@Composable
fun HorizontalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    style: ScrollbarStyle = ScrollbarStyle(),
    thumb: (@Composable (ThumbState) -> Unit)? = null,
    label: (@Composable (progress: Float) -> Unit)? = null,
    contentDescription: String? = null,
) = Scrollbar(adapter, Orientation.Horizontal, modifier, style, thumb, label, descriptionOverride = contentDescription)

// ---------------------------------------------------------------------------

/** A fresh object every time, so tapping the same spot twice still scrolls twice. */
private class ScrollRequest(val offset: Double)

@Composable
private fun Scrollbar(
    adapter: ScrollbarAdapter,
    orientation: Orientation,
    modifier: Modifier,
    style: ScrollbarStyle,
    thumb: (@Composable (ThumbState) -> Unit)?,
    label: (@Composable (Float) -> Unit)?,
    // Not `contentDescription`: inside `semantics {}` that name would shadow the semantics
    // property, and the assignment below would try to reassign this parameter.
    descriptionOverride: String?,
) {
    val vertical = orientation == Orientation.Vertical
    val description = descriptionOverride ?: stringResource(
        if (vertical) R.string.rail_vertical_scrollbar else R.string.rail_horizontal_scrollbar,
    )
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptics = LocalHapticFeedback.current
    val colors = resolveColors(style)

    val pad = with(density) { style.trackPadding.toPx() }
    val minThumb = with(density) { style.minThumbLength.toPx() }
    val maxThumb = with(density) { style.maxThumbLength.toPx() }

    var trackLength by remember(orientation) { mutableIntStateOf(0) }
    var isDragged by remember(adapter, orientation) { mutableStateOf(false) }
    // Where the thumb sits while held. Null when not held, so it follows the list instead.
    var dragOffset by remember(adapter, orientation) { mutableStateOf<Float?>(null) }
    var pendingScroll by remember(adapter, orientation) { mutableStateOf<ScrollRequest?>(null) }

    // Thumb size. Only recomposes when the size actually changes, not every scroll frame.
    val geometry by remember(adapter, orientation, pad, minThumb, maxThumb) {
        derivedStateOf {
            thumbGeometry(trackLength.toFloat(), pad, minThumb, maxThumb, adapter.contentSize, adapter.viewportSize)
        }
    }

    // --- mapping between list position and thumb position ---------------------

    fun thumbOffsetFromScroll(): Float {
        val g = geometry
        val max = adapter.maxScrollOffset
        if (!g.canScroll || max <= 0.0) return 0f
        return thumbPosition(adapter.scrollOffset, max, g.travel, adapter.isReversed)
    }

    fun currentThumbOffset(): Float = dragOffset ?: thumbOffsetFromScroll()

    fun scrollFor(thumbOffset: Float): Double {
        val g = geometry
        if (g.travel <= 0f) return 0.0
        return scrollPosition(thumbOffset, adapter.maxScrollOffset, g.travel, adapter.isReversed)
    }

    fun grab(at: Float) {
        isDragged = true
        dragOffset = at
        if (style.hapticOnGrab) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // --- one scroll job; a fast drag skips stale targets instead of queueing them ---

    LaunchedEffect(adapter, orientation) {
        snapshotFlow { pendingScroll }
            .filterNotNull()
            .collectLatest { adapter.scrollTo(it.offset) }
    }

    // --- fade in / out ---------------------------------------------------------

    val alpha = remember {
        Animatable(if (style.visibility is ScrollbarVisibility.AlwaysVisible) 1f else 0f)
    }
    LaunchedEffect(adapter, orientation, style.visibility) {
        when (val v = style.visibility) {
            ScrollbarVisibility.AlwaysVisible -> alpha.snapTo(1f)
            is ScrollbarVisibility.AutoHide ->
                snapshotFlow { adapter.isScrollInProgress || isDragged }.collectLatest { active ->
                    if (active) {
                        alpha.animateTo(1f, tween(v.fadeInMillis))
                    } else {
                        if (alpha.value > 0f && v.hideDelayMillis > 0) {
                            val start = withFrameNanos { it }
                            do { val elapsed = withFrameNanos { it } - start }
                            while (elapsed / 1_000_000 < v.hideDelayMillis)
                        }
                        alpha.animateTo(0f, tween(v.fadeOutMillis))
                    }
                }
        }
    }
    // When hidden, touches pass straight through to the list underneath.
    val touchable by remember { derivedStateOf { alpha.value > 0.05f } }

    val thickness by animateDpAsState(
        if (isDragged) style.activeThickness else style.thickness, label = "thickness",
    )
    val thumbColor by animateColorAsState(
        if (isDragged) colors.activeThumb else colors.thumb, label = "thumbColor",
    )

    // --- dragging --------------------------------------------------------------

    val dragState = rememberDraggableState { delta ->
        val current = dragOffset ?: return@rememberDraggableState
        val next = (current + delta).coerceIn(0f, geometry.travel)
        dragOffset = next
        pendingScroll = ScrollRequest(scrollFor(next))
    }

    val touch = style.touchTargetWidth
    val thumbLengthDp = with(density) { geometry.length.toDp() }

    Box(
        modifier
            .then(if (vertical) Modifier.fillMaxHeight().width(touch) else Modifier.fillMaxWidth().height(touch))
            .semantics {
                if (geometry.canScroll) {
                    contentDescription = description
                    val maximum = adapter.maxScrollOffset
                    val progress = if (maximum > 0 && adapter.scrollOffset.isFinite()) (adapter.scrollOffset / maximum).toFloat().coerceIn(0f, 1f) else 0f
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    setProgress { target ->
                        if (!target.isFinite()) false else {
                            pendingScroll = ScrollRequest(target.coerceIn(0f, 1f).toDouble() * maximum)
                            true
                        }
                    }
                }
            }
            .onSizeChanged { trackLength = if (vertical) it.height else it.width }
            .graphicsLayer { this.alpha = if (geometry.canScroll) alpha.value else 0f }
            .draggable(
                state = dragState,
                orientation = orientation,
                enabled = touchable && geometry.canScroll,
                startDragImmediately = true,
                reverseDirection = !vertical && rtl,
                onDragStarted = { start: Offset ->
                    val g = geometry
                    val along = when {
                        vertical -> start.y
                        rtl -> trackLength - start.x
                        else -> start.x
                    }
                    val thumbStart = pad + currentThumbOffset()
                    val onThumb = along in thumbStart..(thumbStart + g.length)
                    when {
                        onThumb -> grab(currentThumbOffset())

                        style.trackTap == TrackTapBehavior.JumpToPosition -> {
                            val target = (along - pad - g.length / 2).coerceIn(0f, g.travel)
                            grab(target)
                            pendingScroll = ScrollRequest(scrollFor(target))
                        }

                        style.trackTap == TrackTapBehavior.PageStep -> {
                            var dir = if (along < thumbStart) -1 else 1
                            if (adapter.isReversed) dir = -dir
                            val target = adapter.scrollOffset + dir * adapter.viewportSize
                            pendingScroll = ScrollRequest(target.coerceIn(0.0, adapter.maxScrollOffset))
                        }

                        else -> Unit
                    }
                },
                onDragStopped = {
                    isDragged = false
                    dragOffset = null
                },
            ),
    ) {
        // Track (only drawn if the style asks for it)
        if (colors.track != Color.Transparent) {
            Box(
                Modifier
                    .align(if (vertical) Alignment.CenterEnd else Alignment.BottomCenter)
                    .then(
                        if (vertical) Modifier.padding(end = style.edgePadding, top = style.trackPadding, bottom = style.trackPadding)
                            .fillMaxHeight().width(thickness)
                        else Modifier.padding(bottom = style.edgePadding, start = style.trackPadding, end = style.trackPadding)
                            .fillMaxWidth().height(thickness),
                    )
                    .background(colors.track, style.thumbShape),
            )
        }

        // Thumb. Its position is read during layout, so scrolling doesn't recompose anything.
        Box(
            Modifier
                .offset {
                    val o = (pad + currentThumbOffset()).roundToInt()
                    if (vertical) IntOffset(0, o) else IntOffset(o, 0)
                }
                .then(
                    if (vertical) Modifier.width(touch).height(thumbLengthDp)
                    else Modifier.height(touch).width(thumbLengthDp),
                )
                // Stops Android's back-swipe from stealing the thumb at the screen edge.
                .then(if (touchable && geometry.canScroll) Modifier.systemGestureExclusion() else Modifier),
            contentAlignment = if (vertical) Alignment.CenterEnd else Alignment.BottomCenter,
        ) {
            val state = ThumbState(isDragged)
            if (thumb != null) {
                thumb(state)
            } else {
                Box(
                    if (vertical) {
                        Modifier.padding(end = style.edgePadding).fillMaxHeight().width(thickness)
                    } else {
                        Modifier.padding(bottom = style.edgePadding).fillMaxWidth().height(thickness)
                    }.background(thumbColor, style.thumbShape),
                )
            }
        }

        // Label bubble beside the thumb, only while dragging.
        if (label != null) {
            AnimatedVisibility(
                visible = isDragged,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier.layout { measurable, _ ->
                    val p = measurable.measure(Constraints()) // free to be any size
                    layout(0, 0) { // takes no room, just floats next to the thumb
                        val center = pad + currentThumbOffset() + geometry.length / 2
                        val gap = style.labelGap.roundToPx()
                        if (vertical) {
                            p.placeRelative(-p.width - gap, (center - p.height / 2f).roundToInt())
                        } else {
                            p.placeRelative((center - p.width / 2f).roundToInt(), -p.height - gap)
                        }
                    }
                },
            ) {
                val g = geometry
                label(if (g.travel > 0f) (currentThumbOffset() / g.travel).coerceIn(0f, 1f) else 0f)
            }
        }
    }
}
