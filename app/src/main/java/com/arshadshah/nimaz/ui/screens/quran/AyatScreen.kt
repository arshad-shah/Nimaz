package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.constants.AppConstants.TAFSEER_SCREEN_ROUTE
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading
import com.arshadshah.nimaz.ui.components.quran.AyaItem
import com.arshadshah.nimaz.ui.components.quran.QuranBottomBar
import com.arshadshah.nimaz.ui.components.quran.SurahHeader
import com.arshadshah.nimaz.viewModel.AyatState
import com.arshadshah.nimaz.viewModel.AyatViewModel


// AyatScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyatScreen(
    number: String,
    isSurah: String,
    language: String,
    scrollToAya: Int? = null,
    navController: NavHostController,
    viewModel: AyatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(number, isSurah) {
        viewModel.handleEvent(
            AyatViewModel.AyatEvent.LoadAyat(
                number = number.toInt(),
                isSurah = isSurah.toBoolean(),
                language = language
            )
        )
    }

    AyatScreenContent(
        state = state,
        scrollToAya = scrollToAya,
        isSurah = isSurah.toBoolean(),
        onNavigateBack = { navController.popBackStack() },
        onNavigateToTafsir = { ayaNumber, surahNumber ->
            navController.navigate(
                TAFSEER_SCREEN_ROUTE
                    .replace("{surahNumber}", surahNumber.toString())
                    .replace("{ayaNumber}", ayaNumber.toString())
            )
        },
        onEvent = viewModel::handleEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyatScreenContent(
    state: AyatState,
    scrollToAya: Int?,
    onNavigateBack: () -> Unit,
    isSurah: Boolean,
    onNavigateToTafsir: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    (if (isSurah) state.currentSurah?.englishName else state.currentJuz?.tname)?.let {
                        Text(
                            text = it
                        )
                    }
                },
                navigationIcon = {
                    OutlinedIconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            QuranBottomBar(
                displaySettings = state.displaySettings,
                onEvent = onEvent
            )
        }
    ) { padding ->
        when {
            state.isLoading -> PageLoading()
            state.error != null -> PageErrorState(message = state.error)
            else -> AyatListContainer(
                state = state,
                scrollToAya = scrollToAya,
                contentPadding = padding,
                onNavigateToTafsir = onNavigateToTafsir,
                onEvent = onEvent
            )
        }
    }
}

// AyatListContainer.kt
@Composable
fun AyatListContainer(
    state: AyatState,
    scrollToAya: Int?,
    contentPadding: PaddingValues,
    onNavigateToTafsir: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToAya) {
        scrollToAya?.let { index ->
            listState.animateScrollToItem(index)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // List of Ayat
            items(
                items = state.ayatList,
                key = { it.ayaNumberInQuran }
            ) { aya ->
                if (isSpecialAya(aya)) {
                    onEvent(AyatViewModel.AyatEvent.GetSurahById(aya.suraNumber))
                    SurahHeader(surah = state.currentSurah!!)
                }
                AyaItem(
                    aya = aya,
                    displaySettings = state.displaySettings,
                    audioState = state.audioState,
                    onTafseerClick = onNavigateToTafsir,
                    onEvent = onEvent
                )
            }
        }
    }
}

private fun isSpecialAya(aya: LocalAya): Boolean {
    // Define conditions for special Ayas here
    return aya.ayaNumberInQuran == 0 ||
            aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾" ||
            aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ" ||
            (aya.suraNumber == 9 && aya.ayaNumberInSurah == 1)
}
