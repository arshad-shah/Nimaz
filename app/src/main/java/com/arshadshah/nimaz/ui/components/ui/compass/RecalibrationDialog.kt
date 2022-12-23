package com.arshadshah.nimaz.ui.components.ui.compass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties


@Composable
fun RecalibrationDialog(
	title : String ,
	description : String ,
	setShowDialog : (Boolean) -> Unit ,
					   )
{

	AlertDialog(
			shape = CardDefaults.elevatedShape ,
			title = { Text(text = title) } ,
			text = {
				Column(
						modifier = Modifier
							.verticalScroll(rememberScrollState())
							.fillMaxWidth()
							.fillMaxHeight()
					  ) {
					Text(text = description)
				}
			} ,
			onDismissRequest = { setShowDialog(false) } ,
			properties = DialogProperties(
					dismissOnBackPress = true ,
					dismissOnClickOutside = true ,
										 ) ,
			confirmButton = {

			}
			   )
}