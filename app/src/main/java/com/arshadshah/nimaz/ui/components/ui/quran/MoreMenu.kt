package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceFloatSettingState
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
		rememberPreferenceStringSettingState(AppConstants.PAGE_TYPE , "List" , sharedPreferences)
	val translationState =
		rememberPreferenceStringSettingState(
				AppConstants.TRANSLATION_LANGUAGE ,
				"English" ,
				sharedPreferences
											)

	val items1 : List<String> = listOf("List" , "Page (Experimental)")
	val items2 : List<String> = listOf("English" , "Urdu")
	val (showDialog1 , setShowDialog1) = remember { mutableStateOf(false) }
	val (showDialog2 , setShowDialog2) = remember { mutableStateOf(false) }

	//a dialog with two sliders to control the font size of quran
	val (showDialog3 , setShowDialog3) = remember { mutableStateOf(false) }
	val context = LocalContext.current


	val arabicFontSizeState = rememberPreferenceFloatSettingState(
			key = AppConstants.ARABIC_FONT_SIZE ,
			defaultValue = 24f
																 )
	val translationFontSizeState = rememberPreferenceFloatSettingState(
			key = AppConstants.TRANSLATION_FONT_SIZE ,
			defaultValue = 16f
																	  )

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
		FontSizeDialog(setShowDialog3 , arabicFontSizeState , translationFontSizeState)
	} else
	{
		return
	}
}
