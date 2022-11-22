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
fun AyaJuzList(
    paddingValues: PaddingValues,
    number: Int,
    isEnglish: Boolean,
    state: State<AyaJuzViewModel.AyaJuzState>,
) {
    when (val ayatJuzListState = state.value) {
        is AyaJuzViewModel.AyaJuzState.Loading -> {
            CircularLoaderCard()
        }
        is AyaJuzViewModel.AyaJuzState.Success -> {
            val correctedList = processAyatMap(ayatJuzListState.data, isEnglish, number)

            AyaListUI(ayaList = correctedList, paddingValues = paddingValues)
        }
        is AyaJuzViewModel.AyaJuzState.Error -> {
            Toast.makeText(
                LocalContext.current,
                ayatJuzListState.errorMessage,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

// a function that takes a list of maps of strings to strings and returns a list of maps of strings to strings
fun processAyatMap(
    ayaList: ArrayList<Aya>,
    isEnglish: Boolean,
    juzNumber: Int
): ArrayList<Aya> {
    //add the following object to index 0 of ayaForSurah without losing value of index 0 in ayaForSurah
    val ayaNumberOfBismillah = "0"

    val ayaOfBismillah = if (isEnglish) {
        "In the name of Allah, the Entirely Merciful, the Especially Merciful."
    } else {
        "اللہ کے نام سے جو رحمان و رحیم ہے"
    }
    val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

    //create a map of the aya of bismillah
    val ayaOfBismillahMap = Aya(
        ayaNumberOfBismillah.toInt(),
        ayaArabicOfBismillah,
        ayaOfBismillah,
    )

    //find all the objects in arraylist ayaForJuz where ayaForJuz[i]!!.ayaNumber = 1
    //add object bismillah before it for every occurance of ayaForJuz[i]!!.ayaNumber = 1
    var index = 0
    while (index < ayaList.size) {
        if (ayaList[index].ayaArabic != ayaOfBismillahMap.ayaArabic) {
            //add bismillah before ayaForJuz[i]
            if (ayaList[index].ayaNumber == 1) {
                if (juzNumber + 1 != 10 && index != 36) {
                    //add the map of bismillah to ayaList at the current index
                    ayaList.add(index, ayaOfBismillahMap)
                    //skip the next iteration
                    index++
                }
            }
        }
        index++
    }
    return ayaList
}