package com.arshadshah.nimaz.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.BannerSmall
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.common.LocationTopBar
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayerTimesCard
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayerTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardQuranTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardRandomAyatCard
import com.arshadshah.nimaz.ui.components.dashboard.DashboardTasbihTracker
import com.arshadshah.nimaz.ui.components.dashboard.EidUlAdhaCard
import com.arshadshah.nimaz.ui.components.dashboard.EidUlFitrCard
import com.arshadshah.nimaz.ui.components.dashboard.RamadanCard
import com.arshadshah.nimaz.ui.components.dashboard.RamadanTimesCard
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.DashboardViewModel
import java.time.LocalDateTime

@Composable
fun Dashboard(
    onNavigateToTracker: () -> Unit,
    onNavigateToCalender: () -> Unit,
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
    paddingValues: PaddingValues,
    onNavigateToTasbihListScreen: () -> Unit,
    onNavigateToAyatScreen: (String, Boolean, String, Int) -> Unit,
    context: Activity,
) {
    val viewModel: DashboardViewModel = viewModel(
        key = "dashboard_viewmodel",
        initializer = { DashboardViewModel(context.applicationContext) }
    )

    // Initialize data when the composable is first launched
    LaunchedEffect(Unit) {
        viewModel.initializeData(context)
    }

    // Collect all states from ViewModel using collectAsState
    val dashboardState = with(viewModel) {
        object {
            val isFastingToday by isFasting.collectAsState()
            val updateAvailable by isUpdateAvailable.collectAsState()
            val location by locationName.collectAsState()
            val fajrPrayerTime by fajrTime.collectAsState()
            val maghribPrayerTime by maghribTime.collectAsState()
            val prayerTracker by trackerState.collectAsState()
            val isLoadingData by isLoading.collectAsState()
            val quranBookmarks by bookmarks.collectAsState()
            val suraList by surahList.collectAsState()
            val tasbihListData by tasbihList.collectAsState()
            val isErrored by isError.collectAsState()
            val errorMessage by error.collectAsState()
            val nextPrayerNameValue by nextPrayerName.collectAsState()
            val nextPrayerTimeValue by nextPrayerTime.collectAsState()
            val countDownTimer by countDownTime.collectAsState()
            val randomAya = randomAyaState
        }
    }

    val stateScroll = rememberLazyListState()

    LazyColumn(
        state = stateScroll,
        modifier = Modifier.testTag(TEST_TAG_HOME),
        contentPadding = paddingValues
    ) {
        item{
            LocationTopBar(dashboardState.location, dashboardState.isLoadingData)
        }
        item {
            Log.d("Dashboard", "Dashboard Composable recomposed ${dashboardState.countDownTimer}")
            DashboardPrayerTimesCard(
                nextPrayerName = dashboardState.nextPrayerNameValue,
                nextPrayerTime = dashboardState.nextPrayerTimeValue,
                countDownTimer = dashboardState.countDownTimer,
            )
        }
        item {
            val isOpen = remember {
                mutableStateOf(dashboardState.isErrored)
            }
            BannerLarge(
                variant = BannerVariant.Error,
                title = "Error",
                message = dashboardState.errorMessage,
                isOpen = isOpen,
                showFor = 0,
                onDismiss = {
                    isOpen.value = false
                },
            )
        }

        item {
            RamadanTimesCard(
                isFasting = dashboardState.isFastingToday,
                location = dashboardState.location,
                fajrTime = dashboardState.fajrPrayerTime,
                maghribTime = dashboardState.maghribPrayerTime
            )
        }

        item {
            if (dashboardState.updateAvailable) {
                UpdateBanner(
                    context = context,
                    onUpdateClick = {
                        viewModel.handleEvent(
                            DashboardViewModel.DashboardEvent.CheckUpdate(context, true)
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
                isLoading = remember { mutableStateOf(dashboardState.isLoadingData) },
                dashboardPrayerTracker = dashboardState.prayerTracker
            )
        }

        item {
            if(dashboardState.quranBookmarks.isNotEmpty()){
                DashboardQuranTracker(
                    suraList = dashboardState.suraList,
                    onNavigateToAyatScreen = onNavigateToAyatScreen,
                    quranBookmarks = remember { mutableStateOf(dashboardState.quranBookmarks) },
                    handleEvents = viewModel::handleEvent,
                    isLoading = remember { mutableStateOf(dashboardState.isLoadingData) }
                )
            }

            if(dashboardState.tasbihListData.isNotEmpty()){
                DashboardTasbihTracker(
                    onNavigateToTasbihScreen = onNavigateToTasbihScreen,
                    onNavigateToTasbihListScreen = onNavigateToTasbihListScreen,
                    tasbihList = dashboardState.tasbihListData,
                    handleEvents = viewModel::handleEvent,
                    isLoading = remember { mutableStateOf(dashboardState.isLoadingData) }
                )
            }
        }

        item {
            Log.d("RandomAyaState", "Random Aya State: ${dashboardState.randomAya.value}")
            DashboardRandomAyatCard(
                onNavigateToAyatScreen = onNavigateToAyatScreen,
                randomAya = dashboardState.randomAya.value,
                isLoading = dashboardState.isLoadingData
            )
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
        title = "Update Available",
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