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
fun AyaJuzList(
    viewModel: AyaJuzViewModel = AyaJuzViewModel(),
    paddingValues: PaddingValues,
    number: Int,
    isEnglish: Boolean,
) {
    val context = LocalContext.current
    viewModel.getAllAyaForJuz(context, number, isEnglish)
    when (val state = viewModel.ayaJuzstate.collectAsState().value) {
        is AyaJuzViewModel.AyaJuzState.Loading -> {
            CircularLoaderCard()
        }
        is AyaJuzViewModel.AyaJuzState.Success -> {
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
        is AyaJuzViewModel.AyaJuzState.Error -> {
            Text(text = state.errorMessage)
        }
    }
}