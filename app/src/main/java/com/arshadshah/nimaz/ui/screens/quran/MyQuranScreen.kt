package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuranScreen(
	bookmarks : LiveData<List<Aya>> ,
	favorites : LiveData<List<Aya>> ,
	notes : LiveData<List<Aya>> ,
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
				 )
{

	//execute the code below when the screen is loaded
	LaunchedEffect(Unit)
	{
		handleEvents(QuranViewModel.AyaEvent.getBookmarks)
		handleEvents(QuranViewModel.AyaEvent.getFavorites)
		handleEvents(QuranViewModel.AyaEvent.getNotes)
	}

	val bookMarkDropDown = remember { mutableStateOf(false) }
	val favoriteDropDown = remember { mutableStateOf(false) }
	val noteDropDown = remember { mutableStateOf(false) }

	val translationType =
		PrivateSharedPreferences(LocalContext.current).getData(
				key = AppConstants.TRANSLATION_LANGUAGE ,
				s = "English"
															  )
	val translation = when (translationType)
	{
		"English" -> "english"
		"Urdu" -> "urdu"
		else -> "english"
	}

	Column {
		//three dropdowns for bookmarks, favorites, and notes
		//with a list of ayas in each
		DropdownMenu(
				expanded = bookMarkDropDown.value ,
				onDismissRequest = { bookMarkDropDown.value = false }
					)
		{
			//list of bookmarks
			LazyColumn {
				items(bookmarks.value !!)
				{
					//item
					ElevatedCard(
							onClick = {
								onNavigateToAyatScreen(
										it.suraNumber.toString() ,
										true ,
										translation
													  )
							}
								)
					{
						//content
						Text(text = it.ayaArabic)
					}
				}
			}
		}
	}
}