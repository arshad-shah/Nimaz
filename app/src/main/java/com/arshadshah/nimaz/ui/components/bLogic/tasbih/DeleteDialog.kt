package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel

@Composable
fun DeleteDialog(
	tasbih : Tasbih ,
	showDialog : MutableState<Boolean> ,
				)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "TasbihViewModel" ,
			initializer = { TasbihViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	if (showDialog.value)
	{
		AlertDialog(
				onDismissRequest = { showDialog.value = false } ,
				title = { Text(text = "Delete Tasbih") } ,
				text = { Text(text = "Are you sure you want to delete this tasbih?") } ,
				confirmButton = {
					Button(
							onClick = {
								viewModel.handleEvent(
										TasbihViewModel.TasbihEvent.DeleteTasbih(
												tasbih
																				)
													 )
								showDialog.value = false
							} ,
						  ) {
						Text(text = "Submit" , style = MaterialTheme.typography.titleMedium)
					}
				} ,
				dismissButton = {
					TextButton(
							onClick = {
								showDialog.value = false
							} ,
							  ) {
						Text(text = "Cancel")
					}
				} ,
				   )
	}
}