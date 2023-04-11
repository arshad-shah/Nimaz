package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.arshadshah.nimaz.utils.sunMoonUtils.MoonPhase
import com.arshadshah.nimaz.utils.sunMoonUtils.SunMoonCalc
import kotlinx.coroutines.delay
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
	val sharedPreferences = remember { PrivateSharedPreferences(context) }
	LaunchedEffect(key1 = Unit) {
		settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.LoadLocation(context))
		viewModel.handleEvent(context , PrayerTimesViewModel.PrayerTimesEvent.RELOAD)
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

	LaunchedEffect(locationName.value) {
		//update the prayer times
		viewModel.handleEvent(
				context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
				PrayerTimesParamMapper.getParams(context)
																				  )
							 )
		viewModel.handleEvent(
				context ,
				PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
						context
																   )
							 )
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

	val deviceTimeFormat = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
	//if the device time format is 24 hour then use the 24 hour format
	val formatter = if (deviceTimeFormat)
	{
		DateTimeFormatter.ofPattern("HH:mm")
	} else
	{
		DateTimeFormatter.ofPattern("hh:mm a")
	}


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

				MoonPhaseImage(
						image = phaseOfMoon.phaseSvg
							  )
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
							text = nextPrayerTime.value.format(formatter) ,
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

@Composable
fun MoonPhaseImage(image : Int)
{
	//a composable to show the moon phase image
	//it takes the image as an argument
	//and shows it in a circular shape
	//with a white background
	//and a black border
	Box(
			modifier = Modifier
				.size(40.dp)
				.border(
						width = 1.dp ,
						color = MaterialTheme.colorScheme.outline ,
						shape = CircleShape
					   )
				.clip(shape = CircleShape)
	   ) {
		Image(
				painter = painterResource(id = image) ,
				contentDescription = "Moon Phase Image" ,
				modifier = Modifier
					.size(40.dp)
					.background(color = Color.White.copy(alpha = 0.8f))
					.clip(shape = CircleShape)
			 )
	}
}

@Preview
@Composable
fun MoonPhaseImagePreview()
{
	val fraction = remember { mutableStateOf(0.0) }
	//list of phases
	val phases = listOf(
			MoonPhase.NEW_MOON ,
			MoonPhase.WAXING_CRESCENT ,
			MoonPhase.FIRST_QUARTER ,
			MoonPhase.WAXING_GIBBOUS ,
			MoonPhase.FULL_MOON ,
			MoonPhase.WANING_GIBBOUS ,
			MoonPhase.LAST_QUARTER ,
			MoonPhase.WANING_CRESCENT
					   )
	val currentPhase = remember { mutableStateOf(phases[0]) }
	val percentage = (fraction.value * 100).toInt()
	val imageToShow = when (currentPhase.value)
	{
		MoonPhase.NEW_MOON ->
		{
			R.drawable.new_moon
		}

		MoonPhase.WAXING_CRESCENT ->
		{
			//get the image to show
			when (percentage)
			{
				in 0 .. 10 -> R.drawable.waxing_cresent_7
				in 10 .. 20 -> R.drawable.waxing_cresent_14
				in 20 .. 30 -> R.drawable.waxing_cresent_21
				in 30 .. 40 -> R.drawable.waxing_cresent_29
				in 40 .. 50 -> R.drawable.waxing_cresent_36
				else -> R.drawable.waxing_cresent_36
			}
		}

		MoonPhase.FIRST_QUARTER ->
		{
			R.drawable.first_quarter_moon
		}

		MoonPhase.WAXING_GIBBOUS ->
		{
			//get the image to show
			when (percentage)
			{
				in 50 .. 60 -> R.drawable.waxing_gib_57
				in 60 .. 70 -> R.drawable.waxing_gib_64
				in 70 .. 80 -> R.drawable.waxing_gib_71
				in 80 .. 90 -> R.drawable.waxing_gib_78
				in 90 .. 100 -> R.drawable.waxing_gib_86
				else -> R.drawable.waxing_gib_71
			}
		}

		MoonPhase.FULL_MOON ->
		{
			R.drawable.full_moon
		}

		MoonPhase.WANING_GIBBOUS ->
		{
			val percentageProcessed = 100 - percentage
			//get the image to show
			when (percentageProcessed)
			{
				in 0 .. 10 -> R.drawable.wanning_gib_7
				in 10 .. 20 -> R.drawable.wanning_gib_14
				in 20 .. 30 -> R.drawable.wanning_gib_21
				in 30 .. 40 -> R.drawable.wanning_gib_29
				in 40 .. 50 -> R.drawable.wanning_gib_36
				in 50 .. 60 -> R.drawable.wanning_gib_43
				else -> R.drawable.wanning_gib_36
			}
		}

		MoonPhase.LAST_QUARTER ->
		{
			R.drawable.last_quarter_moon
		}

		MoonPhase.WANING_CRESCENT ->
		{
			val percentageProcessed = 100 - percentage
			//get the image to show
			when (percentageProcessed)
			{
				in 50 .. 60 -> R.drawable.wanning_cres_57
				in 60 .. 70 -> R.drawable.wanning_cres_64
				in 70 .. 80 -> R.drawable.wanning_cres_71
				in 80 .. 90 -> R.drawable.wanning_cres_78
				in 90 .. 100 -> R.drawable.wanning_cres_86
				else -> R.drawable.wanning_cres_93
			}
		}
	}
	val dateOfCurrentPhase = remember { mutableStateOf(LocalDateTime.now()) }
	//one hundred days  to chewck the moon phase over
	val hundredDays = 30
	//get one hundred dates
	val dates = remember {
		mutableStateOf(
				(0 .. hundredDays).map {
					LocalDateTime.now().plusDays(it.toLong())
				}
					  )
	}
	//get a list of moon phases
	val moonPhases = dates.value.map {
		SunMoonCalc(
				latitude = 53.7 ,
				longitude = - 7.35
				   ).getMoonPhase(it)
	}
	//a slider to change date so that the moon phase changes
	//and we can see the different moon phases
	Column(
			modifier = Modifier.fillMaxSize() ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
		  ) {

		//loop through the list of moon phases and get the fraction and phase
		//print the date
		LaunchedEffect(key1 = Unit) {
			moonPhases.forEachIndexed { index , moonPhase ->
				fraction.value = moonPhase.fraction
				currentPhase.value = moonPhase.phaseName
				dateOfCurrentPhase.value = dates.value[index]
				delay(1000)
			}
		}
		Text(text = "Date: ${dateOfCurrentPhase.value}")
		Text(text = "Fraction: ${fraction.value}")
		Text(text = "Percentage: ${percentage}")
		Text(text = "Phase: ${currentPhase.value}")
		Spacer(modifier = Modifier.height(10.dp))
		MoonPhaseImage(image = imageToShow)
	}
}

