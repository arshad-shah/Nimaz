package com.arshadshah.nimaz.ui.screens

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.BannerSmall
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.common.CompactLocationTopBar
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayerTimesCard
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayerTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardRandomAyatCard
import com.arshadshah.nimaz.ui.components.dashboard.EidUlAdhaCard
import com.arshadshah.nimaz.ui.components.dashboard.EidUlFitrCard
import com.arshadshah.nimaz.ui.components.dashboard.RamadanCard
import com.arshadshah.nimaz.ui.components.dashboard.RamadanTimesCard
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.DashboardEvent
import com.arshadshah.nimaz.viewModel.DashboardViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Dashboard(
    onNavigateToCalender: () -> Unit,
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
    onNavigateToTasbihListScreen: () -> Unit,
    onNavigateToAyatScreen: (String, Boolean, String, Int) -> Unit,
    context: Activity,
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initializeData(context)
        viewModel.handleEvent(DashboardEvent.CheckUpdate(context))
    }

    val locationState by viewModel.locationState.collectAsState()
    val prayerTimesState by viewModel.prayerTimesState.collectAsState()
    val trackerState by viewModel.trackerState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val quranState by viewModel.quranState.collectAsState()

    val stateScroll = rememberLazyListState()

    Scaffold(bottomBar = { BottomNavigationBar(navController) }) {
        LazyColumn(
            state = stateScroll,
            modifier = Modifier.testTag(TEST_TAG_HOME),
            contentPadding = it
        ) {
            item {
                // Location Section
                when (locationState.isLoading) {
                    true -> CompactLocationTopBar(
                        locationName = "Loading location...",
                        isLoading = true
                    )

                    false -> CompactLocationTopBar(
                        locationName = locationState.locationName.ifEmpty { "Location unavailable" },
                        isLoading = false
                    )
                }

                if (locationState.error != null) {
                    LocationRetryButton {
                        viewModel.handleEvent(DashboardEvent.LoadLocation)
                    }
                }
            }

            item {
                // Prayer Times Card
                DashboardPrayerTimesCard(
                    nextPrayerName = prayerTimesState.nextPrayer,
                    countDownTimer = prayerTimesState.countDownTime,
                    nextPrayerTime = prayerTimesState.nextPrayerTime,
                    isLoading = prayerTimesState.isLoading,
                    timeFormat = DateTimeFormatter.ofPattern("hh:mm a")
                )
            }

            // Error Banner
            if (prayerTimesState.error != null || trackerState.error != null ||
                locationState.error != null || updateState.error != null ||
                quranState.error != null
            ) {
                item {
                    val isOpen = remember { mutableStateOf(true) }
                    BannerLarge(
                        variant = BannerVariant.Error,
                        title = "Error",
                        message = listOfNotNull(
                            prayerTimesState.error,
                            trackerState.error,
                            locationState.error,
                            updateState.error,
                            quranState.error
                        ).first(),
                        isOpen = isOpen,
                        showFor = 0,
                        onDismiss = {
                            isOpen.value = false
                            viewModel.clearError()
                        }
                    )
                }
            }

            item {
                RamadanTimesCard(
                    isFasting = trackerState.isFasting,
                    location = locationState.locationName,
                    fajrTime = trackerState.fajrTime,
                    maghribTime = trackerState.maghribTime
                )
            }

            // Update Banner
            if (updateState.isUpdateAvailable) {
                item {
                    UpdateBanner(
                        context = context,
                        onUpdateClick = {
                            viewModel.handleEvent(
                                DashboardEvent.StartUpdate(context)
                            )
                        }
                    )
                }
            }

            item {
                RamadanCard(onNavigateToCalender)
                EidUlFitrCard(onNavigateToCalender)
                EidUlAdhaCard(onNavigateToCalender)
            }

            item {
                DashboardPrayerTracker(
                    handleEvents = viewModel::handleEvent,
                    isLoading = remember { mutableStateOf(trackerState.isLoading) },
                    dashboardPrayerTracker = trackerState
                )
            }

            item {
                DashboardRandomAyatCard(
                    onNavigateToAyatScreen = onNavigateToAyatScreen,
                    randomAya = quranState.randomAyaState,
                    isLoading = quranState.isLoading
                )
            }
        }
    }
}

@Composable
private fun UpdateBanner(
    context: Activity,
    onUpdateClick: () -> Unit
) {
    val sharedPref = PrivateSharedPreferences(context.applicationContext)
    val bannerShownLastTime = sharedPref.getData(
        "Update Available-bannerIsOpen-time",
        LocalDateTime.now().toString()
    )

    val has24HoursPassed = LocalDateTime.now().isAfter(
        LocalDateTime.parse(bannerShownLastTime).plusHours(2)
    )

    val isOpen = remember { mutableStateOf(true) }

    if (has24HoursPassed) {
        sharedPref.saveDataBoolean("Update Available-bannerIsOpen", true)
    }

    BannerSmall(
        title = "New Update Available",
        message = "Tap here to update the app",
        isOpen = isOpen,
        onClick = onUpdateClick,
        dismissable = true,
        paddingValues = PaddingValues(
            top = 8.dp,
            bottom = 0.dp,
            start = 8.dp,
            end = 8.dp
        )
    )
}

@Composable
private fun LocationRetryButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text("Retry Location")
    }
}