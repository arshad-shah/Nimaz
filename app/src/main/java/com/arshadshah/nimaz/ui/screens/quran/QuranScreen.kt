package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants.QURAN_VIEWMODEL_KEY
import com.arshadshah.nimaz.ui.components.common.CustomTabs
import com.arshadshah.nimaz.ui.components.quran.*
import com.arshadshah.nimaz.viewModel.QuranViewModel

@Composable
fun QuranScreen(
    paddingValues: PaddingValues,
    context: MainActivity,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    viewModel: QuranViewModel = viewModel(key = QURAN_VIEWMODEL_KEY, viewModelStoreOwner = context),
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
        }
    )

    val pagerState = rememberPagerState { pages.size }

    LaunchedEffect(Unit) {
        viewModel.getSurahList()
        viewModel.getJuzList()
    }

    Column(modifier = Modifier.padding(paddingValues)) {
        CustomTabs(
            pagerState = pagerState,
            titles = pages.map { it.title }
        )

        HorizontalPager(
            pageSize = PageSize.Fill,
            state = pagerState,
        ) { page ->
            QuranCard {
                pages[page].content()
            }
        }
    }
}

@Composable
private fun QuranCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
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
    MyQuranScreen(
        bookmarks = viewModel.bookmarks.collectAsState(),
        favorites = viewModel.favorites.collectAsState(),
        notes = viewModel.notes.collectAsState(),
        suraList = viewModel.surahListState.collectAsState(),
        onNavigateToAyatScreen = onNavigateToAyatScreen,
        handleEvents = viewModel::handleAyaEvent
    )
}

private data class QuranPage(
    val title: String,
    val content: @Composable () -> Unit
)