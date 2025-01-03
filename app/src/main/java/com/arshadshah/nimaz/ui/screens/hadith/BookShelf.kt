package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.HADITH_VIEW_MODEL
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.data.local.models.HadithMetadata
import com.arshadshah.nimaz.ui.components.common.CustomTabs
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.HadithViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookShelf(
    viewModel: HadithViewModel = viewModel(key = HADITH_VIEW_MODEL),
    onNavigateToChaptersList: (id: Int, title: String) -> Unit,
    onNavigateToChapterFromFavourite: (Int, Int) -> Unit,
    navController: NavHostController
) {
    val metadataList by viewModel.allHadithBooks.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val allFavourites by viewModel.allFavourites.collectAsState()

    val titles = listOf("Books", "Favourites")
    val pagerState = rememberPagerState { titles.size }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Hadith Shelf")
                },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = {
                            navController.popBackStack()
                        }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .background(MaterialTheme.colorScheme.background)
        ) {

            CustomTabs(pagerState, titles)

            HorizontalPager(
                pageSize = PageSize.Fill,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BooksTab(metadataList, onNavigateToChaptersList)
                    1 -> FavouritesTab(
                        loading = loading,
                        allFavourites = allFavourites,
                        onNavigateToChapterFromFavourite = onNavigateToChapterFromFavourite,
                        viewModel = viewModel
                    )
                }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(metadataList.size) { index ->
            BookCard(
                metadata = metadataList[index],
                onClick = onNavigateToChaptersList
            )
        }
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
            .clickable { onClick(metadata.id, metadata.title_english) }
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // English Title and Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = metadata.title_english,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = metadata.author_english,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SuggestionChip(
                        onClick = { },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        label = {
                            Text("${metadata.length} Ahadith")
                        }
                    )
                }

                // Arabic Title
                Spacer(modifier = Modifier.width(16.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = metadata.title_arabic,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = utmaniQuranFont,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FavouritesTab(
    loading: Boolean,
    allFavourites: List<HadithFavourite>,
    onNavigateToChapterFromFavourite: (Int, Int) -> Unit,
    viewModel: HadithViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.getAllFavourites()
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    } else {
        // Implement your existing FavouriteHadithList here
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