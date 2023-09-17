package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.R

/**
 * A custom Alert dialog for the app, it is very flexible and allows usage in many different ways
 * it accepts booleans flags to show or hide certain elements of the dialog.
 * @param modifier Modifier to apply to the dialog
 * @param icon Icon to show in the dialog
 * @param action The action shown to right of title in the dialog
 * @param title The title of the dialog
 * @param contentDescription The content description for the dialog icon
 * @param description The description of the dialog
 * @param contentToShow The content to show in the dialog
 * @param contentHeight The height of the content to show in the dialog
 * @param cardContent Whether to show the content in a card or not
 * @param onDismissRequest The callback to be called when the dialog is dismissed
 * @param properties The properties of the dialog
 * @param topDivider Whether to show a top divider or not
 * @param bottomDivider Whether to show a bottom divider or not
 * @param showConfirmButton Whether to show the confirm button or not
 * @param showDismissButton Whether to show the dismiss button or not
 * @param onConfirm The callback to be called when the confirm button is clicked
 * @param confirmButtonText The text to show on the confirm button
 * @param onDismiss The callback to be called when the dismiss button is clicked
 * @param dismissButtonText The text to show on the dismiss button
 * @sample com.arshadshah.nimaz.ui.components.common.AlertDialogNimazPreviewWithAction
 * */
@Composable
fun AlertDialogNimaz(
	modifier : Modifier = Modifier ,
	icon : Painter? = null ,
	action : @Composable (() -> Unit)? = null ,
	title : String ,
	contentDescription : String ,
	description : String? = null ,
	contentToShow : @Composable () -> Unit ,
	contentHeight : Dp = 300.dp ,
	cardContent : Boolean = true ,
	onDismissRequest : () -> Unit ,
	properties : DialogProperties = DialogProperties() ,
	topDivider : Boolean = true ,
	bottomDivider : Boolean = true ,
	showConfirmButton : Boolean = true ,
	showDismissButton : Boolean = true ,
	onConfirm : () -> Unit ,
	confirmButtonText : String = "Done" ,
	onDismiss : () -> Unit ,
	dismissButtonText : String = "Cancel" ,
					)
{
	//state of the scrollable column
	val stateScroll = rememberScrollState()
	Dialog(
			 onDismissRequest = onDismissRequest ,
			 properties = properties ,
		  ) {
		ElevatedCard(
				 modifier = modifier
					 .fillMaxWidth()
					 .wrapContentHeight() ,
				 shape = MaterialTheme.shapes.extraLarge ,
					) {
			Column(
					 modifier = Modifier
						 .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
						 .fillMaxWidth()
						 .wrapContentHeight()
				  ) {
				Column(
						 modifier = Modifier
							 .fillMaxWidth()
							 .wrapContentHeight() ,
						 verticalArrangement = Arrangement.Center ,
						 horizontalAlignment = Alignment.CenterHorizontally
					  ) {
					if (icon != null)
					{
						Icon(
								 painter = icon ,
								 contentDescription = contentDescription ,
								 tint = MaterialTheme.colorScheme.primary ,
								 modifier = Modifier
									 .size(32.dp)
							)
					}
					if (action != null)
					{
						Row(
								 modifier
									 .padding(bottom = 4.dp)
									 .fillMaxWidth() ,
								 horizontalArrangement = Arrangement.SpaceBetween ,
								 verticalAlignment = Alignment.CenterVertically
						   ) {
							Text(
									 text = title ,
									 style = MaterialTheme.typography.titleLarge ,
									 modifier = Modifier
										 .weight(0.8f)
										 .padding(4.dp) ,
									 textAlign = TextAlign.Start ,
									 maxLines = 1 ,
									 overflow = TextOverflow.Ellipsis ,
									 softWrap = true
								)
							action()
						}
					} else
					{
						Text(
								 text = title ,
								 style = MaterialTheme.typography.titleLarge ,
								 modifier = Modifier
									 .padding(8.dp)
							)
					}
				}

				Column {
					if (description != null)
					{
						Row(
								 modifier
									 .padding(bottom = 4.dp)
									 .fillMaxWidth()
						   ) {
							Text(
									 text = description ,
									 style = MaterialTheme.typography.bodyMedium ,
									 modifier = Modifier.padding(vertical = 8.dp)
								)
						}
					}
					if (topDivider) Divider(color = MaterialTheme.colorScheme.outline)
					if (cardContent)
					{
						Card(
								 modifier = Modifier.fillMaxWidth() ,
							) {
							Row(
									 modifier
										 .height(contentHeight)
										 .fillMaxWidth() ,
									 verticalAlignment = Alignment.CenterVertically ,
									 horizontalArrangement = Arrangement.Center
							   ) {
								Column(
										 Modifier
											 .verticalScroll(state = stateScroll) ,
										 verticalArrangement = Arrangement.Center ,
										 horizontalAlignment = Alignment.CenterHorizontally
									  ) {
									contentToShow()
								}
							}
						}
					} else
					{
						Row(
								 modifier
									 .height(contentHeight)
									 .fillMaxWidth() ,
								 verticalAlignment = Alignment.CenterVertically ,
								 horizontalArrangement = Arrangement.Center
						   ) {
							Column(
									 Modifier
										 .verticalScroll(state = stateScroll) ,
									 verticalArrangement = Arrangement.Center ,
									 horizontalAlignment = Alignment.CenterHorizontally
								  ) {
								contentToShow()
							}
						}
					}
				}
				if (bottomDivider) Divider(color = MaterialTheme.colorScheme.outline)
				Row(
						 modifier = Modifier
							 .fillMaxWidth()
							 .wrapContentHeight()
							 .padding(top = 16.dp , end = 8.dp , bottom = 0.dp) ,
						 horizontalArrangement = Arrangement.End
				   ) {
					if (showDismissButton)
					{
						TextButton(
								 modifier = Modifier.padding(horizontal = 16.dp) ,
								 onClick = { onDismiss() } ,
								 content = {
									 Text(
											  text = dismissButtonText ,
											  style = MaterialTheme.typography.titleMedium
										 )
								 }
								  )
					}
					if (showConfirmButton)
					{
						Button(
								 modifier = Modifier.padding(start = 8.dp) ,
								 onClick = { onConfirm() } ,
								 content = {
									 Text(
											  text = confirmButtonText ,
											  style = MaterialTheme.typography.titleMedium
										 )
								 }
							  )
					}
				}
			}
		}
	}
}


//alert dilog nimaz preview
@Preview
@Composable
fun AlertDialogNimazPreview()
{
	AlertDialogNimaz(
			 title = "Hello" ,
			 icon = painterResource(id = R.drawable.mail_icon) ,
			 contentDescription = "Add" ,
			 contentToShow = { Text(text = "This is a content") } ,
			 onDismissRequest = { } ,
			 onConfirm = { } ,
			 onDismiss = { } ,
			 description = "This is a description"
					)
}

//preview of the alert dialog nimaz with action
@Preview
@Composable
fun AlertDialogNimazPreviewWithAction()
{
	AlertDialogNimaz(
			 title = "very long title to test the alert dialog" ,
			 contentDescription = "Add" ,
			 contentToShow = { Text(text = "This is a content") } ,
			 onDismissRequest = { } ,
			 onConfirm = { } ,
			 onDismiss = { } ,
			 description = "This is a description" ,
			 action = {
				 IconButton(
						  onClick = { } ,
						  content = {
							  Icon(
									   modifier = Modifier.size(24.dp) ,
									   painter = painterResource(id = R.drawable.play_icon) ,
									   contentDescription = "Add" ,
									   tint = MaterialTheme.colorScheme.primary
								  )
						  }
						   )
			 }
					)
}

//with a oulinedTextField
@Preview
@Composable
fun AlertDialogNimazPreviewWithOutlinedTextField()
{
	val textToShow = remember { mutableStateOf("") }
	AlertDialogNimaz(
			 title = "Hello" ,
			 contentDescription = "Add" ,
			 topDivider = false ,
			 bottomDivider = false ,
			 contentToShow = {
				 OutlinedTextField(
						  label = { Text(text = "Enter your name") } ,
						  singleLine = true ,
						  shape = MaterialTheme.shapes.extraLarge ,
						  value = textToShow.value ,
						  onValueChange = {
							  textToShow.value = it
						  } ,
						  modifier = Modifier
							  .fillMaxWidth()
							  .fillMaxHeight()
								  )
			 } ,
			 cardContent = false ,
			 contentHeight = 100.dp ,
			 onDismissRequest = { } ,
			 onConfirm = { } ,
			 onDismiss = { } ,
			 action = {
				 IconButton(
						  onClick = { } ,
						  content = {
							  Icon(
									   modifier = Modifier.size(24.dp) ,
									   painter = painterResource(id = R.drawable.play_icon) ,
									   contentDescription = "Add" ,
									   tint = MaterialTheme.colorScheme.primary
								  )
						  }
						   )
			 }
					)
}