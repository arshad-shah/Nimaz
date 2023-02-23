package com.arshadshah.nimaz.ui.components.bLogic.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.quran.NoteInput
import kotlin.reflect.KFunction1

@Composable
fun AyatFeatures(
	isBookMarkedVerse : MutableState<Boolean> ,
	isFavouredVerse : MutableState<Boolean> ,
	hasNote : MutableState<Boolean> ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	aya : Aya ,
	showNoteDialog : MutableState<Boolean> ,
	noteContent : MutableState<String>
				)
{
	Row(
			horizontalArrangement = Arrangement.SpaceBetween ,
			verticalAlignment = Alignment.CenterVertically ,
			modifier = Modifier
				.padding(4.dp)
	   ) {
		if (isBookMarkedVerse.value)
		{
			Icon(
					painter = painterResource(id = R.drawable.bookmark_icon) ,
					contentDescription = "Bookmark" ,
					tint = MaterialTheme.colorScheme.primary ,
					modifier = Modifier
						.size(24.dp)
						.padding(4.dp)
				)
		}

		if (isFavouredVerse.value)
		{
			Icon(
					painter = painterResource(id = R.drawable.favorite_icon) ,
					contentDescription = "Favourite" ,
					tint = MaterialTheme.colorScheme.primary ,
					modifier = Modifier
						.size(24.dp)
						.padding(4.dp)
				)
		}

		if (hasNote.value)
		{
			Icon(
					painter = painterResource(id = R.drawable.note_icon) ,
					contentDescription = "Note" ,
					tint = MaterialTheme.colorScheme.primary ,
					modifier = Modifier
						.size(24.dp)
						.padding(4.dp)
						.clickable {
							handleEvents(
									QuranViewModel.AyaEvent.getNoteForAya(
											aya.ayaNumber ,
											aya.suraNumber ,
											aya.ayaNumberInSurah
																		 )
										)
							showNoteDialog.value = true
							noteContent.value = aya.note
						}
				)
		}
	}


	//the note dialog that appears when the user clicks on the note icon
	if (showNoteDialog.value)
	{
		NoteInput(
				showNoteDialog = showNoteDialog ,
				noteContent = noteContent ,
				onClick = {
					hasNote.value = ! hasNote.value
					aya.note = noteContent.value
					handleEvents(
							QuranViewModel.AyaEvent.AddNoteToAya(
									aya.ayaNumber ,
									aya.suraNumber ,
									aya.ayaNumberInSurah ,
									noteContent.value
																)
								)
					showNoteDialog.value = false
				}
				 )
	}

}