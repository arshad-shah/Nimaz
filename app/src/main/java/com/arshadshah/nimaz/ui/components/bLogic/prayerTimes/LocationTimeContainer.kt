package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.LocationTimeContainerUI

@Composable
fun LocationTimeContainer(
	location : MutableState<String?> ,
	currentTimeName : String? ,
						 )
{
		if (currentTimeName != null && location.value != null)
		{
			LocationTimeContainerUI(location = location.value!!, currentTimeName = currentTimeName)
		}
}