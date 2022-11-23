package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arshadshah.nimaz.data.remote.viewModel.AyaJuzViewModel
import com.arshadshah.nimaz.data.remote.viewModel.AyaSurahViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaJuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaSurahList

@Composable
fun AyatScreen(
    number: String?,
    isSurah: String,
    isEnglish: String,
    paddingValues: PaddingValues
) {
    if (isSurah.toBoolean()) {
        val ayatSuraViewModel = AyaSurahViewModel()
        ayatSuraViewModel.getAllAyaForSurah(number!!.toInt(), isEnglish.toBoolean())
        AyaSurahList(
            number = number.toInt(), isEnglish = isEnglish.toBoolean(),
            paddingValues = paddingValues,
            state = ayatSuraViewModel.ayaSurahState.collectAsState()
        )

    } else {
        val ayatJuzViewModel = AyaJuzViewModel()
        ayatJuzViewModel.getAllAyaForJuz(number!!.toInt(), isEnglish.toBoolean())
        AyaJuzList(
            number = number.toInt(),
            isEnglish = isEnglish.toBoolean(),
            paddingValues = paddingValues,
            state = ayatJuzViewModel.ayaJuzstate.collectAsState()
        )
    }
}
