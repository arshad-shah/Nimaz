package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    ENGLISH("en", "English", "English", "\uD83C\uDDEC\uD83C\uDDE7"),
    ARABIC("ar", "Arabic", "العربية", "\uD83C\uDDF8\uD83C\uDDE6"),
    TURKISH("tr", "Turkish", "Türkçe", "\uD83C\uDDF9\uD83C\uDDF7"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "\uD83C\uDDEE\uD83C\uDDE9"),
    MALAY("ms", "Malay", "Bahasa Melayu", "\uD83C\uDDF2\uD83C\uDDFE"),
    FRENCH("fr", "French", "Français", "\uD83C\uDDEB\uD83C\uDDF7"),
    GERMAN("de", "German", "Deutsch", "\uD83C\uDDE9\uD83C\uDDEA"),
    URDU("ur", "Urdu", "اردو", "\uD83C\uDDF5\uD83C\uDDF0")
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
    val accentColor: String = "Teal",
    val appIcon: String = "Default",
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
    val vibrationEnabled: Boolean = true,
    val reminderMinutes: Int = 15,
    val showReminderBefore: Boolean = true,
    val persistentNotification: Boolean = false,
    val selectedAdhanSound: String = "MISHARY"
)

data class QuranSettingsUiState(
    val selectedTranslatorId: String = "sahih_international",
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val arabicFontSize: Float = 28f,
    val translationFontSize: Float = 16f,
    val continuousReading: Boolean = true,
    val keepScreenOn: Boolean = true,
    val selectedReciterId: String? = null
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
    data class SetAccentColor(val color: String) : SettingsEvent
    data class SetAppIcon(val icon: String) : SettingsEvent
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
    data class SetVibrationEnabled(val enabled: Boolean) : SettingsEvent
    data class SetReminderMinutes(val minutes: Int) : SettingsEvent
    data class SetShowReminderBefore(val enabled: Boolean) : SettingsEvent
    data class SetPersistentNotification(val enabled: Boolean) : SettingsEvent
    data class SetAdhanSound(val sound: String) : SettingsEvent
    data object PreviewAdhanSound : SettingsEvent
    data object StopAdhanPreview : SettingsEvent

    // Quran
    data class SetTranslator(val translatorId: String) : SettingsEvent
    data class SetShowTranslation(val enabled: Boolean) : SettingsEvent
    data class SetShowTransliteration(val enabled: Boolean) : SettingsEvent
    data class SetArabicFontSize(val size: Float) : SettingsEvent
    data class SetTranslationFontSize(val size: Float) : SettingsEvent
    data class SetContinuousReading(val enabled: Boolean) : SettingsEvent
    data class SetKeepScreenOn(val enabled: Boolean) : SettingsEvent
    data class SetReciter(val reciterId: String?) : SettingsEvent

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
    data object ResetNotifications : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val prayerNotificationScheduler: PrayerNotificationScheduler,
    val adhanAudioManager: AdhanAudioManager
) : ViewModel() {

    private val _generalState = MutableStateFlow(GeneralSettingsUiState())
    val generalState: StateFlow<GeneralSettingsUiState> = _generalState.asStateFlow()

    private val _prayerState = MutableStateFlow(PrayerSettingsUiState())
    val prayerState: StateFlow<PrayerSettingsUiState> = _prayerState.asStateFlow()

    private val _notificationState = MutableStateFlow(NotificationSettingsUiState())
    val notificationState: StateFlow<NotificationSettingsUiState> = _notificationState.asStateFlow()

    private val _quranState = MutableStateFlow(QuranSettingsUiState())
    val quranState: StateFlow<QuranSettingsUiState> = _quranState.asStateFlow()

    private val _locationState = MutableStateFlow(LocationSettingsUiState())
    val locationState: StateFlow<LocationSettingsUiState> = _locationState.asStateFlow()

    private val _widgetState = MutableStateFlow(WidgetSettingsUiState())
    val widgetState: StateFlow<WidgetSettingsUiState> = _widgetState.asStateFlow()

    init {
        loadSettings()
        loadLocations()
    }

    fun onEvent(event: SettingsEvent) {
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
                    preferencesDataStore.setThemeMode(modeString)
                }
            }
            is SettingsEvent.SetLanguage -> {
                _generalState.update { it.copy(language = event.language) }
                viewModelScope.launch { preferencesDataStore.setAppLanguage(event.language.code) }
                // Locale change is applied by the language settings screen via LocaleHelper
            }
            is SettingsEvent.SetHijriPrimary -> {
                _generalState.update { it.copy(useHijriPrimary = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setUseHijriPrimary(event.enabled) }
            }
            is SettingsEvent.Set24HourFormat -> {
                _generalState.update { it.copy(use24HourFormat = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setUse24HourFormat(event.enabled) }
            }
            is SettingsEvent.SetShowSeconds -> _generalState.update { it.copy(showSeconds = event.enabled) }
            is SettingsEvent.SetHapticFeedback -> {
                _generalState.update { it.copy(hapticFeedback = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setHapticFeedback(event.enabled) }
            }
            is SettingsEvent.SetAccentColor -> {
                _generalState.update { it.copy(accentColor = event.color) }
                viewModelScope.launch { preferencesDataStore.setAccentColor(event.color) }
            }
            is SettingsEvent.SetAppIcon -> {
                _generalState.update { it.copy(appIcon = event.icon) }
                viewModelScope.launch { preferencesDataStore.setAppIcon(event.icon) }
            }
            is SettingsEvent.SetShowIslamicPatterns -> {
                _generalState.update { it.copy(showIslamicPatterns = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setShowIslamicPatterns(event.enabled) }
            }
            is SettingsEvent.SetAnimationsEnabled -> {
                _generalState.update { it.copy(animationsEnabled = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setAnimationsEnabled(event.enabled) }
            }
            is SettingsEvent.SetShowCountdown -> {
                _generalState.update { it.copy(showCountdown = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setShowCountdown(event.enabled) }
            }
            is SettingsEvent.SetShowQuickActions -> {
                _generalState.update { it.copy(showQuickActions = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setShowQuickActions(event.enabled) }
            }

            // Prayer
            is SettingsEvent.SetCalculationMethod -> {
                _prayerState.update { it.copy(calculationMethod = event.method) }
                viewModelScope.launch {
                    preferencesDataStore.setCalculationMethod(event.method.name)
                    rescheduleNotifications()
                }
            }
            is SettingsEvent.SetAsrMethod -> {
                _prayerState.update { it.copy(asrMethod = event.method) }
                viewModelScope.launch {
                    preferencesDataStore.setAsrCalculation(event.method.name.lowercase())
                    rescheduleNotifications()
                }
            }
            is SettingsEvent.SetHighLatitudeRule -> {
                _prayerState.update { it.copy(highLatitudeRule = event.rule) }
                viewModelScope.launch {
                    preferencesDataStore.setHighLatitudeRule(event.rule.name)
                    rescheduleNotifications()
                }
            }
            is SettingsEvent.SetFajrAngle -> _prayerState.update { it.copy(fajrAngle = event.angle) }
            is SettingsEvent.SetIshaAngle -> _prayerState.update { it.copy(ishaAngle = event.angle) }
            is SettingsEvent.SetPrayerAdjustment -> {
                updatePrayerAdjustment(event.prayer, event.minutes)
                viewModelScope.launch {
                    preferencesDataStore.setPrayerAdjustment(event.prayer, event.minutes)
                    rescheduleNotifications()
                }
            }

            // Notifications
            is SettingsEvent.SetNotificationsEnabled -> {
                _notificationState.update { it.copy(notificationsEnabled = event.enabled) }
                viewModelScope.launch {
                    preferencesDataStore.setPrayerNotificationsEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }
            is SettingsEvent.SetPrayerNotification -> {
                updatePrayerNotification(event.prayer, event.enabled)
                viewModelScope.launch {
                    preferencesDataStore.setPrayerNotificationEnabled(event.prayer, event.enabled)
                    rescheduleNotifications()
                }
            }
            is SettingsEvent.SetAdhanEnabled -> {
                _notificationState.update { it.copy(adhanEnabled = event.enabled) }
                viewModelScope.launch {
                    preferencesDataStore.setAdhanEnabled(event.enabled)
                    rescheduleNotifications()
                }
            }
            is SettingsEvent.SetVibrationEnabled -> {
                _notificationState.update { it.copy(vibrationEnabled = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setNotificationVibration(event.enabled) }
            }
            is SettingsEvent.SetReminderMinutes -> {
                _notificationState.update { it.copy(reminderMinutes = event.minutes) }
                viewModelScope.launch {
                    preferencesDataStore.setNotificationReminderMinutes(event.minutes)
                    rescheduleNotifications()
                }
            }
            is SettingsEvent.SetShowReminderBefore -> {
                _notificationState.update { it.copy(showReminderBefore = event.enabled) }
                viewModelScope.launch {
                    preferencesDataStore.setShowReminderBefore(event.enabled)
                    rescheduleNotifications()
                }
            }
            is SettingsEvent.SetPersistentNotification -> {
                _notificationState.update { it.copy(persistentNotification = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setPersistentNotification(event.enabled) }
            }
            is SettingsEvent.SetAdhanSound -> {
                _notificationState.update { it.copy(selectedAdhanSound = event.sound) }
                viewModelScope.launch { preferencesDataStore.setSelectedAdhanSound(event.sound) }
            }
            SettingsEvent.PreviewAdhanSound -> {
                val sound = AdhanSound.fromName(_notificationState.value.selectedAdhanSound)
                adhanAudioManager.preview(sound)
            }
            SettingsEvent.StopAdhanPreview -> {
                adhanAudioManager.stopPreview()
            }

            // Quran
            is SettingsEvent.SetTranslator -> {
                _quranState.update { it.copy(selectedTranslatorId = event.translatorId) }
                viewModelScope.launch { preferencesDataStore.setQuranTranslatorId(event.translatorId) }
            }
            is SettingsEvent.SetShowTranslation -> {
                _quranState.update { it.copy(showTranslation = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setShowTranslation(event.enabled) }
            }
            is SettingsEvent.SetShowTransliteration -> {
                _quranState.update { it.copy(showTransliteration = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setShowTransliteration(event.enabled) }
            }
            is SettingsEvent.SetArabicFontSize -> {
                _quranState.update { it.copy(arabicFontSize = event.size) }
                viewModelScope.launch { preferencesDataStore.setQuranArabicFontSize(event.size) }
            }
            is SettingsEvent.SetTranslationFontSize -> {
                _quranState.update { it.copy(translationFontSize = event.size) }
                viewModelScope.launch { preferencesDataStore.setQuranTranslationFontSize(event.size) }
            }
            is SettingsEvent.SetContinuousReading -> {
                _quranState.update { it.copy(continuousReading = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setContinuousReading(event.enabled) }
            }
            is SettingsEvent.SetKeepScreenOn -> {
                _quranState.update { it.copy(keepScreenOn = event.enabled) }
                viewModelScope.launch { preferencesDataStore.setKeepScreenOn(event.enabled) }
            }
            is SettingsEvent.SetReciter -> {
                _quranState.update { it.copy(selectedReciterId = event.reciterId) }
                viewModelScope.launch { preferencesDataStore.setSelectedReciterId(event.reciterId) }
            }

            // Location
            is SettingsEvent.SetCurrentLocation -> setCurrentLocation(event.location)
            is SettingsEvent.AddLocation -> addLocation(event.location)
            is SettingsEvent.RemoveLocation -> removeLocation(event.location)
            is SettingsEvent.ToggleLocationFavorite -> toggleLocationFavorite(event.locationId)
            is SettingsEvent.SetAutoDetectLocation -> _locationState.update { it.copy(autoDetectLocation = event.enabled) }

            // Widget
            is SettingsEvent.SetPrayerTimesWidgetEnabled -> _widgetState.update { it.copy(prayerTimesWidgetEnabled = event.enabled) }
            is SettingsEvent.SetWidgetTheme -> _widgetState.update { it.copy(widgetTheme = event.theme) }
            is SettingsEvent.SetShowNextPrayerCountdown -> _widgetState.update { it.copy(showNextPrayerCountdown = event.enabled) }
            is SettingsEvent.SetWidgetTransparency -> _widgetState.update { it.copy(widgetTransparency = event.transparency) }

            // Actions
            SettingsEvent.LoadSettings -> loadSettings()
            SettingsEvent.ResetToDefaults -> resetToDefaults()
            SettingsEvent.ExportSettings -> exportSettings()
            SettingsEvent.ImportSettings -> importSettings()
            SettingsEvent.TestNotification -> {
                prayerNotificationScheduler.sendTestNotification()
            }
            SettingsEvent.ResetNotifications -> {
                viewModelScope.launch {
                    prayerNotificationScheduler.cancelAllPrayerNotifications()
                    rescheduleNotifications()
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // General settings
            val theme = when (preferencesDataStore.themeMode.first()) {
                "light" -> AppTheme.LIGHT
                "dark" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            }
            val langCode = preferencesDataStore.appLanguage.first()
            val language = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.ENGLISH
            val accentColor = preferencesDataStore.accentColor.first()
            val appIcon = preferencesDataStore.appIcon.first()
            val showIslamicPatterns = preferencesDataStore.showIslamicPatterns.first()
            val animationsEnabled = preferencesDataStore.animationsEnabled.first()
            val showCountdown = preferencesDataStore.showCountdown.first()
            val showQuickActions = preferencesDataStore.showQuickActions.first()
            val hapticFeedback = preferencesDataStore.hapticFeedback.first()
            val use24Hour = preferencesDataStore.use24HourFormat.first()
            val useHijri = preferencesDataStore.useHijriPrimary.first()

            _generalState.update {
                it.copy(
                    theme = theme,
                    language = language,
                    accentColor = accentColor,
                    appIcon = appIcon,
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
            val calcMethodStr = preferencesDataStore.calculationMethod.first()
            val calcMethod = try { CalculationMethod.valueOf(calcMethodStr) } catch (_: Exception) { CalculationMethod.MUSLIM_WORLD_LEAGUE }
            val asrStr = preferencesDataStore.asrCalculation.first()
            val asrMethod = when (asrStr.lowercase()) {
                "hanafi" -> AsrJuristicMethod.HANAFI
                else -> AsrJuristicMethod.STANDARD
            }
            val highLatStr = preferencesDataStore.highLatitudeRule.first()
            val highLat = try { HighLatitudeRule.valueOf(highLatStr) } catch (_: Exception) { HighLatitudeRule.MIDDLE_OF_NIGHT }

            val fajrAdj = preferencesDataStore.fajrAdjustment.first()
            val sunriseAdj = preferencesDataStore.sunriseAdjustment.first()
            val dhuhrAdj = preferencesDataStore.dhuhrAdjustment.first()
            val asrAdj = preferencesDataStore.asrAdjustment.first()
            val maghribAdj = preferencesDataStore.maghribAdjustment.first()
            val ishaAdj = preferencesDataStore.ishaAdjustment.first()

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
            val notifEnabled = preferencesDataStore.prayerNotificationsEnabled.first()
            val adhanEnabled = preferencesDataStore.adhanEnabled.first()
            val vibration = preferencesDataStore.notificationVibration.first()
            val reminderMin = preferencesDataStore.notificationReminderMinutes.first()
            val showReminder = preferencesDataStore.showReminderBefore.first()
            val persistent = preferencesDataStore.persistentNotification.first()
            val adhanSoundName = preferencesDataStore.selectedAdhanSound.first()
            val fajrNotif = preferencesDataStore.fajrNotificationEnabled.first()
            val sunriseNotif = preferencesDataStore.sunriseNotificationEnabled.first()
            val dhuhrNotif = preferencesDataStore.dhuhrNotificationEnabled.first()
            val asrNotif = preferencesDataStore.asrNotificationEnabled.first()
            val maghribNotif = preferencesDataStore.maghribNotificationEnabled.first()
            val ishaNotif = preferencesDataStore.ishaNotificationEnabled.first()

            _notificationState.update {
                it.copy(
                    notificationsEnabled = notifEnabled,
                    adhanEnabled = adhanEnabled,
                    vibrationEnabled = vibration,
                    reminderMinutes = reminderMin,
                    showReminderBefore = showReminder,
                    persistentNotification = persistent,
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
            val translatorId = preferencesDataStore.quranTranslatorId.first()
            val showTranslation = preferencesDataStore.showTranslation.first()
            val showTransliteration = preferencesDataStore.showTransliteration.first()
            val arabicFontSize = preferencesDataStore.quranArabicFontSize.first()
            val translationFontSize = preferencesDataStore.quranTranslationFontSize.first()
            val continuousReading = preferencesDataStore.continuousReading.first()
            val keepScreenOn = preferencesDataStore.keepScreenOn.first()
            val reciterId = preferencesDataStore.selectedReciterId.first()

            _quranState.update {
                it.copy(
                    selectedTranslatorId = translatorId,
                    showTranslation = showTranslation,
                    showTransliteration = showTransliteration,
                    arabicFontSize = arabicFontSize,
                    translationFontSize = translationFontSize,
                    continuousReading = continuousReading,
                    keepScreenOn = keepScreenOn,
                    selectedReciterId = reciterId
                )
            }
        }
    }

    private fun loadLocations() {
        viewModelScope.launch {
            prayerRepository.getCurrentLocation().collect { location ->
                _locationState.update { it.copy(currentLocation = location) }
            }
        }

        viewModelScope.launch {
            prayerRepository.getAllLocations().collect { locations ->
                _locationState.update { it.copy(savedLocations = locations, isLoading = false) }
            }
        }

        viewModelScope.launch {
            prayerRepository.getFavoriteLocations().collect { favorites ->
                _locationState.update { it.copy(favoriteLocations = favorites) }
            }
        }
    }

    private suspend fun rescheduleNotifications() {
        val prefs = preferencesDataStore.userPreferences.first()
        val lat = prefs.latitude
        val lng = prefs.longitude
        val notifState = _notificationState.value

        val enabledPrayers = buildSet {
            if (notifState.fajrNotification) add(PrayerType.FAJR)
            if (notifState.dhuhrNotification) add(PrayerType.DHUHR)
            if (notifState.asrNotification) add(PrayerType.ASR)
            if (notifState.maghribNotification) add(PrayerType.MAGHRIB)
            if (notifState.ishaNotification) add(PrayerType.ISHA)
            if (notifState.sunriseNotification) add(PrayerType.SUNRISE)
        }

        prayerNotificationScheduler.scheduleTodaysPrayerNotifications(
            latitude = lat,
            longitude = lng,
            notificationsEnabled = notifState.notificationsEnabled,
            enabledPrayers = enabledPrayers
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

    private fun setCurrentLocation(location: Location) {
        viewModelScope.launch {
            val id = prayerRepository.insertLocation(location)
            prayerRepository.setCurrentLocation(id)
        }
    }

    private fun addLocation(location: Location) {
        viewModelScope.launch {
            prayerRepository.insertLocation(location)
        }
    }

    private fun removeLocation(location: Location) {
        viewModelScope.launch {
            prayerRepository.deleteLocation(location)
        }
    }

    private fun toggleLocationFavorite(locationId: Long) {
        viewModelScope.launch {
            prayerRepository.toggleFavorite(locationId)
        }
    }

    private fun resetToDefaults() {
        _generalState.update { GeneralSettingsUiState() }
        _prayerState.update { PrayerSettingsUiState() }
        _notificationState.update { NotificationSettingsUiState() }
        _quranState.update { QuranSettingsUiState() }
        _widgetState.update { WidgetSettingsUiState() }
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
}
