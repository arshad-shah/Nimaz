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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.AyahDisplay
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.QuranVerseText
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import com.arshadshah.nimaz.presentation.theme.NimazColors
import androidx.compose.ui.tooling.preview.Preview

/**
 * Full Quran verse card with Arabic text, translation, and actions.
 */
@Composable
fun AyahCard(
    arabicText: String,
    translation: String,
    surahNumber: Int,
    ayahNumber: Int,
    modifier: Modifier = Modifier,
    surahName: String? = null,
    transliteration: String? = null,
    showTransliteration: Boolean = false,
    isBookmarked: Boolean = false,
    isSajdaAyah: Boolean = false,
    arabicFontSize: ArabicTextSize = ArabicTextSize.QURAN,
    onBookmarkClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isSajdaAyah) {
                NimazColors.QuranColors.SajdaAyah.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with ayah number and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah number badge
                AyahNumberBadge(
                    ayahNumber = ayahNumber,
                    isSajdaAyah = isSajdaAyah
                )

                // Reference text
                if (surahName != null) {
                    Text(
                        text = "$surahName : $ayahNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Actions
                Row {
                    if (onPlayClick != null) {
                        IconButton(onClick = onPlayClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play audio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (onBookmarkClick != null) {
                        IconButton(onClick = onBookmarkClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                tint = if (isBookmarked) NimazColors.QuranColors.BookmarkPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (onCopyClick != null) {
                        IconButton(onClick = onCopyClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (onShareClick != null) {
                        IconButton(onClick = onShareClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Arabic text
            QuranVerseText(
                arabicText = arabicText,
                verseNumber = ayahNumber,
                size = arabicFontSize,
                showVerseNumber = true
            )

            // Transliteration (optional)
            if (showTransliteration && transliteration != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = transliteration,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center
                )
            }

            // Translation
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = translation,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )

            // Sajda indicator
            if (isSajdaAyah) {
                Spacer(modifier = Modifier.height(8.dp))
                SajdaIndicator()
            }
        }
    }
}

/**
 * Ayah number badge.
 */
@Composable
fun AyahNumberBadge(
    ayahNumber: Int,
    modifier: Modifier = Modifier,
    isSajdaAyah: Boolean = false
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (isSajdaAyah) {
                    NimazColors.QuranColors.SajdaAyah.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ayahNumber.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSajdaAyah) {
                NimazColors.QuranColors.SajdaAyah
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

/**
 * Sajda (prostration) indicator badge.
 */
@Composable
private fun SajdaIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        NimazBadge(
            text = "Sajda Ayah",
            backgroundColor = NimazColors.QuranColors.SajdaAyah,
            textColor = Color.White
        )
    }
}


@Preview(showBackground = true, name = "Ayah Card")
@Composable
private fun AyahCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AyahCard(
                arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                translation = "In the name of Allah, the Most Gracious, the Most Merciful",
                surahNumber = 1,
                ayahNumber = 1,
                surahName = "Al-Fatiha",
                onBookmarkClick = {},
                onShareClick = {},
                onCopyClick = {},
                onPlayClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Ayah Card with Sajda")
@Composable
private fun AyahCardSajdaPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AyahCard(
                arabicText = "إِنَّ الَّذِينَ عِندَ رَبِّكَ لَا يَسْتَكْبِرُونَ عَنْ عِبَادَتِهِ وَيُسَبِّحُونَهُ وَلَهُ يَسْجُدُونَ ۩",
                translation = "Those who are near your Lord are not too proud to worship Him. They glorify Him and prostrate before Him.",
                surahNumber = 7,
                ayahNumber = 206,
                surahName = "Al-A'raf",
                isSajdaAyah = true,
                isBookmarked = true,
                onBookmarkClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Ayah Number Badge")
@Composable
private fun AyahNumberBadgePreview() {
    MaterialTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AyahNumberBadge(ayahNumber = 1)
            AyahNumberBadge(ayahNumber = 42)
            AyahNumberBadge(ayahNumber = 286, isSajdaAyah = true)
        }
    }
}

@Preview(showBackground = true, name = "Bismillah Card")
@Composable
private fun BismillahCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            BismillahCard(showTranslation = true)
        }
    }
}

/**
 * Compact ayah display for reading mode.
 */
@Composable
fun CompactAyahDisplay(
    arabicText: String,
    ayahNumber: Int,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(
                if (isHighlighted) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$arabicText ${toArabicNumber(ayahNumber)}",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Bismillah header card.
 */
@Composable
fun BismillahCard(
    modifier: Modifier = Modifier,
    showTranslation: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArabicText(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                size = ArabicTextSize.LARGE,
                color = MaterialTheme.colorScheme.primary
            )
            if (showTranslation) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "In the name of Allah, the Most Gracious, the Most Merciful",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
