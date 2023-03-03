package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

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