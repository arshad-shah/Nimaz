package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import androidx.compose.ui.tooling.preview.Preview

/**
 * Dua list item for category displays.
 */
@Composable
fun DuaListItem(
    duaTitle: String,
    arabicText: String,
    modifier: Modifier = Modifier,
    translation: String? = null,
    source: String? = null,
    isFavorite: Boolean = false,
    onDuaClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onDuaClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = duaTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (onFavoriteClick != null) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text preview
            ArabicText(
                text = arabicText,
                size = ArabicTextSize.SMALL,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            // Translation preview
            if (translation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Source
            if (source != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Dua category item for category selection.
 */
@Composable
fun DuaCategoryItem(
    categoryName: String,
    categoryNameArabic: String,
    duaCount: Int,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContainedIcon(
                imageVector = Icons.Default.Folder,
                size = NimazIconSize.MEDIUM,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = iconTint.copy(alpha = 0.15f),
                iconColor = iconTint
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categoryNameArabic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "  |  $duaCount duas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Compact dua list item.
 */
@Composable
fun CompactDuaListItem(
    duaTitle: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isFavorite: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = duaTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Featured dua card for home screen.
 */
@Composable
fun FeaturedDuaCard(
    duaTitle: String,
    arabicText: String,
    modifier: Modifier = Modifier,
    translation: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Dua of the Day",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = duaTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            ArabicText(
                text = arabicText,
                size = ArabicTextSize.SMALL,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2
            )

            if (translation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "Dua List Item")
@Composable
private fun DuaListItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DuaListItem(
                duaTitle = "Dua for entering the mosque",
                arabicText = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
                translation = "O Allah, open for me the gates of Your mercy.",
                source = "Sahih Muslim",
                onDuaClick = {},
                onFavoriteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dua List Item Favorite")
@Composable
private fun DuaListItemFavoritePreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DuaListItem(
                duaTitle = "Dua before sleeping",
                arabicText = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
                translation = "In Your name, O Allah, I die and I live.",
                source = "Sahih Bukhari",
                isFavorite = true,
                onDuaClick = {},
                onFavoriteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dua Category Item")
@Composable
private fun DuaCategoryItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DuaCategoryItem(
                categoryName = "Morning & Evening",
                categoryNameArabic = "أذكار الصباح والمساء",
                duaCount = 24,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Compact Dua List Item")
@Composable
private fun CompactDuaListItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CompactDuaListItem(
                duaTitle = "Dua for guidance",
                subtitle = "Quran 1:6",
                isFavorite = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Featured Dua Card")
@Composable
private fun FeaturedDuaCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FeaturedDuaCard(
                duaTitle = "Dua for forgiveness",
                arabicText = "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ",
                translation = "My Lord, forgive me and accept my repentance. You are the Accepter of repentance, the Merciful.",
                onClick = {}
            )
        }
    }
}
