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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TIMES_VIEWMODEL_KEY
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME_PRAYER_TIMES_CARD
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_NEXT_PRAYER_ICON_DASHBOARD
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.sunMoonUtils.SunMoonCalc
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
			key = PRAYER_TIMES_VIEWMODEL_KEY ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = context as ComponentActivity
							 )
	val settingViewModel = viewModel(
			key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
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

	val locationName = remember {
		settingViewModel.locationName
	}.collectAsState()

	val latitude = remember {
		settingViewModel.latitude
	}.collectAsState()

	val longitude = remember {
		settingViewModel.longitude
	}.collectAsState()

	val isLoading = remember {
		viewModel.isLoading
	}.collectAsState()

	val sharedPreferences = remember { PrivateSharedPreferences(context) }

	LaunchedEffect(locationName.value) {
		//update the prayer times
		viewModel.handleEvent(
				context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
				PrayerTimesParamMapper.getParams(context)
																				  )
							 )
	}

	LaunchedEffect(key1 = Unit) {
		//set the alarms
		val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK , false)
		if (! alarmLock)
		{
			viewModel.handleEvent(
					context ,
					PrayerTimesViewModel.PrayerTimesEvent.SET_ALARMS(context)
								 )
			sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , true)
		}
	}

	val phaseOfMoon = SunMoonCalc(
			latitude = latitude.value ,
			longitude = longitude.value
								 ).getMoonPhase()


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
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(top = 8.dp , bottom = 0.dp , start = 8.dp , end = 8.dp)
				.fillMaxWidth()
				.testTag(TEST_TAG_HOME_PRAYER_TIMES_CARD)
				.clickable {
					onNavigateToPrayerTimes()
				} ,
				) {
		Column(
				modifier = Modifier
					.padding(8.dp) ,
				verticalArrangement = Arrangement.SpaceBetween ,
				horizontalAlignment = Alignment.CenterHorizontally
			  ) {
			Row(
					modifier = Modifier
						.padding(horizontal = 8.dp)
						.fillMaxWidth() ,
					verticalAlignment = Alignment.CenterVertically ,
					horizontalArrangement = Arrangement.SpaceBetween
			   ) {
				Column(
						modifier = Modifier.padding(4.dp) ,
						verticalArrangement = Arrangement.SpaceBetween ,
						horizontalAlignment = Alignment.CenterHorizontally
					  ) {
					Text(
							modifier = Modifier
								.padding(4.dp) ,
							textAlign = TextAlign.Start ,
							text = LocalDate.now()
								.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
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
				}
				if (isLoading.value)
				{
					Text(text = "Loading..." , style = MaterialTheme.typography.titleMedium)
				} else
				{
					//process the location name to show only 10 characters and add ... if more than 10 characters
					val locationNameValue = locationName.value
					val locationNameValueLength = locationNameValue.length
					val locationNameValueSubstring = locationNameValue.substring(
							0 ,
							if (locationNameValueLength > 10) 10 else locationNameValueLength
																				)
					val locationNameValueFinal =
						if (locationNameValueLength > 10) "$locationNameValueSubstring..." else locationNameValueSubstring
					Text(
							text = locationNameValueFinal ,
							style = MaterialTheme.typography.titleMedium
						)
				}

				//emoji for moon phase
				Text(text = phaseOfMoon.phaseSvg , style = MaterialTheme.typography.headlineLarge)
			}
			Row(
					modifier = Modifier
						.fillMaxWidth() ,
					verticalAlignment = Alignment.CenterVertically ,
					horizontalArrangement = Arrangement.SpaceBetween
			   ) {
				Box(
						modifier = Modifier
							.clip(MaterialTheme.shapes.extraLarge)
							.size(100.dp)
				   ) {
					Image(
							modifier = Modifier
								.size(100.dp)
								.testTag(TEST_TAG_NEXT_PRAYER_ICON_DASHBOARD) ,
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
							text = getTimerText(timer.value) ,
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

//a function to return in text how much time is left for the next prayer
fun getTimerText(timeToNextPrayer : CountDownTime) : String
{
	return when
	{
		timeToNextPrayer.hours > 1 ->
		{
			//check if there are minutes left
			if (timeToNextPrayer.minutes > 1)
			{
				"${timeToNextPrayer.hours} hours ${timeToNextPrayer.minutes} minutes Left"
			} else if (timeToNextPrayer.minutes == 1L)
			{
				"${timeToNextPrayer.hours} hours ${timeToNextPrayer.minutes} minute Left"
			} else
			{
				"${timeToNextPrayer.hours} hours Left"
			}
		}

		timeToNextPrayer.hours == 1L ->
		{
			//check if there are minutes left
			if (timeToNextPrayer.minutes > 1)
			{
				"${timeToNextPrayer.hours} hour ${timeToNextPrayer.minutes} minutes Left"
			} else if (timeToNextPrayer.minutes == 1L)
			{
				//check if there are seconds left
				if (timeToNextPrayer.seconds > 1)
				{
					"${timeToNextPrayer.hours} hour ${timeToNextPrayer.minutes} minute ${timeToNextPrayer.seconds} seconds Left"
				} else if (timeToNextPrayer.seconds == 1L)
				{
					"${timeToNextPrayer.hours} hour ${timeToNextPrayer.minutes} minute ${timeToNextPrayer.seconds} second Left"
				} else
				{
					"${timeToNextPrayer.hours} hour ${timeToNextPrayer.minutes} minute Left"
				}
			} else
			{
				"${timeToNextPrayer.hours} hour Left"
			}
		}

		timeToNextPrayer.hours == 0L && timeToNextPrayer.minutes > 1 ->
		{
			//check if there are seconds left
			if (timeToNextPrayer.seconds > 1)
			{
				"${timeToNextPrayer.minutes} minutes ${timeToNextPrayer.seconds} seconds Left"
			} else if (timeToNextPrayer.seconds == 1L)
			{
				"${timeToNextPrayer.minutes} minutes ${timeToNextPrayer.seconds} second Left"
			} else
			{
				"${timeToNextPrayer.minutes} minutes Left"
			}
		}

		timeToNextPrayer.hours == 0L && timeToNextPrayer.minutes == 1L ->
		{
			//check if there are seconds left
			if (timeToNextPrayer.seconds > 1)
			{
				"${timeToNextPrayer.minutes} minute ${timeToNextPrayer.seconds} seconds Left"
			} else if (timeToNextPrayer.seconds == 1L)
			{
				"${timeToNextPrayer.minutes} minute ${timeToNextPrayer.seconds} second Left"
			} else
			{
				"${timeToNextPrayer.minutes} minute Left"
			}
		}

		timeToNextPrayer.hours == 0L && timeToNextPrayer.minutes == 0L && timeToNextPrayer.seconds > 1 ->
		{
			"${timeToNextPrayer.seconds} seconds Left"
		}

		timeToNextPrayer.hours == 0L && timeToNextPrayer.minutes == 0L && timeToNextPrayer.seconds == 1L ->
		{
			"${timeToNextPrayer.seconds} second Left"
		}

		else ->
		{
			"Prayer Time"
		}
	}
}