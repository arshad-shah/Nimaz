package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.SliderWithIcons
import com.arshadshah.nimaz.ui.components.settings.state.FloatPreferenceSettingValueState
import com.arshadshah.nimaz.ui.components.settings.state.StringPreferenceSettingValueState
import com.arshadshah.nimaz.viewModel.QuranViewModel
import kotlin.math.roundToInt


@Composable
fun FontSizeDialog(
	showDialog3 : (Boolean) -> Unit ,
	arabicFontSizeState : FloatPreferenceSettingValueState ,
	translationFontSizeState : FloatPreferenceSettingValueState ,
	fontStyleState : StringPreferenceSettingValueState ,
	items3 : List<String> ,
	handleQuranEvents : (QuranViewModel.QuranMenuEvents) -> Unit ,
				  )
{

	AlertDialogNimaz(
			topDivider = false ,
			bottomDivider = false ,
			contentHeight = 250.dp ,
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
					SliderWithIcons(
							value = arabicFontSizeState.value ,
							onValueChange = {
								arabicFontSizeState.value = it
								handleQuranEvents(
										QuranViewModel.QuranMenuEvents.Change_Arabic_Font_Size(
												it
																							  )
												 )
							} ,
							valueRange = if (fontStyleState.value == "IndoPak") 32f .. 60f else 24f .. 46f ,
							leadingIcon = painterResource(id = R.drawable.arabic_font_size_icon) ,
							leadingIconSize = 24.dp ,
							trailaingIcon = painterResource(id = R.drawable.arabic_font_size_icon) ,
							trailingIconSize = 32.dp ,
							contentDescription1 = "Decrease Arabic Font Size" ,
							contentDescription2 = "Increase Arabic Font Size"
								   )


					Row(
							modifier = Modifier
								.fillMaxWidth()
								.wrapContentHeight() ,
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.SpaceBetween
					   ) {
						Text(
								text = "Translation Font" ,
								style = MaterialTheme.typography.bodyMedium
							)
						//a text to show the font value of the translation
						//round this value to make it clean and easy to read
						Text(text = translationFontSizeState.value.roundToInt().toString())
					}

					SliderWithIcons(
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
							leadingIcon = painterResource(id = R.drawable.english_font_size_icon) ,
							leadingIconSize = 16.dp ,
							trailaingIcon = painterResource(id = R.drawable.english_font_size_icon) ,
							trailingIconSize = 24.dp ,
							contentDescription1 = "Decrease Translation Font Size" ,
							contentDescription2 = "Increase Translation Font Size"
								   )

					LabelWithDropdownMenu(
							label = "Font Style (Arabic)" ,
							items = items3 ,
							selectedItem = fontStyleState.value ,
							onItemSelected = {
								fontStyleState.value = it
								setFontBasedOnFontStyle(
										it ,
										arabicFontSizeState ,
										translationFontSizeState
													   )
								handleQuranEvents(
										QuranViewModel.QuranMenuEvents.Change_Arabic_Font(
												it
																						 )
												 )
							} ,
							modifier = Modifier
								.fillMaxWidth()
								.wrapContentHeight()
										 )
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

//label with a dropdown menu at the end
@Composable
fun LabelWithDropdownMenu(
	label : String ,
	items : List<String> ,
	selectedItem : String ,
	onItemSelected : (String) -> Unit ,
	modifier : Modifier = Modifier ,
						 )
{
	val expanded = remember { mutableStateOf(false) }
	Row(
			modifier = modifier
				.padding(vertical = 8.dp)
				.fillMaxWidth()
				.wrapContentHeight() ,
			verticalAlignment = Alignment.CenterVertically ,
			horizontalArrangement = Arrangement.SpaceBetween
	   ) {
		Text(text = label , style = MaterialTheme.typography.bodyMedium)
		ElevatedCard(
				modifier = Modifier
					.width(120.dp)
					) {
			//an elevation card that shows the text and icon
			ElevatedCard(
					modifier = Modifier
						.fillMaxWidth()
						.clickable {
							expanded.value = ! expanded.value
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
									text = items[items.indexOf(selectedItem)] ,
									modifier = Modifier.padding(start = 8.dp) ,
									style = MaterialTheme.typography.bodyMedium ,
								)
							Crossfade(
									targetState = expanded.value ,
									animationSpec = tween(durationMillis = 300)
									 ) { expanded ->
								if (expanded)
								{
									Icon(
											painter = painterResource(id = R.drawable.arrow_up_icon) ,
											contentDescription = "dropdown icon" ,
											modifier = Modifier
												.padding(horizontal = 8.dp)
												.size(18.dp)
										)
								} else
								{
									Icon(
											painter = painterResource(id = R.drawable.arrow_down_icon) ,
											contentDescription = "dropdown icon" ,
											modifier = Modifier
												.padding(horizontal = 8.dp)
												.size(18.dp)
										)
								}
							}
						}
						DropdownMenu(
								offset = DpOffset(5.dp , 0.dp) ,
								modifier = Modifier
									.wrapContentWidth()
									.wrapContentHeight() ,
								expanded = expanded.value ,
								onDismissRequest = {
									expanded.value = false
								} ,
								content = {
									items.forEach { item ->
										DropdownMenuItem(
												onClick = {
													onItemSelected(item)
													expanded.value = false
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

fun setFontBasedOnFontStyle(
	fontStyle : String ,
	arabicFontSizeState : FloatPreferenceSettingValueState ,
	translationFontSizeState : FloatPreferenceSettingValueState ,
						   )
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

		"IndoPak" ->
		{
			arabicFontSizeState.value = 32f
			translationFontSizeState.value = 16f
		}
	}
}
