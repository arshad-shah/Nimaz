package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val hapticFeedback: Boolean = true
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
    val persistentNotification: Boolean = false
)

data class QuranSettingsUiState(
    val selectedTranslatorId: String = "en.sahih",
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
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val preferencesDataStore: PreferencesDataStore
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
            is SettingsEvent.SetLanguage -> _generalState.update { it.copy(language = event.language) }
            is SettingsEvent.SetHijriPrimary -> _generalState.update { it.copy(useHijriPrimary = event.enabled) }
            is SettingsEvent.Set24HourFormat -> _generalState.update { it.copy(use24HourFormat = event.enabled) }
            is SettingsEvent.SetShowSeconds -> _generalState.update { it.copy(showSeconds = event.enabled) }
            is SettingsEvent.SetHapticFeedback -> _generalState.update { it.copy(hapticFeedback = event.enabled) }

            // Prayer
            is SettingsEvent.SetCalculationMethod -> _prayerState.update { it.copy(calculationMethod = event.method) }
            is SettingsEvent.SetAsrMethod -> _prayerState.update { it.copy(asrMethod = event.method) }
            is SettingsEvent.SetHighLatitudeRule -> _prayerState.update { it.copy(highLatitudeRule = event.rule) }
            is SettingsEvent.SetFajrAngle -> _prayerState.update { it.copy(fajrAngle = event.angle) }
            is SettingsEvent.SetIshaAngle -> _prayerState.update { it.copy(ishaAngle = event.angle) }
            is SettingsEvent.SetPrayerAdjustment -> updatePrayerAdjustment(event.prayer, event.minutes)

            // Notifications
            is SettingsEvent.SetNotificationsEnabled -> _notificationState.update { it.copy(notificationsEnabled = event.enabled) }
            is SettingsEvent.SetPrayerNotification -> updatePrayerNotification(event.prayer, event.enabled)
            is SettingsEvent.SetAdhanEnabled -> _notificationState.update { it.copy(adhanEnabled = event.enabled) }
            is SettingsEvent.SetVibrationEnabled -> _notificationState.update { it.copy(vibrationEnabled = event.enabled) }
            is SettingsEvent.SetReminderMinutes -> _notificationState.update { it.copy(reminderMinutes = event.minutes) }
            is SettingsEvent.SetShowReminderBefore -> _notificationState.update { it.copy(showReminderBefore = event.enabled) }
            is SettingsEvent.SetPersistentNotification -> _notificationState.update { it.copy(persistentNotification = event.enabled) }

            // Quran
            is SettingsEvent.SetTranslator -> _quranState.update { it.copy(selectedTranslatorId = event.translatorId) }
            is SettingsEvent.SetShowTranslation -> _quranState.update { it.copy(showTranslation = event.enabled) }
            is SettingsEvent.SetShowTransliteration -> _quranState.update { it.copy(showTransliteration = event.enabled) }
            is SettingsEvent.SetArabicFontSize -> _quranState.update { it.copy(arabicFontSize = event.size) }
            is SettingsEvent.SetTranslationFontSize -> _quranState.update { it.copy(translationFontSize = event.size) }
            is SettingsEvent.SetContinuousReading -> _quranState.update { it.copy(continuousReading = event.enabled) }
            is SettingsEvent.SetKeepScreenOn -> _quranState.update { it.copy(keepScreenOn = event.enabled) }
            is SettingsEvent.SetReciter -> _quranState.update { it.copy(selectedReciterId = event.reciterId) }

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
        }

        // Save settings after each change
        saveSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesDataStore.themeMode.collect { themeMode ->
                val theme = when (themeMode) {
                    "light" -> AppTheme.LIGHT
                    "dark" -> AppTheme.DARK
                    else -> AppTheme.SYSTEM
                }
                _generalState.update { it.copy(theme = theme) }
            }
        }
    }

    private fun saveSettings() {
        // In production, this would persist to DataStore
        viewModelScope.launch {
            // Save to DataStore
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
        saveSettings()
    }

    private fun exportSettings() {
        // Export settings to JSON
        viewModelScope.launch {
            // Implementation would export all settings to a shareable format
        }
    }

    private fun importSettings() {
        // Import settings from JSON
        viewModelScope.launch {
            // Implementation would import settings from a file
        }
    }
}
