package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.ListSkeletonLoader
import com.arshadshah.nimaz.ui.components.ui.loaders.loadingShimmerEffect
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.PrayerTimesListUI
import org.json.JSONObject
import java.time.LocalDateTime

@Preview(showBackground = true)
@Composable
fun PrayerTimesList(
    modifier: Modifier = Modifier,
    viewModel: PrayerTimesViewModel = PrayerTimesViewModel(LocalContext.current)
) {
    when (val state = viewModel.prayerTimesListState.collectAsState().value) {
        is PrayerTimesViewModel.PrayerTimesListState.Loading -> {
            ListSkeletonLoader(brush = loadingShimmerEffect())
        }
        is PrayerTimesViewModel.PrayerTimesListState.Success -> {
            val prayerTimes = JSONObject(state.prayerTimes)
            val prayerTimesMap = mutableMapOf<String, LocalDateTime?>()
            prayerTimesMap["fajr"] = LocalDateTime.parse(prayerTimes.getString("fajr"))
            prayerTimesMap["sunrise"] = LocalDateTime.parse(prayerTimes.getString("sunrise"))
            prayerTimesMap["dhuhr"] = LocalDateTime.parse(prayerTimes.getString("dhuhr"))
            prayerTimesMap["asr"] = LocalDateTime.parse(prayerTimes.getString("asr"))
            prayerTimesMap["maghrib"] = LocalDateTime.parse(prayerTimes.getString("maghrib"))
            prayerTimesMap["isha"] = LocalDateTime.parse(prayerTimes.getString("isha"))

            PrayerTimesListUI(modifier, prayerTimesMap)
        }
        is PrayerTimesViewModel.PrayerTimesListState.Error -> {
            Text(text = state.errorMessage)
        }
    }
}