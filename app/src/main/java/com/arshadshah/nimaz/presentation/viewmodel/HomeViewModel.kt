package com.arshadshah.nimaz.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.data.local.dua.DuaContentSeeder
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
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
    val dailyHadithReference: String? = null,
    val dailyHadithId: String? = null,
    val dailyHadithGrade: String? = null,
    // 0f→1f position of "now" along the Fajr→Isha timeline (drives the progress
    // card's fill); recomputed each tick so it advances with the clock.
    val prayerTimelineProgress: Float = 0f,
    val dailyDua: DailyDua? = null,
    val isFriday: Boolean = false,
    val jumuahTime: String = "",
    val timeUntilJumuah: String = "",
    val isJumuahPassed: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Permission states
    val hasNotificationPermission: Boolean = true,
    val hasLocationPermission: Boolean = true,
    val isBatteryOptimized: Boolean = false
)

/**
 * A single dua surfaced on the home screen's "Today" section, picked to match
 * the current time of day (morning / evening / before sleep adhkar).
 */
data class DailyDua(
    val title: String,
    val arabic: String,
    val translation: String,
    val source: String,
    val categoryLabel: String,
    val categoryIcon: String,
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
    data class UpdateLocation(val latitude: Double, val longitude: Double, val name: String) :
        HomeEvent

    data object RefreshPrayerTimes : HomeEvent
    data object RefreshPermissions : HomeEvent
    data class TogglePrayerStatus(val prayerType: PrayerType) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val prayerRepository: PrayerRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val fastingDao: FastingDao,
    private val hadithDao: HadithDao,
    private val duaDao: DuaDao,
    private val duaContentSeeder: DuaContentSeeder
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // Declared before init{} so it is initialized before loadPrayerRecords()
    // collects into it. The getTodayPrayerRecords() Flow can emit synchronously
    // on an unconfined dispatch during construction; if this field were declared
    // after the init block it would still be null at that point, causing an NPE.
    private val _prayerRecords = MutableStateFlow<Map<PrayerName, PrayerStatus>>(emptyMap())

    init {
        checkPermissions()
        observeLocation()
        observeTimeFormat()
        loadPrayerRecords()
        observeFastingStatus()
        loadDailyHadith()
        loadDailyDua()
        startTimeUpdates()
    }

    private fun loadPrayerRecords() {
        viewModelScope.launch {
            prayerRepository.getTodayPrayerRecords().collect { records ->
                _prayerRecords.update { records }
            }
        }
    }

    private fun observeFastingStatus() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val startOfDay = today.toEpochDay() * 86400000L
            val endOfDay = startOfDay + 86400000L - 1

            fastingDao.getFastRecordsInRange(startOfDay, endOfDay).collect { records ->
                val todayRecord = records.firstOrNull()
                _state.update { it.copy(fastingToday = todayRecord?.status == "fasted") }
            }
        }
    }

    private fun loadDailyHadith() {
        viewModelScope.launch {
            try {
                val totalHadiths = hadithDao.getHadithCount()
                if (totalHadiths == 0) return@launch

                // Spread the daily selection across the whole collection. The
                // previous approach incremented the offset by one per day, so
                // consecutive days landed on adjacent (near-identical) hadiths
                // and barely appeared to change. Multiplying the day index by a
                // large odd constant (Knuth's multiplicative hash) scatters
                // each day to a very different part of the database while still
                // being deterministic: the same day always yields the same
                // hadith.
                val daysSinceEpoch = LocalDate.now().toEpochDay()
                val offset =
                    Math.floorMod(daysSinceEpoch * 2654435761L, totalHadiths.toLong()).toInt()

                val hadith = hadithDao.getHadithByOffset(offset)
                _state.update {
                    it.copy(
                        dailyHadith = hadith?.textEnglish?.let { text ->
                            if (text.length > 150) text.take(150).trimEnd() + "…" else text
                        },
                        dailyHadithReference = hadith?.reference?.takeIf { ref -> ref.isNotBlank() },
                        // Carry the id so tapping the card opens this exact hadith
                        // in the reader, and a short grade label for the card chip.
                        dailyHadithId = hadith?.id?.toString(),
                        dailyHadithGrade = shortGradeLabel(hadith?.grade)
                    )
                }
            } catch (_: Exception) {
                // No hadith data available
            }
        }
    }

    /**
     * Short, chip-friendly grade label for the home hadith card (e.g. "Sahih").
     * Returns null for unknown/blank grades so the card simply omits the chip.
     */
    private fun shortGradeLabel(rawGrade: String?): String? =
        when (HadithGrade.fromString(rawGrade)) {
            HadithGrade.SAHIH -> context.getString(R.string.grade_sahih)
            HadithGrade.HASAN -> context.getString(R.string.grade_hasan)
            HadithGrade.DAIF -> context.getString(R.string.grade_daif)
            HadithGrade.MAWDU -> context.getString(R.string.grade_mawdu)
            else -> null
        }

    /**
     * Loads a dua matching the current time of day (morning / evening / before
     * sleep adhkar) and rotates the specific dua daily within that category.
     */
    private fun loadDailyDua() {
        viewModelScope.launch {
            try {
                // Ensure newly shipped duas are seeded before reading directly
                // from the DAO; on an app update the prepopulated DB is not
                // re-copied, so the seeder is what brings in the new content.
                duaContentSeeder.seedIfNeeded()
                val categoryId = duaCategoryForHour(LocalTime.now().hour)
                val category = duaDao.getCategoryById(categoryId) ?: return@launch
                val duas = duaDao.getDuasByCategoryOnce(categoryId)
                if (duas.isEmpty()) return@launch

                val index = (LocalDate.now().dayOfYear % duas.size).coerceIn(0, duas.size - 1)
                val dua = duas[index]
                _state.update {
                    it.copy(
                        dailyDua = DailyDua(
                            title = dua.titleEnglish,
                            arabic = dua.textArabic,
                            translation = dua.translation,
                            source = dua.source,
                            categoryLabel = category.nameEnglish,
                            categoryIcon = category.icon
                        )
                    )
                }
            } catch (_: Exception) {
                // No dua data available
            }
        }
    }

    /**
     * Maps the hour of day to a dua category id (matching the prepopulated
     * `dua_categories` table): morning adhkar through the day, evening adhkar
     * in the late afternoon, and before-sleep adhkar at night.
     */
    private fun duaCategoryForHour(hour: Int): Int = when (hour) {
        in 4..15 -> DUA_CATEGORY_MORNING
        in 16..20 -> DUA_CATEGORY_EVENING
        else -> DUA_CATEGORY_BEFORE_SLEEP
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UpdateLocation -> updateLocation(
                event.latitude,
                event.longitude,
                event.name
            )

            HomeEvent.RefreshPrayerTimes -> calculatePrayerTimes()
            HomeEvent.RefreshPermissions -> checkPermissions()
            is HomeEvent.TogglePrayerStatus -> togglePrayerStatus(event.prayerType)
        }
    }

    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    private fun checkPermissions() {
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)

        _state.update {
            it.copy(
                hasNotificationPermission = hasNotification,
                hasLocationPermission = hasLocation,
                isBatteryOptimized = isBatteryOptimized
            )
        }
    }

    private fun togglePrayerStatus(prayerType: PrayerType) {
        // Sunrise is not a prayer - don't allow toggling
        if (prayerType == PrayerType.SUNRISE) return

        viewModelScope.launch {
            val prayerName = PrayerName.valueOf(prayerType.name)
            val todayEpoch = LocalDate.now().toEpochDay() * 86400000L
            val currentStatus = _prayerRecords.value[prayerName] ?: PrayerStatus.NOT_PRAYED
            val newStatus =
                if (currentStatus == PrayerStatus.PRAYED) PrayerStatus.NOT_PRAYED else PrayerStatus.PRAYED
            val prayedAt =
                if (newStatus == PrayerStatus.PRAYED) System.currentTimeMillis() else null

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

            // Notify widget to refresh via WorkManager
            PrayerTrackerWorker.enqueueImmediateWork(context)
        }
    }

    // Cached prayer calculation settings
    private var cachedCalcMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE
    private var cachedAsrCalc = AsrCalculation.STANDARD
    private var cachedHighLatRule: HighLatitudeRule? = null
    private var cachedAdjustments = mapOf<PrayerType, Int>()

    private fun observeLocation() {
        viewModelScope.launch {
            // Combine location with all prayer calculation settings
            val locationFlow = combine(
                preferencesDataStore.latitude,
                preferencesDataStore.longitude,
                preferencesDataStore.locationName
            ) { lat: Double, lng: Double, name: String ->
                Triple(lat, lng, name)
            }

            val calcSettingsFlow = combine(
                preferencesDataStore.calculationMethod,
                preferencesDataStore.asrCalculation,
                preferencesDataStore.highLatitudeRule
            ) { calc: String, asr: String, high: String ->
                Triple(calc, asr, high)
            }

            val adjustmentsFlow = combine(
                preferencesDataStore.fajrAdjustment,
                preferencesDataStore.sunriseAdjustment,
                preferencesDataStore.dhuhrAdjustment,
                preferencesDataStore.asrAdjustment,
            ) { fajr, sunrise, dhuhr, asr ->
                mapOf(
                    PrayerType.FAJR to fajr,
                    PrayerType.SUNRISE to sunrise,
                    PrayerType.DHUHR to dhuhr,
                    PrayerType.ASR to asr
                )
            }.combine(
                combine(
                    preferencesDataStore.maghribAdjustment,
                    preferencesDataStore.ishaAdjustment
                ) { maghrib, isha ->
                    mapOf(
                        PrayerType.MAGHRIB to maghrib,
                        PrayerType.ISHA to isha
                    )
                }
            ) { first, second -> first + second }

            combine(
                locationFlow,
                calcSettingsFlow,
                adjustmentsFlow
            ) { location, calcSettings, adjustments ->
                Triple(location, calcSettings, adjustments)
            }.collect { (location, calcSettings, adjustments) ->
                val (lat, lng, name) = location
                val (calcStr, asrStr, highStr) = calcSettings

                val hasLocation = lat != 0.0 && lng != 0.0
                val latitude = if (hasLocation) lat else DEFAULT_LATITUDE
                val longitude = if (hasLocation) lng else DEFAULT_LONGITUDE
                val locationName =
                    if (hasLocation && name.isNotBlank()) name else DEFAULT_LOCATION_NAME

                // Cache calculation settings
                cachedCalcMethod = try {
                    CalculationMethod.valueOf(calcStr)
                } catch (_: Exception) {
                    CalculationMethod.MUSLIM_WORLD_LEAGUE
                }
                cachedAsrCalc = when (asrStr.lowercase()) {
                    "hanafi" -> AsrCalculation.HANAFI
                    else -> AsrCalculation.STANDARD
                }
                cachedHighLatRule = try {
                    HighLatitudeRule.valueOf(highStr)
                } catch (_: Exception) {
                    null
                }
                cachedAdjustments = adjustments

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

        // Dua category ids from the prepopulated `dua_categories` table.
        private const val DUA_CATEGORY_MORNING = 1       // Morning Adhkar
        private const val DUA_CATEGORY_EVENING = 2       // Evening Adhkar
        private const val DUA_CATEGORY_BEFORE_SLEEP = 5  // Before Sleep
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

                val prayerTimes = prayerTimeCalculator.getPrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    calculationMethod = cachedCalcMethod,
                    asrCalculation = cachedAsrCalc,
                    highLatitudeRule = cachedHighLatRule,
                    adjustments = cachedAdjustments
                )
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
                val currentPrayerIndex =
                    if (nextPrayerIndex > 0) nextPrayerIndex - 1 else sortedPrayers.lastIndex

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
                    val tomorrowDate = LocalDate.now().plusDays(1)
                    val tomorrowPrayers = prayerTimeCalculator.getPrayerTimes(
                        latitude = latitude,
                        longitude = longitude,
                        date = tomorrowDate,
                        calculationMethod = cachedCalcMethod,
                        asrCalculation = cachedAsrCalc,
                        highLatitudeRule = cachedHighLatRule,
                        adjustments = cachedAdjustments
                    )
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

                // Where "now" sits along the Fajr→Isha timeline (0f at Fajr, 1f
                // at Isha), interpolated within the current interval. Drives the
                // progress card's fill independently of which prayers are prayed.
                val timelineProgress: Float = run {
                    val order = listOf(
                        PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR,
                        PrayerType.MAGHRIB, PrayerType.ISHA
                    )
                    val ts = order.mapNotNull { type ->
                        prayerTimes.find { it.type == type }?.time
                    }
                    when {
                        ts.size < 2 -> 0f
                        currentTime <= ts.first() -> 0f
                        currentTime >= ts.last() -> 1f
                        else -> {
                            var result = 1f
                            for (k in 0 until ts.size - 1) {
                                if (currentTime >= ts[k] && currentTime < ts[k + 1]) {
                                    val frac = (currentTime - ts[k]).inWholeSeconds.toFloat() /
                                            (ts[k + 1] - ts[k]).inWholeSeconds.toFloat()
                                    result = ((k + frac) / (ts.size - 1)).coerceIn(0f, 1f)
                                    break
                                }
                            }
                            result
                        }
                    }
                }

                // Apply prayer records to displays
                val records = _prayerRecords.value
                val displaysWithStatus = updatedDisplays.map { display ->
                    val prayerName = PrayerName.valueOf(display.type.name)
                    val status = records[prayerName] ?: PrayerStatus.NOT_PRAYED
                    display.copy(prayerStatus = status)
                }

                // Friday / Jumu'ah detection
                val today = LocalDate.now()
                val isFriday = today.dayOfWeek == DayOfWeek.FRIDAY
                val dhuhrDisplay = displaysWithStatus.find { it.type == PrayerType.DHUHR }
                val dhuhrInstant = prayerTimes.find { it.type == PrayerType.DHUHR }?.time

                val jumuahTime = if (isFriday) dhuhrDisplay?.time ?: "" else ""
                val isJumuahPassed = if (isFriday) dhuhrDisplay?.isPassed == true else false
                val timeUntilJumuah = if (isFriday && !isJumuahPassed && dhuhrInstant != null) {
                    val diff: Duration = dhuhrInstant - currentTime
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

                _state.update {
                    it.copy(
                        prayerTimes = displaysWithStatus,
                        prayerTimelineProgress = timelineProgress,
                        currentPrayer = if (currentPrayerIndex >= 0) sortedPrayers[currentPrayerIndex].type else null,
                        nextPrayer = nextPrayer,
                        timeUntilNextPrayer = timeUntilNext,
                        hijriDate = calculateHijriDate(),
                        isFriday = isFriday,
                        jumuahTime = jumuahTime,
                        timeUntilJumuah = timeUntilJumuah,
                        isJumuahPassed = isJumuahPassed,
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

    private var use24HourFormat: Boolean = false

    private fun observeTimeFormat() {
        viewModelScope.launch {
            preferencesDataStore.use24HourFormat.collect { enabled ->
                use24HourFormat = enabled
                calculatePrayerTimes() // Recalculate to reformat times
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return if (use24HourFormat) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            val amPm = if (hour >= 12) "PM" else "AM"
            String.format("%d:%02d %s", h, minute, amPm)
        }
    }

    private fun calculateHijriDate(): String {
        val hijriDate = HijriDateCalculator.today()
        return hijriDate.formatted()
    }
}
