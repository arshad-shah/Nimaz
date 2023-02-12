package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bookmark
import kotlin.reflect.KFunction1


@Composable
fun AyaListUI(
	ayaList : ArrayList<Aya> ,
	paddingValues : PaddingValues ,
	language : String ,
	loading : Boolean ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	ayaState : State<QuranViewModel.AyaState> ,
			 )
{

	//map the ayaList to a mutable list so that we can change the values of the ayaList
	val listOfAya = ayaList.map { it.copy() }
	val ayaListRemembered = remember { mutableStateListOf<Aya>() }

	//swap the list
	ayaListRemembered.swapList(listOfAya)

	//function to swap the list
	//we need to do this because we want to change the values of the ayaList
	val updateList = {
		ayaListRemembered.swapList(listOfAya)
	}

	if (loading)
	{
		LazyColumn(contentPadding = paddingValues) {
			items(ayaList.size) { index ->
				AyaListItemUI(
						loading = loading ,
						handleEvents = handleEvents ,
						aya = ayaList[index] ,
						ayaState = ayaState ,
						updateList = updateList
							 )
			}
		}
	} else
	{
		//if a new item is viewed, then scroll to that item
		val sharedPref = LocalContext.current.getSharedPreferences("quran" , 0)
		val listState = rememberLazyListState()
		val type = ayaList[0].ayaType
		val number = ayaList[0].numberOfType
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
			items(ayaListRemembered.size) { index ->
				AyaListItemUI(
						loading = loading ,
						handleEvents = handleEvents ,
						aya = ayaListRemembered[index] ,
						ayaState = ayaState ,
						updateList = updateList
							 )
			}
		}
	}
}

fun <T> SnapshotStateList<T>.swapList(newList: List<T>){
	clear()
	addAll(newList)
}

@Composable
fun AyaListItemUI(
	loading : Boolean ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	aya : Aya ,
	ayaState : State<QuranViewModel.AyaState> ,
	updateList : () -> Unit ,
				 )
{

	//create a new speech recognizer
//	val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(LocalContext.current)
	val isLoading = remember {
		mutableStateOf(false)
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


	val cardBackgroundColor = if (aya.ayaNumber == 0)
	{
		MaterialTheme.colorScheme.outline
	} else
	{
		//use default color
		MaterialTheme.colorScheme.surface
	}
	val context = LocalContext.current

	//get font size from shared preferences#
	val sharedPreferences = PrivateSharedPreferences(context)
	val arabicFontSize = sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE)
	val translationFontSize = sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE)

	//mutable ayaArabic state so that we can change it when the user clicks on the mic button
	val ayaArabicState = remember { mutableStateOf(aya.ayaArabic) }

//	val isRecording = remember { mutableStateOf(false) }
	ElevatedCard(
			modifier = Modifier
				.padding(4.dp)
				.fillMaxHeight()
				.fillMaxWidth()
				.border(2.dp , cardBackgroundColor , RoundedCornerShape(8.dp))
				.clickable {
					isBookMarkedVerse.value = ! isBookMarkedVerse.value
					handleEvents(QuranViewModel.AyaEvent.BookmarkAya(aya.id, isBookMarkedVerse.value))
					updateList()
				},
			shape = RoundedCornerShape(8.dp)
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp)
		   ) {
			if (isBookMarkedVerse.value){
				Icon(
						imageVector = FeatherIcons.Bookmark ,
						contentDescription = "Bookmark" ,
						tint = MaterialTheme.colorScheme.primary ,
						modifier = Modifier
							.size(24.dp)
							.padding(4.dp)
					)
			}


			Column(
					modifier = Modifier
						.weight(0.90f)
				  ) {
				CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
					Text(
							text = ayaArabicState.value ,
							style = MaterialTheme.typography.titleLarge ,
							fontSize = if (arabicFontSize == 0.0f) 24.sp else arabicFontSize.sp ,
							fontFamily = quranFont ,
							textAlign = if (aya.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(4.dp)
								.placeholder(
										visible = loading ,
										color = MaterialTheme.colorScheme.outline ,
										shape = RoundedCornerShape(4.dp) ,
										highlight = PlaceholderHighlight.shimmer(
												highlightColor = Color.White ,
																				)
											)
						)
				}
				Spacer(modifier = Modifier.height(4.dp))
				if (aya.TranslationLanguage == "URDU")
				{
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
						Text(
								text = "${aya.ayaTranslation} ۔" ,
								style = MaterialTheme.typography.titleSmall ,
								fontSize = if (translationFontSize == 0.0f) 16.sp else translationFontSize.sp ,
								fontFamily = urduFont ,
								textAlign = if (aya.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 4.dp)
									.placeholder(
											visible = loading ,
											color = MaterialTheme.colorScheme.outline ,
											shape = RoundedCornerShape(4.dp) ,
											highlight = PlaceholderHighlight.shimmer(
													highlightColor = Color.White ,
																					)
												)
							)
					}
				}
				if (aya.TranslationLanguage == "ENGLISH")
				{
					Text(
							text = aya.ayaTranslation ,
							style = MaterialTheme.typography.bodySmall ,
							fontSize = translationFontSize.sp ,
							textAlign = if (aya.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 4.dp)
								.placeholder(
										visible = loading ,
										color = MaterialTheme.colorScheme.outline ,
										shape = RoundedCornerShape(4.dp) ,
										highlight = PlaceholderHighlight.shimmer(
												highlightColor = Color.White ,
																				)
											)
						)
				}
				//an icon button that starts recording audio in a wav file and saves it to the device storage
				//it works by holding down the button and releasing it to stop recording
				//the file is saved in the app's private storage
				//the file name is the surah number and aya number
				//button is only visible if the user has granted the app the RECORD_AUDIO permission
				//if the user has not granted the permission, the button is not visible
//				IconButton(
//						onClick = {
//							//start recording
//							val permission = ContextCompat.checkSelfPermission(
//									context ,
//									Manifest.permission.RECORD_AUDIO
//																			  )
//							if (permission == PackageManager.PERMISSION_GRANTED)
//							{
//								if (! isRecording.value)
//								{
//									convertAudioToText(context , speechRecognizer) {
//										val errors = ErrorDetector().errorDetector(ayaArabic , it)
//										Toasty.success(context , errors).show()
//										Log.d("AyaListItemUI" , errors.toString())
//										ayaArabicState.value = errors.toString()
//									}
//									Toasty.info(
//											context ,
//											"Recording started" ,
//											Toast.LENGTH_SHORT ,
//											true
//											   ).show()
//									isRecording.value = true
//								} else
//								{
//									speechRecognizer.stopListening()
//									Toasty.info(
//											context ,
//											"Recording stopped" ,
//											Toast.LENGTH_SHORT ,
//											true
//											   ).show()
//									isRecording.value = false
//								}
//							} else
//							{
//								//request permission
//								ActivityCompat.requestPermissions(
//										context as Activity ,
//										arrayOf(Manifest.permission.RECORD_AUDIO) ,
//										1
//																 )
//							}
//						} ,
//						enabled = true ,
//						modifier = Modifier
//							.padding(4.dp)
//							.align(Alignment.End)
//						  ) {
//					if (isRecording.value)
//					{
//						Icon(
//								imageVector = FeatherIcons.MicOff ,
//								contentDescription = "Stop recording" ,
//								tint = Color.Red
//							)
//					} else
//					{
//						Icon(
//								imageVector = FeatherIcons.Mic ,
//								contentDescription = "Record audio" ,
//							)
//					}
//				}
			}
		}
	}
}


//the function responsible for converting the audio file to text
//fun convertAudioToText(
//	context : Context ,
//	speechRecognizer : SpeechRecognizer ,
//	callback : (String) -> Unit ,
//					  )
//{
//	if (ContextCompat.checkSelfPermission(
//				context ,
//				Manifest.permission.RECORD_AUDIO
//										 ) != PackageManager.PERMISSION_GRANTED
//	)
//	{
//		checkPermission()
//	} else
//	{
//		//create a new speech recognizer intent
//		val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//		//set the language to arabic
//		speechRecognizerIntent.putExtra(
//				RecognizerIntent.EXTRA_LANGUAGE_MODEL ,
//				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
//									   )
//		speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE , "ar")
//		//set the speech recognizer intent to the speech recognizer
//		//we are using the speech recognizer to convert the audio file to text
//		speechRecognizer.setRecognitionListener(object : RecognitionListener
//												{
//													override fun onReadyForSpeech(params : Bundle?)
//													{
//														Log.d("newText" , "onReadyForSpeech")
//													}
//
//													override fun onBeginningOfSpeech()
//													{
//														Log.d("newText" , "onBeginningOfSpeech")
//													}
//
//													override fun onRmsChanged(rmsdB : Float)
//													{
//													}
//
//													override fun onBufferReceived(buffer : ByteArray?)
//													{
//													}
//
//													override fun onEndOfSpeech()
//													{
//														Log.d("newText" , "onEndOfSpeech")
//													}
//
//													override fun onError(error : Int)
//													{
//														Log.d("newText" , "onError: $error")
//													}
//
//													override fun onResults(results : Bundle?)
//													{
//
//														Log.d(
//																"newText" ,
//																"onResults: ${
//																	results?.getStringArrayList(
//																			SpeechRecognizer.RESULTS_RECOGNITION
//																							   )
//																}"
//															 )
//														//get the results
//														val data = results?.getStringArrayList(
//																SpeechRecognizer.RESULTS_RECOGNITION
//																							  )
//														callback(data?.get(0) ?: "")
//													}
//
//													override fun onPartialResults(partialResults : Bundle?)
//													{
//														Log.d(
//																"newText" ,
//																"onPartialResults: $partialResults"
//															 )
//													}
//
//													override fun onEvent(
//														eventType : Int ,
//														params : Bundle? ,
//																		)
//													{
//														Log.d("newText" , "onEvent: $eventType")
//													}
//												})
//
//		//start listening to the audio using the speech recognizer
//		speechRecognizer.startListening(speechRecognizerIntent)
//	}
//}
//
//private fun checkPermission()
//{
//	ActivityCompat.requestPermissions(
//			QuranActivity() ,
//			arrayOf(Manifest.permission.RECORD_AUDIO) ,
//			100000
//									 )
//}