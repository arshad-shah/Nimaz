package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.molecules.QaidaLetterTile
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

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
