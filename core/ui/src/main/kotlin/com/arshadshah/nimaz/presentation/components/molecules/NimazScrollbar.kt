package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.rail.ScrollbarAdapter
import com.arshadshah.rail.ScrollbarBox
import com.arshadshah.rail.ScrollbarStyle
import com.arshadshah.rail.rememberScrollbarAdapter

/**
 * How a [NimazScrollbarBox] behaves.
 *
 * - [Standard] — a thin thumb that fades in while the content moves and can be dragged or tapped
 *   to jump. For any screen whose content runs past the viewport.
 * - [FastScroll] — a fixed-size handle for long, index-like lists (114 surahs, 272 licences, a
 *   hadith collection) where the user wants to race to a position. Pair it with a `label`.
 */
enum class NimazScrollbarVariant { Standard, FastScroll }

/**
 * The design system's scrollbar — Rail (`:core:rail`) dressed in the Nimaz theme.
 *
 * Wrap the scrolling content in this rather than reaching for Rail directly: the colours come
 * from `MaterialTheme.colorScheme`, so the thumb tracks light/dark and the dynamic palette, and
 * no screen has to know Rail exists. The bar overlays the content's end edge (start edge in RTL)
 * and never takes layout space.
 *
 * Pass the *same* state object to the content and to this box. For `verticalScroll`, pass the
 * same `reverseScrolling` flag to both.
 *
 * @param label shown beside the thumb while it is dragged; receives the drag position from 0 to
 *   1. Use [NimazScrollbarLabel] for the standard bubble.
 * @param contentDescription what TalkBack calls the bar. Null keeps Rail's own translated
 *   "Vertical scrollbar"; pass a resolved string to name the list instead.
 */
@Composable
fun NimazScrollbarBox(
    state: LazyListState,
    modifier: Modifier = Modifier,
    variant: NimazScrollbarVariant = NimazScrollbarVariant.Standard,
    label: (@Composable (progress: Float) -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) = ThemedScrollbarBox(rememberScrollbarAdapter(state), modifier, variant, label, contentDescription, content)

/** [NimazScrollbarBox] for a `LazyVerticalGrid` (uniform-span grids). */
@Composable
fun NimazScrollbarBox(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    variant: NimazScrollbarVariant = NimazScrollbarVariant.Standard,
    label: (@Composable (progress: Float) -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) = ThemedScrollbarBox(rememberScrollbarAdapter(state), modifier, variant, label, contentDescription, content)

/** [NimazScrollbarBox] for a `Column` with `Modifier.verticalScroll`. */
@Composable
fun NimazScrollbarBox(
    state: ScrollState,
    modifier: Modifier = Modifier,
    reverseScrolling: Boolean = false,
    variant: NimazScrollbarVariant = NimazScrollbarVariant.Standard,
    label: (@Composable (progress: Float) -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) = ThemedScrollbarBox(
    rememberScrollbarAdapter(state, reverseScrolling = reverseScrolling),
    modifier,
    variant,
    label,
    contentDescription,
    content,
)

/** The standard drag bubble for a [NimazScrollbarBox] label: a short string, e.g. `"Al-Kahf"`. */
@Composable
fun NimazScrollbarLabel(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/**
 * Maps a 0..1 drag position onto an index in a list of [count] items, for a label that names the
 * item under the thumb. Clamped, so 1.0 is the last item rather than one past it.
 */
fun scrollbarLabelIndex(progress: Float, count: Int): Int =
    if (count <= 0) 0 else (progress.coerceIn(0f, 1f) * (count - 1) + 0.5f).toInt().coerceIn(0, count - 1)

@Composable
private fun ThemedScrollbarBox(
    adapter: ScrollbarAdapter,
    modifier: Modifier,
    variant: NimazScrollbarVariant,
    label: (@Composable (progress: Float) -> Unit)?,
    contentDescription: String?,
    content: @Composable BoxScope.() -> Unit,
) {
    ScrollbarBox(
        adapter = adapter,
        modifier = modifier,
        style = nimazScrollbarStyle(variant),
        label = label,
        contentDescription = contentDescription,
        content = content,
    )
}

@Composable
internal fun nimazScrollbarStyle(variant: NimazScrollbarVariant): ScrollbarStyle {
    val colors = MaterialTheme.colorScheme
    val base = when (variant) {
        NimazScrollbarVariant.Standard -> ScrollbarStyle()
        NimazScrollbarVariant.FastScroll -> ScrollbarStyle.FastScroll
    }
    return base.copy(
        thumbColor = colors.onSurfaceVariant.copy(alpha = 0.45f),
        activeThumbColor = colors.primary,
        trackColor = colors.surfaceVariant,
    )
}
