package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
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

	private var _prayerTimesState =
		MutableStateFlow(PrayerTimesState.Loading as PrayerTimesState)
	val prayerTimesState = _prayerTimesState.asStateFlow()

	private var countDownTimer : CountDownTimer? = null

	private val _countDownTimeState = MutableStateFlow(CountDownTime(0 , 0 , 0 ))
	val timer = _countDownTimeState.asStateFlow()

	//current prayer name
	private val _currentPrayerName = MutableStateFlow("Loading...")
	val currentPrayerName = _currentPrayerName.asStateFlow()


	//event that starts the timer
	sealed class PrayerTimesEvent
	{

		class Start(val timeToNextPrayer : Long) : PrayerTimesEvent()
		object RELOAD : PrayerTimesEvent()

		//get updated prayertimes if parameters change in settings
		class UPDATE_PRAYERTIMES(val mapOfParameters : Map<String , String>) : PrayerTimesEvent()
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
			//event to update the prayer times
			is PrayerTimesEvent.UPDATE_PRAYERTIMES ->
			{
				updatePrayerTimes(event.mapOfParameters)
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
			//load the prayer times
			loadPrayerTimes(context)
		}
	}

	//function to update the prayer times
	fun updatePrayerTimes(mapOfParameters : Map<String , String>)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_prayerTimesState.value = PrayerTimesState.Loading
				val response = PrayerTimesRepository.updatePrayerTimes(mapOfParameters)
				if (response.data != null)
				{
					_currentPrayerName.value = response.data.currentPrayer?.name.toString()
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
					if (response.data.currentPrayer?.name.toString() == "SUNRISE")
					{
						_currentPrayerName.value = "Duha"
					} else
					{
						_currentPrayerName.value = response.data.currentPrayer?.name.toString()
					}
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