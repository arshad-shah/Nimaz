package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.data.local.models.HadithMetadata
import com.arshadshah.nimaz.ui.components.common.CustomTabsWithPager
import com.arshadshah.nimaz.ui.components.common.NoResultFound
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.HadithViewModel
import com.arshadshah.nimaz.viewModel.ViewState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookShelf(
    viewModel: HadithViewModel = hiltViewModel(),
    onNavigateToChaptersList: (id: Int, title: String) -> Unit,
    onNavigateToChapterFromFavourite: (Int, Int) -> Unit,
    navController: NavHostController
) {
    val booksState by viewModel.booksState.collectAsState()
    val favouritesState by viewModel.favouritesState.collectAsState()
    val titles = listOf("Books", "Favourites")
    val pagerState = rememberPagerState { titles.size }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            viewModel.getAllFavourites()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Hadith Shelf") },
                navigationIcon = {
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            CustomTabsWithPager(pagerState, titles)

            HorizontalPager(
                pageSize = PageSize.Fill,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BooksTabContent(
                        booksState = booksState,
                        onNavigateToChaptersList = onNavigateToChaptersList
                    )

                    1 -> FavouritesTabContent(
                        favouritesState = favouritesState,
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
private fun BooksTabContent(
    booksState: ViewState<List<HadithMetadata>>,
    onNavigateToChaptersList: (id: Int, title: String) -> Unit
) {
    when (booksState) {
        is ViewState.Loading -> PageLoading()
        is ViewState.Success -> {
            if (booksState.data.isEmpty()) {
                NoResultFound(
                    title = "No Books Found",
                    subtitle = "The book collection appears to be empty"
                )
            } else {
                BooksTab(
                    metadataList = booksState.data,
                    onNavigateToChaptersList = onNavigateToChaptersList
                )
            }
        }

        is ViewState.Error -> PageErrorState(message = booksState.message)
    }
}

@Composable
private fun BookCard(
    metadata: HadithMetadata,
    onClick: (id: Int, title: String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(metadata.id, metadata.title_english) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(4.dp),
                        text = metadata.title_english,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${metadata.id}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Content Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Author and Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            text = metadata.author_english,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "${metadata.length} Ahadith",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Arabic Title
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = metadata.title_arabic,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = utmaniQuranFont,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:small_phone", showBackground = true, name = "Book card small phone")
@Composable
fun BookCardPreview() {
    BookCard(
        metadata = HadithMetadata(
            id = 1,
            title_english = "Sunan Abi Dawood",
            title_arabic = "سنن أبي داود",
            author_english = "Imam Sulaiman bin Ash'ath abu Dawood al-Sijistani",
            author_arabic = "الإمام سليمان بن أشعث أبو داود السجستاني",
            length = 5274,
            introduction_arabic = "سنن أبي داود",
            introduction_english = "Sunan Abi Dawood"
        )
    ) { _, _ -> }
}


@Composable
private fun FavouritesTabContent(
    favouritesState: ViewState<List<HadithFavourite>>,
    onNavigateToChapterFromFavourite: (Int, Int) -> Unit,
    onFavouriteClick: (Int, Int, Int, Boolean) -> Unit
) {
    when (favouritesState) {
        is ViewState.Loading -> PageLoading()
        is ViewState.Success -> {
            if (favouritesState.data.isEmpty()) {
                NoResultFound(
                    title = "No Favourites Found",
                    subtitle = "You haven't added any favourites yet"
                )
            } else {
                FavouriteHadithList(
                    loading = false,
                    allFavourites = favouritesState.data,
                    onNavigateToChapterFromFavourite = onNavigateToChapterFromFavourite,
                    onFavouriteClick = onFavouriteClick
                )
            }
        }

        is ViewState.Error -> PageErrorState(message = favouritesState.message)
    }
}


@Composable
private fun BooksTab(
    metadataList: List<HadithMetadata>,
    onNavigateToChaptersList: (id: Int, title: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(metadataList.size) { index ->
            BookCard(
                metadata = metadataList[index],
                onClick = onNavigateToChaptersList
            )
        }
    }
}