package com.arshadshah.nimaz.ui.features.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
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
        AyaSurahList(
            number = number!!.toInt(), isEnglish = isEnglish.toBoolean(),
            paddingValues = paddingValues
        )

    } else {
        AyaJuzList(
            number = number!!.toInt(),
            isEnglish = isEnglish.toBoolean(),
            paddingValues = paddingValues
        )
    }
}
