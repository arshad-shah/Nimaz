package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.LocationTopBar
import com.arshadshah.nimaz.ui.components.dashboard.getTimerText
import com.arshadshah.nimaz.ui.components.prayerTimes.AnimatedArcView
import com.arshadshah.nimaz.ui.components.prayerTimes.ArcViewState
import com.arshadshah.nimaz.ui.components.prayerTimes.NextPrayerTimerText
import com.arshadshah.nimaz.ui.components.prayerTimes.PrayerTimesList
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import java.time.format.DateTimeFormatter

private const val SCREEN_WIDTH_THRESHOLD = 720

@Composable
fun PrayerTimesScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel(context) },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )

    val prayerTimesState by viewModel.prayerTimesState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val screenWidth = context.resources.displayMetrics.widthPixels

    LaunchedEffect(Unit) {
        viewModel.apply {
            handleEvent(PrayerTimesViewModel.PrayerTimesEvent.Init(context))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        LocationTopBar(prayerTimesState.locationName, isLoading)
        Box(
            modifier = Modifier
                .weight(3f)
                .fillMaxWidth()
        ) {
            if (!isLoading) {
                PrayerTimesHeader(
                    prayerTimesState = prayerTimesState,
                    showArc = screenWidth > SCREEN_WIDTH_THRESHOLD,
                    isLoading = false
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(7f),
            verticalArrangement = Arrangement.Bottom
        ) {
            item {
                PrayerTimesList(
                    prayerTimesState = prayerTimesState,
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
private fun PrayerTimesHeader(
    prayerTimesState: PrayerTimesViewModel.PrayerTimesState,
    showArc: Boolean,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        if (showArc) {
            AnimatedArcView(
                state = ArcViewState(
                    timePoints = listOf(
                        prayerTimesState.fajrTime,
                        prayerTimesState.sunriseTime,
                        prayerTimesState.dhuhrTime,
                        prayerTimesState.asrTime,
                        prayerTimesState.maghribTime,
                        prayerTimesState.ishaTime
                    ),
                    countDownTime = prayerTimesState.countDownTime
                )
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            NextPrayerTimerText(
                prayerNameDisplay = prayerTimesState.nextPrayerName,
                nextPrayerTimeDisplay = prayerTimesState.nextPrayerTime.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                ),
                timerText = getTimerText(prayerTimesState.countDownTime),
                isLoading = isLoading,
                horizontalPosition = Alignment.CenterHorizontally
            )
        }
    }
}