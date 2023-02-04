package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.NetworkChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class PrayerTimesViewModel(context : Context) : ViewModel()
{

	data class PrayerTimesState(
		val isLoading : MutableState<Boolean> = mutableStateOf(false) ,
		val isLoaded : MutableState<Boolean> = mutableStateOf(false) ,
		val prayerTimes : MutableState<PrayerTimes?> = mutableStateOf(null) ,
		val error : MutableState<String?> = mutableStateOf(null) ,
		val location : MutableState<String?> = mutableStateOf(null) ,
							   )

	private var _prayerTimesState =
		MutableStateFlow(PrayerTimesState())
	val prayerTimesState = _prayerTimesState.asStateFlow()

	private var countDownTimer : CountDownTimer? = null

	private val _countDownTimeState = MutableLiveData<CountDownTime>()
	val timer : LiveData<CountDownTime> = _countDownTimeState

	//function to reload the UI state
	fun reload(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			//load the location
			loadLocation(context)
			//load the prayer times
			loadPrayerTimes(context)
		}
	}

	//load prayer times again
	fun loadPrayerTimes(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_prayerTimesState.update {
					it.copy(isLoading = mutableStateOf(true))
				}
				val dataStore = LocalDataStore.getDataStore()
				val prayerTimesAvailable = dataStore.countPrayerTimes()
				val isSettingsUpdated = PrivateSharedPreferences(context).getDataBoolean(
						AppConstants.RECALCULATE_PRAYER_TIMES ,
						false
																						)

				if (prayerTimesAvailable > 0 && ! isSettingsUpdated)
				{

					val localPrayerTimes = dataStore.getAllPrayerTimes()
					Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel", "loadPrayerTimes: $localPrayerTimes")
					val localTimesNull = localPrayerTimes.timestamp == null
					//if timestamp is from today then the data is valid and we can use it
					val localTimesExpired =
						localPrayerTimes.timestamp?.toLocalDate() != LocalDate.now()

					//check if the next prayer time has passed
					val nextPrayerPassed =
						localPrayerTimes.nextPrayer?.time?.isBefore(LocalDateTime.now())

					if (localTimesNull ||
						//check if the prayer times are not from today
						localTimesExpired ||
						nextPrayerPassed == true
					)
					{
						val response = PrayerTimesRepository.getPrayerTimes(context)
						if (response.data != null)
						{
							dataStore.deleteAllPrayerTimes()
							dataStore.saveAllPrayerTimes(response.data)
							_prayerTimesState.update {
								it.copy(
										isLoading = mutableStateOf(false) ,
										isLoaded = mutableStateOf(true) ,
										prayerTimes = mutableStateOf(response.data) ,
										error = mutableStateOf(null)
									   )
							}
						} else
						{
							_prayerTimesState.update {
								it.copy(
										isLoading = mutableStateOf(false) ,
										isLoaded = mutableStateOf(false) ,
										prayerTimes = mutableStateOf(null) ,
										error = mutableStateOf(response.message)
									   )
							}
						}
					} else
					{
						_prayerTimesState.update {
							it.copy(
									isLoading = mutableStateOf(false) ,
									isLoaded = mutableStateOf(true) ,
									prayerTimes = mutableStateOf(localPrayerTimes) ,
									error = mutableStateOf(null)
								   )
						}
					}

				} else
				{
					val response = PrayerTimesRepository.getPrayerTimes(context)
					if (response.data != null)
					{
						//if recalculate_prayer_times is true then set it to false
						if (isSettingsUpdated)
						{
							PrivateSharedPreferences(context).saveDataBoolean(
									AppConstants.RECALCULATE_PRAYER_TIMES ,
									false
																			 )
						}
						Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" , "loadPrayerTimesRemote: ${response.data}")
						dataStore.deleteAllPrayerTimes()
						dataStore.saveAllPrayerTimes(response.data)
						_prayerTimesState.update {
							it.copy(
									isLoading = mutableStateOf(false) ,
									isLoaded = mutableStateOf(true) ,
									prayerTimes = mutableStateOf(response.data) ,
									error = mutableStateOf(null)
								   )
						}
					} else
					{
						_prayerTimesState.update {
							it.copy(
									isLoading = mutableStateOf(false) ,
									isLoaded = mutableStateOf(false) ,
									prayerTimes = mutableStateOf(null) ,
									error = mutableStateOf(response.message)
								   )
						}
					}
				}

			} catch (e : Exception)
			{
				Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" , "loadPrayerTimes: ${e.message}")
				_prayerTimesState.update {
					it.copy(
							isLoading = mutableStateOf(false) ,
							isLoaded = mutableStateOf(false) ,
							prayerTimes = mutableStateOf(null) ,
							error = mutableStateOf(e.message)
						   )
				}
			}
		}
	}

	//load location
	fun loadLocation(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_prayerTimesState.update {
					it.copy(
							location = mutableStateOf(null) ,
						   )
				}
				Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" , "loadLocation: loading...")
				val sharedPreferences = PrivateSharedPreferences(context)
				val locationAuto = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE , true)
				val locationName = mutableStateOf(sharedPreferences.getData(AppConstants.LOCATION_INPUT , "Abbeyleix"))
				val latitude = mutableStateOf(sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.0))
				val longitude = mutableStateOf(sharedPreferences.getDataDouble(AppConstants.LONGITUDE , -7.0))
				//callback for location
				val locationFoundCallbackManual =
					{ longitudeValue : Double , latitudeValue : Double , name : String ->
						//save location
						locationName.value = name
						latitude.value = latitudeValue
						longitude.value = longitudeValue
					}
				val listener = {latitudeValue : Double , longitudeValue : Double ->
					//save location
					latitude.value = latitudeValue
					longitude.value = longitudeValue
				}

				if (NetworkChecker().networkCheck(context))
				{
					if (locationAuto)
					{
						Location().getAutomaticLocation(context,listener, locationFoundCallbackManual)
						Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" , "loadLocation: auto")
						_prayerTimesState.update {
							it.copy(
									location = mutableStateOf(locationName.value) ,
								   )
						}
					} else
					{
						Location().getManualLocation(
								locationName.value ,
								context ,
								locationFoundCallbackManual
													)
						Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" , "loadLocation: manual")
						_prayerTimesState.update {
							it.copy(
									location = mutableStateOf(locationName.value) ,
								   )
						}
					}
				} else
				{
					Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" , "loadLocation: no network")
					_prayerTimesState.update {
						it.copy(
								location = mutableStateOf(null) ,
							   )
					}
				}
			}catch (e : Exception)
			{
				Log.d(AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" , "loadLocation: ${e.message}")
				_prayerTimesState.update {
					it.copy(
							location = mutableStateOf(null) ,
						   )
				}
			}
		}
	}

	fun startTimer(context : Context , timeToNextPrayer : Long)
	{
		countDownTimer = object : CountDownTimer(timeToNextPrayer , 1000)
		{
			override fun onTick(millisUntilFinished : Long)
			{
				var diff = millisUntilFinished
				val secondsInMilli : Long = 1000
				val minutesInMilli = secondsInMilli * 60
				val hoursInMilli = minutesInMilli * 60

				val elapsedHours = diff / hoursInMilli
				diff %= hoursInMilli

				val elapsedMinutes = diff / minutesInMilli
				diff %= minutesInMilli

				val elapsedSeconds = diff / secondsInMilli
				diff %= secondsInMilli

				val countDownTime = CountDownTime(elapsedHours , elapsedMinutes , elapsedSeconds)
				_countDownTimeState.value = countDownTime
			}

			override fun onFinish()
			{
				loadPrayerTimes(context)
			}
		}.start()
	}
}
