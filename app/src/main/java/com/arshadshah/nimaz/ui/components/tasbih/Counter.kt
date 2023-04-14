package com.arshadshah.nimaz.ui.components.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Counter(
	paddingValues : PaddingValues ,
		   )
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = AppConstants.TASBIH_VIEWMODEL_KEY ,
			initializer = { TasbihViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

	val resetTasbih = remember {
		viewModel.resetButtonState
	}.collectAsState()

	val count = remember {
		mutableStateOf(
				context.getSharedPreferences("tasbih" , 0).getInt("count" , 0)
					  )
	}

	val objective = remember {
		mutableStateOf(
				context.getSharedPreferences("tasbih" , 0).getString("objective" , "33") !!
					  )
	}

	val showObjectiveDialog = remember { mutableStateOf(false) }

	//lap counter
	val lap =
		remember {
			mutableStateOf(
					context.getSharedPreferences("tasbih" , 0).getInt("lap" , 1)
						  )
		}
	val lapCountCounter = remember {
		mutableStateOf(
				context.getSharedPreferences("tasbih" , 0).getInt("lapCountCounter" , 0)
					  )
	}

	//persist all the values in shared preferences if the activity is destroyed
	LaunchedEffect(key1 = count.value , key2 = objective.value , key3 = lap.value)
	{
		//save the count
		context.getSharedPreferences("tasbih" , 0).edit().putInt("count" , count.value).apply()
		//save the objective
		context.getSharedPreferences("tasbih" , 0).edit().putString("objective" , objective.value)
			.apply()
		//save the lap
		context.getSharedPreferences("tasbih" , 0).edit().putInt("lap" , lap.value).apply()
		//save the lap count counter
		context.getSharedPreferences("tasbih" , 0).edit()
			.putInt("lapCountCounter" , lapCountCounter.value).apply()
	}

	Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp)
				.padding(paddingValues) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Top
		  ) {
		//lap text
		Text(
				modifier = Modifier
					.align(Alignment.CenterHorizontally) ,
				text = "Loop ${lap.value}" ,
				style = MaterialTheme.typography.bodyMedium ,
				color = MaterialTheme.colorScheme.onSurface
			)
		//large count text
		Text(
				modifier = Modifier
					.align(Alignment.CenterHorizontally) ,
				text = count.value.toString() ,
				style = MaterialTheme.typography.displayMedium ,
				fontSize = 100.sp ,
				color = MaterialTheme.colorScheme.onSurface
			)
		Editbutton(
				count = count ,
				context = LocalContext.current ,
				showObjectiveDialog = showObjectiveDialog ,
				objective = objective ,
				  )

		Spacer(modifier = Modifier.height(32.dp))
		IncrementDecrement(
				count = count ,
				lap = lap ,
				lapCountCounter = lapCountCounter ,
				objective = objective ,
						  )
	}

	if (resetTasbih.value)
	{
		AlertDialogNimaz(
				bottomDivider = false ,
				topDivider = false ,
				contentHeight = 100.dp ,
				contentDescription = "Reset Counter" ,
				title = "Reset Counter" ,
				confirmButtonText = "Yes" ,
				dismissButtonText = "No, Cancel" ,
				contentToShow = {
					Text(
							text = "Are you sure you want to reset the counter?" ,
							style = MaterialTheme.typography.bodyMedium ,
							modifier = Modifier.padding(8.dp)
						)
				} ,
				onDismissRequest = {
					viewModel.handleEvent(TasbihViewModel.TasbihEvent.UpdateResetButtonState(false))
				} ,
				onConfirm = {
					count.value = 0
					lap.value = 1
					lapCountCounter.value = 0
					viewModel.handleEvent(
							TasbihViewModel.TasbihEvent.UpdateResetButtonState(
									false
																			  )
										 )
				} ,
				onDismiss = {
					viewModel.handleEvent(TasbihViewModel.TasbihEvent.UpdateResetButtonState(false))
				})
	}

	if (showObjectiveDialog.value)
	{
		AlertDialogNimaz(
				bottomDivider = false ,
				topDivider = false ,
				contentHeight = 100.dp ,
				contentDescription = "Set Tasbih Objective" ,
				title = "Set Tasbih Objective" ,
				contentToShow = {
					OutlinedTextField(
							shape = MaterialTheme.shapes.extraLarge ,
							textStyle = MaterialTheme.typography.titleLarge ,
							value = objective.value ,
							onValueChange = { objective.value = it } ,
							singleLine = true ,
							keyboardOptions = KeyboardOptions(
									keyboardType = KeyboardType.Number ,
									imeAction = ImeAction.Done ,
															 ) ,
							label = {
								Text(
										text = "Objective" ,
									)
							} ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 16.dp) ,
							keyboardActions = KeyboardActions(
									onDone = {
										val isInt = objective.value.toIntOrNull()
										if (isInt != null)
										{
											if (objective.value != "" || isInt != 0)
											{
												showObjectiveDialog.value = false
											} else
											{
												Toasty
													.error(
															context ,
															"Objective must be greater than 0" ,
															Toasty.LENGTH_SHORT
														  )
													.show()
											}
										} else
										{
											Toasty
												.error(
														context ,
														"Objective must be greater than 0" ,
														Toasty.LENGTH_SHORT
													  )
												.show()
										}
									})
									 )
				} ,
				onDismissRequest = {
					showObjectiveDialog.value = false
				} ,
				onConfirm = {
					val isInt = objective.value.toIntOrNull()
					if (isInt != null)
					{
						if (objective.value != "" || isInt != 0)
						{
							showObjectiveDialog.value = false
						} else
						{
							Toasty
								.error(
										context ,
										"Objective must be greater than 0" ,
										Toasty.LENGTH_SHORT
									  )
								.show()
						}
					} else
					{
						Toasty
							.error(
									context ,
									"Objective must be greater than 0" ,
									Toasty.LENGTH_SHORT
								  )
							.show()
					}
				} ,
				onDismiss = {
					showObjectiveDialog.value = false
				})
	}
}