package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import org.json.JSONArray

@Composable
fun AyaSurahList(
    viewModel: AyaSurahViewModel = AyaSurahViewModel(),
    paddingValues: PaddingValues,
    number: Int,
    isEnglish: Boolean,
) {
    val context = LocalContext.current

    viewModel.getAllAyaForSurah(context, number, isEnglish)

    when (val state = viewModel.ayaSurahState.collectAsState().value) {
        is AyaSurahViewModel.AyaSurahState.Loading -> {
            CircularLoaderCard()
        }
        is AyaSurahViewModel.AyaSurahState.Success -> {
            //map the surahs to its number
            //convert the surah from json to a map
            val ayaList = JSONArray(state.data)
            val ayaMap = List<Map<String, String>>(ayaList.length()) { index ->
                val surah = ayaList.getJSONObject(index)
                mapOf(
                    "ayaNumber" to surah.getString("ayaNumber"),
                    "ayaArabic" to surah.getString("ayaArabic"),
                    "ayaTranslation" to surah.getString("ayaTranslation"),
                )
            }
            AyaListUI(ayaList = ayaMap, paddingValues = paddingValues)
        }
        is AyaSurahViewModel.AyaSurahState.Error -> {
            Text(text = state.errorMessage)
        }
    }
}