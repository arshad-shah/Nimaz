package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
	state : State<PrayerTimesViewModel.PrayerTimesState> ,
						 )
{
	val currentPrayerNameSentenceCase = remember {
		mutableStateOf("Loading...")
	}
	if (! state.value.isLoading.value && state.value.prayerTimes.value != null)
	{
		//only allow 50% of the width for the time text
		currentPrayerNameSentenceCase.value =
			state.value.prayerTimes.value?.currentPrayer?.name?.substring(0 , 1)?.uppercase(
					Locale.ROOT
																						   ) + state.value.prayerTimes.value?.currentPrayer?.name?.substring(
					1
																																							)
				?.lowercase(Locale.ROOT)
	}
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
							visible = state.value.isLoading.value || state.value.prayerTimes.value == null ,
							color = MaterialTheme.colorScheme.outline ,
							shape = RoundedCornerShape(4.dp) ,
							highlight = PlaceholderHighlight.shimmer(
									highlightColor = Color.White ,
																	)
													   ) ,
					heading = "Location" , text = state.value.location.value.toString()
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
							visible = state.value.isLoading.value || state.value.prayerTimes.value == null ,
							color = MaterialTheme.colorScheme.outline ,
							shape = RoundedCornerShape(4.dp) ,
							highlight = PlaceholderHighlight.shimmer(
									highlightColor = Color.White ,
																	)
													   ) ,
					heading = "Current Prayer" ,
					//fix the name to be sentence case,
					text = currentPrayerNameSentenceCase.value
					  )
		}
	}
}