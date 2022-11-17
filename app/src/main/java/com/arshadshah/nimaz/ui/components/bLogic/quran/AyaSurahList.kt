package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import com.arshadshah.nimaz.ui.models.Aya

@Composable
fun AyaSurahList(
    viewModel: AyaSurahViewModel = AyaSurahViewModel(),
    paddingValues: PaddingValues,
    number: Int,
    isEnglish: Boolean,
) {
    viewModel.getAllAyaForSurah(number, isEnglish)

    when (val state = viewModel.ayaSurahState.collectAsState().value) {
        is AyaSurahViewModel.AyaSurahState.Loading -> {
            CircularLoaderCard()
        }
        is AyaSurahViewModel.AyaSurahState.Success -> {
            val correctedAyatList = processSurahAyatMap(state.data, isEnglish, number)
            AyaListUI(ayaList = correctedAyatList, paddingValues = paddingValues)
        }
        is AyaSurahViewModel.AyaSurahState.Error -> {
            Text(text = state.errorMessage)
        }
    }
}

// a function that takes a list of maps of strings to strings and returns a list of maps of strings to strings
fun processSurahAyatMap(
    ayaList: ArrayList<Aya>,
    isEnglish: Boolean,
    surahNumber: Int
): ArrayList<Aya> {
    val ayaNumberOfBismillah = 0
    val ayaOfBismillah = if (isEnglish) {
        "In the name of Allah, the Entirely Merciful, the Especially Merciful."
    } else {
        "اللہ کے نام سے جو رحمان و رحیم ہے"
    }
    val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
    //create a map of the aya of bismillah
    val aya = Aya(
        ayaNumberOfBismillah,
        ayaArabicOfBismillah,
        ayaOfBismillah,
    )
    //first check if an object like this is already in the list
    //check all the attributes of the object bisimillah with the attributes of the object in the list at index 0
    if (ayaList[0].ayaArabic != ayaArabicOfBismillah) {
        if (surahNumber + 1 != 9) {
            ayaList.add(0, aya)
        }
    }

    return ayaList
}