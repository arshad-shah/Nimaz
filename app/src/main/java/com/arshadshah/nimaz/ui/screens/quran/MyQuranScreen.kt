package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.FeatureDropdownItem
import com.arshadshah.nimaz.ui.components.ui.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.ui.trackers.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuranScreen(
	bookmarks : State<List<Aya>> ,
	favorites : State<List<Aya>> ,
	notes : State<List<Aya>> ,
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
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

	LazyColumn(
			modifier = Modifier
				.testTag("MyQuranScreen")
				.fillMaxSize() ,
			userScrollEnabled = true ,
			  ) {
		item {
			FeaturesDropDown(
					label = "Bookmarks" ,
					items = bookmarks.value ,
					dropDownItem = { bookmark ->
						val currentItem = rememberUpdatedState(newValue = bookmark)
						val dismissState = rememberDismissState(
								confirmValueChange = {
									if (it == DismissValue.DismissedToStart)
									{
										handleEvents(
												QuranViewModel.AyaEvent.deleteBookmarkFromAya(
														currentItem.value.ayaNumber ,
														currentItem.value.suraNumber ,
														currentItem.value.ayaNumberInSurah
																							 )
													)
									}
									false
								}
															   )

						SwipeToDismiss(
								directions = setOf(DismissDirection.EndToStart) ,
								state = dismissState ,
								background = {
									SwipeBackground(dismissState = dismissState)
								} ,
								dismissContent = {
									FeatureDropdownItem(
											item = bookmark ,
											onClick = { aya ->
												onNavigateToAyatScreen(
														aya.suraNumber.toString() ,
														true ,
														translation ,
														aya.ayaNumberInSurah
																	  )
											} ,
											itemContent = { aya ->
												//the text
												Text(
														modifier = Modifier
															.padding(8.dp) ,
														text = "Chapter " + aya.suraNumber.toString() + ":" + " Verse " + aya.ayaNumber.toString() ,
														textAlign = TextAlign.Start ,
														maxLines = 2 ,
														overflow = TextOverflow.Ellipsis ,
														style = MaterialTheme.typography.bodyLarge
													)
											}
													   )
								})
					}
							)
		}
		item {
			FeaturesDropDown(
					label = "Favorites" ,
					items = favorites.value ,
					dropDownItem = { favourite ->
						val currentItem = rememberUpdatedState(newValue = favourite)
						val dismissState = rememberDismissState(
								confirmValueChange = {
									if (it == DismissValue.DismissedToStart)
									{
										handleEvents(
												QuranViewModel.AyaEvent.deleteFavoriteFromAya(
														currentItem.value.ayaNumber ,
														currentItem.value.suraNumber ,
														currentItem.value.ayaNumberInSurah
																							 )
													)
									}
									false
								}
															   )

						SwipeToDismiss(
								directions = setOf(DismissDirection.EndToStart) ,
								state = dismissState ,
								background = {
									SwipeBackground(dismissState = dismissState)
								} ,
								dismissContent = {
									FeatureDropdownItem(
											item = favourite ,
											onClick = { aya ->
												onNavigateToAyatScreen(
														aya.suraNumber.toString() ,
														true ,
														translation ,
														aya.ayaNumberInSurah
																	  )
											} ,
											itemContent = { aya ->
												//the text
												Text(
														modifier = Modifier
															.padding(8.dp) ,
														text = "Chapter " + aya.suraNumber.toString() + ":" + "Verse " + aya.ayaNumber.toString() ,
														textAlign = TextAlign.Start ,
														maxLines = 2 ,
														overflow = TextOverflow.Ellipsis ,
														style = MaterialTheme.typography.bodyLarge
													)
											} ,
													   )
								})
					}
							)
		}
		item {
			FeaturesDropDown(
					label = "Notes" ,
					items = notes.value ,
					dropDownItem = { note ->
						val currentItem = rememberUpdatedState(newValue = note)
						val dismissState = rememberDismissState(
								confirmValueChange = {
									if (it == DismissValue.DismissedToStart)
									{
										handleEvents(
												QuranViewModel.AyaEvent.deleteNoteFromAya(
														currentItem.value.ayaNumber ,
														currentItem.value.suraNumber ,
														currentItem.value.ayaNumberInSurah
																						 )
													)
									}
									false
								}
															   )

						SwipeToDismiss(
								directions = setOf(DismissDirection.EndToStart) ,
								state = dismissState ,
								background = {
									SwipeBackground(dismissState = dismissState)
								} ,
								dismissContent = {
									FeatureDropdownItem(
											item = note ,
											onClick = { aya ->
												onNavigateToAyatScreen(
														aya.suraNumber.toString() ,
														true ,
														translation ,
														aya.ayaNumberInSurah
																	  )
											} ,
											itemContent = { aya ->
												//the text
												Text(
														modifier = Modifier
															.padding(8.dp) ,
														text = "Chapter " + aya.suraNumber.toString() + ":" + "Verse " + aya.ayaNumber.toString() ,
														textAlign = TextAlign.Start ,
														maxLines = 2 ,
														overflow = TextOverflow.Ellipsis ,
														style = MaterialTheme.typography.bodyLarge
													)
											} ,
													   )
								})
					}
							)
		}
	}
}