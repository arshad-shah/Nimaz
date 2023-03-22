package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.PrayerTimesListUI
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import es.dmoral.toasty.Toasty
import java.time.LocalDateTime


@Composable
fun PrayerTimesList()
{
	val context = LocalContext.current

	val viewModel = viewModel(
			key = "PrayerTimesViewModel" ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

	val fajrTime = remember {
		viewModel.fajrTime
	}.collectAsState()

	val sunriseTime = remember {
		viewModel.sunriseTime
	}.collectAsState()

	val dhuhrTime = remember {
		viewModel.dhuhrTime
	}.collectAsState()

	val asrTime = remember {
		viewModel.asrTime
	}.collectAsState()

	val maghribTime = remember {
		viewModel.maghribTime
	}.collectAsState()

	val ishaTime = remember {
		viewModel.ishaTime
	}.collectAsState()

	val isLoading = remember {
		viewModel.isLoading
	}.collectAsState()

	val isError = remember {
		viewModel.error
	}.collectAsState()

	val nextPrayerName = remember {
		viewModel.nextPrayerName
	}.collectAsState()

	val nextPrayerTime = remember {
		viewModel.nextPrayerTime
	}.collectAsState()

	if (isError.value.isNotBlank())
	{
		Toasty.error(context , isError.value).show()
	} else if (isLoading.value)
	{
		PrayerTimesListUI(
				name = nextPrayerName.value ,
				prayerTimesMap = mapOf(
						"Fajr" to fajrTime.value ,
						"Sunrise" to sunriseTime.value ,
						"Dhuhr" to dhuhrTime.value ,
						"Asr" to asrTime.value ,
						"Maghrib" to maghribTime.value ,
						"Isha" to ishaTime.value ,
									  ) ,
				loading = true ,
						 )
	} else
	{
		val sharedPreferences = PrivateSharedPreferences(context)
		val alarmLock = sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK , false)

		if (! alarmLock)
		{
			CreateAlarms().exact(
					context ,
					fajrTime.value !! ,
					sunriseTime.value !! ,
					dhuhrTime.value !! ,
					asrTime.value !! ,
					maghribTime.value !! ,
					ishaTime.value !! ,
								)
			sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , true)
		}
		val timeToNextPrayerLong =
			nextPrayerTime.value.atZone(java.time.ZoneId.systemDefault())
				?.toInstant()
				?.toEpochMilli()
		val currentTime =
			LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
				.toEpochMilli()

		val difference = timeToNextPrayerLong?.minus(currentTime)
		viewModel.handleEvent(
				LocalContext.current ,
				PrayerTimesViewModel.PrayerTimesEvent.Start(difference !!)
							 )

		val mapOfPrayerTimes = mapOf(
				"Fajr" to fajrTime.value !! ,
				"Sunrise" to sunriseTime.value !! ,
				"Dhuhr" to dhuhrTime.value !! ,
				"Asr" to asrTime.value !! ,
				"Maghrib" to maghribTime.value !! ,
				"Isha" to ishaTime.value !! ,
									)
		PrayerTimesListUI(
				name = nextPrayerName.value.first()
					.uppercaseChar() + nextPrayerName.value.substring(1) ,
				loading = false ,
				prayerTimesMap = mapOfPrayerTimes,
						 )
	}

}