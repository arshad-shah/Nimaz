package com.arshadshah.nimaz.ui.components.quran

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.QURAN_VIEWMODEL_KEY
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.ProgressBarCustom
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceFloatSettingState
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.viewModel.QuranViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MoreMenu(
	menuOpen : Boolean = false ,
	setMenuOpen : (Boolean) -> Unit ,
	state : SettingValueState<Int> = rememberIntSettingState() ,
			)
{

	val context = LocalContext.current

	val items1 : List<String> = listOf("List" , "Page (Experimental)")
	val items2 : List<String> = listOf("English" , "Urdu")
	val items3 : List<String> = listOf("Default" , "Quranme" , "Hidayat" , "Amiri" , "IndoPak")
	val (showDialog1 , setShowDialog1) = remember { mutableStateOf(false) }
	val (showDialog2 , setShowDialog2) = remember { mutableStateOf(false) }

	//a dialog with two sliders to control the font size of quran
	val (showDialog3 , setShowDialog3) = remember { mutableStateOf(false) }
	val (showDialog4 , setShowDialog4) = remember { mutableStateOf(false) }

	val viewModel = viewModel(
			 key = QURAN_VIEWMODEL_KEY ,
			 initializer = { QuranViewModel(context) } ,
			 viewModelStoreOwner = context as ComponentActivity
							 )
	//downloadButtonState
	val isDownloadButtonEnabled = remember {
		viewModel.downloadButtonState
	}.collectAsState()

	viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Initialize_Quran)

	val pageTypeState =
		rememberPreferenceStringSettingState(AppConstants.PAGE_TYPE , "List")
	val translationState =
		rememberPreferenceStringSettingState(
				 AppConstants.TRANSLATION_LANGUAGE ,
				 "English" ,
											)

	val arabicFontSizeState = rememberPreferenceFloatSettingState(
			 key = AppConstants.ARABIC_FONT_SIZE ,
			 defaultValue = 26f
																 )
	val translationFontSizeState = rememberPreferenceFloatSettingState(
			 key = AppConstants.TRANSLATION_FONT_SIZE ,
			 defaultValue = 16f
																	  )
	//font style
	val fontStyleState = rememberPreferenceStringSettingState(
			 key = AppConstants.FONT_STYLE ,
			 defaultValue = "Default" ,
															 )

	//log the initial state of the font size
	Log.d("MoreMenu" , "arabicFontSizeState.value = ${arabicFontSizeState.value}")
	Log.d("MoreMenu" , "translationFontSizeState.value = ${translationFontSizeState.value}")
	Log.d("MoreMenu" , "fontStyleState.value = ${fontStyleState.value}")
	Log.d("MoreMenu" , "isDownloadButtonEnabled.value = ${isDownloadButtonEnabled.value}")

	DropdownMenu(
			 expanded = menuOpen ,
			 onDismissRequest = { setMenuOpen(false) } ,
			 content = {
				 DropdownMenuItem(onClick = {
					 setShowDialog1(true)
					 setMenuOpen(false)
				 } , text = { Text(text = "Display Type") })
				 //disable the translation option if the page type is page
				 DropdownMenuItem(onClick = {
					 //if translation is disabled and the user clicks on the translation option
					 //then show a toast message
					 if (pageTypeState.value == "Page (Experimental)")
					 {
						 Toasty.info(
								  context ,
								  "Translation is disabled for Page View" ,
								  Toasty.LENGTH_SHORT ,
								  true
									).show()
					 } else
					 {
						 setShowDialog2(true)
						 setMenuOpen(false)
					 }
				 } , text = {
					 Text(
							  text = "Translation"
							  //if page type is page then show text color as grey
							  //else show it as black
							  ,
							  color = if (pageTypeState.value == "Page (Experimental)") Color.Gray else MaterialTheme.colorScheme.onBackground
						 )
				 })
				 DropdownMenuItem(onClick = {
					 setShowDialog3(true)
					 setMenuOpen(false)
				 } , text = { Text(text = "Font") })

				 //download quran
				 DropdownMenuItem(
						  onClick = {
							  //if isDownloadButtonEnabled.value then gray out the download button
							  //else download the quran
							  if (! isDownloadButtonEnabled.value)
							  {
								  Toasty.info(
										   context ,
										   "Quran is already downloaded" ,
										   Toasty.LENGTH_SHORT ,
										   true
											 ).show()
								  return@DropdownMenuItem
							  } else
							  {
								  //if the quran is not downloaded then download it
								  viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Download_Quran)
								  setShowDialog4(true)
								  setMenuOpen(false)
							  }
						  } ,
						  text = {
							  Text(
									   text = "Download Quran" ,
									   color = if (! isDownloadButtonEnabled.value) Color.Gray else MaterialTheme.colorScheme.onBackground ,
								  )
						  })
			 }
				)


	if (showDialog1)
	{
		CustomDialog(
				 title = "Display Type" ,
				 setShowDialog = setShowDialog1 ,
				 state = state ,
				 valueState = pageTypeState ,
				 items = items1
					) {
			viewModel.handleQuranMenuEvents(
					 QuranViewModel.QuranMenuEvents.Change_Display_Mode(
							  it
																	   )
										   )
		}
	} else if (showDialog2)
	{
		CustomDialog(
				 title = "Translation" ,
				 items = items2 ,
				 setShowDialog = setShowDialog2 ,
				 state = state ,
				 valueState = translationState
					) {
			viewModel.handleQuranMenuEvents(
					 QuranViewModel.QuranMenuEvents.Change_Translation(
							  it
																	  )
										   )
		}
	} else if (showDialog4)
	{
		val downloadState = remember {
			viewModel.downloadProgress
		}.collectAsState()
		DownloadQuranDialog(setShowDialog4 , downloadState , viewModel::handleQuranMenuEvents)
	} else if (showDialog3)
	{
		FontSizeDialog(
				 setShowDialog3 ,
				 arabicFontSizeState ,
				 translationFontSizeState ,
				 fontStyleState ,
				 items3 ,
				 viewModel::handleQuranMenuEvents
					  )
	} else
	{
		return
	}
}

@Composable
fun MoreMenuMain(
	menuOpen : Boolean = false ,
	setMenuOpen : (Boolean) -> Unit ,
				)
{

	val context = LocalContext.current
	val (showDialog4 , setShowDialog4) = remember { mutableStateOf(false) }
	val (showDialog6 , setShowDialog6) = remember { mutableStateOf(false) }

	val viewModel = viewModel(
			 key = QURAN_VIEWMODEL_KEY ,
			 initializer = { QuranViewModel(context) } ,
			 viewModelStoreOwner = context as ComponentActivity
							 )
	//downloadButtonState
	val isDownloadButtonEnabled = remember {
		viewModel.downloadButtonState
	}.collectAsState()

	viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Initialize_Quran)

	Log.d("MoreMenu" , "isDownloadButtonEnabled.value = ${isDownloadButtonEnabled.value}")

	DropdownMenu(
			 expanded = menuOpen ,
			 onDismissRequest = { setMenuOpen(false) } ,
			 content = {
				 //Reset_Quran_Data
				 DropdownMenuItem(onClick = {
					 setShowDialog6(true)
					 setMenuOpen(false)
				 } , text = { Text(text = "Reset Quran") })

				 //download quran
				 DropdownMenuItem(
						  onClick = {
							  //if isDownloadButtonEnabled.value then gray out the download button
							  //else download the quran
							  if (! isDownloadButtonEnabled.value)
							  {
								  Toasty.info(
										   context ,
										   "Quran is already downloaded" ,
										   Toasty.LENGTH_SHORT ,
										   true
											 ).show()
								  return@DropdownMenuItem
							  } else
							  {
								  //if the quran is not downloaded then download it
								  viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Download_Quran)
								  setShowDialog4(true)
								  setMenuOpen(false)
							  }
						  } ,
						  text = {
							  Text(
									   text = "Download Quran" ,
									   color = if (! isDownloadButtonEnabled.value) Color.Gray else MaterialTheme.colorScheme.onBackground ,
								  )
						  })
			 }
				)


	if (showDialog4)
	{
		val downloadState = remember {
			viewModel.downloadProgress
		}.collectAsState()
		DownloadQuranDialog(setShowDialog4 , downloadState , viewModel::handleQuranMenuEvents)
	} else if (showDialog6)
	{
		ResetQuranDataDialog(setShowDialog6 , viewModel::handleQuranMenuEvents)
	} else
	{
		return
	}
}

@Composable
fun ResetQuranDataDialog(
	showDialog6 : (Boolean) -> Unit ,
	handleEvents : (QuranViewModel.QuranMenuEvents) -> Unit ,
						)
{
	AlertDialogNimaz(
			 topDivider = false ,
			 bottomDivider = false ,
			 contentDescription = "Reset Quran Data" ,
			 contentHeight = 180.dp ,
			 title = "Reset Quran Data" ,
			 contentToShow = {
				 Text(
						  text = "Are you sure you want to reset the Quran Data?" ,
						  style = MaterialTheme.typography.bodyMedium ,
						  modifier = Modifier.padding(8.dp)
					 )
				 Text(
						  text = "This will delete all the Verses, and get fresh data from the server." ,
						  style = MaterialTheme.typography.bodySmall ,
						  modifier = Modifier.padding(8.dp)
					 )
				 Text(
						  text = "Bookmarks, Notes, Favorites, and Settings for Quran will be deleted." ,
						  style = MaterialTheme.typography.bodySmall ,
						  modifier = Modifier.padding(8.dp)
					 )
			 } ,
			 onDismissRequest = {
				 showDialog6(false)
			 } ,
			 onConfirm = {
				 handleEvents(QuranViewModel.QuranMenuEvents.Reset_Quran_Data)
				 showDialog6(false)
			 } ,
			 onDismiss = {
				 showDialog6(false)
			 } ,
			 confirmButtonText = "Yes" ,
			 dismissButtonText = "No, Cancel" ,
					)
}

@Composable
fun DownloadQuranDialog(
	showDialog4 : (Boolean) -> Unit ,
	downloadProgress : State<Int> ,
	handleEvents : (QuranViewModel.QuranMenuEvents) -> Unit ,
					   )
{

	val progress = remember { mutableStateOf(0f) }
	//every few seconds check if the download is complete
	LaunchedEffect(Unit) {
		launch {
			while (isActive)
			{
				delay(10)
				handleEvents(QuranViewModel.QuranMenuEvents.Check_Download_Progress)
				progress.value = downloadProgress.value.toFloat()
				when (progress.value)
				{
					- 1f ->
					{
						showDialog4(false)
						cancel(
								 cause = CancellationException(
										  "Download Failed"
															  )
							  )
						Log.d("Nimaz: DownloadQuranDialog" , "Download Failed")
					}

					- 2f ->
					{
						showDialog4(false)
						cancel(
								 cause = CancellationException(
										  "Download Cancelled"
															  )
							  )
						Log.d("Nimaz: DownloadQuranDialog" , "Download Cancelled")
					}

					100f ->
					{
						showDialog4(false)
						cancel(
								 cause = CancellationException(
										  "Download Complete"
															  )
							  )
						Log.d("Nimaz: DownloadQuranDialog" , "Download Complete")
					}
				}
			}
		}
	}
	AlertDialogNimaz(
			 icon = painterResource(id = R.drawable.download_icon) ,
			 topDivider = false ,
			 bottomDivider = false ,
			 contentHeight = 200.dp ,
			 contentDescription = "Download Quran" ,
			 title = "Downloading Quran" ,
			 contentToShow = {
				 Column(
						  modifier = Modifier
							  .fillMaxWidth()
							  .padding(4.dp) ,
						  verticalArrangement = Arrangement.Center ,
						  horizontalAlignment = Alignment.CenterHorizontally
					   )
				 {
					 ProgressBarCustom(
							  progress = progress.value ,
							  radius = 80.dp ,
							  waveAnimation = true ,
									  )
				 }
			 } ,
			 onDismissRequest = {
				 handleEvents(QuranViewModel.QuranMenuEvents.Cancel_Download)
				 showDialog4(false)
			 } ,
			 showDismissButton = false ,
			 confirmButtonText = "Cancel" ,
			 onConfirm = {
				 handleEvents(QuranViewModel.QuranMenuEvents.Cancel_Download)
				 showDialog4(false)
			 } ,
			 onDismiss = {

			 })
}

@Preview
@Composable
fun DownloadQuranDialogPreview()
{
	//satte for the progress bar
	val state = remember {
		derivedStateOf {
			0
		}
	}

	NimazTheme {
		DownloadQuranDialog({} , state , {})
	}
}
