package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.ui.compass.CustomText
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun CoordinatesView(latitude : MutableState<Double>, longitude : MutableState<Double>)
{
	//round the latitude and longitude to 4 decimal places
	val latitudeRounded = String.format("%.4f" , latitude.value)
	val longitudeRounded = String.format("%.4f" , longitude.value)

	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.height(IntrinsicSize.Max)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				.fillMaxWidth()
				) {
		Row(
				horizontalArrangement = Arrangement.Center ,
				modifier = Modifier.fillMaxWidth() ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			CustomText(
					modifier = Modifier
						.weight(0.5f)
						.padding(8.dp)  ,
					text = latitudeRounded ,
					heading = "Latitude"
					  )
			Divider(
					modifier = Modifier
						.fillMaxHeight()
						.width(1.dp) ,
					color = MaterialTheme.colorScheme.outline
				   )
			CustomText(
					modifier = Modifier
						.weight(0.5f)
						.padding(8.dp)  ,
					text = longitudeRounded ,
					heading = "Longitude"
					  )
		}
	}
}

@Preview
@Composable
fun CoordinatesViewPreview()
{
	val latitude = remember { mutableStateOf(53.4) }
	val longitude = remember { mutableStateOf(-7.3) }
	CoordinatesView(latitude , longitude)
}