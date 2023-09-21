package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.FeatureDropdownItem
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.tasbih.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class , ExperimentalMaterialApi::class)
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

	val titleOfDialog = remember {
		mutableStateOf("")
	}
	val openDialog = remember {
		mutableStateOf(false)
	}
	val messageOfDialog = remember {
		mutableStateOf("")
	}
	val itemToDelete = remember {
		mutableStateOf<Aya?>(null)
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
								  confirmStateChange = {
									  if (it == DismissValue.DismissedToStart)
									  {
										  itemToDelete.value = currentItem.value
										  titleOfDialog.value = "Delete Bookmark"
										  messageOfDialog.value =
											  "Are you sure you want to delete this bookmark?"
										  openDialog.value = true
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
								  confirmStateChange = {
									  if (it == DismissValue.DismissedToStart)
									  {
										  itemToDelete.value = currentItem.value
										  titleOfDialog.value = "Delete Favorite"
										  messageOfDialog.value =
											  "Are you sure you want to delete this favorite?"
										  openDialog.value = true
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
								  confirmStateChange = {
									  if (it == DismissValue.DismissedToStart)
									  {
										  itemToDelete.value = currentItem.value
										  titleOfDialog.value = "Delete Note"
										  messageOfDialog.value =
											  "Are you sure you want to delete this note?"
										  openDialog.value = true
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

	if (openDialog.value)
	{
		AlertDialogNimaz(
				 topDivider = false ,
				 bottomDivider = false ,
				 contentDescription = "Ayat features dialog" ,
				 title = titleOfDialog.value ,
				 contentToShow = {
					 Text(
							  text = messageOfDialog.value ,
							  style = MaterialTheme.typography.bodyMedium ,
							  modifier = Modifier.padding(8.dp)
						 )
				 } ,
				 onDismissRequest = {
					 openDialog.value = false
				 } ,
				 contentHeight = 100.dp ,
				 confirmButtonText = "Yes" ,
				 dismissButtonText = "No, Cancel" ,
				 onConfirm = {
					 when (titleOfDialog.value)
					 {
						 "Delete Bookmark" ->
						 {
							 handleEvents(
									  QuranViewModel.AyaEvent.deleteBookmarkFromAya(
											   itemToDelete.value !!.ayaNumber ,
											   itemToDelete.value !!.suraNumber ,
											   itemToDelete.value !!.ayaNumberInSurah
																				   )
										 )
						 }

						 "Delete Favorite" ->
						 {
							 handleEvents(
									  QuranViewModel.AyaEvent.deleteFavoriteFromAya(
											   itemToDelete.value !!.ayaNumber ,
											   itemToDelete.value !!.suraNumber ,
											   itemToDelete.value !!.ayaNumberInSurah
																				   )
										 )
						 }

						 "Delete Note" ->
						 {
							 handleEvents(
									  QuranViewModel.AyaEvent.deleteNoteFromAya(
											   itemToDelete.value !!.ayaNumber ,
											   itemToDelete.value !!.suraNumber ,
											   itemToDelete.value !!.ayaNumberInSurah
																			   )
										 )
						 }
					 }
					 openDialog.value = false
				 } ,
				 onDismiss = {
					 openDialog.value = false
				 })
	}
}