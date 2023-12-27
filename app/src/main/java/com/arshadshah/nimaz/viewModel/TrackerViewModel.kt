package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class TrackerViewModel : ViewModel() {

    sealed class TrackerState {

        object Loading : TrackerState()
        data class Tracker(val tracker: LocalPrayersTracker) : TrackerState()
        data class Error(val message: String) : TrackerState()
    }

    private var _trackerState = MutableStateFlow(TrackerState.Loading as TrackerState)
    val trackerState = _trackerState.asStateFlow()

    sealed class FastTrackerState {

        object Loading : FastTrackerState()
        data class Tracker(val tracker: LocalFastTracker) : FastTrackerState()
        data class Error(val message: String) : FastTrackerState()
    }

    private var _fastTrackerState = MutableStateFlow(FastTrackerState.Loading as FastTrackerState)
    val fastTrackerState = _fastTrackerState.asStateFlow()

    //state of date
    private var _dateState = MutableStateFlow(LocalDate.now())
    val dateState = _dateState.asStateFlow()

    //state of the date selector component
    private var _showDateSelector = MutableStateFlow(true)
    val showDateSelector = _showDateSelector.asStateFlow()

    //fajr
    private var _fajrState = MutableStateFlow(false)
    val fajrState = _fajrState.asStateFlow()

    //zuhr
    private var _zuhrState = MutableStateFlow(false)
    val zuhrState = _zuhrState.asStateFlow()

    //asr
    private var _asrState = MutableStateFlow(false)
    val asrState = _asrState.asStateFlow()

    //maghrib
    private var _maghribState = MutableStateFlow(false)
    val maghribState = _maghribState.asStateFlow()

    //isha
    private var _ishaState = MutableStateFlow(false)
    val ishaState = _ishaState.asStateFlow()


    //state to show progress of completed prayers
    private var _progressState = MutableStateFlow(0)
    val progressState = _progressState.asStateFlow()

    //dates with trackers
    private var _allTrackers = MutableStateFlow(listOf<LocalPrayersTracker>())
    val allTrackers = _allTrackers.asStateFlow()

    private var _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()

    //state for month progress
    private val _progressForMonth = MutableStateFlow<List<LocalPrayersTracker>>(emptyList())
    val progressForMonth: StateFlow<List<LocalPrayersTracker>> = _progressForMonth.asStateFlow()

    //fast progress for month
    private val _fastProgressForMonth = MutableStateFlow<List<LocalFastTracker>>(emptyList())
    val fastProgressForMonth: StateFlow<List<LocalFastTracker>> =
        _fastProgressForMonth.asStateFlow()

    //progress for monday
    private val _progressForMonday = MutableStateFlow(0)
    val progressForMonday = _progressForMonday.asStateFlow()

    //progress for tuesday
    private val _progressForTuesday = MutableStateFlow(0)
    val progressForTuesday = _progressForTuesday.asStateFlow()

    //progress for wednesday
    private val _progressForWednesday = MutableStateFlow(0)
    val progressForWednesday = _progressForWednesday.asStateFlow()

    //progress for thursday
    private val _progressForThursday = MutableStateFlow(0)
    val progressForThursday = _progressForThursday.asStateFlow()

    //progress for friday
    private val _progressForFriday = MutableStateFlow(0)
    val progressForFriday = _progressForFriday.asStateFlow()

    //progress for saturday
    private val _progressForSaturday = MutableStateFlow(0)
    val progressForSaturday = _progressForSaturday.asStateFlow()

    //progress for sunday
    private val _progressForSunday = MutableStateFlow(0)
    val progressForSunday = _progressForSunday.asStateFlow()

    //isMenstrauting state
    private val _isMenstrauting = MutableStateFlow(false)
    val isMenstrauting = _isMenstrauting.asStateFlow()


    //event for the tracker for prayer
    sealed class TrackerEvent {

        class UPDATE_TRACKER(val date: LocalDate, val prayerName: String, val prayerDone: Boolean) :
            TrackerEvent()

        class UPDATE_FAST_TRACKER(val tracker: LocalFastTracker) : TrackerEvent()
        class GET_TRACKER_FOR_DATE(val date: LocalDate) : TrackerEvent()

        class GET_FAST_TRACKER_FOR_DATE(val date: LocalDate) : TrackerEvent()

        class SAVE_TRACKER(val tracker: LocalPrayersTracker) : TrackerEvent()

        class SAVE_FAST_TRACKER(val tracker: LocalFastTracker) : TrackerEvent()

        //event for the date selector
        class SHOW_DATE_SELECTOR(val shouldShow: Boolean) : TrackerEvent()

        //set date event
        class SET_DATE(val date: LocalDate) : TrackerEvent()

        //progress event
        class SET_PROGRESS(val progress: Int) : TrackerEvent()

        //update Chart Data
        object GET_ALL_TRACKERS : TrackerEvent()

        //get progress for each day of the current week
        class GET_PROGRESS_FOR_WEEK(val date: LocalDate) : TrackerEvent()

        class GET_PROGRESS_FOR_MONTH(val date: LocalDate) : TrackerEvent()

        //progress of fast fro month
        class GET_FAST_PROGRESS_FOR_MONTH(val date: YearMonth) : TrackerEvent()

        //update menstrauting state
        class UPDATE_MENSTRAUTING_STATE(val isMenstrauting: Boolean) : TrackerEvent()

        class IsFastingToday(val date: LocalDate) : TrackerEvent()
    }

    fun onEvent(event: TrackerEvent) {
        when (event) {
            is TrackerEvent.UPDATE_TRACKER -> updateTracker(
                event.date,
                event.prayerName,
                event.prayerDone
            )

            is TrackerEvent.GET_TRACKER_FOR_DATE -> getTrackerForDate(event.date)
            is TrackerEvent.SAVE_TRACKER -> saveTracker(event.tracker)
            is TrackerEvent.SHOW_DATE_SELECTOR -> _showDateSelector.value = event.shouldShow
            is TrackerEvent.SET_DATE -> updateDate(event.date)
            is TrackerEvent.SET_PROGRESS -> _progressState.value = event.progress
            is TrackerEvent.GET_ALL_TRACKERS -> getAllTrackers()
            is TrackerEvent.UPDATE_FAST_TRACKER -> updateFastTracker(event.tracker)
            is TrackerEvent.GET_FAST_TRACKER_FOR_DATE -> getFastTrackerForDate(event.date)
            is TrackerEvent.SAVE_FAST_TRACKER -> saveFastTracker(event.tracker)
            is TrackerEvent.GET_PROGRESS_FOR_WEEK -> getProgressForWeek(event.date)
            is TrackerEvent.GET_PROGRESS_FOR_MONTH -> getProgressForMonth(event.date)
            is TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH -> getFastProgressForMonth(event.date)

            is TrackerEvent.UPDATE_MENSTRAUTING_STATE -> updateMenstrautingState(
                event.isMenstrauting
            )

            is TrackerEvent.IsFastingToday -> isFastingToday(event.date)

        }
    }

    private fun updateDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            _dateState.value = date
        }
    }


    private fun updateMenstrautingState(menstrauting: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val dataStore = LocalDataStore.getDataStore()
            dataStore.updateIsMenstruating(_dateState.value, menstrauting)
            getMenstruatingState(_dateState.value)
        }
    }

    private fun getMenstruatingState(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val dataStore = LocalDataStore.getDataStore()
            dataStore.getMenstruatingState(date).collect { isMenstruating ->
                _isMenstrauting.value = isMenstruating
            }
        }
    }

    private fun getFastProgressForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            val dataStore = LocalDataStore.getDataStore()
            dataStore.getFastTrackersForMonth(
                firstDay = yearMonth.atDay(1),
                lastDay = yearMonth.atEndOfMonth()
            ).collect { trackers ->
                _fastProgressForMonth.value = trackers
            }
        }
    }

    private fun getProgressForMonth(date: LocalDate) {
        val dataStore = LocalDataStore.getDataStore()
        val firstDayOfMonth = date.withDayOfMonth(1)
        val lastDayOfMonth =
            date.withDayOfMonth(date.lengthOfMonth())

        viewModelScope.launch {
            dataStore.getTrackersForMonth(firstDayOfMonth, lastDayOfMonth).collect { trackers ->
                _progressForMonth.value = trackers
            }
        }
    }

    private val _trackersForWeek = MutableStateFlow<List<LocalPrayersTracker>>(emptyList())
    val trackersForWeek: StateFlow<List<LocalPrayersTracker>> = _trackersForWeek.asStateFlow()


    private fun getProgressForWeek(date: LocalDate) {
        val dataStore = LocalDataStore.getDataStore()
        viewModelScope.launch {
            val startDate = date.with(DayOfWeek.MONDAY)
            val endDate = date.with(DayOfWeek.SUNDAY)

            dataStore.getTrackersForWeek(startDate, endDate).collect { trackers ->
                // Check for missing dates and fill them in
                val completeWeek = (0L..ChronoUnit.DAYS.between(
                    startDate,
                    endDate
                )).map { days ->
                    startDate.plusDays(days)
                }.map { date ->
                    trackers.find { it.date == date } ?: LocalPrayersTracker(date = date)
                }
                _trackersForWeek.value = completeWeek
            }
        }
    }


    private fun updateFastTracker(tracker: LocalFastTracker) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val trackerExists = dataStore.fastTrackerExistsForDate(tracker.date)
                if (!trackerExists) {
                    dataStore.saveFastTracker(tracker)
                    _isFasting.value = tracker.isFasting
                    _fastTrackerState.value = FastTrackerState.Tracker(tracker)
                } else {
                    dataStore.updateFastTracker(tracker)
                    _isFasting.value = tracker.isFasting
                    _fastTrackerState.value = FastTrackerState.Tracker(tracker)
                }
            } catch (e: Exception) {
                _fastTrackerState.value =
                    FastTrackerState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun isFastingToday(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.isFastingForDate(date)
                    .catch { emit(false) }
                    .collect {
                        _isFasting.value = it
                    }
            } catch (e: Exception) {
                _isFasting.value = false
            }
        }
    }

    private fun getFastTrackerForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val trackerExists = dataStore.fastTrackerExistsForDate(date)
                if (!trackerExists) {
                    val tracker = LocalFastTracker(date)
                    dataStore.saveFastTracker(tracker)
                    _fastTrackerState.value = FastTrackerState.Tracker(tracker)
                    _isFasting.value = false
                } else {
                    val tracker = dataStore.getFastTrackerForDate(date)
                    _fastTrackerState.value = FastTrackerState.Tracker(tracker)
                    _isFasting.value = tracker.isFasting
                }
            } catch (e: Exception) {
                _fastTrackerState.value =
                    FastTrackerState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun saveFastTracker(tracker: LocalFastTracker) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.saveFastTracker(tracker)
                _fastTrackerState.value = FastTrackerState.Tracker(tracker)
            } catch (e: Exception) {
                _fastTrackerState.value =
                    FastTrackerState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun getAllTrackers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _allTrackers.value = PrayerTrackerRepository.getAllTrackers()
            } catch (e: Exception) {
                _trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    data class PrayerTrackerState(
        val date: LocalDate,
        val fajr: Boolean,
        val dhuhr: Boolean,
        val asr: Boolean,
        val maghrib: Boolean,
        val isha: Boolean,
        val progress: Int,
        val isMenstruating: Boolean
    )

    private var _prayerTrackerState =
        MutableStateFlow(
            PrayerTrackerState(
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
    val prayerTrackerState = _prayerTrackerState.asStateFlow()

    private fun updateTracker(date: LocalDate, prayerName: String, prayerDone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedTracker =
                    PrayerTrackerRepository.updateSpecificPrayer(date, prayerName, prayerDone)
                _prayerTrackerState.update {
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
            } catch (e: Exception) {
                Log.d("updateTracker", e.message ?: "Unknown error")
            }
        }
    }

    private fun getTrackerForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                PrayerTrackerRepository.getPrayersForDate(date)
                    .catch { emit(LocalPrayersTracker()) }
                    .collect { prayerTrackerFromStorage ->
                        if (prayerTrackerFromStorage.date == _dateState.value) {
                            // compare each of the values and onyl update if any one of them has changed
                            _prayerTrackerState.update {
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
                    }
            } catch (e: Exception) {
                Log.d("Nimaz: dashboard viewmodel", "Error getting today's prayer tracker:'")
            }
        }
    }

    //function to save a tracker
    private fun saveTracker(tracker: LocalPrayersTracker) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedTracker = PrayerTrackerRepository.saveTrackerForDate(tracker)
                _dateState.value = tracker.date
                _fajrState.value = tracker.fajr
                _zuhrState.value = tracker.dhuhr
                _asrState.value = tracker.asr
                _maghribState.value = tracker.maghrib
                _ishaState.value = tracker.isha
                _trackerState.value = TrackerState.Tracker(updatedTracker)
                _progressState.value = tracker.progress
                _isMenstrauting.value = tracker.isMenstruating
            } catch (e: Exception) {
                _trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}