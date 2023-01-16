package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty

@Composable
fun MoreMenu(
	menuOpen : Boolean = false ,
	setMenuOpen : (Boolean) -> Unit ,
	state : SettingValueState<Int> = rememberIntSettingState() ,
			)
{

	val sharedPreferences = PrivateSharedPreferences(LocalContext.current)

	val pageTypeState =
		rememberPreferenceStringSettingState("PageType" , "List" , sharedPreferences)
	val translationState =
		rememberPreferenceStringSettingState("Translation" , "English" , sharedPreferences)

	val items1 : List<String> = listOf("List" , "Page (Experimental)")
	val items2 : List<String> = listOf("English" , "Urdu")
	val (showDialog1 , setShowDialog1) = remember { mutableStateOf(false) }
	val (showDialog2 , setShowDialog2) = remember { mutableStateOf(false) }

	//a dialog with two sliders to control the font size of quran
	val (showDialog3 , setShowDialog3) = remember { mutableStateOf(false) }
	val context = LocalContext.current
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
				} , text = { Text(text = "Font Size") })
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
					)
	} else if (showDialog2)
	{
		CustomDialog(
				title = "Translation" ,
				setShowDialog = setShowDialog2 ,
				state = state ,
				valueState = translationState ,
				items = items2 ,
					)
	} else if (showDialog3)
	{
		FontSizeDialog(setShowDialog3)
	} else
	{
		return
	}
}

@Composable
fun FontSizeDialog(showDialog3 : (Boolean) -> Unit)
{
	//a dialog with two sliders to control the font size of quran
	//after the user selects the font size then save it in shared preferences
	//and then update the state of the quran page

	val context = LocalContext.current
	val sharedPreferences = PrivateSharedPreferences(context)
	val arabicFontSizeState =
		remember {
			mutableStateOf(
					sharedPreferences.getDataFloat("ArabicFontSize")
						  )
		}
	val translationFontSizeState =
		remember {
			mutableStateOf(
					sharedPreferences.getDataFloat("TranslationFontSize")
						  )
		}
	AlertDialog(
			modifier = Modifier
				.fillMaxWidth()
				.wrapContentHeight() ,
			onDismissRequest = { showDialog3(false) } ,
			title = { Text(text = "Font Size") } ,
			text = {
				Column(
						verticalArrangement = Arrangement.spacedBy(8.dp) ,
						horizontalAlignment = Alignment.CenterHorizontally
					  ) {
					Text(text = "Arabic Font Size")
					//a slider to control the font size of arabic text
					//it needs to have markings on it with the font size
					//and the user can slide it to select the font size
					//vertical ticks on the slider
					Slider(
							value = arabicFontSizeState.value ,
							onValueChange = { arabicFontSizeState.value = it } ,
							valueRange = 20f .. 40f ,
							steps = 5 ,
							modifier = Modifier.width(300.dp)
						  )
					Text(text = "Translation Font Size")
					//a slider to control the font size of translation text
					//it needs to have markings on it with the font size
					//and the user can slide it to select the font size
					Slider(
							value = translationFontSizeState.value ,
							onValueChange = { translationFontSizeState.value = it } ,
							valueRange = 15f .. 40f ,
							steps = 5 ,
							modifier = Modifier.width(300.dp)
						  )
				}
			} ,
			confirmButton = {
				Button(
						onClick = {
							//save the font size in shared preferences
							sharedPreferences.saveDataFloat(
									"ArabicFontSize" ,
									arabicFontSizeState.value
														   )
							sharedPreferences.saveDataFloat(
									"TranslationFontSize" ,
									translationFontSizeState.value
														   )
							showDialog3(false)
						} ,
						modifier = Modifier.padding(8.dp)
					  ) {
					Text(text = "OK")
				}
			} ,
			dismissButton = {
				TextButton(
						onClick = { showDialog3(false) } ,
						modifier = Modifier.padding(8.dp)
						  ) {
					Text(text = "Cancel")
				}
			}
			   )
}

//preview the dialog
@Preview
@Composable
fun FontSizeDialogPreview()
{
	FontSizeDialog({})
}
