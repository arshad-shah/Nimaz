package com.arshadshah.rail

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** When the scrollbar shows up. */
sealed interface ScrollbarVisibility {
    /** Always on screen (if there's anything to scroll). */
    data object AlwaysVisible : ScrollbarVisibility

    /** Fades in while scrolling, fades out after a short pause. */
    data class AutoHide(
        val hideDelayMillis: Long = 1200,
        val fadeInMillis: Int = 150,
        val fadeOutMillis: Int = 300,
    ) : ScrollbarVisibility {
        init {
            require(hideDelayMillis >= 0) { "hideDelayMillis must be nonnegative" }
            require(fadeInMillis >= 0) { "fadeInMillis must be nonnegative" }
            require(fadeOutMillis >= 0) { "fadeOutMillis must be nonnegative" }
        }
    }
}

/** What happens when you tap the bar somewhere other than the thumb. */
enum class TrackTapBehavior {
    /** Nothing. Only dragging the thumb works. */
    None,
    /** Thumb jumps to where you tapped, and you can keep dragging from there. */
    JumpToPosition,
    /** Moves one screen up or down, like a desktop scrollbar. */
    PageStep,
}

/** Passed to a custom thumb so it can react to being held. */
@Immutable
data class ThumbState(val isDragged: Boolean)

/**
 * Everything about how the scrollbar looks and behaves.
 * Colors left as [Color.Unspecified] pick a sensible grey for light/dark mode.
 */
@Immutable
data class ScrollbarStyle(
    /** Thumb thickness at rest. */
    val thickness: Dp = 4.dp,
    /** Thumb thickness while held — it grows so you can see you've grabbed it. */
    val activeThickness: Dp = 8.dp,
    /** Thumb never gets shorter than this, even on huge lists. */
    val minThumbLength: Dp = 40.dp,
    /** Thumb never gets longer than this. Set equal to min for a fixed-size "fast scroll" handle. */
    val maxThumbLength: Dp = Dp.Infinity,
    /** How wide the touchable strip is. Bigger than the visible thumb so it's easy to grab. */
    val touchTargetWidth: Dp = 24.dp,
    /** Gap between the thumb and the screen edge. */
    val edgePadding: Dp = 3.dp,
    /** Gap at both ends of the bar. */
    val trackPadding: Dp = 4.dp,
    /** Gap between the thumb and its label bubble. */
    val labelGap: Dp = 12.dp,
    val thumbShape: Shape = RoundedCornerShape(50),
    val thumbColor: Color = Color.Unspecified,
    val activeThumbColor: Color = Color.Unspecified,
    val showTrack: Boolean = false,
    val trackColor: Color = Color.Unspecified,
    val visibility: ScrollbarVisibility = ScrollbarVisibility.AutoHide(),
    val trackTap: TrackTapBehavior = TrackTapBehavior.JumpToPosition,
    /** Small vibration when you grab the thumb. */
    val hapticOnGrab: Boolean = true,
) {
    init {
        fun positive(name: String, value: Dp) {
            require(value.value.isFinite() && value.value > 0f) { "$name must be positive and finite" }
        }
        fun nonnegative(name: String, value: Dp) {
            require(value.value.isFinite() && value.value >= 0f) { "$name must be nonnegative and finite" }
        }
        positive("thickness", thickness)
        positive("activeThickness", activeThickness)
        positive("minThumbLength", minThumbLength)
        require(!maxThumbLength.value.isNaN() && maxThumbLength >= minThumbLength) { "maxThumbLength must be at least minThumbLength" }
        positive("touchTargetWidth", touchTargetWidth)
        nonnegative("edgePadding", edgePadding)
        nonnegative("trackPadding", trackPadding)
        nonnegative("labelGap", labelGap)
        require(touchTargetWidth >= maxOf(thickness, activeThickness) + edgePadding) { "touchTargetWidth must contain the thumb and edge padding" }
    }

    companion object {
        /** Thin and quiet. Just shows where you are. */
        val Minimal = ScrollbarStyle(
            thickness = 3.dp,
            activeThickness = 6.dp,
            trackTap = TrackTapBehavior.None,
        )

        /** Chunky fixed-size handle for racing through long lists. Pair with a label. */
        val FastScroll = ScrollbarStyle(
            thickness = 6.dp,
            activeThickness = 12.dp,
            minThumbLength = 52.dp,
            maxThumbLength = 52.dp,
            touchTargetWidth = 36.dp,
        )

        /** Desktop feel: always visible, with a track, tap to page. */
        val Classic = ScrollbarStyle(
            thickness = 8.dp,
            activeThickness = 8.dp,
            showTrack = true,
            visibility = ScrollbarVisibility.AlwaysVisible,
            trackTap = TrackTapBehavior.PageStep,
            thumbShape = RoundedCornerShape(4.dp),
        )
    }
}

@Immutable
internal data class ResolvedColors(val thumb: Color, val activeThumb: Color, val track: Color)

@Composable
internal fun resolveColors(style: ScrollbarStyle): ResolvedColors {
    val base = if (isSystemInDarkTheme()) Color.White else Color.Black
    return ResolvedColors(
        thumb = style.thumbColor.takeOrElse { base.copy(alpha = 0.35f) },
        activeThumb = style.activeThumbColor.takeOrElse { base.copy(alpha = 0.65f) },
        track = if (style.showTrack) style.trackColor.takeOrElse { base.copy(alpha = 0.08f) } else Color.Transparent,
    )
}
