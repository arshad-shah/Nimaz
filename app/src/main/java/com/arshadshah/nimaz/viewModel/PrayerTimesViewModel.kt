package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerTimesService: PrayerTimesService,
    private val locationService: LocationService,
    private val sharedPreferences: PrivateSharedPreferences,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val createAlarms: CreateAlarms
) : ViewModel() {

    // Data classes and sealed interfaces remain the same for API compatibility
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
    private var activeJobs = mutableMapOf<String, Job>()

    companion object {
        private const val TAG = "Nimaz: PrayerTimesVM"
        private const val OPERATION_TIMEOUT = 30L // seconds
    }

    init {
        launchSafely("initialization") {
            supervisorScope {
                launch { initialize(context) }
            }
        }
    }

    private fun launchSafely(operationName: String, block: suspend () -> Unit): Job {
        activeJobs[operationName]?.cancel()
        return viewModelScope.launch {
            try {
                withTimeout(OPERATION_TIMEOUT.seconds) {
                    block()
                }
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> throw e
                    else -> {
                        Log.e(TAG, "Error in $operationName: ${e.message}", e)
                        _error.value = "Error in $operationName: ${e.message}"
                    }
                }
            } finally {
                activeJobs.remove(operationName)
            }
        }.also { activeJobs[operationName] = it }
    }

    fun handleEvent(event: PrayerTimesEvent) {
        Log.d(TAG, "Handling event: $event")
        when (event) {
            is PrayerTimesEvent.Start -> launchSafely("timer") {
                withContext(Dispatchers.Main) {
                    startTimer(event.timeToNextPrayer)
                }
            }

            is PrayerTimesEvent.RELOAD -> launchSafely("reload") { loadPrayerTimes() }
            is PrayerTimesEvent.UPDATE_PRAYERTIMES -> launchSafely("updatePrayerTimes") {
                updatePrayerTimes(event.mapOfParameters)
            }

            is PrayerTimesEvent.UPDATE_WIDGET -> launchSafely("updateWidget") {
                updateWidget(event.context)
            }

            is PrayerTimesEvent.SET_LOADING -> setLoading(event.isLoading)
            is PrayerTimesEvent.SET_ALARMS -> launchSafely("setAlarms") {
                setAlarms(event.context)
            }

            is PrayerTimesEvent.LOAD_LOCATION -> launchSafely("loadLocation") { loadLocation() }
            is PrayerTimesEvent.Init -> launchSafely("init") { initialize(event.context) }
        }
    }

    private suspend fun initialize(context: Context) = withContext(Dispatchers.Default) {
        ViewModelLogger.d(TAG, "⭐ Starting initialization sequence")
        setLoading(true)

        try {
            ViewModelLogger.d(TAG, "📍 Stage 1: Loading location and prayer times")
            supervisorScope {
                val locationJob = launch {
                    ViewModelLogger.d(TAG, "🌍 Loading location")
                    loadLocation()
                    ViewModelLogger.d(
                        TAG,
                        "🌍 Location loaded: ${prayerTimesState.value.locationName}"
                    )
                }

                val prayerTimesJob = launch {
                    ViewModelLogger.d(TAG, "🕌 Loading prayer times")
                    loadPrayerTimes()
                    ViewModelLogger.d(TAG, "🕌 Prayer times loaded")
                }

                // Wait for critical data to be loaded
                locationJob.join()
                prayerTimesJob.join()

                ViewModelLogger.d(TAG, "📍 Stage 2: Checking alarm lock status")
                if (!sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK, false)) {
                    ViewModelLogger.d(TAG, "⏰ Setting up alarms")
                    launch {
                        setAlarms(context)
                        ViewModelLogger.d(TAG, "⏰ Alarms setup completed")
                    }
                } else {
                    ViewModelLogger.d(TAG, "⏰ Alarms are locked, skipping setup")
                }

                ViewModelLogger.d(TAG, "📍 Stage 3: Validating location data")
                if (prayerTimesState.value.isValidLocation()) {
                    ViewModelLogger.d(TAG, "✅ Location is valid, proceeding with updates")

                    supervisorScope {
                        launch {
                            ViewModelLogger.d(TAG, "🔄 Updating prayer times with parameters")
                            updatePrayerTimes(PrayerTimesParamMapper.getParams(context))
                            ViewModelLogger.d(TAG, "🔄 Prayer times update completed")
                        }

                        launch {
                            ViewModelLogger.d(TAG, "🔄 Updating widget")
                            updateWidget(context)
                            ViewModelLogger.d(TAG, "🔄 Widget update completed")
                        }

                        withContext(Dispatchers.Main) {
                            ViewModelLogger.d(TAG, "⏲️ Starting countdown timer")
                            calculateAndStartTimer(prayerTimesState.value)
                            ViewModelLogger.d(TAG, "⏲️ Timer started successfully")
                        }
                    }
                } else {
                    ViewModelLogger.e(TAG, "❌ Invalid location data: ${prayerTimesState.value}")
                    _error.value = "Invalid location data"
                }
            }
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Initialization failed", e)
            _error.value = "Initialization failed: ${e.message}"
        } finally {
            ViewModelLogger.d(TAG, "🏁 Initialization sequence completed")
            setLoading(false)
        }
    }

    private suspend fun setAlarms(context: Context) {
        Log.d(TAG, "Setting alarms")
        withContext(Dispatchers.Default) {
            loadPrayerTimes()
            _prayerTimesState.value.let { state ->
                createAlarms.exact(
                    context,
                    state.fajrTime,
                    state.sunriseTime,
                    state.dhuhrTime,
                    state.asrTime,
                    state.maghribTime,
                    state.ishaTime,
                )
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        withContext(Dispatchers.IO) {
            PrayerTimeWorker.enqueue(context, true)
        }
    }

    private suspend fun updatePrayerTimes(parameters: Parameters) {
        _error.value = null
        val response = withContext(Dispatchers.IO) {
            prayerTimesRepository.updatePrayerTimes(parameters)
        }

        response.data?.let {
            updatePrayerTimesState(it)
        } ?: throw IllegalStateException("Failed to update prayer times. Data is null.")
    }

    private suspend fun updatePrayerTimesState(data: LocalPrayerTimes) {
        ViewModelLogger.d(TAG, "🔄 Updating prayer times state with data: $data")

        try {
            val currentAndNextPrayertimes = withContext(Dispatchers.Default) {
                ViewModelLogger.d(TAG, "🕒 Calculating current and next prayer times")
                prayerTimesService.getCurrentAndNextPrayer(data.toPrayerTimesData())
            }

            ViewModelLogger.d(TAG, "📊 Current prayer: ${currentAndNextPrayertimes.currentPrayer}")
            ViewModelLogger.d(TAG, "📊 Next prayer: ${currentAndNextPrayertimes.nextPrayer}")

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
                )
            }
            ViewModelLogger.d(TAG, "✅ Prayer times state updated successfully")
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Failed to update prayer times state", e)
            throw e
        }
    }

    private suspend fun loadPrayerTimes() {
        ViewModelLogger.d(TAG, "📥 Starting prayer times load")
        _error.value = null

        try {
            val prayerTimes = withContext(Dispatchers.IO) {
                ViewModelLogger.d(TAG, "🔄 Fetching prayer times from service")
                prayerTimesService.getPrayerTimes()
            } ?: throw IllegalStateException("Failed to load prayer times")

            ViewModelLogger.d(TAG, "✅ Prayer times loaded successfully: $prayerTimes")
            updatePrayerTimesState(prayerTimes.toLocalPrayerTimes())
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Failed to load prayer times", e)
            throw e
        }
    }

    private fun startTimer(timeToNextPrayer: Long) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            viewModelScope.launch(Dispatchers.Main) {
                createAndStartTimer(timeToNextPrayer)
            }
            return
        }
        createAndStartTimer(timeToNextPrayer)
    }

    private fun createAndStartTimer(timeToNextPrayer: Long) {
        ViewModelLogger.d(TAG, "⏲️ Creating new timer with duration: ${timeToNextPrayer}ms")
        countDownTimer?.let {
            ViewModelLogger.d(TAG, "🛑 Cancelling existing timer")
            it.cancel()
        }

        countDownTimer = object : CountDownTimer(timeToNextPrayer, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished % 60000 == 0L) { // Log every minute
                    ViewModelLogger.v(
                        TAG,
                        "⏱️ Timer tick: ${millisUntilFinished / 1000}s remaining"
                    )
                }
                updateCountdownState(millisUntilFinished)
            }

            override fun onFinish() {
                ViewModelLogger.d(TAG, "🏁 Timer finished, reloading prayer times")
                launchSafely("timerFinish") { loadPrayerTimes() }
            }
        }.start()
        ViewModelLogger.d(TAG, "✅ Timer started successfully")
    }

    private fun updateCountdownState(millisUntilFinished: Long) {
        val hours = millisUntilFinished / (60 * 60 * 1000)
        val minutes = (millisUntilFinished % (60 * 60 * 1000)) / (60 * 1000)
        val seconds = (millisUntilFinished % (60 * 1000)) / 1000

        _prayerTimesState.update {
            it.copy(countDownTime = CountDownTime(hours, minutes, seconds))
        }
    }

    private suspend fun loadLocation() {
        val isAuto = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, false)
        ViewModelLogger.d(TAG, "📍 Loading location (Auto mode: $isAuto)")

        try {
            locationService.loadLocation(isAuto)
                .onSuccess { location ->
                    ViewModelLogger.d(
                        TAG,
                        "✅ Location loaded successfully: ${location.locationName} (${location.latitude}, ${location.longitude})"
                    )
                    _prayerTimesState.update {
                        it.copy(
                            locationName = location.locationName,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }
                }
                .onFailure { throwable ->
                    ViewModelLogger.e(TAG, "❌ Failed to load location", throwable)
                    _error.value = throwable.message
                    ViewModelLogger.d(TAG, "⚠️ Attempting to load fallback location")
                    loadFallbackLocation()
                }
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error loading location", e)
            _error.value = e.message
            loadFallbackLocation()
        }
    }

    private fun loadFallbackLocation() {
        ViewModelLogger.d(TAG, "🔄 Loading fallback location")
        _prayerTimesState.update {
            it.copy(
                locationName = sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""),
                latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
            ).also { state ->
                ViewModelLogger.d(
                    TAG,
                    "✅ Fallback location loaded: ${state.locationName} (${state.latitude}, ${state.longitude})"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        activeJobs.values.forEach { it.cancel() }
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