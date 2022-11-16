package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.JuzListUI

import org.json.JSONArray

@Composable
fun JuzList(
    viewModel: JuzViewModel = JuzViewModel(LocalContext.current),
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {
    when (val state = viewModel.juzState.collectAsState().value) {
        is JuzViewModel.JuzState.Loading -> {
            CircularLoaderCard()
        }
        is JuzViewModel.JuzState.Success -> {
            //map the surahs to its number
            //convert the surah from json to a map
            val juzList = JSONArray(state.data)
            val juzMap = List<Map<String, String>>(juzList.length()) { index ->
                val surah = juzList.getJSONObject(index)
                mapOf(
                    "juzNumber" to surah.getString("juznumberdata"),
                    "name" to surah.getString("name"),
                    "tname" to surah.getString("tname"),
                )
            }
            JuzListUI(
                juz = juzMap,
                paddingValues = paddingValues,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
        is JuzViewModel.JuzState.Error -> {
            Text(text = state.errorMessage)
        }
    }
}