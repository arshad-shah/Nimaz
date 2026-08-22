package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle

sealed interface SettingsEvent {
    // General
    data class SetTheme(val theme: AppTheme) : SettingsEvent
    data class SetLanguage(val language: AppLanguage) : SettingsEvent
    data class SetHijriPrimary(val enabled: Boolean) : SettingsEvent
    data class SetHijriDayOffset(val days: Int) : SettingsEvent
    data class Set24HourFormat(val enabled: Boolean) : SettingsEvent
    data class SetHapticFeedback(val enabled: Boolean) : SettingsEvent
    data class SetShowIslamicPatterns(val enabled: Boolean) : SettingsEvent
    data class SetPatternStyle(val style: NimazPatternStyle) : SettingsEvent
    data class SetAnimationsEnabled(val enabled: Boolean) : SettingsEvent
    data class SetShowCountdown(val enabled: Boolean) : SettingsEvent
    data class SetShowQuickActions(val enabled: Boolean) : SettingsEvent

    // Prayer
    data class SetCalculationMethod(val method: CalculationMethod) : SettingsEvent
    data class SetAsrMethod(val method: AsrCalculation) : SettingsEvent
    data class SetHighLatitudeRule(val rule: HighLatitudeRule) : SettingsEvent
    data class SetPrayerAdjustment(val prayer: String, val minutes: Int) : SettingsEvent

    // Notifications
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsEvent
    data class SetPrayerNotification(val prayer: String, val enabled: Boolean) : SettingsEvent
    data class SetAdhanEnabled(val enabled: Boolean) : SettingsEvent

    /** How one prayer announces itself: the adhan, the standard tone, or nothing. */
    data class SetPrayerAlertStyle(val prayer: String, val style: PrayerAlertStyle) : SettingsEvent

    /** Whether one prayer gets a reminder ahead of its time, and how far ahead. */
    data class SetPrayerReminderEnabled(val prayer: String, val enabled: Boolean) : SettingsEvent
    data class SetPrayerReminderMinutes(val prayer: String, val minutes: Int) : SettingsEvent
    data class SetVibrationEnabled(val enabled: Boolean) : SettingsEvent
    data class SetRespectDnd(val enabled: Boolean) : SettingsEvent
    data class SetReminderMinutes(val minutes: Int) : SettingsEvent
    data class SetShowReminderBefore(val enabled: Boolean) : SettingsEvent
    data class SetPersistentNotification(val enabled: Boolean) : SettingsEvent
    data class SetFridayReminderEnabled(val enabled: Boolean) : SettingsEvent
    data class SetFridayReminderMinutes(val minutes: Int) : SettingsEvent
    data class SetKhatamReminderEnabled(val enabled: Boolean) : SettingsEvent

    /** Reminder time as "HH:mm". */
    data class SetKhatamReminderTime(val time: String) : SettingsEvent
    data class SetAdhanSound(val sound: String) : SettingsEvent

    /** Extended worship reminders, keyed by WorshipReminderType.key. */
    data class SetWorshipReminderEnabled(val key: String, val enabled: Boolean) : SettingsEvent
    data class SetWorshipReminderOffset(val key: String, val minutes: Int) : SettingsEvent
    data class SetWorshipReminderMode(val key: String, val mode: String) : SettingsEvent
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

    /**
     * Play a one-ayah sample in [reciterId], so the user can hear a voice before choosing it.
     *
     * New in PR 21 of #551, and it replaces a call that never worked. `SelectReciterScreen` used
     * to do this by taking a second ViewModel — `quranViewModel: QuranViewModel = hiltViewModel()`
     * — and dispatching `QuranEvent.PreviewReciter`. But `hiltViewModel()` resolves against the
     * *destination's* `NavBackStackEntry`, so that was a **fresh** `QuranViewModel`, not the
     * reader's: its `readerState.ayahs` was empty, `playAyahAudio` therefore built an empty
     * playlist, and `QuranAudioManager.playFromAyah` bails on `indexOfFirst(...) == -1`. The
     * preview button set its spinner and played silence.
     *
     * Handled here against an explicit single-item playlist, so it does not depend on anything
     * else having been loaded first.
     */
    data class PreviewReciter(val reciterId: String) : SettingsEvent

    /** Stop a running reciter preview. */
    data object StopReciterPreview : SettingsEvent
    data class SetShowTajweed(val enabled: Boolean) : SettingsEvent
    data class SetTajweedUnderline(val enabled: Boolean) : SettingsEvent
    data class SetMushafScript(val script: MushafScript) : SettingsEvent

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

    // Actions
    data object ResetToDefaults : SettingsEvent
    data object TestNotification : SettingsEvent
    data object TestAllNotifications : SettingsEvent
    data object ResetNotifications : SettingsEvent
    data object DeleteAllData : SettingsEvent
}
