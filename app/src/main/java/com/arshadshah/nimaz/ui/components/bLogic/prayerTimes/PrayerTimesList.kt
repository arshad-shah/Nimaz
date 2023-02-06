package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.PrayerTimesListUI
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import java.time.LocalDateTime
import kotlin.reflect.KFunction2


@Composable
fun PrayerTimesList(
	state : PrayerTimesViewModel.PrayerTimesState ,
	timer : LiveData<CountDownTime> ,
	handleEvent : KFunction2<Context , PrayerTimesViewModel.PrayerTimesEvent , Unit> ,
	currentPrayerName : MutableState<String> ,
				   )
{
	val context = LocalContext.current
	val sharedPreferences = PrivateSharedPreferences(context)
	var countDownTime by remember { mutableStateOf(CountDownTime(0 , 0 , 0)) }

	when (state)
	{
		is PrayerTimesViewModel.PrayerTimesState.Loading ->
		{
			PrayerTimesListUI(
					prayerTimesMap = mapOf(
							"FAJR" to LocalDateTime.now() ,
							"DHUHR" to LocalDateTime.now() ,
							"ASR" to LocalDateTime.now() ,
							"MAGHRIB" to LocalDateTime.now() ,
							"ISHA" to LocalDateTime.now(),
										  ) ,
					name = "" ,
					state = state ,
					countDownTime = countDownTime ,
					loading = true ,
						 )
		}
		is PrayerTimesViewModel.PrayerTimesState.Error ->
		{
			PrayerTimesListUI(
					prayerTimesMap = mapOf() ,
					name = "" ,
					state = state ,
					countDownTime = countDownTime ,
					loading = false ,
						 )
		}
		is PrayerTimesViewModel.PrayerTimesState.Success ->
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

			val prayerTimes = state.prayerTimes
			//Map<String , LocalDateTime?>
			val prayerTimesMap = mapOf(
				"fajr" to prayerTimes.fajr ,
				"sunrise" to prayerTimes.sunrise ,
				"dhuhr" to prayerTimes.dhuhr ,
				"asr" to prayerTimes.asr ,
				"maghrib" to prayerTimes.maghrib ,
				"isha" to prayerTimes.isha ,
															)

			currentPrayerName.value = prayerTimes.currentPrayer?.name !!

			//save the prayer times in shared preferences
			sharedPreferences.saveData(AppConstants.FAJR , prayerTimesMap["fajr"] !!.toString())
			sharedPreferences.saveData(
					AppConstants.SUNRISE ,
					prayerTimesMap["sunrise"] !!.toString()
									  )
			sharedPreferences.saveData(AppConstants.DHUHR , prayerTimesMap["dhuhr"] !!.toString())
			sharedPreferences.saveData(AppConstants.ASR , prayerTimesMap["asr"] !!.toString())
			sharedPreferences.saveData(
					AppConstants.MAGHRIB ,
					prayerTimesMap["maghrib"] !!.toString()
									  )
			sharedPreferences.saveData(AppConstants.ISHA , prayerTimesMap["isha"] !!.toString())
			sharedPreferences.saveData(
					AppConstants.CURRENT_PRAYER ,
					prayerTimes.currentPrayer?.name !!
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

			LaunchedEffect(key1 = timer) {
				timer.observeForever {
					countDownTime = it
				}
			}
			val timeToNextPrayerLong =
				state.prayerTimes.nextPrayer?.time?.atZone(java.time.ZoneId.systemDefault())?.toInstant()
					?.toEpochMilli()
			val currentTime =
				LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
					.toEpochMilli()

			val difference = timeToNextPrayerLong?.minus(currentTime)
			handleEvent(LocalContext.current, PrayerTimesViewModel.PrayerTimesEvent.Start(difference !!))
			PrayerTimesListUI(
					name = prayerTimes.nextPrayer?.name ?: "" ,
					prayerTimesMap = prayerTimesMap ,
					state = state ,
					countDownTime = countDownTime ,
					loading = false ,
							 )
		}
	}

}