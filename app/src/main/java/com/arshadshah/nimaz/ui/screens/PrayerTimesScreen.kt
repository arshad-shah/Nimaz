package com.arshadshah.nimaz.ui.screens

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.ui.components.common.CompactLocationTopBar
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
    val prayerTimesState by viewModel.prayerTimesState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    //get screen width
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Log.d("PrayerTimesScreen", "Screen Width: $screenWidth and Screen Height: $screenHeight")

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
                CompactLocationTopBar(prayerTimesState.locationName, isLoading)

            // Prayer Times Card
            DashboardPrayerTimesCard(
                nextPrayerName = prayerTimesState.nextPrayerName,
                countDownTimer = prayerTimesState.countDownTime,
                nextPrayerTime = prayerTimesState.nextPrayerTime,
                isLoading = isLoading,
                timeFormat = DateTimeFormatter.ofPattern("hh:mm a"),
                height = if(screenHeight < 900.dp) 150.dp else 200.dp
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