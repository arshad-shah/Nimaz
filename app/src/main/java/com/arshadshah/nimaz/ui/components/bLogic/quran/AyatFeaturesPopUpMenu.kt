package com.arshadshah.nimaz.ui.components.bLogic.quran

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
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
				modifier = Modifier.shadow(8.dp , RoundedCornerShape(8.dp)) ,
					) {
			Row(
					modifier = Modifier
						.padding(8.dp) ,
					verticalAlignment = Alignment.CenterVertically
			   ) {

				IconButton(
						modifier = Modifier.border(
								1.dp ,
								MaterialTheme.colorScheme.primary ,
								RoundedCornerShape(50)
												  ) ,
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
								.padding(4.dp) ,
						)
				}
				Spacer(modifier = Modifier.width(8.dp))
				IconButton(
						modifier = Modifier.border(
								1.dp ,
								MaterialTheme.colorScheme.primary ,
								RoundedCornerShape(50)
												  ) ,
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
								.padding(4.dp) ,
						)
				}


				Spacer(modifier = Modifier.width(8.dp))
				IconButton(
						modifier = Modifier.border(
								1.dp ,
								MaterialTheme.colorScheme.primary ,
								RoundedCornerShape(50)
												  ) ,
						onClick = {
							showNoteDialog.value = ! showNoteDialog.value
							popUpOpen.value = false
						} ,
						enabled = true ,
						  ) {
					Icon(
							modifier = Modifier
								.size(24.dp)
								.padding(4.dp) ,
							painter = painterResource(id = R.drawable.edit_note_icon) ,
							tint = MaterialTheme.colorScheme.primary ,
							contentDescription = "Add Note" ,
						)
				}

				Spacer(modifier = Modifier.width(8.dp))
				IconButton(
						modifier = Modifier.border(
								1.dp ,
								MaterialTheme.colorScheme.primary ,
								RoundedCornerShape(50)
												  ) ,
						onClick = {
							//share the aya
							val shareIntent = Intent(Intent.ACTION_SEND)
							shareIntent.type = "text/plain"
							shareIntent.putExtra(Intent.EXTRA_TEXT , aya.ayaArabic)
							context.startActivity(
									Intent.createChooser(
											shareIntent ,
											"Share via"
														)
												 )
						} ,
						enabled = true ,
						  ) {
					Icon(
							modifier = Modifier
								.size(24.dp)
								.padding(4.dp) ,
							painter = painterResource(id = R.drawable.share_icon) ,
							tint = MaterialTheme.colorScheme.primary ,
							contentDescription = "Share aya" ,
						)
				}

				if (aya.audioFileLocation.isEmpty())
				{
					Spacer(modifier = Modifier.width(8.dp))
					IconButton(
							modifier = Modifier.border(
									1.dp ,
									MaterialTheme.colorScheme.primary ,
									RoundedCornerShape(50)
													  ) ,
							onClick = {
								onDownloadClicked()
								popUpOpen.value = false
									  } ,
							enabled = true ,
							  ) {
						Icon(
								modifier = Modifier
									.size(24.dp)
									.padding(4.dp) ,
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