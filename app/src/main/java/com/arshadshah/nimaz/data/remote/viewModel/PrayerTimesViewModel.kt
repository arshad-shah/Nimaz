package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.NetworkChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrayerTimesViewModel : ViewModel()
{

	sealed class PrayerTimesState
	{

		object Loading : PrayerTimesState()
		data class Success(val prayerTimes : PrayerTimes) : PrayerTimesState()
		data class Error(val error : String) : PrayerTimesState()
	}

	sealed class LocationState
	{

		object Loading : LocationState()
		data class Success(val location : String) : LocationState()
		data class Error(val error : String) : LocationState()
	}

	private val _locationState = MutableStateFlow(LocationState.Loading as LocationState)
	val locationState = _locationState.asStateFlow()

	private var _prayerTimesState =
		MutableStateFlow(PrayerTimesState.Loading as PrayerTimesState)
	val prayerTimesState = _prayerTimesState.asStateFlow()

	private var countDownTimer : CountDownTimer? = null

	private val _countDownTimeState = MutableLiveData<CountDownTime>()
	val timer : LiveData<CountDownTime> = _countDownTimeState

	//event that starts the timer
	sealed class PrayerTimesEvent
	{

		class Start(val timeToNextPrayer : Long) : PrayerTimesEvent()
		object RELOAD : PrayerTimesEvent()
	}

	//function to handle the timer event
	fun handleEvent(context : Context , event : PrayerTimesEvent)
	{
		when (event)
		{
			is PrayerTimesEvent.Start ->
			{
				//this takes a timeToNextPrayer in milliseconds as a parameter on event
				startTimer(context , event.timeToNextPrayer)
			}
			//event to reload the prayer times
			is PrayerTimesEvent.RELOAD ->
			{
				reload(context)
			}

			else ->
			{
			}
		}
	}

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
				_prayerTimesState.value = PrayerTimesState.Loading
					val response = PrayerTimesRepository.getPrayerTimes(context)
					if (response.data != null)
					{
						_prayerTimesState.value = PrayerTimesState.Success(response.data)
					} else
					{
						_prayerTimesState.value = PrayerTimesState.Error(response.message !!)
					}

			} catch (e : Exception)
			{
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadPrayerTimes: ${e.message}"
					 )
				_prayerTimesState.value = PrayerTimesState.Error(e.message !!)
			}
		}
	}

	//load location
	fun loadLocation(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_locationState.value = LocationState.Loading
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadLocation: loading..."
					 )
				val sharedPreferences = PrivateSharedPreferences(context)
				val locationAuto =
					sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE , true)
				val locationName = mutableStateOf(
						sharedPreferences.getData(
								AppConstants.LOCATION_INPUT ,
								"Abbeyleix"
												 )
												 )
				val latitude =
					mutableStateOf(sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.0))
				val longitude =
					mutableStateOf(sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 7.0))
				//callback for location
				val locationFoundCallbackManual =
					{ longitudeValue : Double , latitudeValue : Double , name : String ->
						//save location
						locationName.value = name
						latitude.value = latitudeValue
						longitude.value = longitudeValue
					}
				val listener = { latitudeValue : Double , longitudeValue : Double ->
					//save location
					latitude.value = latitudeValue
					longitude.value = longitudeValue
				}

				if (NetworkChecker().networkCheck(context))
				{
					if (locationAuto)
					{
						Location().getAutomaticLocation(
								context ,
								listener ,
								locationFoundCallbackManual
													   )
						Log.d(
								AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
								"loadLocation: auto"
							 )
						_locationState.value = LocationState.Success(locationName.value)
					} else
					{
						Location().getManualLocation(
								locationName.value ,
								context ,
								locationFoundCallbackManual
													)
						Log.d(
								AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
								"loadLocation: manual"
							 )
						_locationState.value = LocationState.Success(locationName.value)
					}
				} else
				{
					Log.d(
							AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
							"loadLocation: no network"
						 )
					_locationState.value = LocationState.Error("No network")
				}
			} catch (e : Exception)
			{
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadLocation: ${e.message}"
					 )
				_locationState.value = LocationState.Error(e.message !!)
			}
		}
	}

	fun startTimer(context : Context , timeToNextPrayer : Long)
	{
		countDownTimer?.cancel()
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
				reload(context)
			}
		}.start()
	}
}