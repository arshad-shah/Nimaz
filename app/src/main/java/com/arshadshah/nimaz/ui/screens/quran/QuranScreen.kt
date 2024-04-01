package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.QURAN_VIEWMODEL_KEY
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN
import com.arshadshah.nimaz.ui.components.common.CustomTabs
import com.arshadshah.nimaz.ui.components.quran.JuzList
import com.arshadshah.nimaz.ui.components.quran.SurahList
import com.arshadshah.nimaz.viewModel.QuranViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranScreen(
    paddingValues: PaddingValues,
    context: MainActivity,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    viewModel: QuranViewModel = viewModel(
        key = QURAN_VIEWMODEL_KEY,
        viewModelStoreOwner = context
    ),
) {
    viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Initialize_Quran)

    val titles = listOf("Sura", "Juz", "My Quran")
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0F,
    ) {
        titles.size
    }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .testTag(TEST_TAG_QURAN)
    ) {

        CustomTabs(pagerState, titles)
        HorizontalPager(
            pageSize = PageSize.Fill,
            state = pagerState,
        ) { page ->
            when (page) {
                0 -> {
                    Log.d(AppConstants.QURAN_SURAH_SCREEN_TAG, "Surah Screen")
                    val surahListState = viewModel.surahListState.collectAsState()
                    val isLoadingSurah = viewModel.loadingState.collectAsState()
                    val errorSurah = viewModel.errorState.collectAsState()
                    Log.d(
                        AppConstants.QURAN_SURAH_SCREEN_TAG,
                        "surahListState.value = ${surahListState.value}"
                    )
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        SurahList(
                            onNavigateToAyatScreen = onNavigateToAyatScreen,
                            state = surahListState,
                            loading = isLoadingSurah.value,
                            error = errorSurah.value
                        )
                    }
                }

                1 -> {
                    Log.d(AppConstants.QURAN_JUZ_SCREEN_TAG, "Juz Screen")
                    val juzListState = viewModel.juzListState.collectAsState()
                    val isLoadingJuz = viewModel.loadingState.collectAsState()
                    val errorJuz = viewModel.errorState.collectAsState()
                    Log.d(
                        AppConstants.QURAN_JUZ_SCREEN_TAG,
                        "juzListState.value = ${juzListState.value}"
                    )
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        JuzList(
                            onNavigateToAyatScreen = onNavigateToAyatScreen,
                            state = juzListState,
                            loading = isLoadingJuz.value,
                            error = errorJuz.value
                        )
                    }
                }

                2 -> {
                    val bookmarks = viewModel.bookmarks.collectAsState()
                    val favorites = viewModel.favorites.collectAsState()
                    val notes = viewModel.notes.collectAsState()
                    MyQuranScreen(
                        bookmarks = bookmarks,
                        favorites = favorites,
                        notes = notes,
                        suraList = viewModel.surahListState.collectAsState(),
                        onNavigateToAyatScreen = onNavigateToAyatScreen,
                        handleEvents = viewModel::handleAyaEvent
                    )
                }
            }
        }
    }
}