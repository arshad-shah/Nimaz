package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun DashboardPrayertimesCard(onNavigateToPrayerTimes : () -> Unit)
{

	val context = LocalContext.current
	val viewModel = viewModel(
			key = "PrayerTimesViewModel" ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = context as ComponentActivity
							 )
	val settingViewModel = viewModel(
			key = "SettingViewModel" ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = context
									)
	LaunchedEffect(Unit) {
		settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.LoadLocation(context))
		viewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.RELOAD)
	}

	val nextPrayerName = remember {
		viewModel.nextPrayerName
	}.collectAsState()

	val nextPrayerTime = remember {
		viewModel.nextPrayerTime
	}.collectAsState()

	val timer = remember {
		viewModel.timer
	}.collectAsState()

	val timeToNextPrayerLong =
		nextPrayerTime.value?.atZone(java.time.ZoneId.systemDefault())
			?.toInstant()
			?.toEpochMilli()
	val currentTime =
		LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
			.toEpochMilli()

	val difference = timeToNextPrayerLong?.minus(currentTime)

	viewModel.handleEvent(
			context ,
			PrayerTimesViewModel.PrayerTimesEvent.Start(difference !!)
						 )

	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth()
				.clickable {
					onNavigateToPrayerTimes()
				} ,
				) {
		Column(
				modifier = Modifier
					.padding(8.dp) ,
				verticalArrangement = Arrangement.SpaceEvenly ,
				horizontalAlignment = Alignment.CenterHorizontally
			  ) {
			Text(
					modifier = Modifier
						.padding(4.dp) ,
					textAlign = TextAlign.Start ,
					text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
					style = MaterialTheme.typography.titleMedium
				)
			Text(
					modifier = Modifier
						.padding(4.dp) ,
					textAlign = TextAlign.Start ,
					text = HijrahDate.from(LocalDate.now())
						.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
					style = MaterialTheme.typography.titleSmall
				)
			Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 16.dp) ,
					verticalAlignment = Alignment.CenterVertically ,
					horizontalArrangement = Arrangement.SpaceBetween
			   ) {
				Box(
						modifier = Modifier
							.clip(MaterialTheme.shapes.extraLarge)
							.padding(8.dp)
							.size(100.dp)
				   ) {
					Image(
							modifier = Modifier
								.size(100.dp) ,
							painter = when (nextPrayerName.value)
							{
								"sunrise" ->
								{
									painterResource(id = R.drawable.sunrise_icon)
								}

								"fajr" ->
								{
									painterResource(id = R.drawable.fajr_icon)
								}

								"dhuhr" ->
								{
									painterResource(id = R.drawable.dhuhr_icon)
								}

								"asr" ->
								{
									painterResource(id = R.drawable.asr_icon)
								}

								"maghrib" ->
								{
									painterResource(id = R.drawable.maghrib_icon)
								}

								"isha" ->
								{
									painterResource(id = R.drawable.isha_icon)
								}

								else ->
								{
									painterResource(id = R.drawable.sunrise_icon)
								}
							} ,
							contentDescription = "Next Prayer Icon"
						 )
				}
				Column(
						modifier = Modifier.fillMaxWidth() ,
						verticalArrangement = Arrangement.Center ,
						horizontalAlignment = Alignment.CenterHorizontally
					  ) {
					Text(
							text = nextPrayerName.value.first()
								.uppercase() + nextPrayerName.value.substring(1)
								.lowercase(
										Locale.ROOT
										  ) ,
							style = MaterialTheme.typography.titleLarge
						)
					Text(
							text = nextPrayerTime.value.format(DateTimeFormatter.ofPattern("hh:mm a")) ,
							style = MaterialTheme.typography.titleLarge
						)
					Text(
							text = "-${timer.value.hours}:${timer.value.minutes}:${timer.value.seconds}" ,
							style = MaterialTheme.typography.titleMedium ,
							textAlign = TextAlign.Center ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(8.dp)
						)
				}
			}
		}
	}
}