package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.TokenType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.HarakatArabicText
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

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
        else MaterialTheme.colorScheme.surfaceContainer,
        label = "cellContainer",
    )
    val elevation by animateDpAsState(if (isPlaying) 6.dp else 1.dp, label = "cellElevation")
    // A heard-but-not-playing tile carries a teal hairline so a returning
    // learner can see at a glance which sounds they've already practised.
    val borderColor =
        if (isCompleted) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline
    val translitColor =
        if (isPlaying) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    val cellDescription = if (isCompleted) {
        stringResource(R.string.qaida_a11y_cell_done_format, cell.transliteration)
    } else {
        stringResource(R.string.qaida_a11y_cell_format, cell.transliteration)
    }

    Box(modifier = modifier) {
        NimazCard(
            onClick = { onTap(cell) },
            modifier = Modifier
                .defaultMinSize(minWidth = 72.dp, minHeight = 84.dp)
                .semantics {
                    contentDescription = cellDescription
                },
            selected = isPlaying,
            shape = RoundedCornerShape(NimazCornerRadius.Large),
            colors = NimazCardDefaults.selectable(
                container = container,
                content = MaterialTheme.colorScheme.onSurface,
                border = borderColor,
                activeContainer = container,
                activeContent = MaterialTheme.colorScheme.onPrimary,
                activeBorder = null,
            ),
            elevation = elevation,
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
            NimazIcon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                variant = NimazIconVariant.PRIMARY,
                size = NimazIconSize.SMALL,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}


// ==================== PREVIEWS ====================

private fun sampleQaidaCell(
    id: Int = 1,
    textArabic: String = "بَ",
    transliteration: String = "ba",
) = QaidaCell(
    id = id,
    lineId = 1,
    lessonId = 1,
    position = id,
    textArabic = textArabic,
    transliteration = transliteration,
    tokenType = TokenType.SYLLABLE,
    audioKey = "ba",
    audioPath = "",
    highlightGroup = "fatha",
    letterId = 2,
    notes = null,
)

@Composable
private fun QaidaCellTileShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QaidaCellTile(
            cell = sampleQaidaCell(id = 1, textArabic = "بَ", transliteration = "ba"),
            isPlaying = false,
            showTransliteration = true,
            onTap = {},
        )
        QaidaCellTile(
            cell = sampleQaidaCell(id = 2, textArabic = "بِ", transliteration = "bi"),
            isPlaying = true,
            showTransliteration = true,
            onTap = {},
        )
        QaidaCellTile(
            cell = sampleQaidaCell(id = 3, textArabic = "بُ", transliteration = "bu"),
            isPlaying = false,
            showTransliteration = true,
            onTap = {},
            isCompleted = true,
        )
    }
}

@Preview(showBackground = true, name = "Qaida Cell Tile — Light")
@Composable
private fun QaidaCellTileLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaCellTileShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Cell Tile — Dark")
@Composable
private fun QaidaCellTileDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaCellTileShowcase()
    }
}
