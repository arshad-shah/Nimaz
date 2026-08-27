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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.arshadshah.nimaz.domain.model.LineType
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLine
import com.arshadshah.nimaz.domain.model.QaidaLineContent
import com.arshadshah.nimaz.domain.model.TokenType
import com.arshadshah.nimaz.presentation.components.molecules.QAIDA_AUDIO_UI_ENABLED
import com.arshadshah.nimaz.presentation.components.molecules.QaidaCellTile
import com.arshadshah.nimaz.presentation.components.molecules.QaidaPlayLineButton
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme

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
    completedCellIds: Set<Int> = emptySet(),
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
                // Show the header row only when there's an instruction to read or
                // the play control is enabled — text-only mode drops empty rows.
                if (!instruction.isNullOrBlank() || QAIDA_AUDIO_UI_ENABLED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Weighted so a long instruction wraps/ellipsizes instead of
                        // squeezing the Play-line button at the row's end.
                        Text(
                            text = instruction ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        // Hidden while audio is being regenerated (text-only mode).
                        if (QAIDA_AUDIO_UI_ENABLED) {
                            QaidaPlayLineButton(onClick = { onPlayLine(lineContent.line.id) })
                        }
                    }
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
                                isCompleted = cell.id in completedCellIds,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 360,
    name = "Lesson Lines — long instruction"
)
@Composable
private fun QaidaLessonLinesPreview() {
    fun previewCell(id: Int, arabic: String, translit: String) = QaidaCell(
        id = id, lineId = 1, lessonId = 1, position = id, textArabic = arabic,
        transliteration = translit, tokenType = TokenType.SYLLABLE,
        audioKey = "k$id", audioPath = "", highlightGroup = null, letterId = null, notes = null,
    )

    val content = QaidaLessonContent(
        lesson = QaidaLesson(
            id = 1, lessonNumber = 16, titleEnglish = "Integrated Words", titleArabic = "",
            titleTransliteration = "", description = "", conceptTags = emptyList(),
            icon = "", displayOrder = 16,
        ),
        lines = listOf(
            QaidaLineContent(
                line = QaidaLine(
                    id = 1, lessonId = 1, lineNumber = 1, lineType = LineType.EXERCISE,
                    // The longest real instruction in the course — used to verify the
                    // Play-line button keeps its size instead of being squished.
                    instructionEnglish = "Integrated words: madd, shadda and sukoon together (mind the timing).",
                    instructionArabic = null, displayOrder = 1,
                ),
                cells = listOf(
                    previewCell(1, "مَدّ", "madd"),
                    previewCell(2, "شَدّ", "shadd"),
                    previewCell(3, "سُكُون", "sukoon"),
                ),
            ),
        ),
    )
    NimazTheme {
        QaidaLessonLines(
            content = content,
            playingCellId = null,
            showTransliteration = true,
            onCellTap = {},
            onPlayLine = {},
            // First cell shown as already heard (check badge + teal hairline).
            completedCellIds = setOf(1),
        )
    }
}
