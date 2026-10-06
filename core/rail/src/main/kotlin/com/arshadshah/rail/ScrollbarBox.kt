package com.arshadshah.rail

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Wrap your list in this and you get a scrollbar on top of it, on the right side
 * (left side in right-to-left languages), or along the bottom for sideways lists.
 */
@Composable
fun ScrollbarBox(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    style: ScrollbarStyle = ScrollbarStyle(),
    thumb: (@Composable (ThumbState) -> Unit)? = null,
    label: (@Composable (progress: Float) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        content()
        // matchParentSize: the bar takes the list's size and never makes the Box bigger.
        Box(
            Modifier.matchParentSize(),
            contentAlignment = if (orientation == Orientation.Vertical) Alignment.CenterEnd else Alignment.BottomCenter,
        ) {
            if (orientation == Orientation.Vertical) {
                VerticalScrollbar(adapter, style = style, thumb = thumb, label = label)
            } else {
                HorizontalScrollbar(adapter, style = style, thumb = thumb, label = label)
            }
        }
    }
}
