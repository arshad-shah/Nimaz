package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.JuzListUI

@Composable
fun JuzList(
    viewModel: JuzViewModel = JuzViewModel(),
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {
    when (val state = viewModel.juzState.collectAsState().value) {
        is JuzViewModel.JuzState.Loading -> {
            CircularLoaderCard()
        }
        is JuzViewModel.JuzState.Success -> {
            JuzListUI(
                juz = state.data,
                paddingValues = paddingValues,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
        is JuzViewModel.JuzState.Error -> {
            Text(text = state.errorMessage)
        }
    }
}