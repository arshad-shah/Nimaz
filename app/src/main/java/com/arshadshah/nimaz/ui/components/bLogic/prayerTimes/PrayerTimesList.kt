package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.ListSkeletonLoader
import com.arshadshah.nimaz.ui.components.ui.loaders.loadingShimmerEffect
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.PrayerTimesListUI
import es.dmoral.toasty.Toasty
import java.time.LocalDateTime


@Composable
fun PrayerTimesList(
	modifier : Modifier = Modifier ,
	state : State<PrayerTimesViewModel.PrayerTimesState> ,
	paddingValues : PaddingValues ,
				   )
{
	when (val prayerTimesState = state.value)
	{
		is PrayerTimesViewModel.PrayerTimesState.Loading ->
		{
			ListSkeletonLoader(brush = loadingShimmerEffect())
		}

		is PrayerTimesViewModel.PrayerTimesState.Success ->
		{
			val prayerTimes = prayerTimesState.prayerTimes
			val prayerTimesMap = mutableMapOf<String , LocalDateTime?>()
			prayerTimesMap["fajr"] = prayerTimes !!.fajr
			prayerTimesMap["sunrise"] = prayerTimes.sunrise
			prayerTimesMap["dhuhr"] = prayerTimes.dhuhr
			prayerTimesMap["asr"] = prayerTimes.asr
			prayerTimesMap["maghrib"] = prayerTimes.maghrib
			prayerTimesMap["isha"] = prayerTimes.isha

			prayerTimes.currentPrayer?.let {
				PrayerTimesListUI(modifier ,
								  prayerTimesMap ,
								  it.name ,
								  paddingValues)
			}
		}

		is PrayerTimesViewModel.PrayerTimesState.Error ->
		{
			//empty map to avoid null pointer exception
			val prayerTimesMap = mutableMapOf<String , LocalDateTime?>()
			PrayerTimesListUI(modifier , prayerTimesMap , "No connection" , paddingValues)
		}
	}
}