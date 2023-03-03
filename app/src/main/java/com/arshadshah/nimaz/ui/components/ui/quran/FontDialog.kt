package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.StringPreferenceSettingValueState
import kotlin.math.roundToInt


@Composable
fun FontSizeDialog(
	showDialog3 : (Boolean) -> Unit ,
	arabicFontSizeState : SettingValueState<Float> ,
	translationFontSizeState : SettingValueState<Float> ,
	fontStyleState : StringPreferenceSettingValueState ,
	items3 : List<String> ,
	handleQuranEvents : (QuranViewModel.QuranMenuEvents) -> Unit ,
				  )
{
	val fontMenuExpanded = remember { mutableStateOf(false) }
	fun setFontBasedOnFontStyle(fontStyle : String)
	{
		when (fontStyle)
		{
			"Default" ->
			{
				arabicFontSizeState.value = 26f
				translationFontSizeState.value = 16f
			}

			"Quranme" ->
			{
				arabicFontSizeState.value = 24f
				translationFontSizeState.value = 16f
			}

			"Hidayat" ->
			{
				arabicFontSizeState.value = 24f
				translationFontSizeState.value = 16f
			}

			"Amiri" ->
			{
				arabicFontSizeState.value = 24f
				translationFontSizeState.value = 16f
			}
		}
	}
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
							onValueChange = {
								arabicFontSizeState.value = it
								handleQuranEvents(
										QuranViewModel.QuranMenuEvents.Change_Arabic_Font_Size(
												it
																							  )
												 )
							} ,
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
							onValueChange = {
								translationFontSizeState.value = it
								handleQuranEvents(
										QuranViewModel.QuranMenuEvents.Change_Translation_Font_Size(
												it
																								   )
												 )
							} ,
							valueRange = 16f .. 40f ,
							modifier = Modifier.width(300.dp)
						  )

					Row(
							modifier = Modifier
								.fillMaxWidth()
								.wrapContentHeight() ,
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.SpaceBetween
					   ) {
						Text(text = "Font Style")
						Column {
							Row(
									modifier = Modifier
										.clickable { fontMenuExpanded.value = true }
										.padding(8.dp)
										.height(48.dp)
										.border(
												width = 1.dp ,
												color = MaterialTheme.colorScheme.onSurface.copy(
														alpha = 0.2f
																								) ,
												shape = MaterialTheme.shapes.small
											   ) ,
									verticalAlignment = Alignment.CenterVertically
							   ) {
								//find the font style from the list of font styles
								//and then show it in the text
								Text(
										text = items3[items3.indexOf(fontStyleState.value)] ,
										modifier = Modifier.padding(start = 8.dp)
									)
								Icon(
										modifier = Modifier.padding(start = 8.dp) ,
										imageVector = Icons.Outlined.ArrowDropDown ,
										contentDescription = null
									)
							}
							DropdownMenu(
									modifier = Modifier
										.wrapContentWidth()
										.wrapContentHeight() ,
									expanded = fontMenuExpanded.value ,
									onDismissRequest = { fontMenuExpanded.value = false } ,
									content = {
										items3.forEach { item ->
											DropdownMenuItem(
													onClick = {
														fontStyleState.value = item
														setFontBasedOnFontStyle(fontStyleState.value)
														fontMenuExpanded.value = false
														handleQuranEvents(
																QuranViewModel.QuranMenuEvents.Change_Arabic_Font(
																		fontStyleState.value
																												 )
																		 )
													} ,
													text = { Text(text = item) }
															)
										}
									}
										)
						}
					}


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