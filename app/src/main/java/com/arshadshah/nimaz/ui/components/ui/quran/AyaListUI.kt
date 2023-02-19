package com.arshadshah.nimaz.ui.components.ui.quran

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.repositories.SpacesFileRepository
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.theme.*
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.reflect.KFunction1


@Composable
fun AyaListUI(
	ayaList : ArrayList<Aya> ,
	paddingValues : PaddingValues ,
	language : String ,
	loading : Boolean ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	noteState : LiveData<String> ,
	type : String ,
	number : Int ,
			 )
{

	val spacesFileRepository = SpacesFileRepository(LocalContext.current)

	//media player
	val mediaPlayer = remember {
		MediaPlayer()
	}

	val lifecycle = LocalLifecycleOwner.current.lifecycle

	DisposableEffect(lifecycle) {
		val observer = LifecycleEventObserver { _ , event ->
			when
			{
				event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY || event == Lifecycle.Event.ON_PAUSE ->
				{
					mediaPlayer.release()
				}
			}
		}

		lifecycle.addObserver(observer)
		onDispose {
			lifecycle.removeObserver(observer)
		}
	}
	if (loading)
	{
		LazyColumn(contentPadding = paddingValues) {
			items(ayaList.size) { index ->
				AyaListItemUI(
						loading = loading ,
						handleEvents = handleEvents ,
						aya = ayaList[index] ,
						noteState = noteState ,
						spacesFileRepository = spacesFileRepository ,
						mediaPlayer = mediaPlayer
							 )
			}
		}
	} else
	{
		//if a new item is viewed, then scroll to that item
		val sharedPref = LocalContext.current.getSharedPreferences("quran" , 0)
		val listState = rememberLazyListState()
		val visibleItemIndex =
			remember {
				mutableStateOf(
						sharedPref.getInt(
								"visibleItemIndex-${type}-${number}" ,
								- 1
										 )
							  )
			}

		//when we close the app, we want to save the index of the last item viewed so that we can scroll to it when we open the app again
		LaunchedEffect(listState.firstVisibleItemIndex)
		{
			sharedPref.edit()
				.putInt("visibleItemIndex-${type}-${number}" , listState.firstVisibleItemIndex)
				.apply()
		}

		//when we reopen the app, we want to scroll to the last item viewed
		LaunchedEffect(visibleItemIndex.value)
		{
			if (visibleItemIndex.value != - 1)
			{
				listState.scrollToItem(visibleItemIndex.value)
				//set the value back to -1 so that we don't scroll to the same item again
				visibleItemIndex.value = - 1
			}
		}

		LazyColumn(userScrollEnabled = true , contentPadding = paddingValues , state = listState) {
			items(ayaList.size) { index ->
				AyaListItemUI(
						loading = loading ,
						handleEvents = handleEvents ,
						aya = ayaList[index] ,
						noteState = noteState ,
						spacesFileRepository = spacesFileRepository ,
						mediaPlayer = mediaPlayer
							 )
			}
		}
	}
}

@Composable
fun AyaListItemUI(
	loading : Boolean ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	aya : Aya ,
	noteState : LiveData<String> ,
	spacesFileRepository : SpacesFileRepository ,
	mediaPlayer : MediaPlayer ,
				 )
{

	val context = LocalContext.current

	//create a new speech recognizer
//	val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(LocalContext.current)
	val isLoading = remember {
		mutableStateOf(false)
	}

	val error = remember {
		mutableStateOf("")
	}

	val isBookMarkedVerse = remember {
		mutableStateOf(aya.bookmark)
	}

	val isFavoured = remember {
		mutableStateOf(aya.favorite)
	}

	val hasNote = remember {
		mutableStateOf(aya.note.isNotBlank())
	}

	val noteContent = remember {
		mutableStateOf(aya.note)
	}

	val popUpOpen = remember {
		mutableStateOf(false)
	}

	val showNoteDialog = remember {
		mutableStateOf(false)
	}

	val duration = remember {
		mutableStateOf(0)
	}

	//isDownloaded is used to show a progress bar when the user clicks on the download button
	val isDownloaded = remember {
		mutableStateOf(false)
	}
	val progressOfDownload = remember {
		mutableStateOf(0f)
	}
	val isPlaying = remember {
		mutableStateOf(false)
	}
	val isPaused = remember {
		mutableStateOf(false)
	}
	val isStopped = remember {
		mutableStateOf(false)
	}

	val fileToBePlayed = remember {
		mutableStateOf<File?>(File(aya.audioFileLocation))
	}

	val hasAudio = remember {
		mutableStateOf(aya.audioFileLocation.isNotEmpty())
	}

	//callback fro the download progress
	//callback: (File?, Exception?, progress:Int, completed: Boolean) -> Unit)
	val downloadCallback =
		{ file : File? , exception : Exception? , progress : Int , completed : Boolean ->
			if (exception != null)
			{
				isDownloaded.value = false
				progressOfDownload.value = 0f
				fileToBePlayed.value = null
				error.value = exception.message.toString()
			}
			if (completed)
			{
				isDownloaded.value = true
				progressOfDownload.value = 100f
				fileToBePlayed.value = file
				aya.audioFileLocation = file?.absolutePath.toString()
				handleEvents(
						QuranViewModel.AyaEvent.addAudioToAya(
								aya.suraNumber ,
								aya.ayaNumberInSurah ,
								aya.audioFileLocation
															 )
							)
			} else
			{
				isDownloaded.value = false
				progressOfDownload.value = progress.toFloat()
				fileToBePlayed.value = null
			}
		}

	//play the file
	fun playFile()
	{
		if (fileToBePlayed.value != null)
		{
			mediaPlayer.reset()
			val uri = Uri.fromFile(fileToBePlayed.value)
			mediaPlayer.setAudioAttributes(
					AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.build()
										  )
			mediaPlayer.setDataSource(uri.toString())
			mediaPlayer.prepare()
			mediaPlayer.start()
			duration.value = mediaPlayer.duration
			isPlaying.value = true
			isPaused.value = false
			isStopped.value = false
		}
	}

	//pause the file
	fun pauseFile()
	{
		if (mediaPlayer.isPlaying)
		{
			mediaPlayer.pause()
			isPlaying.value = false
			isPaused.value = true
			isStopped.value = false
		}
	}

	//stop the file
	fun stopFile()
	{
		if (mediaPlayer.isPlaying)
		{
			mediaPlayer.stop()
			mediaPlayer.reset()
			isPlaying.value = false
			isPaused.value = false
			isStopped.value = true
		}
	}

	//function to check if the file is downloaded already
	fun checkIfFileIsDownloaded()
	{
		if (hasAudio.value)
		{
			isDownloaded.value = true
			fileToBePlayed.value = File(aya.audioFileLocation)
			playFile()
		} else
		{
			isDownloaded.value = false
			fileToBePlayed.value = null
			spacesFileRepository.downloadAyaFile(
					aya.suraNumber ,
					aya.ayaNumberInSurah ,
					downloadCallback
												)
		}
	}

	mediaPlayer.setOnCompletionListener {
		isPlaying.value = false
		isPaused.value = false
		isStopped.value = true
		mediaPlayer.stop()
		mediaPlayer.reset()
	}

	val cardBackgroundColor = if (aya.ayaNumber == 0)
	{
		MaterialTheme.colorScheme.outline
	} else
	{
		//use default color
		MaterialTheme.colorScheme.surface
	}

	//get font size from shared preferences#
	val sharedPreferences = PrivateSharedPreferences(context)
	val arabicFontSize = sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE)
	val translationFontSize = sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE)
	val fontStyle = sharedPreferences.getData(AppConstants.FONT_STYLE , "Default")
	//get the translation type from shared preferences
	val translationType =
		PrivateSharedPreferences(context).getData(
				key = AppConstants.TRANSLATION_LANGUAGE ,
				s = "English"
												 )

	ElevatedCard(
			modifier = Modifier
				.padding(4.dp)
				.fillMaxHeight()
				.fillMaxWidth()
				.border(2.dp , cardBackgroundColor , RoundedCornerShape(8.dp)) ,
			shape = RoundedCornerShape(8.dp)
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp)
		   ) {


			Column(
					modifier = Modifier
						.weight(0.90f)
				  ) {
				SelectionContainer {
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
						Text(
								text = aya.ayaArabic ,
								style = MaterialTheme.typography.titleLarge ,
								fontSize = if (arabicFontSize == 0.0f) 24.sp else arabicFontSize.sp ,
								fontFamily = when (fontStyle)
								{
									"Default" ->
									{
										utmaniQuranFont
									}

									"Quranme" ->
									{
										quranFont
									}

									"Hidayat" ->
									{
										hidayat
									}

									"Amiri" ->
									{
										amiri
									}

									else ->
									{
										utmaniQuranFont
									}
								} ,
								textAlign = if (aya.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
								modifier = Modifier
									.fillMaxWidth()
									.padding(4.dp)
									.placeholder(
											visible = isLoading.value ,
											color = MaterialTheme.colorScheme.outline ,
											shape = RoundedCornerShape(4.dp) ,
											highlight = PlaceholderHighlight.shimmer(
													highlightColor = Color.White ,
																					)
												)
							)
					}
				}
				Spacer(modifier = Modifier.height(4.dp))
				if (translationType == "Urdu")
				{
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
						Text(
								text = "${aya.ayaTranslationUrdu} ۔" ,
								style = MaterialTheme.typography.titleSmall ,
								fontSize = if (translationFontSize == 0.0f) 16.sp else translationFontSize.sp ,
								fontFamily = urduFont ,
								textAlign = if (aya.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 4.dp)
									.placeholder(
											visible = isLoading.value ,
											color = MaterialTheme.colorScheme.outline ,
											shape = RoundedCornerShape(4.dp) ,
											highlight = PlaceholderHighlight.shimmer(
													highlightColor = Color.White ,
																					)
												)
							)
					}
				}
				if (translationType == "English")
				{
					Text(
							text = aya.ayaTranslationEnglish ,
							style = MaterialTheme.typography.bodySmall ,
							fontSize = translationFontSize.sp ,
							textAlign = if (aya.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 4.dp)
								.placeholder(
										visible = isLoading.value ,
										color = MaterialTheme.colorScheme.outline ,
										shape = RoundedCornerShape(4.dp) ,
										highlight = PlaceholderHighlight.shimmer(
												highlightColor = Color.White ,
																				)
											)
						)
				}
				/***************************************************************************/
				//row for the features of the aya when they are invoked
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

					if (isFavoured.value)
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
				//end of row for the features of the aya when they are invoked
				/***************************************************************************/

				//more menu button that opens a popup menu
				if (aya.ayaNumberInQuran != 0)
				{
					//a button that opens a popup menu
					IconButton(
							onClick = {
								popUpOpen.value = ! popUpOpen.value
							} ,
							enabled = true ,
							modifier = Modifier
								.align(Alignment.End)
							  ) {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.more_menu_icon) ,
								contentDescription = "More Menu" ,
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

				//a linear progress to show the audio player progress
				if (isPlaying.value)
				{

					//get seconds from the duration
					val seconds = duration.value / 1000f

					//every second update the progress until seconds is reached
					val currentProgress = remember { mutableStateOf(0f) }
					LaunchedEffect(key1 = isPlaying.value) {
						//start the timer
						launch {
							while (currentProgress.value < seconds)
							{
								delay(100)
								currentProgress.value += 0.1f
								//when the progress reaches the duration stop the timer
								if (currentProgress.value >= seconds)
								{
									isPlaying.value = false
									cancel(
											cause = CancellationException(
													"Audio finished playing"
																		 )
										  )
								}
							}
						}
					}
					//log the current progress
					Log.d("AyaListItemUI" , "current progress is $currentProgress")
					LinearProgressIndicator(
							progress = currentProgress.value / seconds ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(4.dp)
										   )
				}


				if (isDownloaded.value || hasAudio.value)
				{
					//a row to show th play button and the audio player
					Row(
							horizontalArrangement = Arrangement.SpaceBetween ,
							verticalAlignment = Alignment.CenterVertically ,
							modifier = Modifier
								.padding(4.dp)
					   ) {
						if (isPaused.value || isStopped.value || ! isPlaying.value)
						{
							//play and pause button
							IconButton(
									onClick = {
										playFile()
									} ,
									enabled = true ,
									modifier = Modifier
										.align(Alignment.CenterVertically)
									  ) {
								Icon(
										painter = painterResource(id = R.drawable.play_icon) ,
										contentDescription = "Play" ,
										tint = MaterialTheme.colorScheme.primary ,
										modifier = Modifier
											.size(24.dp)
											.padding(4.dp)
									)
							}
						}

						if (isPlaying.value)
						{
							//play and puase button
							IconButton(
									onClick = {
										pauseFile()
									} ,
									enabled = true ,
									modifier = Modifier
										.align(Alignment.CenterVertically)
									  ) {
								Icon(
										painter = painterResource(id = R.drawable.pause_icon) ,
										contentDescription = "Pause" ,
										tint = MaterialTheme.colorScheme.primary ,
										modifier = Modifier
											.size(24.dp)
											.padding(4.dp)
									)
							}

							//stop button
							IconButton(
									onClick = {
										stopFile()
									} ,
									enabled = true ,
									modifier = Modifier
										.align(Alignment.CenterVertically)
									  ) {
								Icon(
										painter = painterResource(id = R.drawable.stop_icon) ,
										contentDescription = "Stop" ,
										tint = MaterialTheme.colorScheme.primary ,
										modifier = Modifier
											.size(24.dp)
											.padding(4.dp)
									)
							}
						}
					}
				}

				if (popUpOpen.value)
				{
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
											isFavoured.value = ! isFavoured.value
											aya.favorite = isFavoured.value
											handleEvents(
													QuranViewModel.AyaEvent.FavoriteAya(
															aya.ayaNumber ,
															aya.suraNumber ,
															aya.ayaNumberInSurah ,
															isFavoured.value
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
												checkIfFileIsDownloaded()
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
			}
		}
	}
}