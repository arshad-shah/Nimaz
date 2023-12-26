package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.models.Parameters
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.LocationRepository
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.PrayerTimesData
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.widgets.prayertimesthin.PrayerTimeWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class PrayerTimesViewModel(context: Context) : ViewModel() {
    val sharedPreferences = PrivateSharedPreferences(context)
    private val locationService = LocationService(context, LocationRepository(context))
    private val prayerTimesService = PrayerTimesService(context, PrayerTimesRepository)

    data class PrayerTimesState(
        val currentPrayerName: String = "Loading...",
        val nextPrayerName: String = "Loading...",
        val nextPrayerTime: LocalDateTime = LocalDateTime.now(),
        val fajrTime: LocalDateTime = LocalDateTime.now(),
        val sunriseTime: LocalDateTime = LocalDateTime.now(),
        val dhuhrTime: LocalDateTime = LocalDateTime.now(),
        val asrTime: LocalDateTime = LocalDateTime.now(),
        val maghribTime: LocalDateTime = LocalDateTime.now(),
        val ishaTime: LocalDateTime = LocalDateTime.now(),
        val countDownTime: CountDownTime = CountDownTime(0, 0, 0),
        val locationName: String = "Loading...",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
    )

    sealed class PrayerTimesEvent {
        class Start(val timeToNextPrayer: Long) : PrayerTimesEvent()
        object RELOAD : PrayerTimesEvent()
        class UPDATE_PRAYERTIMES(val mapOfParameters: Parameters) : PrayerTimesEvent()
        class UPDATE_WIDGET(val context: Context) : PrayerTimesEvent()
        class SET_LOADING(val isLoading: Boolean) : PrayerTimesEvent()
        class SET_ALARMS(val context: Context) : PrayerTimesEvent()

        object LOAD_LOCATION : PrayerTimesEvent()
    }

    private var countDownTimer: CountDownTimer? = null

    private val _prayerTimesState = MutableStateFlow(PrayerTimesState())
    val prayerTimesState: StateFlow<PrayerTimesState> = _prayerTimesState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    //function to handle the timer event
    fun handleEvent(context: Context, event: PrayerTimesEvent) {
        when (event) {
            is PrayerTimesEvent.Start -> {
                //this takes a timeToNextPrayer in milliseconds as a parameter on event
                startTimer(context, event.timeToNextPrayer)
            }
            //event to reload the prayer times
            is PrayerTimesEvent.RELOAD -> {
                loadPrayerTimes()
            }
            //event to update the prayer times
            is PrayerTimesEvent.UPDATE_PRAYERTIMES -> {
                updatePrayerTimes(event.mapOfParameters)
            }
            //event to update the widget
            is PrayerTimesEvent.UPDATE_WIDGET -> {
                updateWidget(event.context)
            }
            //event to set the loading state
            is PrayerTimesEvent.SET_LOADING -> {
                _isLoading.value = event.isLoading
            }
            //event to set the alarms
            is PrayerTimesEvent.SET_ALARMS -> {
                setAlarms(event.context)
            }
            //event to load the location
            is PrayerTimesEvent.LOAD_LOCATION -> {
                loadLocation()
            }
        }
    }

    private fun setAlarms(context: Context) {
        loadPrayerTimes()
        CreateAlarms().exact(
            context,
            _prayerTimesState.value.fajrTime,
            _prayerTimesState.value.sunriseTime,
            _prayerTimesState.value.dhuhrTime,
            _prayerTimesState.value.asrTime,
            _prayerTimesState.value.maghribTime,
            _prayerTimesState.value.ishaTime,
        )
    }

    private fun updateWidget(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            PrayerTimeWorker.enqueue(context, true)
        }
    }

    //function to update the prayer times
    private fun updatePrayerTimes(parameters: Parameters) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = ""
            try {
                val response = PrayerTimesRepository.updatePrayerTimes(parameters)
                response.data?.let { data ->
                    val currentAndNextPrayertimes = prayerTimesService.getCurrentAndNextPrayer(
                        PrayerTimesData(
                            fajr = data.fajr ?: LocalDateTime.now(),
                            sunrise = data.sunrise ?: LocalDateTime.now(),
                            dhuhr = data.dhuhr ?: LocalDateTime.now(),
                            asr = data.asr ?: LocalDateTime.now(),
                            maghrib = data.maghrib ?: LocalDateTime.now(),
                            isha = data.isha ?: LocalDateTime.now()
                        )
                    )

                    Log.d(
                        "Nimaz: PrayerTimesViewModel",
                        "updatePrayerTimes: $currentAndNextPrayertimes"
                    )

                    _prayerTimesState.update {
                        it.copy(
                            currentPrayerName = currentAndNextPrayertimes.currentPrayer,
                            nextPrayerName = currentAndNextPrayertimes.nextPrayer,
                            nextPrayerTime = currentAndNextPrayertimes.nextPrayerTime,
                            fajrTime = data.fajr ?: LocalDateTime.now(),
                            sunriseTime = data.sunrise ?: LocalDateTime.now(),
                            dhuhrTime = data.dhuhr ?: LocalDateTime.now(),
                            asrTime = data.asr ?: LocalDateTime.now(),
                            maghribTime = data.maghrib ?: LocalDateTime.now(),
                            ishaTime = data.isha ?: LocalDateTime.now()
                        )
                    }
                    _isLoading.value = false
                } ?: run {
                    _error.value = "Failed to update prayer times. Data is null."
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(
                    AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel",
                    "updatePrayerTimes: ${e.message}"
                )
                _error.value = e.message ?: "An unknown error occurred"
                _isLoading.value = false
            }
        }
    }


    //load prayer times again
    fun loadPrayerTimes() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = ""
            try {
                val prayerTimes = prayerTimesService.getPrayerTimes()
                if (prayerTimes != null) {
                    val currentAndNextPrayer =
                        prayerTimesService.getCurrentAndNextPrayer(prayerTimes)

                    _prayerTimesState.update {
                        it.copy(
                            currentPrayerName = currentAndNextPrayer.currentPrayer,
                            nextPrayerName = currentAndNextPrayer.nextPrayer,
                            nextPrayerTime = currentAndNextPrayer.nextPrayerTime,
                            fajrTime = prayerTimes.fajr ?: LocalDateTime.now(),
                            sunriseTime = prayerTimes.sunrise ?: LocalDateTime.now(),
                            dhuhrTime = prayerTimes.dhuhr ?: LocalDateTime.now(),
                            asrTime = prayerTimes.asr ?: LocalDateTime.now(),
                            maghribTime = prayerTimes.maghrib ?: LocalDateTime.now(),
                            ishaTime = prayerTimes.isha ?: LocalDateTime.now()
                        )
                    }
                } else {
                    _error.value = "Failed to load prayer times"
                }
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(
                    AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel",
                    "loadPrayerTimes: ${e.message}"
                )
                _error.value = e.message.toString()
                _isLoading.value = false
            }
        }
    }


    private fun startTimer(context: Context, timeToNextPrayer: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeToNextPrayer, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                var diff = millisUntilFinished
                val secondsInMilli: Long = 1000
                val minutesInMilli = secondsInMilli * 60
                val hoursInMilli = minutesInMilli * 60

                val elapsedHours = diff / hoursInMilli
                diff %= hoursInMilli

                val elapsedMinutes = diff / minutesInMilli
                diff %= minutesInMilli

                val elapsedSeconds = diff / secondsInMilli
                diff %= secondsInMilli

                val countDownTime = CountDownTime(elapsedHours, elapsedMinutes, elapsedSeconds)
                _prayerTimesState.update {
                    it.copy(
                        countDownTime = countDownTime
                    )
                }
            }

            override fun onFinish() {
                loadPrayerTimes()
            }
        }.start()
    }

    private fun loadLocation() {
        val isAutoLocation = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, true)
        locationService.loadLocation(isAutoLocation, onSuccess = { location ->
            _prayerTimesState.update {
                it.copy(
                    locationName = location.locationName,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }
        },
            onError = { errorMessage ->
                _prayerTimesState.update {
                    it.copy(
                        locationName = sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""),
                        latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                        longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
                    )
                }
                _error.value = errorMessage
            })
    }
}


fun currentPrayer(
    time: LocalDateTime,
    mapOfPrayerTimes: Map<String, LocalDateTime?>,
): Pair<String, LocalDateTime> {
    val fajrTommorow = mapOfPrayerTimes["fajr"]?.plusDays(1)
    val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
    return when {
        //if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
        mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
            Pair("isha", mapOfPrayerTimes["isha"]!!)
        }

        mapOfPrayerTimes["maghrib"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
            Pair("maghrib", mapOfPrayerTimes["maghrib"]!!)
        }

        mapOfPrayerTimes["asr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
            Pair("asr", mapOfPrayerTimes["asr"]!!)
        }

        mapOfPrayerTimes["dhuhr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
            Pair("dhuhr", mapOfPrayerTimes["dhuhr"]!!)
        }

        mapOfPrayerTimes["sunrise"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
            Pair("sunrise", mapOfPrayerTimes["sunrise"]!!)
        }

        mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
            Pair("fajr", mapOfPrayerTimes["fajr"]!!)
        }

        `when` in fajrTommorow?.toInstant(ZoneOffset.UTC)
            ?.toEpochMilli()!!..mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)
            ?.toEpochMilli()!! -> {
            Pair("isha", mapOfPrayerTimes["isha"]!!)
        }

        `when` < mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! -> {
            Pair("fajr", mapOfPrayerTimes["fajr"]!!)
        }

        else -> {
            Pair("none", mapOfPrayerTimes["none"]!!)
        }
    }
}

fun nextPrayer(
    time: LocalDateTime,
    mapOfPrayerTimes: Map<String, LocalDateTime?>,
): Pair<String, LocalDateTime> {
    val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
    val fajrTommorow = mapOfPrayerTimes["fajr"]?.plusDays(1)
    val isha = mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
    val fajr = mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
    val sunrise = mapOfPrayerTimes["sunrise"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
    val dhuhr = mapOfPrayerTimes["dhuhr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
    val asr = mapOfPrayerTimes["asr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
    val maghrib = mapOfPrayerTimes["maghrib"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!

    return when {
        //if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
        isha - `when` <= 0 -> {
            Pair("fajr", mapOfPrayerTimes["fajr"]!!)
        }

        maghrib - `when` <= 0 -> {
            Pair("isha", mapOfPrayerTimes["isha"]!!)
        }

        asr - `when` <= 0 -> {
            Pair("maghrib", mapOfPrayerTimes["maghrib"]!!)
        }

        dhuhr - `when` <= 0 -> {
            Pair("asr", mapOfPrayerTimes["asr"]!!)
        }

        sunrise - `when` <= 0 -> {
            Pair("dhuhr", mapOfPrayerTimes["dhuhr"]!!)
        }

        fajr - `when` <= 0 -> {
            Pair("sunrise", mapOfPrayerTimes["sunrise"]!!)
        }

        `when` in fajr..sunrise -> {
            Pair("sunrise", mapOfPrayerTimes["sunrise"]!!)
        }

        `when` in sunrise..dhuhr -> {
            Pair("dhuhr", mapOfPrayerTimes["dhuhr"]!!)
        }

        `when` in dhuhr..asr -> {
            Pair("asr", mapOfPrayerTimes["asr"]!!)
        }

        `when` in asr..maghrib -> {
            Pair("maghrib", mapOfPrayerTimes["maghrib"]!!)
        }

        `when` in maghrib..isha -> {
            Pair("isha", mapOfPrayerTimes["isha"]!!)
        }

        `when` in isha..fajrTommorow?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! -> {
            Pair("fajr", mapOfPrayerTimes["fajr"]!!)
        }
        //if the current time is less than the fajr time than the next prayer is fajr
        `when` < fajr -> {
            Pair("fajr", mapOfPrayerTimes["fajr"]!!)
        }

        else -> {
            Pair("none", mapOfPrayerTimes["none"]!!)
        }
    }
}
