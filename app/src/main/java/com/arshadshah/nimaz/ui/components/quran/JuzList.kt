package com.arshadshah.nimaz.ui.components.quran


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_JUZ
import com.arshadshah.nimaz.data.local.models.LocalJuz

@Composable
fun JuzList(
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean,
    error: String,
    state: State<ArrayList<LocalJuz>>,
) {
    if (loading) {
        val listState = rememberLazyListState()

        val juz = LocalJuz(
            number = 1,
            name = "Al-Fatiha",
            tname = "Al-Fatiha",
            juzStartAyaInQuran = 1
        )

        LazyColumn(
            state = listState,
            userScrollEnabled = false
        ) {
            items(7) { index ->
                JuzListItem(
                    isLoading = true,
                    juzNumber = juz.number,
                    name = juz.name,
                    translatedName = juz.tname,
                    navigateToAyatScreen = onNavigateToAyatScreen,
                )
            }
        }
    } else if (error.isNotEmpty()) {
        JuzListUI(
            juz = ArrayList(30),
            onNavigateToAyatScreen = onNavigateToAyatScreen,
            loading = false,
        )
    } else {
        JuzListUI(
            juz = state.value,
            onNavigateToAyatScreen = onNavigateToAyatScreen,
            loading = false
        )
    }
}