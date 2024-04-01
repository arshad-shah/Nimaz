package com.arshadshah.nimaz.ui.components.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.arshadshah.nimaz.data.local.models.CountDownTime
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardPrayertimesCard() {

    val context = LocalContext.current

    val viewModel = viewModel(
        key = PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel(context) },
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

    val prayerTimesState = viewModel.prayerTimesState.collectAsState(initial = null)
    val nextPrayerName = prayerTimesState.value?.nextPrayerName ?: "Loading..."

    val nextPrayerTime = prayerTimesState.value?.nextPrayerTime ?: LocalDateTime.now()

    // if next prayer time is not in today then show the next prayer name as Fajr Tomorrow
    val newNextPrayerName = if (nextPrayerTime.toLocalDate() != LocalDate.now()) {
        "Fajr Tomorrow"
    } else {
        nextPrayerName.first()
            .uppercase() + nextPrayerName.substring(1)
            .lowercase(
                Locale.ROOT
            )
    }

    val timer = prayerTimesState.value?.countDownTime

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
        nextPrayerTime?.atZone(java.time.ZoneId.systemDefault())
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
            .padding(4.dp)
            .fillMaxWidth()
            .testTag(TEST_TAG_HOME_PRAYER_TIMES_CARD),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(4.dp),
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
                        painter = when (nextPrayerName) {
                            "Sunrise" -> {
                                painterResource(id = R.drawable.sunrise_icon)
                            }

                            "Fajr" -> {
                                painterResource(id = R.drawable.fajr_icon)
                            }

                            "Dhuhr" -> {
                                painterResource(id = R.drawable.dhuhr_icon)
                            }

                            "Asr" -> {
                                painterResource(id = R.drawable.asr_icon)
                            }

                            "Maghrib" -> {
                                painterResource(id = R.drawable.maghrib_icon)
                            }

                            "Isha" -> {
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
                        text = nextPrayerTime.format(formatter),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Badge(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ){
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .placeholder(
                                    visible = false,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp),
                                    highlight = PlaceholderHighlight.shimmer(
                                        highlightColor = Color.White,
                                    )
                                ),
                            text = getTimerText(timer),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

//a function to return in text how much time is left for the next prayer
fun getTimerText(timeToNextPrayer: CountDownTime?): String {
    if (timeToNextPrayer == null) {
        return "No Timer set"
    }
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