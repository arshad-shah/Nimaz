package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * App-wide user settings/preferences. Implemented by the DataStore-backed
 * PreferencesDataStore in the data layer; presentation depends on this interface.
 */
interface SettingsRepository {
    suspend fun clearAllData()
    val onboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    val themeMode: Flow<String>
    suspend fun setThemeMode(mode: String)
    val dynamicColor: Flow<Boolean>
    suspend fun setDynamicColor(enabled: Boolean)
    val showIslamicPatterns: Flow<Boolean>
    suspend fun setShowIslamicPatterns(enabled: Boolean)

    /** The ornament style, as a [NimazPatternStyle] name; "NONE" means off. */
    val patternStyle: Flow<String>
    suspend fun setPatternStyle(style: String)
    val animationsEnabled: Flow<Boolean>
    suspend fun setAnimationsEnabled(enabled: Boolean)
    val showCountdown: Flow<Boolean>
    suspend fun setShowCountdown(enabled: Boolean)
    val showQuickActions: Flow<Boolean>
    suspend fun setShowQuickActions(enabled: Boolean)
    val hapticFeedback: Flow<Boolean>
    suspend fun setHapticFeedback(enabled: Boolean)
    val use24HourFormat: Flow<Boolean>
    suspend fun setUse24HourFormat(enabled: Boolean)
    val useHijriPrimary: Flow<Boolean>
    suspend fun setUseHijriPrimary(enabled: Boolean)

    /** Signed day offset (-2..+2, default 0) applied to the tabular Hijri date to correct
     *  for local moon-sighting differences (see HijriDateCalculator.today(offsetDays)). */
    val hijriDayOffset: Flow<Int>
    suspend fun setHijriDayOffset(days: Int)
    val appLanguage: Flow<String>
    suspend fun setAppLanguage(language: String)

    /** Last content patch applied. See data/local/content/ContentPatchSeeder. */
    val contentPatchVersion: Flow<Int>
    suspend fun setContentPatchVersion(version: Int)
    val tasbihBeadMode: Flow<Boolean>
    suspend fun setTasbihBeadMode(enabled: Boolean)
    val tasbihBeadDesign: Flow<String>
    suspend fun setTasbihBeadDesign(key: String)
    val tasbihSelectedPresetId: Flow<Long>
    suspend fun setTasbihSelectedPresetId(id: Long)
    val tasbihPresetSeedVersion: Flow<Int>
    suspend fun setTasbihPresetSeedVersion(version: Int)
    val tasbihFavorites: Flow<Set<String>>
    suspend fun setTasbihFavorites(ids: Set<String>)
    val tasbihLeftHanded: Flow<Boolean>
    suspend fun setTasbihLeftHanded(enabled: Boolean)
    val arabicFontSize: Flow<String>
    suspend fun setArabicFontSize(size: String)
    val calculationMethod: Flow<String>
    suspend fun setCalculationMethod(method: String)
    val asrCalculation: Flow<String>
    suspend fun setAsrCalculation(calculation: String)
    val highLatitudeRule: Flow<String>
    suspend fun setHighLatitudeRule(rule: String)
    val currentLocationId: Flow<Long?>
    suspend fun setCurrentLocationId(id: Long)
    val fajrAdjustment: Flow<Int>
    val sunriseAdjustment: Flow<Int>
    val dhuhrAdjustment: Flow<Int>
    val asrAdjustment: Flow<Int>
    val maghribAdjustment: Flow<Int>
    val ishaAdjustment: Flow<Int>
    suspend fun setPrayerAdjustment(prayer: String, minutes: Int)
    val prayerNotificationsEnabled: Flow<Boolean>
    suspend fun setPrayerNotificationsEnabled(enabled: Boolean)
    val adhanEnabled: Flow<Boolean>
    suspend fun setAdhanEnabled(enabled: Boolean)
    val selectedAdhanSound: Flow<String>
    suspend fun setSelectedAdhanSound(sound: String)
    val fajrNotificationEnabled: Flow<Boolean>
    val sunriseNotificationEnabled: Flow<Boolean>
    val dhuhrNotificationEnabled: Flow<Boolean>
    val asrNotificationEnabled: Flow<Boolean>
    val maghribNotificationEnabled: Flow<Boolean>
    val ishaNotificationEnabled: Flow<Boolean>
    suspend fun setPrayerNotificationEnabled(prayer: String, enabled: Boolean)
    val fajrAdhanEnabled: Flow<Boolean>
    val dhuhrAdhanEnabled: Flow<Boolean>
    val asrAdhanEnabled: Flow<Boolean>
    val maghribAdhanEnabled: Flow<Boolean>
    val ishaAdhanEnabled: Flow<Boolean>
    suspend fun setPrayerAdhanEnabled(prayer: String, enabled: Boolean)
    fun isAdhanEnabledForPrayer(prayer: String): Flow<Boolean>
    val adhanRespectDnd: Flow<Boolean>
    suspend fun setAdhanRespectDnd(enabled: Boolean)
    val notificationVibration: Flow<Boolean>
    suspend fun setNotificationVibration(enabled: Boolean)
    val notificationReminderMinutes: Flow<Int>
    suspend fun setNotificationReminderMinutes(minutes: Int)
    val showReminderBefore: Flow<Boolean>
    suspend fun setShowReminderBefore(enabled: Boolean)
    val persistentNotification: Flow<Boolean>
    suspend fun setPersistentNotification(enabled: Boolean)
    val fridayReminderEnabled: Flow<Boolean>
    suspend fun setFridayReminderEnabled(enabled: Boolean)
    val fridayReminderMinutes: Flow<Int>
    suspend fun setFridayReminderMinutes(minutes: Int)
    val khatamReminderEnabled: Flow<Boolean>
    suspend fun setKhatamReminderEnabled(enabled: Boolean)

    /** Time of day for the khatam reminder, stored as "HH:mm". */
    val khatamReminderTime: Flow<String>
    suspend fun setKhatamReminderTime(time: String)

    // Extended worship reminders (Tahajjud, Witr, Suhoor, Iftar, …), keyed by
    // WorshipReminderType.key. One uniform surface for all 11 reminders. Default off.
    fun worshipReminderEnabled(key: String): Flow<Boolean>
    suspend fun setWorshipReminderEnabled(key: String, enabled: Boolean)

    /** Editable offset (minutes) for reminders that lead/lag their anchor prayer. */
    fun worshipReminderOffset(key: String, default: Int): Flow<Int>
    suspend fun setWorshipReminderOffset(key: String, minutes: Int)

    /** Optional per-reminder mode string (e.g. Witr: "after_isha" | "before_fajr"). */
    fun worshipReminderMode(key: String, default: String): Flow<String>
    suspend fun setWorshipReminderMode(key: String, mode: String)
    val quranTranslatorId: Flow<String>
    suspend fun setQuranTranslatorId(translatorId: String)
    val showTranslation: Flow<Boolean>
    suspend fun setShowTranslation(show: Boolean)
    val showTransliteration: Flow<Boolean>
    suspend fun setShowTransliteration(show: Boolean)
    val selectedReciterId: Flow<String?>
    suspend fun setSelectedReciterId(reciterId: String?)
    val quranArabicFont: Flow<String>
    suspend fun setQuranArabicFont(fontId: String)

    /** The Mushaf edition/layout for the page reader, as a [com.arshadshah.nimaz.domain.model.MushafScript]
     *  name ("MADANI" = default Uthmani/604; "INDOPAK_16" = 16-line IndoPak/548). */
    val quranMushafScript: Flow<String>
    suspend fun setQuranMushafScript(script: String)
    val quranArabicFontSize: Flow<Float>
    suspend fun setQuranArabicFontSize(size: Float)
    val quranTranslationFontSize: Flow<Float>
    suspend fun setQuranTranslationFontSize(size: Float)
    val continuousReading: Flow<Boolean>
    suspend fun setContinuousReading(enabled: Boolean)
    val keepScreenOn: Flow<Boolean>
    suspend fun setKeepScreenOn(enabled: Boolean)
    val showTajweed: Flow<Boolean>
    suspend fun setShowTajweed(enabled: Boolean)
    val tajweedUnderline: Flow<Boolean>
    suspend fun setTajweedUnderline(enabled: Boolean)
    val duaArabicFont: Flow<String>
    suspend fun setDuaArabicFont(fontId: String)
    val duaArabicFontSize: Flow<Float>
    suspend fun setDuaArabicFontSize(size: Float)
    val duaTranslationFontSize: Flow<Float>
    suspend fun setDuaTranslationFontSize(size: Float)
    val duaShowArabic: Flow<Boolean>
    suspend fun setDuaShowArabic(show: Boolean)
    val duaShowTransliteration: Flow<Boolean>
    suspend fun setDuaShowTransliteration(show: Boolean)
    val duaShowTranslation: Flow<Boolean>
    suspend fun setDuaShowTranslation(show: Boolean)
    val duaCategoriesSortAlphabetical: Flow<Boolean>
    suspend fun setDuaCategoriesSortAlphabetical(enabled: Boolean)
    val hadithArabicFont: Flow<String>
    suspend fun setHadithArabicFont(fontId: String)
    val hadithArabicFontSize: Flow<Float>
    suspend fun setHadithArabicFontSize(size: Float)
    val hadithTranslationFontSize: Flow<Float>
    suspend fun setHadithTranslationFontSize(size: Float)
    val hadithShowArabic: Flow<Boolean>
    suspend fun setHadithShowArabic(show: Boolean)
    val hadithShowTranslation: Flow<Boolean>
    suspend fun setHadithShowTranslation(show: Boolean)
    val hadithShowGrade: Flow<Boolean>
    suspend fun setHadithShowGrade(show: Boolean)
    val hadithShowChain: Flow<Boolean>
    suspend fun setHadithShowChain(show: Boolean)
    val tasbihVibrationEnabled: Flow<Boolean>
    suspend fun setTasbihVibrationEnabled(enabled: Boolean)
    val tasbihSoundEnabled: Flow<Boolean>
    suspend fun setTasbihSoundEnabled(enabled: Boolean)
    val latitude: Flow<Double>
    val longitude: Flow<Double>
    val locationName: Flow<String>
    suspend fun updateLocation(latitude: Double, longitude: Double, name: String)

    // AI — Ask with Proof (opt-in)
    val aiAskEnabled: Flow<Boolean>
    suspend fun setAiAskEnabled(enabled: Boolean)
    val aiConsentTimestamp: Flow<Long>
    suspend fun setAiConsentTimestamp(timestamp: Long)
    val aiHistoryEnabled: Flow<Boolean>
    suspend fun setAiHistoryEnabled(enabled: Boolean)
    val aiAskHintDismissed: Flow<Boolean>
    suspend fun setAiAskHintDismissed(dismissed: Boolean)
    val aiQuestionHistory: Flow<String>
    suspend fun setAiQuestionHistory(json: String)
    suspend fun exportAllPreferences(): Map<String, String>
    suspend fun importPreferences(prefsMap: Map<String, String>)
    val userPreferences: Flow<UserPreferences>
}
