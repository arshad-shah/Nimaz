package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.SurahListUI
import org.json.JSONArray

@Composable
fun SurahList(
    viewModel: SurahViewModel = SurahViewModel(LocalContext.current),
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {
    when (val state = viewModel.surahState.collectAsState().value) {
        is SurahViewModel.SurahState.Loading -> {
            CircularLoaderCard()
        }
        is SurahViewModel.SurahState.Success -> {
            //map the surahs to its number
            //convert the surah from json to a map
            val surahList = JSONArray(state.data)
            val surahMap = List<Map<String, String>>(surahList.length()) { index ->
                val surah = surahList.getJSONObject(index)
                mapOf(
                    "surahNumber" to surah.getString("suranumberdata"),
                    "surahAyaAmount" to surah.getString("aya"),
                    "surahAyaStart" to surah.getString("start"),
                    "surahName" to surah.getString("name"),
                    "englishName" to surah.getString("tname"),
                    "englishNameTranslation" to surah.getString("ename"),
                    "type" to surah.getString("type"),
                    "revelationOrder" to surah.getInt("orderOfNuzool").toString(),
                    "rukus" to surah.getInt("rukus").toString(),
                )
            }
            SurahListUI(
                surahs = surahMap,
                paddingValues = paddingValues,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
        is SurahViewModel.SurahState.Error -> {
            Text(text = state.errorMessage)
        }
    }
}