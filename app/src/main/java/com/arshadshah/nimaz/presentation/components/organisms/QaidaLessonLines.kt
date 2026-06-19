package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.presentation.components.molecules.QaidaCellTile
import com.arshadshah.nimaz.presentation.components.molecules.QaidaPlayLineButton
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * The reader body: a lesson's lines, each with an optional instruction, a
 * "Play line" control, and its cells laid out right-to-left as tappable tiles.
 * The tile that matches [playingCellId] glows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QaidaLessonLines(
    content: QaidaLessonContent,
    playingCellId: Int?,
    showTransliteration: Boolean,
    onCellTap: (QaidaCell) -> Unit,
    onPlayLine: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.ExtraLarge),
    ) {
        content.lines.forEach { lineContent ->
            Column(verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)) {
                val instruction = lineContent.line.instructionEnglish
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = instruction ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    QaidaPlayLineButton(onClick = { onPlayLine(lineContent.line.id) })
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
                        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
                    ) {
                        lineContent.cells.forEach { cell ->
                            QaidaCellTile(
                                cell = cell,
                                isPlaying = cell.id == playingCellId,
                                showTransliteration = showTransliteration,
                                onTap = onCellTap,
                            )
                        }
                    }
                }
            }
        }
    }
}
