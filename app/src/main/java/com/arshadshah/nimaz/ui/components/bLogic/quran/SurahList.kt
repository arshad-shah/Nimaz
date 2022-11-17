package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.SurahListUI

@Composable
fun SurahList(
    viewModel: SurahViewModel = SurahViewModel(),
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {
    when (val state = viewModel.surahState.collectAsState().value) {
        is SurahViewModel.SurahState.Loading -> {
            CircularLoaderCard()
        }
        is SurahViewModel.SurahState.Success -> {
            SurahListUI(
                surahs = state.data!!,
                paddingValues = paddingValues,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
        is SurahViewModel.SurahState.Error -> {
            Text(text = state.errorMessage)
        }
    }
}