package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.JuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.SurahList

@Composable
fun QuranScreen(
	paddingValues : PaddingValues ,
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
			   )
{
	val viewModel = QuranViewModel(LocalContext.current)
	//save the state of the tab
	val (selectedTab , setSelectedTab) = rememberSaveable { mutableStateOf(0) }
	val titles = listOf("Sura" , "Juz")
	Column(modifier = Modifier.padding(paddingValues)) {

		TabRow(selectedTabIndex = selectedTab) {
			titles.forEachIndexed { index , title ->
				Tab(
						selected = selectedTab == index ,
						onClick = { setSelectedTab(index) } ,
						text = {
							Text(
									text = title ,
									maxLines = 2 ,
									overflow = TextOverflow.Ellipsis ,
									style = MaterialTheme.typography.titleSmall
								)
						}
				   )
			}
		}
		when (selectedTab)
		{
			0 ->
			{
				val surahListState = remember { viewModel.surahState }.collectAsState()
				SurahList(
						onNavigateToAyatScreen = onNavigateToAyatScreen ,
						state = surahListState
						 )
			}

			1 ->
			{
				val juzListState = remember { viewModel.juzState }.collectAsState()
				JuzList(
						onNavigateToAyatScreen = onNavigateToAyatScreen ,
						state = juzListState ,
					   )
			}
		}
	}
}