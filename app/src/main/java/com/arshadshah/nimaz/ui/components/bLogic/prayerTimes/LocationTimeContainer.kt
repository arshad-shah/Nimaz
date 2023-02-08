package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.compass.CustomText
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import java.util.*

@Composable
fun LocationTimeContainer(
	locationState : PrayerTimesViewModel.LocationState ,
	currentPrayerName : MutableState<String> ,
						 )
{

	when (val location = locationState)
	{
		is PrayerTimesViewModel.LocationState.Loading ->
		{
			ContainerUI(
					currentPrayerNameSentenceCase = "Fajr" ,
					isLoading = true ,
					location = "Loading"
					   )
		}

		is PrayerTimesViewModel.LocationState.Success ->
		{
			val currentPrayerNameSentenceCase = currentPrayerName.value
				.substring(0 , 1)
				.uppercase(Locale.ROOT) + currentPrayerName.value
				.substring(1).lowercase(Locale.ROOT)

			ContainerUI(
					currentPrayerNameSentenceCase = currentPrayerNameSentenceCase ,
					isLoading = false ,
					location = location.location
					   )
		}

		is PrayerTimesViewModel.LocationState.Error ->
		{
			ContainerUI(
					currentPrayerNameSentenceCase = "" ,
					isLoading = false ,
					location = "Error"
					   )
		}
	}
}

@Composable
fun ContainerUI(
	currentPrayerNameSentenceCase : String ,
	isLoading : Boolean ,
	location : String ,
			   )
{
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
					textModifier = Modifier.placeholder(
							visible = isLoading ,
							color = MaterialTheme.colorScheme.outline ,
							shape = RoundedCornerShape(4.dp) ,
							highlight = PlaceholderHighlight.shimmer(
									highlightColor = Color.White ,
																	)
													   ) ,
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
					textModifier = Modifier.placeholder(
							visible = isLoading ,
							color = MaterialTheme.colorScheme.outline ,
							shape = RoundedCornerShape(4.dp) ,
							highlight = PlaceholderHighlight.shimmer(
									highlightColor = Color.White ,
																	)
													   ) ,
					heading = "Current Prayer" ,
					//fix the name to be sentence case,
					text = currentPrayerNameSentenceCase
					  )
		}
	}
}