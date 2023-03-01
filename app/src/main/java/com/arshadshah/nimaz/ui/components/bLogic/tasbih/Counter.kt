package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Counter(
	vibrator : Vibrator ,
	paddingValues : PaddingValues ,
	vibrationAllowed : MutableState<Boolean> ,
	count : MutableState<Int> ,
	reset : MutableState<Boolean> ,
	showResetDialog : MutableState<Boolean> ,
	rOrl : MutableState<Int> ,
	integrated : Boolean = false
		   )
{

	//get all the values from the shared preferences

	val context = LocalContext.current
	val objective = remember {
		mutableStateOf(
				context.getSharedPreferences("tasbih" , 0).getString("objective" , "33") !!
					  )
	}

	val showObjectiveDialog = remember { mutableStateOf(false) }

	//lap counter
	val lap =
		remember { mutableStateOf(context.getSharedPreferences("tasbih" , 0).getInt("lap" , 0)) }
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

	val viewModel = viewModel(key = "TasbihViewModel", initializer = { TasbihViewModel(context) }, viewModelStoreOwner = LocalContext.current as ComponentActivity)
	val tasbih = if(integrated) remember {
		viewModel.tasbih
	}.collectAsState() else null

	LaunchedEffect(key1 = lap.value){
		if(integrated){
			viewModel.handleEvent(TasbihViewModel.TasbihEvent.UpdateTasbih(Tasbih(
					id = tasbih?.value?.id!!,
					date = tasbih.value.date,
					arabicName = tasbih.value.arabicName,
					englishName = tasbih.value.englishName,
					translationName = tasbih.value.translationName,
					goal = tasbih.value.goal,
					completed = lap.value,
					isCompleted = tasbih.value.isCompleted
																				 )))
		}
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
		//objective text
		if (integrated)
		{
			Text(
					modifier = Modifier
						.align(Alignment.CenterHorizontally) ,
					text = tasbih?.value?.goal.toString() ,
					style = MaterialTheme.typography.bodyMedium ,
					color = MaterialTheme.colorScheme.onSurface
				)
		}

		if(!integrated){
			Editbutton(
					count = count ,
					context = LocalContext.current ,
					showObjectiveDialog = showObjectiveDialog ,
					objective = objective ,
					  )
		}

		Spacer(modifier = Modifier.height(32.dp))
		IncrementDecrement(
				vibrator = vibrator ,
				vibrationAllowed = vibrationAllowed ,
				count = count ,
				lap = lap ,
				lapCountCounter = lapCountCounter ,
				objective = objective ,
				context = LocalContext.current ,
				rOrl = rOrl ,
						  )
	}

	if (showResetDialog.value)
	{
		AlertDialog(
				onDismissRequest = { showResetDialog.value = false } ,
				title = { Text(text = "Reset Counter") } ,
				text = {
					Text(
							text = "Are you sure you want to reset the counter?" ,
							style = MaterialTheme.typography.titleLarge
						)
				} ,
				confirmButton = {
					Button(onClick = {
						count.value = 0
						lap.value = 1
						lapCountCounter.value = 0

						reset.value = true

						showResetDialog.value = false
					}) {
						Text(text = "Reset" , style = MaterialTheme.typography.titleLarge)
					}
				} ,
				dismissButton = {
					TextButton(onClick = { showResetDialog.value = false }) {
						Text(text = "Cancel" , style = MaterialTheme.typography.titleLarge)
					}
				}
				   )
	}

	if (showObjectiveDialog.value)
	{
		AlertDialog(
				onDismissRequest = { showObjectiveDialog.value = false } ,
				title = { Text(text = "Set Tasbih Objective") } ,
				text = {
					Spacer(modifier = Modifier.height(16.dp))
					OutlinedTextField(
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
										style = MaterialTheme.typography.titleLarge
									)
							} ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 16.dp) ,
							keyboardActions = KeyboardActions(
									onDone = {
										if (objective.value.toInt() > 0)
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
									})
									 )
				} ,
				confirmButton = {
					Button(onClick = {
						showObjectiveDialog.value = false
					}) {
						Text(text = "Set" , style = MaterialTheme.typography.titleLarge)
					}
				} ,
				dismissButton = {
					TextButton(onClick = { showObjectiveDialog.value = false }) {
						Text(text = "Cancel" , style = MaterialTheme.typography.titleLarge)
					}
				}
				   )
	}
}

@Composable
fun Editbutton(
	count : MutableState<Int> ,
	context : Context ,
	showObjectiveDialog : MutableState<Boolean> ,
	objective : MutableState<String> ,
			  )
{
	Row(
			modifier = Modifier.fillMaxWidth() ,
			horizontalArrangement = Arrangement.SpaceBetween ,
			verticalAlignment = Alignment.CenterVertically
	   ) {
		ElevatedButton(
				modifier = Modifier.shadow(3.dp , RoundedCornerShape(50)) ,
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
						showObjectiveDialog.value = true
					}
				}) {
			Row(
					horizontalArrangement = Arrangement.Start ,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				Text(
						modifier = Modifier.padding(8.dp) ,
						text = objective.value ,
						style = MaterialTheme.typography.titleLarge ,
						fontSize = 26.sp
					)
				Icon(
						modifier = Modifier.size(24.dp) ,
						painter = painterResource(id = R.drawable.edit_icon) ,
						contentDescription = "Edit"
					)
			}
		}
	}
}

@Composable
fun IncrementDecrement(
	count : MutableState<Int> ,
	lap : MutableState<Int> ,
	lapCountCounter : MutableState<Int> ,
	objective : MutableState<String> ,
	vibrationAllowed : MutableState<Boolean> ,
	vibrator : Vibrator ,
	context : Context ,
	rOrl : MutableState<Int> ,
					  )
{
	//if rorl is 0 then switch the place of the increment and decrement buttons to right side and if its 1 then switch the place of the increment and decrement buttons to left side
	if (rOrl.value == 0)
	{
		Row(
				modifier = Modifier.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceEvenly ,
				verticalAlignment = Alignment.CenterVertically
		   ) {

			Decrementbutton(
					count = count ,
					lap = lap ,
					lapCountCounter = lapCountCounter ,
					objective = objective ,
					vibrationAllowed = vibrationAllowed ,
					vibrator = vibrator ,
						   )

			Spacer(modifier = Modifier.width(16.dp))
			IncrementButton(
					count = count ,
					lap = lap ,
					lapCountCounter = lapCountCounter ,
					objective = objective ,
					vibrationAllowed = vibrationAllowed ,
					vibrator = vibrator ,
					context = context
						   )
		}
	} else
	{
		Row(
				modifier = Modifier.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceEvenly ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			IncrementButton(
					count = count ,
					lap = lap ,
					lapCountCounter = lapCountCounter ,
					objective = objective ,
					vibrationAllowed = vibrationAllowed ,
					vibrator = vibrator ,
					context = context
						   )
			Spacer(modifier = Modifier.width(16.dp))
			Decrementbutton(
					count = count ,
					lap = lap ,
					lapCountCounter = lapCountCounter ,
					objective = objective ,
					vibrationAllowed = vibrationAllowed ,
					vibrator = vibrator ,
						   )

		}
	}
}


@Composable
fun IncrementButton(
	count : MutableState<Int> ,
	lap : MutableState<Int> ,
	lapCountCounter : MutableState<Int> ,
	objective : MutableState<String> ,
	vibrationAllowed : MutableState<Boolean> ,
	vibrator : Vibrator ,
	context : Context ,
				   )
{
	ElevatedButton(
			contentPadding = PaddingValues(38.dp) ,
			modifier = Modifier.shadow(5.dp , RoundedCornerShape(50)) ,
			onClick = {
				if (vibrationAllowed.value)
				{
					vibrator.vibrate(
							VibrationEffect.createOneShot(
									50 ,
									VibrationEffect.DEFAULT_AMPLITUDE
														 )
									)
				} else
				{
					//can't vibrate
					vibrator.cancel()
				}
				count.value ++
				lapCountCounter.value ++
				if (lapCountCounter.value == objective.value.toInt())
				{
					if (vibrationAllowed.value)
					{
						vibrator.vibrate(
								VibrationEffect.createOneShot(
										200 ,
										VibrationEffect.DEFAULT_AMPLITUDE
															 )
										)
					} else
					{
						//can't vibrate
						vibrator.cancel()
					}
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
			}) {
		Icon(
				modifier = Modifier.size(48.dp) ,
				painter = painterResource(id = R.drawable.plus_icon) ,
				contentDescription = "Add"
			)
	}
}

@Composable
fun Decrementbutton(
	count : MutableState<Int> ,
	lap : MutableState<Int> ,
	lapCountCounter : MutableState<Int> ,
	objective : MutableState<String> ,
	vibrationAllowed : MutableState<Boolean> ,
	vibrator : Vibrator ,
				   )
{
	ElevatedButton(
			contentPadding = PaddingValues(16.dp) ,
			modifier = Modifier.shadow(5.dp , RoundedCornerShape(50)) ,
			onClick = {
				//count should not go below 0
				if (count.value > 0)
				{
					if (vibrationAllowed.value)
					{
						vibrator.vibrate(
								VibrationEffect.createOneShot(
										200 ,
										VibrationEffect.DEFAULT_AMPLITUDE
															 )
										)
					} else
					{
						//can't vibrate
						vibrator.cancel()
					}
					count.value --
					if (count.value == objective.value.toInt())
					{
						if (vibrationAllowed.value)
						{
							vibrator.vibrate(
									VibrationEffect.createOneShot(
											200 ,
											VibrationEffect.DEFAULT_AMPLITUDE
																 )
											)
						} else
						{
							//can't vibrate
							vibrator.cancel()
						}
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
		Icon(
				modifier = Modifier.size(24.dp) ,
				painter = painterResource(id = R.drawable.minus_icon) ,
				contentDescription = "Delete"
			)
	}

}