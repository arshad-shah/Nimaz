package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import android.content.Context
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
import es.dmoral.toasty.Toasty

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
				count.value = count.value + 1
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