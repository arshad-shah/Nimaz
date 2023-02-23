package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInput(
	showNoteDialog : MutableState<Boolean> ,
	onClick : () -> Unit ,
	noteContent : MutableState<String> ,
			 )
{
	val context = LocalContext.current
	//text input field
	//open dialog
	AlertDialog(
			onDismissRequest = {
			} ,
			title = {
				Row(
						modifier = Modifier.fillMaxWidth() ,
						horizontalArrangement = Arrangement.SpaceBetween ,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					Text(text = "Add Note")
					//an icvonbutton to remove the note
					IconButton(
							enabled = noteContent.value.isNotEmpty() ,
							onClick = {
								//remove note
								noteContent.value = ""
								onClick()
							}) {
						Icon(
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp) ,
								painter = painterResource(id = R.drawable.delete_icon) ,
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
						modifier = Modifier
							.fillMaxWidth()
							.height(200.dp) ,
						trailingIcon = {
							//an icon to clear the text field
							IconButton(onClick = {
								//clear text field
								noteContent.value = ""
							}) {
								Icon(
										modifier = Modifier
											.size(24.dp)
											.padding(4.dp) ,
										painter = painterResource(id = R.drawable.cross_icon) ,
										contentDescription = "Clear Text"
									)
							}
						}
								 )
			} ,
			confirmButton = {
				Button(onClick = {
					if (noteContent.value.isEmpty())
					{
						//show toast message saying note is empty
						Toasty.warning(
								context ,
								"Note is empty. closing without save." ,
								Toasty.LENGTH_SHORT
									  ).show()
						showNoteDialog.value = false
					} else
					{
						onClick()
					}
				}) { Text(text = "Save") }
			} ,
			dismissButton = {
				TextButton(onClick = {
					//close dialog
					showNoteDialog.value = false
				}) { Text(text = "Cancel") }
			} ,
			   )

}