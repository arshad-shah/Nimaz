package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.RevelationTypeChip
import com.arshadshah.nimaz.presentation.components.atoms.SurahNumberBadge
import com.arshadshah.nimaz.presentation.theme.NimazColors


// ==================== PREVIEWS ====================

import androidx.compose.ui.tooling.preview.Preview


/**
 * Surah list item displaying surah information.
 */
@Composable
fun SurahListItem(
    surahNumber: Int,
    arabicName: String,
    englishName: String,
    englishMeaning: String,
    versesCount: Int,
    isMeccan: Boolean,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    readingProgress: Float? = null,
    onSurahClick: () -> Unit,
    onBookmarkClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSurahClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah number badge
            SurahNumberBadge(
                number = surahNumber,
                size = 44.dp,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Surah details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = englishName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RevelationTypeChip(isMeccan = isMeccan)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$englishMeaning  |  $versesCount verses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Reading progress indicator
                if (readingProgress != null && readingProgress > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(readingProgress)
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Arabic name
            ArabicText(
                text = arabicName,
                size = ArabicTextSize.MEDIUM,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Action buttons
            if (onBookmarkClick != null || onPlayClick != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (onBookmarkClick != null) {
                        IconButton(
                            onClick = onBookmarkClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) {
                                    Icons.Default.Bookmark
                                } else {
                                    Icons.Default.BookmarkBorder
                                },
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
                    if (onPlayClick != null) {
                        IconButton(
                            onClick = onPlayClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play audio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact surah list item for smaller displays.
 */
@Composable
fun CompactSurahListItem(
    surahNumber: Int,
    arabicName: String,
    englishName: String,
    versesCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surahNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and verses
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = englishName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$versesCount verses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Arabic name
        ArabicText(
            text = arabicName,
            size = ArabicTextSize.SMALL,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Featured surah card for home screen.
 */
@Composable
fun FeaturedSurahCard(
    surahNumber: Int,
    arabicName: String,
    englishName: String,
    englishMeaning: String,
    modifier: Modifier = Modifier,
    lastReadAyah: Int? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Continue Reading",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = englishName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = englishMeaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                ArabicText(
                    text = arabicName,
                    size = ArabicTextSize.LARGE,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (lastReadAyah != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Last read: Ayah $lastReadAyah",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
@Preview(showBackground = true, name = "Surah List Item")
@Composable
private fun SurahListItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SurahListItem(
                surahNumber = 1,
                arabicName = "الفاتحة",
                englishName = "Al-Fatiha",
                englishMeaning = "The Opening",
                versesCount = 7,
                isMeccan = true,
                onSurahClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Surah List Item with Actions")
@Composable
private fun SurahListItemWithActionsPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SurahListItem(
                surahNumber = 36,
                arabicName = "يس",
                englishName = "Ya-Sin",
                englishMeaning = "Ya Sin",
                versesCount = 83,
                isMeccan = true,
                isBookmarked = true,
                readingProgress = 0.45f,
                onSurahClick = {},
                onBookmarkClick = {},
                onPlayClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Compact Surah List Item")
@Composable
private fun CompactSurahListItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CompactSurahListItem(
                surahNumber = 112,
                arabicName = "الإخلاص",
                englishName = "Al-Ikhlas",
                versesCount = 4,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Featured Surah Card")
@Composable
private fun FeaturedSurahCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FeaturedSurahCard(
                surahNumber = 2,
                arabicName = "البقرة",
                englishName = "Al-Baqarah",
                englishMeaning = "The Cow",
                lastReadAyah = 142,
                onClick = {}
            )
        }
    }
}
