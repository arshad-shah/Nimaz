package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
				   )
{
	val context = LocalContext.current
	val sharedPreferences = PrivateSharedPreferences(context)
	val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK , false)
	//delete previous values from shared preferences
	sharedPreferences.removeData(AppConstants.FAJR)
	sharedPreferences.removeData(AppConstants.SUNRISE)
	sharedPreferences.removeData(AppConstants.DHUHR)
	sharedPreferences.removeData(AppConstants.ASR)
	sharedPreferences.removeData(AppConstants.MAGHRIB)
	sharedPreferences.removeData(AppConstants.ISHA)
	sharedPreferences.removeData(AppConstants.CURRENT_PRAYER)

	val prayerTimesMap = mutableMapOf<String , LocalDateTime?>()
	prayerTimesMap["fajr"] = prayerTimes.value?.fajr
	prayerTimesMap["sunrise"] = prayerTimes.value?.sunrise
	prayerTimesMap["dhuhr"] = prayerTimes.value?.dhuhr
	prayerTimesMap["asr"] = prayerTimes.value?.asr
	prayerTimesMap["maghrib"] = prayerTimes.value?.maghrib
	prayerTimesMap["isha"] = prayerTimes.value?.isha

	//save the prayer times in shared preferences
	sharedPreferences.saveData(AppConstants.FAJR , prayerTimesMap["fajr"] !!.toString())
	sharedPreferences.saveData(AppConstants.SUNRISE , prayerTimesMap["sunrise"] !!.toString())
	sharedPreferences.saveData(AppConstants.DHUHR , prayerTimesMap["dhuhr"] !!.toString())
	sharedPreferences.saveData(AppConstants.ASR , prayerTimesMap["asr"] !!.toString())
	sharedPreferences.saveData(AppConstants.MAGHRIB , prayerTimesMap["maghrib"] !!.toString())
	sharedPreferences.saveData(AppConstants.ISHA , prayerTimesMap["isha"] !!.toString())
	sharedPreferences.saveData(
			AppConstants.CURRENT_PRAYER ,
			prayerTimes.value?.currentPrayer?.name !!
							  )


	if (! alarmLock)
	{
		CreateAlarms().exact(
				context ,
				prayerTimesMap["fajr"] !! ,
				prayerTimesMap["sunrise"] !! ,
				prayerTimesMap["dhuhr"] !! ,
				prayerTimesMap["asr"] !! ,
				prayerTimesMap["maghrib"] !! ,
				prayerTimesMap["isha"] !! ,
							)
		sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , true)
	}

	PrayerTimesListUI(
			modifier = modifier ,
			paddingValues = paddingValues ,
			timerState = timerState ,
			viewModel = viewModel ,
			name = prayerTimes.value?.nextPrayer?.name !! ,
			prayerTimesMap = prayerTimesMap ,
			prayertimes = prayerTimes.value
					 )
}