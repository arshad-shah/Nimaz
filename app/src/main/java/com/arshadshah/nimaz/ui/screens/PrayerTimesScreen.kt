package com.arshadshah.nimaz.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.SETTINGS_VIEWMODEL_KEY
import com.arshadshah.nimaz.ui.components.prayerTimes.DatesContainer
import com.arshadshah.nimaz.ui.components.prayerTimes.LocationTimeContainer
import com.arshadshah.nimaz.ui.components.prayerTimes.PrayerTimesList
import com.arshadshah.nimaz.utils.api.PrayerTimesParamMapper
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel

@Composable
fun PrayerTimesScreen(
    paddingValues: PaddingValues,
    onNavigateToTracker: () -> Unit,
) {
    val context = LocalContext.current

    val viewModel = viewModel(
        key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel() },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
    val settingViewModel = viewModel(
        key = SETTINGS_VIEWMODEL_KEY,
        initializer = { SettingsViewModel(context) },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )

    //reload the data when the screen is resumed
    LaunchedEffect(Unit) {
        settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.LoadLocation(context))
        viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.RELOAD)
    }

    val locationState = remember { settingViewModel.locationName }.collectAsState()
    val latitude = remember { settingViewModel.latitude }.collectAsState()
    val longitude = remember { settingViewModel.longitude }.collectAsState()


    LaunchedEffect(locationState.value, latitude.value, longitude.value) {
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
    val currentPrayerName = remember {
        viewModel.currentPrayerName
    }.collectAsState()

    Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG, "locationState: $locationState")
    Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG, "currentPrayerName: $currentPrayerName")
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = getBackgroundImage(currentPrayerName.value),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Bottom
        ) {
            item {
                PrayerTimesList()
            }
        }
    }

}

@Composable
fun getBackgroundImage(currentPrayerName : String) : Painter
{
    return when (currentPrayerName)
    {
        "fajr" -> painterResource(id = R.drawable.fajr_back)
        "sunrise" -> painterResource(id = R.drawable.sunrise_back)
        "dhuhr" -> painterResource(id = R.drawable.dhuhr_back)
        "asr" -> painterResource(id = R.drawable.asr_back)
        "maghrib" -> painterResource(id = R.drawable.maghrib_back)
        "isha" -> painterResource(id = R.drawable.isha_back)
        else -> painterResource(id = R.drawable.fajr_back)
    }
}