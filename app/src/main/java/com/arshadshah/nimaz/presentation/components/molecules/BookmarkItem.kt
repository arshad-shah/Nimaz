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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import androidx.compose.ui.tooling.preview.Preview

/**
 * Bookmark type enumeration.
 */
enum class BookmarkType(val displayName: String, val icon: ImageVector, val color: Color) {
    QURAN("Quran", Icons.Default.MenuBook, NimazColors.QuranColors.BookmarkPrimary),
    HADITH("Hadith", Icons.Default.MenuBook, NimazColors.QuranColors.BookmarkSecondary),
    DUA("Dua", Icons.Default.Bookmark, Color(0xFF14B8A6))
}

/**
 * Unified bookmark item for all content types.
 */
@Composable
fun BookmarkItem(
    type: BookmarkType,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    arabicText: String? = null,
    timestamp: String? = null,
    note: String? = null,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

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
            verticalAlignment = Alignment.Top
        ) {
            // Type indicator
            ContainedIcon(
                imageVector = type.icon,
                size = NimazIconSize.MEDIUM,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = type.color.copy(alpha = 0.15f),
                iconColor = type.color
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NimazBadge(
                        text = type.displayName,
                        backgroundColor = type.color.copy(alpha = 0.2f),
                        textColor = type.color,
                        size = NimazBadgeSize.SMALL
                    )
                    if (timestamp != null) {
                        Text(
                            text = timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (arabicText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ArabicText(
                        text = arabicText,
                        size = ArabicTextSize.SMALL,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }

                if (note != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Note: $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Menu
            if (onDeleteClick != null) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Quran bookmark item.
 */
@Composable
fun QuranBookmarkItem(
    surahName: String,
    surahNumber: Int,
    ayahNumber: Int,
    arabicText: String,
    modifier: Modifier = Modifier,
    timestamp: String? = null,
    note: String? = null,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    BookmarkItem(
        type = BookmarkType.QURAN,
        title = surahName,
        subtitle = "Surah $surahNumber, Ayah $ayahNumber",
        arabicText = arabicText,
        timestamp = timestamp,
        note = note,
        modifier = modifier,
        onClick = onClick,
        onDeleteClick = onDeleteClick
    )
}

/**
 * Hadith bookmark item.
 */
@Composable
fun HadithBookmarkItem(
    collectionName: String,
    hadithNumber: Int,
    arabicText: String,
    modifier: Modifier = Modifier,
    bookName: String? = null,
    timestamp: String? = null,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    BookmarkItem(
        type = BookmarkType.HADITH,
        title = collectionName,
        subtitle = buildString {
            if (bookName != null) append("$bookName, ")
            append("Hadith #$hadithNumber")
        },
        arabicText = arabicText,
        timestamp = timestamp,
        modifier = modifier,
        onClick = onClick,
        onDeleteClick = onDeleteClick
    )
}

/**
 * Dua bookmark/favorite item.
 */
@Composable
fun DuaBookmarkItem(
    duaTitle: String,
    categoryName: String,
    arabicText: String,
    modifier: Modifier = Modifier,
    timestamp: String? = null,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    BookmarkItem(
        type = BookmarkType.DUA,
        title = duaTitle,
        subtitle = categoryName,
        arabicText = arabicText,
        timestamp = timestamp,
        modifier = modifier,
        onClick = onClick,
        onDeleteClick = onDeleteClick
    )
}

/**
 * Compact bookmark list item.
 */
@Composable
fun CompactBookmarkItem(
    type: BookmarkType,
    title: String,
    subtitle: String,
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
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = type.color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        NimazBadge(
            text = type.displayName,
            backgroundColor = type.color.copy(alpha = 0.15f),
            textColor = type.color,
            size = NimazBadgeSize.SMALL
        )
    }
}
@Preview(showBackground = true, name = "Quran Bookmark Item")
@Composable
private fun QuranBookmarkItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            QuranBookmarkItem(
                surahName = "Al-Baqarah",
                surahNumber = 2,
                ayahNumber = 255,
                arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
                timestamp = "2 hours ago",
                note = "Ayatul Kursi - memorize this",
                onClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Hadith Bookmark Item")
@Composable
private fun HadithBookmarkItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HadithBookmarkItem(
                collectionName = "Sahih Bukhari",
                hadithNumber = 1,
                arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ",
                bookName = "Book of Revelation",
                timestamp = "Yesterday",
                onClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dua Bookmark Item")
@Composable
private fun DuaBookmarkItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DuaBookmarkItem(
                duaTitle = "Dua before sleeping",
                categoryName = "Daily Duas",
                arabicText = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
                timestamp = "Last week",
                onClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Compact Bookmark Items")
@Composable
private fun CompactBookmarkItemsPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CompactBookmarkItem(
                type = BookmarkType.QURAN,
                title = "Al-Fatiha, Ayah 1",
                subtitle = "The Opening",
                onClick = {}
            )
            CompactBookmarkItem(
                type = BookmarkType.HADITH,
                title = "Sahih Bukhari #1",
                subtitle = "Actions are by intentions",
                onClick = {}
            )
            CompactBookmarkItem(
                type = BookmarkType.DUA,
                title = "Morning Dua",
                subtitle = "Morning & Evening",
                onClick = {}
            )
        }
    }
}
