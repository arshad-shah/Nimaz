package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import android.os.VibrationEffect
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel

@Composable
fun Decrementbutton(
	count : MutableState<Int> ,
	lap : MutableState<Int> ,
	lapCountCounter : MutableState<Int> ,
	objective : MutableState<String> ,
				   )
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = TASBIH_VIEWMODEL_KEY ,
			initializer = { TasbihViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	val vibrationAllowed = remember {
		viewModel.vibrationButtonState
	}.collectAsState()
	val vibrator = viewModel.vibrator
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
					count.value = count.value - 1
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