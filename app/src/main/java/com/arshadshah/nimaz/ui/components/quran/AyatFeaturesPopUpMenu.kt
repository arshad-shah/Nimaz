package com.arshadshah.nimaz.ui.components.quran

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.viewModel.QuranViewModel
import kotlin.reflect.KFunction1

@Composable
fun AyatFeaturesPopUpMenu(
	aya : Aya ,
	isBookMarkedVerse : MutableState<Boolean> ,
	isFavouredVerse : MutableState<Boolean> ,
	hasNote : MutableState<Boolean> ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	showNoteDialog : MutableState<Boolean> ,
	noteContent : MutableState<String> ,
	popUpOpen : MutableState<Boolean> ,
	onDownloadClicked : () -> Unit ,
						 )
{

	val context = LocalContext.current
	//popup menu
	Popup(
			 onDismissRequest = {
				 popUpOpen.value = false
			 } ,
			 alignment = Alignment.BottomEnd ,
			 offset = IntOffset(0 , - 150) ,
		 ) {
		ElevatedCard(
				 shape = MaterialTheme.shapes.extraLarge ,
				 elevation = CardDefaults.elevatedCardElevation(
						  defaultElevation = 8.dp ,
															   )
					) {
			Row(
					 modifier = Modifier
						 .padding(8.dp) ,
					 verticalAlignment = Alignment.CenterVertically
			   ) {

				OutlinedIconButton(
						 modifier = Modifier.size(52.dp) ,
						 border = BorderStroke(1.dp , MaterialTheme.colorScheme.primary) ,
						 onClick = {
							 isBookMarkedVerse.value = ! isBookMarkedVerse.value
							 aya.bookmark = isBookMarkedVerse.value
							 handleEvents(
									  QuranViewModel.AyaEvent.BookmarkAya(
											   aya.ayaNumber ,
											   aya.suraNumber ,
											   aya.ayaNumberInSurah ,
											   isBookMarkedVerse.value
																		 )
										 )
							 popUpOpen.value = false
						 }
								  ) {
					Icon(
							 painter = painterResource(id = R.drawable.bookmark_icon) ,
							 contentDescription = "Bookmark" ,
							 tint = MaterialTheme.colorScheme.primary ,
							 modifier = Modifier
								 .size(24.dp)
								 .padding(2.dp) ,
						)
				}
				Spacer(modifier = Modifier.width(8.dp))
				OutlinedIconButton(
						 modifier = Modifier.size(52.dp) ,
						 border = BorderStroke(1.dp , MaterialTheme.colorScheme.primary) ,
						 onClick = {
							 isFavouredVerse.value = ! isFavouredVerse.value
							 aya.favorite = isFavouredVerse.value
							 handleEvents(
									  QuranViewModel.AyaEvent.FavoriteAya(
											   aya.ayaNumber ,
											   aya.suraNumber ,
											   aya.ayaNumberInSurah ,
											   isFavouredVerse.value
																		 )
										 )
							 popUpOpen.value = false
						 } ,
						 enabled = true ,
								  ) {
					Icon(
							 painter = painterResource(id = R.drawable.favorite_icon) ,
							 contentDescription = "Favourite" ,
							 tint = MaterialTheme.colorScheme.primary ,
							 modifier = Modifier
								 .size(24.dp)
								 .padding(2.dp) ,
						)
				}


				Spacer(modifier = Modifier.width(8.dp))
				OutlinedIconButton(
						 modifier = Modifier.size(52.dp) ,
						 border = BorderStroke(1.dp , MaterialTheme.colorScheme.primary) ,
						 onClick = {
							 showNoteDialog.value = ! showNoteDialog.value
							 popUpOpen.value = false
						 } ,
						 enabled = true ,
								  ) {
					Icon(
							 modifier = Modifier
								 .size(24.dp)
								 .padding(2.dp) ,
							 painter = painterResource(id = R.drawable.edit_note_icon) ,
							 tint = MaterialTheme.colorScheme.primary ,
							 contentDescription = "Add Note" ,
						)
				}

				Spacer(modifier = Modifier.width(8.dp))
				OutlinedIconButton(
						 modifier = Modifier.size(52.dp) ,
						 border = BorderStroke(1.dp , MaterialTheme.colorScheme.primary) ,
						 onClick = {
							 //share the aya
							 val shareIntent = Intent(Intent.ACTION_SEND)
							 shareIntent.type = "text/plain"
							 //create the share message
							 //with the aya text, aya translation
							 //the sura number followed by the aya number
							 shareIntent.putExtra(
									  Intent.EXTRA_TEXT ,
									  "Chapter ${aya.suraNumber}: Verse ${aya.ayaNumberInSurah}\n\n${aya.ayaArabic}\n${aya.ayaTranslationEnglish}"
												 )

							 //start the share intent
							 context.startActivity(Intent.createChooser(shareIntent , "Share Aya"))
						 } ,
						 enabled = true ,
								  ) {
					Icon(
							 modifier = Modifier
								 .size(24.dp)
								 .padding(2.dp) ,
							 painter = painterResource(id = R.drawable.share_icon) ,
							 tint = MaterialTheme.colorScheme.primary ,
							 contentDescription = "Share aya" ,
						)
				}

				if (aya.audioFileLocation.isEmpty())
				{
					Spacer(modifier = Modifier.width(8.dp))
					OutlinedIconButton(
							 modifier = Modifier.size(52.dp) ,
							 border = BorderStroke(1.dp , MaterialTheme.colorScheme.primary) ,
							 onClick = {
								 onDownloadClicked()
								 popUpOpen.value = false
							 } ,
							 enabled = true ,
									  ) {
						Icon(
								 modifier = Modifier
									 .size(24.dp) ,
								 painter = painterResource(id = R.drawable.download_icon) ,
								 tint = MaterialTheme.colorScheme.primary ,
								 contentDescription = "Play ayah" ,
							)
					}
				}
			}
		}
	}
}