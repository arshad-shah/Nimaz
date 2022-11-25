package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.CurrentNextPrayerContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DatesContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.LocationTimeContainer
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.PrayerTimesList
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.LocationFinder
import com.arshadshah.nimaz.utils.location.NetworkChecker

@Composable
fun PrayerTimesScreen(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .wrapContentSize(Alignment.Center),
    ) {
        val context = LocalContext.current
        val viewModel = PrayerTimesViewModel(context)
        val state = viewModel.prayerTimesListState.collectAsState()

        val sharedPreferences = PrivateSharedPreferences(context)
        val locationAuto = sharedPreferences.getDataBoolean("location_auto", true)
        val location = remember {
            mutableStateOf(
                sharedPreferences.getData(
                    "location_input",
                    "Abbeyleix"
                )
            )
        }


        if (NetworkChecker().networkCheck(context)) {
            if (locationAuto) {
                val locationfinder = LocationFinder()
                val latitude = locationfinder.latitudeValue
                val longitude = locationfinder.longitudeValue
                locationfinder.findCityName(context, latitude, longitude)
                location.value = sharedPreferences.getData("location_input", "Abbeyleix")
            } else {
                Location().getManualLocation(location.value, context)
                location.value = sharedPreferences.getData("location_input", "Abbeyleix")
            }
        } else {
            location.value = "No Internet"
        }

        LocationTimeContainer()
        DatesContainer()
        CurrentNextPrayerContainer(state = state)
        PrayerTimesList(state = state)
    }
}
