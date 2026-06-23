package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.molecules.QaidaLetterTile
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The alphabet board: every letter as a tappable tile in an RTL 4-column grid.
 * Letters in [heardLetterIds] show a gold "heard" star.
 */
@Composable
fun QaidaLetterBoard(
    letters: List<QaidaLetter>,
    heardLetterIds: Set<Int>,
    onLetterClick: (QaidaLetter) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = NimazSpacing.Large),
            horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = NimazSpacing.Medium),
        ) {
            items(letters, key = { it.id }) { letter ->
                QaidaLetterTile(
                    letter = letter,
                    heard = letter.id in heardLetterIds,
                    onClick = onLetterClick,
                )
            }
        }
    }
}


// ==================== PREVIEWS ====================

private fun sampleBoardLetter(id: Int, arabic: String, name: String) = QaidaLetter(
    id = id,
    letterArabic = arabic,
    nameArabic = "اسم",
    nameTransliteration = name,
    isolatedForm = arabic,
    initialForm = null,
    medialForm = null,
    finalForm = null,
    isConnecting = true,
    makhrajArea = MakhrajArea.LISAN,
    makhrajDetail = "",
    phoneticHint = null,
    audioKey = name.lowercase(),
    audioPath = "",
    displayOrder = id,
)

@Composable
private fun QaidaLetterBoardShowcase() {
    val letters = listOf(
        sampleBoardLetter(1, "ا", "Alif"),
        sampleBoardLetter(2, "ب", "Ba"),
        sampleBoardLetter(3, "ت", "Ta"),
        sampleBoardLetter(4, "ث", "Tha"),
        sampleBoardLetter(5, "ج", "Jeem"),
        sampleBoardLetter(6, "ح", "Ha"),
        sampleBoardLetter(7, "خ", "Kha"),
        sampleBoardLetter(8, "د", "Dal"),
    )
    QaidaLetterBoard(
        letters = letters,
        heardLetterIds = setOf(1, 2, 5),
        onLetterClick = {},
        modifier = Modifier.height(400.dp),
    )
}

@Preview(showBackground = true, name = "Qaida Letter Board — Light", heightDp = 400)
@Composable
private fun QaidaLetterBoardLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaLetterBoardShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Letter Board — Dark", heightDp = 400)
@Composable
private fun QaidaLetterBoardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaLetterBoardShowcase()
    }
}
