package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_TAB
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.JuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.SurahList

@Composable
fun QuranScreen(
	paddingValues : PaddingValues ,
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
			   )
{
	val context = LocalContext.current
	val viewModel = viewModel(key = "QuranViewModel", initializer = { QuranViewModel(context) }, viewModelStoreOwner = context as ComponentActivity)

	viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Initialize_Quran)

	//save the state of the tab
	val (selectedTab , setSelectedTab) = rememberSaveable { mutableStateOf(0) }
	val titles = listOf("Sura" , "Juz", "My Quran")
	Column(modifier = Modifier
		.padding(paddingValues)
		.testTag(TEST_TAG_QURAN)) {

		TabRow(selectedTabIndex = selectedTab) {
			titles.forEachIndexed { index , title ->
				Tab(
						modifier = Modifier.testTag(TEST_TAG_QURAN_TAB.replace("{number}" , index.toString())) ,
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
				Log.d(AppConstants.QURAN_SURAH_SCREEN_TAG , "Surah Screen")
				val surahListState = remember { viewModel.surahListState }.collectAsState()
				val isLoadingSurah = remember { viewModel.loadingState }.collectAsState()
				val errorSurah = remember { viewModel.errorState }.collectAsState()
				Log.d(
						AppConstants.QURAN_SURAH_SCREEN_TAG ,
						"surahListState.value = ${surahListState.value}"
					 )
				SurahList(
						onNavigateToAyatScreen = onNavigateToAyatScreen ,
						state = surahListState ,
						loading = isLoadingSurah.value ,
						error = errorSurah.value
						 )
			}

			1 ->
			{
				Log.d(AppConstants.QURAN_JUZ_SCREEN_TAG , "Juz Screen")
				val juzListState = remember { viewModel.juzListState }.collectAsState()
				val isLoadingJuz = remember { viewModel.loadingState }.collectAsState()
				val errorJuz = remember { viewModel.errorState }.collectAsState()
				Log.d(
						AppConstants.QURAN_JUZ_SCREEN_TAG ,
						"juzListState.value = ${juzListState.value}"
					 )
				JuzList(
						onNavigateToAyatScreen = onNavigateToAyatScreen ,
						state = juzListState ,
						loading = isLoadingJuz.value ,
						error = errorJuz.value
					   )
			}
			2 ->
			{
				val bookmarks = remember { viewModel.bookmarks }.collectAsState()
				val favorites = remember { viewModel.favorites }.collectAsState()
				val notes = remember { viewModel.notes }.collectAsState()
				MyQuranScreen(
						bookmarks = bookmarks ,
						favorites = favorites ,
						notes = notes ,
						onNavigateToAyatScreen = onNavigateToAyatScreen ,
						handleEvents = viewModel::handleAyaEvent
							 )
			}
		}
	}
}