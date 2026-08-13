package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Track thickness. */
enum class NimazProgressSize(val height: Dp) {
    THIN(4.dp),
    MEDIUM(6.dp),
    THICK(10.dp)
}

/**
 * The app's determinate progress track.
 *
 * Eight files hand-rolled `LinearProgressIndicator` with their own height, shape and colours
 * before this existed, which is eight independent answers to "how thick is a progress bar".
 *
 * [progress] is coerced **here** rather than at each call site: eight callers is eight chances
 * for a `NaN` from a zero denominator or a fraction that overshoots, and a progress bar should
 * never be the thing that takes a screen down.
 *
 * @param progress fraction complete. `NaN` reads as `0f`; everything else is clamped to `0f..1f`.
 * @param tone semantic colour of the filled portion.
 * @param size thickness rung.
 * @param gradient ramps the fill from the tone into gold. Reserved for celebratory progress —
 *   the Ramadan strip — not everyday bars, which should read as one flat colour.
 * @param trackColor overrides the unfilled bed; `null` uses the tone's container colour.
 * @param contentDescription accessibility label; `null` leaves the bar decorative, which is
 *   correct when an adjacent label already states the numbers it is drawing.
 */
@Composable
fun NimazProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    tone: NimazTone = NimazTone.ACCENT,
    size: NimazProgressSize = NimazProgressSize.MEDIUM,
    gradient: Boolean = false,
    trackColor: Color? = null,
    contentDescription: String? = null,
) {
    val safeProgress = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    val fillColor = NimazToneColors.foreground(tone)
    val bed = trackColor ?: NimazToneColors.container(tone)
    val shape = RoundedCornerShape(percent = 50)

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(size.height)
            .clip(shape)
            .background(bed)
            .then(semanticsModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                // Measured rather than `fillMaxWidth(fraction)`: a fraction of 0f still lays out
                // a node whose rounded clip paints a stray pip at the left edge, so an empty bar
                // reads as a bar with something in it.
                .layout { measurable, constraints ->
                    val width = (constraints.maxWidth * safeProgress).toInt()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width, maxWidth = width)
                    )
                    layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                }
                .clip(shape)
                .background(
                    if (gradient) {
                        Brush.horizontalGradient(listOf(fillColor, NimazColors.Gold500))
                    } else {
                        // A one-colour gradient rather than a branch on the modifier chain, so
                        // both paths produce the same node tree and neither can drift.
                        Brush.horizontalGradient(listOf(fillColor, fillColor))
                    }
                )
        )
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun ShowcaseLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NimazProgressTrackShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShowcaseLabel("Sizes at 60%")
        NimazProgressSize.entries.forEach { size ->
            NimazProgressTrack(progress = 0.6f, size = size)
        }

        ShowcaseLabel("Tones at 45%")
        listOf(
            NimazTone.ACCENT,
            NimazTone.SUCCESS,
            NimazTone.WARNING,
            NimazTone.ERROR,
            NimazTone.NEUTRAL,
            NimazTone.MUTED,
        ).forEach { tone ->
            NimazProgressTrack(progress = 0.45f, tone = tone)
        }

        ShowcaseLabel("Edges — 0%, 3%, 100%")
        NimazProgressTrack(progress = 0f)
        NimazProgressTrack(progress = 0.03f)
        NimazProgressTrack(progress = 1f)

        ShowcaseLabel("Gradient — the Ramadan strip")
        NimazProgressTrack(progress = 0.4f, gradient = true, size = NimazProgressSize.THIN)
        NimazProgressTrack(progress = 0.85f, gradient = true, size = NimazProgressSize.THICK)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazProgressTrack — Light")
@Composable
private fun NimazProgressTrackLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazProgressTrackShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazProgressTrack — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazProgressTrackDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazProgressTrackShowcase() }
}
