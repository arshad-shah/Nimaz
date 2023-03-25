package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel

@Composable
fun IncrementDecrement(
	count : MutableState<Int> ,
	lap : MutableState<Int> ,
	lapCountCounter : MutableState<Int> ,
	objective : MutableState<String> ,
					  )
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "TasbihViewModel" ,
			initializer = { TasbihViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	val rOrl = remember {
		viewModel.orientationButtonState
	}.collectAsState()

	//if rorl is 0 then switch the place of the increment and decrement buttons to right side and if its 1 then switch the place of the increment and decrement buttons to left side
	if (rOrl.value)
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
						   )

			Spacer(modifier = Modifier.width(16.dp))
			IncrementButton(
					count = count ,
					lap = lap ,
					lapCountCounter = lapCountCounter ,
					objective = objective ,
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
					context = context
						   )
			Spacer(modifier = Modifier.width(16.dp))
			Decrementbutton(
					count = count ,
					lap = lap ,
					lapCountCounter = lapCountCounter ,
					objective = objective ,
						   )

		}
	}
}