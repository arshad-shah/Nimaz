package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.tooling.preview.Preview

/**
 * Search result type enumeration.
 */
enum class SearchResultType(val displayName: String, val icon: ImageVector, val color: Color) {
    QURAN_AYAH("Quran", Icons.Default.MenuBook, NimazColors.QuranColors.BookmarkPrimary),
    HADITH("Hadith", Icons.Default.MenuBook, NimazColors.QuranColors.BookmarkSecondary),
    DUA("Dua", Icons.Default.Star, Color(0xFF14B8A6)),
    SURAH("Surah", Icons.Default.MenuBook, Color(0xFF14B8A6))
}

/**
 * Search result item with highlighted matching text.
 */
@Composable
fun SearchResultItem(
    type: SearchResultType,
    title: String,
    subtitle: String,
    matchedText: String,
    query: String,
    modifier: Modifier = Modifier,
    arabicPreview: String? = null,
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
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            ContainedIcon(
                imageVector = type.icon,
                size = NimazIconSize.SMALL,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = type.color.copy(alpha = 0.15f),
                iconColor = type.color
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    NimazBadge(
                        text = type.displayName,
                        backgroundColor = type.color.copy(alpha = 0.15f),
                        textColor = type.color,
                        size = NimazBadgeSize.SMALL
                    )
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Highlighted matched text
                HighlightedText(
                    text = matchedText,
                    query = query,
                    maxLines = 3
                )

                // Arabic preview if available
                if (arabicPreview != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ArabicText(
                        text = arabicPreview,
                        size = ArabicTextSize.SMALL,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Text with highlighted search query matches.
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    highlightColor: Color = MaterialTheme.colorScheme.primary
) {
    val normalStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val highlightStyle = SpanStyle(
        color = highlightColor,
        fontWeight = FontWeight.Bold,
        background = highlightColor.copy(alpha = 0.1f)
    )

    val annotatedString = buildAnnotatedString {
        if (query.isBlank()) {
            withStyle(normalStyle) {
                append(text)
            }
        } else {
            var currentIndex = 0
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()

            while (currentIndex < text.length) {
                val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
                if (matchIndex == -1) {
                    withStyle(normalStyle) {
                        append(text.substring(currentIndex))
                    }
                    break
                }

                // Text before match
                if (matchIndex > currentIndex) {
                    withStyle(normalStyle) {
                        append(text.substring(currentIndex, matchIndex))
                    }
                }

                // Matched text
                withStyle(highlightStyle) {
                    append(text.substring(matchIndex, matchIndex + query.length))
                }

                currentIndex = matchIndex + query.length
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Quran search result item.
 */
@Composable
fun QuranSearchResult(
    surahName: String,
    surahNumber: Int,
    ayahNumber: Int,
    ayahText: String,
    query: String,
    modifier: Modifier = Modifier,
    arabicText: String? = null,
    onClick: () -> Unit
) {
    SearchResultItem(
        type = SearchResultType.QURAN_AYAH,
        title = surahName,
        subtitle = "Surah $surahNumber : Ayah $ayahNumber",
        matchedText = ayahText,
        query = query,
        arabicPreview = arabicText,
        modifier = modifier,
        onClick = onClick
    )
}

/**
 * Hadith search result item.
 */
@Composable
fun HadithSearchResult(
    collectionName: String,
    hadithNumber: Int,
    hadithText: String,
    query: String,
    modifier: Modifier = Modifier,
    bookName: String? = null,
    onClick: () -> Unit
) {
    SearchResultItem(
        type = SearchResultType.HADITH,
        title = collectionName,
        subtitle = buildString {
            if (bookName != null) append("$bookName, ")
            append("Hadith #$hadithNumber")
        },
        matchedText = hadithText,
        query = query,
        modifier = modifier,
        onClick = onClick
    )
}

/**
 * Dua search result item.
 */
@Composable
fun DuaSearchResult(
    duaTitle: String,
    categoryName: String,
    duaText: String,
    query: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SearchResultItem(
        type = SearchResultType.DUA,
        title = duaTitle,
        subtitle = categoryName,
        matchedText = duaText,
        query = query,
        modifier = modifier,
        onClick = onClick
    )
}

/**
 * No search results found placeholder.
 */
@Composable
fun NoSearchResults(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "No matches found for \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Search Result Item")
@Composable
private fun SearchResultItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SearchResultItem(
                type = SearchResultType.QURAN_AYAH,
                title = "Al-Baqarah",
                subtitle = "Surah 2 : Ayah 255",
                matchedText = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of existence.",
                query = "Allah",
                arabicPreview = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Quran Search Result")
@Composable
private fun QuranSearchResultPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            QuranSearchResult(
                surahName = "Al-Fatiha",
                surahNumber = 1,
                ayahNumber = 1,
                ayahText = "In the name of Allah, the Most Gracious, the Most Merciful.",
                query = "name",
                arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Hadith Search Result")
@Composable
private fun HadithSearchResultPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HadithSearchResult(
                collectionName = "Sahih Bukhari",
                hadithNumber = 1,
                hadithText = "Actions are judged by intentions, so each man will have what he intended.",
                query = "intentions",
                bookName = "Book of Revelation",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Highlighted Text")
@Composable
private fun HighlightedTextPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HighlightedText(
                text = "The best among you are those who learn the Quran and teach it.",
                query = "Quran"
            )
            HighlightedText(
                text = "Actions are judged by intentions",
                query = "actions"
            )
        }
    }
}

@Preview(showBackground = true, name = "No Search Results")
@Composable
private fun NoSearchResultsPreview() {
    MaterialTheme {
        NoSearchResults(query = "xyz123")
    }
}
