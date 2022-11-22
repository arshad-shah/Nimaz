package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.SurahListUI

@Composable
fun SurahList(
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit,
    state: State<SurahViewModel.SurahState>
) {
    when (val surahState = state.value) {
        is SurahViewModel.SurahState.Loading -> {
            CircularLoaderCard()
        }
        is SurahViewModel.SurahState.Success -> {
            SurahListUI(
                surahs = surahState.data!!,
                paddingValues = paddingValues,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
        is SurahViewModel.SurahState.Error -> {
            Toast.makeText(
                LocalContext.current,
                surahState.errorMessage,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}