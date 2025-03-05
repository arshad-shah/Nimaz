package com.arshadshah.nimaz.viewModel

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.LOCATION_TYPE
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.CountDownTime
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.data.local.models.Parameters
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.LocationStateManager
import com.arshadshah.nimaz.services.PrayerTimesData
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.services.UpdateService
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.widgets.prayertimesthin.PrayerTimeWorker
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.String.valueOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import javax.inject.Inject


@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: PrivateSharedPreferences,
    private val updateService: UpdateService,
    private val locationService: LocationService,
    private val prayerTimesService: PrayerTimesService,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val prayerTrackerRepository: PrayerTrackerRepository,
    private val dataStore: DataStore,
    private val createAlarms: CreateAlarms,
    private val locationStateManager: LocationStateManager,
    val firebaseLogger: FirebaseLogger
) : ViewModel() {
    private val TAG = "DashboardViewModel"
    private var countDownTimer: CountDownTimer? = null

    // Feature States
    private val _locationState = MutableStateFlow(LocationState())
    val locationState = _locationState.asStateFlow()

    private val _prayerTimesState = MutableStateFlow(PrayerTimesState())
    val prayerTimesState = _prayerTimesState.asStateFlow()

    private val _trackerState = MutableStateFlow(TrackerState())
    val trackerState = _trackerState.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState = _updateState.asStateFlow()

    private val _quranState = MutableStateFlow(QuranState())
    val quranState = _quranState.asStateFlow()

    private val _tasbihState = MutableStateFlow(TasbihState())
    val tasbihState = _tasbihState.asStateFlow()


    // Location Related Functions
    private suspend fun loadLocation(isAuto: Boolean) {
        _locationState.update { it.copy(isLoading = true, error = null) }
        // Log location loading attempt
        firebaseLogger.logEvent(
            "dashboard_loading_location",
            mapOf("auto_location" to isAuto),
            FirebaseLogger.Companion.EventCategory.PERFORMANCE
        )
        try {
            locationService.loadLocation(isAuto)
                .onSuccess { location ->
                    _locationState.update { state ->
                        state.copy(
                            locationName = location.locationName,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isLoading = false
                        )
                    }
                    // Update dependent features
                    updatePrayerTimes(PrayerTimesParamMapper.getParams(context))
                    updateWidget(context)
                    loadPrayerTimes()
                }
                .onFailure { throwable ->
                    locationStateManager.updateLocationState(
                        LocationStateManager.LocationState.Error(
                            throwable.message ?: "Failed to load location"
                        )
                    )
                    // Log location loading failure
                    firebaseLogger.logError(
                        "dashboard_location_loading_failed",
                        throwable.message ?: "Failed to load location",
                        mapOf("auto_location" to isAuto)
                    )
                    loadFallbackLocation()
                }
        } catch (e: Exception) {
            _locationState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
            // Log location loading exception
            firebaseLogger.logError(
                "dashboard_location_loading_exception",
                e.message ?: "Unknown error occurred",
                mapOf("auto_location" to isAuto)
            )
            loadFallbackLocation()
        }
    }

    private suspend fun loadFallbackLocation() {
        _locationState.update { state ->
            state.copy(
                locationName = sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""),
                latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603),
                isLoading = false
            )
        }
    }

    private fun updateWidget(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            PrayerTimeWorker.enqueue(context, true)
        }
    }

    // Prayer Times Related Functions
    private suspend fun updatePrayerTimes(parameters: Parameters) {
        _prayerTimesState.update { it.copy(isLoading = true, error = null) }
        // Log prayer times update attempt
        firebaseLogger.logEvent(
            "dashboard_updating_prayer_times",
            mapOf(
                "calculation_method" to valueOf(parameters.method),
                "latitude" to parameters.latitude,
                "longitude" to parameters.longitude
            ),
            FirebaseLogger.Companion.EventCategory.PERFORMANCE
        )
        try {
            val response = withContext(Dispatchers.IO) {
                prayerTimesRepository.updatePrayerTimes(parameters)
            }

            response.data?.let { data ->
                _prayerTimesState.update { state ->
                    state.copy(
                        prayerTimes = PrayerTimesData(
                            fajr = data.fajr,
                            sunrise = data.sunrise,
                            dhuhr = data.dhuhr,
                            asr = data.asr,
                            maghrib = data.maghrib,
                            isha = data.isha
                        ),
                        isLoading = false
                    )
                }
                // Log prayer times update success
                firebaseLogger.logEvent(
                    "dashboard_prayer_times_updated",
                    null,
                    FirebaseLogger.Companion.EventCategory.PERFORMANCE
                )
            } ?:  run {
                _prayerTimesState.update {
                    it.copy(
                        error = "Failed to update prayer times. Data is null.",
                        isLoading = false
                    )
                }

                // Log prayer times update failure
                firebaseLogger.logError(
                    "dashboard_prayer_times_update_failed",
                    "Failed to update prayer times. Data is null.",
                    mapOf("parameters" to parameters.toString())
                )
            }
        } catch (e: Exception) {
            _prayerTimesState.update {
                it.copy(
                    error = e.message ?: "Failed to update prayer times",
                    isLoading = false
                )
            }
            // Log prayer times update exception
            firebaseLogger.logError(
                "dashboard_prayer_times_update_exception",
                e.message ?: "Failed to update prayer times",
                mapOf("parameters" to parameters.toString())
            )
        }
    }

    private suspend fun loadPrayerTimes() = withContext(Dispatchers.IO) {
        _prayerTimesState.update { it.copy(isLoading = true, error = null) }
        try {
            val prayerTimes = prayerTimesService.getPrayerTimes()
            if (prayerTimes != null) {
                _prayerTimesState.update { state ->
                    state.copy(
                        prayerTimes = prayerTimes,
                        isLoading = false
                    )
                }
            }
        } catch (e: Exception) {
            _prayerTimesState.update {
                it.copy(
                    error = "Error fetching prayer times: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun getCurrentAndNextPrayerTimes() = withContext(Dispatchers.IO) {
        _prayerTimesState.update { it.copy(isLoading = true, error = null) }
        try {
            val prayerTimes = prayerTimesService.getPrayerTimes()
            if (prayerTimes != null) {
                val currentAndNextPrayer = prayerTimes.let {
                    prayerTimesService.getCurrentAndNextPrayer(it)
                }
                _prayerTimesState.update { state ->
                    state.copy(
                        currentPrayer = currentAndNextPrayer.currentPrayer,
                        currentPrayerTime = currentAndNextPrayer.currentPrayerTime,
                        nextPrayer = currentAndNextPrayer.nextPrayer,
                        nextPrayerTime = currentAndNextPrayer.nextPrayerTime,
                        isLoading = false
                    )
                }
            }
            firebaseLogger.logEvent(
                "dashboard_current_next_prayer_times_fetched",
                null,
                FirebaseLogger.Companion.EventCategory.PERFORMANCE
            )
        } catch (e: Exception) {
            _prayerTimesState.update {
                it.copy(
                    error = "Error fetching current/next prayer times: ${e.message}",
                    isLoading = false
                )
            }
            firebaseLogger.logError(
                "dashboard_current_next_prayer_times_fetch_failed",
                e.message ?: "Error fetching current/next prayer times",
                null
            )
        }
    }

    //function to clear error message for all states
    fun clearError() {
        // Log error clearing
        firebaseLogger.logEvent(
            "dashboard_clear_errors",
            null,
            FirebaseLogger.Companion.EventCategory.USER_ACTION
        )
        _locationState.update { it.copy(error = null) }
        _prayerTimesState.update { it.copy(error = null) }
        _trackerState.update { it.copy(error = null) }
        _updateState.update { it.copy(error = null) }
        _quranState.update { it.copy(error = null) }
        _tasbihState.update { it.copy(error = null) }
    }

    private suspend fun setAlarms(context: Context) {
        _prayerTimesState.update { it.copy(isLoading = true, error = null) }
        // Log alarm setting attempt
        firebaseLogger.logEvent(
            "dashboard_setting_alarms_started",
            null,
            FirebaseLogger.Companion.EventCategory.PERFORMANCE
        )
        try {
            loadLocation(sharedPreferences.getDataBoolean(LOCATION_TYPE, false))
            loadPrayerTimes()
            getCurrentAndNextPrayerTimes()

            val timeToNextPrayerLong = prayerTimesState.value.nextPrayerTime
                .atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            val currentTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            timeToNextPrayerLong?.minus(currentTime)?.let { difference ->
                startTimer(difference)
            }

            if (prayerTimesState.value.prayerTimes.areAllTimesAvailable() &&
                !sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK, false)
            ) {
                with(prayerTimesState.value.prayerTimes) {
                    createAlarms.exact(
                        context,
                        fajr!!,
                        sunrise!!,
                        dhuhr!!,
                        asr!!,
                        maghrib!!,
                        isha!!,
                    )
                }
                sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, true)
                // Log alarms created
                firebaseLogger.logEvent(
                    "dashboard_alarms_created",
                    null,
                    FirebaseLogger.Companion.EventCategory.PERFORMANCE
                )
            }
            _prayerTimesState.update { it.copy(isLoading = false) }
            // Log alarm setting completed
            firebaseLogger.logEvent(
                "dashboard_setting_alarms_completed",
                null,
                FirebaseLogger.Companion.EventCategory.PERFORMANCE
            )
        } catch (e: Exception) {
            _prayerTimesState.update {
                it.copy(
                    error = "Error setting alarms: ${e.message}",
                    isLoading = false
                )
            }
            // Log alarm setting error
            firebaseLogger.logError(
                "dashboard_setting_alarms_failed",
                e.message ?: "Error setting alarms",
                null
            )
        }
    }

    private fun startTimer(timeToNextPrayer: Long) {
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

                _prayerTimesState.update { state ->
                    state.copy(
                        countDownTime = CountDownTime(
                            hours = elapsedHours,
                            minutes = elapsedMinutes,
                            seconds = elapsedSeconds
                        )
                    )
                }
            }

            override fun onFinish() {
                viewModelScope.launch { getCurrentAndNextPrayerTimes() }
            }
        }.start()
    }

    private fun PrayerTimesData.areAllTimesAvailable(): Boolean {
        return fajr != null && sunrise != null && dhuhr != null &&
                asr != null && maghrib != null && isha != null
    }

    // Tracker Related Functions
    private suspend fun isFastingToday() = withContext(Dispatchers.IO) {
        _trackerState.update { it.copy(isLoading = true, error = null) }
        try {
            dataStore.isFastingForDate(LocalDate.now())
                .collect { isFasting ->
                    _trackerState.update { state ->
                        state.copy(
                            isFasting = isFasting,
                            isLoading = false
                        )
                    }
                    val today = LocalDate.now()
                    val todayHijri = HijrahDate.from(today)
                    val isRamadan = todayHijri[ChronoField.MONTH_OF_YEAR] == 9 &&
                            todayHijri[ChronoField.DAY_OF_MONTH] <= 29
                    if (isFasting || isRamadan) fajrAndMaghribTimes()
                }
            firebaseLogger.logEvent(
                "dashboard_fasting_status_fetched",
                null,
                FirebaseLogger.Companion.EventCategory.PERFORMANCE
            )
        } catch (e: Exception) {
            _trackerState.update {
                it.copy(
                    isFasting = false,
                    error = "Error checking fasting status: ${e.message}",
                    isLoading = false
                )
            }
            firebaseLogger.logError(
                "dashboard_fasting_status_fetch_failed",
                e.message ?: "Error checking fasting status",
                null
            )
        }
    }

    private suspend fun fajrAndMaghribTimes() = withContext(Dispatchers.IO) {
        _trackerState.update { it.copy(isLoading = true, error = null) }
        try {
            val prayerTimes = prayerTimesService.getPrayerTimes()
            _trackerState.update { state ->
                state.copy(
                    fajrTime = prayerTimes?.fajr ?: LocalDateTime.now(),
                    maghribTime = prayerTimes?.maghrib ?: LocalDateTime.now(),
                    isLoading = false
                )

            }
            firebaseLogger.logEvent(
                "dashboard_fajr_maghrib_times_fetched",
                null,
                FirebaseLogger.Companion.EventCategory.PERFORMANCE
            )

        } catch (e: Exception) {
            _trackerState.update {
                it.copy(
                    error = "Error fetching prayer times: ${e.message}",
                    isLoading = false
                )
            }
            firebaseLogger.logError(
                "dashboard_fajr_maghrib_times_fetch_failed",
                e.message ?: "Error fetching prayer times",
                null
            )
        }
    }

    private suspend fun updatePrayerTrackerForToday(
        date: LocalDate,
        prayerName: String,
        prayerDone: Boolean
    ) = withContext(Dispatchers.IO) {
        _trackerState.update { it.copy(isLoading = true, error = null) }
        // Log prayer tracker update
        firebaseLogger.logEvent(
            "dashboard_prayer_tracker_update",
            mapOf(
                "date" to date.toString(),
                "prayer_name" to prayerName,
                "prayer_done" to prayerDone
            ),
            FirebaseLogger.Companion.EventCategory.USER_ACTION
        )
        try {
            prayerTrackerRepository.updateSpecificPrayer(date, prayerName, prayerDone)
            prayerTrackerRepository.observePrayersForDate(date)
                .collect { tracker ->
                    _trackerState.update { state ->
                        state.copy(
                            date = tracker.date,
                            fajr = tracker.fajr,
                            dhuhr = tracker.dhuhr,
                            asr = tracker.asr,
                            maghrib = tracker.maghrib,
                            isha = tracker.isha,
                            progress = tracker.progress,
                            isMenstruating = tracker.isMenstruating,
                            isLoading = false
                        )
                    }
                }
        } catch (e: Exception) {
            _trackerState.update {
                it.copy(
                    error = "Error updating prayer tracker: ${e.message}",
                    isLoading = false
                )
            }
            // Log prayer tracker update error
            firebaseLogger.logError(
                "dashboard_prayer_tracker_update_failed",
                e.message ?: "Error updating prayer tracker",
                mapOf(
                    "date" to date.toString(),
                    "prayer_name" to prayerName
                )
            )
        }
    }

    private suspend fun getTodaysPrayerTracker(date: LocalDate) = withContext(Dispatchers.IO) {
        _trackerState.update { it.copy(isLoading = true, error = null) }
        try {
            prayerTrackerRepository.observePrayersForDate(date)
                .collect { tracker ->
                    _trackerState.update { state ->
                        state.copy(
                            date = tracker.date,
                            fajr = tracker.fajr,
                            dhuhr = tracker.dhuhr,
                            asr = tracker.asr,
                            maghrib = tracker.maghrib,
                            isha = tracker.isha,
                            progress = tracker.progress,
                            isMenstruating = tracker.isMenstruating,
                            isLoading = false
                        )
                    }
                }
        } catch (e: Exception) {
            _trackerState.update {
                it.copy(
                    error = "Error getting prayer tracker: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun updateFastingTracker(
        date: LocalDate,
        isFasting: Boolean
    ) = withContext(Dispatchers.IO) {
        _trackerState.update { it.copy(isLoading = true, error = null) }
        try {
            dataStore.updateFastTracker(LocalFastTracker(date = date, isFasting = isFasting))
            isFastingToday()
            _trackerState.update { it.copy(isLoading = false) }
        } catch (e: Exception) {
            _trackerState.update {
                it.copy(
                    error = "Error updating fasting tracker: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // Update Related Functions
    private fun handleCheckUpdate(event: DashboardEvent.CheckUpdate) {
        _updateState.update { it.copy(isLoading = true, error = null) }
        // Log update check
        firebaseLogger.logEvent(
            "dashboard_update_check",
            mapOf("update_type" to event.updateType),
            FirebaseLogger.Companion.EventCategory.USER_ACTION
        )
        updateService.checkForUpdate(event.updateType) { result ->
            result.onSuccess { isUpdateAvailable ->
                _updateState.update { state ->
                    state.copy(
                        isUpdateAvailable = isUpdateAvailable,
                        isLoading = false
                    )
                }
                // Log update check result
                firebaseLogger.logEvent(
                    "dashboard_update_check_result",
                    mapOf("update_available" to isUpdateAvailable),
                    FirebaseLogger.Companion.EventCategory.USER_ACTION
                )
            }.onFailure { error ->
                //if error code is -6 then do not show error message
                if (error.message?.contains("-6") == false) {
                    _updateState.update { state ->
                        state.copy(
                            isUpdateAvailable = false,
                            error = error.message ?: "Update check failed",
                            isLoading = false
                        )
                    }
                    // Log update check error
                    firebaseLogger.logError(
                        "dashboard_update_check_failed",
                        error.message ?: "Update check failed",
                        null
                    )
                }
            }
        }
    }

    private suspend fun handleStartUpdate(event: DashboardEvent.StartUpdate) {
        _updateState.update { it.copy(isLoading = true, error = null) }
        updateService.startUpdateFlow(
            activity = event.activity,
            requestCode = event.requestCode,
            updateType = event.updateType
        ) { result ->
            result.onFailure { error ->
                _updateState.update { state ->
                    state.copy(
                        isUpdateAvailable = false,
                        error = error.message ?: "Update start failed",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun handleRegisterUpdateListener() {
        updateService.registerInstallStateListener { statusCode ->
            when (statusCode.toString()) {
                InstallStatus.PENDING.toString() -> {
                    _updateState.update { state ->
                        state.copy(
                            isLoading = true,
                            isUpdateAvailable = true,
                            error = null
                        )
                    }
                }

                InstallStatus.DOWNLOADING.toString() -> {
                    _updateState.update { state ->
                        state.copy(
                            isLoading = true,
                            isUpdateAvailable = true,
                            error = null
                        )
                    }
                }

                InstallStatus.DOWNLOADED.toString() -> {
                    _updateState.update { state ->
                        state.copy(
                            isLoading = false,
                            isUpdateAvailable = false,
                            error = null
                        )
                    }
                }

                InstallStatus.INSTALLING.toString() -> {
                    _updateState.update { state ->
                        state.copy(
                            isLoading = true,
                            isUpdateAvailable = false,
                            error = null
                        )
                    }
                }

                InstallStatus.INSTALLED.toString() -> {
                    _updateState.update { state ->
                        state.copy(
                            isLoading = false,
                            isUpdateAvailable = false,
                            error = null
                        )
                    }
                }

                InstallStatus.FAILED.toString() -> {
                    _updateState.update { state ->
                        state.copy(
                            isLoading = false,
                            isUpdateAvailable = false,
                            error = "Update installation failed"
                        )
                    }
                }

                InstallStatus.CANCELED.toString() -> {
                    _updateState.update { state ->
                        state.copy(
                            isLoading = false,
                            isUpdateAvailable = false,
                            error = null
                        )
                    }
                }

                else -> {
                    _updateState.update { state ->
                        state.copy(
                            isLoading = false,
                            isUpdateAvailable = false,
                            error = "Unknown update status"
                        )
                    }
                }
            }
        }
    }

    private fun handleUnregisterUpdateListener() {
        updateService.unregisterInstallStateListener()
    }

    // Quran Related Functions
    private suspend fun getBookmarksOfQuran() = withContext(Dispatchers.IO) {
        _quranState.update { it.copy(isLoading = true, error = null) }
        try {
            val bookmarks = dataStore.getBookmarkedAyas()
            val surahList = bookmarks.map { dataStore.getSurahById(it.suraNumber) }
            _quranState.update { state ->
                state.copy(
                    bookmarks = bookmarks,
                    surahList = surahList,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _quranState.update {
                it.copy(
                    error = "Error fetching bookmarks: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun deleteBookmarkFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int
    ) = withContext(Dispatchers.IO) {
        _quranState.update { it.copy(isLoading = true, error = null) }
        try {
            dataStore.deleteBookmarkFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
            val bookmarks = dataStore.getBookmarkedAyas()
            _quranState.update { state ->
                state.copy(
                    bookmarks = bookmarks,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _quranState.update {
                it.copy(
                    error = "Error deleting bookmark: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun getRandomAya() = withContext(Dispatchers.IO) {
        _quranState.update { it.copy(isLoading = true, error = null) }
        try {
            val totalAyas = dataStore.countAllAyat()
            if (totalAyas > 0) {
                val lastFetchedAyaNumber = sharedPreferences.getDataInt(
                    AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED
                )
                val timeOfDay = LocalDateTime.now().hour

                val randomAya = when (timeOfDay) {
                    in 5..11 -> dataStore.getRandomAya().let { aya ->
                        if (aya.juzNumber > 10) getAlternativeAya(1..10) else aya
                    }

                    in 12..17 -> dataStore.getRandomAya().let { aya ->
                        if (aya.juzNumber < 11 || aya.juzNumber > 20)
                            getAlternativeAya(11..20) else aya
                    }

                    else -> dataStore.getRandomAya().let { aya ->
                        if (aya.juzNumber < 21) getAlternativeAya(21..30) else aya
                    }
                }

                val finalAya = if (randomAya.ayaNumberInQuran == lastFetchedAyaNumber) {
                    dataStore.getRandomAya()
                } else randomAya

                val surah = dataStore.getSurahById(finalAya.suraNumber)
                val juz = dataStore.getJuzById(finalAya.juzNumber)

                _quranState.update { state ->
                    state.copy(
                        randomAyaState = RandomAyaState(
                            randomAya = finalAya,
                            surah = surah,
                            juz = juz
                        ),
                        isLoading = false
                    )
                }

                sharedPreferences.saveDataInt(
                    AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED,
                    finalAya.ayaNumberInQuran
                )
                // Log after successful fetch
                firebaseLogger.logEvent(
                    "dashboard_random_aya_fetched",
                    mapOf(
                        "surah_name" to surah.name,
                        "juz_number" to juz.number
                    ),
                    FirebaseLogger.Companion.EventCategory.PERFORMANCE
                )
            }
        } catch (e: Exception) {
            _quranState.update {
                it.copy(
                    error = "Error fetching random aya: ${e.message}",
                    isLoading = false
                )
            }
            firebaseLogger.logError(
                "dashboard_random_aya_fetch_failed",
                e.message ?: "Error fetching random aya",
                null
            )
        }
    }

    private suspend fun getAlternativeAya(juzRange: IntRange): LocalAya {
        var attempts = 0
        var aya: LocalAya
        do {
            aya = dataStore.getRandomAya()
            attempts++
        } while (aya.juzNumber !in juzRange && attempts < 3)
        return aya
    }

    // Tasbih Related Functions
    private suspend fun recreateTasbih(date: LocalDate) = withContext(Dispatchers.IO) {
        _tasbihState.update { it.copy(isLoading = true, error = null) }
        try {
            val tasbihList = dataStore.getAllTasbih()
            val yesterdayTasbihList = tasbihList.filter { it.date == date.minusDays(1) }
            val todayTasbihList = tasbihList.filter { it.date == date }

            yesterdayTasbihList.forEach { yesterdayTasbih ->
                val exists = todayTasbihList.any {
                    it.arabicName == yesterdayTasbih.arabicName ||
                            it.goal == yesterdayTasbih.goal
                }

                if (!exists) {
                    dataStore.saveTasbih(
                        yesterdayTasbih.copy(
                            id = 0,
                            count = 0,
                            date = date
                        )
                    )
                }
            }

            getTasbihList(date)
            _tasbihState.update { it.copy(isLoading = false) }
        } catch (e: Exception) {
            _tasbihState.update {
                it.copy(
                    error = "Error recreating tasbih: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun getTasbihList(date: LocalDate) {
        _tasbihState.update { state ->
            state.copy(
                tasbihList = dataStore.getTasbihForDate(date),
                isLoading = false
            )
        }
    }

    // Event Handler and Initialization
    fun handleEvent(event: DashboardEvent) {
        viewModelScope.launch {
            firebaseLogger.logEvent(
                "dashboard_event",
                mapOf("event_type" to event.javaClass.simpleName),
                FirebaseLogger.Companion.EventCategory.USER_ACTION
            )
            when (event) {
                is DashboardEvent.LoadLocation ->
                    loadLocation(sharedPreferences.getDataBoolean(LOCATION_TYPE, false))

                is DashboardEvent.GetCurrentAndNextPrayer -> getCurrentAndNextPrayerTimes()
                is DashboardEvent.StartTimer -> startTimer(event.timeToNextPrayer)
                is DashboardEvent.CreateAlarms -> setAlarms(event.context)

                is DashboardEvent.IsFastingToday -> isFastingToday()
                is DashboardEvent.UpdatePrayerTracker ->
                    updatePrayerTrackerForToday(event.date, event.prayerName, event.prayerDone)

                is DashboardEvent.GetTrackerForToday -> getTodaysPrayerTracker(event.date)
                is DashboardEvent.UpdateFastTracker ->
                    updateFastingTracker(event.date, event.isFasting)

                is DashboardEvent.CheckUpdate -> handleCheckUpdate(event)
                is DashboardEvent.StartUpdate -> handleStartUpdate(event)
                is DashboardEvent.RegisterUpdateListener -> handleRegisterUpdateListener()
                is DashboardEvent.UnregisterUpdateListener -> handleUnregisterUpdateListener()

                is DashboardEvent.GetBookmarksOfQuran -> getBookmarksOfQuran()
                is DashboardEvent.DeleteBookmarkFromAya ->
                    deleteBookmarkFromAya(
                        event.ayaNumber,
                        event.surahNumber,
                        event.ayaNumberInSurah
                    )

                is DashboardEvent.GetRandomAya -> getRandomAya()

                is DashboardEvent.RecreateTasbih -> recreateTasbih(event.date)
            }
        }
    }

    fun initializeData(context: Activity) {
        viewModelScope.launch {
            // Log initialization start
            firebaseLogger.logEvent(
                "dashboard_initialization_started",
                null,
                FirebaseLogger.Companion.EventCategory.PERFORMANCE
            )
            withContext(Dispatchers.Main) {
                launch { setAlarms(context) }
                launch { isFastingToday() }
                launch { getCurrentAndNextPrayerTimes() }
                launch { getTodaysPrayerTracker(LocalDate.now()) }
                launch { getBookmarksOfQuran() }
                launch { recreateTasbih(LocalDate.now()) }
                launch { getRandomAya() }
            }
            // Log initialization complete
            firebaseLogger.logEvent(
                "dashboard_initialization_completed",
                null,
                FirebaseLogger.Companion.EventCategory.PERFORMANCE
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        handleUnregisterUpdateListener()

        // Log ViewModel cleared
        firebaseLogger.logEvent(
            "dashboard_viewmodel_cleared",
            null,
            FirebaseLogger.Companion.EventCategory.LIFECYCLE
        )
    }

}

sealed class DashboardEvent {
    // Location Events
    data object LoadLocation : DashboardEvent()

    // Prayer Times Events
    data object GetCurrentAndNextPrayer : DashboardEvent()
    data class StartTimer(val timeToNextPrayer: Long) : DashboardEvent()
    data class CreateAlarms(val context: Context) : DashboardEvent()

    // Tracker Events
    data object IsFastingToday : DashboardEvent()
    data class UpdatePrayerTracker(
        val date: LocalDate,
        val prayerName: String,
        val prayerDone: Boolean
    ) : DashboardEvent()

    data class GetTrackerForToday(val date: LocalDate) : DashboardEvent()
    data class UpdateFastTracker(val date: LocalDate, val isFasting: Boolean) : DashboardEvent()

    // Update Events
    data class CheckUpdate(
        val activity: Activity,
        val updateType: Int = AppUpdateType.IMMEDIATE
    ) : DashboardEvent()

    data class StartUpdate(
        val activity: Activity,
        val requestCode: Int = AppConstants.APP_UPDATE_REQUEST_CODE,
        val updateType: Int = AppUpdateType.IMMEDIATE
    ) : DashboardEvent()

    data object RegisterUpdateListener : DashboardEvent()
    data object UnregisterUpdateListener : DashboardEvent()

    // Quran Events
    data object GetBookmarksOfQuran : DashboardEvent()
    data class DeleteBookmarkFromAya(
        val ayaNumber: Int,
        val surahNumber: Int,
        val ayaNumberInSurah: Int,
    ) : DashboardEvent()

    data object GetRandomAya : DashboardEvent()

    // Tasbih Events
    data class RecreateTasbih(val date: LocalDate) : DashboardEvent()
}

data class LocationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class PrayerTimesState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPrayer: String = "",
    val nextPrayer: String = "",
    val currentPrayerTime: LocalDateTime = LocalDateTime.now(),
    val nextPrayerTime: LocalDateTime = LocalDateTime.now(),
    val countDownTime: CountDownTime = CountDownTime(0, 0, 0),
    val prayerTimes: PrayerTimesData = PrayerTimesData()
)

data class TrackerState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val date: LocalDate = LocalDate.now(),
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val progress: Int = 0,
    val isMenstruating: Boolean = false,
    val isFasting: Boolean = false,
    val fajrTime: LocalDateTime = LocalDateTime.now(),
    val maghribTime: LocalDateTime = LocalDateTime.now()
)

data class UpdateState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdateAvailable: Boolean = false
)

data class QuranState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookmarks: List<LocalAya> = emptyList(),
    val surahList: List<LocalSurah> = emptyList(),
    val randomAyaState: RandomAyaState? = null
)

data class TasbihState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tasbihList: List<LocalTasbih> = emptyList()
)

data class CountDownTime(
    val hours: Long,
    val minutes: Long,
    val seconds: Long
)

data class RandomAyaState(
    val randomAya: LocalAya,
    val surah: LocalSurah,
    val juz: LocalJuz
)