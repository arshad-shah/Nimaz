package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
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
                QuranCard {
                    pages[page].content()
                }
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
        shape = MaterialTheme.shapes.extraLarge,
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
    val isLoading = viewModel.loadingState.collectAsState()
    MyQuranScreen(
        bookmarks = viewModel.bookmarks.collectAsState(),
        favorites = viewModel.favorites.collectAsState(),
        notes = viewModel.notes.collectAsState(),
        suraList = viewModel.surahListState.collectAsState(),
        onNavigateToAyatScreen = onNavigateToAyatScreen,
        handleEvents = viewModel::handleAyaEvent,
        isLoading = isLoading
    )
}

private data class QuranPage(
    val title: String,
    val content: @Composable () -> Unit
)