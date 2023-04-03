package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.AlertDialogNimaz
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

	val icon = when (fontMenuExpanded.value)
	{
		true -> painterResource(id = R.drawable.arrow_up_icon)
		false -> painterResource(id = R.drawable.arrow_down_icon)
	}
	AlertDialogNimaz(
			topDivider = false ,
			bottomDivider = false ,
			contentHeight = 250.dp,
			dismissButtonText = "Close" ,
			contentDescription = "Font Settings" ,
			title = "Font Settings" ,
			contentToShow = {
				Column(
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth()
							.wrapContentHeight() ,
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
					Text(text = "Arabic Font" , style = MaterialTheme.typography.bodyMedium)
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
					Text(text = "Translation Font" , style = MaterialTheme.typography.bodyMedium)
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
					Text(text = "Font Style (Arabic)" , style = MaterialTheme.typography.bodyMedium)
					ElevatedCard(
							shape = MaterialTheme.shapes.extraLarge ,
							elevation = CardDefaults.elevatedCardElevation(
									defaultElevation = 4.dp
																		  ) ,
							modifier = Modifier
								.width(120.dp)
								) {
						//an elevation card that shows the text and icon
						ElevatedCard(
								modifier = Modifier
									.fillMaxWidth()
									.clickable {
										fontMenuExpanded.value = ! fontMenuExpanded.value
									} ,
								content = {
									Row(
											modifier = Modifier
												.fillMaxWidth()
												.padding(8.dp) ,
											verticalAlignment = Alignment.CenterVertically ,
											horizontalArrangement = Arrangement.SpaceBetween
									   ) {
										//find the font style from the list of font styles
										//and then show it in the text
										Text(
												text = items3[items3.indexOf(fontStyleState.value)] ,
												modifier = Modifier.padding(start = 8.dp) ,
												style = MaterialTheme.typography.bodyMedium ,
											)
										Icon(
												modifier = Modifier
													.padding(start = 8.dp)
													.size(18.dp) ,
												painter = icon ,
												contentDescription = null
											)
									}
									DropdownMenu(
											modifier = Modifier
												.wrapContentWidth()
												.wrapContentHeight() ,
											expanded = fontMenuExpanded.value ,
											onDismissRequest = {
												fontMenuExpanded.value = false
											} ,
											content = {
												items3.forEach { item ->
													DropdownMenuItem(
															onClick = {
																fontStyleState.value = item
																setFontBasedOnFontStyle(
																		fontStyleState.value
																					   )
																fontMenuExpanded.value =
																	false
																handleQuranEvents(
																		QuranViewModel.QuranMenuEvents.Change_Arabic_Font(
																				fontStyleState.value
																														 )
																				 )
															} ,
															text = {
																Text(
																		text = item ,
																		style = MaterialTheme.typography.bodyMedium
																	)
															}
																	)
												}
											}
												)
								}
									)
					}
				}
			}
			} ,
			onDismissRequest = {
				showDialog3(false)
			} ,
			onConfirm = {
				showDialog3(false)
			} ,
			onDismiss = {
					showDialog3(false)
			})
}