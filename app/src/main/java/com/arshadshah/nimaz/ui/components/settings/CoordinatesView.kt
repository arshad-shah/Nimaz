package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.CustomText

@Composable
fun CoordinatesView(latitudeState : State<Double> , longitudeState : State<Double>)
{
	//round the latitude and longitude to 4 decimal places
	val latitudeRounded = String.format("%.4f" , latitudeState.value)
	val longitudeRounded = String.format("%.4f" , longitudeState.value)

	ElevatedCard(
			 colors = CardDefaults.elevatedCardColors(
					  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 32.dp) ,
					  contentColor = MaterialTheme.colorScheme.onSurface ,
					  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
					  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
													 ) ,
			 shape = MaterialTheme.shapes.extraLarge ,
			 modifier = Modifier
				 .padding(8.dp)
				 .height(IntrinsicSize.Max)
				 .fillMaxWidth()
				 .testTag("coordinatesView")
				) {
		Row(
				 horizontalArrangement = Arrangement.Center ,
				 modifier = Modifier.fillMaxWidth() ,
				 verticalAlignment = Alignment.CenterVertically
		   ) {
			CustomText(
					 modifier = Modifier
						 .weight(0.5f)
						 .padding(8.dp) ,
					 textModifier = Modifier.testTag("latitudeText") ,
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
						 .padding(8.dp) ,
					 textModifier = Modifier.testTag("longitudeText") ,
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
	val longitude = remember { mutableStateOf(- 7.3) }
	CoordinatesView(latitude , longitude)
}