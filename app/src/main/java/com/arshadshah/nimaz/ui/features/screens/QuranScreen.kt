package com.arshadshah.nimaz.ui.features.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import com.arshadshah.nimaz.ui.components.bLogic.quran.JuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.SurahList

@Composable
fun QuranScreen(
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {

    var state by remember { mutableStateOf(0) }
    val titles = listOf("Surah", "Juz")
    Column {
        TabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = state == index,
                    onClick = { state = index },
                    text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
        when (state) {
            0 -> SurahList(
                paddingValues = paddingValues,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
            1 -> JuzList(
                paddingValues = paddingValues,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
    }
}