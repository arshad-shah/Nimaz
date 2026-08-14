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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.NimazToneColors
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Height of the row the nodes sit in. Tall enough to centre the largest dot with room to spare. */
private val TrackHeight = 30.dp

/** Thickness of the hairline the nodes sit on. */
private val LineHeight = 2.dp

/** Width of the "now" marker. Thin enough to read as a position, not a segment. */
private val MarkerWidth = 2.dp

/** Diameter of a node. Larger than [NimazStatusDotSize.LARGE] because it must read on a line. */
private val NodeDiameter = 14.dp

/**
 * One marker on a [NimazTimelineTrack].
 *
 * @param position where along the track it sits, `0f..1f`. Coerced by [safePosition] rather than
 *   validated: the caller computes it from two clock times, and a day whose Isha equals its Fajr
 *   (polar latitudes, a bad location fix) would otherwise divide by zero and place a node at NaN.
 * @param label the node's own name, used only for accessibility when the track is not described
 *   as a whole.
 */
data class NimazTimelineNode(
    val position: Float,
    val spec: NimazStatusDotSpec,
    val label: String,
) {
    internal val safePosition: Float
        get() = if (position.isNaN()) 0f else position.coerceIn(0f, 1f)
}

/**
 * A hairline carrying N status nodes at proportional positions, with an optional "now" marker.
 *
 * Deliberately **not** [NimazWindowTrack] and **not** [NimazProgressTrack]. A window has two
 * named, differently-tinted ends and nothing between them; a progress bar has one meaningful end
 * and no interior structure. This has five interior nodes that each carry their own status colour,
 * and its fill means *elapsed*, not *complete* — a day can be entirely elapsed and entirely
 * unrecorded. Folding any two of those three into one component would give it parameters that
 * only apply in one of its modes, which is two atoms wearing one name.
 *
 * @param progress position of the "now" marker in `0f..1f`. `null` draws no marker and lights the
 *   whole line — the correct rendering for any day that is not today, and most days you look at
 *   are not today. Out-of-range values and `NaN` are coerced rather than thrown.
 * @param contentDescription one spoken sentence for the whole track. Five unlabelled dots read as
 *   noise, so supplying this **clears** the children from the accessibility tree rather than
 *   adding a sixth announcement on top of them.
 */
@Composable
fun NimazTimelineTrack(
    nodes: List<NimazTimelineNode>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    contentDescription: String? = null,
) {
    val safeProgress = progress?.let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Column(modifier = modifier.then(semanticsModifier)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                // Inset by half a node so a node at 0f or 1f is not clipped by the track edge.
                .padding(horizontal = NodeDiameter / 2)
                .height(TrackHeight)
        ) {
            val trackWidth = maxWidth

            // The unlit line, full width.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(LineHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )

            // The elapsed portion. A null progress lights all of it.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(LineHeight)
                    .layout { measurable, constraints ->
                        val width = (constraints.maxWidth * (safeProgress ?: 1f)).toInt()
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = width, maxWidth = width)
                        )
                        layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                    }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary)
            )

            nodes.forEach { node ->
                NimazStatusDot(
                    color = NimazToneColors.foreground(node.spec.tone),
                    style = node.spec.style,
                    diameter = NodeDiameter,
                    contentDescription = if (contentDescription == null) node.label else null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = trackWidth * node.safePosition - NodeDiameter / 2)
                )
            }

            if (safeProgress != null) {
                Box(
                    modifier = Modifier
                        .offset(x = trackWidth * safeProgress - MarkerWidth / 2)
                        .width(MarkerWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EdgeLabel(startLabel)
            EdgeLabel(endLabel)
        }
    }
}

@Composable
private fun EdgeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
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

/** The five prayers of 13 August at their real proportional positions. */
private val previewNodes = listOf(
    NimazTimelineNode(0f, NimazStatusDotSpec(NimazTone.SUCCESS), "Fajr"),
    NimazTimelineNode(0.503f, NimazStatusDotSpec(NimazTone.ACCENT), "Dhuhr"),
    NimazTimelineNode(
        0.725f,
        NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED),
        "Asr"
    ),
    NimazTimelineNode(0.913f, NimazStatusDotSpec(NimazTone.MUTED), "Maghrib"),
    NimazTimelineNode(1f, NimazStatusDotSpec(NimazTone.MUTED), "Isha"),
)

@Composable
private fun NimazTimelineTrackShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        ShowcaseLabel("Today — 18:10, Asr passed unrecorded")
        NimazTimelineTrack(previewNodes, "Fajr 04:31", "Isha 22:35", progress = 0.755f)

        ShowcaseLabel("A finished day — every prayer recorded")
        NimazTimelineTrack(
            nodes = previewNodes.map { it.copy(spec = NimazStatusDotSpec(NimazTone.SUCCESS)) },
            startLabel = "Fajr 04:29",
            endLabel = "Isha 22:38",
            progress = null,
        )

        ShowcaseLabel("A day nobody logged — all rings")
        NimazTimelineTrack(
            nodes = previewNodes.map {
                it.copy(spec = NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED))
            },
            startLabel = "Fajr 04:27",
            endLabel = "Isha 22:41",
            progress = null,
        )

        ShowcaseLabel("Just after Fajr — the marker sits on the first node")
        NimazTimelineTrack(previewNodes, "Fajr 04:31", "Isha 22:35", progress = 0.01f)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazTimelineTrack — Light")
@Composable
private fun NimazTimelineTrackLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazTimelineTrackShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazTimelineTrack — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazTimelineTrackDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazTimelineTrackShowcase() }
}
