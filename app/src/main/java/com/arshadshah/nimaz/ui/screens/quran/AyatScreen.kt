package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.repositories.SpacesFileRepository
import com.arshadshah.nimaz.ui.components.common.BannerSmall
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.quran.AyaListUI
import com.arshadshah.nimaz.ui.components.quran.Page
import com.arshadshah.nimaz.viewModel.QuranViewModel

@Composable
fun AyatScreen(
    number: String,
    isSurah: String,
    language: String,
    paddingValues: PaddingValues,
    scrollToAya: Int? = null,
    context: MainActivity,
    viewModel: QuranViewModel = viewModel(
        key = AppConstants.QURAN_VIEWMODEL_KEY,
        viewModelStoreOwner = context
    ),  // Pass ViewModel directly
    spaceFilesRepository: SpacesFileRepository = SpacesFileRepository(context), // Pass repository directly
) {
    // LaunchedEffect should depend on both number and isSurah
    LaunchedEffect(number, isSurah) {
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

    LaunchedEffect(key1 = ayat.value, block = {
        Log.d(AppConstants.QURAN_SCREEN_TAG, "Update occurred on Ayat screen: ${ayat.value}")
    })

    val pageMode = viewModel.display_Mode.collectAsState()
    val surah = viewModel.surahState.collectAsState()
    val arabicFontSize = viewModel.arabic_Font_size.collectAsState()
    val arabicFont = viewModel.arabic_Font.collectAsState()
    val translationFontSize = viewModel.translation_Font_size.collectAsState()
    val translation = viewModel.translation.collectAsState()
    val scrollToVerse = viewModel.scrollToAya.collectAsState()

    if (error.value != "") {
        BannerSmall(title = "Error", message = error.value, variant = BannerVariant.Error)
    }

    if (pageMode.value == "List") {
        AyaListUI(
            ayaList = ayat.value,
            paddingValues = paddingValues,
            language = language,
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
            }
        )
    } else {
        Page(ayat.value, paddingValues, loading.value)
    }
}
