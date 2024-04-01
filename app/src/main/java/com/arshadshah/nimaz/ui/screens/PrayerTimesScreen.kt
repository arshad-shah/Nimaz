package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.dashboard.getTimerText
import com.arshadshah.nimaz.ui.components.prayerTimes.AnimatedArcView
import com.arshadshah.nimaz.ui.components.prayerTimes.NextPrayerTimerText
import com.arshadshah.nimaz.ui.components.prayerTimes.PrayerTimesList
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import java.time.format.DateTimeFormatter

@Composable
fun PrayerTimesScreen(
    paddingValues: PaddingValues,
) {
    val context = LocalContext.current

    val viewModel = viewModel(
        key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel(context) },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
    //reload the data when the screen is resumed
    LaunchedEffect(Unit) {
        viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.LOAD_LOCATION)
        viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.RELOAD)
    }

    val prayerTimesState = viewModel.prayerTimesState.collectAsState()
    val locationState = prayerTimesState.value.locationName
    val latitude = prayerTimesState.value.latitude
    val longitude = prayerTimesState.value.longitude

    val isLoading = viewModel.isLoading.collectAsState()
    val error = viewModel.error.collectAsState()


    LaunchedEffect(locationState, latitude, longitude) {
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
    val currentPrayerName = prayerTimesState.value.currentPrayerName
    val nextPrayerName = prayerTimesState.value.nextPrayerName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .weight(3f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (!isLoading.value) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(4.dp)
                    ) {
                        AnimatedArcView(
                            timePoints =
                            listOf(
                                prayerTimesState.value.fajrTime,
                                prayerTimesState.value.sunriseTime,
                                prayerTimesState.value.dhuhrTime,
                                prayerTimesState.value.asrTime,
                                prayerTimesState.value.maghribTime,
                                prayerTimesState.value.ishaTime
                            ),
                            prayerTimesState.value.countDownTime
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            NextPrayerTimerText(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                prayerNameDisplay = nextPrayerName,
                                nextPrayerTimeDisplay = prayerTimesState.value.nextPrayerTime.format(
                                    DateTimeFormatter.ofPattern("HH:mm")
                                ),
                                timerText = getTimerText(prayerTimesState.value.countDownTime),
                                isLoading = isLoading.value,
                                horizontalPosition = Alignment.CenterHorizontally
                            )
                        }
                    }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(7f),
            verticalArrangement = Arrangement.Bottom
        ) {
            item {
                PrayerTimesList(
                    prayerTimesState,
                    error,
                    isLoading,
                    handleEvents = viewModel::handleEvent
                )
            }
        }
    }
}