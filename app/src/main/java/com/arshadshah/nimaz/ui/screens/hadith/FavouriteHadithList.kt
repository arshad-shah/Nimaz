package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont

@Composable
fun FavouriteHadithList(
    loading: Boolean,
    allFavourites: List<HadithFavourite>,
    onNavigateToChapterFromFavourite: (bookId: Int, chapterId: Int) -> Unit,
    onFavouriteClick: (bookId: Int, chapterId: Int, hadithId: Int, favourite: Boolean) -> Unit
) {
    when {
        loading -> LoadingState()
        allFavourites.isEmpty() -> EmptyState()
        else -> FavouriteContent(
            allFavourites = allFavourites,
            onNavigateToChapterFromFavourite = onNavigateToChapterFromFavourite,
            onFavouriteClick = onFavouriteClick
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading Favourites...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.favorite_icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "No Favourites Found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Add hadith to favourites to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FavouriteContent(
    allFavourites: List<HadithFavourite>,
    onNavigateToChapterFromFavourite: (bookId: Int, chapterId: Int) -> Unit,
    onFavouriteClick: (bookId: Int, chapterId: Int, hadithId: Int, favourite: Boolean) -> Unit
) {
    val groupedFavourites = allFavourites.groupBy { it.book_title_english }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedFavourites.forEach { (bookTitle, favourites) ->
            item {
                BookSection(
                    bookTitle = bookTitle,
                    favourites = favourites,
                    onNavigateToChapterFromFavourite = onNavigateToChapterFromFavourite,
                    onFavouriteClick = onFavouriteClick
                )
            }
        }
    }
}

@Composable
private fun BookSection(
    bookTitle: String,
    favourites: List<HadithFavourite>,
    onNavigateToChapterFromFavourite: (bookId: Int, chapterId: Int) -> Unit,
    onFavouriteClick: (bookId: Int, chapterId: Int, hadithId: Int, favourite: Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Book Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${favourites.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Favourites List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                favourites.forEach { favourite ->
                    FavouriteItem(
                        favourite = favourite,
                        onNavigateToChapterFromFavourite = onNavigateToChapterFromFavourite,
                        onFavouriteClick = onFavouriteClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FavouriteItem(
    favourite: HadithFavourite,
    onNavigateToChapterFromFavourite: (bookId: Int, chapterId: Int) -> Unit,
    onFavouriteClick: (bookId: Int, chapterId: Int, hadithId: Int, favourite: Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigateToChapterFromFavourite(favourite.bookId, favourite.chapterId)
            },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter number container
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = favourite.chapter_title_arabic,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = utmaniQuranFont,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = favourite.chapter_title_english,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Hadith ${favourite.hadithId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Favourite button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (favourite.favourite)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = {
                        onFavouriteClick(
                            favourite.bookId,
                            favourite.chapterId,
                            favourite.hadithId,
                            !favourite.favourite
                        )
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (favourite.favourite) R.drawable.favorite_icon
                            else R.drawable.favorite_icon_unseletced
                        ),
                        contentDescription = if (favourite.favourite) "Remove from favorites"
                        else "Add to favorites",
                        tint = if (favourite.favourite) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun FavouriteListItemPreview() {
    FavouriteItem(
        favourite = HadithFavourite(
            bookId = 1,
            chapterId = 1,
            hadithId = 1,
            chapter_title_arabic = "الفاتحة",
            chapter_title_english = "Al-Fatihah",
            favourite = true,
            book_title_arabic = "القرآن",
            book_title_english = "Quran",
            idInBook = 1
        ),
        onNavigateToChapterFromFavourite = { _, _ -> },
        onFavouriteClick = { _, _, _, _ -> }
    )
}