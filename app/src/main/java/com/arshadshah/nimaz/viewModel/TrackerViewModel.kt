package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.repositories.FastTrackerRepository
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class PrayerTrakerForWeek(
    val date: LocalDate,
    val progress: Int,
    val isMenstruating: Boolean
)

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val prayerTrackerRepository: PrayerTrackerRepository,
    private val fastTrackerRepository: FastTrackerRepository
) : ViewModel() {
    private var _dateState = MutableStateFlow(LocalDate.now())
    val dateState = _dateState.asStateFlow()

    private var _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()

    private val _progressForMonth = MutableStateFlow<List<LocalPrayersTracker>>(emptyList())
    val progressForMonth: StateFlow<List<LocalPrayersTracker>> = _progressForMonth.asStateFlow()

    private val _fastProgressForMonth = MutableStateFlow<List<LocalFastTracker>>(emptyList())
    val fastProgressForMonth: StateFlow<List<LocalFastTracker>> =
        _fastProgressForMonth.asStateFlow()

    private val _trackersForWeek = MutableStateFlow<List<PrayerTrakerForWeek>>(emptyList())
    val trackersForWeek: StateFlow<List<PrayerTrakerForWeek>> = _trackersForWeek.asStateFlow()

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
            } catch (e: Exception) {
                error(e.message ?: "An unknown error occurred")
            } finally {
                finishLoading()
            }
        }
    }

    fun updateMenstruatingState(menstruating: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            loading()
            noError()
            try {
                val date = _dateState.value
                prayerTrackerRepository.updateMenstruationStatus(date, menstruating)
                getMenstruatingState(date)
            } catch (e: Exception) {
                error(e.message ?: "An unknown error occurred")
            } finally {
                finishLoading()
            }
        }
    }

    private fun getMenstruatingState(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            prayerTrackerRepository.observeMenstruationStatus(date)
                .collect { isMenstruating ->
                    _isMenstruating.value = isMenstruating
                }
        }
    }

    private fun getFastProgressForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            fastTrackerRepository.observeFastingForMonth(yearMonth)
                .collect { trackers ->
                    _fastProgressForMonth.value = trackers
                    getMenstruatingState(_dateState.value)
                }
        }
    }

    private fun getProgressForMonth(date: LocalDate) {
        val yearMonth = YearMonth.from(date)
        viewModelScope.launch {
            prayerTrackerRepository.observeTrackersForMonth(yearMonth)
                .collect { trackers ->
                    _progressForMonth.value = trackers
                    getMenstruatingState(_dateState.value)
                }
        }
    }

    private fun getProgressForWeek(date: LocalDate) {
        viewModelScope.launch {
            prayerTrackerRepository.observeTrackersForWeek(date)
                .collect { weekTrackers ->
                    _trackersForWeek.value = weekTrackers.map {
                        PrayerTrakerForWeek(
                            it.date,
                            it.progress,
                            it.isMenstruating
                        )
                    }
                    getMenstruatingState(_dateState.value)
                }
        }
    }

    fun updateFastTracker(date: LocalDate, isFasting: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fastTrackerRepository.updateFasting(date, isFasting)
                _isFasting.value = isFasting
                getMenstruatingState(_dateState.value)
            } catch (e: Exception) {
                Log.e("updateFastTracker", e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun isFastingToday(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fastTrackerRepository.observeFastingForDate(date)
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
                val tracker = fastTrackerRepository.getTrackerForDate(date)
                _isFasting.value = tracker.isFasting
                getMenstruatingState(_dateState.value)
            } catch (e: Exception) {
                Log.e("getFastTrackerForDate", e.message ?: "An unknown error occurred")
            }
        }
    }

    fun updateTracker(date: LocalDate, prayerName: String, prayerDone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                prayerTrackerRepository.updateSpecificPrayer(date, prayerName, prayerDone)
                getTrackerForDate(date)
                getMenstruatingState(_dateState.value)
            } catch (e: Exception) {
                Log.d("updateTracker", e.message ?: "Unknown error")
            }
        }
    }

    private fun getTrackerForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                prayerTrackerRepository.observePrayersForDate(date)
                    .catch { emit(LocalPrayersTracker()) }
                    .collect { prayerTrackerFromStorage ->
                        if (prayerTrackerFromStorage.date == _dateState.value) {
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
                Log.d("Nimaz: dashboard viewmodel", "Error getting today's prayer tracker")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        prayerTrackerRepository.clearCache()
        fastTrackerRepository.clearCache()
    }
}