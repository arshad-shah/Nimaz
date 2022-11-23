package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.AyaSurahViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import com.arshadshah.nimaz.ui.components.ui.quran.Verses
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

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
            //get the translation type from shared preferences
            val pageType = PrivateSharedPreferences(LocalContext.current).getData(key ="PageType", s = "List")
            var isList = true
            if(pageType != "List"){
                isList = false
            }

            if (isList) {
                AyaListUI(ayaList = correctedAyatList, paddingValues = paddingValues)
            } else {
                Verses(correctedAyatList, paddingValues)
            }
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
    //an empty number
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
        "surah",
        surahNumber
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