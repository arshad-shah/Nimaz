package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * Favourite-toggle FAB shared by the three "names" detail screens
 * (Asma ul Husna, Asma un Nabi, Prophets). Previously copy-pasted verbatim in
 * each screen — only the accent and the toggle callback differed.
 */
@Composable
fun FavoriteFab(
    isFavorite: Boolean,
    accent: NamesAccent,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = accent.chipContainer,
        contentColor = accent.onChipContainer
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFavorite) {
                stringResource(R.string.remove_from_favorites)
            } else {
                stringResource(R.string.add_to_favorites)
            },
            tint = if (isFavorite) MaterialTheme.colorScheme.error else accent.onChipContainer
        )
    }
}

/**
 * Titled body-text card used by the names/prophet detail screens. Renders
 * nothing when [content] is blank. Previously duplicated in each detail screen.
 */
@Composable
fun NameDetailSectionCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (content.isNotBlank()) {
        NimazCard(
            modifier = modifier.fillMaxWidth(),
            style = NimazCardStyle.FILLED,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(NimazSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
