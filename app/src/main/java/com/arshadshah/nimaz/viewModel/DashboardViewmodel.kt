package com.arshadshah.nimaz.viewModel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.repositories.LocationRepository
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.services.UpdateService
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class DashboardViewmodel(context: Context) : ViewModel() {

    val sharedPreferences = PrivateSharedPreferences(context)
    private val updateService = UpdateService(context)
    private val locationService = LocationService(context, LocationRepository(context))
    private val prayerTimesService = PrayerTimesService(context, PrayerTimesRepository)

    private var _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable = _isUpdateAvailable.asStateFlow()

    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var _isError = MutableStateFlow(false)
    val isError = _isError.asStateFlow()

    //location name state
    private var _locationName =
        MutableStateFlow(sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""))
    val locationName = _locationName.asStateFlow()

    //latitude state
    private var _latitude =
        MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0))
    val latitude = _latitude.asStateFlow()

    //longitude state
    private var _longitude =
        MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0))
    val longitude = _longitude.asStateFlow()

    private var _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()

    private var _fajrTime = MutableStateFlow(LocalDateTime.now())
    val fajrTime = _fajrTime.asStateFlow()

    private var _maghribTime = MutableStateFlow(LocalDateTime.now())
    val maghribTime = _maghribTime.asStateFlow()

    private var _tasbihList = MutableStateFlow(
        listOf<LocalTasbih>()
    )
    val tasbihList = _tasbihList.asStateFlow()

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

    private var _trackerState =
        MutableStateFlow(
            DashboardTrackerState(
                LocalDate.now(),
                false,
                false,
                false,
                false,
                false,
                0,
                false
            )
        )
    val trackerState = _trackerState.asStateFlow()

    private val _bookmarks = MutableStateFlow(listOf<LocalAya>())
    val bookmarks = _bookmarks.asStateFlow()

    //initialize data
    fun initializeData(context: Context) {
        checkForUpdate(context, false)
        isFastingToday()
        getTodaysPrayerTracker(LocalDate.now())
        getBookmarksOfQuran()
        recreateTasbih(LocalDate.now())
        getRandomAya()
    }

    sealed class DashboardEvent {
        object LoadLocation : DashboardEvent()
        class CheckUpdate(val context: Context, val doUpdate: Boolean) : DashboardEvent()

        object IsFastingToday : DashboardEvent()

        class UpdatePrayerTracker(
            val date: LocalDate,
            val prayerName: String,
            val prayerDone: Boolean
        ) : DashboardEvent()

        class GetTrackerForToday(val date: LocalDate) : DashboardEvent()

        class UpdateFastTracker(val date: LocalDate, val isFasting: Boolean) : DashboardEvent()

        object GetBookmarksOfQuran : DashboardEvent()
        class DeleteBookmarkFromAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : DashboardEvent()

        class RecreateTasbih(val date: LocalDate) : DashboardEvent()

        object GetRandomAya : DashboardEvent()
    }

    fun checkForUpdate(context: Context, doUpdate: Boolean) {
        updateService.checkForUpdate(doUpdate) { updateIsAvailable ->
            _isUpdateAvailable.value = updateIsAvailable
            if (doUpdate && updateIsAvailable) {
                updateService.startUpdateFlowForResult(
                    context.applicationContext as Activity,
                    AppConstants.APP_UPDATE_REQUEST_CODE
                )
            }
        }
    }

    fun handleEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.CheckUpdate -> {
                Log.d("Nimaz: SettingsViewModel", "Checking for update")
                checkForUpdate(event.context, event.doUpdate)
            }

            is DashboardEvent.LoadLocation -> {
                loadLocation()
            }

            is DashboardEvent.IsFastingToday -> {
                isFastingToday()
            }

            is DashboardEvent.UpdatePrayerTracker -> {
                updatePrayerTrackerForToday(event.date, event.prayerName, event.prayerDone)
            }

            is DashboardEvent.GetTrackerForToday -> {
                getTodaysPrayerTracker(event.date)
            }

            is DashboardEvent.UpdateFastTracker -> {
                updateFastingTracker(event.date, event.isFasting)
            }

            is DashboardEvent.GetBookmarksOfQuran -> {
                getBookmarksOfQuran()
            }

            is DashboardEvent.DeleteBookmarkFromAya -> {
                deleteBookmarkFromAya(event.ayaNumber, event.surahNumber, event.ayaNumberInSurah)
            }

            is DashboardEvent.RecreateTasbih -> {
                recreateTasbih(event.date)
            }

            is DashboardEvent.GetRandomAya -> {
                getRandomAya()
            }
        }
    }


    private val _surahList = MutableStateFlow(listOf<LocalSurah>())
    val surahList = _surahList.asStateFlow()
    //get surah for the aya
    private fun getBookmarksOfQuran() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val bookmarks = dataStore.getBookmarkedAyas()
                //suralist
                val surahList = mutableListOf<LocalSurah>()
                //for each bookmark get the surah
                for (bookmark in bookmarks) {
                    val surah = dataStore.getSurahById(bookmark.suraNumber)
                    surahList.add(surah)
                }
                _surahList.value = surahList
                _bookmarks.value = bookmarks
            } catch (e: Exception) {
                Log.d("getAllBookmarks", e.message ?: "Unknown error")
            }
        }
    }

    private fun deleteBookmarkFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.deleteBookmarkFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
                val bookmarks = dataStore.getBookmarkedAyas()
                _bookmarks.value = bookmarks
            } catch (e: Exception) {
                Log.d("deleteBookmarkFromAya", e.message ?: "Unknown error")
            }
        }
    }

    private fun isFastingToday() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.isFastingForDate(LocalDate.now())
                    .catch { emit(false) }
                    .collect {
                        _isFasting.value = it
                        if (it) {
                            fajrAndMaghribTimes()
                        }
                    }
            } catch (e: Exception) {
                _isFasting.value = false
            }
        }
    }

    private fun fajrAndMaghribTimes() {
        viewModelScope.launch(Dispatchers.IO) {
            _isError.value = false
            _isLoading.value = true
            try {
                val prayerTimes = prayerTimesService.getPrayerTimes()

                Log.d("Nimaz: dashboard viewmodel", "Fajr and Maghrib Times: $prayerTimes")

                _fajrTime.value = prayerTimes?.fajr
                _maghribTime.value = prayerTimes?.maghrib
                _isLoading.value = false
                _isError.value = false
            } catch (e: Exception) {
                _isError.value = true
                _isLoading.value = false
            }
        }
    }

    private fun updatePrayerTrackerForToday(
        date: LocalDate,
        prayerName: String,
        prayerDone: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isError.value = false
            _isLoading.value = true
            try {
                val updatedTracker =
                    PrayerTrackerRepository.updateSpecificPrayer(date, prayerName, prayerDone)
                _trackerState.update {
                    it.copy(
                        date = updatedTracker.date,
                        fajr = updatedTracker.fajr,
                        dhuhr = updatedTracker.dhuhr,
                        asr = updatedTracker.asr,
                        maghrib = updatedTracker.maghrib,
                        isha = updatedTracker.isha,
                        progress = updatedTracker.progress,
                        isMenstruating = updatedTracker.isMenstruating
                    )
                }
                _isLoading.value = false
                _isError.value = false
            } catch (e: Exception) {
                _isError.value = true
            }
        }
    }

    private fun getTodaysPrayerTracker(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                PrayerTrackerRepository.getPrayersForDate(date)
                    .catch { emit(LocalPrayersTracker()) }
                    .collect { prayerTrackerFromStorage ->
                        _trackerState.update {
                            it.copy(
                                date = prayerTrackerFromStorage.date,
                                fajr = prayerTrackerFromStorage.fajr,
                                dhuhr = prayerTrackerFromStorage.dhuhr,
                                asr = prayerTrackerFromStorage.asr,
                                maghrib = prayerTrackerFromStorage.maghrib,
                                isha = prayerTrackerFromStorage.isha,
                                progress = prayerTrackerFromStorage.progress,
                                isMenstruating = prayerTrackerFromStorage.isMenstruating
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.d("Nimaz: dashboard viewmodel", "Error getting today's prayer tracker:'")
            }
        }
    }

    private fun updateFastingTracker(date: LocalDate, isFasting: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _isError.value = false
            _isLoading.value = true
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.updateFastTracker(
                    LocalFastTracker(
                        date = date,
                        isFasting = isFasting
                    )
                )
                isFastingToday()
                _isLoading.value = false
                _isError.value = false
            } catch (e: Exception) {
                _isError.value = true
            }
        }
    }


    private fun loadLocation() {
        val isAutoLocation = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, true)
        locationService.loadLocation(isAutoLocation, onSuccess = {
            _locationName.value = it.locationName
            _latitude.value = it.latitude
            _longitude.value = it.longitude
        },
            onError = {
                _latitude.value =
                    sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498)
                _longitude.value =
                    sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
                _locationName.value = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
                _isError.value = it.isNotEmpty()
            })
    }

    private fun recreateTasbih(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _isError.value = false
                val datastore = LocalDataStore.getDataStore()
                //get all the tasbih
                val tasbihList = datastore.getAllTasbih()
                //create a unique list of tasbih by date
                val tasbihListByDate = tasbihList.groupBy { it.date }
                //recreate the tasbih for today from the yesterday tasbih
                //we need to basically copy the tasbih from yesterday to today
                //alter the date, id and count where date is today, id is 0 and count is 0
                //then insert the tasbih into the database
                //then get the tasbih list for today
                //yersterday date
                val yesterday = date.minusDays(1)
                val yesterdayTasbihList = tasbihListByDate[yesterday]
                if (yesterdayTasbihList != null) {
                    for (tasbih in yesterdayTasbihList) {
                        //check if the tasbih for today already exists by checking both arabic name and goal
                        val todayTasbihList = tasbihListByDate[date]
                        if (todayTasbihList != null) {
                            val tasbihExists = todayTasbihList.find {
                                it.arabicName == tasbih.arabicName || it.goal == tasbih.goal && it.date == date
                            }
                            if (tasbihExists != null) {
                                //if the tasbih exists for today then we don't need to recreate it
                                continue
                            }
                        }
                        val newTasbih = LocalTasbih(
                            id = 0,
                            arabicName = tasbih.arabicName,
                            englishName = tasbih.englishName,
                            translationName = tasbih.translationName,
                            count = 0,
                            date = date,
                            goal = tasbih.goal
                        )
                        datastore.saveTasbih(newTasbih)
                    }
                } else {
                    _isError.value = true
                }
                //get the tasbih list for today
                getTasbihList(date)
                _isLoading.value = false
            } catch (e: Exception) {
                _isError.value = true
            }
        }
    }

    private fun getTasbihList(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val datastore = LocalDataStore.getDataStore()
            val tasbihList = datastore.getTasbihForDate(date)
            _tasbihList.value = tasbihList
        }
    }

    data class RandomAyaState(val randomAya: LocalAya, val surah: LocalSurah, val juz: LocalJuz)

    private val _randomAyaState = MutableLiveData<RandomAyaState>()
    val randomAyaState: LiveData<RandomAyaState> = _randomAyaState

    fun getRandomAya() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                //check if there are any ayas in the database
                val ayas = dataStore.countAllAyat()
                if (ayas > 0) {
                    //get a random aya
                    val randomAya = dataStore.getRandomAya()
                    if (randomAya.ayaNumberInSurah == 0 || randomAya.ayaNumberInSurah == 1) {
                        getRandomAya()
                    }
                    val surahOfTheAya = dataStore.getSurahById(randomAya.suraNumber)
                    val juzOfTheAya = dataStore.getJuzById(randomAya.juzNumber)
                    _randomAyaState.postValue(
                        RandomAyaState(
                            randomAya = randomAya,
                            surah = surahOfTheAya,
                            juz = juzOfTheAya
                        )
                    )
                    sharedPreferences.saveDataInt(
                        AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED,
                        randomAya.ayaNumberInSurah
                    )

                } else {
                    //get a random aya
                    val randomAya = dataStore.getRandomAya()
                    if (randomAya.ayaNumberInSurah == 0 || randomAya.ayaNumberInSurah == 1) {
                        getRandomAya()
                    }
                    val surahOfTheAya = dataStore.getSurahById(randomAya.suraNumber)
                    val juzOfTheAya = dataStore.getJuzById(randomAya.juzNumber)
                    _randomAyaState.postValue(
                        RandomAyaState(
                            randomAya = randomAya,
                            surah = surahOfTheAya,
                            juz = juzOfTheAya
                        )
                    )
                    sharedPreferences.saveDataInt(
                        AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED,
                        randomAya.ayaNumberInSurah
                    )
                }
            } catch (e: Exception) {
                Log.d("getRandomAya", e.message ?: "Unknown error")
            }
        }
    }
}