package com.arshadshah.nimaz.ui.components.quran


import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arshadshah.nimaz.data.local.models.LocalJuz

@Composable
fun JuzList(
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean,
    error: String,
    state: State<ArrayList<LocalJuz>>,
) {
    if (loading) {
        JuzListUI(
            juz = ArrayList(30),
            onNavigateToAyatScreen = onNavigateToAyatScreen,
            loading = true
        )
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