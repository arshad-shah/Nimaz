package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Height of the band itself, before the labels beneath it. */
private val BandHeight = 36.dp

/** Width of the "now" marker. Thin enough to read as a position, not a segment. */
private val MarkerWidth = 2.dp

/**
 * A labelled span with an optional "now" marker inside it.
 *
 * Deliberately **not** [NimazProgressTrack] with extra parameters. A progress bar has one
 * meaningful end; a span has two *named, differently-tinted* ends and a marker that sits inside
 * the fill rather than terminating it. Built for the suhoor→iftar window, where both times are
 * facts the reader wants and the marker is where they currently stand between them.
 *
 * @param progress position of the marker in `0f..1f`. `null` lights the whole band and draws no
 *   marker — the correct rendering for any day that is not today, and most days you look at are
 *   not today. Out-of-range values and `NaN` are coerced rather than thrown.
 * @param contentDescription one spoken sentence for the whole band. Four separate unlabelled text
 *   nodes read as noise, so supplying this **clears** the band's children from the accessibility
 *   tree rather than adding a fifth announcement on top of them.
 */
@Composable
fun NimazWindowTrack(
    startLabel: String,
    startValue: String,
    endLabel: String,
    endValue: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    startTone: NimazTone = NimazTone.ACCENT,
    endTone: NimazTone = NimazTone.WARNING,
    contentDescription: String? = null,
) {
    val safeProgress = progress?.let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) }
    val fillFraction = safeProgress ?: 1f

    val startColor = NimazToneColors.foreground(startTone)
    val endColor = NimazToneColors.foreground(endTone)
    val bandShape = RoundedCornerShape(12.dp)

    val semanticsModifier = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Column(modifier = modifier.then(semanticsModifier)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(BandHeight)
                .clip(bandShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            val bandWidth = maxWidth

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .layout { measurable, constraints ->
                        val width = (constraints.maxWidth * fillFraction).toInt()
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = width, maxWidth = width)
                        )
                        layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                    }
                    .background(
                        // Low alphas on purpose: the band is a bed the marker and the labels sit
                        // against, not a element competing with them for attention.
                        Brush.horizontalGradient(
                            listOf(
                                startColor.copy(alpha = 0.20f),
                                endColor.copy(alpha = 0.26f)
                            )
                        )
                    )
            )

            if (safeProgress != null) {
                Box(
                    modifier = Modifier
                        .offset(x = bandWidth * safeProgress - MarkerWidth / 2)
                        .width(MarkerWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WindowEnd(startLabel, startValue, startColor, Alignment.Start)
            WindowEnd(endLabel, endValue, endColor, Alignment.End)
        }
    }
}

@Composable
private fun WindowEnd(
    label: String,
    value: String,
    valueColor: Color,
    alignment: Alignment.Horizontal,
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
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
private fun NimazWindowTrackShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ShowcaseLabel("Today — most of the way to iftar")
        NimazWindowTrack("Suhoor ends", "04:31", "Iftar", "20:58", progress = 0.8f)

        ShowcaseLabel("Just after suhoor")
        NimazWindowTrack("Suhoor ends", "04:31", "Iftar", "20:58", progress = 0.02f)

        ShowcaseLabel("At iftar")
        NimazWindowTrack("Suhoor ends", "04:31", "Iftar", "20:58", progress = 1f)

        ShowcaseLabel("Another day — no marker")
        NimazWindowTrack("Suhoor ends", "04:44", "Iftar", "20:31", progress = null)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazWindowTrack — Light")
@Composable
private fun NimazWindowTrackLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazWindowTrackShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazWindowTrack — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazWindowTrackDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazWindowTrackShowcase() }
}
