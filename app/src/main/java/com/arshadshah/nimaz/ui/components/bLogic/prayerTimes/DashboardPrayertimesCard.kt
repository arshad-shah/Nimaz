package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
fun DashboardPrayertimesCard(){

	val context = LocalContext.current
	val viewModel = viewModel(key = "PrayerTimesViewModel", initializer = { PrayerTimesViewModel() }, viewModelStoreOwner = context as ComponentActivity)
	val settingViewModel = viewModel(key = "SettingViewModel", initializer = { SettingsViewModel(context) }, viewModelStoreOwner = context as ComponentActivity)
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
		nextPrayerTime.value.atZone(java.time.ZoneId.systemDefault())
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
				.padding(16.dp)
				.fillMaxWidth() ,
				) {
		Row {
			Text(
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth() ,
					textAlign = TextAlign.Center ,
					text = HijrahDate.from(LocalDate.now()).format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
		}
		Row(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth() ,
				verticalAlignment = Alignment.CenterVertically ,
				horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			Column(
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth(0.5f) ,
					verticalArrangement = Arrangement.SpaceEvenly,
					horizontalAlignment = Alignment.CenterHorizontally
				  ) {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(8.dp) ,
						horizontalArrangement = Arrangement.SpaceEvenly,
						verticalAlignment = Alignment.CenterVertically
				   ){
					Text(text = nextPrayerName.value.first() + nextPrayerName.value.substring(1)
						.lowercase(
								Locale.ROOT
								  ))
					Text(text = nextPrayerTime.value.format(DateTimeFormatter.ofPattern("hh:mm a")))
				}
				Text(text = "-${timer.value.hours}:${timer.value.minutes}:${timer.value.seconds}")
			}
				Image(
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth(0.5f) ,
						painter = when(nextPrayerName.value) {
							"SUNRISE" -> {
								painterResource(id = R.drawable.sunrise_icon)
							}
							"FAJR" -> {
								painterResource(id = R.drawable.fajr_icon)
							}
							"DHUHR" -> {
								painterResource(id = R.drawable.dhuhr_icon)
							}
							"ASR" -> {
								painterResource(id = R.drawable.asr_icon)
							}
							"MAGHRIB" -> {
								painterResource(id = R.drawable.maghrib_icon)
							}
							"ISHA" -> {
								painterResource(id = R.drawable.isha_icon)
							}
							else -> {
								painterResource(id = R.drawable.sunrise_icon)
							}
						} ,
						contentDescription = "Next Prayer Icon"
					 )
		}
	}
}

@Preview
@Composable
fun DashboardPrayertimesCardPreview() {
	DashboardPrayertimesCard()
}