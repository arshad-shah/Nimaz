package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.ListSkeletonLoader
import com.arshadshah.nimaz.ui.components.ui.loaders.loadingShimmerEffect
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.PrayerTimesListUI
import java.time.LocalDateTime


@Composable
fun PrayerTimesList(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    viewModel: PrayerTimesViewModel = PrayerTimesViewModel(context),
) {
    val state = viewModel.prayerTimesListState.collectAsState().value
    when (state) {
        is PrayerTimesViewModel.PrayerTimesListState.Loading -> {
            ListSkeletonLoader(brush = loadingShimmerEffect())
        }
        is PrayerTimesViewModel.PrayerTimesListState.Success -> {
            val prayerTimes = state.prayerTimes
            val prayerTimesMap = mutableMapOf<String, LocalDateTime?>()
            prayerTimesMap["fajr"] = prayerTimes!!.fajr
            prayerTimesMap["sunrise"] = prayerTimes.sunrise
            prayerTimesMap["dhuhr"] = prayerTimes.dhuhr
            prayerTimesMap["asr"] = prayerTimes.asr
            prayerTimesMap["maghrib"] = prayerTimes.maghrib
            prayerTimesMap["isha"] = prayerTimes.isha

            PrayerTimesListUI(modifier, prayerTimesMap)
        }
        is PrayerTimesViewModel.PrayerTimesListState.Error -> {
            PrayerTimesListUI(modifier, mapOf())
            Text(text = state.errorMessage)
        }
    }
}