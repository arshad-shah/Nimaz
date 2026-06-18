package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

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
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent.chipContainer,
                selectedLabelColor = accent.onChipContainer
            )
        )
    }
}
