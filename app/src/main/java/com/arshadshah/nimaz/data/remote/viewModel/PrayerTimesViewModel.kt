package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.LocationFinder
import com.arshadshah.nimaz.utils.location.NetworkChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class PrayerTimesViewModel(context : Context) : ViewModel()
{

	sealed class PrayerTimesState
	{

		object Loading : PrayerTimesState()
		data class Success(val prayerTimes : PrayerTimes?) : PrayerTimesState()
		data class Error(val errorMessage : String) : PrayerTimesState()
	}

	private var _prayerTimesState =
		MutableStateFlow<PrayerTimesState>(PrayerTimesState.Loading)
	val prayerTimesState = _prayerTimesState.asStateFlow()

	//location state
	sealed class LocationState
	{

		object Loading : LocationState()
		data class Success(val location : String) : LocationState()
		data class Error(val errorMessage : String) : LocationState()
	}

	private var _location = MutableStateFlow(LocationState.Loading as LocationState)
	val location = _location.asStateFlow()

	private var countDownTimer : CountDownTimer? = null

	private val _countDownTimeState = MutableLiveData<CountDownTime>()
	val timer : LiveData<CountDownTime> = _countDownTimeState


	init
	{
		loadLocation(context)
		loadPrayerTimes(context)
	}

	//load prayer times again
	fun loadPrayerTimes(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val prayerTimesAvailable = dataStore.countPrayerTimes()
				if (prayerTimesAvailable > 0)
				{

					val localPrayerTimes = dataStore.getAllPrayerTimes()
					Log.d("PrayerTimesViewModel" , "loadPrayerTimes: $localPrayerTimes")
					val localTimesNull = localPrayerTimes.timestamp == null
					//if timestamp is from today then the data is valid and we can use it
					val localTimesExpired =
						localPrayerTimes.timestamp?.toLocalDate() != LocalDate.now()

					//check if the next prayer time has passed
					val nextPrayerPassed = localPrayerTimes.nextPrayer?.time?.isBefore(LocalDateTime.now())

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
							_prayerTimesState.value = PrayerTimesState.Success(response.data)
						} else
						{
							_prayerTimesState.value = PrayerTimesState.Error(response.message !!)
						}
					} else
					{
						_prayerTimesState.value = PrayerTimesState.Success(localPrayerTimes)
					}

				} else
				{
					val response = PrayerTimesRepository.getPrayerTimes(context)
					if (response.data != null)
					{
						Log.d("PrayerTimesViewModel" , "loadPrayerTimesRemote: $response")
						dataStore.deleteAllPrayerTimes()
						dataStore.saveAllPrayerTimes(response.data)
						_prayerTimesState.value = PrayerTimesState.Success(response.data)
					} else
					{
						_prayerTimesState.value = PrayerTimesState.Error(response.message !!)
					}
				}

			} catch (e : Exception)
			{
				_prayerTimesState.value =
					PrayerTimesState.Error(e.message ?: "Unknown error")
			}
		}
	}

	//load location
	fun loadLocation(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			val sharedPreferences = PrivateSharedPreferences(context)
			val locationAuto = sharedPreferences.getDataBoolean("location_auto" , true)
			val locationInput = sharedPreferences.getData("location_input" , "Abbeyleix")
			if (NetworkChecker().networkCheck(context))
			{
				if (locationAuto)
				{
					val locationfinder = LocationFinder()
					val latitude = locationfinder.latitudeValue
					val longitude = locationfinder.longitudeValue
					locationfinder.findCityName(context , latitude , longitude)
					_location.value = LocationState.Success(locationfinder.cityName)
				} else
				{
					Location().getManualLocation(locationInput , context)
					_location.value = LocationState.Success(locationInput)
				}
			} else
			{
				_location.value = LocationState.Error("No internet connection")
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
