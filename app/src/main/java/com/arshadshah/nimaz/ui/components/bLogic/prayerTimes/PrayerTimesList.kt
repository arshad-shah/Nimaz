package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import android.app.NotificationManager
import android.content.Context
import android.util.Log
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
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import es.dmoral.toasty.Toasty
import java.time.LocalDateTime


@Composable
fun PrayerTimesList(
	modifier : Modifier = Modifier ,
	state : State<PrayerTimesViewModel.PrayerTimesState> ,
	paddingValues : PaddingValues ,
				   )
{
	val context = LocalContext.current
	val sharedPreferences = PrivateSharedPreferences(context)
	val alarmLock = sharedPreferences.getDataBoolean("alarmLock", false)
	when (val prayerTimesState = state.value)
	{
		is PrayerTimesViewModel.PrayerTimesState.Loading ->
		{
			ListSkeletonLoader(brush = loadingShimmerEffect())
		}

		is PrayerTimesViewModel.PrayerTimesState.Success ->
		{

			//delete previous values from shared preferences
			sharedPreferences.removeData("fajr")
			sharedPreferences.removeData("sunrise")
			sharedPreferences.removeData("dhuhr")
			sharedPreferences.removeData("asr")
			sharedPreferences.removeData("maghrib")
			sharedPreferences.removeData("isha")

			val prayerTimes = prayerTimesState.prayerTimes
			val prayerTimesMap = mutableMapOf<String , LocalDateTime?>()
			prayerTimesMap["fajr"] = prayerTimes !!.fajr
			prayerTimesMap["sunrise"] = prayerTimes.sunrise
			prayerTimesMap["dhuhr"] = prayerTimes.dhuhr
			prayerTimesMap["asr"] = prayerTimes.asr
			prayerTimesMap["maghrib"] = prayerTimes.maghrib
			prayerTimesMap["isha"] = prayerTimes.isha

			//save the prayer times in shared preferences
			sharedPreferences.saveData("fajr", prayerTimes.fajr.toString())
			sharedPreferences.saveData("sunrise", prayerTimes.sunrise.toString())
			sharedPreferences.saveData("dhuhr", prayerTimes.dhuhr.toString())
			sharedPreferences.saveData("asr", prayerTimes.asr.toString())
			sharedPreferences.saveData("maghrib", prayerTimes.maghrib.toString())
			sharedPreferences.saveData("isha", prayerTimes.isha.toString())


			if (!alarmLock)
			{
				CreateAlarms().exact(context , prayerTimes.fajr!!,prayerTimes.sunrise!!,prayerTimes.dhuhr!!,prayerTimes.asr!!,prayerTimes.maghrib!!,prayerTimes.isha!!)
				sharedPreferences.saveDataBoolean("alarmLock", true)
			}

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

			Log.e("PrayerTimesList" , "Error: ${prayerTimesState.errorMessage}")

			Toasty.error(LocalContext.current ,
						 prayerTimesState.errorMessage ,
						 Toast.LENGTH_SHORT ,
						 true).show()
		}
	}
}