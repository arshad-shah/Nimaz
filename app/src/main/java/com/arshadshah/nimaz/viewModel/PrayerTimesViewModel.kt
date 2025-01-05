package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.LOCATION_TYPE
import com.arshadshah.nimaz.data.local.models.CountDownTime
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.Parameters
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.PrayerTimesData
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.widgets.prayertimesthin.PrayerTimeWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val prayerTimesService: PrayerTimesService,
    private val locationService: LocationService,
    private val sharedPreferences: PrivateSharedPreferences
) : ViewModel() {

    // State definitions remain the same for API compatibility
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

    private var countDownTimer: CountDownTimer? = null

    companion object {
        private const val TAG = "PrayerTimesVM"
    }


    init {
        Log.d(TAG, "ViewModel initialized")
    }

    fun handleEvent(event: PrayerTimesEvent) {
        Log.d(TAG, "Handling event: $event")
        viewModelScope.launch {
            try {
                when (event) {
                    is PrayerTimesEvent.Start -> {
                        Log.d(TAG, "Starting timer with duration: ${event.timeToNextPrayer}ms")
                        withContext(Dispatchers.Main) {
                            startTimer(event.timeToNextPrayer)
                        }
                    }

                    is PrayerTimesEvent.RELOAD -> {
                        Log.d(TAG, "Reloading prayer times")
                        loadPrayerTimes()
                    }

                    is PrayerTimesEvent.UPDATE_PRAYERTIMES -> {
                        Log.d(TAG, "Updating prayer times with params: ${event.mapOfParameters}")
                        updatePrayerTimes(event.mapOfParameters)
                    }

                    is PrayerTimesEvent.UPDATE_WIDGET -> {
                        Log.d(TAG, "Updating widget")
                        updateWidget(event.context)
                    }

                    is PrayerTimesEvent.SET_LOADING -> {
                        Log.d(TAG, "Setting loading state to: ${event.isLoading}")
                        setLoading(event.isLoading)
                    }

                    is PrayerTimesEvent.SET_ALARMS -> {
                        Log.d(TAG, "Setting alarms")
                        setAlarms(event.context)
                    }

                    is PrayerTimesEvent.LOAD_LOCATION -> {
                        Log.d(TAG, "Loading location")
                        loadLocation()
                    }

                    is PrayerTimesEvent.Init -> {
                        Log.d(TAG, "Initializing with context")
                        initialize(event.context)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling event: ${e.message}", e)
                _error.value = e.message ?: "An unknown error occurred"
                _isLoading.value = false
            }
        }
    }

    private suspend fun initialize(context: Context) = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting initialization")
        setLoading(true)
        try {
            loadLocation()
            loadPrayerTimes()

            prayerTimesState.value.let { state ->
                if (state.isValidLocation()) {
                    Log.d(TAG, "Location valid, proceeding with initialization")
                    updatePrayerTimes(PrayerTimesParamMapper.getParams(context))
                    updateWidget(context)
                    withContext(Dispatchers.Main) {
                        calculateAndStartTimer(state)
                    }
                } else {
                    Log.w(TAG, "Invalid location during initialization: $state")
                }
            }
        } finally {
            setLoading(false)
            Log.d(TAG, "Initialization completed")
        }
    }

    private suspend fun setAlarms(context: Context) {
        Log.d(TAG, "Setting alarms - loading prayer times first")
        loadPrayerTimes()
        withContext(Dispatchers.Default) {
            _prayerTimesState.value.let { state ->
                Log.d(
                    TAG,
                    "Creating alarms with times: Fajr=${state.fajrTime}, Dhuhr=${state.dhuhrTime}"
                )
                CreateAlarms().exact(
                    context,
                    state.fajrTime,
                    state.sunriseTime,
                    state.dhuhrTime,
                    state.asrTime,
                    state.maghribTime,
                    state.ishaTime,
                )
                Log.d(TAG, "Alarms set successfully")
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        Log.d(TAG, "Updating widget")
        withContext(Dispatchers.IO) {
            PrayerTimeWorker.enqueue(context, true)
            Log.d(TAG, "Widget update enqueued")
        }
    }

    private suspend fun updatePrayerTimes(parameters: Parameters) {
        Log.d(TAG, "Updating prayer times with parameters: $parameters")
        try {
            _error.value = null
            val response = withContext(Dispatchers.IO) {
                PrayerTimesRepository.updatePrayerTimes(parameters)
            }
            Log.d(TAG, "Prayer times update response received: ${response.data != null}")

            response.data?.let {
                Log.d(TAG, "Updating prayer times state with new data")
                updatePrayerTimesState(it)
            } ?: run {
                Log.e(TAG, "Failed to update prayer times - null data received")
                _error.value = "Failed to update prayer times. Data is null."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating prayer times: ${e.message}", e)
            _error.value = e.message
        }
    }

    private suspend fun updatePrayerTimesState(data: LocalPrayerTimes) {
        Log.d(TAG, "Updating prayer times state with data: $data")
        val currentAndNextPrayertimes = withContext(Dispatchers.Default) {
            prayerTimesService.getCurrentAndNextPrayer(data.toPrayerTimesData())
        }
        Log.d(
            TAG,
            "Current prayer: ${currentAndNextPrayertimes.currentPrayer}, Next prayer: ${currentAndNextPrayertimes.nextPrayer}"
        )

        _prayerTimesState.update { state ->
            state.copy(
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
            ).also {
                Log.d(
                    TAG,
                    "Prayer times state updated: Current=${it.currentPrayerName}, Next=${it.nextPrayerName}"
                )
            }
        }
    }

    private suspend fun loadPrayerTimes() {
        Log.d(TAG, "Loading prayer times")
        try {
            _error.value = null
            val prayerTimes = withContext(Dispatchers.IO) {
                prayerTimesService.getPrayerTimes()
            }
            Log.d(TAG, "Prayer times loaded: ${prayerTimes != null}")

            prayerTimes?.let {
                Log.d(TAG, "Updating state with loaded prayer times")
                updatePrayerTimesState(it.toLocalPrayerTimes())
            } ?: run {
                Log.e(TAG, "Failed to load prayer times - null data received")
                _error.value = "Failed to load prayer times"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading prayer times: ${e.message}", e)
            _error.value = e.message
        }
    }

    private fun startTimer(timeToNextPrayer: Long) {
        Log.d(TAG, "Starting countdown timer on thread: ${Thread.currentThread().name}")

        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.w(TAG, "Not on main thread, switching context")
            viewModelScope.launch(Dispatchers.Main) {
                createAndStartTimer(timeToNextPrayer)
            }
            return
        }

        createAndStartTimer(timeToNextPrayer)
    }

    private fun createAndStartTimer(timeToNextPrayer: Long) {
        Log.d(TAG, "Creating new timer instance")
        countDownTimer?.cancel() // Cancel any existing timer

        countDownTimer = object : CountDownTimer(timeToNextPrayer, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateCountdownState(millisUntilFinished)
            }

            override fun onFinish() {
                Log.d(TAG, "Timer finished, reloading prayer times")
                viewModelScope.launch {
                    loadPrayerTimes()
                }
            }
        }.start()
        Log.d(TAG, "New timer started successfully")
    }

    private fun updateCountdownState(millisUntilFinished: Long) {
        val hours = millisUntilFinished / (60 * 60 * 1000)
        val minutes = (millisUntilFinished % (60 * 60 * 1000)) / (60 * 1000)
        val seconds = (millisUntilFinished % (60 * 1000)) / 1000

        _prayerTimesState.update {
            it.copy(
                countDownTime = CountDownTime(hours, minutes, seconds)
            ).also {
                if (seconds % 30 == 0L) { // Log every 30 seconds to avoid spam
                    Log.v(TAG, "Countdown: ${hours}h ${minutes}m ${seconds}s")
                }
            }
        }
    }

    private suspend fun loadLocation() {
        val isAuto = sharedPreferences.getDataBoolean(LOCATION_TYPE, false)
        Log.d(TAG, "Loading location (Auto: $isAuto)")
        try {
            locationService.loadLocation(isAuto)
                .onSuccess { location ->
                    Log.d(TAG, "Location loaded successfully: ${location.locationName}")
                    _prayerTimesState.update {
                        it.copy(
                            locationName = location.locationName,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }
                }
                .onFailure { throwable ->
                    Log.e(TAG, "Failed to load location: ${throwable.message}", throwable)
                    _error.value = throwable.message
                    loadFallbackLocation()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading location: ${e.message}", e)
            _error.value = e.message
            loadFallbackLocation()
        }
    }

    private fun loadFallbackLocation() {
        Log.d(TAG, "Loading fallback location")
        _prayerTimesState.update {
            it.copy(
                locationName = sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""),
                latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
            ).also { state ->
                Log.d(
                    TAG,
                    "Fallback location loaded: ${state.locationName} (${state.latitude}, ${state.longitude})"
                )
            }
        }
    }

    override fun onCleared() {
        Log.d(TAG, "ViewModel clearing")
        super.onCleared()
        countDownTimer?.cancel()
    }

    private fun PrayerTimesState.isValidLocation() =
        locationName != "Loading..." && latitude != 0.0 && longitude != 0.0

    private fun calculateAndStartTimer(state: PrayerTimesState) {
        val timeToNextPrayer = state.nextPrayerTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val currentTime = LocalDateTime.now()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        startTimer(timeToNextPrayer - currentTime)
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    // Extension functions for data conversion
    private fun PrayerTimesData.toLocalPrayerTimes() = LocalPrayerTimes(
        fajr = fajr,
        sunrise = sunrise,
        dhuhr = dhuhr,
        asr = asr,
        maghrib = maghrib,
        isha = isha
    )

    private fun LocalPrayerTimes.toPrayerTimesData() = PrayerTimesData(
        fajr = fajr ?: LocalDateTime.now(),
        sunrise = sunrise ?: LocalDateTime.now(),
        dhuhr = dhuhr ?: LocalDateTime.now(),
        asr = asr ?: LocalDateTime.now(),
        maghrib = maghrib ?: LocalDateTime.now(),
        isha = isha ?: LocalDateTime.now()
    )
}