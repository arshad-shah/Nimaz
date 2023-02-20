package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.ui.compass.CustomText
import java.util.*

@Composable
fun LocationTimeContainer(
	locationState : String ,
	currentPrayerName : State<String> ,
						 )
{
			val currentPrayerNameSentenceCase = currentPrayerName.value
				.substring(0 , 1)
				.uppercase(Locale.ROOT) + currentPrayerName.value
				.substring(1).lowercase(Locale.ROOT)

			ContainerUI(
					currentPrayerNameSentenceCase = currentPrayerNameSentenceCase ,
					location = locationState ,
					   )
}

@Composable
fun ContainerUI(
	currentPrayerNameSentenceCase : String ,
	location : String ,
			   )
{
	val context = LocalContext.current
	ElevatedCard(
			modifier = Modifier
				.padding(vertical = 8.dp , horizontal = 0.dp)
				.height(IntrinsicSize.Max)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
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
						.padding(8.dp),
					heading = "Current Prayer" ,
					//fix the name to be sentence case,
					text = currentPrayerNameSentenceCase
					  )
		}
	}
}