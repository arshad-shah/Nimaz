package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.ListSkeletonLoader
import com.arshadshah.nimaz.ui.components.ui.loaders.loadingShimmerEffect
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.PrayerTimesListUI
import java.time.LocalDateTime


@Composable
fun PrayerTimesList(
	modifier : Modifier = Modifier ,
	state : State<PrayerTimesViewModel.PrayerTimesState> ,
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
								  it.name)
			}
		}

		is PrayerTimesViewModel.PrayerTimesState.Error ->
		{
			PrayerTimesListUI(modifier , mapOf() , "")
			Toast.makeText(
					LocalContext.current ,
					prayerTimesState.errorMessage ,
					Toast.LENGTH_SHORT
						  ).show()
		}
	}
}