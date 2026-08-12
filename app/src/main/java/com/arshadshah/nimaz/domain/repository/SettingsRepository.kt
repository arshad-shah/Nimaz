package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.repository.settings.AiSettings
import com.arshadshah.nimaz.domain.repository.settings.AppSettings
import com.arshadshah.nimaz.domain.repository.settings.DuaDisplaySettings
import com.arshadshah.nimaz.domain.repository.settings.HadithDisplaySettings
import com.arshadshah.nimaz.domain.repository.settings.LocationSettings
import com.arshadshah.nimaz.domain.repository.settings.MoreSettings
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import com.arshadshah.nimaz.domain.repository.settings.TasbihSettings
import com.arshadshah.nimaz.domain.repository.settings.HijriSettings
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import kotlinx.coroutines.flow.Flow

/**
 * App-wide user settings/preferences. Implemented by the DataStore-backed
 * PreferencesDataStore in the data layer; presentation depends on this interface.
 *
 * The feature-scoped seams it extends live in [com.arshadshah.nimaz.domain.repository.settings].
 * A ViewModel that needs one feature's preferences injects that seam, not this whole
 * surface — only `SettingsViewModel`, which edits nearly all of it, takes this type.
 */
interface SettingsRepository :
    QuranPreferences,
    HadithDisplaySettings,
    DuaDisplaySettings,
    TasbihSettings,
    ZakatSettings,
    HijriSettings,
    AiSettings,
    LocationSettings,
    MoreSettings,
    AppSettings {
    suspend fun clearAllData()
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

    // `hijriDayOffset` itself is on the HijriSettings seam above — three unrelated readers want
    // that one flow and none of them wants the rest of this interface. The writer stays here,
    // because only the settings screen writes it.
    suspend fun setHijriDayOffset(days: Int)
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

    // Per-prayer alert style and reminder, keyed by prayer name ("fajr", "dhuhr", "asr",
    // "maghrib", "isha" — sunrise has neither). These supersede the global adhan pair and
    // the single pre-adhan reminder above, which stay only so the one-time migration has
    // something to read.

    /** How a prayer announces itself: the adhan, the standard tone, or nothing. */
    fun prayerAlertStyle(prayer: String): Flow<PrayerAlertStyle>
    suspend fun setPrayerAlertStyle(prayer: String, style: PrayerAlertStyle)

    /** Whether this prayer gets a reminder ahead of its time. */
    fun prayerReminderEnabled(prayer: String): Flow<Boolean>
    suspend fun setPrayerReminderEnabled(prayer: String, enabled: Boolean)

    /** How many minutes before the prayer that reminder lands. */
    fun prayerReminderMinutes(prayer: String): Flow<Int>
    suspend fun setPrayerReminderMinutes(prayer: String, minutes: Int)

    /**
     * Carries an existing install from the global adhan/pre-adhan preferences onto the
     * per-prayer ones above. Runs at most once; safe to call on every start.
     */
    suspend fun migratePrayerNotificationPreferences()
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
    suspend fun exportAllPreferences(): Map<String, String>
    suspend fun importPreferences(prefsMap: Map<String, String>)
}
