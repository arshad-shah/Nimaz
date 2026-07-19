package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

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
        NimazIcon(
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
            style = NimazCardStyle.ELEVATED,
            tone = NimazTone.NEUTRAL
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


// ==================== PREVIEWS ====================

@Composable
private fun NameDetailComponentsShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FavoriteFab(
            isFavorite = true,
            accent = NamesAccents.allah(),
            onClick = {},
        )
        FavoriteFab(
            isFavorite = false,
            accent = NamesAccents.prophets(),
            onClick = {},
        )
        NameDetailSectionCard(
            title = "Meaning",
            content = "Ar-Rahman is one of the most beautiful names of Allah, " +
                "denoting the boundless mercy and compassion that encompasses " +
                "all of creation, believers and non-believers alike.",
            titleColor = NamesAccents.allah().contentTint,
        )
        NameDetailSectionCard(
            title = "Benefits",
            content = "Reciting this name frequently fills the heart with " +
                "gratitude and softens it toward all of creation.",
        )
    }
}

@Preview(showBackground = true, name = "NameDetailComponents — Light")
@Composable
private fun NameDetailComponentsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NameDetailComponentsShowcase()
    }
}

@Preview(showBackground = true, name = "NameDetailComponents — Dark")
@Composable
private fun NameDetailComponentsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NameDetailComponentsShowcase()
    }
}
