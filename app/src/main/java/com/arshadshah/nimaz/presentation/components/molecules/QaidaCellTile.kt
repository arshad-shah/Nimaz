package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.HarakatArabicText
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * A single tappable Qaida cell (one sound) in the lesson reader. Shows the
 * Arabic glyph with colour-coded harakat and, optionally, its transliteration.
 * When [isPlaying] the tile fills teal (theme `primary`) and lifts, so the child
 * sees which sound is sounding. Big touch target (≥72dp).
 */
@Composable
fun QaidaCellTile(
    cell: QaidaCell,
    isPlaying: Boolean,
    showTransliteration: Boolean,
    onTap: (QaidaCell) -> Unit,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
) {
    val container by animateColorAsState(
        if (isPlaying) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "cellContainer",
    )
    val elevation by animateDpAsState(if (isPlaying) 6.dp else 1.dp, label = "cellElevation")
    // A heard-but-not-playing tile carries a teal hairline so a returning
    // learner can see at a glance which sounds they've already practised.
    val border = when {
        isPlaying -> null
        isCompleted -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val translitColor =
        if (isPlaying) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    val doneSuffix = if (isCompleted) ", done" else ""

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .defaultMinSize(minWidth = 72.dp, minHeight = 84.dp)
                .semantics { contentDescription = "${cell.transliteration}, tap to hear$doneSuffix" }
                .clickable { onTap(cell) },
            shape = RoundedCornerShape(NimazCornerRadius.Large),
            color = container,
            tonalElevation = elevation,
            shadowElevation = elevation,
            border = border,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = NimazSpacing.Medium,
                    vertical = NimazSpacing.Small,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HarakatArabicText(
                    text = cell.textArabic,
                    highlightGroup = cell.highlightGroup,
                    size = ArabicTextSize.EXTRA_LARGE,
                    playing = isPlaying,
                )
                if (showTransliteration && cell.transliteration.isNotBlank()) {
                    Text(
                        text = cell.transliteration,
                        style = MaterialTheme.typography.labelMedium,
                        color = translitColor,
                    )
                }
            }
        }

        // "Heard" check badge in the top-end corner (hidden while playing).
        if (isCompleted && !isPlaying) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .size(16.dp),
            )
        }
    }
}
