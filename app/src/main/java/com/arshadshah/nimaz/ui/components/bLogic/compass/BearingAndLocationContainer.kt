package com.arshadshah.nimaz.ui.components.bLogic.compass

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QiblaViewModel
import com.arshadshah.nimaz.ui.components.ui.compass.BearingAndLocationContainerUI
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty

@Composable
fun BearingAndLocationContainer(state : State<QiblaViewModel.QiblaState>)
{

	//get the context
	val context = LocalContext.current
	//get the shared preferences
	val sharedPref = PrivateSharedPreferences(context)
	when (val qiblaState = state.value)
	{
		is QiblaViewModel.QiblaState.Loading ->
		{
			//show a loading indicator
			BearingAndLocationContainerUI(location = "Loading..." , heading = "Loading...")

		}

		is QiblaViewModel.QiblaState.Success ->
		{
			//get the location
			val location = sharedPref.getData(AppConstants.LOCATION_INPUT , "Abbeyleix")
			//round the bearing to 2 decimal places
			val bearing = qiblaState.bearing !!.toString().substring(0 , 5)
			val compassDirection = bearingToCompassDirection(qiblaState.bearing.toFloat())
			val heading = "$bearing° $compassDirection"
			Log.d(AppConstants.QIBLA_COMPASS_SCREEN_TAG , "BearingAndLocationContainer: $heading")
			//show the bearing and location
			BearingAndLocationContainerUI(location , heading)
		}

		is QiblaViewModel.QiblaState.Error ->
		{
			BearingAndLocationContainerUI(location = "Error" , heading = "Error")
			Toasty.error(LocalContext.current , qiblaState.errorMessage).show()
		}
	}
}


//a function that turns a bearing into actual compass direction with a heading
fun bearingToCompassDirection(bearing : Float) : String
{
	val directions = arrayOf("N" , "NE" , "E" , "SE" , "S" , "SW" , "W" , "NW" , "N")
	val index = ((bearing + 22.5f) / 45f).toInt()
	return directions[index % 8]
}