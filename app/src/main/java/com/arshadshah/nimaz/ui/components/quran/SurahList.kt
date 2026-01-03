package com.arshadshah.nimaz.ui.components.quran


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading

@Composable
fun SurahList(
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    state: State<ArrayList<LocalSurah>>,
    loading: Boolean,
    error: String,
) {

    if (loading) {
        val listState = rememberLazyListState()

        val surah = LocalSurah(
            number = 1,
            name = "Al-Fatiha",
            englishName = "Al-Fatiha",
            englishNameTranslation = "Al-Fatiha",
            numberOfAyahs = 7,
            revelationType = "Meccan",
            startAya = 2,
            revelationOrder = 5,
            rukus = 2
        )

        LazyColumn(
            state = listState,
            userScrollEnabled = false,
        ) {
            items(6) { _ ->
                SurahCard(
                    surah = surah,
                    loading = true,
                    onNavigate = onNavigateToAyatScreen
                )
            }
        }
    } else if (error.isNotEmpty()) {
        PageErrorState(error)
    } else {
        SurahListUI(
            surahs = state.value,
            onNavigateToAyatScreen = onNavigateToAyatScreen,
            loading = false
        )
    }
}