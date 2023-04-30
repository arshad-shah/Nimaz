package com.arshadshah.nimaz.viewModel

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import android.widget.RemoteViews
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.RoutingActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.widgets.Nimaz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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

	private val _countDownTimeState = MutableStateFlow(CountDownTime(0 , 0 , 0))
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

	private val _isRefreshing = MutableStateFlow(false)
	val isRefreshing : StateFlow<Boolean>
		get() = _isRefreshing.asStateFlow()


	//event that starts the timer
	sealed class PrayerTimesEvent
	{

		class Start(val timeToNextPrayer : Long) : PrayerTimesEvent()
		object RELOAD : PrayerTimesEvent()

		//get updated prayertimes if parameters change in settings
		class UPDATE_PRAYERTIMES(val mapOfParameters : Map<String , String>) : PrayerTimesEvent()

		class UPDATE_WIDGET(val context : Context) : PrayerTimesEvent()

		class REFRESH(val isRefreshingUI : Boolean) : PrayerTimesEvent()

		class SET_LOADING(val isLoading : Boolean) : PrayerTimesEvent()

		//set alarms
		class SET_ALARMS(val context : Context) : PrayerTimesEvent()
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
				updatePrayerTimes(event.mapOfParameters)
			}
			//event to update the widget
			is PrayerTimesEvent.UPDATE_WIDGET ->
			{
				updateWidget(event.context)
			}
			//event to refresh the UI
			is PrayerTimesEvent.REFRESH ->
			{
				_isRefreshing.value = event.isRefreshingUI
			}
			//event to set the loading state
			is PrayerTimesEvent.SET_LOADING ->
			{
				_isLoading.value = event.isLoading
			}
			//event to set the alarms
			is PrayerTimesEvent.SET_ALARMS ->
			{
				setAlarms(event.context)
			}
		}
	}

	private fun setAlarms(context : Context)
	{
		loadPrayerTimes(context)
		CreateAlarms().exact(
				context ,
				fajrTime.value !! ,
				sunriseTime.value !! ,
				dhuhrTime.value !! ,
				asrTime.value !! ,
				maghribTime.value !! ,
				ishaTime.value !! ,
							)
	}

	private fun updateWidget(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			val appWidgetManager = AppWidgetManager.getInstance(context)
			val widgetIds = appWidgetManager.getAppWidgetIds(
					ComponentName(context , Nimaz::class.java)
															)
			Log.d("Nimaz: Widget Update viewmodel" , "Updating widget")
			widgetIds.forEach { widgetId ->
				val views = RemoteViews(context.packageName , R.layout.nimaz)
				val intent = Intent(context , RoutingActivity::class.java)
				val pendingIntent = PendingIntent.getActivity(
						context ,
						AppConstants.WIDGET_PENDING_INTENT_REQUEST_CODE ,
						intent ,
						PendingIntent.FLAG_IMMUTABLE
															 )
				views.setOnClickPendingIntent(R.id.widget , pendingIntent)
				val repository = PrayerTimesRepository.getPrayerTimes(context)
				views.setTextViewText(
						R.id.Fajr_time , repository.data?.fajr?.format(
						DateTimeFormatter.ofPattern("hh:mm a")
																	  )
									 )
				views.setTextViewText(
						R.id.Zuhar_time , repository.data?.dhuhr?.format(
						DateTimeFormatter.ofPattern("hh:mm a")
																		)
									 )
				views.setTextViewText(
						R.id.Asar_time , repository.data?.asr?.format(
						DateTimeFormatter.ofPattern("hh:mm a")
																	 )
									 )
				views.setTextViewText(
						R.id.Maghrib_time ,
						repository.data?.maghrib?.format(DateTimeFormatter.ofPattern("hh:mm a"))
									 )
				val ishaTime = repository.data?.isha?.toLocalTime()?.hour
				val ishaTimeMinutes = repository.data?.isha?.toLocalTime()?.minute
				val newIshaTime = if (ishaTime !! >= 22 && ishaTimeMinutes !! >= 30)
				{
					repository.data.maghrib?.plusMinutes(30)
				} else
				{
					repository.data.isha
				}
				views.setTextViewText(
						R.id.Ishaa_time , newIshaTime?.format(
						DateTimeFormatter.ofPattern("hh:mm a")
																	   )
									 )
				// Update the widget
				appWidgetManager.updateAppWidget(widgetId , views)
				Log.d("Nimaz: Widget Update viewmodel" , "Widget updated")
			}
		}
	}

	//function to update the prayer times
	private fun updatePrayerTimes(mapOfParameters : Map<String , String>)
	{
		viewModelScope.launch(Dispatchers.IO) {
			_isLoading.value = true
			_error.value = ""
			try
			{
				val response = PrayerTimesRepository.updatePrayerTimes(mapOfParameters)
				if (response.data != null)
				{
					val mapOfPrayerTimes = mapOf(
							"fajr" to response.data.fajr ,
							"sunrise" to response.data.sunrise ,
							"dhuhr" to response.data.dhuhr ,
							"asr" to response.data.asr ,
							"maghrib" to response.data.maghrib ,
							"isha" to response.data.isha
												)

					val currentPrayerName =
						currentPrayer(LocalDateTime.now() , mapOfPrayerTimes).first
					val nextPrayerName = nextPrayer(LocalDateTime.now() , mapOfPrayerTimes).first
					if (currentPrayerName == "isha" && nextPrayerName == "fajr" && LocalTime.now().hour <= response.data.fajr !!.toLocalTime().hour && LocalTime.now().hour >= 0)
					{
						//minus 1 day from current prayer times
					}
					if (nextPrayerName == "fajr" && currentPrayerName == "isha" && LocalTime.now().hour <= 24 && LocalTime.now().hour >= response.data.isha !!.toLocalTime().hour)
					{
						//add 1 day to next prayer times
						_nextPrayerName.value =
							nextPrayer(LocalDateTime.now() , mapOfPrayerTimes).first
						_nextPrayerTime.value = response.data.fajr !!.plusDays(1)
					} else
					{
						_nextPrayerName.value =
							nextPrayer(LocalDateTime.now() , mapOfPrayerTimes).first
						_nextPrayerTime.value =
							nextPrayer(LocalDateTime.now() , mapOfPrayerTimes).second
					}

					//set the current prayer name
					_currentPrayerName.value =
						currentPrayer(LocalDateTime.now() , mapOfPrayerTimes).first
					_fajrTimeState.value = response.data.fajr !!
					_sunriseTimeState.value = response.data.sunrise !!
					_dhuhrTimeState.value = response.data.dhuhr !!
					_asrTimeState.value = response.data.asr !!
					_maghribTimeState.value = response.data.maghrib !!
					val ishaTime = response.data.isha?.toLocalTime()?.hour
					val ishaTimeMinutes = response.data?.isha?.toLocalTime()?.minute
					val newIshaTime = if (ishaTime !! >= 22 && ishaTimeMinutes !! >= 30)
					{
						response.data.maghrib?.plusMinutes(30)
					} else
					{
						response.data.isha
					}
					_ishaTimeState.value = newIshaTime!!
					Log.d(
							AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
							"UpdatePrayerTimes: ${response.data}"
						 )
					_isLoading.value = false
					_isRefreshing.value = false
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
							"sunrise" to response.data.sunrise ,
							"dhuhr" to response.data.dhuhr ,
							"asr" to response.data.asr ,
							"maghrib" to response.data.maghrib ,
							"isha" to response.data.isha
												)
					val currentDate = LocalDateTime.now()
					val currentPrayerName = currentPrayer(currentDate , mapOfPrayerTimes).first
					val nextPrayerName = nextPrayer(currentDate , mapOfPrayerTimes).first
					if (currentPrayerName == "isha" && nextPrayerName == "fajr" && LocalTime.now().hour <= response.data.fajr !!.toLocalTime().hour && LocalTime.now().hour >= 0)
					{
						//minus 1 day from current prayer times
					}
					if (nextPrayerName == "fajr" && currentPrayerName == "isha" && LocalTime.now().hour <= 24 && LocalTime.now().hour >= response.data.isha !!.toLocalTime().hour)
					{
						//add 1 day to next prayer times
						_nextPrayerName.value =
							nextPrayer(LocalDateTime.now() , mapOfPrayerTimes).first
						_nextPrayerTime.value = response.data.fajr !!.plusDays(1)
					} else
					{
						_nextPrayerName.value =
							nextPrayer(LocalDateTime.now() , mapOfPrayerTimes).first
						_nextPrayerTime.value =
							nextPrayer(LocalDateTime.now() , mapOfPrayerTimes).second
					}
					//set the current prayer name
					_currentPrayerName.value =
						currentPrayer(LocalDateTime.now() , mapOfPrayerTimes).first
					_fajrTimeState.value = response.data.fajr !!
					_sunriseTimeState.value = response.data.sunrise !!
					_dhuhrTimeState.value = response.data.dhuhr !!
					_asrTimeState.value = response.data.asr !!
					_maghribTimeState.value = response.data.maghrib !!

					val ishaTime = response.data.isha?.toLocalTime()?.hour
					val ishaTimeMinutes = response.data?.isha?.toLocalTime()?.minute
					val newIshaTime = if (ishaTime !! >= 22 && ishaTimeMinutes !! >= 30)
					{
						response.data.maghrib?.plusMinutes(30)
					} else
					{
						response.data.isha
					}
					_ishaTimeState.value = newIshaTime!!

					_isLoading.value = false
					_isRefreshing.value = false

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

	private fun startTimer(context : Context , timeToNextPrayer : Long)
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


fun currentPrayer(
	time : LocalDateTime ,
	mapOfPrayerTimes : Map<String , LocalDateTime?> ,
				 ) : Pair<String , LocalDateTime>
{
	val fajrTommorow = mapOfPrayerTimes["fajr"]?.plusDays(1)
	val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
	return when
	{
		//if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
		mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !! - `when` <= 0 ->
		{
			Pair("isha" , mapOfPrayerTimes["isha"] !!)
		}

		mapOfPrayerTimes["maghrib"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !! - `when` <= 0 ->
		{
			Pair("maghrib" , mapOfPrayerTimes["maghrib"] !!)
		}

		mapOfPrayerTimes["asr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !! - `when` <= 0 ->
		{
			Pair("asr" , mapOfPrayerTimes["asr"] !!)
		}

		mapOfPrayerTimes["dhuhr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !! - `when` <= 0 ->
		{
			Pair("dhuhr" , mapOfPrayerTimes["dhuhr"] !!)
		}

		mapOfPrayerTimes["sunrise"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !! - `when` <= 0 ->
		{
			Pair("sunrise" , mapOfPrayerTimes["sunrise"] !!)
		}

		mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !! - `when` <= 0 ->
		{
			Pair("fajr" , mapOfPrayerTimes["fajr"] !!)
		}

		`when` in fajrTommorow?.toInstant(ZoneOffset.UTC)
			?.toEpochMilli() !! .. mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)
			?.toEpochMilli() !! ->
		{
			Pair("isha" , mapOfPrayerTimes["isha"] !!)
		}

		`when` < mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !! ->
		{
			Pair("fajr" , mapOfPrayerTimes["fajr"] !!)
		}

		else ->
		{
			Pair("none" , mapOfPrayerTimes["none"] !!)
		}
	}
}

fun nextPrayer(
	time : LocalDateTime ,
	mapOfPrayerTimes : Map<String , LocalDateTime?> ,
			  ) : Pair<String , LocalDateTime>
{
	val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
	val fajrTommorow = mapOfPrayerTimes["fajr"]?.plusDays(1)
	val isha = mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !!
	val fajr = mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !!
	val sunrise = mapOfPrayerTimes["sunrise"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !!
	val dhuhr = mapOfPrayerTimes["dhuhr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !!
	val asr = mapOfPrayerTimes["asr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !!
	val maghrib = mapOfPrayerTimes["maghrib"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !!

	return when
	{
		//if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
		isha - `when` <= 0 ->
		{
			Pair("fajr" , mapOfPrayerTimes["fajr"] !!)
		}

		maghrib - `when` <= 0 ->
		{
			Pair("isha" , mapOfPrayerTimes["isha"] !!)
		}

		asr - `when` <= 0 ->
		{
			Pair("maghrib" , mapOfPrayerTimes["maghrib"] !!)
		}

		dhuhr - `when` <= 0 ->
		{
			Pair("asr" , mapOfPrayerTimes["asr"] !!)
		}

		sunrise - `when` <= 0 ->
		{
			Pair("dhuhr" , mapOfPrayerTimes["dhuhr"] !!)
		}

		fajr - `when` <= 0 ->
		{
			Pair("sunrise" , mapOfPrayerTimes["sunrise"] !!)
		}

		`when` in fajr .. sunrise ->
		{
			Pair("sunrise" , mapOfPrayerTimes["sunrise"] !!)
		}

		`when` in sunrise .. dhuhr ->
		{
			Pair("dhuhr" , mapOfPrayerTimes["dhuhr"] !!)
		}

		`when` in dhuhr .. asr ->
		{
			Pair("asr" , mapOfPrayerTimes["asr"] !!)
		}

		`when` in asr .. maghrib ->
		{
			Pair("maghrib" , mapOfPrayerTimes["maghrib"] !!)
		}

		`when` in maghrib .. isha ->
		{
			Pair("isha" , mapOfPrayerTimes["isha"] !!)
		}

		`when` in isha .. fajrTommorow?.toInstant(ZoneOffset.UTC)?.toEpochMilli() !! ->
		{
			Pair("fajr" , mapOfPrayerTimes["fajr"] !!)
		}
		//if the current time is less than the fajr time than the next prayer is fajr
		`when` < fajr ->
		{
			Pair("fajr" , mapOfPrayerTimes["fajr"] !!)
		}

		else ->
		{
			Pair("none" , mapOfPrayerTimes["none"] !!)
		}
	}
}
