package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import android.content.Context
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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