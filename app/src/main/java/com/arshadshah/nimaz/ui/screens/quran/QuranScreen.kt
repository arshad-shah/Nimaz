package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
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
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.JuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.SurahList
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun QuranScreen(
	paddingValues : PaddingValues ,
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
			   )
{
	val viewModel = QuranViewModel(LocalContext.current)


	val sharedPreferences = PrivateSharedPreferences(LocalContext.current)

	val fontArabic = sharedPreferences.getDataFloat(
			AppConstants.ARABIC_FONT_SIZE
												   )
	val fontTranslation = sharedPreferences.getDataFloat(
			AppConstants.TRANSLATION_FONT_SIZE
														)
	val fontStyle = sharedPreferences.getData(
			AppConstants.FONT_STYLE ,
			"Default"
											 )


	if (fontArabic == 0f)
	{
		sharedPreferences.saveDataFloat(
				customkey = AppConstants.ARABIC_FONT_SIZE ,
				data = 24f
									   )
	} else if (fontTranslation == 0f)
	{
		sharedPreferences.saveDataFloat(
				customkey = AppConstants.TRANSLATION_FONT_SIZE ,
				data = 16f
									   )
	} else
	{
		sharedPreferences.saveDataFloat(
				customkey = AppConstants.ARABIC_FONT_SIZE ,
				data = fontArabic
									   )
		sharedPreferences.saveDataFloat(
				customkey = AppConstants.TRANSLATION_FONT_SIZE ,
				data = fontTranslation
									   )

		sharedPreferences.saveData(
				customkey = AppConstants.FONT_STYLE ,
				data = fontStyle
								  )
	}

	//save the state of the tab
	val (selectedTab , setSelectedTab) = rememberSaveable { mutableStateOf(0) }
//	val titles = listOf("Sura" , "Juz", "My Quran")
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
				Log.d(AppConstants.QURAN_SURAH_SCREEN_TAG , "Surah Screen")
				val surahListState = remember { viewModel.surahState }.collectAsState()
				Log.d(
						AppConstants.QURAN_SURAH_SCREEN_TAG ,
						"surahListState.value = ${surahListState.value}"
					 )
				SurahList(
						onNavigateToAyatScreen = onNavigateToAyatScreen ,
						state = surahListState
						 )
			}

			1 ->
			{
				Log.d(AppConstants.QURAN_JUZ_SCREEN_TAG , "Juz Screen")
				val juzListState = remember { viewModel.juzState }.collectAsState()
				Log.d(
						AppConstants.QURAN_JUZ_SCREEN_TAG ,
						"juzListState.value = ${juzListState.value}"
					 )
				JuzList(
						onNavigateToAyatScreen = onNavigateToAyatScreen ,
						state = juzListState ,
					   )
			}
//			2 ->
//			{
//				val bookmarks = viewModel.bookmarks
//				val favorites = viewModel.favorites
//				val notes = viewModel.notes
//				MyQuranScreen(
//						bookmarks = bookmarks ,
//						favorites = favorites ,
//						notes = notes ,
//						onNavigateToAyatScreen = onNavigateToAyatScreen ,
//						handleEvents = viewModel::handleAyaEvent
//							 )
//			}
		}
	}
}