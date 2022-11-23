package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.data.remote.viewModel.LocationViewModel
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.LocationTimeContainerUI

@Preview
@Composable
fun LocationTimeContainer(viewModel: LocationViewModel = LocationViewModel(LocalContext.current)) {
    when (val state = viewModel.location.collectAsState().value) {
        is LocationViewModel.LocationState.Loading -> LocationTimeContainerUI("Loading...")
        is LocationViewModel.LocationState.Success -> LocationTimeContainerUI(state.location)
        is LocationViewModel.LocationState.Error -> LocationTimeContainerUI(state.errorMessage)
    }
}