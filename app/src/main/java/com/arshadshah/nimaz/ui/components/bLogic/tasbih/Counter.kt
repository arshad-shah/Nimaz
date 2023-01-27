package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import android.widget.ImageButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.Edit
import compose.icons.feathericons.Minus
import compose.icons.feathericons.Plus
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Counter()
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
				.fillMaxHeight()
				.padding(16.dp)
				.padding(bottom = 196.dp),
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Top
		  ) {
		//lap text
		Text(
				modifier = Modifier
					.align(Alignment.CenterHorizontally) ,
				text = "Loop ${lap.value}" , style = MaterialTheme.typography.bodyMedium
			)
		//large count text
		Text(
				modifier = Modifier
					.align(Alignment.CenterHorizontally) ,
				text = count.value.toString() ,
				style = MaterialTheme.typography.displayMedium ,
				fontSize = 100.sp
			)

		Row(
				modifier = Modifier.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			ElevatedButton(
					modifier = Modifier.shadow(5.dp, RoundedCornerShape(50)) ,
					onClick = {
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
							style = MaterialTheme.typography.titleLarge,
							fontSize = 26.sp
						)
					Icon(imageVector = FeatherIcons.Edit , contentDescription = "Edit")
				}
			}
			ElevatedButton(
					modifier = Modifier.shadow(5.dp, RoundedCornerShape(50)) ,
					contentPadding = PaddingValues(16.dp) ,
					onClick = {
						showResetDialog = true
					}) {
				Icon(imageVector = Icons.Filled.Refresh , contentDescription = "Reset", modifier = Modifier.size(48.dp))
			}
		}

		Spacer(modifier = Modifier.height(32.dp))
		//a big image button to increment the tasbih count
		//the row should take as much space as possible
		//center the icon in the middle of the row
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.border(
							1.dp ,
							MaterialTheme.colorScheme.outline ,
							shape = RoundedCornerShape(8.dp)
						   )
					.clickable {
						count.value ++
						lapCountCounter.value ++
						if (lapCountCounter.value == objective.value.toInt())
						{
							lap.value ++
							lapCountCounter.value = 0
							Toasty
								.info(
										context ,
										"Objective of ${objective.value.toInt()} has been reached" ,
										Toasty.LENGTH_SHORT
									 )
								.show()
						}
					} ,
				horizontalArrangement = Arrangement.Center ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			//center the icon in the Row
			Icon(
					modifier = Modifier
						.size(100.dp),
					imageVector = FeatherIcons.Plus ,
					contentDescription = "Tasbih Plus"
				)
		}
	}

	if (showResetDialog)
	{
		AlertDialog(
				onDismissRequest = { showResetDialog = false } ,
				title = { Text(text = "Reset Counter") } ,
				text = { Text(text = "Are you sure you want to reset the counter?",style = MaterialTheme.typography.titleLarge) } ,
				confirmButton = {
					Button(onClick = {
						count.value = 0
						lap.value = 1
						lapCountCounter.value = 0

						showResetDialog = false
					}) {
						Text(text = "Reset", style = MaterialTheme.typography.titleLarge)
					}
				} ,
				dismissButton = {
					TextButton(onClick = { showResetDialog = false }) {
						Text(text = "Cancel", style = MaterialTheme.typography.titleLarge)
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
							textStyle = MaterialTheme.typography.titleLarge ,
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
						Text(text = "Set", style = MaterialTheme.typography.titleLarge)
					}
				} ,
				dismissButton = {
					TextButton(onClick = { showObjectiveDialog = false }) {
						Text(text = "Cancel",style = MaterialTheme.typography.titleLarge)
					}
				}
				   )
	}
}

//preview of the counter
@Preview(showBackground = true)
@Composable
fun DefaultPreview()
{
	NimazTheme {
		Counter()
	}
}
