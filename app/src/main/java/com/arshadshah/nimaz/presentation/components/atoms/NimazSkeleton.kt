package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.LocalAnimationsEnabled
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A shimmering placeholder block, shaped like the content it stands in for.
 *
 * Prefer this over a spinner whenever the shape of the incoming content is known:
 * a skeleton holds the layout still, so the screen does not jump when data
 * arrives, and it reads as "this is loading" rather than "the app is busy".
 * A spinner ([NimazLoadingState]) is the right choice only when the result's
 * shape is unknown or the wait is very short.
 *
 * Honours [LocalAnimationsEnabled]: with animations off the block renders as a
 * flat tint, so reduced-motion users are not shown a looping gradient.
 */
@Composable
fun NimazSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val base = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SKELETON_BASE_ALPHA)
    val highlight = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SKELETON_HIGHLIGHT_ALPHA)

    val animated = LocalAnimationsEnabled.current
    val brush = if (animated) {
        // Sweep a soft highlight across the block. The travel distance is tied to
        // screen width so the sweep speed reads the same on a phone and a tablet.
        val widthPx = with(LocalConfiguration.current) { screenWidthDp.toFloat() } * 3f
        val transition = rememberInfiniteTransition(label = "skeleton")
        val offset by transition.animateFloat(
            initialValue = -widthPx,
            targetValue = widthPx,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = SHIMMER_DURATION_MS),
                repeatMode = RepeatMode.Restart
            ),
            label = "skeleton-offset"
        )
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(offset, 0f),
            end = Offset(offset + widthPx, 0f)
        )
    } else {
        Brush.linearGradient(listOf(base, base))
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
            // A placeholder carries no information; announcing it would read as
            // a stray empty element between real content.
            .clearAndSetSemantics { }
    )
}

/**
 * A paragraph-shaped skeleton: [lines] full-width bars with a shortened last
 * line, matching how real text ends mid-row.
 */
@Composable
fun NimazSkeletonText(
    modifier: Modifier = Modifier,
    lines: Int = 3,
    lineHeight: Dp = 11.dp,
    lastLineFraction: Float = 0.6f,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(lines) { index ->
            val isLast = index == lines - 1
            NimazSkeleton(
                modifier = Modifier
                    .fillMaxWidth(if (isLast) lastLineFraction else 1f)
                    .height(lineHeight)
            )
        }
    }
}

/**
 * A skeleton shaped like a list row — leading circle, two stacked text bars.
 * Repeat it inside a list while the real rows load.
 */
@Composable
fun NimazSkeletonRow(
    modifier: Modifier = Modifier,
    showLeading: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLeading) {
            NimazSkeleton(
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            NimazSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
            )
            NimazSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(10.dp)
            )
        }
    }
}

/** Base and highlight opacity of a skeleton block's sweeping gradient. */
private const val SKELETON_BASE_ALPHA = 0.5f
private const val SKELETON_HIGHLIGHT_ALPHA = 0.15f
private const val SHIMMER_DURATION_MS = 1200

// ==================== PREVIEWS ====================

@Composable
private fun NimazSkeletonShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        NimazSkeletonText()
        NimazSkeletonRow()
        NimazSkeletonRow(showLeading = false)
        NimazSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Skeleton — Light")
@Composable
private fun NimazSkeletonLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazSkeletonShowcase()
    }
}

@Preview(
    showBackground = true, name = "Skeleton — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazSkeletonDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazSkeletonShowcase()
    }
}
