package com.arshadshah.nimaz.viewModel

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.*
import com.arshadshah.nimaz.repositories.*
import com.arshadshah.nimaz.services.*
import com.arshadshah.nimaz.utils.*
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DashboardViewModel(context: Context) : ViewModel() {
    private val TAG = "DashboardViewModel"

    private val sharedPreferences = PrivateSharedPreferences(context)
    private val updateService = UpdateService(context)
    private val locationService = LocationService(context, LocationRepository(context))
    private val prayerTimesService = PrayerTimesService(context, PrayerTimesRepository)
    private val dataStore by lazy { LocalDataStore.getDataStore() }

    // State management using StateFlow
    private val states = DashboardStates()

    // Public StateFlow exposures
    val isUpdateAvailable = states.isUpdateAvailable.asStateFlow()
    val isLoading = states.isLoading.asStateFlow()
    val isError = states.isError.asStateFlow()
    val locationName = states.locationName.asStateFlow()
    val latitude = states.latitude.asStateFlow()
    val longitude = states.longitude.asStateFlow()
    val isFasting = states.isFasting.asStateFlow()
    var fajrTime = states.fajrTime.asStateFlow()
    val maghribTime = states.maghribTime.asStateFlow()
    val tasbihList = states.tasbihList.asStateFlow()
    val trackerState = states.trackerState.asStateFlow()
    val bookmarks = states.bookmarks.asStateFlow()
    val surahList = states.surahList.asStateFlow()
    val randomAyaState: LiveData<RandomAyaState> = states.randomAyaState
    val error = states.error.asStateFlow()
    val currentPrayerName = states.currentPrayerName.asStateFlow()
    val currentPrayerTime = states.currentPrayerTime.asStateFlow()
    val nextPrayerName = states.nextPrayerName.asStateFlow()
    val nextPrayerTime = states.nextPrayerTime.asStateFlow()
    val countDownTime = states.countDownTime.asStateFlow()
    val prayerTimes = states.prayerTimes.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    fun initializeData(context: Activity) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                launch { setAlarms(context) }
                launch { checkForUpdate(context, false) }
                launch { isFastingToday() }
                launch { getCurrentAndNextPrayerTimes() }
                launch { getTodaysPrayerTracker(LocalDate.now()) }
                launch { getBookmarksOfQuran() }
                launch { recreateTasbih(LocalDate.now()) }
                launch { getRandomAya() }
            }
        }
    }

    fun handleEvent(event: DashboardEvent) {
        viewModelScope.launch {
            when (event) {
                is DashboardEvent.CheckUpdate -> checkForUpdate(event.context, event.doUpdate)
                is DashboardEvent.LoadLocation -> loadLocation()
                is DashboardEvent.IsFastingToday -> isFastingToday()
                is DashboardEvent.UpdatePrayerTracker ->
                    updatePrayerTrackerForToday(event.date, event.prayerName, event.prayerDone)
                is DashboardEvent.GetTrackerForToday -> getTodaysPrayerTracker(event.date)
                is DashboardEvent.UpdateFastTracker ->
                    updateFastingTracker(event.date, event.isFasting)
                is DashboardEvent.GetBookmarksOfQuran -> getBookmarksOfQuran()
                is DashboardEvent.DeleteBookmarkFromAya ->
                    deleteBookmarkFromAya(event.ayaNumber, event.surahNumber, event.ayaNumberInSurah)
                is DashboardEvent.RecreateTasbih -> recreateTasbih(event.date)
                is DashboardEvent.GetRandomAya -> getRandomAya()
                is DashboardEvent.StartTimer -> startTimer(event.timeToNextPrayer)
                is DashboardEvent.GetCurrentAndNextPrayer -> getCurrentAndNextPrayerTimes()
                is DashboardEvent.CreateAlarms -> setAlarms(event.context)
            }
        }
    }
    private fun PrayerTimesData.areAllTimesAvailable(): Boolean {
        return fajr != null &&
                sunrise != null &&
                dhuhr != null &&
                asr != null &&
                maghrib != null &&
                isha != null
    }

    private suspend fun setAlarms(context: Context) {
        loadLocation()
        loadPrayerTimes()
        getCurrentAndNextPrayerTimes()
        val timeToNextPrayerLong =
            nextPrayerTime.value.atZone(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
        val currentTime =
            LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()

        val difference = timeToNextPrayerLong?.minus(currentTime)
        if (difference != null) {
            startTimer(difference)
        }
        val arePrayerTimesAvailable = prayerTimes.value.areAllTimesAvailable()
        if(arePrayerTimesAvailable){
            CreateAlarms().exact(
                context,
                prayerTimes.value.fajr!!,
                prayerTimes.value.sunrise!!,
                prayerTimes.value.dhuhr!!,
                prayerTimes.value.asr!!,
                prayerTimes.value.maghrib!!,
                prayerTimes.value.isha!!,
            )
        }
    }

    private suspend fun loadPrayerTimes() = withContext(Dispatchers.IO){
        safeOperation(
            operation = {
                val prayerTimes = prayerTimesService.getPrayerTimes()
                if (prayerTimes != null) {
                    states.prayerTimes.value = prayerTimes
                }
            },
            errorMessage = "Error fetching prayer times"
        )
    }

    private fun checkForUpdate(context: Activity, doUpdate: Boolean) {
        updateService.checkForUpdate(doUpdate) { updateIsAvailable ->
            states.isUpdateAvailable.value = updateIsAvailable
            if (doUpdate && updateIsAvailable) {
                updateService.startUpdateFlowForResult(context, AppConstants.APP_UPDATE_REQUEST_CODE)
            }
        }
    }

    private suspend fun getBookmarksOfQuran() = withContext(Dispatchers.IO) {
        safeOperation(
            operation = {
                val bookmarks = dataStore.getBookmarkedAyas()
                val surahList = bookmarks.map { dataStore.getSurahById(it.suraNumber) }
                states.surahList.value = surahList
                states.bookmarks.value = bookmarks
            },
            errorMessage = "Error fetching bookmarks"
        )
    }

    private suspend fun deleteBookmarkFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int
    ) = withContext(Dispatchers.IO) {
        safeOperation(
            operation = {
                dataStore.deleteBookmarkFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
                states.bookmarks.value = dataStore.getBookmarkedAyas()
            },
            errorMessage = "Error deleting bookmark"
        )
    }

    private suspend fun isFastingToday() = withContext(Dispatchers.IO) {
        dataStore.isFastingForDate(LocalDate.now())
            .catch { states.isFasting.value = false }
            .collect { isFasting ->
                states.isFasting.value = isFasting
                if (isFasting) fajrAndMaghribTimes()
            }
    }

    private suspend fun fajrAndMaghribTimes() = withContext(Dispatchers.IO) {
        safeOperation(
            operation = {
                val prayerTimes = prayerTimesService.getPrayerTimes()
                states.fajrTime.value = prayerTimes?.fajr
                states.maghribTime.value = prayerTimes?.maghrib
            },
            errorMessage = "Error fetching prayer times"
        )
    }

    private suspend fun getCurrentAndNextPrayerTimes() = withContext(Dispatchers.IO) {
        safeOperation(
            operation = {
                val prayerTimes = prayerTimesService.getPrayerTimes()
                val currentAndNextPrayer = prayerTimes?.let {
                    prayerTimesService.getCurrentAndNextPrayer(
                        it
                    )
                }
                states.currentPrayerName.value = currentAndNextPrayer?.currentPrayer.toString()
                states.currentPrayerTime.value = currentAndNextPrayer?.currentPrayerTime
                states.nextPrayerName.value = currentAndNextPrayer?.nextPrayer.toString()
                states.nextPrayerTime.value = currentAndNextPrayer?.nextPrayerTime
            },
            errorMessage = "Error fetching prayer times"
        )
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
                diff %= secondsInMilli

                states.countDownTime.update {
                    it.copy(
                        hours = elapsedHours,
                        minutes = elapsedMinutes,
                        seconds = elapsedSeconds
                    )
                }
            }

            override fun onFinish() {
                viewModelScope.launch { getCurrentAndNextPrayerTimes() }
            }
        }.start()
    }

    private suspend fun updatePrayerTrackerForToday(
        date: LocalDate,
        prayerName: String,
        prayerDone: Boolean
    ) = withContext(Dispatchers.IO) {
        safeOperation(
            operation = {
                val updatedTracker = PrayerTrackerRepository.updateSpecificPrayer(
                    date,
                    prayerName,
                    prayerDone
                )
                states.trackerState.update { it.copy(
                    date = updatedTracker.date,
                    fajr = updatedTracker.fajr,
                    dhuhr = updatedTracker.dhuhr,
                    asr = updatedTracker.asr,
                    maghrib = updatedTracker.maghrib,
                    isha = updatedTracker.isha,
                    progress = updatedTracker.progress,
                    isMenstruating = updatedTracker.isMenstruating
                )}
            },
            errorMessage = "Error updating prayer tracker"
        )
    }

    private suspend fun getTodaysPrayerTracker(date: LocalDate) = withContext(Dispatchers.IO) {
        PrayerTrackerRepository.getPrayersForDate(date)
            .catch { emit(LocalPrayersTracker()) }
            .collect { tracker ->
                states.trackerState.update { it.copy(
                    date = tracker.date,
                    fajr = tracker.fajr,
                    dhuhr = tracker.dhuhr,
                    asr = tracker.asr,
                    maghrib = tracker.maghrib,
                    isha = tracker.isha,
                    progress = tracker.progress,
                    isMenstruating = tracker.isMenstruating
                )}
            }
    }

    private suspend fun updateFastingTracker(
        date: LocalDate,
        isFasting: Boolean
    ) = withContext(Dispatchers.IO) {
        safeOperation(
            operation = {
                dataStore.updateFastTracker(LocalFastTracker(date = date, isFasting = isFasting))
                isFastingToday()
            },
            errorMessage = "Error updating fasting tracker"
        )
    }

    private fun loadLocation() {
        val isAutoLocation = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, true)
        locationService.loadLocation(
            isAutoLocation,
            onSuccess = { location ->
                states.locationName.value = location.locationName
                states.latitude.value = location.latitude
                states.longitude.value = location.longitude
            },
            onError = { error ->
                states.latitude.value = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498)
                states.longitude.value = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
                states.locationName.value = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
                states.isError.value = error.isNotEmpty()
            }
        )
    }

    private suspend fun recreateTasbih(date: LocalDate) = withContext(Dispatchers.IO) {
        safeOperation(
            operation = {
                val tasbihList = dataStore.getAllTasbih()
                val yesterdayTasbihList = tasbihList.filter { it.date == date.minusDays(1) }
                val todayTasbihList = tasbihList.filter { it.date == date }

                yesterdayTasbihList.forEach { yesterdayTasbih ->
                    val exists = todayTasbihList.any {
                        it.arabicName == yesterdayTasbih.arabicName ||
                                it.goal == yesterdayTasbih.goal
                    }

                    if (!exists) {
                        dataStore.saveTasbih(yesterdayTasbih.copy(
                            id = 0,
                            count = 0,
                            date = date
                        ))
                    }
                }

                getTasbihList(date)
            },
            errorMessage = "Error recreating tasbih"
        )
    }

    private fun getTasbihList(date: LocalDate) {
        states.tasbihList.value = dataStore.getTasbihForDate(date)
    }

    private suspend fun getRandomAya() = withContext(Dispatchers.IO) {
        safeOperation(
            operation = {
                val ayas = dataStore.countAllAyat()
                if (ayas > 0) {
                    var randomAya = dataStore.getRandomAya()
                    while (randomAya.ayaNumberInSurah <= 1) {
                        randomAya = dataStore.getRandomAya()
                    }

                    val surah = dataStore.getSurahById(randomAya.suraNumber)
                    val juz = dataStore.getJuzById(randomAya.juzNumber)

                    states.randomAyaState.postValue(RandomAyaState(
                        randomAya = randomAya,
                        surah = surah,
                        juz = juz
                    ))
                    Log.d("RandomAya", "Random Aya: ${states.randomAyaState.value}")

                    sharedPreferences.saveDataInt(
                        AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED,
                        randomAya.ayaNumberInSurah
                    )
                }
            },
            errorMessage = "Error fetching random aya"
        )
    }

    private suspend fun safeOperation(
        operation: suspend () -> Unit,
        errorMessage: String
    ) {
        try {
            states.isError.value = false
            states.isLoading.value = true
            operation()
            states.isLoading.value = false
        } catch (e: Exception) {
            Log.e(TAG, "$errorMessage: ${e.message}")
            states.isError.value = true
            states.error.value = errorMessage
            states.isLoading.value = false
        }
    }

    // Helper class to manage all StateFlow instances
    private inner class DashboardStates {
        val isUpdateAvailable = MutableStateFlow(false)
        val isLoading = MutableStateFlow(false)
        val isError = MutableStateFlow(false)
        val error = MutableStateFlow("")
        val locationName = MutableStateFlow(sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""))
        val latitude = MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0))
        val longitude = MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0))
        val isFasting = MutableStateFlow(false)
        val fajrTime = MutableStateFlow(LocalDateTime.now())
        val maghribTime = MutableStateFlow(LocalDateTime.now())
        val currentPrayerName = MutableStateFlow("")
        val currentPrayerTime = MutableStateFlow(LocalDateTime.now())
        val nextPrayerName = MutableStateFlow("")
        val nextPrayerTime = MutableStateFlow(LocalDateTime.now())
        val tasbihList = MutableStateFlow(listOf<LocalTasbih>())
        val trackerState = MutableStateFlow(DashboardTrackerState(
            LocalDate.now(), false, false, false,
            false, false, 0, false
        ))
        val bookmarks = MutableStateFlow(listOf<LocalAya>())
        val surahList = MutableStateFlow(listOf<LocalSurah>())
        val randomAyaState = MutableLiveData<RandomAyaState>()
        val countDownTime: MutableStateFlow<CountDownTime> = MutableStateFlow(CountDownTime(0, 0, 0))
        val prayerTimes: MutableStateFlow<PrayerTimesData> = MutableStateFlow(PrayerTimesData())
    }

    data class DashboardTrackerState(
        val date: LocalDate,
        val fajr: Boolean,
        val dhuhr: Boolean,
        val asr: Boolean,
        val maghrib: Boolean,
        val isha: Boolean,
        val progress: Int,
        val isMenstruating: Boolean
    )

    data class RandomAyaState(
        val randomAya: LocalAya,
        val surah: LocalSurah,
        val juz: LocalJuz
    )

    sealed class DashboardEvent {
        data object LoadLocation : DashboardEvent()
        data class CheckUpdate(val context: Activity, val doUpdate: Boolean) : DashboardEvent()
        data object IsFastingToday : DashboardEvent()
        data class UpdatePrayerTracker(
            val date: LocalDate,
            val prayerName: String,
            val prayerDone: Boolean
        ) : DashboardEvent()
        data class GetTrackerForToday(val date: LocalDate) : DashboardEvent()
        data class UpdateFastTracker(
            val date: LocalDate,
            val isFasting: Boolean
        ) : DashboardEvent()
        data object GetBookmarksOfQuran : DashboardEvent()
        data class DeleteBookmarkFromAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : DashboardEvent()
        data class RecreateTasbih(val date: LocalDate) : DashboardEvent()
        data object GetRandomAya : DashboardEvent()
        data class StartTimer(val timeToNextPrayer: Long) : DashboardEvent()
        data object GetCurrentAndNextPrayer: DashboardEvent()
        data class CreateAlarms(val context: Context): DashboardEvent()
    }
}