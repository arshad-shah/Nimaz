package com.arshadshah.nimaz.presentation.viewmodel.home

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
import com.arshadshah.nimaz.core.util.currentPrayerIndexAt
import com.arshadshah.nimaz.core.util.nextPrayerIndexAt
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementAction
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.FallbackLocation
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.WorshipReminderOccurrence
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.AnnouncementUseCases
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.FastingUseCases
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.ObserveEventCardsUseCase
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.presentation.components.organisms.WorshipCardUi
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import com.arshadshah.nimaz.core.time.TodayProvider
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.Job
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
import com.arshadshah.nimaz.presentation.model.DailyDua
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.model.withClockState

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
    private val todayProvider: TodayProvider,
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

    // Re-armable per rollover: each of these is scoped to a specific day, either because it
    // collects a Room flow bound to a fixed epoch range or because it reads the date once.
    private var fastingJob: Job? = null
    private var prayerRecordsJob: Job? = null
    private var dailyHadithJob: Job? = null
    private var dailyDuaJob: Job? = null

    init {
        checkPermissions()
        // observeLocation() also observes the calculation settings and adjustments, and triggers
        // the recompute itself — so there is no separate time-format observer any more: the
        // 12/24-hour preference is read at the leaf and never invalidates prayer instants.
        observeLocation()
        loadPrayerRecords()
        observeFastingStatus()
        loadDailyHadith()
        loadDailyDua()
        observeCelebrationCards()
        scheduleWorshipRefresh()
        observeDateRollover()
    }

    /**
     * One place where the day changing re-arms everything scoped to it.
     *
     * Each of these read the date once, at `init`, and never again: the fasting collector
     * bound a Room query to a fixed `[startOfDay, endOfDay]` range, the prayer-record flow
     * resolved "today" inside the repository at call time, and the daily hadith and dua were
     * picked from a day number and an hour read at construction. Left open at 23:50 the app
     * still said "fasting today" at 00:05 about yesterday's record — and marking the new day's
     * fast could never light it up, because the collector's range did not include the new day.
     *
     * `HomeScreen` already dispatched `RefreshPrayerTimes` on rollover, but that reached only
     * `calculatePrayerTimes()`. This re-issues the lot as one unit.
     */
    private fun observeDateRollover() {
        viewModelScope.launch {
            todayProvider.todayChanges.collect { today ->
                // `dayTimesDate` is the record of which day the cached prayer instants were
                // computed for. It was assigned and read nowhere, while the comment beside it
                // claimed it guarded the rollover recompute. It does now.
                if (dayTimesDate == today) return@collect

                observeFastingStatus()
                loadPrayerRecords()
                loadDailyHadith()
                loadDailyDua()
                calculatePrayerTimes()
            }
        }
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
        // `getTodayPrayerRecords()` resolves "today" once, at call time, and returns a Flow
        // bound to that epoch — so this needs re-invoking at rollover exactly as the fasting
        // collector does, or Home's prayer card stays bound to the day the app was opened.
        prayerRecordsJob?.cancel()
        prayerRecordsJob = viewModelScope.launch {
            prayerUseCases.getTodayPrayerRecords().collect { records ->
                _prayerRecords.update { records }
            }
        }
    }

    private fun observeFastingStatus() {
        fastingJob?.cancel()
        fastingJob = viewModelScope.launch {
            val today = todayProvider.today()
            val startOfDay = today.toUtcMidnightMillis()
            val endOfDay = startOfDay + MILLIS_PER_DAY - 1

            fastingUseCases.getFastRecordsInRange(startOfDay, endOfDay).collect { records ->
                val todayRecord = records.firstOrNull()
                _state.update { it.copy(fastingToday = todayRecord?.status == FastStatus.FASTED) }
            }
        }
    }

    private fun loadDailyHadith() {
        dailyHadithJob?.cancel()
        dailyHadithJob = viewModelScope.launch {
            try {
                // GetDailyHadithUseCase seeds the backfill and applies the Knuth
                // multiplicative-hash scatter so consecutive days land on very
                // different hadiths while staying deterministic per day.
                val hadith =
                    hadithUseCases.getDailyHadith(todayProvider.today().toEpochDay())
                        ?: return@launch
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
                AppAnalytics.logError(AppAnalytics.Feature.HOME, "load_daily_hadith", e.message)
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
        dailyDuaJob?.cancel()
        dailyDuaJob = viewModelScope.launch {
            try {
                val now = todayProvider.today()
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
                AppAnalytics.logError(AppAnalytics.Feature.HOME, "load_daily_dua", e.message)
                // No dua data available
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UpdateLocation -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.HOME, "update_location")
                updateLocation(
                    event.latitude,
                    event.longitude,
                    event.name
                )
            }

            HomeEvent.RefreshPrayerTimes -> calculatePrayerTimes()
            HomeEvent.RefreshPermissions -> checkPermissions()
            // The log moved inside togglePrayerStatus, past its Sunrise guard. Logged here
            // it counted taps that toggled nothing: Sunrise is not a prayer and returns early,
            // so the dashboard has been reporting toggles that never happened.
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

        AppAnalytics.logFeatureUsed(AppAnalytics.Feature.HOME, "toggle_prayer_status")

        viewModelScope.launch {
            val prayerName = PrayerName.valueOf(prayerType.name)
            val todayEpoch = todayProvider.today().toUtcMidnightMillis()
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

                val resolved = resolveLocation(lat, lng, name)
                val hasLocation = !resolved.isFallback
                val latitude = resolved.latitude
                val longitude = resolved.longitude
                val locationName = resolved.name.ifBlank { FallbackLocation.NAME }

                // Cache calculation settings
                // The domain's own parsers, in place of `valueOf` in a swallowing `try` plus a
                // hand-written "hanafi" comparison. `valueOf` throws on every persisted alias
                // the app itself writes — "MWL", "ISNA", "MAKKAH" — and the catch then
                // substituted Muslim World League, so a user on ISNA silently got MWL prayer
                // times, forever, with no signal. `fromString` knows the aliases.
                cachedCalcMethod = CalculationMethod.fromString(calcStr)
                cachedAsrCalc = AsrCalculation.fromString(asrStr)
                cachedHighLatRule = HighLatitudeRule.fromString(highStr)
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
        /**
         * How long to wait before re-checking the worship card when there is nothing to surface
         * (or the occurrence has no window). Long enough that it is not polling, short enough that
         * enabling a reminder in settings shows up without a restart. A settings change cancels
         * the wait immediately via [scheduleWorshipRefresh]'s cancel-and-replace.
         */
        private const val FALLBACK_WORSHIP_RECHECK_MS = 15 * 60 * 1000L

        /** Floor on the sleep, so a just-expired occurrence cannot spin the loop. */
        private const val MIN_WORSHIP_RECHECK_MS = 1_000L

        // Default location: Dublin, Ireland (as shown in prototype)
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

    // ── Cached heavy results ────────────────────────────────────────────────
    // Today's prayer instants and tomorrow's Fajr, plus the date they were
    // computed for. Recomputed only when their inputs change (location,
    // calculation settings, time format, date roll-over) — never on a countdown
    // tick, which re-derives everything below from these cached values.
    private var dayTimes: List<PrayerTime> = emptyList()
    private var dayTimesDate: LocalDate? = null
    private var tomorrowFajr: Instant? = null

    // Nearest upcoming worship reminder. Resolving one costs ~30 sequential
    // DataStore reads, but `eventAt` is a fixed instant and the card's countdown
    // renders in whole minutes — so we resolve rarely and reformat in between.
    private var worshipOccurrence: WorshipReminderOccurrence? = null
    private var worshipStale: Boolean = true

    /**
     * Full recompute of the day's prayer instants — the *expensive* path, running
     * the astronomical calculation for today and tomorrow. Called only when its
     * inputs actually change: location, calculation settings, or the date rolling over. Nothing
     * clock-derived lives here — [publishPrayerDisplays] emits only facts, and the UI derives
     * countdowns/next-prayer from them at the leaf. The 12/24-hour toggle no longer reaches this
     * path at all.
     */
    private fun calculatePrayerTimes() {
        val resolved = resolveLocation(_state.value.latitude, _state.value.longitude)
        val latitude = resolved.latitude
        val longitude = resolved.longitude

        viewModelScope.launch {
            try {
                // `isLoading` means "nothing to show yet", not "busy". Only the
                // genuine first load may show the full-screen spinner; a refresh
                // over existing data updates in place. Setting it on every refresh
                // is what made the screen flash a spinner once a second, since any
                // suspension before it is cleared makes the `true` observable.
                if (dayTimes.isEmpty()) {
                    _state.update { it.copy(isLoading = true) }
                }

                val today = todayProvider.today()
                val prayerTimes = prayerTimeCalculator.getPrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    date = today,
                    calculationMethod = cachedCalcMethod,
                    asrCalculation = cachedAsrCalc,
                    highLatitudeRule = cachedHighLatRule,
                    adjustments = cachedAdjustments
                )
                // Tomorrow's Fajr, for the after-Isha wrap. Computed here rather
                // than lazily in the tick so the hot path stays branch-free.
                tomorrowFajr = prayerTimeCalculator.getPrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    date = today.plusDays(1),
                    calculationMethod = cachedCalcMethod,
                    asrCalculation = cachedAsrCalc,
                    highLatitudeRule = cachedHighLatRule,
                    adjustments = cachedAdjustments
                ).find { it.type == PrayerType.FAJR }?.time
                dayTimes = prayerTimes
                dayTimesDate = today

                // Location/calculation settings feed the worship reminders too.
                worshipStale = true

                publishPrayerDisplays()
                // No suspension between publishPrayerDisplays()'s emission and this one, so
                // StateFlow conflates them into a single update.
                _state.update { it.copy(isLoading = false, error = null) }

                // Suspends (DataStore), but only after isLoading is already false. Re-arm rather
                // than calling refreshWorshipCard() directly: the settings that just changed also
                // move the occurrence's expiry, so the pending sleep must be replaced too.
                scheduleWorshipRefresh()
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError(AppAnalytics.Feature.HOME, "calculate_prayer_times", e.message)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Publish the day's prayer facts — instants, records, sunrise/sunset fractions, the Hijri date
     * and Friday's Dhuhr. Deliberately **free of "now"**: nothing here depends on what time it is,
     * so it runs when its *inputs* change (location, calculation settings, records, date rollover)
     * rather than once a second.
     *
     * Everything that does move with the clock — which prayer is next/current/passed, every
     * countdown, the timeline fill — is derived at the leaf from these instants plus the shared
     * ticker (`rememberNow`). See [withClockState].
     */
    private fun publishPrayerDisplays() {
        val prayerTimes = dayTimes
        if (prayerTimes.isEmpty()) return

        val timeZone = TimeZone.currentSystemDefault()
        val records = _prayerRecords.value

        val displays = prayerTimes
            .sortedBy { it.time }
            .map { prayerTime ->
                val prayerName = PrayerName.valueOf(prayerTime.type.name)
                PrayerTimeDisplay(
                    type = prayerTime.type,
                    name = prayerTime.type.displayName,
                    timeAt = prayerTime.time,
                    prayerStatus = records[prayerName] ?: PrayerStatus.NOT_PRAYED
                )
            }

        // Sunrise/sunset as day-fractions for the living sky's sun arc. These follow the day's
        // prayer times, not the clock, so they stay pushed state.
        val sunriseFraction = prayerTimes.find { it.type == PrayerType.SUNRISE }?.time
            ?.toLocalDateTime(timeZone)
            ?.let { (it.hour * 60 + it.minute) / 1440f } ?: 0.27f
        val sunsetFraction = prayerTimes.find { it.type == PrayerType.MAGHRIB }?.time
            ?.toLocalDateTime(timeZone)
            ?.let { (it.hour * 60 + it.minute) / 1440f } ?: 0.80f

        val isFriday = todayProvider.today().dayOfWeek == DayOfWeek.FRIDAY
        val dhuhrInstant = prayerTimes.find { it.type == PrayerType.DHUHR }?.time

        _state.update {
            it.copy(
                prayerTimes = displays,
                tomorrowFajrAt = tomorrowFajr,
                sunriseFraction = sunriseFraction,
                sunsetFraction = sunsetFraction,
                hijriDate = calculateHijriDate(),
                isFriday = isFriday,
                jumuahAt = if (isFriday) dhuhrInstant else null,
            )
        }
    }

    /**
     * Re-resolve the "Next Worship" card exactly when it can change, instead of polling.
     *
     * [NextWorshipResolver.nearest] costs ~30 sequential DataStore reads, so the old 60s loop paid
     * that 1,440 times a day to notice a handful of transitions. Here the card is resolved once,
     * then a single job sleeps until the current occurrence's window closes (or a short fallback
     * when there is nothing to show) and resolves again. Cancel-and-replace via [worshipJob] means
     * a settings change supersedes an in-flight wait rather than racing it.
     */
    private var worshipJob: Job? = null

    private fun scheduleWorshipRefresh() {
        worshipJob?.cancel()
        worshipJob = viewModelScope.launch {
            // Not a tick loop: exactly one wake per transition. `delay` is cancellable, so
            // cancelling the job (or the scope) exits here without an isActive guard.
            while (true) {
                // Guarded end-to-end, including the sleep arithmetic. This runs from a bare
                // viewModelScope coroutine, so anything escaping here reaches the uncaught
                // handler and crashes the app — the card is optional, so on failure we show
                // nothing and retry on the slow fallback instead.
                val millis = runCatching {
                    refreshWorshipCard()
                    val expiry = worshipOccurrence?.let { it.windowEnd ?: it.eventAt }
                    // Sleep until the surfaced occurrence stops being current. With nothing to
                    // show, fall back to a slow re-check so a newly-enabled reminder still
                    // appears without a restart.
                    if (expiry == null) {
                        FALLBACK_WORSHIP_RECHECK_MS
                    } else {
                        java.time.Duration.between(LocalDateTime.now(), expiry)
                            .toMillis()
                            .coerceIn(MIN_WORSHIP_RECHECK_MS, FALLBACK_WORSHIP_RECHECK_MS)
                    }
                }.onFailure { CrashReporter.recordException(it) }
                    .getOrDefault(FALLBACK_WORSHIP_RECHECK_MS)
                delay(millis)
            }
        }
    }

    /**
     * Resolve + render the "Next Worship" card. [NextWorshipResolver.nearest] reads
     * every worship preference plus location and calculation settings — ~30
     * sequential DataStore reads — so the resolved occurrence is cached and
     * re-resolved only when it is missing, has elapsed, or its settings changed
     * ([worshipStale]). In between, only the countdown string is reformatted.
     */
    private suspend fun refreshWorshipCard() {
        // Guarded end-to-end: this runs from a bare viewModelScope loop, so an
        // escaping exception would reach the uncaught handler and crash the app.
        // The card is optional — on failure we simply show nothing.
        val card = runCatching {
            val now = LocalDateTime.now()
            val cached = worshipOccurrence
            // An occurrence stays valid until its window closes (or its event, if it has no
            // window) — not merely until its event begins. Re-resolving on `eventAt` alone would
            // drop an active occurrence the instant its event started, reintroducing the very
            // gap the window model fixed.
            val liveUntil = cached?.let { it.windowEnd ?: it.eventAt }
            if (worshipStale || liveUntil == null || !now.isBefore(liveUntil)) {
                worshipOccurrence = nextWorshipResolver.nearest(now)
                worshipStale = false
            }
            worshipOccurrence?.let { renderWorshipCard(it) }
        }.onFailure { CrashReporter.recordException(it) }.getOrNull()
        _state.update { it.copy(worshipCard = card) }
    }

    // No `use24HourFormat` mirror here on purpose. The theme already publishes
    // `LocalUse24HourFormat`, and prayer/worship times are formatted at the leaf from it, so
    // toggling the preference is a pure recomposition instead of a full astronomical recompute.

    private fun calculateHijriDate(): String {
        val hijriDate = HijriDateCalculator.today()
        return hijriDate.formatted()
    }

    /**
     * Map an already-resolved worship occurrence into card data. The card carries **instants**
     * ([WorshipReminderOccurrence.eventAt]/`windowStart`/`windowEnd`) and derives its own
     * countdown, proximity and 12/24-hour formatting at the leaf via the shared ticker — so this
     * is a pure, allocation-light mapping, not a per-minute string render. This is the one place
     * the occurrence's wall-clock `LocalDateTime`s convert to instants (system zone).
     */
    private fun renderWorshipCard(occ: WorshipReminderOccurrence): WorshipCardUi {
        val zone = ZoneId.systemDefault()
        fun toInstant(ldt: LocalDateTime): Instant =
            Instant.fromEpochMilliseconds(ldt.atZone(zone).toInstant().toEpochMilli())
        return WorshipCardUi(
            type = occ.type,
            name = WorshipReminderContent.name(context, occ.type),
            arabic = WorshipReminderContent.arabic(context, occ.type),
            body = WorshipReminderContent.body(context, occ.type, occ.subKey),
            eventAt = toInstant(occ.eventAt),
            windowStart = occ.windowStart?.let(::toInstant),
            windowEnd = occ.windowEnd?.let(::toInstant),
            subKey = occ.subKey,
        )
    }
}
