package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihGoalDialog(
	onConfirm : (String) -> Unit ,
	isOpen : MutableState<Boolean> ,
	state : MutableState<String> ,
)
{
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
								onValueChange = {
									state.value = it
								} ,
								label = { Text(text = "Daily Goal") } ,
										 )
					}
				} ,
				confirmButton = {
					Button(
							onClick = {
								onConfirm(state.value)
								isOpen.value = false
							} ,
							content = { Text(text = "Confirm") } ,
						  )
				} ,
				dismissButton = {
					Button(
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