package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.HADITH_VIEW_MODEL
import com.arshadshah.nimaz.data.local.models.HadithMetadata
import com.arshadshah.nimaz.ui.components.common.CustomTabs
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.HadithViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookShelf(
    viewModel: HadithViewModel = viewModel(
        key = HADITH_VIEW_MODEL,
        initializer = { HadithViewModel() }
    ),
    paddingValues: PaddingValues,
    onNavigateToChaptersList: (id: Int, title: String) -> Unit,
    onNavigateToChapterFromFavourite: (Int, Int) -> Unit
) {
    val metadataList by viewModel.allHadithBooks.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val allFavourites by viewModel.allFavourites.collectAsState()

    val titles = listOf("Books", "Favourites")
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0F,
    ) {
        titles.size
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .testTag(AppConstants.TEST_TAG_QURAN)
    ) {

        CustomTabs(pagerState, titles)

        HorizontalPager(
            pageSize = PageSize.Fill,
            state = pagerState,
        ) { page ->
            when (page) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(metadataList.size) { row ->
                            BookItem(metadataList[row]) { id: Int, title: String ->
                                onNavigateToChaptersList(
                                    id,
                                    title
                                )
                            }
                        }
                    }
                }

                1 -> {
                    LaunchedEffect(Unit, block = {
                        viewModel.getAllFavourites()
                    })

                    FavouriteHadithList(
                        loading = loading,
                        allFavourites = allFavourites,
                        onNavigateToChapterFromFavourite = onNavigateToChapterFromFavourite,
                        onFavouriteClick = { bookId, chapterId, hadithId, favourite ->
                            viewModel.updateFavouriteStatus(
                                bookId = bookId,
                                chapterId = chapterId,
                                id = hadithId,
                                favouriteStatus = favourite
                            )
                        }
                    )

                }
            }
        }
    }
}


@Composable
fun BookItem(metadata: HadithMetadata, onClick: (id: Int, title: String) -> Unit) {
    val reusableModifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)

    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
    )
    Card(
        colors = cardColors,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = reusableModifier
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable {
                onClick(metadata.id, metadata.title_english)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = metadata.title_arabic,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = utmaniQuranFont,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = reusableModifier
                )
            }
            Text(
                text = metadata.title_english,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = reusableModifier
            )
            Text(
                text = metadata.author_english,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = reusableModifier
            )
            Text(
                text = "${metadata.length} Ahadith",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}


@Preview
@Composable
fun BookShelfPreview() {
    BookItem(
        HadithMetadata(
            id = 1,
            length = 100,
            title_arabic = "عنوان الكتاب",
            author_arabic = "المؤلف",
            introduction_arabic = "تفاصيل الكتاب",
            title_english = "Title",
            author_english = "Author",
            introduction_english = "Introduction"
        )
    ) { _, _ -> }
}
