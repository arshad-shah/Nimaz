package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.repositories.FastTrackerRepository
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val monthlyTrackers: List<LocalPrayersTracker> = emptyList(),
    val currentTracker: LocalPrayersTracker? = null,
    val isMenstruating: Boolean = false,
    val isFasting: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CalendarData(
    val monthlyTrackers: List<LocalPrayersTracker>,
    val currentTracker: LocalPrayersTracker?,
    val monthlyFasts: List<LocalFastTracker>,
    val isMenstruating: Boolean
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val prayerRepository: PrayerTrackerRepository,
    private val fastRepository: FastTrackerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var monthLoadJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val calendarData = _uiState
        .map { state ->
            combine(
                prayerRepository.observeTrackersForMonth(state.currentMonth),
                prayerRepository.observePrayersForDate(state.selectedDate),
                prayerRepository.observeMenstruationStatus(state.selectedDate),
                fastRepository.observeFastingForMonth(state.currentMonth)
            ) { trackers, currentTracker, isMenstruating, monthlyFasts ->
                CalendarData(
                    monthlyTrackers = trackers,
                    currentTracker = currentTracker,
                    monthlyFasts = monthlyFasts,
                    isMenstruating = isMenstruating
                )
            }
        }
        .flattenConcat()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarData(
                monthlyTrackers = emptyList(),
                currentTracker = null,
                monthlyFasts = emptyList(),
                isMenstruating = false
            )
        )

    init {
        loadCurrentMonth()
    }

    private fun loadCurrentMonth() {
        monthLoadJob?.cancel()
        monthLoadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                updateMonth(_uiState.value.currentMonth)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load calendar data: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedDate = date,
                isLoading = true
            )
            try {
                val tracker = prayerRepository.getTrackerForDate(date)
                val isFasting = fastRepository.isFastingForDate(date)
                _uiState.value = _uiState.value.copy(
                    currentTracker = tracker,
                    isFasting = isFasting,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load date data: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun updateMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentMonth = yearMonth,
                isLoading = true
            )
            try {
                // The repository will handle creating missing trackers if needed
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update month: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun updatePrayer(date: LocalDate, prayerName: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                prayerRepository.updateSpecificPrayer(date, prayerName, completed)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update prayer: ${e.message}"
                )
            }
        }
    }

    fun updateMenstruating(isMenstruating: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(
                    "Nimaz: updateMenstruating",
                    "updateMenstruating called with isMenstruating: $isMenstruating for date: ${_uiState.value.selectedDate}"
                )
                prayerRepository.updateMenstruationStatus(
                    _uiState.value.selectedDate,
                    isMenstruating
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update menstruation status: ${e.message}"
                )
            }
        }
    }

    fun updateFasting(isFasting: Boolean) {
        viewModelScope.launch {
            try {
                ViewModelLogger.d(
                    "Nimaz: updateFasting",
                    "updateFasting called with isFasting: $isFasting"
                )
                fastRepository.updateFasting(
                    _uiState.value.selectedDate,
                    isFasting
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update fasting status: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        prayerRepository.clearCache()
    }
}