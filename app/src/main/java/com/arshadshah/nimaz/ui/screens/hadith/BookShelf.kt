package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.data.local.models.HadithMetadata
import com.arshadshah.nimaz.ui.components.common.CustomTabs
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
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
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
            CustomTabs(pagerState, titles)

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
        is ViewState.Loading -> LoadingState()
        is ViewState.Success -> {
            if (booksState.data.isEmpty()) {
                EmptyState(
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

        is ViewState.Error -> ErrorState(message = booksState.message)
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
            .padding(horizontal = 8.dp)
            .clickable { onClick(metadata.id, metadata.title_english) },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = metadata.title_english,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${metadata.length}",
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Author and Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = metadata.author_english,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
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
                        shape = RoundedCornerShape(16.dp),
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
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavouritesTabContent(
    favouritesState: ViewState<List<HadithFavourite>>,
    onNavigateToChapterFromFavourite: (Int, Int) -> Unit,
    onFavouriteClick: (Int, Int, Int, Boolean) -> Unit
) {
    when (favouritesState) {
        is ViewState.Loading -> LoadingState()
        is ViewState.Success -> {
            if (favouritesState.data.isEmpty()) {
                EmptyState(
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

        is ViewState.Error -> ErrorState(message = favouritesState.message)
    }
}


@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
private fun BooksTab(
    metadataList: List<HadithMetadata>,
    onNavigateToChaptersList: (id: Int, title: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
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