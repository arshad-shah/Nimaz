package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.CustomTabsWithPager
import com.arshadshah.nimaz.ui.components.quran.JuzList
import com.arshadshah.nimaz.ui.components.quran.SurahList
import com.arshadshah.nimaz.viewModel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    navController: NavHostController,
    viewModel: QuranViewModel = hiltViewModel()
) {
    val pages = listOf(
        QuranPage("Sura") {
            SuraContent(viewModel, onNavigateToAyatScreen)
        },
        QuranPage("Juz") {
            JuzContent(viewModel, onNavigateToAyatScreen)
        },
        QuranPage("My Quran") {
            MyQuranContent(viewModel, onNavigateToAyatScreen)
        },
        QuranPage("Search") {
            SearchContent(viewModel, onAyaClick = {
                //number: String, isSurah: Boolean, language: String, scrollToAya: Int?
                onNavigateToAyatScreen(
                    it.suraNumber.toString(),
                    true,
                    "English",
                    it.ayaNumberInSurah
                )
            })
        }
    )

    val pagerState = rememberPagerState { pages.size }

    LaunchedEffect(Unit) {
        viewModel.getSurahList()
        viewModel.getJuzList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Quran")
                },
                navigationIcon = {
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
            )
        },
    ) {
        Column(modifier = Modifier.padding(it)) {
            CustomTabsWithPager(
                pagerState = pagerState,
                titles = pages.map { it.title }
            )

            HorizontalPager(
                pageSize = PageSize.Fill,
                state = pagerState,
            ) { page ->
                pages[page].content()
            }
        }
    }
}

@Composable
private fun SuraContent(
    viewModel: QuranViewModel,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit
) {
    val surahListState = viewModel.surahListState.collectAsState()
    val isLoading = viewModel.loadingState.collectAsState()
    val error = viewModel.errorState.collectAsState()

    SurahList(
        onNavigateToAyatScreen = onNavigateToAyatScreen,
        state = surahListState,
        loading = isLoading.value,
        error = error.value
    )
}

@Composable
private fun JuzContent(
    viewModel: QuranViewModel,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit
) {
    val juzListState = viewModel.juzListState.collectAsState()
    val isLoading = viewModel.loadingState.collectAsState()
    val error = viewModel.errorState.collectAsState()

    JuzList(
        onNavigateToAyatScreen = onNavigateToAyatScreen,
        state = juzListState,
        loading = isLoading.value,
        error = error.value
    )
}

@Composable
private fun MyQuranContent(
    viewModel: QuranViewModel,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit
) {
    val isLoading = viewModel.loadingState.collectAsState()
    val readingProgress = viewModel.readingProgress.collectAsState()
    val quickJumps = viewModel.quickJumps.collectAsState()
    val khatamState = viewModel.khatamState.collectAsState() // ADD THIS

    MyQuranScreen(
        bookmarks = viewModel.bookmarks.collectAsState(),
        favorites = viewModel.favorites.collectAsState(),
        notes = viewModel.notes.collectAsState(),
        suraList = viewModel.surahListState.collectAsState(),
        readingProgress = readingProgress,
        quickJumps = quickJumps,
        khatamState = khatamState, // ADD THIS
        onNavigateToAyatScreen = onNavigateToAyatScreen,
        handleEvents = viewModel::handleAyaEvent,
        onKhatamEvent = viewModel::handleAyaEvent, // ADD THIS
        onDeleteQuickJump = viewModel::deleteQuickJump,
        onDeleteReadingProgress = viewModel::deleteReadingProgress,
        onClearAllProgress = viewModel::clearAllReadingProgress,
        isLoading = isLoading
    )
}

@Composable
private fun SearchContent(
    viewModel: QuranViewModel,
    onAyaClick: (LocalAya) -> Unit
) {
    val searchQuery = viewModel.searchQuery.collectAsState()
    val searchResults = viewModel.filteredSearchResults.collectAsState()
    val searchLanguage = viewModel.searchLanguage.collectAsState()
    val searchFilters = viewModel.searchFilters.collectAsState()
    val isLoading = viewModel.loadingState.collectAsState()
    val error = viewModel.errorState.collectAsState()

    QuranSearchScreen(
        searchQuery = searchQuery.value,
        searchResults = searchResults.value,
        searchLanguage = searchLanguage.value,
        searchFilters = searchFilters.value,
        isLoading = isLoading.value,
        error = error.value,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSearchLanguageChange = viewModel::setSearchLanguage,
        onSearchFiltersChange = viewModel::updateSearchFilters,
        onSearch = viewModel::searchAyas,
        onAdvancedSearch = viewModel::searchAyasAdvanced,
        onSearchInFavorites = viewModel::searchInFavorites,
        onSearchInBookmarks = viewModel::searchInBookmarks,
        onSearchInNotes = viewModel::searchInNotes,
        onClearSearch = viewModel::clearSearch,
        onAyaClick = onAyaClick,
        onBookmarkToggle = { aya ->
            viewModel.handleAyaEvent(
                QuranViewModel.AyaEvent.BookmarkAya(
                    aya.ayaNumberInQuran,
                    aya.suraNumber,
                    aya.ayaNumberInSurah,
                    !aya.bookmark
                )
            )
        },
        onFavoriteToggle = { aya ->
            viewModel.handleAyaEvent(
                QuranViewModel.AyaEvent.FavoriteAya(
                    aya.ayaNumberInQuran,
                    aya.suraNumber,
                    aya.ayaNumberInSurah,
                    !aya.favorite
                )
            )
        }
    )
}

private data class QuranPage(
    val title: String,
    val content: @Composable () -> Unit
)