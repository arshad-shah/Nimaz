package com.arshadshah.nimaz.ui.components.ui.quran

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arshadshah.nimaz.activities.QuranActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.utils.ErrorDetector
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import compose.icons.FeatherIcons
import compose.icons.feathericons.Mic
import compose.icons.feathericons.MicOff
import es.dmoral.toasty.Toasty


@Composable
fun AyaListUI(ayaList : ArrayList<Aya> , paddingValues : PaddingValues , language : String)
{

	//if a new item is viewed, then scroll to that item
	val sharedPref = LocalContext.current.getSharedPreferences("quran" , 0)
	val listState = rememberLazyListState()
	val type = ayaList[0].ayaType
	val number = ayaList[0].numberOfType
	val visibleItemIndex = remember { mutableStateOf(sharedPref.getInt("visibleItemIndex-${type}-${number}" , -1)) }

	//when we close the app, we want to save the index of the last item viewed so that we can scroll to it when we open the app again
	LaunchedEffect(listState.firstVisibleItemIndex)
	{
		sharedPref.edit().putInt("visibleItemIndex-${type}-${number}" , listState.firstVisibleItemIndex).apply()
	}

	//when we reopen the app, we want to scroll to the last item viewed
	LaunchedEffect(visibleItemIndex.value)
	{
		if (visibleItemIndex.value != -1)
		{
			listState.scrollToItem(visibleItemIndex.value)
			//set the value back to -1 so that we don't scroll to the same item again
			visibleItemIndex.value = -1
		}
	}

	LazyColumn(userScrollEnabled = true , contentPadding = paddingValues, state = listState) {
		items(ayaList.size) { index ->
			AyaListItemUI(
					ayaNumber = ayaList[index].ayaNumber.toString() ,
					ayaArabic = ayaList[index].ayaArabic ,
					ayaTranslation = ayaList[index].translation ,
					language = language
						 )
		}
	}
}

@Composable
fun AyaListItemUI(
	ayaNumber : String ,
	ayaArabic : String ,
	ayaTranslation : String ,
	language : String ,
				 )
{

	//create a new speech recognizer
	val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(LocalContext.current)

	val cardBackgroundColor = if (ayaNumber == "0")
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
	val ayaArabicState = remember { mutableStateOf(ayaArabic) }

	val isRecording = remember { mutableStateOf(false) }

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
				CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
					Text(
							text = ayaArabicState.value ,
							style = MaterialTheme.typography.titleLarge ,
							fontSize = if (arabicFontSize == 0.0f) 24.sp else arabicFontSize.sp ,
							fontFamily = quranFont ,
							textAlign = if (ayaNumber != "0") TextAlign.Justify else TextAlign.Center ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(4.dp)
						)
				}
				Spacer(modifier = Modifier.height(4.dp))
				if (language == "urdu")
				{
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
						Text(
								text = "$ayaTranslation ۔" ,
								style = MaterialTheme.typography.titleSmall ,
								fontSize = if (translationFontSize == 0.0f) 16.sp else translationFontSize.sp ,
								fontFamily = urduFont ,
								textAlign = if (ayaNumber != "0") TextAlign.Justify else TextAlign.Center ,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 4.dp)
							)
					}
				} else
				{
					Text(
							text = ayaTranslation ,
							style = MaterialTheme.typography.bodySmall ,
							fontSize = translationFontSize.sp ,
							textAlign = if (ayaNumber != "0") TextAlign.Justify else TextAlign.Center ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 4.dp)
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
fun convertAudioToText(
	context : Context ,
	speechRecognizer : SpeechRecognizer ,
	callback : (String) -> Unit ,
					  )
{
	if (ContextCompat.checkSelfPermission(
				context ,
				Manifest.permission.RECORD_AUDIO
										 ) != PackageManager.PERMISSION_GRANTED
	)
	{
		checkPermission()
	} else
	{
		//create a new speech recognizer intent
		val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
		//set the language to arabic
		speechRecognizerIntent.putExtra(
				RecognizerIntent.EXTRA_LANGUAGE_MODEL ,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
									   )
		speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE , "ar")
		//set the speech recognizer intent to the speech recognizer
		//we are using the speech recognizer to convert the audio file to text
		speechRecognizer.setRecognitionListener(object : RecognitionListener
												{
													override fun onReadyForSpeech(params : Bundle?)
													{
														Log.d("newText" , "onReadyForSpeech")
													}

													override fun onBeginningOfSpeech()
													{
														Log.d("newText" , "onBeginningOfSpeech")
													}

													override fun onRmsChanged(rmsdB : Float)
													{
													}

													override fun onBufferReceived(buffer : ByteArray?)
													{
													}

													override fun onEndOfSpeech()
													{
														Log.d("newText" , "onEndOfSpeech")
													}

													override fun onError(error : Int)
													{
														Log.d("newText" , "onError: $error")
													}

													override fun onResults(results : Bundle?)
													{

														Log.d(
																"newText" ,
																"onResults: ${
																	results?.getStringArrayList(
																			SpeechRecognizer.RESULTS_RECOGNITION
																							   )
																}"
															 )
														//get the results
														val data = results?.getStringArrayList(
																SpeechRecognizer.RESULTS_RECOGNITION
																							  )
														callback(data?.get(0) ?: "")
													}

													override fun onPartialResults(partialResults : Bundle?)
													{
														Log.d(
																"newText" ,
																"onPartialResults: $partialResults"
															 )
													}

													override fun onEvent(
														eventType : Int ,
														params : Bundle? ,
																		)
													{
														Log.d("newText" , "onEvent: $eventType")
													}
												})

		//start listening to the audio using the speech recognizer
		speechRecognizer.startListening(speechRecognizerIntent)
	}
}

private fun checkPermission()
{
	ActivityCompat.requestPermissions(
			QuranActivity() ,
			arrayOf(Manifest.permission.RECORD_AUDIO) ,
			100000
									 )
}


@Preview
@Composable
fun AyaListItemUIPreview()
{
	NimazTheme {
		//make 10 LocalAya
		val ayaList = ArrayList<Aya>()
		//add the aya to the list
		ayaList.add(
				Aya(
						0 ,
						"بسم الله الرحمن الرحيم" ,
						"In the name of Allah, the Entirely Merciful, the Especially Merciful." ,
						"Surah" ,
						1 ,
						"ENGLISH"
				   )
				   )
		ayaList.add(
				Aya(
						1 ,
						"الحمد لله رب العالمين" ,
						"All praise is due to Allah, Lord of the worlds." , "Surah" ,
						1 ,
						"ENGLISH"
				   )
				   )
		ayaList.add(
				Aya(
						2 ,
						"الرحمن الرحيم" ,
						"The Entirely Merciful, the Especially Merciful." ,
						"Surah" , 1 ,
						"ENGLISH"
				   )
				   )
		ayaList.add(
				Aya(
						3 , "مالك يوم الدين" , "Master of the Day of Judgment." , "Surah" ,
						1 ,
						"ENGLISH"
				   )
				   )
		ayaList.add(
				Aya(
						4 ,
						"إياك نعبد وإياك نستعين" ,
						"You alone do we worship, and You alone do we implore for help." , "Surah" ,
						1 ,
						"ENGLISH"
				   )
				   )
		ayaList.add(
				Aya(
						5 , "اهدنا الصراط المستقيم" , "Guide us to the straight path." , "Surah" ,
						1 ,
						"ENGLISH"
				   )
				   )
		ayaList.add(
				Aya(
						6 ,
						"صراط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين" ,
						"The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray." ,
						"Surah" ,
						1 ,
						"ENGLISH"
				   )
				   )

		AyaListUI(ayaList , PaddingValues(8.dp) , "english")
	}
}