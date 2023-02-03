package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import kotlin.math.roundToInt


@Composable
fun FontSizeDialog(
	showDialog3 : (Boolean) -> Unit ,
	arabicFontSizeState : SettingValueState<Float> ,
	translationFontSizeState : SettingValueState<Float> ,
				  )
{
	//a dialog with two sliders to control the font size of quran
	//after the user selects the font size then save it in shared preferences
	//and then update the state of the quran page
	AlertDialog(
			modifier = Modifier
				.fillMaxWidth()
				.wrapContentHeight() ,
			onDismissRequest = { showDialog3(false) } ,
			title = { Text(text = "Font") } ,
			text = {
				Column(
						verticalArrangement = Arrangement.SpaceBetween ,
						horizontalAlignment = Alignment.CenterHorizontally
					  ) {
					Row(
							modifier = Modifier
								.fillMaxWidth()
								.wrapContentHeight() ,
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.SpaceBetween
					   ) {
						Text(text = "Arabic Font")
						Spacer(modifier = Modifier.width(8.dp))
						//round this value to make it clean and easy to read
						Text(text = arabicFontSizeState.value.roundToInt().toString())
					}
					Slider(
							value = arabicFontSizeState.value ,
							onValueChange = { arabicFontSizeState.value = it } ,
							valueRange = 24f .. 40f ,
							modifier = Modifier.width(300.dp)
						  )


					Row(
							modifier = Modifier
								.fillMaxWidth()
								.wrapContentHeight() ,
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.SpaceBetween
					   ) {
						Text(text = "Translation Font")
						//a text to show the font value of the translation
						//round this value to make it clean and easy to read
						Text(text = translationFontSizeState.value.roundToInt().toString())
					}

					Slider(
							value = translationFontSizeState.value ,
							onValueChange = { translationFontSizeState.value = it } ,
							valueRange = 16f .. 40f ,
							modifier = Modifier.width(300.dp)
						  )
				}
			} ,
			confirmButton = {
				Button(
						onClick = {
							showDialog3(false)
						} ,
						modifier = Modifier.padding(8.dp)
					  ) {
					Text(text = "Confirm")
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