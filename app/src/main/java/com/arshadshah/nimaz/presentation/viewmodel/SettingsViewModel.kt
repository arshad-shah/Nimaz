package com.arshadshah.nimaz.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.LocaleHelper
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

enum class AsrJuristicMethod {
    STANDARD, // Shafi'i, Maliki, Hanbali
    HANAFI
}

enum class HighLatitudeRule {
    MIDDLE_OF_NIGHT,
    SEVENTH_OF_NIGHT,
    TWILIGHT_ANGLE
}

data class GeneralSettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val useHijriPrimary: Boolean = false,
    val use24HourFormat: Boolean = false,
    val showSeconds: Boolean = false,
    val hapticFeedback: Boolean = true,
    val showIslamicPatterns: Boolean = true,
    val animationsEnabled: Boolean = true,
    val showCountdown: Boolean = true,
    val showQuickActions: Boolean = true
)

data class PrayerSettingsUiState(
    val calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val asrMethod: AsrJuristicMethod = AsrJuristicMethod.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT,
    val fajrAngle: Double = 18.0,
    val ishaAngle: Double = 17.0,
    val fajrAdjustment: Int = 0,
    val sunriseAdjustment: Int = 0,
    val dhuhrAdjustment: Int = 0,
    val asrAdjustment: Int = 0,
    val maghribAdjustment: Int = 0,
    val ishaAdjustment: Int = 0
)

data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val fajrNotification: Boolean = true,
    val sunriseNotification: Boolean = false,
    val dhuhrNotification: Boolean = true,
    val asrNotification: Boolean = true,
    val maghribNotification: Boolean = true,
    val ishaNotification: Boolean = true,
    val adhanEnabled: Boolean = false,
    // Per-prayer adhan settings
    val fajrAdhanEnabled: Boolean = true,
    val dhuhrAdhanEnabled: Boolean = true,
    val asrAdhanEnabled: Boolean = true,
    val maghribAdhanEnabled: Boolean = true,
    val ishaAdhanEnabled: Boolean = true,
    // Sunrise always uses beep only (no toggle needed)
    val vibrationEnabled: Boolean = true,
    val respectDnd: Boolean = true,
    val reminderMinutes: Int = 15,
    val showReminderBefore: Boolean = true,
    val persistentNotification: Boolean = false,
    val fridayReminderEnabled: Boolean = false,
    val fridayReminderMinutes: Int = 60,
    val selectedAdhanSound: String = "MISHARY"
)

data class QuranSettingsUiState(
    val selectedTranslatorId: String = "sahih_international",
    val selectedArabicFontId: String = "amiri",
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val arabicFontSize: Float = 28f,
    val translationFontSize: Float = 16f,
    val continuousReading: Boolean = true,
    val keepScreenOn: Boolean = true,
    val selectedReciterId: String? = null,
    val showTajweed: Boolean = false
)

data class DuaSettingsUiState(
    val selectedArabicFontId: String = "amiri",
    val arabicFontSize: Float = 28f,
    val translationFontSize: Float = 16f,
    val showArabic: Boolean = true,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true
)

data class HadithSettingsUiState(
    val selectedArabicFontId: String = "amiri",
    val arabicFontSize: Float = 24f,
    val translationFontSize: Float = 16f,
    val showArabic: Boolean = true,
    val showTranslation: Boolean = true,
    val showGrade: Boolean = true,
    val showChain: Boolean = true
)

data class LocationSettingsUiState(
    val currentLocation: Location? = null,
    val savedLocations: List<Location> = emptyList(),
    val favoriteLocations: List<Location> = emptyList(),
    val autoDetectLocation: Boolean = true,
    val isLoading: Boolean = true
)

data class WidgetSettingsUiState(
    val prayerTimesWidgetEnabled: Boolean = true,
    val widgetTheme: AppTheme = AppTheme.SYSTEM,
    val showNextPrayerCountdown: Boolean = true,
    val widgetTransparency: Float = 1f
)

sealed interface SettingsEvent {
    // General
    data class SetTheme(val theme: AppTheme) : SettingsEvent
    data class SetLanguage(val language: AppLanguage) : SettingsEvent
    data class SetHijriPrimary(val enabled: Boolean) : SettingsEvent
    data class Set24HourFormat(val enabled: Boolean) : SettingsEvent
    data class SetShowSeconds(val enabled: Boolean) : SettingsEvent
    data class SetHapticFeedback(val enabled: Boolean) : SettingsEvent
    data class SetShowIslamicPatterns(val enabled: Boolean) : SettingsEvent
    data class SetAnimationsEnabled(val enabled: Boolean) : SettingsEvent
    data class SetShowCountdown(val enabled: Boolean) : SettingsEvent
    data class SetShowQuickActions(val enabled: Boolean) : SettingsEvent

    // Prayer
    data class SetCalculationMethod(val method: CalculationMethod) : SettingsEvent
    data class SetAsrMethod(val method: AsrJuristicMethod) : SettingsEvent
    data class SetHighLatitudeRule(val rule: HighLatitudeRule) : SettingsEvent
    data class SetFajrAngle(val angle: Double) : SettingsEvent
    data class SetIshaAngle(val angle: Double) : SettingsEvent
    data class SetPrayerAdjustment(val prayer: String, val minutes: Int) : SettingsEvent

    // Notifications
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsEvent
    data class SetPrayerNotification(val prayer: String, val enabled: Boolean) : SettingsEvent
    data class SetAdhanEnabled(val enabled: Boolean) : SettingsEvent
    data class SetPrayerAdhanEnabled(val prayer: String, val enabled: Boolean) : SettingsEvent
    data class SetVibrationEnabled(val enabled: Boolean) : SettingsEvent
    data class SetRespectDnd(val enabled: Boolean) : SettingsEvent
    data class SetReminderMinutes(val minutes: Int) : SettingsEvent
    data class SetShowReminderBefore(val enabled: Boolean) : SettingsEvent
    data class SetPersistentNotification(val enabled: Boolean) : SettingsEvent
    data class SetFridayReminderEnabled(val enabled: Boolean) : SettingsEvent
    data class SetFridayReminderMinutes(val minutes: Int) : SettingsEvent
    data class SetAdhanSound(val sound: String) : SettingsEvent
    data object PreviewAdhanSound : SettingsEvent
    data object StopAdhanPreview : SettingsEvent

    // Quran
    data class SetTranslator(val translatorId: String) : SettingsEvent
    data class SetArabicFont(val fontId: String) : SettingsEvent
    data class SetShowTranslation(val enabled: Boolean) : SettingsEvent
    data class SetShowTransliteration(val enabled: Boolean) : SettingsEvent
    data class SetArabicFontSize(val size: Float) : SettingsEvent
    data class SetTranslationFontSize(val size: Float) : SettingsEvent
    data class SetContinuousReading(val enabled: Boolean) : SettingsEvent
    data class SetKeepScreenOn(val enabled: Boolean) : SettingsEvent
    data class SetReciter(val reciterId: String?) : SettingsEvent
    data class SetShowTajweed(val enabled: Boolean) : SettingsEvent

    // Dua
    data class SetDuaArabicFont(val fontId: String) : SettingsEvent
    data class SetDuaArabicFontSize(val size: Float) : SettingsEvent
    data class SetDuaTranslationFontSize(val size: Float) : SettingsEvent
    data class SetDuaShowArabic(val enabled: Boolean) : SettingsEvent
    data class SetDuaShowTransliteration(val enabled: Boolean) : SettingsEvent
    data class SetDuaShowTranslation(val enabled: Boolean) : SettingsEvent

    // Hadith
    data class SetHadithArabicFont(val fontId: String) : SettingsEvent
    data class SetHadithArabicFontSize(val size: Float) : SettingsEvent
    data class SetHadithTranslationFontSize(val size: Float) : SettingsEvent
    data class SetHadithShowArabic(val enabled: Boolean) : SettingsEvent
    data class SetHadithShowTranslation(val enabled: Boolean) : SettingsEvent
    data class SetHadithShowGrade(val enabled: Boolean) : SettingsEvent
    data class SetHadithShowChain(val enabled: Boolean) : SettingsEvent

    // Location
    data class SetCurrentLocation(val location: Location) : SettingsEvent
    data class AddLocation(val location: Location) : SettingsEvent
    data class RemoveLocation(val location: Location) : SettingsEvent
    data class ToggleLocationFavorite(val locationId: Long) : SettingsEvent
    data class SetAutoDetectLocation(val enabled: Boolean) : SettingsEvent

    // Widget
    data class SetPrayerTimesWidgetEnabled(val enabled: Boolean) : SettingsEvent
    data class SetWidgetTheme(val theme: AppTheme) : SettingsEvent
    data class SetShowNextPrayerCountdown(val enabled: Boolean) : SettingsEvent
    data class SetWidgetTransparency(val transparency: Float) : SettingsEvent

    // Actions
    data object LoadSettings : SettingsEvent
    data object ResetToDefaults : SettingsEvent
    data object ExportSettings : SettingsEvent
    data object ImportSettings : SettingsEvent
    data object TestNotification : SettingsEvent
    data object TestAllNotifications : SettingsEvent
    data object ResetNotifications : SettingsEvent
    data object DeleteAllData : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerUseCases: PrayerUseCases,
    private val settingsRepository: SettingsRepository,
    private val prayerNotificationScheduler: PrayerNotificationScheduler,
    val adhanAudioManager: AdhanAudioManager,
    private val database: NimazDatabase
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

    private val _widgetState = MutableStateFlow(WidgetSettingsUiState())
    val widgetState: StateFlow<WidgetSettingsUiState> = _widgetState.asStateFlow()

    private val _shouldRestart = MutableStateFlow(false)
    val shouldRestart: StateFlow<Boolean> = _shouldRestart.asStateFlow()

    private val _adhanPreviewError = MutableStateFlow<String?>(null)
    val adhanPreviewError: StateFlow<String?> = _adhanPreviewError.asStateFlow()

    fun clearAdhanPreviewError() {
        _adhanPreviewError.value = null
    }

    init {
        loadSettings()
        loadLocations()
    }

    fun onEvent(event: SettingsEvent) {
        // Record meaningful configuration changes. Notification and calculation
        // settings are the ones most often behind "it's doing the wrong thing".
        when (event) {
            is SettingsEvent.SetTheme -> AppAnalytics.logSettingChanged("theme", event.theme.name)
            is SettingsEvent.SetLanguage -> AppAnalytics.logSettingChanged(
                "language",
                event.language.name
            )

            is SettingsEvent.SetCalculationMethod -> AppAnalytics.logSettingChanged(
                "calculation_method",
                event.method.name
            )

            is SettingsEvent.SetAsrMethod -> AppAnalytics.logSettingChanged(
                "asr_method",
                event.method.name
            )

            is SettingsEvent.SetHighLatitudeRule -> AppAnalytics.logSettingChanged(
                "high_latitude_rule",
                event.rule.name
            )

            is SettingsEvent.SetNotificationsEnabled -> AppAnalytics.logSettingChanged(
                "notifications_enabled",
                event.enabled.toString()
            )

            is SettingsEvent.SetPrayerNotification -> AppAnalytics.logSettingChanged(
                "prayer_notification_${event.prayer.lowercase()}",
                event.enabled.toString()
            )

            is SettingsEvent.SetAdhanEnabled -> AppAnalytics.logSettingChanged(
                "adhan_enabled",
                event.enabled.toString()
            )

            is SettingsEvent.SetPrayerAdhanEnabled -> AppAnalytics.logSettingChanged(
                "adhan_${event.prayer.lowercase()}",
                event.enabled.toString()
            )

            is SettingsEvent.SetAdhanSound -> AppAnalytics.logSettingChanged(
                "adhan_sound",
                event.sound
            )

            is SettingsEvent.SetRespectDnd -> AppAnalytics.logSettingChanged(
                "respect_dnd",
                event.enabled.toString()
            )

            is SettingsEvent.SetShowReminderBefore -> AppAnalytics.logSettingChanged(
                "pre_reminder_enabled",
                event.enabled.toString()
            )

            is SettingsEvent.SetReminderMinutes -> AppAnalytics.logSettingChanged(
                "reminder_minutes",
                event.minutes.toString()
            )

            else -> {}
        }
        when (event) {
            // General
            is SettingsEvent.SetTheme -> {
                _generalState.update { it.copy(theme = event.theme) }
                viewModelScope.launch {
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
                viewModelScope.launch {
                    settingsRepository.setAppLanguage(event.language.code)
                    LocaleHelper.setLocale(context, event.language.code)
                    _shouldRestart.value = true
                }
            }

            is SettingsEvent.SetHijriPrimary -> {
                _generalState.update { it.copy(useHijriPrimary = event.enabled) }
                viewModelScope.launch { settingsRepository.setUseHijriPrimary(event.enabled) }
            }

            is SettingsEvent.Set24HourFormat -> {
                _generalState.update { it.copy(use24HourFormat = event.enabled) }
                viewModelScope.launch { settingsRepository.setUse24HourFormat(event.enabled) }
            }

            is SettingsEvent.SetShowSeconds -> _generalState.update { it.copy(showSeconds = event.enabled) }
            is SettingsEvent.SetHapticFeedback -> {
                _generalState.update { it.copy(hapticFeedback = event.enabled) }
                viewModelScope.launch { settingsRepository.setHapticFeedback(event.enabled) }
            }

            is SettingsEvent.SetShowIslamicPatterns -> {
                _generalState.update { it.copy(showIslamicPatterns = event.enabled) }
                viewModelScope.launch { settingsRepository.setShowIslamicPatterns(event.enabled) }
            }

            is SettingsEvent.SetAnimationsEnabled -> {
                _generalState.update { it.copy(animationsEnabled = event.enabled) }
                viewModelScope.launch { settingsRepository.setAnimationsEnabled(event.enabled) }
            }

            is SettingsEvent.SetShowCountdown -> {
                _generalState.update { it.copy(showCountdown = event.enabled) }
                viewModelScope.launch { settingsRepository.setShowCountdown(event.enabled) }
            }

            is SettingsEvent.SetShowQuickActions -> {
                _generalState.update { it.copy(showQuickActions = event.enabled) }
                viewModelScope.launch { settingsRepository.setShowQuickActions(event.enabled) }
            }

            // Prayer
            is SettingsEvent.SetCalculationMethod -> {
                _prayerState.update { it.copy(calculationMethod = event.method) }
                viewModelScope.launch {
                    settingsRepository.setCalculationMethod(event.method.name)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetAsrMethod -> {
                _prayerState.update { it.copy(asrMethod = event.method) }
                viewModelScope.launch {
                    settingsRepository.setAsrCalculation(event.method.name.lowercase())
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetHighLatitudeRule -> {
                _prayerState.update { it.copy(highLatitudeRule = event.rule) }
                viewModelScope.launch {
                    settingsRepository.setHighLatitudeRule(event.rule.name)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetFajrAngle -> {
                _prayerState.update { it.copy(fajrAngle = event.angle) }
                viewModelScope.launch { rescheduleNotifications() }
            }

            is SettingsEvent.SetIshaAngle -> {
                _prayerState.update { it.copy(ishaAngle = event.angle) }
                viewModelScope.launch { rescheduleNotifications() }
            }

            is SettingsEvent.SetPrayerAdjustment -> {
                updatePrayerAdjustment(event.prayer, event.minutes)
                viewModelScope.launch {
                    settingsRepository.setPrayerAdjustment(event.prayer, event.minutes)
                    rescheduleNotifications()
                }
            }

            // Notifications
            is SettingsEvent.SetNotificationsEnabled -> {
                _notificationState.update { it.copy(notificationsEnabled = event.enabled) }
                viewModelScope.launch {
                    settingsRepository.setPrayerNotificationsEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetPrayerNotification -> {
                updatePrayerNotification(event.prayer, event.enabled)
                viewModelScope.launch {
                    settingsRepository.setPrayerNotificationEnabled(event.prayer, event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetAdhanEnabled -> {
                _notificationState.update { it.copy(adhanEnabled = event.enabled) }
                viewModelScope.launch {
                    settingsRepository.setAdhanEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetPrayerAdhanEnabled -> {
                updatePrayerAdhanEnabled(event.prayer, event.enabled)
                viewModelScope.launch {
                    settingsRepository.setPrayerAdhanEnabled(event.prayer, event.enabled)
                }
            }

            is SettingsEvent.SetVibrationEnabled -> {
                _notificationState.update { it.copy(vibrationEnabled = event.enabled) }
                viewModelScope.launch { settingsRepository.setNotificationVibration(event.enabled) }
            }

            is SettingsEvent.SetRespectDnd -> {
                _notificationState.update { it.copy(respectDnd = event.enabled) }
                viewModelScope.launch { settingsRepository.setAdhanRespectDnd(event.enabled) }
            }

            is SettingsEvent.SetReminderMinutes -> {
                _notificationState.update { it.copy(reminderMinutes = event.minutes) }
                viewModelScope.launch {
                    settingsRepository.setNotificationReminderMinutes(event.minutes)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetShowReminderBefore -> {
                _notificationState.update { it.copy(showReminderBefore = event.enabled) }
                viewModelScope.launch {
                    settingsRepository.setShowReminderBefore(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetPersistentNotification -> {
                _notificationState.update { it.copy(persistentNotification = event.enabled) }
                viewModelScope.launch { settingsRepository.setPersistentNotification(event.enabled) }
            }

            is SettingsEvent.SetFridayReminderEnabled -> {
                _notificationState.update { it.copy(fridayReminderEnabled = event.enabled) }
                viewModelScope.launch {
                    settingsRepository.setFridayReminderEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetFridayReminderMinutes -> {
                _notificationState.update { it.copy(fridayReminderMinutes = event.minutes) }
                viewModelScope.launch {
                    settingsRepository.setFridayReminderMinutes(event.minutes)
                    rescheduleNotifications()
                }
            }

            is SettingsEvent.SetAdhanSound -> {
                _notificationState.update { it.copy(selectedAdhanSound = event.sound) }
                viewModelScope.launch {
                    settingsRepository.setSelectedAdhanSound(event.sound)
                    // Download the selected adhan if not already downloaded
                    val sound = AdhanSound.fromName(event.sound)
                    if (!adhanAudioManager.isFullyDownloaded(sound)) {
                        AdhanDownloadService.downloadSelected(context, sound)
                    }
                }
            }

            SettingsEvent.PreviewAdhanSound -> {
                val sound = AdhanSound.fromName(_notificationState.value.selectedAdhanSound)
                viewModelScope.launch {
                    try {
                        // Ensure both variants are downloaded
                        if (!adhanAudioManager.isFullyDownloaded(sound)) {
                            val success = adhanAudioManager.downloadAdhanWithFajr(sound)
                            if (!success) {
                                _adhanPreviewError.value =
                                    "Failed to download adhan audio. Please check your internet connection."
                                return@launch
                            }
                        }
                        // Now play the preview
                        adhanAudioManager.preview(sound, false)
                    } catch (e: Exception) {
                        CrashReporter.recordException(e)
                        AppAnalytics.logError("settings", "adhan_preview", e.message)
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
                viewModelScope.launch { settingsRepository.setQuranTranslatorId(event.translatorId) }
            }

            is SettingsEvent.SetArabicFont -> {
                _quranState.update { it.copy(selectedArabicFontId = event.fontId) }
                viewModelScope.launch { settingsRepository.setQuranArabicFont(event.fontId) }
            }

            is SettingsEvent.SetShowTranslation -> {
                _quranState.update { it.copy(showTranslation = event.enabled) }
                viewModelScope.launch { settingsRepository.setShowTranslation(event.enabled) }
            }

            is SettingsEvent.SetShowTransliteration -> {
                _quranState.update { it.copy(showTransliteration = event.enabled) }
                viewModelScope.launch { settingsRepository.setShowTransliteration(event.enabled) }
            }

            is SettingsEvent.SetArabicFontSize -> {
                _quranState.update { it.copy(arabicFontSize = event.size) }
                viewModelScope.launch { settingsRepository.setQuranArabicFontSize(event.size) }
            }

            is SettingsEvent.SetTranslationFontSize -> {
                _quranState.update { it.copy(translationFontSize = event.size) }
                viewModelScope.launch { settingsRepository.setQuranTranslationFontSize(event.size) }
            }

            is SettingsEvent.SetContinuousReading -> {
                _quranState.update { it.copy(continuousReading = event.enabled) }
                viewModelScope.launch { settingsRepository.setContinuousReading(event.enabled) }
            }

            is SettingsEvent.SetKeepScreenOn -> {
                _quranState.update { it.copy(keepScreenOn = event.enabled) }
                viewModelScope.launch { settingsRepository.setKeepScreenOn(event.enabled) }
            }

            is SettingsEvent.SetReciter -> {
                _quranState.update { it.copy(selectedReciterId = event.reciterId) }
                viewModelScope.launch { settingsRepository.setSelectedReciterId(event.reciterId) }
            }

            is SettingsEvent.SetShowTajweed -> {
                _quranState.update { it.copy(showTajweed = event.enabled) }
                viewModelScope.launch { settingsRepository.setShowTajweed(event.enabled) }
            }

            // Dua
            is SettingsEvent.SetDuaArabicFont -> {
                _duaState.update { it.copy(selectedArabicFontId = event.fontId) }
                viewModelScope.launch { settingsRepository.setDuaArabicFont(event.fontId) }
            }

            is SettingsEvent.SetDuaArabicFontSize -> {
                _duaState.update { it.copy(arabicFontSize = event.size) }
                viewModelScope.launch { settingsRepository.setDuaArabicFontSize(event.size) }
            }

            is SettingsEvent.SetDuaTranslationFontSize -> {
                _duaState.update { it.copy(translationFontSize = event.size) }
                viewModelScope.launch { settingsRepository.setDuaTranslationFontSize(event.size) }
            }

            is SettingsEvent.SetDuaShowArabic -> {
                _duaState.update { it.copy(showArabic = event.enabled) }
                viewModelScope.launch { settingsRepository.setDuaShowArabic(event.enabled) }
            }

            is SettingsEvent.SetDuaShowTransliteration -> {
                _duaState.update { it.copy(showTransliteration = event.enabled) }
                viewModelScope.launch { settingsRepository.setDuaShowTransliteration(event.enabled) }
            }

            is SettingsEvent.SetDuaShowTranslation -> {
                _duaState.update { it.copy(showTranslation = event.enabled) }
                viewModelScope.launch { settingsRepository.setDuaShowTranslation(event.enabled) }
            }

            // Hadith
            is SettingsEvent.SetHadithArabicFont -> {
                _hadithState.update { it.copy(selectedArabicFontId = event.fontId) }
                viewModelScope.launch { settingsRepository.setHadithArabicFont(event.fontId) }
            }

            is SettingsEvent.SetHadithArabicFontSize -> {
                _hadithState.update { it.copy(arabicFontSize = event.size) }
                viewModelScope.launch { settingsRepository.setHadithArabicFontSize(event.size) }
            }

            is SettingsEvent.SetHadithTranslationFontSize -> {
                _hadithState.update { it.copy(translationFontSize = event.size) }
                viewModelScope.launch { settingsRepository.setHadithTranslationFontSize(event.size) }
            }

            is SettingsEvent.SetHadithShowArabic -> {
                _hadithState.update { it.copy(showArabic = event.enabled) }
                viewModelScope.launch { settingsRepository.setHadithShowArabic(event.enabled) }
            }

            is SettingsEvent.SetHadithShowTranslation -> {
                _hadithState.update { it.copy(showTranslation = event.enabled) }
                viewModelScope.launch { settingsRepository.setHadithShowTranslation(event.enabled) }
            }

            is SettingsEvent.SetHadithShowGrade -> {
                _hadithState.update { it.copy(showGrade = event.enabled) }
                viewModelScope.launch { settingsRepository.setHadithShowGrade(event.enabled) }
            }

            is SettingsEvent.SetHadithShowChain -> {
                _hadithState.update { it.copy(showChain = event.enabled) }
                viewModelScope.launch { settingsRepository.setHadithShowChain(event.enabled) }
            }

            // Location
            is SettingsEvent.SetCurrentLocation -> setCurrentLocation(event.location)
            is SettingsEvent.AddLocation -> addLocation(event.location)
            is SettingsEvent.RemoveLocation -> removeLocation(event.location)
            is SettingsEvent.ToggleLocationFavorite -> toggleLocationFavorite(event.locationId)
            is SettingsEvent.SetAutoDetectLocation -> _locationState.update {
                it.copy(
                    autoDetectLocation = event.enabled
                )
            }

            // Widget
            is SettingsEvent.SetPrayerTimesWidgetEnabled -> _widgetState.update {
                it.copy(
                    prayerTimesWidgetEnabled = event.enabled
                )
            }

            is SettingsEvent.SetWidgetTheme -> _widgetState.update { it.copy(widgetTheme = event.theme) }
            is SettingsEvent.SetShowNextPrayerCountdown -> _widgetState.update {
                it.copy(
                    showNextPrayerCountdown = event.enabled
                )
            }

            is SettingsEvent.SetWidgetTransparency -> _widgetState.update {
                it.copy(
                    widgetTransparency = event.transparency
                )
            }

            // Actions
            SettingsEvent.LoadSettings -> loadSettings()
            SettingsEvent.ResetToDefaults -> resetToDefaults()
            SettingsEvent.ExportSettings -> exportSettings()
            SettingsEvent.ImportSettings -> importSettings()
            SettingsEvent.TestNotification -> {
                prayerNotificationScheduler.sendTestNotification()
            }

            SettingsEvent.TestAllNotifications -> {
                prayerNotificationScheduler.sendAllPrayerTestNotifications()
            }

            SettingsEvent.ResetNotifications -> {
                viewModelScope.launch {
                    prayerNotificationScheduler.cancelAllPrayerNotifications()
                    rescheduleNotifications()
                }
            }

            SettingsEvent.DeleteAllData -> deleteAllData()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // General settings
            val theme = when (settingsRepository.themeMode.first()) {
                "light" -> AppTheme.LIGHT
                "dark" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            }
            val langCode = settingsRepository.appLanguage.first()
            val language = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.ENGLISH
            val showIslamicPatterns = settingsRepository.showIslamicPatterns.first()
            val animationsEnabled = settingsRepository.animationsEnabled.first()
            val showCountdown = settingsRepository.showCountdown.first()
            val showQuickActions = settingsRepository.showQuickActions.first()
            val hapticFeedback = settingsRepository.hapticFeedback.first()
            val use24Hour = settingsRepository.use24HourFormat.first()
            val useHijri = settingsRepository.useHijriPrimary.first()

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
                    useHijriPrimary = useHijri
                )
            }

            // Prayer settings
            val calcMethodStr = settingsRepository.calculationMethod.first()
            val calcMethod = try {
                CalculationMethod.valueOf(calcMethodStr)
            } catch (_: Exception) {
                CalculationMethod.MUSLIM_WORLD_LEAGUE
            }
            val asrStr = settingsRepository.asrCalculation.first()
            val asrMethod = when (asrStr.lowercase()) {
                "hanafi" -> AsrJuristicMethod.HANAFI
                else -> AsrJuristicMethod.STANDARD
            }
            val highLatStr = settingsRepository.highLatitudeRule.first()
            val highLat = try {
                HighLatitudeRule.valueOf(highLatStr)
            } catch (_: Exception) {
                HighLatitudeRule.MIDDLE_OF_NIGHT
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
            val respectDnd = settingsRepository.adhanRespectDnd.first()
            val adhanSoundName = settingsRepository.selectedAdhanSound.first()
            val fajrNotif = settingsRepository.fajrNotificationEnabled.first()
            val sunriseNotif = settingsRepository.sunriseNotificationEnabled.first()
            val dhuhrNotif = settingsRepository.dhuhrNotificationEnabled.first()
            val asrNotif = settingsRepository.asrNotificationEnabled.first()
            val maghribNotif = settingsRepository.maghribNotificationEnabled.first()
            val ishaNotif = settingsRepository.ishaNotificationEnabled.first()

            // Per-prayer adhan settings
            val fajrAdhan = settingsRepository.fajrAdhanEnabled.first()
            val dhuhrAdhan = settingsRepository.dhuhrAdhanEnabled.first()
            val asrAdhan = settingsRepository.asrAdhanEnabled.first()
            val maghribAdhan = settingsRepository.maghribAdhanEnabled.first()
            val ishaAdhan = settingsRepository.ishaAdhanEnabled.first()

            _notificationState.update {
                it.copy(
                    notificationsEnabled = notifEnabled,
                    adhanEnabled = adhanEnabled,
                    fajrAdhanEnabled = fajrAdhan,
                    dhuhrAdhanEnabled = dhuhrAdhan,
                    asrAdhanEnabled = asrAdhan,
                    maghribAdhanEnabled = maghribAdhan,
                    ishaAdhanEnabled = ishaAdhan,
                    vibrationEnabled = vibration,
                    reminderMinutes = reminderMin,
                    showReminderBefore = showReminder,
                    persistentNotification = persistent,
                    fridayReminderEnabled = fridayReminder,
                    fridayReminderMinutes = fridayReminderMin,
                    respectDnd = respectDnd,
                    selectedAdhanSound = adhanSoundName,
                    fajrNotification = fajrNotif,
                    sunriseNotification = sunriseNotif,
                    dhuhrNotification = dhuhrNotif,
                    asrNotification = asrNotif,
                    maghribNotification = maghribNotif,
                    ishaNotification = ishaNotif
                )
            }

            // Quran settings
            val translatorId = settingsRepository.quranTranslatorId.first()
            val arabicFontId = settingsRepository.quranArabicFont.first()
            val showTranslation = settingsRepository.showTranslation.first()
            val showTransliteration = settingsRepository.showTransliteration.first()
            val arabicFontSize = settingsRepository.quranArabicFontSize.first()
            val translationFontSize = settingsRepository.quranTranslationFontSize.first()
            val continuousReading = settingsRepository.continuousReading.first()
            val keepScreenOn = settingsRepository.keepScreenOn.first()
            val reciterId = settingsRepository.selectedReciterId.first()
            val showTajweed = settingsRepository.showTajweed.first()

            _quranState.update {
                it.copy(
                    selectedTranslatorId = translatorId,
                    selectedArabicFontId = arabicFontId,
                    showTranslation = showTranslation,
                    showTransliteration = showTransliteration,
                    arabicFontSize = arabicFontSize,
                    translationFontSize = translationFontSize,
                    continuousReading = continuousReading,
                    keepScreenOn = keepScreenOn,
                    selectedReciterId = reciterId,
                    showTajweed = showTajweed
                )
            }

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
        viewModelScope.launch {
            prayerUseCases.getCurrentLocation().collect { location ->
                _locationState.update { it.copy(currentLocation = location) }
            }
        }

        viewModelScope.launch {
            prayerUseCases.getAllLocations().collect { locations ->
                _locationState.update { it.copy(savedLocations = locations, isLoading = false) }
            }
        }

        viewModelScope.launch {
            prayerUseCases.getFavoriteLocations().collect { favorites ->
                _locationState.update { it.copy(favoriteLocations = favorites) }
            }
        }
    }

    private suspend fun rescheduleNotifications() {
        val prefs = settingsRepository.userPreferences.first()
        val lat = prefs.latitude
        val lng = prefs.longitude
        val notifState = _notificationState.value
        val prayerSettings = _prayerState.value

        val enabledPrayers = buildSet {
            if (notifState.fajrNotification) add(PrayerType.FAJR)
            if (notifState.dhuhrNotification) add(PrayerType.DHUHR)
            if (notifState.asrNotification) add(PrayerType.ASR)
            if (notifState.maghribNotification) add(PrayerType.MAGHRIB)
            if (notifState.ishaNotification) add(PrayerType.ISHA)
            if (notifState.sunriseNotification) add(PrayerType.SUNRISE)
        }

        val calcMethod = prayerSettings.calculationMethod
        val asrCalc = when (prayerSettings.asrMethod) {
            AsrJuristicMethod.STANDARD -> AsrCalculation.STANDARD
            AsrJuristicMethod.HANAFI -> AsrCalculation.HANAFI
        }
        val highLatRule = try {
            com.arshadshah.nimaz.domain.model.HighLatitudeRule.valueOf(
                prayerSettings.highLatitudeRule.name.let {
                    // Map SettingsViewModel enum names to domain model enum names
                    when (it) {
                        "MIDDLE_OF_NIGHT" -> "MIDDLE_OF_THE_NIGHT"
                        "SEVENTH_OF_NIGHT" -> "SEVENTH_OF_THE_NIGHT"
                        else -> it
                    }
                }
            )
        } catch (_: Exception) {
            null
        }

        val adjustments = mapOf(
            PrayerType.FAJR to prayerSettings.fajrAdjustment,
            PrayerType.SUNRISE to prayerSettings.sunriseAdjustment,
            PrayerType.DHUHR to prayerSettings.dhuhrAdjustment,
            PrayerType.ASR to prayerSettings.asrAdjustment,
            PrayerType.MAGHRIB to prayerSettings.maghribAdjustment,
            PrayerType.ISHA to prayerSettings.ishaAdjustment
        )

        prayerNotificationScheduler.scheduleTodaysPrayerNotifications(
            latitude = lat,
            longitude = lng,
            notificationsEnabled = notifState.notificationsEnabled,
            enabledPrayers = enabledPrayers,
            preReminderEnabled = notifState.showReminderBefore,
            preReminderMinutes = notifState.reminderMinutes,
            calculationMethod = calcMethod,
            asrCalculation = asrCalc,
            highLatitudeRule = highLatRule,
            adjustments = adjustments,
            fridayReminderEnabled = notifState.fridayReminderEnabled,
            fridayReminderMinutes = notifState.fridayReminderMinutes
        )
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

    private fun updatePrayerAdhanEnabled(prayer: String, enabled: Boolean) {
        _notificationState.update { state ->
            when (prayer.lowercase()) {
                "fajr" -> state.copy(fajrAdhanEnabled = enabled)
                "dhuhr" -> state.copy(dhuhrAdhanEnabled = enabled)
                "asr" -> state.copy(asrAdhanEnabled = enabled)
                "maghrib" -> state.copy(maghribAdhanEnabled = enabled)
                "isha" -> state.copy(ishaAdhanEnabled = enabled)
                else -> state
            }
        }
    }

    private fun setCurrentLocation(location: Location) {
        viewModelScope.launch {
            val id = prayerUseCases.insertLocation(location)
            prayerUseCases.setCurrentLocation(id)
        }
    }

    private fun addLocation(location: Location) {
        viewModelScope.launch {
            prayerUseCases.insertLocation(location)
        }
    }

    private fun removeLocation(location: Location) {
        viewModelScope.launch {
            prayerUseCases.deleteLocation(location)
        }
    }

    private fun toggleLocationFavorite(locationId: Long) {
        viewModelScope.launch {
            prayerUseCases.toggleFavorite(locationId)
        }
    }

    private fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.clearAllData()
            _generalState.update { GeneralSettingsUiState() }
            _prayerState.update { PrayerSettingsUiState() }
            _notificationState.update { NotificationSettingsUiState() }
            _quranState.update { QuranSettingsUiState() }
            _widgetState.update { WidgetSettingsUiState() }
            _shouldRestart.value = true
        }
    }

    private fun exportSettings() {
        viewModelScope.launch {
            // Implementation would export all settings to a shareable format
        }
    }

    private fun importSettings() {
        viewModelScope.launch {
            // Implementation would import settings from a file
        }
    }

    private fun deleteAllData() {
        viewModelScope.launch {
            // Clear all user data from DAOs
            database.quranDao().deleteAllUserData()
            database.hadithDao().deleteAllUserData()
            database.duaDao().deleteAllUserData()
            database.prayerDao().deleteAllUserData()
            database.fastingDao().deleteAllUserData()
            database.tasbihDao().deleteAllUserData()
            database.zakatDao().deleteAllUserData()
            database.locationDao().deleteAllUserData()
            database.tafseerDao().deleteAllUserData()

            // Clear DataStore preferences
            settingsRepository.clearAllData()

            // Reset UI state to defaults
            _generalState.update { GeneralSettingsUiState() }
            _prayerState.update { PrayerSettingsUiState() }
            _notificationState.update { NotificationSettingsUiState() }
            _quranState.update { QuranSettingsUiState() }
            _locationState.update { LocationSettingsUiState() }
            _widgetState.update { WidgetSettingsUiState() }
            _shouldRestart.value = true
        }
    }
}
