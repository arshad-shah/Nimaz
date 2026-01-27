package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
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
import com.arshadshah.nimaz.presentation.components.atoms.HadithGradeBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * Hadith list item for collection displays.
 */
@Composable
fun HadithListItem(
    hadithNumber: Int,
    arabicText: String,
    translationText: String,
    collectionName: String,
    modifier: Modifier = Modifier,
    grade: String? = null,
    narrator: String? = null,
    isBookmarked: Boolean = false,
    onHadithClick: () -> Unit,
    onBookmarkClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onHadithClick),
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
            // Header with number, grade, and bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NimazBadge(
                        text = "#$hadithNumber",
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = NimazBadgeSize.MEDIUM
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = collectionName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (grade != null) {
                        HadithGradeBadge(grade = grade)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (onBookmarkClick != null) {
                        IconButton(
                            onClick = onBookmarkClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                tint = if (isBookmarked) {
                                    NimazColors.QuranColors.BookmarkPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text preview
            Text(
                text = arabicText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Translation preview
            Text(
                text = translationText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Narrator
            if (narrator != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Narrated by: $narrator",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Hadith collection/book item.
 */
@Composable
fun HadithCollectionItem(
    collectionName: String,
    arabicName: String,
    hadithCount: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collectionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$hadithCount hadiths",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = arabicName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

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
 * Hadith chapter item.
 */
@Composable
fun HadithChapterItem(
    chapterNumber: Int,
    chapterName: String,
    hadithCount: Int,
    modifier: Modifier = Modifier,
    arabicName: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazBadge(
            text = chapterNumber.toString(),
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            size = NimazBadgeSize.MEDIUM
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapterName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (arabicName != null) {
                Text(
                    text = arabicName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = hadithCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Hadith List Item")
@Composable
private fun HadithListItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HadithListItem(
                hadithNumber = 1,
                arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ",
                translationText = "Actions are judged by intentions, so each man will have what he intended.",
                collectionName = "Sahih Bukhari",
                grade = "Sahih",
                narrator = "Umar ibn al-Khattab",
                onHadithClick = {},
                onBookmarkClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Hadith List Item Bookmarked")
@Composable
private fun HadithListItemBookmarkedPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HadithListItem(
                hadithNumber = 2,
                arabicText = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
                translationText = "Whoever believes in Allah and the Last Day should speak good or remain silent.",
                collectionName = "Sahih Muslim",
                grade = "Sahih",
                isBookmarked = true,
                onHadithClick = {},
                onBookmarkClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Hadith Collection Item")
@Composable
private fun HadithCollectionItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HadithCollectionItem(
                collectionName = "Sahih Bukhari",
                arabicName = "صحيح البخاري",
                hadithCount = 7275,
                description = "The most authentic hadith collection",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Hadith Chapter Item")
@Composable
private fun HadithChapterItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HadithChapterItem(
                chapterNumber = 1,
                chapterName = "Book of Revelation",
                hadithCount = 7,
                arabicName = "بدء الوحي",
                onClick = {}
            )
        }
    }
}
