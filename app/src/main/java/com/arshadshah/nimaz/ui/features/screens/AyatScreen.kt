package com.arshadshah.nimaz.ui.features.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaJuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaJuzViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaSurahList
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaSurahViewModel

@Composable
fun AyatScreen(
    number: String?,
    isSurah: String,
    isEnglish: String,
    paddingValues: PaddingValues
) {
    val ayatSuraViewModel = AyaSurahViewModel()
    val ayatJuzViewModel = AyaJuzViewModel()
    val ayatSuraState = ayatSuraViewModel.ayaSurahState.collectAsState()
    val ayatJuzState = ayatJuzViewModel.ayaJuzstate.collectAsState()
    if (isSurah.toBoolean()) {
        ayatSuraViewModel.getAllAyaForSurah(number!!.toInt(), isEnglish.toBoolean())
        AyaSurahList(
            number = number.toInt(), isEnglish = isEnglish.toBoolean(),
            paddingValues = paddingValues,
            state = ayatSuraState
        )

    } else {
        ayatJuzViewModel.getAllAyaForJuz(number!!.toInt(), isEnglish.toBoolean())
        AyaJuzList(
            number = number.toInt(),
            isEnglish = isEnglish.toBoolean(),
            paddingValues = paddingValues,
            state = ayatJuzState
        )
    }
}
