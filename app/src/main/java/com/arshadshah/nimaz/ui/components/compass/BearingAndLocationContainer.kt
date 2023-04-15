package com.arshadshah.nimaz.ui.components.compass

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.CustomText
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QiblaViewModel
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
			val location = sharedPref.getData(AppConstants.LOCATION_INPUT , "")
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


@Composable
fun BearingAndLocationContainerUI(location : String , heading : String)
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(8.dp)
				.height(IntrinsicSize.Max)
				) {
		//align items to center

		Row(
				horizontalArrangement = Arrangement.Center ,
				modifier = Modifier.fillMaxWidth() ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			//only allow 50% of the width for the location text
			CustomText(
					modifier = Modifier
						.weight(0.5f)
						.padding(8.dp) ,
					heading = "Location" , text = location
					  )
			//vertical divider line
			Divider(
					modifier = Modifier
						.fillMaxHeight()
						.width(1.dp) ,
					color = MaterialTheme.colorScheme.outline
				   )
			CustomText(
					modifier = Modifier
						.weight(0.5f)
						.padding(8.dp) ,
					heading = "Heading" , text = heading
					  )
		}
	}
}
