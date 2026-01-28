package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration

data class HomeUiState(
    val currentDate: LocalDate = LocalDate.now(),
    val hijriDate: String = "",
    val prayerTimes: List<PrayerTimeDisplay> = emptyList(),
    val currentPrayer: PrayerType? = null,
    val nextPrayer: PrayerType? = null,
    val timeUntilNextPrayer: String = "",
    val locationName: String = "Location not set",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val fastingToday: Boolean = false,
    val dailyHadith: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PrayerTimeDisplay(
    val type: PrayerType,
    val name: String,
    val time: String,
    val isPassed: Boolean,
    val isCurrent: Boolean,
    val isNext: Boolean,
    val prayerStatus: PrayerStatus = PrayerStatus.NOT_PRAYED
)

sealed interface HomeEvent {
    data class UpdateLocation(val latitude: Double, val longitude: Double, val name: String) : HomeEvent
    data object RefreshPrayerTimes : HomeEvent
    data class TogglePrayerStatus(val prayerType: PrayerType) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val prayerRepository: PrayerRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val fastingDao: FastingDao,
    private val hadithDao: HadithDao
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        observeLocation()
        loadPrayerRecords()
        loadFastingStatus()
        loadDailyHadith()
        startTimeUpdates()
    }

    private fun loadPrayerRecords() {
        viewModelScope.launch {
            val todayEpoch = LocalDate.now().toEpochDay() * 86400000L
            prayerRepository.getPrayerRecordsForDate(todayEpoch).collect { records ->
                val recordMap = records.associate { it.prayerName to it.status }
                _prayerRecords.update { recordMap }
            }
        }
    }

    private fun loadFastingStatus() {
        viewModelScope.launch {
            try {
                val todayEpoch = LocalDate.now().toEpochDay() * 86400000L
                val record = fastingDao.getFastRecordForDate(todayEpoch)
                _state.update { it.copy(fastingToday = record?.status == "fasted") }
            } catch (_: Exception) {
                // No fasting data available
            }
        }
    }

    private fun loadDailyHadith() {
        viewModelScope.launch {
            try {
                val dayOfYear = java.time.LocalDate.now().dayOfYear
                val hadith = hadithDao.getHadithByNumber(1, dayOfYear % 50 + 1)
                _state.update {
                    it.copy(
                        dailyHadith = hadith?.textEnglish?.take(120)?.let { text ->
                            if (text.length >= 120) "$text..." else text
                        }
                    )
                }
            } catch (_: Exception) {
                // No hadith data available
            }
        }
    }

    private val _prayerRecords = MutableStateFlow<Map<PrayerName, PrayerStatus>>(emptyMap())

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UpdateLocation -> updateLocation(event.latitude, event.longitude, event.name)
            HomeEvent.RefreshPrayerTimes -> calculatePrayerTimes()
            is HomeEvent.TogglePrayerStatus -> togglePrayerStatus(event.prayerType)
        }
    }

    private fun togglePrayerStatus(prayerType: PrayerType) {
        viewModelScope.launch {
            val prayerName = PrayerName.valueOf(prayerType.name)
            val todayEpoch = LocalDate.now().toEpochDay() * 86400000L
            val currentStatus = _prayerRecords.value[prayerName] ?: PrayerStatus.NOT_PRAYED
            val newStatus = if (currentStatus == PrayerStatus.PRAYED) PrayerStatus.NOT_PRAYED else PrayerStatus.PRAYED
            val prayedAt = if (newStatus == PrayerStatus.PRAYED) System.currentTimeMillis() else null

            prayerRepository.updatePrayerStatus(todayEpoch, prayerName, newStatus, prayedAt, false)
            _prayerRecords.update { it + (prayerName to newStatus) }

            // Update displays with new status
            _state.update { state ->
                state.copy(
                    prayerTimes = state.prayerTimes.map { display ->
                        val name = PrayerName.valueOf(display.type.name)
                        val status = _prayerRecords.value[name] ?: PrayerStatus.NOT_PRAYED
                        display.copy(prayerStatus = status)
                    }
                )
            }
        }
    }

    private fun observeLocation() {
        viewModelScope.launch {
            combine(
                preferencesDataStore.latitude,
                preferencesDataStore.longitude,
                preferencesDataStore.locationName
            ) { lat: Double, lng: Double, name: String ->
                Triple(lat, lng, name)
            }.combine(
                combine(
                    preferencesDataStore.calculationMethod,
                    preferencesDataStore.asrCalculation,
                    preferencesDataStore.highLatitudeRule
                ) { calc: String, asr: String, high: String ->
                    Triple(calc, asr, high)
                }
            ) { location, _ ->
                location
            }.collect { (lat, lng, name) ->
                val hasLocation = lat != 0.0 && lng != 0.0
                val latitude = if (hasLocation) lat else DEFAULT_LATITUDE
                val longitude = if (hasLocation) lng else DEFAULT_LONGITUDE
                val locationName = if (hasLocation && name.isNotBlank()) name else DEFAULT_LOCATION_NAME

                _state.update {
                    it.copy(
                        latitude = latitude,
                        longitude = longitude,
                        locationName = locationName
                    )
                }
                calculatePrayerTimes()
            }
        }
    }

    companion object {
        // Default location: Dublin, Ireland (as shown in prototype)
        private const val DEFAULT_LATITUDE = 53.3498
        private const val DEFAULT_LONGITUDE = -6.2603
        private const val DEFAULT_LOCATION_NAME = "Dublin, Ireland"
    }

    private fun updateLocation(latitude: Double, longitude: Double, name: String) {
        viewModelScope.launch {
            preferencesDataStore.updateLocation(latitude, longitude, name)
            _state.update {
                it.copy(
                    latitude = latitude,
                    longitude = longitude,
                    locationName = name
                )
            }
            calculatePrayerTimes()
        }
    }

    private fun calculatePrayerTimes() {
        val latitude = _state.value.latitude.takeIf { it != 0.0 } ?: DEFAULT_LATITUDE
        val longitude = _state.value.longitude.takeIf { it != 0.0 } ?: DEFAULT_LONGITUDE

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                val prayerTimes = prayerTimeCalculator.getPrayerTimes(latitude, longitude)
                val currentTime = Clock.System.now()
                val timeZone = TimeZone.currentSystemDefault()
                val localTime = currentTime.toLocalDateTime(timeZone)

                val prayerTimeDisplays = prayerTimes.map { prayerTime ->
                    val prayerLocalTime = prayerTime.time.toLocalDateTime(timeZone)
                    val isPassed = prayerLocalTime.time < localTime.time

                    PrayerTimeDisplay(
                        type = prayerTime.type,
                        name = prayerTime.type.displayName,
                        time = formatTime(prayerLocalTime.hour, prayerLocalTime.minute),
                        isPassed = isPassed,
                        isCurrent = false,
                        isNext = false
                    )
                }

                // Find current and next prayer
                val sortedPrayers = prayerTimeDisplays.sortedBy {
                    prayerTimes.find { pt -> pt.type == it.type }?.time
                }

                val nextPrayerIndex = sortedPrayers.indexOfFirst { !it.isPassed }
                val currentPrayerIndex = if (nextPrayerIndex > 0) nextPrayerIndex - 1 else sortedPrayers.lastIndex

                val updatedDisplays = sortedPrayers.mapIndexed { index, display ->
                    display.copy(
                        isCurrent = index == currentPrayerIndex,
                        isNext = index == nextPrayerIndex
                    )
                }

                val nextPrayer: PrayerType?
                val timeUntilNext: String

                if (nextPrayerIndex >= 0) {
                    // There's a future prayer today
                    nextPrayer = sortedPrayers[nextPrayerIndex].type
                    val nextPrayerTime = prayerTimes.find { it.type == nextPrayer }?.time
                    timeUntilNext = if (nextPrayerTime != null) {
                        val diff: Duration = nextPrayerTime - currentTime
                        val totalSeconds = diff.inWholeSeconds
                        val hours = totalSeconds / 3600
                        val minutes = (totalSeconds % 3600) / 60
                        val seconds = totalSeconds % 60
                        when {
                            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                            minutes > 0 -> "${minutes}m ${seconds}s"
                            else -> "${seconds}s"
                        }
                    } else ""
                } else {
                    // All prayers passed — wrap to tomorrow's Fajr
                    nextPrayer = PrayerType.FAJR
                    val tomorrowDate = java.time.LocalDate.now().plusDays(1)
                    val tomorrowPrayers = prayerTimeCalculator.getPrayerTimes(latitude, longitude, tomorrowDate)
                    val tomorrowFajr = tomorrowPrayers.find { it.type == PrayerType.FAJR }?.time
                    timeUntilNext = if (tomorrowFajr != null) {
                        val diff: Duration = tomorrowFajr - currentTime
                        val totalSeconds = diff.inWholeSeconds
                        val hours = totalSeconds / 3600
                        val minutes = (totalSeconds % 3600) / 60
                        val seconds = totalSeconds % 60
                        when {
                            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                            minutes > 0 -> "${minutes}m ${seconds}s"
                            else -> "${seconds}s"
                        }
                    } else ""
                }

                // Apply prayer records to displays
                val records = _prayerRecords.value
                val displaysWithStatus = updatedDisplays.map { display ->
                    val prayerName = PrayerName.valueOf(display.type.name)
                    val status = records[prayerName] ?: PrayerStatus.NOT_PRAYED
                    display.copy(prayerStatus = status)
                }

                _state.update {
                    it.copy(
                        prayerTimes = displaysWithStatus,
                        currentPrayer = if (currentPrayerIndex >= 0) sortedPrayers[currentPrayerIndex].type else null,
                        nextPrayer = nextPrayer,
                        timeUntilNextPrayer = timeUntilNext,
                        hijriDate = calculateHijriDate(),
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000) // Update every second for smooth countdown
                calculatePrayerTimes()
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format("%d:%02d %s", h, minute, amPm)
    }

    private fun calculateHijriDate(): String {
        val hijriDate = HijriDateCalculator.today()
        return hijriDate.formatted()
    }
}
