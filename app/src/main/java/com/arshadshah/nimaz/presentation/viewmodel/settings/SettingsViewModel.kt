package com.arshadshah.nimaz.presentation.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.audio.DownloadState
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.repository.AdhanDownloader
import com.arshadshah.nimaz.domain.repository.AppLocale
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.ClearAllUserDataUseCase
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.notification.RescheduleNotificationsUseCase
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** The prayer whose settings stand in for the set on summary rows. */
private const val FAJR = "fajr"

// Enums for settings options
enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
) {
    ENGLISH("en", "English", "English", "GB"),
    TURKISH("tr", "Turkish", "Türkçe", "TR"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "ID"),
    MALAY("ms", "Malay", "Bahasa Melayu", "MY"),
    FRENCH("fr", "French", "Français", "FR"),
    GERMAN("de", "German", "Deutsch", "DE")
}

/**
 * A small, read-only rollup of the notification settings that other screens (e.g. Prayer
 * Settings) show as summary subtitles. Sourced reactively from DataStore so it stays in sync
 * no matter which screen changed the underlying value — see [SettingsViewModel.notificationSummary].
 */
data class NotificationSummary(
    val notificationsMasterEnabled: Boolean = true,
    val enabledPrayerCount: Int = TOTAL_PRAYER_COUNT,
    /** Fajr's reminder — it stands for the set where one line has to speak for five. */
    val reminderEnabled: Boolean = true,
    val reminderMinutes: Int = PrayerAlertStyle.DEFAULT_REMINDER_MINUTES,
    val fajrAlertStyle: PrayerAlertStyle = PrayerAlertStyle.NOTIFICATION
) {
    companion object {
        /** The five obligatory prayers the notification screen exposes toggles for. */
        const val TOTAL_PRAYER_COUNT = 5
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // The one remaining site that constructed a PrayerTimeCalculator directly was
    // WidgetsScreen's preview builder; it takes this now, injected like everywhere else
    // (CLEAN_ARCHITECTURE_CHECKLIST — the pattern already removed from five ViewModels).
    val prayerTimeCalculator: PrayerTimeCalculator,
    private val appLocale: AppLocale,
    private val adhanDownloader: AdhanDownloader,
    private val prayerUseCases: PrayerUseCases,
    private val settingsRepository: SettingsRepository,
    private val quranUseCases: QuranUseCases,
    private val prayerNotificationScheduler: PrayerNotificationScheduler,
    private val rescheduleNotificationsUseCase: RescheduleNotificationsUseCase,
    // Only the new failure paths report through this so far. The ~40 existing AppAnalytics
    // calls in this file are the analytics catalog's job (#355), not this layer's.
    private val telemetry: Telemetry,
    private val adhanAudioManager: AdhanAudioManager,
    private val clearAllUserData: ClearAllUserDataUseCase,
    private val todayProvider: TodayProvider,
) : ViewModel() {

    private val _generalState = MutableStateFlow(GeneralSettingsUiState())
    val generalState: StateFlow<GeneralSettingsUiState> = _generalState.asStateFlow()

    private val _prayerState = MutableStateFlow(PrayerSettingsUiState())
    val prayerState: StateFlow<PrayerSettingsUiState> = _prayerState.asStateFlow()

    private val _notificationState = MutableStateFlow(NotificationSettingsUiState())
    val notificationState: StateFlow<NotificationSettingsUiState> = _notificationState.asStateFlow()

    private val _quranState = MutableStateFlow(QuranSettingsUiState())
    val quranState: StateFlow<QuranSettingsUiState> = _quranState.asStateFlow()

    private val _duaState = MutableStateFlow(DuaSettingsUiState())
    val duaState: StateFlow<DuaSettingsUiState> = _duaState.asStateFlow()

    private val _hadithState = MutableStateFlow(HadithSettingsUiState())
    val hadithState: StateFlow<HadithSettingsUiState> = _hadithState.asStateFlow()

    private val _locationState = MutableStateFlow(LocationSettingsUiState())
    val locationState: StateFlow<LocationSettingsUiState> = _locationState.asStateFlow()

    private val _shouldRestart = MutableStateFlow(false)
    val shouldRestart: StateFlow<Boolean> = _shouldRestart.asStateFlow()

    private val _adhanPreviewError = MutableStateFlow<String?>(null)
    val adhanPreviewError: StateFlow<String?> = _adhanPreviewError.asStateFlow()

    // The adhan engine's live playback and download state, named and re-exposed rather than
    // reached through the manager itself. `ARCHITECTURE.md` §9 sanctions a playback ViewModel
    // forwarding the engine's `StateFlow` for live progress; it does not sanction handing the
    // screen the whole manager, which also gave it `play`, `stop` and `downloadAdhan`.
    val adhanDownloadState: StateFlow<Map<AdhanSound, DownloadState>> =
        adhanAudioManager.downloadState
    val isAdhanPlaying: StateFlow<Boolean> = adhanAudioManager.isPlaying
    val currentlyPlayingAdhan: StateFlow<AdhanSound?> = adhanAudioManager.currentlyPlaying

    /**
     * Reactive rollup of the notification settings for summary subtitles on other screens.
     *
     * Unlike [notificationState] (a one-shot snapshot loaded in [loadSettings]), this collects
     * DataStore directly, so it reflects edits made from *any* screen — including the
     * Notification Settings screen, which runs on a separate [SettingsViewModel] instance.
     * DataStore is a singleton, so every collector across every instance sees the same live value.
     */
    val notificationSummary: StateFlow<NotificationSummary> = combine(
        combine(
            settingsRepository.fajrNotificationEnabled,
            settingsRepository.dhuhrNotificationEnabled,
            settingsRepository.asrNotificationEnabled,
            settingsRepository.maghribNotificationEnabled,
            settingsRepository.ishaNotificationEnabled
        ) { flags -> flags.count { it } },
        settingsRepository.prayerNotificationsEnabled,
        // Fajr stands for the set on the hub: it is the prayer people set most deliberately,
        // and a row cannot show five different offsets in one line.
        settingsRepository.prayerReminderEnabled(FAJR),
        settingsRepository.prayerReminderMinutes(FAJR),
        settingsRepository.prayerAlertStyle(FAJR)
    ) { enabledPrayerCount, masterEnabled, reminderEnabled, reminderMinutes, alertStyle ->
        NotificationSummary(
            notificationsMasterEnabled = masterEnabled,
            enabledPrayerCount = enabledPrayerCount,
            reminderEnabled = reminderEnabled,
            reminderMinutes = reminderMinutes,
            fajrAlertStyle = alertStyle
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationSummary()
    )

    /**
     * Today's prayer times for the current location, or null until a location is known.
     *
     * The prayer notification rows show each prayer's time in their header, so the setting
     * reads against the thing it governs rather than as an abstraction.
     */
    /**
     * Today's times for the notification rows — re-derived when the **day** changes, not only
     * when the location does.
     *
     * `LocalDate.now()` was read inside the `map`, so the date was fixed at whatever it was when
     * `getCurrentLocation()` last emitted. A settings screen left open across midnight went on
     * showing yesterday's prayer times next to each notification row. Combining with
     * `todayChanges` makes the day an input rather than an ambient read.
     */
    val todayPrayerTimes: StateFlow<PrayerTimes?> = combine(
        // The calculation settings, not `getCurrentLocation()`. That reads the `locations`
        // table, which is written only by searching for a place and picking it — a user who
        // detected their location by GPS or set it in onboarding has one in preferences and no
        // row, so these rows showed no time at all next to a notification they had enabled.
        prayerUseCases.observeCalculationSettings(),
        todayProvider.todayChanges,
    ) { settings, today ->
        runCatching {
            prayerUseCases.getPrayerTimesForDate(today, settings)
        }.getOrNull()
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun clearAdhanPreviewError() {
        _adhanPreviewError.value = null
    }

    init {
        loadSettings()
        loadLocations()
        observeQuranSettings()
        observeQuranPreviewTranslation()
    }

    /**
     * Keeps [quranState] collecting DataStore rather than holding the snapshot [loadSettings]
     * read once at construction.
     *
     * `hiltViewModel()` scopes a ViewModel to the *nav back-stack entry*, so the Quran Settings
     * screen and the reciter/translation pickers it opens each run their own [SettingsViewModel].
     * The picker wrote the new reciter to DataStore and updated **its own** `_quranState`; the
     * settings screen behind it kept the snapshot it took at construction, so coming back showed
     * the old reciter (and the old translator) until the screen was destroyed and rebuilt.
     *
     * DataStore is a singleton, so collecting it here means every instance sees the same live
     * value whichever screen changed it — the same fix [notificationSummary] applies to the
     * notification rollup. The optimistic `_quranState.update` in [onEvent] stays: it paints the
     * change on the frame of the tap, and this observer reconciles right behind it.
     */
    private fun observeQuranSettings() {
        val content = combine(
            settingsRepository.quranTranslatorId,
            settingsRepository.quranArabicFont,
            settingsRepository.selectedReciterId,
            settingsRepository.quranMushafScript
        ) { translatorId, arabicFontId, reciterId, script ->
            QuranContentPrefs(translatorId, arabicFontId, reciterId, MushafScript.fromName(script))
        }
        val display = combine(
            settingsRepository.showTranslation,
            settingsRepository.showTransliteration,
            settingsRepository.quranArabicFontSize,
            settingsRepository.quranTranslationFontSize
        ) { showTranslation, showTransliteration, arabicFontSize, translationFontSize ->
            QuranDisplayPrefs(
                showTranslation,
                showTransliteration,
                arabicFontSize,
                translationFontSize
            )
        }
        val behaviour = combine(
            settingsRepository.continuousReading,
            settingsRepository.keepScreenOn,
            settingsRepository.showTajweed,
            settingsRepository.tajweedUnderline
        ) { continuousReading, keepScreenOn, showTajweed, tajweedUnderline ->
            QuranBehaviourPrefs(continuousReading, keepScreenOn, showTajweed, tajweedUnderline)
        }

        combine(content, display, behaviour) { c, d, b -> Triple(c, d, b) }
            .distinctUntilChanged()
            .onEach { (c, d, b) ->
                _quranState.update {
                    it.copy(
                        selectedTranslatorId = c.translatorId,
                        selectedArabicFontId = c.arabicFontId,
                        selectedReciterId = c.reciterId,
                        mushafScript = c.mushafScript,
                        showTranslation = d.showTranslation,
                        showTransliteration = d.showTransliteration,
                        arabicFontSize = d.arabicFontSize,
                        translationFontSize = d.translationFontSize,
                        continuousReading = b.continuousReading,
                        keepScreenOn = b.keepScreenOn,
                        showTajweed = b.showTajweed,
                        tajweedUnderline = b.tajweedUnderline
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Keeps [QuranSettingsUiState.previewTranslation] in step with the selected translation
     * so the settings preview card renders the real text of whatever the user just picked.
     *
     * Driven off the persisted preference rather than the picker callback, so it is correct
     * however the value changed. `flatMapLatest` means rapidly switching translations never
     * lets a slow earlier load overwrite a newer one — the first read of a translation also
     * seeds its 6,236 rows, so an earlier one can genuinely still be in flight.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeQuranPreviewTranslation() {
        settingsRepository.quranTranslatorId
            .distinctUntilChanged()
            .flatMapLatest { translatorId ->
                flow { emit(quranUseCases.getAyahTranslation(PREVIEW_AYAH_ID, translatorId)) }
                    // Inside the flatMapLatest, deliberately. Applied outside, the first
                    // failed read would end the whole chain — `quranTranslatorId` never
                    // completes on its own, but a caught upstream throw completes it anyway —
                    // and the preview would be frozen on the previous translation for the
                    // ViewModel's entire life, with every later change silently doing nothing.
                    .catchAndReport(telemetry, "settings", "preview_translation") { emit(null) }
            }
            .onEach { text ->
                // Keep the previous text on a null (missing/failed) result rather than
                // blanking the card.
                if (text != null) _quranState.update { it.copy(previewTranslation = text) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsEvent) {
        // Every settings-shaped event reports itself, from one table rather than a line in
        // each of 78 branches — which is how 56 of them came to report nothing at all. See
        // `asSettingChange`. The events that are not setting changes return null and are
        // logged where they happen.
        event.asSettingChange()?.let { (setting, value) ->
            telemetry.settingChanged(setting, value)
        }
        when (event) {
            // General
            is SettingsEvent.SetTheme -> {
                _generalState.update { it.copy(theme = event.theme) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    val modeString = when (event.theme) {
                        AppTheme.SYSTEM -> "system"
                        AppTheme.LIGHT -> "light"
                        AppTheme.DARK -> "dark"
                    }
                    settingsRepository.setThemeMode(modeString)
                }
            }

            is SettingsEvent.SetLanguage -> {
                _generalState.update { it.copy(language = event.language) }
                // AppAnalytics.UserProperty.APP_LANGUAGE has been declared and never set, so
                // every segmentation by language has been empty since it was added.
                AppAnalytics.setUserProperty(
                    AppAnalytics.UserProperty.APP_LANGUAGE,
                    event.language.code
                )
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setAppLanguage(event.language.code)
                    appLocale.apply(event.language.code)
                    _shouldRestart.value = true
                }
            }

            is SettingsEvent.SetHijriPrimary -> {
                _generalState.update { it.copy(useHijriPrimary = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setUseHijriPrimary(event.enabled) }
            }

            is SettingsEvent.SetHijriDayOffset -> {
                _generalState.update { it.copy(hijriDayOffset = event.days) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHijriDayOffset(event.days) }
            }

            is SettingsEvent.Set24HourFormat -> {
                _generalState.update { it.copy(use24HourFormat = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setUse24HourFormat(event.enabled) }
            }

            is SettingsEvent.SetHapticFeedback -> {
                _generalState.update { it.copy(hapticFeedback = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHapticFeedback(event.enabled) }
            }

            is SettingsEvent.SetShowIslamicPatterns -> {
                _generalState.update { it.copy(showIslamicPatterns = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setShowIslamicPatterns(event.enabled) }
            }

            is SettingsEvent.SetPatternStyle -> {
                // The style is the single source of truth for the ornament: NONE is
                // "off". We also keep the legacy boolean in sync so import/export and
                // any remaining reader of showIslamicPatterns stay correct.
                val enabled = event.style != NimazPatternStyle.NONE
                _generalState.update {
                    it.copy(patternStyle = event.style, showIslamicPatterns = enabled)
                }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setPatternStyle(event.style.name)
                    settingsRepository.setShowIslamicPatterns(enabled)
                }
            }

            is SettingsEvent.SetAnimationsEnabled -> {
                _generalState.update { it.copy(animationsEnabled = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setAnimationsEnabled(event.enabled) }
            }

            is SettingsEvent.SetShowCountdown -> {
                _generalState.update { it.copy(showCountdown = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setShowCountdown(event.enabled) }
            }

            is SettingsEvent.SetShowQuickActions -> {
                _generalState.update { it.copy(showQuickActions = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setShowQuickActions(event.enabled) }
            }

            // Prayer
            is SettingsEvent.SetCalculationMethod -> {
                _prayerState.update { it.copy(calculationMethod = event.method) }
                AppAnalytics.setUserProperty(
                    AppAnalytics.UserProperty.CALC_METHOD,
                    event.method.name
                )
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setCalculationMethod(event.method.name)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetAsrMethod -> {
                _prayerState.update { it.copy(asrMethod = event.method) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setAsrCalculation(event.method.name.lowercase())
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetHighLatitudeRule -> {
                _prayerState.update { it.copy(highLatitudeRule = event.rule) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setHighLatitudeRule(event.rule.name)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetPrayerAdjustment -> {
                updatePrayerAdjustment(event.prayer, event.minutes)
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setPrayerAdjustment(event.prayer, event.minutes)
                    rescheduleNotifications()
                }
            }

            // Notifications
            is SettingsEvent.SetNotificationsEnabled -> {
                _notificationState.update { it.copy(notificationsEnabled = event.enabled) }
                AppAnalytics.setUserProperty(
                    AppAnalytics.UserProperty.NOTIFICATIONS_ENABLED,
                    event.enabled.toString()
                )
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setPrayerNotificationsEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetPrayerNotification -> {
                updatePrayerNotification(event.prayer, event.enabled)
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setPrayerNotificationEnabled(event.prayer, event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetAdhanEnabled -> {
                _notificationState.update { it.copy(adhanEnabled = event.enabled) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setAdhanEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetPrayerAlertStyle -> {
                val prayer = event.prayer.lowercase()
                _notificationState.update {
                    it.copy(alertStyles = it.alertStyles + (prayer to event.style))
                }
                // The style is read at fire time, so there is nothing to reschedule.
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setPrayerAlertStyle(prayer, event.style)
                }
            }

            is SettingsEvent.SetPrayerReminderEnabled -> {
                val prayer = event.prayer.lowercase()
                _notificationState.update {
                    it.copy(reminderEnabled = it.reminderEnabled + (prayer to event.enabled))
                }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setPrayerReminderEnabled(prayer, event.enabled)
                    // The lead time is baked into the alarm, so this one does need rearming.
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetPrayerReminderMinutes -> {
                val prayer = event.prayer.lowercase()
                _notificationState.update {
                    it.copy(reminderOffsets = it.reminderOffsets + (prayer to event.minutes))
                }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setPrayerReminderMinutes(prayer, event.minutes)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetVibrationEnabled -> {
                _notificationState.update { it.copy(vibrationEnabled = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setNotificationVibration(event.enabled) }
            }

            is SettingsEvent.SetRespectDnd -> {
                _notificationState.update { it.copy(respectDnd = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setAdhanRespectDnd(event.enabled) }
            }

            is SettingsEvent.SetReminderMinutes -> {
                _notificationState.update { it.copy(reminderMinutes = event.minutes) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setNotificationReminderMinutes(event.minutes)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetShowReminderBefore -> {
                _notificationState.update { it.copy(showReminderBefore = event.enabled) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setShowReminderBefore(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetPersistentNotification -> {
                _notificationState.update { it.copy(persistentNotification = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setPersistentNotification(event.enabled) }
            }

            is SettingsEvent.SetFridayReminderEnabled -> {
                _notificationState.update { it.copy(fridayReminderEnabled = event.enabled) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setFridayReminderEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetFridayReminderMinutes -> {
                _notificationState.update { it.copy(fridayReminderMinutes = event.minutes) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setFridayReminderMinutes(event.minutes)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetKhatamReminderEnabled -> {
                _notificationState.update { it.copy(khatamReminderEnabled = event.enabled) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setKhatamReminderEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetKhatamReminderTime -> {
                _notificationState.update { it.copy(khatamReminderTime = event.time) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setKhatamReminderTime(event.time)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetWorshipReminderEnabled -> {
                _notificationState.update {
                    it.copy(worshipReminders = it.worshipReminders + (event.key to event.enabled))
                }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setWorshipReminderEnabled(event.key, event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetWorshipReminderOffset -> {
                _notificationState.update {
                    it.copy(worshipOffsets = it.worshipOffsets + (event.key to event.minutes))
                }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setWorshipReminderOffset(event.key, event.minutes)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetWorshipReminderMode -> {
                _notificationState.update {
                    it.copy(worshipModes = it.worshipModes + (event.key to event.mode))
                }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setWorshipReminderMode(event.key, event.mode)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetAdhanSound -> {
                _notificationState.update { it.copy(selectedAdhanSound = event.sound) }
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    settingsRepository.setSelectedAdhanSound(event.sound)
                    // Download the selected adhan if not already downloaded
                    val sound = AdhanSound.fromName(event.sound)
                    if (!adhanAudioManager.isFullyDownloaded(sound)) {
                        adhanDownloader.download(sound.name)
                    }
                }
            }

            SettingsEvent.PreviewAdhanSound -> {
                val sound = AdhanSound.fromName(_notificationState.value.selectedAdhanSound)
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    try {
                        // Ensure both variants are downloaded
                        if (!adhanAudioManager.isFullyDownloaded(sound)) {
                            val success = adhanAudioManager.downloadAdhanWithFajr(sound)
                            if (!success) {
                                _adhanPreviewError.value =
                                    "Failed to download adhan audio. Please check your internet connection."
                                return@launchSafely
                            }
                        }
                        // Now play the preview
                        adhanAudioManager.preview(sound, false)
                    } catch (e: Exception) {
                        telemetry.failure(AppAnalytics.Feature.SETTINGS, "adhan_preview", e)
                        _adhanPreviewError.value = "Failed to play adhan preview: ${e.message}"
                    }
                }
            }

            SettingsEvent.StopAdhanPreview -> {
                adhanAudioManager.stopPreview()
            }

            // Quran
            is SettingsEvent.SetTranslator -> {
                _quranState.update { it.copy(selectedTranslatorId = event.translatorId) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setQuranTranslatorId(event.translatorId) }
            }

            is SettingsEvent.SetArabicFont -> {
                _quranState.update { it.copy(selectedArabicFontId = event.fontId) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setQuranArabicFont(event.fontId) }
            }

            is SettingsEvent.SetShowTranslation -> {
                _quranState.update { it.copy(showTranslation = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setShowTranslation(event.enabled) }
            }

            is SettingsEvent.SetShowTransliteration -> {
                _quranState.update { it.copy(showTransliteration = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setShowTransliteration(event.enabled) }
            }

            is SettingsEvent.SetArabicFontSize -> {
                _quranState.update { it.copy(arabicFontSize = event.size) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setQuranArabicFontSize(event.size) }
            }

            is SettingsEvent.SetTranslationFontSize -> {
                _quranState.update { it.copy(translationFontSize = event.size) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setQuranTranslationFontSize(event.size) }
            }

            is SettingsEvent.SetContinuousReading -> {
                _quranState.update { it.copy(continuousReading = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setContinuousReading(event.enabled) }
            }

            is SettingsEvent.SetKeepScreenOn -> {
                _quranState.update { it.copy(keepScreenOn = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setKeepScreenOn(event.enabled) }
            }

            is SettingsEvent.SetReciter -> {
                _quranState.update { it.copy(selectedReciterId = event.reciterId) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setSelectedReciterId(event.reciterId) }
            }

            is SettingsEvent.SetShowTajweed -> {
                _quranState.update { it.copy(showTajweed = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setShowTajweed(event.enabled) }
            }

            is SettingsEvent.SetTajweedUnderline -> {
                _quranState.update { it.copy(tajweedUnderline = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setTajweedUnderline(event.enabled) }
            }

            is SettingsEvent.SetMushafScript -> {
                _quranState.update { it.copy(mushafScript = event.script) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setQuranMushafScript(event.script.name) }
            }

            // Dua
            is SettingsEvent.SetDuaArabicFont -> {
                _duaState.update { it.copy(selectedArabicFontId = event.fontId) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setDuaArabicFont(event.fontId) }
            }

            is SettingsEvent.SetDuaArabicFontSize -> {
                _duaState.update { it.copy(arabicFontSize = event.size) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setDuaArabicFontSize(event.size) }
            }

            is SettingsEvent.SetDuaTranslationFontSize -> {
                _duaState.update { it.copy(translationFontSize = event.size) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setDuaTranslationFontSize(event.size) }
            }

            is SettingsEvent.SetDuaShowArabic -> {
                _duaState.update { it.copy(showArabic = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setDuaShowArabic(event.enabled) }
            }

            is SettingsEvent.SetDuaShowTransliteration -> {
                _duaState.update { it.copy(showTransliteration = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setDuaShowTransliteration(event.enabled) }
            }

            is SettingsEvent.SetDuaShowTranslation -> {
                _duaState.update { it.copy(showTranslation = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setDuaShowTranslation(event.enabled) }
            }

            // Hadith
            is SettingsEvent.SetHadithArabicFont -> {
                _hadithState.update { it.copy(selectedArabicFontId = event.fontId) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHadithArabicFont(event.fontId) }
            }

            is SettingsEvent.SetHadithArabicFontSize -> {
                _hadithState.update { it.copy(arabicFontSize = event.size) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHadithArabicFontSize(event.size) }
            }

            is SettingsEvent.SetHadithTranslationFontSize -> {
                _hadithState.update { it.copy(translationFontSize = event.size) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHadithTranslationFontSize(event.size) }
            }

            is SettingsEvent.SetHadithShowArabic -> {
                _hadithState.update { it.copy(showArabic = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHadithShowArabic(event.enabled) }
            }

            is SettingsEvent.SetHadithShowTranslation -> {
                _hadithState.update { it.copy(showTranslation = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHadithShowTranslation(event.enabled) }
            }

            is SettingsEvent.SetHadithShowGrade -> {
                _hadithState.update { it.copy(showGrade = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHadithShowGrade(event.enabled) }
            }

            is SettingsEvent.SetHadithShowChain -> {
                _hadithState.update { it.copy(showChain = event.enabled) }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.SETTINGS,
                    "on_event"
                ) { settingsRepository.setHadithShowChain(event.enabled) }
            }

            // Location
            is SettingsEvent.SetCurrentLocation -> setCurrentLocation(event.location)
            is SettingsEvent.AddLocation -> addLocation(event.location)
            is SettingsEvent.RemoveLocation -> removeLocation(event.location)
            is SettingsEvent.ToggleLocationFavorite -> toggleLocationFavorite(event.locationId)
            // Actions
            SettingsEvent.ResetToDefaults -> resetToDefaults()
            SettingsEvent.TestNotification -> {
                // AppAnalytics.logTestNotification exists for exactly this and was never
                // called, so "did the user try a test notification before reporting that
                // notifications do not work" has been unanswerable.
                telemetry.featureUsed(AppAnalytics.Feature.SETTINGS, "test_notification")
                AppAnalytics.logTestNotification(allPrayers = false)
                prayerNotificationScheduler.sendTestNotification()
            }

            SettingsEvent.TestAllNotifications -> {
                telemetry.featureUsed(AppAnalytics.Feature.SETTINGS, "test_all_notifications")
                AppAnalytics.logTestNotification(allPrayers = true)
                prayerNotificationScheduler.sendAllPrayerTestNotifications()
            }

            SettingsEvent.ResetNotifications -> {
                telemetry.featureUsed(AppAnalytics.Feature.SETTINGS, "reset_notifications")
                launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "on_event") {
                    prayerNotificationScheduler.cancelAllPrayerNotifications()
                    rescheduleNotifications()
                }
            }

            SettingsEvent.DeleteAllData -> deleteAllData()
        }
    }

    private fun loadSettings() {
        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "load_settings") {
            // General settings
            val theme = when (settingsRepository.themeMode.first()) {
                "light" -> AppTheme.LIGHT
                "dark" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            }
            val langCode = settingsRepository.appLanguage.first()
            val language = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.ENGLISH
            val showIslamicPatterns = settingsRepository.showIslamicPatterns.first()
            val patternStyle = NimazPatternStyle.fromKey(settingsRepository.patternStyle.first())
                .let { if (showIslamicPatterns) it else NimazPatternStyle.NONE }
            val animationsEnabled = settingsRepository.animationsEnabled.first()
            val showCountdown = settingsRepository.showCountdown.first()
            val showQuickActions = settingsRepository.showQuickActions.first()
            val hapticFeedback = settingsRepository.hapticFeedback.first()
            val use24Hour = settingsRepository.use24HourFormat.first()
            val useHijri = settingsRepository.useHijriPrimary.first()
            val hijriOffset = settingsRepository.hijriDayOffset.first()

            _generalState.update {
                it.copy(
                    theme = theme,
                    language = language,
                    showIslamicPatterns = showIslamicPatterns,
                    animationsEnabled = animationsEnabled,
                    showCountdown = showCountdown,
                    showQuickActions = showQuickActions,
                    hapticFeedback = hapticFeedback,
                    use24HourFormat = use24Hour,
                    useHijriPrimary = useHijri,
                    hijriDayOffset = hijriOffset,
                    patternStyle = patternStyle
                )
            }

            // Prayer settings
            val calcMethodStr = settingsRepository.calculationMethod.first()
            // `valueOf` inside `catch (_: Exception)`, falling back to MWL. Two things were
            // wrong with that: the exception carried the only evidence that a persisted
            // method could not be read and it was dropped on the floor, and `valueOf` is
            // stricter than what the app persists — `CalculationMethod.fromString` accepts
            // "MWL" and "ISNA" style aliases, so a legitimately stored value could be
            // silently reset to MWL. Prayer times are the app's core output; a settings
            // screen quietly showing a method the user did not choose is not a small bug.
            val calcMethod = CalculationMethod.parseOrNull(calcMethodStr)
                ?: run {
                    telemetry.error(
                        AppAnalytics.Feature.SETTINGS,
                        "unreadable_calculation_method",
                    )
                    CalculationMethod.MUSLIM_WORLD_LEAGUE
                }
            val asrStr = settingsRepository.asrCalculation.first()
            // Routed through the domain parser rather than a hand-written comparison, so a
            // rename of AsrCalculation.HANAFI cannot compile and silently mean STANDARD.
            val asrMethod = AsrCalculation.fromString(asrStr)
            val highLatStr = settingsRepository.highLatitudeRule.first()
            // The domain parser accepts both the old and the new spelling, so a value
            // persisted by the deleted presentation enum still reads back correctly.
            val highLat = HighLatitudeRule.fromString(highLatStr)
                ?: run {
                    // Same rule as the calculation method above: fall back, but say so.
                    telemetry.error(
                        AppAnalytics.Feature.SETTINGS,
                        "unreadable_high_latitude_rule",
                    )
                    HighLatitudeRule.MIDDLE_OF_THE_NIGHT
                }

            val fajrAdj = settingsRepository.fajrAdjustment.first()
            val sunriseAdj = settingsRepository.sunriseAdjustment.first()
            val dhuhrAdj = settingsRepository.dhuhrAdjustment.first()
            val asrAdj = settingsRepository.asrAdjustment.first()
            val maghribAdj = settingsRepository.maghribAdjustment.first()
            val ishaAdj = settingsRepository.ishaAdjustment.first()

            _prayerState.update {
                it.copy(
                    calculationMethod = calcMethod,
                    asrMethod = asrMethod,
                    highLatitudeRule = highLat,
                    fajrAdjustment = fajrAdj,
                    sunriseAdjustment = sunriseAdj,
                    dhuhrAdjustment = dhuhrAdj,
                    asrAdjustment = asrAdj,
                    maghribAdjustment = maghribAdj,
                    ishaAdjustment = ishaAdj
                )
            }

            // Notification settings
            val notifEnabled = settingsRepository.prayerNotificationsEnabled.first()
            val adhanEnabled = settingsRepository.adhanEnabled.first()
            val vibration = settingsRepository.notificationVibration.first()
            val reminderMin = settingsRepository.notificationReminderMinutes.first()
            val showReminder = settingsRepository.showReminderBefore.first()
            val persistent = settingsRepository.persistentNotification.first()
            val fridayReminder = settingsRepository.fridayReminderEnabled.first()
            val fridayReminderMin = settingsRepository.fridayReminderMinutes.first()
            val khatamReminder = settingsRepository.khatamReminderEnabled.first()
            val khatamReminderAt = settingsRepository.khatamReminderTime.first()
            val respectDnd = settingsRepository.adhanRespectDnd.first()
            val adhanSoundName = settingsRepository.selectedAdhanSound.first()
            val fajrNotif = settingsRepository.fajrNotificationEnabled.first()
            val sunriseNotif = settingsRepository.sunriseNotificationEnabled.first()
            val dhuhrNotif = settingsRepository.dhuhrNotificationEnabled.first()
            val asrNotif = settingsRepository.asrNotificationEnabled.first()
            val maghribNotif = settingsRepository.maghribNotificationEnabled.first()
            val ishaNotif = settingsRepository.ishaNotificationEnabled.first()

            // Per-prayer alert style and reminder. The migration in AppInitializer has
            // already carried an existing install onto these, so they are the truth here.
            val alertStyles = PrayerAlertStyle.PRAYER_KEYS
                .associateWith { settingsRepository.prayerAlertStyle(it).first() }
            val reminderEnabled = PrayerAlertStyle.PRAYER_KEYS
                .associateWith { settingsRepository.prayerReminderEnabled(it).first() }
            val reminderOffsets = PrayerAlertStyle.PRAYER_KEYS
                .associateWith { settingsRepository.prayerReminderMinutes(it).first() }

            // Extended worship reminders — enabled flags + offsets keyed by type.
            val worshipEnabled = com.arshadshah.nimaz.domain.model.WorshipReminderType.entries
                .associate { it.key to settingsRepository.worshipReminderEnabled(it.key).first() }
            val worshipOffsets = com.arshadshah.nimaz.domain.model.WorshipReminderType.entries
                .associate {
                    it.key to settingsRepository.worshipReminderOffset(
                        it.key,
                        it.defaultOffsetMinutes
                    ).first()
                }
            val witrModeDefault =
                com.arshadshah.nimaz.core.util.WorshipReminderCalculator.WITR_MODE_AFTER_ISHA
            val worshipModes = mapOf(
                com.arshadshah.nimaz.domain.model.WorshipReminderType.WITR.key to
                        settingsRepository.worshipReminderMode(
                            com.arshadshah.nimaz.domain.model.WorshipReminderType.WITR.key,
                            witrModeDefault
                        ).first()
            )

            _notificationState.update {
                it.copy(
                    notificationsEnabled = notifEnabled,
                    adhanEnabled = adhanEnabled,
                    alertStyles = alertStyles,
                    reminderEnabled = reminderEnabled,
                    reminderOffsets = reminderOffsets,
                    vibrationEnabled = vibration,
                    reminderMinutes = reminderMin,
                    showReminderBefore = showReminder,
                    persistentNotification = persistent,
                    fridayReminderEnabled = fridayReminder,
                    fridayReminderMinutes = fridayReminderMin,
                    khatamReminderEnabled = khatamReminder,
                    khatamReminderTime = khatamReminderAt,
                    respectDnd = respectDnd,
                    selectedAdhanSound = adhanSoundName,
                    fajrNotification = fajrNotif,
                    sunriseNotification = sunriseNotif,
                    dhuhrNotification = dhuhrNotif,
                    asrNotification = asrNotif,
                    maghribNotification = maghribNotif,
                    ishaNotification = ishaNotif,
                    worshipReminders = worshipEnabled,
                    worshipOffsets = worshipOffsets,
                    worshipModes = worshipModes
                )
            }

            // Quran settings are not read here: [observeQuranSettings] collects them for the
            // lifetime of the ViewModel, so a change made on a picker screen (which runs its
            // own instance) is visible on the settings screen behind it.

            // Dua settings
            _duaState.update {
                it.copy(
                    selectedArabicFontId = settingsRepository.duaArabicFont.first(),
                    arabicFontSize = settingsRepository.duaArabicFontSize.first(),
                    translationFontSize = settingsRepository.duaTranslationFontSize.first(),
                    showArabic = settingsRepository.duaShowArabic.first(),
                    showTransliteration = settingsRepository.duaShowTransliteration.first(),
                    showTranslation = settingsRepository.duaShowTranslation.first()
                )
            }

            // Hadith settings
            _hadithState.update {
                it.copy(
                    selectedArabicFontId = settingsRepository.hadithArabicFont.first(),
                    arabicFontSize = settingsRepository.hadithArabicFontSize.first(),
                    translationFontSize = settingsRepository.hadithTranslationFontSize.first(),
                    showArabic = settingsRepository.hadithShowArabic.first(),
                    showTranslation = settingsRepository.hadithShowTranslation.first(),
                    showGrade = settingsRepository.hadithShowGrade.first(),
                    showChain = settingsRepository.hadithShowChain.first()
                )
            }
        }
    }

    private fun loadLocations() {
        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "load_locations") {
            prayerUseCases.getCurrentLocation().collect { location ->
                _locationState.update { it.copy(currentLocation = location) }
            }
        }

        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "load_locations") {
            prayerUseCases.getAllLocations().collect { locations ->
                _locationState.update { it.copy(savedLocations = locations, isLoading = false) }
            }
        }

        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "load_locations") {
            prayerUseCases.getFavoriteLocations().collect { favorites ->
                _locationState.update { it.copy(favoriteLocations = favorites) }
            }
        }
    }

    /**
     * Delegates to [RescheduleNotificationsUseCase], which reads the persisted values.
     *
     * This used to build the alarm set from `_notificationState.value` and `_prayerState.value`
     * — construction-time snapshots. Since `hiltViewModel()` gives each settings screen its own
     * instance, one screen's snapshot went stale as soon as another wrote, and a prayer the
     * user had switched off in Notification Settings was re-armed by an unrelated change in
     * Prayer Settings. See the use case's KDoc.
     */
    private suspend fun rescheduleNotifications() {
        rescheduleNotificationsUseCase()
    }

    private fun updatePrayerAdjustment(prayer: String, minutes: Int) {
        _prayerState.update { state ->
            when (prayer.lowercase()) {
                "fajr" -> state.copy(fajrAdjustment = minutes)
                "sunrise" -> state.copy(sunriseAdjustment = minutes)
                "dhuhr" -> state.copy(dhuhrAdjustment = minutes)
                "asr" -> state.copy(asrAdjustment = minutes)
                "maghrib" -> state.copy(maghribAdjustment = minutes)
                "isha" -> state.copy(ishaAdjustment = minutes)
                else -> state
            }
        }
    }

    private fun updatePrayerNotification(prayer: String, enabled: Boolean) {
        _notificationState.update { state ->
            when (prayer.lowercase()) {
                "fajr" -> state.copy(fajrNotification = enabled)
                "sunrise" -> state.copy(sunriseNotification = enabled)
                "dhuhr" -> state.copy(dhuhrNotification = enabled)
                "asr" -> state.copy(asrNotification = enabled)
                "maghrib" -> state.copy(maghribNotification = enabled)
                "isha" -> state.copy(ishaNotification = enabled)
                else -> state
            }
        }
    }

    private fun setCurrentLocation(location: Location) {
        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "set_current_location") {
            val id = prayerUseCases.insertLocation(location)
            prayerUseCases.setCurrentLocation(id)
        }
    }

    private fun addLocation(location: Location) {
        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "add_location") {
            prayerUseCases.insertLocation(location)
        }
    }

    private fun removeLocation(location: Location) {
        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "remove_location") {
            prayerUseCases.deleteLocation(location)
        }
    }

    private fun toggleLocationFavorite(locationId: Long) {
        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "toggle_location_favorite") {
            prayerUseCases.toggleFavorite(locationId)
        }
    }

    /**
     * The three destructive actions were the least instrumented in the app, which is exactly
     * backwards: a wipe is the event you most want to see in a funnel, because it is either
     * someone tidying up or someone about to uninstall, and the two look nothing alike in
     * context. Logged before the work, so an action that fails part-way is still visible.
     */
    private fun resetToDefaults() {
        telemetry.featureUsed(AppAnalytics.Feature.SETTINGS, "reset_to_defaults")
        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "reset_to_defaults") {
            settingsRepository.clearAllData()
            resetAllUiState()
            _shouldRestart.value = true
        }
    }

    /**
     * Every settings surface back to its defaults, in one place.
     *
     * The two reset paths disagreed about what "reset everything" means: `resetToDefaults`
     * cleared general, prayer, notification and Quran; `deleteAllData` cleared those plus
     * location; **neither** cleared Dua or Hadith. So after "reset to defaults" the Dua and
     * Hadith settings screens went on showing pre-reset font sizes. `_shouldRestart` papered
     * over it — the app restarts and reloads — but two functions that disagree about the same
     * word are a trap for the next person to add a state holder to only one of them.
     */
    private fun resetAllUiState() {
        _generalState.update { GeneralSettingsUiState() }
        _prayerState.update { PrayerSettingsUiState() }
        _notificationState.update { NotificationSettingsUiState() }
        _quranState.update { QuranSettingsUiState() }
        _duaState.update { DuaSettingsUiState() }
        _hadithState.update { HadithSettingsUiState() }
        _locationState.update { LocationSettingsUiState() }
    }

    private fun deleteAllData() {
        telemetry.featureUsed(AppAnalytics.Feature.SETTINGS, "delete_all_data")
        launchSafely(telemetry, AppAnalytics.Feature.SETTINGS, "delete_all_data") {
            // Everything the person made lives in the user database now, so "delete all my
            // data" clears that one — and it is a much more honest operation for it: the
            // content database is not touched at all, so there is no way for this to reach
            // the corpus, and no way for it to miss a table by forgetting a DAO.
            clearAllUserData()

            // The content database is deliberately not touched. Content is not user data: the
            // corpus, and the presets we ship, are ours to replace and never theirs to lose.

            // Clear DataStore preferences
            settingsRepository.clearAllData()

            // Reset UI state to defaults
            resetAllUiState()
            _shouldRestart.value = true
        }
    }

    /** Grouping types for [observeQuranSettings] — `combine` takes at most five typed flows. */
    private data class QuranContentPrefs(
        val translatorId: String,
        val arabicFontId: String,
        val reciterId: String?,
        val mushafScript: MushafScript
    )

    private data class QuranDisplayPrefs(
        val showTranslation: Boolean,
        val showTransliteration: Boolean,
        val arabicFontSize: Float,
        val translationFontSize: Float
    )

    private data class QuranBehaviourPrefs(
        val continuousReading: Boolean,
        val keepScreenOn: Boolean,
        val showTajweed: Boolean,
        val tajweedUnderline: Boolean
    )

    private companion object {
        /**
         * Global ayah id of the verse the Quran settings preview card renders — Al-Fatihah
         * 1:1, the Bismillah. Kept next to the loader so the preview's Arabic and its
         * translation can never drift apart.
         */
        const val PREVIEW_AYAH_ID = 1
    }
}
