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
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

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

	private val _nextPrayerName = MutableStateFlow("Loading...")
	val nextPrayerName = _nextPrayerName.asStateFlow()

	private val _nextPrayerTime = MutableStateFlow(LocalDateTime.now())
	val nextPrayerTime = _nextPrayerTime.asStateFlow()

	private val _fajrTimeState = MutableStateFlow(LocalDateTime.now())
	val fajrTime = _fajrTimeState.asStateFlow()

	private val _sunriseTimeState = MutableStateFlow(LocalDateTime.now())
	val sunriseTime = _sunriseTimeState.asStateFlow()

	private val _dhuhrTimeState = MutableStateFlow(LocalDateTime.now())
	val dhuhrTime = _dhuhrTimeState.asStateFlow()

	private val _asrTimeState = MutableStateFlow(LocalDateTime.now())
	val asrTime = _asrTimeState.asStateFlow()

	private val _maghribTimeState = MutableStateFlow(LocalDateTime.now())
	val maghribTime = _maghribTimeState.asStateFlow()

	private val _ishaTimeState = MutableStateFlow(LocalDateTime.now())
	val ishaTime = _ishaTimeState.asStateFlow()

	//loading
	private val _isLoading = MutableStateFlow(false)
	val isLoading = _isLoading.asStateFlow()

	//error
	private val _error = MutableStateFlow("")
	val error = _error.asStateFlow()




	//event that starts the timer
	sealed class PrayerTimesEvent
	{

		class Start(val timeToNextPrayer : Long) : PrayerTimesEvent()
		object RELOAD : PrayerTimesEvent()

		//get updated prayertimes if parameters change in settings
		class UPDATE_PRAYERTIMES(val mapOfParameters : Map<String , String>) : PrayerTimesEvent()

		class UPDATE_WIDGET(val context : Context) : PrayerTimesEvent()
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
				loadPrayerTimes(context)
			}
			//event to update the prayer times
			is PrayerTimesEvent.UPDATE_PRAYERTIMES ->
			{
				PrivateSharedPreferences(context).saveDataBoolean(AppConstants.ALARM_LOCK , false)
				updatePrayerTimes(event.mapOfParameters)
			}

			else ->
			{
			}
		}
	}

	//function to update the prayer times
	fun updatePrayerTimes(mapOfParameters : Map<String , String>)
	{
		viewModelScope.launch(Dispatchers.IO) {


			try
			{
				val response = PrayerTimesRepository.updatePrayerTimes(mapOfParameters)
				if (response.data != null)
				{
					val mapOfPrayerTimes = mapOf(
							"fajr" to response.data .fajr ,
							"sunrise" to response.data .sunrise ,
							"dhuhr" to response.data .dhuhr ,
							"asr" to response.data .asr ,
							"maghrib" to response.data .maghrib ,
							"isha" to response.data .isha
												)

					val currentPrayerName = currentPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
					val nextPrayerName = nextPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
					if(currentPrayerName == "isha" && nextPrayerName == "fajr" && LocalTime.now().hour <= response.data .fajr!!.toLocalTime().hour && LocalTime.now().hour >=0){
						//minus 1 day from current prayer times
					}
					if (nextPrayerName == "fajr" && currentPrayerName == "isha" && LocalTime.now().hour <= 24 && LocalTime.now().hour >= response.data .isha!!.toLocalTime().hour){
						//add 1 day to next prayer times
						_nextPrayerName.value = nextPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
						_nextPrayerTime.value = response.data .fajr!!.plusDays(1)
					}else{
						_nextPrayerName.value = nextPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
						_nextPrayerTime.value = nextPrayer(LocalDateTime.now(), mapOfPrayerTimes).second
					}

					//set the current prayer name
					_currentPrayerName.value = currentPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
					_fajrTimeState.value = response.data .fajr!!
					_sunriseTimeState.value = response.data .sunrise!!
					_dhuhrTimeState.value = response.data .dhuhr!!
					_asrTimeState.value = response.data .asr!!
					_maghribTimeState.value = response.data .maghrib!!
					_ishaTimeState.value = response.data .isha!!
					_isLoading.value = false

				} else
				{
					_error.value = response.message.toString()
					_isLoading.value = false
				}

			} catch (e : Exception)
			{
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadPrayerTimes: ${e.message}"
					 )
				_error.value = e.message.toString()
				_isLoading.value = false
			}
		}
	}

	//load prayer times again
	fun loadPrayerTimes(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			_isLoading.value = true
			_error.value = ""
			try
			{
				val response = PrayerTimesRepository.getPrayerTimes(context)
				if (response.data != null)
				{
					val mapOfPrayerTimes = mapOf(
							"fajr" to response.data.fajr ,
							"sunrise" to response.data .sunrise ,
							"dhuhr" to response.data .dhuhr ,
							"asr" to response.data .asr ,
							"maghrib" to response.data .maghrib ,
							"isha" to response.data .isha
												)

					val currentPrayerName = currentPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
					val nextPrayerName = nextPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
					if(currentPrayerName == "isha" && nextPrayerName == "fajr" && LocalTime.now().hour <= response.data .fajr!!.toLocalTime().hour && LocalTime.now().hour >=0){
						//minus 1 day from current prayer times
					}
					if (nextPrayerName == "fajr" && currentPrayerName == "isha" && LocalTime.now().hour <= 24 && LocalTime.now().hour >= response.data .isha!!.toLocalTime().hour){
						//add 1 day to next prayer times
						_nextPrayerName.value = nextPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
						_nextPrayerTime.value = response.data .fajr!!.plusDays(1)
					}else{
						_nextPrayerName.value = nextPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
						_nextPrayerTime.value = nextPrayer(LocalDateTime.now(), mapOfPrayerTimes).second
					}
					//set the current prayer name
					_currentPrayerName.value = currentPrayer(LocalDateTime.now(), mapOfPrayerTimes).first
					_fajrTimeState.value = response.data .fajr!!
					_sunriseTimeState.value = response.data .sunrise!!
					_dhuhrTimeState.value = response.data .dhuhr!!
					_asrTimeState.value = response.data .asr!!
					_maghribTimeState.value = response.data .maghrib!!
					_ishaTimeState.value = response.data .isha!!
					_isLoading.value = false

				} else
				{
					_isLoading.value = false
					_error.value = response.message.toString()
				}

			} catch (e : Exception)
			{
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadPrayerTimes: ${e.message}"
					 )
				_error.value = e.message.toString()
				_isLoading.value = false
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
				loadPrayerTimes(context)
			}
		}.start()
	}
}


	fun currentPrayer(time: LocalDateTime, mapOfPrayerTimes : Map<String , LocalDateTime?>): Pair<String, LocalDateTime> {
		val fajrTommorow = mapOfPrayerTimes["fajr"]?.plusDays(1)
		val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
		return when {
			//if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
			mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("isha" , mapOfPrayerTimes["isha"]!!)
			}

			mapOfPrayerTimes["maghrib"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("maghrib" , mapOfPrayerTimes["maghrib"]!!)
			}

			mapOfPrayerTimes["asr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("asr" , mapOfPrayerTimes["asr"]!!)
			}

			mapOfPrayerTimes["dhuhr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("dhuhr" , mapOfPrayerTimes["dhuhr"]!!)
			}

			mapOfPrayerTimes["sunrise"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("sunrise" , mapOfPrayerTimes["sunrise"]!!)
			}

			mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("fajr" , mapOfPrayerTimes["fajr"]!!)
			}

			`when` in fajrTommorow?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!..mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! -> {
				Pair("isha" , mapOfPrayerTimes["isha"]!!)
			}

			`when` < mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! -> {
				Pair("fajr" , mapOfPrayerTimes["fajr"]!!)
			}

			else -> {
				Pair("none" , mapOfPrayerTimes["none"]!!)
			}
		}
	}

	fun nextPrayer(time: LocalDateTime, mapOfPrayerTimes : Map<String, LocalDateTime?>): Pair<String, LocalDateTime> {
		val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
		return when {
			//if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
			mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("fajr" , mapOfPrayerTimes["fajr"]!!)
			}

			mapOfPrayerTimes["maghrib"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("isha" , mapOfPrayerTimes["isha"]!!)
			}

			mapOfPrayerTimes["asr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("maghrib" , mapOfPrayerTimes["maghrib"]!!)
			}

			mapOfPrayerTimes["dhuhr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("asr" , mapOfPrayerTimes["asr"]!!)
			}

			mapOfPrayerTimes["sunrise"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("dhuhr" , mapOfPrayerTimes["dhuhr"]!!)
			}

			mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
				Pair("sunrise" , mapOfPrayerTimes["sunrise"]!!)
			}

			else -> {
				Pair("none" , mapOfPrayerTimes["none"]!!)
			}
		}
	}
