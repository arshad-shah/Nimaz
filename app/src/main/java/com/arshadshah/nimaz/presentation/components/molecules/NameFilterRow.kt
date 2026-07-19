package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * All / Favorites filter chips with the screen accent applied to the selected state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameFilterRow(
    showFavoritesOnly: Boolean,
    onShowAll: () -> Unit,
    onShowFavorites: () -> Unit,
    accent: NamesAccent,
    allLabel: String,
    favoritesLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
    ) {
        FilterChip(
            selected = !showFavoritesOnly,
            onClick = { if (showFavoritesOnly) onShowAll() },
            label = { Text(allLabel) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent.chipContainer,
                selectedLabelColor = accent.onChipContainer
            )
        )
        FilterChip(
            selected = showFavoritesOnly,
            onClick = { if (!showFavoritesOnly) onShowFavorites() },
            label = { Text(favoritesLabel) },
            leadingIcon = {
                NimazIcon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    iconSize = FilterChipDefaults.IconSize
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent.chipContainer,
                selectedLabelColor = accent.onChipContainer
            )
        )
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun NameFilterRowShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "All" selected
        NameFilterRow(
            showFavoritesOnly = false,
            onShowAll = {},
            onShowFavorites = {},
            accent = NamesAccents.allah(),
            allLabel = "All Names",
            favoritesLabel = "Favorites",
        )
        // "Favorites" selected, different accent
        NameFilterRow(
            showFavoritesOnly = true,
            onShowAll = {},
            onShowFavorites = {},
            accent = NamesAccents.prophets(),
            allLabel = "All Prophets",
            favoritesLabel = "Favorites",
        )
    }
}

@Preview(showBackground = true, name = "NameFilterRow — Light")
@Composable
private fun NameFilterRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NameFilterRowShowcase()
    }
}

@Preview(showBackground = true, name = "NameFilterRow — Dark")
@Composable
private fun NameFilterRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NameFilterRowShowcase()
    }
}
