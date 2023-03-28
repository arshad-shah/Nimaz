package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.FeatureDropdownItem
import com.arshadshah.nimaz.ui.components.ui.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.ui.trackers.Placeholder
import com.arshadshah.nimaz.ui.components.ui.trackers.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardQuranTracker(onNavigateToAyatScreen : (String , Boolean , String , Int) -> Unit)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "QuranViewModel" ,
			initializer = { QuranViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
							 )
	val bookmarks = remember { viewModel.bookmarks }.collectAsState()

	LaunchedEffect(Unit)
	{
		viewModel.handleAyaEvent(QuranViewModel.AyaEvent.getBookmarks)
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

	if (bookmarks.value.isEmpty())
	{
		Placeholder(nameOfDropdown = "Quran Bookmarks")
		return
	}
	FeaturesDropDown(
			label = "Quran Bookmarks" ,
			items = bookmarks.value ,
			dropDownItem = { Aya ->
				val currentItem = rememberUpdatedState(newValue = Aya)
				val dismissState = rememberDismissState(
						confirmValueChange = {
							viewModel.handleAyaEvent(
									QuranViewModel.AyaEvent.deleteBookmarkFromAya(
											currentItem.value.ayaNumber ,
											currentItem.value.suraNumber ,
											currentItem.value.ayaNumberInSurah
																				 )
													)
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
									item = Aya ,
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
									}
											   )
						})
			}
					)
}
