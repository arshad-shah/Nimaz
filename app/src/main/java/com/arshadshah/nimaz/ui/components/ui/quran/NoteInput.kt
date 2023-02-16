package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInput(
	showNoteDialog : MutableState<Boolean> ,
	onClick : () -> Unit,
	noteContent : MutableState<String> ,
			 )
{
	//text input field
	//open dialog
	AlertDialog(
			onDismissRequest = {
			} ,
			title = {
				Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					Text(text = "Add Note")
					//an icvonbutton to remove the note
					IconButton(onClick = {
						//remove note
						noteContent.value = ""
						onClick()
					}) {
						Icon(
								modifier = Modifier
									.size(24.dp)
									.padding(4.dp),
								painter = painterResource(id = R.drawable.delete_icon),
								contentDescription = "Remove Note"
								)
					}
				}
					} ,
			text = {
				OutlinedTextField(
						value = noteContent.value ,
						onValueChange = { noteContent.value = it } ,
						label = { Text(text = "Note") } ,
						modifier = Modifier.fillMaxWidth()
								 )
			} ,
			confirmButton = {
				Button(onClick = onClick) { Text(text = "Confirm") }
			} ,
			dismissButton = {
				TextButton(onClick = {
					//close dialog
					showNoteDialog.value = false

				}) { Text(text = "Cancel") }
			} ,
			   )

}