package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import es.dmoral.toasty.Toasty


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihDefaultScreen(paddingValues : PaddingValues)
{

	//count should not go below 0
	val count = remember { mutableStateOf(0) }
	val objective = remember { mutableStateOf("33") }
	var showResetDialog by remember { mutableStateOf(false) }
	var showObjectiveDialog by remember { mutableStateOf(false) }

	//lap counter
	val lap = remember { mutableStateOf(0) }
	val lapCountCounter = remember { mutableStateOf(0) }

	val context = LocalContext.current

	Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp)
				.padding(paddingValues)
		  ) {
		//lap text
		Text(
				modifier = Modifier
					.align(Alignment.CenterHorizontally) ,
				text = "Loop ${lap.value}" , style = MaterialTheme.typography.bodyMedium
			)
		Text(
				modifier = Modifier
					.align(Alignment.CenterHorizontally) ,
				text = count.value.toString() ,
				style = MaterialTheme.typography.displayMedium
			)
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp)
		   ) {

			Column(
					modifier = Modifier.weight(1f) ,
					horizontalAlignment = Alignment.CenterHorizontally ,
					verticalArrangement = Arrangement.Center
				  ) {
				ElevatedButton(
						contentPadding = PaddingValues(16.dp) ,
						onClick = {
							count.value ++
							lapCountCounter.value ++
							if (lapCountCounter.value == objective.value.toInt())
							{
								lap.value ++
								lapCountCounter.value = 0
								Toasty.info(
										context ,
										"Objective of ${objective.value.toInt()} has been reached" ,
										Toasty.LENGTH_SHORT
										   ).show()
							}
						}) {
					Icon(imageVector = FeatherIcons.Plus , contentDescription = "Add")
				}
				Spacer(modifier = Modifier.height(16.dp))
				ElevatedButton(
						contentPadding = PaddingValues(16.dp) ,
						onClick = {
							//count should not go below 0
							if (count.value > 0)
							{
								count.value --
								if (count.value == objective.value.toInt())
								{
									lap.value --
								}
							}
							//if count is 0 then set all values to default
							if (count.value == 0)
							{
								lap.value = 0
								lapCountCounter.value = 0
							}
						}) {
					Icon(imageVector = FeatherIcons.Minus , contentDescription = "Delete")
				}
			}
		}
		Row(
				modifier = Modifier.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			ElevatedButton(onClick = {
				//if the tasbih count is greater then show toast saying that the tasbih count must be 0 to edit the objective
				if (count.value > 0)
				{
					Toasty.info(
							context ,
							"Objective can only be changed when the tasbih count is 0" ,
							Toasty.LENGTH_SHORT
							   ).show()
				} else
				{
					showObjectiveDialog = true
				}
			}) {
				Row(
						horizontalArrangement = Arrangement.SpaceBetween ,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					Text(
							modifier = Modifier.padding(16.dp) ,
							text = objective.value ,
							style = MaterialTheme.typography.titleLarge
						)
					Icon(imageVector = FeatherIcons.Edit , contentDescription = "Edit")
				}
			}
			ElevatedButton(
					contentPadding = PaddingValues(16.dp) ,
					onClick = {
						showResetDialog = true
					}) {
				Icon(imageVector = Icons.Filled.Refresh , contentDescription = "Reset")
			}
		}
	}

	if (showResetDialog)
	{
		AlertDialog(
				onDismissRequest = { showResetDialog = false } ,
				title = { Text(text = "Reset Counter") } ,
				text = { Text(text = "Are you sure you want to reset the counter?") } ,
				confirmButton = {
					Button(onClick = {
						count.value = 0
						lap.value = 1
						lapCountCounter.value = 0

						showResetDialog = false
					}) {
						Text(text = "Reset")
					}
				} ,
				dismissButton = {
					TextButton(onClick = { showResetDialog = false }) {
						Text(text = "Cancel")
					}
				}
				   )
	}

	if (showObjectiveDialog)
	{
		AlertDialog(
				onDismissRequest = { showObjectiveDialog = false } ,
				title = { Text(text = "Set Tasbih Objective") } ,
				text = {
					Spacer(modifier = Modifier.height(16.dp))
					OutlinedTextField(
							value = objective.value ,
							onValueChange = { objective.value = it } ,
							maxLines = 1 ,
							keyboardOptions = KeyboardOptions(
									keyboardType = KeyboardType.Number ,
									imeAction = ImeAction.Done ,
															 )
									 )
				} ,
				confirmButton = {
					Button(onClick = {
						showObjectiveDialog = false
					}) {
						Text(text = "Set")
					}
				} ,
				dismissButton = {
					TextButton(onClick = { showObjectiveDialog = false }) {
						Text(text = "Cancel")
					}
				}
				   )
	}
}


@Preview
@Composable
fun TasbihDefaultScreenPreview()
{
	NimazTheme {
		TasbihDefaultScreen(paddingValues = PaddingValues(16.dp))
	}
}