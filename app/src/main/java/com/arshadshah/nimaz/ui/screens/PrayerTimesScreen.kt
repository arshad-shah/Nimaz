package com.arshadshah.nimaz.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_SUNRISE
import com.arshadshah.nimaz.ui.components.prayerTimes.PrayerTimesList
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel

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

    Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG, "currentPrayerName: $currentPrayerName")

    val mapOfPrayerNameToImagePainterResource = mapOf(
        PRAYER_NAME_FAJR to R.drawable.fajr_back,
        PRAYER_NAME_SUNRISE to R.drawable.sunrise_back,
        PRAYER_NAME_DHUHR to R.drawable.dhuhr_back,
        PRAYER_NAME_ASR to R.drawable.asr_back,
        PRAYER_NAME_MAGHRIB to R.drawable.maghrib_back,
        PRAYER_NAME_ISHA to R.drawable.isha_back,
    )

    val backgroundImagePainter = remember(currentPrayerName) {
        mapOfPrayerNameToImagePainterResource[currentPrayerName] ?: R.drawable.fajr_back
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(id = backgroundImagePainter),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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