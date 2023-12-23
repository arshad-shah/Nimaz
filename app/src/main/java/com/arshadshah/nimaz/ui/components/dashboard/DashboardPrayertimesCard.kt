package com.arshadshah.nimaz.ui.components.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.api.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.sunMoonUtils.moon.MoonCalc
import com.arshadshah.nimaz.utils.sunMoonUtils.moon.MoonPhase
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardPrayertimesCard() {

    val context = LocalContext.current

    val viewModel = viewModel(
        key = PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel() },
        viewModelStoreOwner = context as ComponentActivity
    )
    val settingViewModel = viewModel(
        key = AppConstants.SETTINGS_VIEWMODEL_KEY,
        initializer = { SettingsViewModel(context) },
        viewModelStoreOwner = context
    )
    val sharedPreferences = remember { PrivateSharedPreferences(context) }
    LaunchedEffect(key1 = Unit) {
        settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.LoadLocation(context))
        viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.RELOAD)
        //set the alarms
        val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK, false)
        if (!alarmLock) {
            viewModel.handleEvent(
                context,
                PrayerTimesViewModel.PrayerTimesEvent.SET_ALARMS(context)
            )
            sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, true)
        }
    }

    val nextPrayerName = remember {
        viewModel.nextPrayerName
    }.collectAsState()

    val nextPrayerTime = remember {
        viewModel.nextPrayerTime
    }.collectAsState()

    // if next prayer time is not in today then show the next prayer name as Fajr Tomorrow
    val newNextPrayerName = if (nextPrayerTime.value.toLocalDate() != LocalDate.now()) {
        "Fajr Tomorrow"
    } else {
        nextPrayerName.value.first()
            .uppercase() + nextPrayerName.value.substring(1)
            .lowercase(
                Locale.ROOT
            )
    }

    val timer = remember {
        viewModel.timer
    }.collectAsState()

    val locationName = remember {
        settingViewModel.locationName
    }.collectAsState()


    LaunchedEffect(locationName.value) {
        //update the prayer times
        viewModel.handleEvent(
            context, PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                PrayerTimesParamMapper.getParams(context)
            )
        )
        viewModel.handleEvent(
            context,
            PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                context
            )
        )
    }


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
    val formatter = if (deviceTimeFormat) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("hh:mm a")
    }


    viewModel.handleEvent(
        context,
        PrayerTimesViewModel.PrayerTimesEvent.Start(difference!!)
    )

    Card(
        modifier = Modifier
			.padding(top = 8.dp, bottom = 0.dp, start = 8.dp, end = 8.dp)
			.fillMaxWidth()
			.testTag(TEST_TAG_HOME_PRAYER_TIMES_CARD),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
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
							.testTag(TEST_TAG_NEXT_PRAYER_ICON_DASHBOARD),
                        painter = when (nextPrayerName.value) {
                            "sunrise" -> {
                                painterResource(id = R.drawable.sunrise_icon)
                            }

                            "fajr" -> {
                                painterResource(id = R.drawable.fajr_icon)
                            }

                            "dhuhr" -> {
                                painterResource(id = R.drawable.dhuhr_icon)
                            }

                            "asr" -> {
                                painterResource(id = R.drawable.asr_icon)
                            }

                            "maghrib" -> {
                                painterResource(id = R.drawable.maghrib_icon)
                            }

                            "isha" -> {
                                painterResource(id = R.drawable.isha_icon)
                            }

                            else -> {
                                painterResource(id = R.drawable.sunrise_icon)
                            }
                        },
                        contentDescription = "Next Prayer Icon"
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = newNextPrayerName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = nextPrayerTime.value.format(formatter),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = getTimerText(timer.value),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
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
fun getTimerText(timeToNextPrayer: CountDownTime): String {
    return when {
        timeToNextPrayer.hours > 1 -> {
            //check if there are minutes left
            if (timeToNextPrayer.minutes > 1) {
                "${timeToNextPrayer.hours} hours ${timeToNextPrayer.minutes} minutes Left"
            } else if (timeToNextPrayer.minutes == 1L) {
                "${timeToNextPrayer.hours} hours ${timeToNextPrayer.minutes} minute Left"
            } else {
                "${timeToNextPrayer.hours} hours Left"
            }
        }

        timeToNextPrayer.hours == 1L -> {
            //check if there are minutes left
            if (timeToNextPrayer.minutes > 1) {
                "${timeToNextPrayer.hours} hour ${timeToNextPrayer.minutes} minutes Left"
            } else if (timeToNextPrayer.minutes == 1L) {
                //check if there are seconds left
                if (timeToNextPrayer.seconds > 1) {
                    "${timeToNextPrayer.hours} hour ${timeToNextPrayer.minutes} minute ${timeToNextPrayer.seconds} seconds Left"
                } else if (timeToNextPrayer.seconds == 1L) {
                    "${timeToNextPrayer.hours} hour ${timeToNextPrayer.minutes} minute ${timeToNextPrayer.seconds} second Left"
                } else {
                    "${timeToNextPrayer.hours} hour ${timeToNextPrayer.minutes} minute Left"
                }
            } else {
                "${timeToNextPrayer.hours} hour Left"
            }
        }

        timeToNextPrayer.hours == 0L && timeToNextPrayer.minutes > 1 -> {
            //check if there are seconds left
            if (timeToNextPrayer.seconds > 1) {
                "${timeToNextPrayer.minutes} minutes ${timeToNextPrayer.seconds} seconds Left"
            } else if (timeToNextPrayer.seconds == 1L) {
                "${timeToNextPrayer.minutes} minutes ${timeToNextPrayer.seconds} second Left"
            } else {
                "${timeToNextPrayer.minutes} minutes Left"
            }
        }

        timeToNextPrayer.hours == 0L && timeToNextPrayer.minutes == 1L -> {
            //check if there are seconds left
            if (timeToNextPrayer.seconds > 1) {
                "${timeToNextPrayer.minutes} minute ${timeToNextPrayer.seconds} seconds Left"
            } else if (timeToNextPrayer.seconds == 1L) {
                "${timeToNextPrayer.minutes} minute ${timeToNextPrayer.seconds} second Left"
            } else {
                "${timeToNextPrayer.minutes} minute Left"
            }
        }

        timeToNextPrayer.hours == 0L && timeToNextPrayer.minutes == 0L && timeToNextPrayer.seconds > 1 -> {
            "${timeToNextPrayer.seconds} seconds Left"
        }

        timeToNextPrayer.hours == 0L && timeToNextPrayer.minutes == 0L && timeToNextPrayer.seconds == 1L -> {
            "${timeToNextPrayer.seconds} second Left"
        }

        else -> {
            "Loading..."
        }
    }
}

@Composable
fun MoonPhaseImage(image: Int) {
    //a composable to show the moon phase image
    //it takes the image as an argument
    //and shows it in a circular shape
    //with a white background
    //and a black border
    Box(
        modifier = Modifier
			.size(40.dp)
			.border(
				width = 1.dp,
				color = MaterialTheme.colorScheme.outline,
				shape = CircleShape
			)
			.clip(shape = CircleShape)
    ) {
        Image(
            painter = painterResource(id = image),
            contentDescription = "Moon Phase Image",
            modifier = Modifier
				.size(40.dp)
				.background(color = Color.White.copy(alpha = 0.8f))
				.clip(shape = CircleShape)
        )
    }
}

@Preview
@Composable
fun MoonPhaseImagePreview() {
    val fraction = remember { mutableDoubleStateOf(0.0) }
    //list of phases
    val phases = listOf(
        MoonPhase.NEW_MOON,
        MoonPhase.WAXING_CRESCENT,
        MoonPhase.FIRST_QUARTER,
        MoonPhase.WAXING_GIBBOUS,
        MoonPhase.FULL_MOON,
        MoonPhase.WANING_GIBBOUS,
        MoonPhase.LAST_QUARTER,
        MoonPhase.WANING_CRESCENT
    )
    val currentPhase = remember { mutableStateOf(phases[0]) }
    val percentage = (fraction.value * 100).toInt()
    val imageToShow = when (currentPhase.value) {
        MoonPhase.NEW_MOON -> {
            R.drawable.new_moon
        }

        MoonPhase.WAXING_CRESCENT -> {
            //get the image to show
            when (percentage) {
                in 0..10 -> R.drawable.waxing_cresent_7
                in 10..20 -> R.drawable.waxing_cresent_14
                in 20..30 -> R.drawable.waxing_cresent_21
                in 30..40 -> R.drawable.waxing_cresent_29
                in 40..50 -> R.drawable.waxing_cresent_36
                else -> R.drawable.waxing_cresent_36
            }
        }

        MoonPhase.FIRST_QUARTER -> {
            R.drawable.first_quarter_moon
        }

        MoonPhase.WAXING_GIBBOUS -> {
            //get the image to show
            when (percentage) {
                in 50..60 -> R.drawable.waxing_gib_57
                in 60..70 -> R.drawable.waxing_gib_64
                in 70..80 -> R.drawable.waxing_gib_71
                in 80..90 -> R.drawable.waxing_gib_78
                in 90..100 -> R.drawable.waxing_gib_86
                else -> R.drawable.waxing_gib_71
            }
        }

        MoonPhase.FULL_MOON -> {
            R.drawable.full_moon
        }

        MoonPhase.WANING_GIBBOUS -> {
            //get the image to show
            when (100 - percentage) {
                in 0..10 -> R.drawable.wanning_gib_7
                in 10..20 -> R.drawable.wanning_gib_14
                in 20..30 -> R.drawable.wanning_gib_21
                in 30..40 -> R.drawable.wanning_gib_29
                in 40..50 -> R.drawable.wanning_gib_36
                in 50..60 -> R.drawable.wanning_gib_43
                else -> R.drawable.wanning_gib_36
            }
        }

        MoonPhase.LAST_QUARTER -> {
            R.drawable.last_quarter_moon
        }

        MoonPhase.WANING_CRESCENT -> {
            //get the image to show
            when (100 - percentage) {
                in 50..60 -> R.drawable.wanning_cres_57
                in 60..70 -> R.drawable.wanning_cres_64
                in 70..80 -> R.drawable.wanning_cres_71
                in 80..90 -> R.drawable.wanning_cres_78
                in 90..100 -> R.drawable.wanning_cres_86
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
            (0..hundredDays).map {
                LocalDateTime.now().plusDays(it.toLong())
            }
        )
    }
    //get a list of moon phases
    val moonPhases = dates.value.map {
        MoonCalc(
            latitude = 53.7,
            longitude = -7.35
        ).getMoonPhase(it)
    }
    //a slider to change date so that the moon phase changes
    //and we can see the different moon phases
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        //loop through the list of moon phases and get the fraction and phase
        //print the date
        LaunchedEffect(key1 = Unit) {
            moonPhases.forEachIndexed { index, moonPhase ->
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

