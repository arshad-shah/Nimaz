package com.arshadshah.nimaz.ui.components.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.QURAN_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.FeatureDropdownItem
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.common.Placeholder
import com.arshadshah.nimaz.ui.components.tasbih.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class , ExperimentalMaterialApi::class)
@Composable
fun DashboardQuranTracker(onNavigateToAyatScreen : (String , Boolean , String , Int) -> Unit)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			 key = QURAN_VIEWMODEL_KEY ,
			 initializer = { QuranViewModel(context) } ,
			 viewModelStoreOwner = context as ComponentActivity
							 )

	LaunchedEffect(Unit)
	{
		viewModel.handleAyaEvent(QuranViewModel.AyaEvent.getBookmarks)
	}
	val bookmarks = remember { viewModel.bookmarks }.collectAsState()

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
	if (bookmarks.value.isEmpty())
	{
		Box(
				 modifier = Modifier.clickable {
					 onNavigateToAyatScreen(1.toString() , true , translation , 1)
				 }
		   ) {
			Placeholder(nameOfDropdown = "Quran Bookmarks")
		}
	} else
	{
		FeaturesDropDown(
				 label = "Quran Bookmarks" ,
				 items = bookmarks.value ,
				 dropDownItem = { Aya ->
					 val currentItem = rememberUpdatedState(newValue = Aya)
					 val dismissState = rememberDismissState(
							  confirmStateChange = {
								  if (it == DismissValue.DismissedToStart)
								  {
									  titleOfDialog.value = "Delete Bookmark"
									  messageOfDialog.value =
										  "Are you sure you want to delete this bookmark?"
									  itemToDelete.value = currentItem.value
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
					 viewModel.handleAyaEvent(
							  QuranViewModel.AyaEvent.deleteBookmarkFromAya(
									   itemToDelete.value !!.ayaNumber ,
									   itemToDelete.value !!.suraNumber ,
									   itemToDelete.value !!.ayaNumberInSurah
																		   )
											 )
					 openDialog.value = false
				 } ,
				 onDismiss = {
					 openDialog.value = false
				 })
	}
}
