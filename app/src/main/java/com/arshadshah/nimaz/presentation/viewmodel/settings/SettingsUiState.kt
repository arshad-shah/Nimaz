package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import kotlinx.coroutines.flow.first

data class GeneralSettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val useHijriPrimary: Boolean = false,
    val hijriDayOffset: Int = 0,
    val use24HourFormat: Boolean = false,
    val hapticFeedback: Boolean = true,
    val showIslamicPatterns: Boolean = true,
    val patternStyle: NimazPatternStyle = NimazPatternStyle.CORNER_MEDALLION,
    val animationsEnabled: Boolean = true,
    val showCountdown: Boolean = true,
    val showQuickActions: Boolean = true
)

data class PrayerSettingsUiState(
    val calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val asrMethod: AsrJuristicMethod = AsrJuristicMethod.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
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
    // Per-prayer alert style and reminder, keyed by prayer name ("fajr" … "isha").
    // Sunrise has neither: it is a plain alert with a beep and no pre-reminder.
    val alertStyles: Map<String, PrayerAlertStyle> = emptyMap(),
    val reminderEnabled: Map<String, Boolean> = emptyMap(),
    val reminderOffsets: Map<String, Int> = emptyMap(),
    val vibrationEnabled: Boolean = true,
    val respectDnd: Boolean = true,
    val reminderMinutes: Int = 15,
    val showReminderBefore: Boolean = true,
    val persistentNotification: Boolean = false,
    val fridayReminderEnabled: Boolean = false,
    val fridayReminderMinutes: Int = 60,
    val khatamReminderEnabled: Boolean = false,
    val khatamReminderTime: String = "06:00",
    val selectedAdhanSound: String = "MISHARY",
    // Extended worship reminders, keyed by WorshipReminderType.key.
    val worshipReminders: Map<String, Boolean> = emptyMap(),
    val worshipOffsets: Map<String, Int> = emptyMap(),
    // Per-reminder mode (currently only Witr: "after_isha" | "before_fajr").
    val worshipModes: Map<String, String> = emptyMap(),
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
    val showTajweed: Boolean = false,
    val tajweedUnderline: Boolean = false,
    /** The Mushaf edition/layout for the page reader (default Uthmani/604 vs 16-line IndoPak/548). */
    val mushafScript: MushafScript = MushafScript.DEFAULT,
    /**
     * The selected translation's actual text for the preview card's sample ayah, so the
     * preview shows the chosen translation rather than a fixed English string. Null until
     * the first load; the previous value is kept while a newly picked translation resolves
     * (its first read seeds it), so the card never flashes empty.
     */
    val previewTranslation: String? = null
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
    val isLoading: Boolean = true
)
