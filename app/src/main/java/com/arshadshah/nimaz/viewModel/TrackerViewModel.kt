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
    //state of date
    private var _dateState = MutableStateFlow(LocalDate.now())
    val dateState = _dateState.asStateFlow()

    private var _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()

    //state for month progress
    private val _progressForMonth = MutableStateFlow<List<LocalPrayersTracker>>(emptyList())
    val progressForMonth: StateFlow<List<LocalPrayersTracker>> = _progressForMonth.asStateFlow()

    //fast progress for month
    private val _fastProgressForMonth = MutableStateFlow<List<LocalFastTracker>>(emptyList())
    val fastProgressForMonth: StateFlow<List<LocalFastTracker>> =
        _fastProgressForMonth.asStateFlow()

    private val _trackersForWeek = MutableStateFlow<List<LocalPrayersTracker>>(emptyList())
    val trackersForWeek: StateFlow<List<LocalPrayersTracker>> = _trackersForWeek.asStateFlow()

    data class PrayerTrackerState(
        val date: LocalDate = LocalDate.now(),
        val fajr: Boolean = false,
        val dhuhr: Boolean = false,
        val asr: Boolean = false,
        val maghrib: Boolean = false,
        val isha: Boolean = false,
        val progress: Int = 0,
        val isMenstruating: Boolean = false
    )

    private var _prayerTrackerState = MutableStateFlow(PrayerTrackerState())
    val prayerTrackerState = _prayerTrackerState.asStateFlow()

    //isMenstruating state
    private val _isMenstruating = MutableStateFlow(false)
    val isMenstruating = _isMenstruating.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError = _isError.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loading()
            noError()
            try {
                getTrackerForDate(_dateState.value)
                getFastTrackerForDate(_dateState.value)
                getProgressForWeek(_dateState.value)
                getProgressForMonth(_dateState.value)
                getFastProgressForMonth(YearMonth.now())
                isFastingToday(_dateState.value)
                getMenstruatingState(_dateState.value)
            } catch (e: Exception) {
                error(e.message ?: "An unknown error occurred")
            } finally {
                finishLoading()
            }
        }
    }

    private fun loading() {
        _isLoading.value = true
    }

    private fun error(message: String) {
        _isError.value = true
        _errorMessage.value = message
    }

    private fun noError() {
        _isError.value = false
        _errorMessage.value = ""
    }

    private fun finishLoading() {
        _isLoading.value = false
    }

    fun updateDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loading()
                noError()
                _dateState.value = date
                getTrackerForDate(_dateState.value)
                getFastTrackerForDate(_dateState.value)
                getProgressForWeek(_dateState.value)
                getProgressForMonth(_dateState.value)
                getFastProgressForMonth(YearMonth.now())
                isFastingToday(_dateState.value)
                getMenstruatingState(_dateState.value)
                finishLoading()
            } catch (e: Exception) {
                finishLoading()
                error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun updateMenstruatingState(menstruating: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            loading()
            noError()
            try {
                val date = _dateState.value
                val dataStore = LocalDataStore.getDataStore()
                dataStore.updateIsMenstruating(date, menstruating)
                getMenstruatingState(date)
                finishLoading()
            } catch (e: Exception) {
                finishLoading()
                error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun getMenstruatingState(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val dataStore = LocalDataStore.getDataStore()
            dataStore.getMenstruatingState(date).collect { isMenstruating ->
                _isMenstruating.value = isMenstruating
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
                getMenstruatingState(_dateState.value)
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
                getMenstruatingState(_dateState.value)
            }
        }
    }

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
                getMenstruatingState(_dateState.value)
            }
        }
    }


    fun updateFastTracker(tracker: LocalFastTracker) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val trackerExists = dataStore.fastTrackerExistsForDate(tracker.date)
                if (!trackerExists) {
                    dataStore.saveFastTracker(tracker)
                    _isFasting.value = tracker.isFasting
                    getMenstruatingState(_dateState.value)
                } else {
                    dataStore.updateFastTracker(tracker)
                    _isFasting.value = tracker.isFasting
                    getMenstruatingState(_dateState.value)
                }
            } catch (e: Exception) {
                Log.e("updateFastTracker", e.message ?: "An unknown error occurred")
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
                    getMenstruatingState(_dateState.value)
                    _isFasting.value = false
                } else {
                    val tracker = dataStore.getFastTrackerForDate(date)
                    getMenstruatingState(_dateState.value)
                    _isFasting.value = tracker.isFasting
                }
            } catch (e: Exception) {
                Log.e("getFastTrackerForDate", e.message ?: "An unknown error occurred")
            }
        }
    }

    fun updateTracker(date: LocalDate, prayerName: String, prayerDone: Boolean) {
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
                getMenstruatingState(_dateState.value)
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
                            // compare each of the values and only update if any one of them has changed
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
                            getMenstruatingState(_dateState.value)
                        }
                    }
            } catch (e: Exception) {
                Log.d("Nimaz: dashboard viewmodel", "Error getting today's prayer tracker:'")
            }
        }
    }
}