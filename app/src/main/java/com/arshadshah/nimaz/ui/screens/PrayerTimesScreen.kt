package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.ui.components.common.LocationTopBar
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayerTimesCard
import com.arshadshah.nimaz.ui.components.prayerTimes.PrayerTimesList
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import java.time.format.DateTimeFormatter

@Composable
fun PrayerTimesScreen(
    navController: NavHostController,
    viewModel: PrayerTimesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prayerTimesState by viewModel.prayerTimesState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    //get screen width
    LocalConfiguration.current.screenHeightDp.dp

    LaunchedEffect(Unit) {
        viewModel.handleEvent(PrayerTimesViewModel.PrayerTimesEvent.Init(context = context))
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            LocationTopBar(prayerTimesState.locationName, isLoading)

            // Prayer Times Card
            DashboardPrayerTimesCard(
                currentPrayerPeriod = prayerTimesState.currentPrayerName,
                nextPrayerName = prayerTimesState.nextPrayerName,
                countDownTimer = prayerTimesState.countDownTime,
                nextPrayerTime = prayerTimesState.nextPrayerTime,
                isLoading = isLoading,
                timeFormat = DateTimeFormatter.ofPattern("hh:mm a"),
            )

            LazyColumn(
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
}