package com.arshadshah.nimaz.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.MILLIS_PER_DAY
import com.arshadshah.nimaz.core.util.NextWorshipResolver
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.core.util.WorshipReminderContent
import com.arshadshah.nimaz.core.util.formatClockTime
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.components.organisms.WorshipCardUi
import kotlinx.coroutines.flow.first
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementAction
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.AnnouncementUseCases
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.FastingUseCases
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.ObserveEventCardsUseCase
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
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
    // Today's sunrise/sunset as fractions of the day — anchor the living sky's
    // sun arc to the real sun instead of fixed clock times.
    val sunriseFraction: Float = 0.27f,
    val sunsetFraction: Float = 0.80f,
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
    val isBatteryOptimized: Boolean = false,
    // Local calendar occasions merged with any pushed CELEBRATION announcement,
    // rendered as cards in the Home events carousel (after the Jumu'ah card).
    val celebrationCards: List<HomeEventCard> = emptyList(),
    // The single nearest upcoming *enabled* extended worship reminder, rendered as the
    // "Next Worship" card in the events carousel. Null when nothing is enabled/near.
    val worshipCard: WorshipCardUi? = null,
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

/**
 * The FCM engagement banner's slice of Home state. [announcement] is null when
 * there is nothing active (nothing received, dismissed, expired or outside the
 * version window). [showCta] is true only when the announcement carries a CTA
 * label AND its route resolves (allowlisted key or https URL) — otherwise the
 * banner renders without a button.
 */
data class AnnouncementUiState(
    val announcement: Announcement? = null,
    val showCta: Boolean = false,
)

sealed interface HomeEvent {
    data class UpdateLocation(val latitude: Double, val longitude: Double, val name: String) :
        HomeEvent

    data object RefreshPrayerTimes : HomeEvent
    data object RefreshPermissions : HomeEvent
    data class TogglePrayerStatus(val prayerType: PrayerType) : HomeEvent
    data object DismissAnnouncement : HomeEvent
    data object AnnouncementCtaClicked : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val prayerUseCases: PrayerUseCases,
    private val fastingUseCases: FastingUseCases,
    private val hadithUseCases: HadithUseCases,
    private val duaUseCases: DuaUseCases,
    private val settingsRepository: SettingsRepository,
    private val announcementUseCases: AnnouncementUseCases,
    private val observeEventCards: ObserveEventCardsUseCase,
    private val nextWorshipResolver: NextWorshipResolver,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // Last announcement id logged as shown — analytics fires once per id, not
    // on every recomposition/re-emission.
    private var lastShownAnnouncementId: String? = null

    // Last announcement id logged as a route rejection — the map below can
    // re-run on every re-emission of observeActiveAnnouncement(), so this
    // guards against logging the same rejection more than once per id.
    private var lastRejectedAnnouncementId: String? = null

    /**
     * Active FCM engagement announcement (already dismissal/expiry/version-gated),
     * for the banner only. CELEBRATION-typed announcements are excluded here —
     * they render as carousel cards via [ObserveEventCardsUseCase] instead, so
     * showing them in the banner too would double-render the same occasion.
     */
    val announcement: StateFlow<AnnouncementUiState> =
        announcementUseCases.observeActiveAnnouncement()
            .map { it?.takeIf { a -> a.type != AnnouncementType.CELEBRATION } }
            .map { active ->
                val routeAction = active?.let {
                    announcementUseCases.resolveAnnouncementRoute(it.route)
                }
                if (active != null &&
                    !active.route.isNullOrBlank() &&
                    routeAction == AnnouncementAction.None &&
                    active.id != lastRejectedAnnouncementId
                ) {
                    lastRejectedAnnouncementId = active.id
                    AppAnalytics.logAnnouncementRouteRejected(active.id, active.route)
                }
                AnnouncementUiState(
                    announcement = active,
                    showCta = active?.ctaLabel != null && routeAction != AnnouncementAction.None,
                )
            }
            .onEach { uiState ->
                val active = uiState.announcement ?: return@onEach
                if (active.id != lastShownAnnouncementId) {
                    lastShownAnnouncementId = active.id
                    AppAnalytics.logAnnouncementShown(active.id, active.type.key)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnnouncementUiState())

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
        observeCelebrationCards()
        startTimeUpdates()
    }

    /** Local calendar occasions merged with any pushed CELEBRATION announcement. */
    private fun observeCelebrationCards() {
        viewModelScope.launch {
            observeEventCards().collect { cards ->
                _state.update { it.copy(celebrationCards = cards) }
            }
        }
    }

    private fun loadPrayerRecords() {
        viewModelScope.launch {
            prayerUseCases.getTodayPrayerRecords().collect { records ->
                _prayerRecords.update { records }
            }
        }
    }

    private fun observeFastingStatus() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val startOfDay = today.toUtcMidnightMillis()
            val endOfDay = startOfDay + MILLIS_PER_DAY - 1

            fastingUseCases.getFastRecordsInRange(startOfDay, endOfDay).collect { records ->
                val todayRecord = records.firstOrNull()
                _state.update { it.copy(fastingToday = todayRecord?.status == FastStatus.FASTED) }
            }
        }
    }

    private fun loadDailyHadith() {
        viewModelScope.launch {
            try {
                // GetDailyHadithUseCase seeds the backfill and applies the Knuth
                // multiplicative-hash scatter so consecutive days land on very
                // different hadiths while staying deterministic per day.
                val hadith =
                    hadithUseCases.getDailyHadith(LocalDate.now().toEpochDay()) ?: return@launch
                _state.update {
                    it.copy(
                        dailyHadith = hadith.textEnglish.let { text ->
                            if (text.length > 150) text.take(150).trimEnd() + "…" else text
                        },
                        dailyHadithReference = hadith.reference?.takeIf { ref -> ref.isNotBlank() },
                        // Carry the id so tapping the card opens this exact hadith
                        // in the reader, and a short grade label for the card chip.
                        dailyHadithId = hadith.id,
                        dailyHadithGrade = shortGradeLabel(hadith.grade)
                    )
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("home", "load_daily_hadith", e.message)
                // No hadith data available
            }
        }
    }

    /**
     * Short, chip-friendly grade label for the home hadith card (e.g. "Sahih").
     * Returns null for unknown/blank grades so the card simply omits the chip.
     */
    private fun shortGradeLabel(grade: HadithGrade?): String? =
        when (grade) {
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
                val now = LocalDate.now()
                val selection = duaUseCases.getDailyDua(
                    hourOfDay = LocalTime.now().hour,
                    dayOfYear = now.dayOfYear
                ) ?: return@launch
                val dua = selection.dua
                _state.update {
                    it.copy(
                        dailyDua = DailyDua(
                            title = dua.titleEnglish,
                            arabic = dua.textArabic,
                            translation = dua.textEnglish,
                            source = dua.reference ?: "",
                            categoryLabel = selection.categoryName,
                            categoryIcon = selection.categoryIcon ?: ""
                        )
                    )
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("home", "load_daily_dua", e.message)
                // No dua data available
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UpdateLocation -> AppAnalytics.logFeatureUsed("home", "update_location")
            is HomeEvent.TogglePrayerStatus -> AppAnalytics.logFeatureUsed(
                "home",
                "toggle_prayer_status"
            )

            else -> {}
        }
        when (event) {
            is HomeEvent.UpdateLocation -> updateLocation(
                event.latitude,
                event.longitude,
                event.name
            )

            HomeEvent.RefreshPrayerTimes -> calculatePrayerTimes()
            HomeEvent.RefreshPermissions -> checkPermissions()
            is HomeEvent.TogglePrayerStatus -> togglePrayerStatus(event.prayerType)
            HomeEvent.DismissAnnouncement -> dismissAnnouncement()
            HomeEvent.AnnouncementCtaClicked -> logAnnouncementCta()
        }
    }

    private fun dismissAnnouncement() {
        val active = announcement.value.announcement ?: return
        viewModelScope.launch {
            announcementUseCases.dismissAnnouncement(active.id)
            AppAnalytics.logAnnouncementDismissed(active.id)
        }
    }

    // Navigation itself is handled by the screen's onOpenAnnouncementRoute
    // callback (NavGraph owns the controller); the VM only records the click.
    private fun logAnnouncementCta() {
        val active = announcement.value.announcement ?: return
        AppAnalytics.logAnnouncementCtaClicked(active.id, active.route)
    }

    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
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
            val todayEpoch = LocalDate.now().toUtcMidnightMillis()
            val currentStatus = _prayerRecords.value[prayerName] ?: PrayerStatus.NOT_PRAYED
            val newStatus =
                if (currentStatus == PrayerStatus.PRAYED) PrayerStatus.NOT_PRAYED else PrayerStatus.PRAYED
            val prayedAt =
                if (newStatus == PrayerStatus.PRAYED) System.currentTimeMillis() else null

            prayerUseCases.updatePrayerStatus(todayEpoch, prayerName, newStatus, prayedAt, false)
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
                settingsRepository.latitude,
                settingsRepository.longitude,
                settingsRepository.locationName
            ) { lat: Double, lng: Double, name: String ->
                Triple(lat, lng, name)
            }

            val calcSettingsFlow = combine(
                settingsRepository.calculationMethod,
                settingsRepository.asrCalculation,
                settingsRepository.highLatitudeRule
            ) { calc: String, asr: String, high: String ->
                Triple(calc, asr, high)
            }

            val adjustmentsFlow = combine(
                settingsRepository.fajrAdjustment,
                settingsRepository.sunriseAdjustment,
                settingsRepository.dhuhrAdjustment,
                settingsRepository.asrAdjustment,
            ) { fajr, sunrise, dhuhr, asr ->
                mapOf(
                    PrayerType.FAJR to fajr,
                    PrayerType.SUNRISE to sunrise,
                    PrayerType.DHUHR to dhuhr,
                    PrayerType.ASR to asr
                )
            }.combine(
                combine(
                    settingsRepository.maghribAdjustment,
                    settingsRepository.ishaAdjustment
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
    }

    private fun updateLocation(latitude: Double, longitude: Double, name: String) {
        viewModelScope.launch {
            settingsRepository.updateLocation(latitude, longitude, name)
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

                // Sunrise/sunset as day-fractions for the living sky's sun arc.
                val sunriseFraction = prayerTimes.find { it.type == PrayerType.SUNRISE }?.time
                    ?.toLocalDateTime(timeZone)
                    ?.let { (it.hour * 60 + it.minute) / 1440f } ?: 0.27f
                val sunsetFraction = prayerTimes.find { it.type == PrayerType.MAGHRIB }?.time
                    ?.toLocalDateTime(timeZone)
                    ?.let { (it.hour * 60 + it.minute) / 1440f } ?: 0.80f

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

                val worshipCard = runCatching { buildWorshipCard(LocalDateTime.now()) }.getOrNull()

                _state.update {
                    it.copy(
                        prayerTimes = displaysWithStatus,
                        prayerTimelineProgress = timelineProgress,
                        sunriseFraction = sunriseFraction,
                        sunsetFraction = sunsetFraction,
                        currentPrayer = if (currentPrayerIndex >= 0) sortedPrayers[currentPrayerIndex].type else null,
                        nextPrayer = nextPrayer,
                        timeUntilNextPrayer = timeUntilNext,
                        hijriDate = calculateHijriDate(),
                        isFriday = isFriday,
                        jumuahTime = jumuahTime,
                        timeUntilJumuah = timeUntilJumuah,
                        isJumuahPassed = isJumuahPassed,
                        worshipCard = worshipCard,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("home", "calculate_prayer_times", e.message)
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
            settingsRepository.use24HourFormat.collect { enabled ->
                use24HourFormat = enabled
                calculatePrayerTimes() // Recalculate to reformat times
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String =
        formatClockTime(hour, minute, use24HourFormat)

    private fun calculateHijriDate(): String {
        val hijriDate = HijriDateCalculator.today()
        return hijriDate.formatted()
    }

    /**
     * Resolve the nearest upcoming enabled worship reminder into ready-to-render card data, or
     * null when none is enabled/near. Countdown is to the event instant ([eventAt]); the time
     * label is shown only where it adds meaning (Tahajjud's "Begins").
     */
    private suspend fun buildWorshipCard(now: LocalDateTime): WorshipCardUi? {
        val occ = nextWorshipResolver.nearest(now) ?: return null
        val use24 = settingsRepository.use24HourFormat.first()
        val et = occ.eventAt.toLocalTime()
        val eventTime = formatClockTime(et.hour, et.minute, use24)
        val secs = java.time.Duration.between(now, occ.eventAt).seconds.coerceAtLeast(0)
        val hours = secs / 3600
        val minutes = (secs % 3600) / 60
        val countdown = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        val timeLabel = if (occ.type == WorshipReminderType.TAHAJJUD)
            context.getString(R.string.worship_card_begins) else ""
        return WorshipCardUi(
            type = occ.type,
            name = WorshipReminderContent.name(context, occ.type),
            arabic = WorshipReminderContent.arabic(context, occ.type),
            body = WorshipReminderContent.body(context, occ.type, occ.subKey),
            eventTime = eventTime,
            timeLabel = timeLabel,
            countdown = countdown,
            countdownLabel = context.getString(R.string.worship_card_in)
        )
    }
}
