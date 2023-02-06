package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.PrayerTimesListUI
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import java.time.LocalDateTime


@Composable
fun PrayerTimesList(
	modifier : Modifier = Modifier ,
	prayerTimes : MutableState<PrayerTimes?> ,
	paddingValues : PaddingValues ,
	timerState : LiveData<CountDownTime> ,
	viewModel : PrayerTimesViewModel ,
	state : State<PrayerTimesViewModel.PrayerTimesState> ,
				   )
{
	val context = LocalContext.current
	val sharedPreferences = PrivateSharedPreferences(context)
	val prayerTimesMapState = remember {
		mutableMapOf(
				"fajr" to LocalDateTime.now() ,
				"sunrise" to LocalDateTime.now() ,
				"dhuhr" to LocalDateTime.now() ,
				"asr" to LocalDateTime.now() ,
				"maghrib" to LocalDateTime.now() ,
				"isha" to LocalDateTime.now() ,
					)
	}
	if (! state.value.isLoading.value && state.value.prayerTimes.value != null)
	{
		val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK , false)
		//delete previous values from shared preferences
		sharedPreferences.removeData(AppConstants.FAJR)
		sharedPreferences.removeData(AppConstants.SUNRISE)
		sharedPreferences.removeData(AppConstants.DHUHR)
		sharedPreferences.removeData(AppConstants.ASR)
		sharedPreferences.removeData(AppConstants.MAGHRIB)
		sharedPreferences.removeData(AppConstants.ISHA)
		sharedPreferences.removeData(AppConstants.CURRENT_PRAYER)

		prayerTimesMapState["fajr"] = prayerTimes.value?.fajr
		prayerTimesMapState["sunrise"] = prayerTimes.value?.sunrise
		prayerTimesMapState["dhuhr"] = prayerTimes.value?.dhuhr
		prayerTimesMapState["asr"] = prayerTimes.value?.asr
		prayerTimesMapState["maghrib"] = prayerTimes.value?.maghrib
		prayerTimesMapState["isha"] = prayerTimes.value?.isha


		//save the prayer times in shared preferences
		sharedPreferences.saveData(AppConstants.FAJR , prayerTimesMapState["fajr"] !!.toString())
		sharedPreferences.saveData(
				AppConstants.SUNRISE ,
				prayerTimesMapState["sunrise"] !!.toString()
								  )
		sharedPreferences.saveData(AppConstants.DHUHR , prayerTimesMapState["dhuhr"] !!.toString())
		sharedPreferences.saveData(AppConstants.ASR , prayerTimesMapState["asr"] !!.toString())
		sharedPreferences.saveData(
				AppConstants.MAGHRIB ,
				prayerTimesMapState["maghrib"] !!.toString()
								  )
		sharedPreferences.saveData(AppConstants.ISHA , prayerTimesMapState["isha"] !!.toString())
		sharedPreferences.saveData(
				AppConstants.CURRENT_PRAYER ,
				prayerTimes.value?.currentPrayer?.name !!
								  )


		if (! alarmLock)
		{
			CreateAlarms().exact(
					context ,
					prayerTimesMapState["fajr"] !! ,
					prayerTimesMapState["sunrise"] !! ,
					prayerTimesMapState["dhuhr"] !! ,
					prayerTimesMapState["asr"] !! ,
					prayerTimesMapState["maghrib"] !! ,
					prayerTimesMapState["isha"] !! ,
								)
			sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , true)
		}
	}

	PrayerTimesListUI(
			modifier = modifier ,
			paddingValues = paddingValues ,
			timerState = timerState ,
			viewModel = viewModel ,
			name = prayerTimes.value?.nextPrayer?.name ?: "" ,
			prayerTimesMap = prayerTimesMapState ,
			prayertimes = prayerTimes.value ,
			state = state ,
					 )
}