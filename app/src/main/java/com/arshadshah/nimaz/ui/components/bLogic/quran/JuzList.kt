package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.JuzListUI

@Composable
fun JuzList(
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit,
    state: State<JuzViewModel.JuzState>
) {
    when (val juzState = state.value) {
        is JuzViewModel.JuzState.Loading -> {
            CircularLoaderCard()
        }
        is JuzViewModel.JuzState.Success -> {
            JuzListUI(
                juz = juzState.data,
                paddingValues = paddingValues,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
        is JuzViewModel.JuzState.Error -> {
            Toast.makeText(LocalContext.current, juzState.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}