package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihGoalDialog(
	onConfirm : (String) -> Unit ,
	isOpen : MutableState<Boolean> ,
	state : MutableState<String> ,
)
{
	val context = LocalContext.current
	if (isOpen.value)
	{
		//an alert dialog with text field to enter the tasbih goal
		AlertDialog(
				onDismissRequest = { isOpen.value = false } ,
				title = { Text(text = "Set Daily Goal") } ,
				text = {
					Column(
							modifier = Modifier.padding(8.dp) ,
							verticalArrangement = Arrangement.spacedBy(8.dp) ,
							horizontalAlignment = Alignment.CenterHorizontally ,
						  ) {
						OutlinedTextField(
								value = state.value ,
								maxLines = 1,
								keyboardActions = KeyboardActions(
										onDone = {
											val isInt = state.value.toIntOrNull()
											if (isInt != null && state.value != "")
											{
												if (state.value.toInt() > 0){
													onConfirm(state.value)
													isOpen.value = false
												}
												else
												{
													Toasty
														.error(
																context ,
																"Goal must be greater than 0" ,
																Toasty.LENGTH_SHORT
															  )
														.show()
												}
											} else
											{
												Toasty
													.error(
															context ,
															"Goal must be greater than 0" ,
															Toasty.LENGTH_SHORT
														  )
													.show()
											}
										}) ,
								onValueChange = {
									state.value = it
								} ,
								label = { Text(text = "Daily Goal") } ,
								keyboardOptions = KeyboardOptions(
										keyboardType = KeyboardType.Number ,
										imeAction = ImeAction.Done ,
																 ) ,
										 )
					}
				} ,
				confirmButton = {
					Button(
							onClick = {
								val isInt = state.value.toIntOrNull()
								if (isInt != null && state.value != "")
								{
									if (state.value.toInt() > 0){
										onConfirm(state.value)
										isOpen.value = false
									}
									else
									{
										Toasty
											.error(
													context ,
													"Goal must be greater than 0" ,
													Toasty.LENGTH_SHORT
												  )
											.show()
									}
								} else
								{
									Toasty
										.error(
												context ,
												"Goal must be greater than 0" ,
												Toasty.LENGTH_SHORT
											  )
										.show()
								}
							} ,
							content = { Text(text = "Confirm") } ,
						  )
				} ,
				dismissButton = {
					TextButton(
							onClick = {
								isOpen.value = false
									  } ,
							content = { Text(text = "Cancel") } ,
						  )
				} ,
				   )
	}
	else
	{
		//do nothing
		return
	}
}