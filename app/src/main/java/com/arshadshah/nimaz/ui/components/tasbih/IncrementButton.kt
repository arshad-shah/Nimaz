package com.arshadshah.nimaz.ui.components.tasbih

import android.content.Context
import android.os.VibrationEffect
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import es.dmoral.toasty.Toasty

@Composable
fun IncrementButton(
	count : MutableState<Int> ,
	lap : MutableState<Int> ,
	lapCountCounter : MutableState<Int> ,
	objective : MutableState<String> ,
	context : Context ,
				   )
{
	val context = LocalContext.current
	val viewModel = viewModel(
			 key = AppConstants.TASBIH_VIEWMODEL_KEY ,
			 initializer = { TasbihViewModel(context) } ,
			 viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	val vibrationAllowed = remember {
		viewModel.vibrationButtonState
	}.collectAsState()
	val vibrator = viewModel.vibrator

	ElevatedButton(
			 elevation = ButtonDefaults.elevatedButtonElevation(
					  defaultElevation = 4.dp
															   ) ,
			 contentPadding = PaddingValues(48.dp) ,
			 onClick = {
				 if (vibrationAllowed.value)
				 {
					 Log.d("Nimaz: vibration" , "vibrating")
					 vibrator.vibrate(
							  VibrationEffect.createOneShot(
									   50 ,
									   VibrationEffect.DEFAULT_AMPLITUDE
														   )
									 )
				 } else
				 {
					 Log.d("Nimaz: vibration" , "can't vibrate")
					 //can't vibrate
					 vibrator.cancel()
				 }
				 count.value = count.value + 1
				 lapCountCounter.value ++
				 if (lapCountCounter.value == objective.value.toInt())
				 {
					 if (vibrationAllowed.value)
					 {
						 Log.d("Nimaz: vibration" , "vibrating")
						 vibrator.vibrate(
								  VibrationEffect.createOneShot(
										   200 ,
										   VibrationEffect.DEFAULT_AMPLITUDE
															   )
										 )
					 } else
					 {
						 Log.d("Nimaz: vibration" , "can't vibrate")
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