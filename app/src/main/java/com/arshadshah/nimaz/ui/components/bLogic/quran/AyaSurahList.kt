package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import com.arshadshah.nimaz.ui.models.Aya

@Composable
fun AyaSurahList(
    paddingValues: PaddingValues,
    number: Int,
    isEnglish: Boolean,
    state: State<AyaSurahViewModel.AyaSurahState>,
) {
    when (val ayatSurahListState = state.value) {
        is AyaSurahViewModel.AyaSurahState.Loading -> {
            CircularLoaderCard()
        }
        is AyaSurahViewModel.AyaSurahState.Success -> {
            val correctedAyatList = processSurahAyatMap(ayatSurahListState.data, isEnglish, number)
            AyaListUI(ayaList = correctedAyatList, paddingValues = paddingValues)
        }
        is AyaSurahViewModel.AyaSurahState.Error -> {
            Toast.makeText(
                LocalContext.current,
                ayatSurahListState.errorMessage,
                Toast.LENGTH_SHORT
            ).show()
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