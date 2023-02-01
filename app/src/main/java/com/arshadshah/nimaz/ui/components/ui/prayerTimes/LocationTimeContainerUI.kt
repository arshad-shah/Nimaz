package com.arshadshah.nimaz.ui.components.ui.prayerTimes

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.compass.CustomText
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import java.util.*

@Composable
fun LocationTimeContainerUI(
	location : String ,
	prayerTimesState : State<PrayerTimesViewModel.PrayerTimesState>
						   )
{
	val context = LocalContext.current
	// Initalising the view model
	val viewModel = PrayerTimesViewModel(context)
	val sharedPreferences = PrivateSharedPreferences(context)
	ElevatedCard(
			modifier = Modifier
				.padding(vertical = 8.dp , horizontal = 0.dp)
				.height(IntrinsicSize.Max)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				) {
		//align items to center

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
			//only allow 50% of the width for the time text
			when (prayerTimesState.value) {
				is PrayerTimesViewModel.PrayerTimesState.Loading -> {
					CustomText(
							modifier = Modifier
								.weight(0.5f)
								.padding(8.dp) ,
							heading = "Current Prayer" , text = "Loading..."
							  )
				}
				is PrayerTimesViewModel.PrayerTimesState.Error -> {
					CustomText(
							modifier = Modifier
								.weight(0.5f)
								.padding(8.dp) ,
							heading = "Current Prayer" , text = "Error"
							  )
				}
				is PrayerTimesViewModel.PrayerTimesState.Success -> {
					val prayerTimes = (prayerTimesState.value as PrayerTimesViewModel.PrayerTimesState.Success).prayerTimes
					//get the current prayer name
					//it is in format ASR
					//we need to get it in Sentence case
					val currentPrayerName = prayerTimes?.currentPrayer!!.name
					val currentPrayerNameSentenceCase = currentPrayerName.substring(0 , 1).toUpperCase(
							Locale.ROOT) + currentPrayerName.substring(1).toLowerCase(Locale.ROOT)
						CustomText(
								modifier = Modifier
									.weight(0.5f)
									.padding(8.dp) ,
								heading = "Current Prayer" ,
								//fix the name to be sentence case,
								text = currentPrayerNameSentenceCase
								  )
				}
			}
		}
	}
}