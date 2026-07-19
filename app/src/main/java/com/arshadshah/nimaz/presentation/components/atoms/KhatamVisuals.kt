package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.KhatamPace
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Feature accent for Khatam, in the shape of
 * [com.arshadshah.nimaz.presentation.components.molecules.NamesAccent].
 *
 * One value threads through the hero card, row cards, journey trail and chips so the
 * feature re-themes correctly in dark mode without every call site repeating colour logic.
 */
@Immutable
data class KhatamAccent(
    /** Progress and "current" accent — the app's teal primary. */
    val progress: Color,
    val onProgress: Color,
    /** Completion accent — the app's gold secondary. */
    val complete: Color,
    val onComplete: Color,
    /** Muted fill for not-yet-started juz and archived khatams. */
    val muted: Color,
    val onMuted: Color,
    /** Unfilled portion of rings and bars. */
    val track: Color,
    /** Gradient used across progress bars and the walked trail. */
    val progressGradient: List<Color>,
)

@Composable
fun rememberKhatamAccent(): KhatamAccent {
    val c = MaterialTheme.colorScheme
    return remember(c) {
        KhatamAccent(
            progress = c.primary,
            onProgress = c.onPrimary,
            complete = c.secondary,
            onComplete = c.onSecondary,
            muted = c.surfaceVariant,
            onMuted = c.onSurfaceVariant,
            track = c.surfaceVariant,
            progressGradient = listOf(c.secondary, c.primary),
        )
    }
}

/**
 * Label and colour for a [KhatamPace] verdict.
 *
 * Centralised so the list hero, detail hero, home card and widget cannot describe the
 * same pace differently.
 */
@Composable
fun paceLabel(pace: KhatamPace): String = stringResource(
    when (pace) {
        KhatamPace.ON_TRACK -> R.string.khatam_pace_on_track
        KhatamPace.SLIGHTLY_BEHIND -> R.string.khatam_pace_slightly_behind
        KhatamPace.BEHIND -> R.string.khatam_pace_behind
        KhatamPace.NOT_STARTED -> R.string.khatam_pace_not_started
    }
)

@Composable
fun paceColor(pace: KhatamPace): Color = when (pace) {
    KhatamPace.ON_TRACK -> NimazColors.Success
    KhatamPace.SLIGHTLY_BEHIND -> NimazColors.Warning
    KhatamPace.BEHIND -> MaterialTheme.colorScheme.error
    KhatamPace.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Circular progress ring with the percentage centred inside.
 *
 * Replaces two hand-rolled rings that differed only in size — [size] and [strokeWidth]
 * are parameters precisely so a third copy never gets written.
 */
@Composable
fun KhatamProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 6.dp,
    accent: KhatamAccent = rememberKhatamAccent(),
    isComplete: Boolean = false,
    textStyle: TextStyle? = null,
    animated: Boolean = true,
) {
    val target = progress.coerceIn(0f, 1f)
    val sweep by animateFloatAsState(
        targetValue = if (animated) target else target,
        label = "khatamRingSweep"
    )
    val percent = (target * 100).toInt()
    val ringColor = if (isComplete) accent.complete else accent.progress
    val brush = remember(ringColor, accent.progressGradient, isComplete) {
        if (isComplete) Brush.linearGradient(listOf(accent.complete, accent.complete))
        else Brush.linearGradient(accent.progressGradient)
    }
    val description = stringResource(R.string.khatam_a11y_progress_ring, percent)

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = description }
            .drawBehind {
                val stroke = strokeWidth.toPx()
                val inset = stroke / 2f
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                drawArc(
                    color = accent.track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (sweep > 0f) {
                    drawArc(
                        brush = brush,
                        startAngle = -90f,
                        sweepAngle = 360f * sweep,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$percent%",
            style = textStyle ?: MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ringColor,
        )
    }
}

/**
 * Horizontal progress bar using the shared khatam gradient.
 *
 * The list used a stock [androidx.compose.material3.LinearProgressIndicator] while the
 * home card hand-rolled a gradient bar; this is the single rendering both now use.
 */
@Composable
fun KhatamProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    accent: KhatamAccent = rememberKhatamAccent(),
    isComplete: Boolean = false,
) {
    val target = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = target, label = "khatamBar")
    val brush = remember(accent, isComplete) {
        if (isComplete) Brush.horizontalGradient(listOf(accent.complete, accent.complete))
        else Brush.horizontalGradient(accent.progressGradient)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val radius = size.height / 2f
                drawRoundRect(
                    color = accent.track,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                )
                if (animatedProgress > 0f) {
                    drawRoundRect(
                        brush = brush,
                        size = Size(size.width * animatedProgress, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                    )
                }
            }
    )
}

// ---- Previews ----

@Composable
private fun KhatamRingShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KhatamProgressRing(progress = 0f, size = 44.dp, strokeWidth = 5.dp)
        KhatamProgressRing(progress = 0.38f, size = 56.dp)
        KhatamProgressRing(progress = 0.72f, size = 72.dp, strokeWidth = 8.dp)
        KhatamProgressRing(progress = 1f, size = 56.dp, isComplete = true)
    }
}

@Composable
private fun KhatamBarShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KhatamProgressBar(progress = 0f)
        KhatamProgressBar(progress = 0.38f)
        KhatamProgressBar(progress = 0.85f, height = 8.dp)
        KhatamProgressBar(progress = 1f, isComplete = true)
    }
}

@Preview(showBackground = true, name = "Khatam Ring — Light")
@Composable
private fun KhatamProgressRingLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { KhatamRingShowcase() }
}

@Preview(showBackground = true, name = "Khatam Ring — Dark")
@Composable
private fun KhatamProgressRingDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { KhatamRingShowcase() }
}

@Preview(showBackground = true, widthDp = 320, name = "Khatam Bar — Light")
@Composable
private fun KhatamProgressBarLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { KhatamBarShowcase() }
}

@Preview(showBackground = true, widthDp = 320, name = "Khatam Bar — Dark")
@Composable
private fun KhatamProgressBarDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { KhatamBarShowcase() }
}
