package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.quran.AyaJuzList
import com.arshadshah.nimaz.ui.components.quran.AyaSurahList
import com.arshadshah.nimaz.viewModel.QuranViewModel

@Composable
fun AyatScreen(
    number: String,
    isSurah: String,
    language: String,
    paddingValues: PaddingValues,
    scrollToAya: Int? = null,
) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.QURAN_VIEWMODEL_KEY,
        initializer = { QuranViewModel(context) },
        viewModelStoreOwner = context as ComponentActivity
    )
    val pageMode = remember { viewModel.display_Mode }.collectAsState()


    Log.d(AppConstants.QURAN_SCREEN_TAG, "AyatScreen: $number $isSurah $language")

    if (isSurah.toBoolean()) {
        Log.d(AppConstants.QURAN_SCREEN_TAG, "AyatScreen: isSurah")
        //execute it once
        LaunchedEffect(key1 = number)
        {
            viewModel.getAllAyaForSurah(number.toInt(), language)
        }
        val ayatSurah = remember { viewModel.ayaListState }.collectAsState()
        val loadingAyatSurah = remember { viewModel.loadingState }.collectAsState()
        val errorAyatSurah = remember { viewModel.errorState }.collectAsState()
        AyaSurahList(
            number = number.toInt(), language = language,
            paddingValues = paddingValues,
            state = ayatSurah,
            pageMode = pageMode,
            type = "surah",
            loading = loadingAyatSurah.value,
            error = errorAyatSurah.value,
            scrollToAya = scrollToAya,
        )

    } else {
        Log.d(AppConstants.QURAN_SCREEN_TAG, "AyatScreen: isJuz")
        //execute it once
        LaunchedEffect(key1 = number)
        {
            viewModel.getAllAyaForJuz(number.toInt(), language)
        }
        val ayatJuz = remember { viewModel.ayaListState }.collectAsState()
        val loadingAyatJuz = remember { viewModel.loadingState }.collectAsState()
        val errorAyatJuz = remember { viewModel.errorState }.collectAsState()
        AyaJuzList(
            number = number.toInt(),
            language = language,
            paddingValues = paddingValues,
            state = ayatJuz,
            type = "juz",
            pageMode = pageMode,
            loading = loadingAyatJuz.value,
            error = errorAyatJuz.value,
            scrollToAya = scrollToAya,
        )
    }
}
