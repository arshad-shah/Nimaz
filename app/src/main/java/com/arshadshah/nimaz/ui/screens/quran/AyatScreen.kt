package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.repositories.SpacesFileRepository
import com.arshadshah.nimaz.ui.components.common.BannerSmall
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.quran.AyaListUI
import com.arshadshah.nimaz.ui.components.quran.MoreMenu
import com.arshadshah.nimaz.ui.components.quran.TopBarMenu
import com.arshadshah.nimaz.viewModel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyatScreen(
    number: String,
    isSurah: String,
    language: String,
    scrollToAya: Int? = null,
    context: MainActivity,
    spaceFilesRepository: SpacesFileRepository = SpacesFileRepository(context), // Pass repository directly
    navController: NavHostController,
    viewModel: QuranViewModel = hiltViewModel()
) {
    // LaunchedEffect should depend on both number and isSurah
    LaunchedEffect(Unit) {
        Log.d(AppConstants.QURAN_SCREEN_TAG, "Update occurred on Ayat screen: $number")
        if (isSurah.toBoolean()) {
            viewModel.getAllAyaForSurah(number.toInt(), language)
        } else {
            viewModel.getAllAyaForJuz(number.toInt(), language)
        }
    }

    val ayat = viewModel.ayaListState.collectAsState()
    val loading = viewModel.loadingState.collectAsState()
    val error = viewModel.errorState.collectAsState()

    val pageMode = viewModel.display_Mode.collectAsState()
    val surah = viewModel.surahState.collectAsState()
    val arabicFontSize = viewModel.arabic_Font_size.collectAsState()
    val arabicFont = viewModel.arabic_Font.collectAsState()
    val translationFontSize = viewModel.translation_Font_size.collectAsState()
    val translation = viewModel.translation.collectAsState()
    val scrollToVerse = viewModel.scrollToAya.collectAsState()
    val (menuOpen, setMenuOpen) = remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(title = {
                TopBarMenu(
                    number = number.toInt(),
                    isSurah = isSurah.toBoolean(),
                    getAllAyats = if (isSurah.toBoolean()) viewModel::getAllAyaForSurah else viewModel::getAllAyaForJuz
                )
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
                actions = {
                    //open the menu
                    IconButton(onClick = {
                        setMenuOpen(
                            true
                        )
                    }) {
                        Icon(
                            modifier = Modifier.size(
                                24.dp
                            ),
                            painter = painterResource(
                                id = R.drawable.settings_sliders_icon
                            ),
                            contentDescription = "Menu"
                        )
                    }
                    MoreMenu(
                        menuOpen = menuOpen,
                        setMenuOpen = setMenuOpen,
                        handleEvents = viewModel::handleQuranMenuEvents
                    )
                }
            )
        },
    ) {
        if (error.value != "") {
            BannerSmall(title = "Error", message = error.value, variant = BannerVariant.Error)
        }

        if (pageMode.value == "List") {
            AyaListUI(
                ayaList = ayat.value,
                paddingValues = it,
                loading = loading.value,
                type = if (isSurah.toBoolean()) "surah" else "juz",
                number = number.toInt(),
                scrollToAya = scrollToAya,
                surah = surah.value,
                arabicFontSize = arabicFontSize.value,
                arabicFont = arabicFont.value,
                translationFontSize = translationFontSize.value,
                translation = translation.value,
                scrollToVerse = scrollToVerse.value,
                handleAyaEvents = viewModel::handleAyaEvent,
                handleQuranMenuEvents = viewModel::handleQuranMenuEvents,
                downloadAyaAudioFile = { surahNumber, ayaNumberInSurah, downloadCallback ->
                    spaceFilesRepository.downloadAyaFile(
                        surahNumber,
                        ayaNumberInSurah,
                        downloadCallback
                    )
                },
            )
        }
    }
}
