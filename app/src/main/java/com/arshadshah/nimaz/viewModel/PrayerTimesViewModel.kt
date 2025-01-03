package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.CountDownTime
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.Parameters
import com.arshadshah.nimaz.repositories.LocationRepository
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.PrayerTimesData
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.widgets.prayertimesthin.PrayerTimeWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerTimesViewModel(context: Context) : ViewModel() {
    private val sharedPreferences = PrivateSharedPreferences(context)
    private val locationService = LocationService(context, LocationRepository(context))
    private val prayerTimesService = PrayerTimesService(context, PrayerTimesRepository)
    private var countDownTimer: CountDownTimer? = null
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _error.value = throwable.message ?: "An unknown error occurred"
        _isLoading.value = false
    }
    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob + coroutineExceptionHandler)

    data class PrayerTimesState(
        val currentPrayerName: String = "Loading...",
        val currentPrayerTime: LocalDateTime = LocalDateTime.now(),
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
        data object RELOAD : PrayerTimesEvent()
        class UPDATE_PRAYERTIMES(val mapOfParameters: Parameters) : PrayerTimesEvent()
        class UPDATE_WIDGET(val context: Context) : PrayerTimesEvent()
        class SET_LOADING(val isLoading: Boolean) : PrayerTimesEvent()
        class SET_ALARMS(val context: Context) : PrayerTimesEvent()
        data object LOAD_LOCATION : PrayerTimesEvent()
        class Init(val context: Context) : PrayerTimesEvent()
    }

    private val _prayerTimesState = MutableStateFlow(PrayerTimesState())
    val prayerTimesState: StateFlow<PrayerTimesState> = _prayerTimesState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun handleEvent(event: PrayerTimesEvent) {
        when (event) {
            is PrayerTimesEvent.Start -> startTimer(event.timeToNextPrayer)
            is PrayerTimesEvent.RELOAD -> loadPrayerTimes()
            is PrayerTimesEvent.UPDATE_PRAYERTIMES -> updatePrayerTimes(event.mapOfParameters)
            is PrayerTimesEvent.UPDATE_WIDGET -> updateWidget(event.context)
            is PrayerTimesEvent.SET_LOADING -> _isLoading.value = event.isLoading
            is PrayerTimesEvent.SET_ALARMS -> setAlarms(event.context)
            is PrayerTimesEvent.LOAD_LOCATION -> loadLocation()
            is PrayerTimesEvent.Init -> init(event.context)
        }
    }

    private fun init(context: Context){
        viewModelScope.launch {
            handleEvent(PrayerTimesEvent.SET_LOADING(true))
            handleEvent(PrayerTimesEvent.LOAD_LOCATION)
            handleEvent(PrayerTimesEvent.RELOAD)
            handleEvent(PrayerTimesEvent.SET_LOADING(false))

            _prayerTimesState.collect { state ->
                if (state.locationName != "Loading..." && state.latitude != 0.0 && state.longitude != 0.0) {
                    handleEvent(PrayerTimesEvent.UPDATE_PRAYERTIMES(PrayerTimesParamMapper.getParams(context)))
                    handleEvent(PrayerTimesEvent.UPDATE_WIDGET(context))

                    val timeToNextPrayer = state.nextPrayerTime
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    val currentTime = LocalDateTime.now()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                    handleEvent(PrayerTimesEvent.Start(timeToNextPrayer - currentTime))
                }
            }
        }
    }

    private fun setAlarms(context: Context) = viewModelScope.launch {
        loadPrayerTimes()
        withContext(Dispatchers.Default) {
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
    }

    private fun updateWidget(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            PrayerTimeWorker.enqueue(context, true)
        }
    }

    private fun updatePrayerTimes(parameters: Parameters) = viewModelScope.launch {
        try {
            _error.value = null

            val response = withContext(Dispatchers.IO) {
                PrayerTimesRepository.updatePrayerTimes(parameters)
            }

            response.data?.let { data ->
                updatePrayerTimesState(data)
            } ?: run {
                _error.value = "Failed to update prayer times. Data is null."
            }
        } finally {
        }
    }

    private suspend fun updatePrayerTimesState(data: LocalPrayerTimes) {
        val currentAndNextPrayertimes = withContext(Dispatchers.Default) {
            prayerTimesService.getCurrentAndNextPrayer(
                PrayerTimesData(
                    fajr = data.fajr ?: LocalDateTime.now(),
                    sunrise = data.sunrise ?: LocalDateTime.now(),
                    dhuhr = data.dhuhr ?: LocalDateTime.now(),
                    asr = data.asr ?: LocalDateTime.now(),
                    maghrib = data.maghrib ?: LocalDateTime.now(),
                    isha = data.isha ?: LocalDateTime.now()
                )
            )
        }

        _prayerTimesState.update {
            it.copy(
                currentPrayerName = currentAndNextPrayertimes.currentPrayer,
                currentPrayerTime = currentAndNextPrayertimes.currentPrayerTime,
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
    }

    fun loadPrayerTimes() = viewModelScope.launch {
        try {
            _error.value = null

            val prayerTimes = withContext(Dispatchers.IO) {
                prayerTimesService.getPrayerTimes()
            }

            if (prayerTimes != null) {
                updatePrayerTimesState(LocalPrayerTimes(
                    fajr = prayerTimes.fajr,
                    sunrise = prayerTimes.sunrise,
                    dhuhr = prayerTimes.dhuhr,
                    asr = prayerTimes.asr,
                    maghrib = prayerTimes.maghrib,
                    isha = prayerTimes.isha,
                ))
            } else {
                _error.value = "Failed to load prayer times"
            }
        } finally {

        }
    }

    private fun startTimer(timeToNextPrayer: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeToNextPrayer, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateCountdownState(millisUntilFinished)
            }

            override fun onFinish() {
                loadPrayerTimes()
            }
        }.start()
    }

    private fun updateCountdownState(millisUntilFinished: Long) {
        val hours = millisUntilFinished / (60 * 60 * 1000)
        val minutes = (millisUntilFinished % (60 * 60 * 1000)) / (60 * 1000)
        val seconds = (millisUntilFinished % (60 * 1000)) / 1000

        _prayerTimesState.update {
            it.copy(countDownTime = CountDownTime(hours, minutes, seconds))
        }
    }

    private fun loadLocation() {
        val isAutoLocation = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, true)
        locationService.loadLocation(
            isAutoLocation,
            onSuccess = { location ->
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
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        viewModelJob.cancel()
    }
}