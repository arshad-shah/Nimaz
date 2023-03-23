package com.arshadshah.nimaz.ui.components.bLogic.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.quran.NoteInput
import com.arshadshah.nimaz.utils.LocalDataStore
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import kotlin.reflect.KFunction1

@Composable
fun AyatFeatures(
	isBookMarkedVerse : MutableState<Boolean> ,
	isFavouredVerse : MutableState<Boolean> ,
	hasNote : MutableState<Boolean> ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	aya : Aya ,
	showNoteDialog : MutableState<Boolean> ,
	noteContent : MutableState<String> ,
	isLoading : Boolean ,
				)
{
	Row(
			horizontalArrangement = Arrangement.SpaceBetween ,
			verticalAlignment = Alignment.CenterVertically ,
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
						.placeholder(
								visible = isLoading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									)
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
						.placeholder(
								visible = isLoading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									)
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
						.placeholder(
								visible = isLoading,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									)
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
					//update the note in the aya object if the note is not empty
					hasNote.value = noteContent.value.isNotEmpty()

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

@Preview
@Composable
fun AyatFeaturesPreview()
{

	val viewModel = QuranViewModel(LocalContext.current)
	LocalDataStore.init(LocalContext.current)
	//create a dummy aya
	val aya = Aya(
			ayaNumber = 1 ,
			ayaNumberInQuran = 1 ,
			ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ" ,
			ayaTranslationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful." ,
			ayaTranslationUrdu = "اللہ کا نام سے، جو بہت مہربان ہے اور جو بہت مہربان ہے" ,
			audioFileLocation = "https://download.quranicaudio.com/quran/abdulbasitmurattal/001.mp3" ,
			ayaNumberInSurah = 1 ,
			bookmark = true ,
			favorite = true ,
			note = "dsfhsdhsgdfhstghs" ,
			juzNumber = 1 ,
			suraNumber = 1 ,
			ruku = 1 ,
			sajda = false ,
			sajdaType = "" ,
				 )

	AyatFeatures(
			isBookMarkedVerse = remember { mutableStateOf(aya.bookmark) } ,
			isFavouredVerse = remember { mutableStateOf(aya.favorite) } ,
			hasNote = remember { mutableStateOf(aya.note.isNotEmpty()) } ,
			handleEvents = viewModel::handleAyaEvent ,
			aya = aya ,
			showNoteDialog = remember { mutableStateOf(false) } ,
			noteContent = remember { mutableStateOf("") } ,
			isLoading = false ,
				)
}