package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.JuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.SurahList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {
    val viewModel = QuranViewModel()
    //save the state of the tab
    val (selectedTab, setSelectedTab) = rememberSaveable { mutableStateOf(0) }
    val titles = listOf("Surah", "Juz")
    Column(modifier = Modifier.padding(paddingValues)) {

        TabRow(selectedTabIndex = selectedTab) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { setSelectedTab(index) },
                    text = {
                        Text(
                            text = title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }
        }
        when (selectedTab) {
            0 -> {
                SurahList(
                    onNavigateToAyatScreen = onNavigateToAyatScreen,
                    state = viewModel.surahState.collectAsState()
                )
            }
            1 -> {
                JuzList(
                    onNavigateToAyatScreen = onNavigateToAyatScreen,
                    state = viewModel.juzState.collectAsState(),
                )
            }
        }
    }
}