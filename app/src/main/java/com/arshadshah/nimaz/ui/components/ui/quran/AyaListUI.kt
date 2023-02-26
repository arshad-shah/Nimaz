package com.arshadshah.nimaz.ui.components.ui.quran

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_AYA
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.repositories.SpacesFileRepository
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyatFeatures
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyatFeaturesPopUpMenu
import com.arshadshah.nimaz.ui.components.bLogic.quran.PlayerForAyat
import com.arshadshah.nimaz.ui.theme.*
import com.arshadshah.nimaz.utils.LocalDataStore
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import java.io.File


@Composable
fun AyaListUI(
	ayaList : ArrayList<Aya> ,
	paddingValues : PaddingValues ,
	language : String ,
	loading : Boolean ,
	type : String ,
	number : Int ,
	scrollToAya : Int? = null ,
			 )
{

	val context = LocalContext.current
	val viewModel = viewModel(key = "QuranViewModel" , initializer = { QuranViewModel(context) } , viewModelStoreOwner = context as ComponentActivity)
	val spaceFilesRepository = SpacesFileRepository(context)
	val arabicFontSize = remember {
		viewModel.arabic_Font_size
	}.collectAsState()
	val arabicFont = remember {
		viewModel.arabic_Font
	}.collectAsState()

	val translationFontSize = remember {
		viewModel.translation_Font_size
	}.collectAsState()

	val translation = remember {
		viewModel.translation
	}.collectAsState()


	//media player
	val mediaPlayer = remember {
		MediaPlayer()
	}

	//when we close or move to another screen, we want to clean the state o'f the AyaListItemUI so that we don't have any bugs such as dangling data
	val lifecycle = LocalLifecycleOwner.current.lifecycle

	DisposableEffect(lifecycle) {
		val observer = LifecycleEventObserver { _ , event ->
			when (event)
			{
				Lifecycle.Event.ON_STOP , Lifecycle.Event.ON_DESTROY , Lifecycle.Event.ON_PAUSE ->
				{
					mediaPlayer.release()
				}

				else ->
				{
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
						aya = ayaList[index] ,
						mediaPlayer = mediaPlayer,
						arabic_Font_size = arabicFontSize,
						arabic_Font = arabicFont,
						translation_Font_size = translationFontSize,
						translation = translation,
						spacesFileRepository = spaceFilesRepository,
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
		LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemIndex } })
		{
			sharedPref.edit()
				.putInt("visibleItemIndex-${type}-${number}" , listState.firstVisibleItemIndex)
				.apply()
		}

		//when we reopen the app, we want to scroll to the last item viewed
		LaunchedEffect(visibleItemIndex.value)
		{
			if(scrollToAya != null)
			{
				listState.animateScrollToItem(scrollToAya)
			}
			else
			{
				if (visibleItemIndex.value != - 1)
				{
					listState.animateScrollToItem(visibleItemIndex.value)
					//set the value back to -1 so that we don't scroll to the same item again
					visibleItemIndex.value = - 1
				}
			}
		}

		LazyColumn(
				modifier = Modifier.testTag(TEST_TAG_AYA),
				userScrollEnabled = true , contentPadding = paddingValues , state = listState
				  ) {
			items(ayaList.size) { index ->
				AyaListItemUI(
						aya = ayaList[index] ,
						mediaPlayer = mediaPlayer ,
						arabic_Font_size = arabicFontSize,
						arabic_Font = arabicFont,
						translation_Font_size = translationFontSize,
						translation = translation,
						spacesFileRepository = spaceFilesRepository,
							 )
			}
		}
	}
}

@Composable
fun AyaListItemUI(
	aya : Aya ,
	spacesFileRepository : SpacesFileRepository,
	mediaPlayer : MediaPlayer ,
	arabic_Font_size : State<Float> ,
	translation_Font_size : State<Float> ,
	arabic_Font : State<String> ,
	translation : State<String> ,
				 )
{
	val context = LocalContext.current
	val viewModel = viewModel(key = "QuranViewModel" , initializer = { QuranViewModel(context) } , viewModelStoreOwner = context as ComponentActivity)


	val isLoading = remember {
		mutableStateOf(false)
	}

	val error = remember {
		mutableStateOf("")
	}

	val isBookMarkedVerse = remember {
		mutableStateOf(false)
	}

	val isFavoured = remember {
		mutableStateOf(false)
	}

	val hasNote = remember {
		mutableStateOf(false)
	}

	val noteContent = remember {
		mutableStateOf("")
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
		mutableStateOf<File?>(null)
	}

	val hasAudio = remember {
		mutableStateOf(false)
	}

	isFavoured.value = aya.favorite
	isBookMarkedVerse.value = aya.bookmark
	hasNote.value = aya.note.isNotEmpty()
	hasAudio.value = aya.audioFileLocation.isNotEmpty()
	fileToBePlayed.value = File(aya.audioFileLocation)

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
				viewModel.handleAyaEvent(
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

	fun prepareMediaPlayer()
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
	}

	//play the file
	fun playFile()
	{
		//if the file isnull and there is no audio playing then prepare the media player and play the file
		//else just start the current file that is playing
		if (!isPaused.value)
		{
			prepareMediaPlayer()
			mediaPlayer.start()
			duration.value = mediaPlayer.duration
			isPlaying.value = true
			isPaused.value = false
			isStopped.value = false
		} else
		{
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
		if (!isStopped.value)
		{
			mediaPlayer.stop()
			mediaPlayer.reset()
			isPlaying.value = false
			isPaused.value = false
			isStopped.value = true
		}
	}

	//function to check if the file is downloaded already
	fun downloadFile()
	{
		isDownloaded.value = false
		fileToBePlayed.value = null
		spacesFileRepository.downloadAyaFile(
				aya.suraNumber ,
				aya.ayaNumberInSurah ,
				downloadCallback
											)
	}

	val cardBackgroundColor = if (aya.ayaNumber == 0)
	{
		MaterialTheme.colorScheme.outline
	} else
	{
		//use default color
		MaterialTheme.colorScheme.surface
	}
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
								fontSize = if (arabic_Font_size.value == 0.0f) 24.sp else arabic_Font_size.value.sp ,
								fontFamily = when (arabic_Font.value)
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
				if (translation.value == "Urdu")
				{
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
						Text(
								text = "${aya.ayaTranslationUrdu} ۔" ,
								style = MaterialTheme.typography.titleSmall ,
								fontSize = if (translation_Font_size.value == 0.0f) 16.sp else translation_Font_size.value.sp ,
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
				if (translation.value == "English")
				{
					Text(
							text = aya.ayaTranslationEnglish ,
							style = MaterialTheme.typography.bodySmall ,
							fontSize = if (translation_Font_size.value == 0.0f) 16.sp else translation_Font_size.value.sp ,
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

				Row(
						modifier = Modifier.fillMaxWidth() ,
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					AyatFeatures(
							isBookMarkedVerse = isBookMarkedVerse ,
							isFavouredVerse = isFavoured ,
							hasNote = hasNote ,
							handleEvents = viewModel::handleAyaEvent ,
							aya = aya ,
							showNoteDialog = showNoteDialog ,
							noteContent = noteContent ,
								)

					//more menu button that opens a popup menu
					if (aya.ayaNumberInQuran != 0)
					{
						//a button that opens a popup menu
						IconButton(
								onClick = {
									popUpOpen.value = ! popUpOpen.value
								} ,
								enabled = true ,
								  ) {
							Icon(
									modifier = Modifier
										.size(24.dp) ,
									painter = painterResource(id = R.drawable.more_menu_icon) ,
									contentDescription = "More Menu" ,
								)
						}
					}
				}

				if (popUpOpen.value)
				{
					AyatFeaturesPopUpMenu(
							aya = aya ,
							isBookMarkedVerse = isBookMarkedVerse ,
							isFavouredVerse = isFavoured ,
							hasNote = hasNote ,
							handleEvents = viewModel::handleAyaEvent ,
							showNoteDialog = showNoteDialog ,
							noteContent = noteContent ,
							popUpOpen = popUpOpen ,
							onDownloadClicked = { downloadFile() }
										 )
				}

				PlayerForAyat(
						isPlaying = isPlaying ,
						isPaused = isPaused ,
						isStopped = isStopped ,
						duration = duration ,
						isDownloaded = isDownloaded ,
						hasAudio = hasAudio ,
						onPlayClicked = { playFile() } ,
						onPauseClicked = { pauseFile() } ,
						onStopClicked = { stopFile() } ,
							 )
			}
		}
	}
}

//preview AyaListItemUI
@Preview(showBackground = true)
@Composable
fun AyaListItemUIPreview()
{
	NimazTheme {
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

		AyaListItemUI(
				aya = aya ,
				mediaPlayer = MediaPlayer() ,
				arabic_Font_size = remember { mutableStateOf(0.0f) } ,
				translation_Font_size = remember { mutableStateOf(0.0f) } ,
				arabic_Font = remember { mutableStateOf("Amiri") } ,
				translation = remember { mutableStateOf("English") } ,
				spacesFileRepository = SpacesFileRepository(LocalContext.current) ,
					 )
	}

}
