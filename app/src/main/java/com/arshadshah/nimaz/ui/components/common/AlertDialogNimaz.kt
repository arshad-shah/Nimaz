package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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

//custom AlertDialog
@Composable
fun AlertDialogNimaz(
	modifier : Modifier = Modifier ,
	icon : Painter? = null ,
	action : @Composable() (() -> Unit)? = null ,
	contentDescription : String ,
	title : String ,
	description : String? = null ,
	contentToShow : @Composable () -> Unit ,
	contentHeight : Dp = 300.dp ,
	onDismissRequest : () -> Unit ,
	properties : DialogProperties = DialogProperties() ,
	topDivider : Boolean = true ,
	bottomDivider : Boolean = true ,
	onConfirm : () -> Unit ,
	confirmButtonText : String = "Done" ,
	onDismiss : () -> Unit ,
	dismissButtonText : String = "Cancel" ,
	showConfirmButton : Boolean = true ,
	showDismissButton : Boolean = true ,
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
						.padding(16.dp)
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
				}
				if (bottomDivider) Divider(color = MaterialTheme.colorScheme.outline)
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.wrapContentHeight()
							.padding(top = 8.dp , end = 8.dp , bottom = 8.dp) ,
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
								modifier = Modifier.padding(end = 8.dp) ,
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
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AlertDialogNimazPreviewWithOutlinedTextField()
{
	val textToShow = remember { mutableStateOf("") }
	AlertDialogNimaz(
			title = "Hello" ,
			contentDescription = "Add" ,
			topDivider = true ,
			bottomDivider = true ,
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