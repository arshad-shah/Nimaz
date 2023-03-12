package com.arshadshah.nimaz.ui.components.ui.quran

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceFloatSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.*

@Composable
fun MoreMenu(
	menuOpen : Boolean = false ,
	setMenuOpen : (Boolean) -> Unit ,
	state : SettingValueState<Int> = rememberIntSettingState() ,
			)
{

	val context = LocalContext.current


	val sharedPreferences = PrivateSharedPreferences(context)

	val pageTypeState =
		rememberPreferenceStringSettingState(AppConstants.PAGE_TYPE , "List" , sharedPreferences)
	val translationState =
		rememberPreferenceStringSettingState(
				AppConstants.TRANSLATION_LANGUAGE ,
				"English" ,
				sharedPreferences
											)

	val items1 : List<String> = listOf("List" , "Page (Experimental)")
	val items2 : List<String> = listOf("English" , "Urdu")
	val items3 : List<String> = listOf("Default" , "Quranme" , "Hidayat" , "Amiri")
	val (showDialog1 , setShowDialog1) = remember { mutableStateOf(false) }
	val (showDialog2 , setShowDialog2) = remember { mutableStateOf(false) }

	//a dialog with two sliders to control the font size of quran
	val (showDialog3 , setShowDialog3) = remember { mutableStateOf(false) }
	val (showDialog4 , setShowDialog4) = remember { mutableStateOf(false) }


	val arabicFontSizeState = rememberPreferenceFloatSettingState(
			key = AppConstants.ARABIC_FONT_SIZE ,
			defaultValue = 24f
																 )
	val translationFontSizeState = rememberPreferenceFloatSettingState(
			key = AppConstants.TRANSLATION_FONT_SIZE ,
			defaultValue = 16f
																	  )

	//font style
	val fontStyleState = rememberPreferenceStringSettingState(
			key = AppConstants.FONT_STYLE ,
			defaultValue = "Default" ,
			sharedPreferences
															 )

	val viewModel = viewModel(
			key = "QuranViewModel" ,
			initializer = { QuranViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
							 )

	val isDownloadButtonEnabled = remember {
		viewModel.downloadButtonState
	}.collectAsState(initial = true)

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
						enabled = isDownloadButtonEnabled.value ,
						onClick = {
					viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Download_Quran)
					setShowDialog4(true)
					setMenuOpen(false)
				} , text = { Text(text = "Download Quran") })
			}
				)


	if (showDialog1)
	{
		CustomDialog(
				title = "Display Type" ,
				setShowDialog = setShowDialog1 ,
				state = state ,
				valueState = pageTypeState ,
				items = items1 ,
				onStateChange = {
					viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Change_Display_Mode(it))
				}
					)
	} else if (showDialog2)
	{
		CustomDialog(
				title = "Translation" ,
				items = items2 ,
				setShowDialog = setShowDialog2 ,
				state = state ,
				valueState = translationState ,
				onStateChange = {
					viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Change_Translation(it))
				}
					)
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
	}else if (showDialog4)
	{
		val downloadState = remember {
			viewModel.downloadProgress
		}.collectAsState()
		DownloadQuranDialog(setShowDialog4, downloadState, viewModel::handleQuranMenuEvents)
	}
	else
	{
		return
	}
}

@Composable
fun DownloadQuranDialog(
	showDialog4 : (Boolean) -> Unit ,
	downloadProgress : State<Int> ,
	handleEvents : (QuranViewModel.QuranMenuEvents) -> Unit
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
					-1f ->
					{
						showDialog4(false)
						cancel(
								cause = CancellationException(
										"Download Failed"
															 )
							  )
						Log.d("Nimaz: DownloadQuranDialog" , "Download Failed")
					}
					-2f ->
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
	//a progress bar to show the progress of the download in an alert dialog
	AlertDialog(
			icon = {
				Icon(
						painter = painterResource(id = R.drawable.download_icon) ,
						modifier = Modifier.size(24.dp),
						contentDescription = "Download Quran" ,
						tint = MaterialTheme.colorScheme.primary
					)
			},
			onDismissRequest = { showDialog4(false) } ,
			title = { Text(text = "Quran Download") } ,
			text = {
				Column {
					Row(
							modifier = Modifier.fillMaxWidth(),
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.SpaceBetween
					   ) {
						Text(text = "Downloading Quran", modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.titleMedium)
						Text(text = "${progress.value.toInt()}%", modifier = Modifier.padding(4.dp) , style = MaterialTheme.typography.titleMedium)
					}
					LinearProgressIndicator(progress = progress.value / 100f,
											modifier = Modifier
												.fillMaxWidth()
												.padding(4.dp)
												.height(8.dp) ,
											strokeCap = StrokeCap.Round ,)
				}
			} ,
			confirmButton = {
			} ,
			dismissButton = {
				Button(onClick = {
					handleEvents(QuranViewModel.QuranMenuEvents.Cancel_Download)
					showDialog4(false)
								 } , modifier = Modifier.padding(4.dp)) {
					Text(text = "Cancel Download")
				}
			}
			   )
}
