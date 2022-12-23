package com.arshadshah.nimaz.ui.components.ui.compass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BearingAndLocationContainerUI(location : String , bearing : String)
{
	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.height(IntrinsicSize.Max)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
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
					heading = "Bearing°" , text = bearing
					  )
		}
	}
}

@Preview
@Composable
fun BearingAndLocationContainerUIPreview()
{
	BearingAndLocationContainerUI(location = "Location" , bearing = "Bearing")
}