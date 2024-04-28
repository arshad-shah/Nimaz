package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.common.NoResultFound
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont

@Composable
fun FavouriteHadithList(
    loading: Boolean,
    allFavourites: List<HadithFavourite>,
    onNavigateToChapterFromFavourite: (bookId: Int, chapterId: Int) -> Unit,
    onFavouriteClick: (bookId: Int, chapterId: Int, hadithId: Int, favourite: Boolean) -> Unit
) {
    if (loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Loading...", style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (allFavourites.isEmpty()) {
        NoResultFound(
            title = "No Favourites Found",
            subtitle = "You have not added any favourites yet"
        )
    } else {
        // group the favourites by book_title_english
        val groupedFavourites = allFavourites.groupBy { it.book_title_english }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedFavourites.keys.forEach { bookTitle ->
                val listOfFavouritesForABook = groupedFavourites[bookTitle]!!
                item {
                    FeaturesDropDown(
                        items = listOfFavouritesForABook,
                        label = bookTitle,
                        dropDownItem = {
                            FavouriteListItem(
                                chapter_title_arabic = it.chapter_title_arabic,
                                chapter_title_english = it.chapter_title_english,
                                favourite_status = it.favourite,
                                chapterId = it.chapterId,
                                hadithId = it.hadithId,
                                onCardClick = {
                                    onNavigateToChapterFromFavourite(
                                        it.bookId,
                                        it.chapterId
                                    )
                                },
                                onFavouriteClick = { isFavourite: Boolean ->

                                    onFavouriteClick(
                                        it.bookId,
                                        it.chapterId,
                                        it.hadithId,
                                        isFavourite
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FavouriteListItem(
    chapter_title_arabic: String,
    chapter_title_english: String,
    onCardClick: () -> Unit, // Callback when book card is clicked
    onFavouriteClick: (isChecked: Boolean) -> Unit, // Callback when favourite icon is clicked
    favourite_status: Boolean,
    chapterId: Int,
    hadithId: Int
) {
    val reusableModifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)

    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
    )
    OutlinedCard(
        colors = cardColors,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = reusableModifier
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable {
                onCardClick()
            }
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f),
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = chapter_title_english,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = chapter_title_arabic,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = utmaniQuranFont,
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center,
                            modifier = reusableModifier
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = "Book: $chapterId, Hadith: $hadithId",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                OutlinedIconButton(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    enabled = true, onClick = {
                        // Handle favorite toggle here
                        onFavouriteClick(!favourite_status)
                    }) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.favorite_icon),
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun FavouriteListItemPreview() {
    FavouriteListItem(
        chapter_title_arabic = "عنوان القسم",
        chapter_title_english = "Title",
        onCardClick = {},
        onFavouriteClick = {},
        favourite_status = true,
        chapterId = 1,
        hadithId = 1
    )
}