package com.arshadshah.nimaz.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nimaz_preferences")

@Singleton
class PreferencesDataStore @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private object PreferencesKeys {
        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Theme
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        // Appearance
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val APP_ICON = stringPreferencesKey("app_icon")
        val SHOW_ISLAMIC_PATTERNS = booleanPreferencesKey("show_islamic_patterns")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")

        // Display
        val SHOW_COUNTDOWN = booleanPreferencesKey("show_countdown")
        val SHOW_QUICK_ACTIONS = booleanPreferencesKey("show_quick_actions")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val USE_HIJRI_PRIMARY = booleanPreferencesKey("use_hijri_primary")

        // Language
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ARABIC_FONT_SIZE = stringPreferencesKey("arabic_font_size")

        // Prayer Settings
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val ASR_CALCULATION = stringPreferencesKey("asr_calculation")
        val HIGH_LATITUDE_RULE = stringPreferencesKey("high_latitude_rule")
        val CURRENT_LOCATION_ID = longPreferencesKey("current_location_id")

        // Prayer Adjustments
        val FAJR_ADJUSTMENT = intPreferencesKey("fajr_adjustment")
        val SUNRISE_ADJUSTMENT = intPreferencesKey("sunrise_adjustment")
        val DHUHR_ADJUSTMENT = intPreferencesKey("dhuhr_adjustment")
        val ASR_ADJUSTMENT = intPreferencesKey("asr_adjustment")
        val MAGHRIB_ADJUSTMENT = intPreferencesKey("maghrib_adjustment")
        val ISHA_ADJUSTMENT = intPreferencesKey("isha_adjustment")

        // Notifications
        val PRAYER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("prayer_notifications_enabled")
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val PRE_NOTIFICATION_MINUTES = stringPreferencesKey("pre_notification_minutes")
        val NOTIFICATION_VIBRATION = booleanPreferencesKey("notification_vibration")
        val NOTIFICATION_REMINDER_MINUTES = intPreferencesKey("notification_reminder_minutes")
        val SHOW_REMINDER_BEFORE = booleanPreferencesKey("show_reminder_before")
        val PERSISTENT_NOTIFICATION = booleanPreferencesKey("persistent_notification")
        val SELECTED_ADHAN_SOUND = stringPreferencesKey("selected_adhan_sound")
        val FAJR_NOTIFICATION_ENABLED = booleanPreferencesKey("fajr_notification_enabled")
        val SUNRISE_NOTIFICATION_ENABLED = booleanPreferencesKey("sunrise_notification_enabled")
        val DHUHR_NOTIFICATION_ENABLED = booleanPreferencesKey("dhuhr_notification_enabled")
        val ASR_NOTIFICATION_ENABLED = booleanPreferencesKey("asr_notification_enabled")
        val MAGHRIB_NOTIFICATION_ENABLED = booleanPreferencesKey("maghrib_notification_enabled")
        val ISHA_NOTIFICATION_ENABLED = booleanPreferencesKey("isha_notification_enabled")

        // Per-prayer adhan/sound enabled
        val FAJR_ADHAN_ENABLED = booleanPreferencesKey("fajr_adhan_enabled")
        val DHUHR_ADHAN_ENABLED = booleanPreferencesKey("dhuhr_adhan_enabled")
        val ASR_ADHAN_ENABLED = booleanPreferencesKey("asr_adhan_enabled")
        val MAGHRIB_ADHAN_ENABLED = booleanPreferencesKey("maghrib_adhan_enabled")
        val ISHA_ADHAN_ENABLED = booleanPreferencesKey("isha_adhan_enabled")
        // Note: Sunrise always uses beep only, no full adhan option

        // Quran Settings
        val QURAN_TRANSLATOR_ID = stringPreferencesKey("quran_translator_id")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val SHOW_TRANSLITERATION = booleanPreferencesKey("show_transliteration")
        val SELECTED_RECITER_ID = stringPreferencesKey("selected_reciter_id")
        val QURAN_ARABIC_FONT_SIZE = floatPreferencesKey("quran_arabic_font_size")
        val QURAN_TRANSLATION_FONT_SIZE = floatPreferencesKey("quran_translation_font_size")
        val CONTINUOUS_READING = booleanPreferencesKey("continuous_reading")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_TAJWEED = booleanPreferencesKey("show_tajweed")

        // Tasbih Settings
        val TASBIH_VIBRATION_ENABLED = booleanPreferencesKey("tasbih_vibration_enabled")
        val TASBIH_SOUND_ENABLED = booleanPreferencesKey("tasbih_sound_enabled")

        // Location
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val LOCATION_NAME = stringPreferencesKey("location_name")
    }

    // Onboarding
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    // Theme
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    val dynamicColor: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    // Appearance
    val accentColor: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCENT_COLOR] ?: "Teal"
    }

    suspend fun setAccentColor(color: String) {
        dataStore.edit { it[PreferencesKeys.ACCENT_COLOR] = color }
    }

    val appIcon: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_ICON] ?: "Default"
    }

    suspend fun setAppIcon(icon: String) {
        dataStore.edit { it[PreferencesKeys.APP_ICON] = icon }
    }

    val showIslamicPatterns: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_ISLAMIC_PATTERNS] ?: true
    }

    suspend fun setShowIslamicPatterns(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_ISLAMIC_PATTERNS] = enabled }
    }

    val animationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ANIMATIONS_ENABLED] ?: true
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ANIMATIONS_ENABLED] = enabled }
    }

    // Display
    val showCountdown: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_COUNTDOWN] ?: true
    }

    suspend fun setShowCountdown(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_COUNTDOWN] = enabled }
    }

    val showQuickActions: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_QUICK_ACTIONS] ?: true
    }

    suspend fun setShowQuickActions(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_QUICK_ACTIONS] = enabled }
    }

    val hapticFeedback: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.HAPTIC_FEEDBACK] = enabled }
    }

    val use24HourFormat: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USE_24_HOUR_FORMAT] ?: false
    }

    suspend fun setUse24HourFormat(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.USE_24_HOUR_FORMAT] = enabled }
    }

    val useHijriPrimary: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USE_HIJRI_PRIMARY] ?: false
    }

    suspend fun setUseHijriPrimary(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.USE_HIJRI_PRIMARY] = enabled }
    }

    // Language
    val appLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "en"
    }

    suspend fun setAppLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language
        }
    }

    val arabicFontSize: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ARABIC_FONT_SIZE] ?: "medium"
    }

    suspend fun setArabicFontSize(size: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARABIC_FONT_SIZE] = size
        }
    }

    // Prayer Settings
    val calculationMethod: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CALCULATION_METHOD] ?: "MUSLIM_WORLD_LEAGUE"
    }

    suspend fun setCalculationMethod(method: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALCULATION_METHOD] = method
        }
    }

    val asrCalculation: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ASR_CALCULATION] ?: "standard"
    }

    suspend fun setAsrCalculation(calculation: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASR_CALCULATION] = calculation
        }
    }

    val highLatitudeRule: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HIGH_LATITUDE_RULE] ?: "MIDDLE_OF_NIGHT"
    }

    suspend fun setHighLatitudeRule(rule: String) {
        dataStore.edit { it[PreferencesKeys.HIGH_LATITUDE_RULE] = rule }
    }

    val currentLocationId: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENT_LOCATION_ID]
    }

    suspend fun setCurrentLocationId(id: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_LOCATION_ID] = id
        }
    }

    // Prayer Adjustments
    val fajrAdjustment: Flow<Int> = dataStore.data.map { it[PreferencesKeys.FAJR_ADJUSTMENT] ?: 0 }
    val sunriseAdjustment: Flow<Int> = dataStore.data.map { it[PreferencesKeys.SUNRISE_ADJUSTMENT] ?: 0 }
    val dhuhrAdjustment: Flow<Int> = dataStore.data.map { it[PreferencesKeys.DHUHR_ADJUSTMENT] ?: 0 }
    val asrAdjustment: Flow<Int> = dataStore.data.map { it[PreferencesKeys.ASR_ADJUSTMENT] ?: 0 }
    val maghribAdjustment: Flow<Int> = dataStore.data.map { it[PreferencesKeys.MAGHRIB_ADJUSTMENT] ?: 0 }
    val ishaAdjustment: Flow<Int> = dataStore.data.map { it[PreferencesKeys.ISHA_ADJUSTMENT] ?: 0 }

    suspend fun setPrayerAdjustment(prayer: String, minutes: Int) {
        dataStore.edit { prefs ->
            val key = when (prayer.lowercase()) {
                "fajr" -> PreferencesKeys.FAJR_ADJUSTMENT
                "sunrise" -> PreferencesKeys.SUNRISE_ADJUSTMENT
                "dhuhr" -> PreferencesKeys.DHUHR_ADJUSTMENT
                "asr" -> PreferencesKeys.ASR_ADJUSTMENT
                "maghrib" -> PreferencesKeys.MAGHRIB_ADJUSTMENT
                "isha" -> PreferencesKeys.ISHA_ADJUSTMENT
                else -> return@edit
            }
            prefs[key] = minutes
        }
    }

    // Notifications
    val prayerNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PRAYER_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setPrayerNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRAYER_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val adhanEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADHAN_ENABLED] ?: false
    }

    suspend fun setAdhanEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADHAN_ENABLED] = enabled
        }
    }

    val selectedAdhanSound: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_ADHAN_SOUND] ?: "MISHARY"
    }

    suspend fun setSelectedAdhanSound(sound: String) {
        dataStore.edit { it[PreferencesKeys.SELECTED_ADHAN_SOUND] = sound }
    }

    val fajrNotificationEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.FAJR_NOTIFICATION_ENABLED] ?: true }
    val sunriseNotificationEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.SUNRISE_NOTIFICATION_ENABLED] ?: false }
    val dhuhrNotificationEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DHUHR_NOTIFICATION_ENABLED] ?: true }
    val asrNotificationEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ASR_NOTIFICATION_ENABLED] ?: true }
    val maghribNotificationEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.MAGHRIB_NOTIFICATION_ENABLED] ?: true }
    val ishaNotificationEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ISHA_NOTIFICATION_ENABLED] ?: true }

    suspend fun setPrayerNotificationEnabled(prayer: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val key = when (prayer.lowercase()) {
                "fajr" -> PreferencesKeys.FAJR_NOTIFICATION_ENABLED
                "sunrise" -> PreferencesKeys.SUNRISE_NOTIFICATION_ENABLED
                "dhuhr" -> PreferencesKeys.DHUHR_NOTIFICATION_ENABLED
                "asr" -> PreferencesKeys.ASR_NOTIFICATION_ENABLED
                "maghrib" -> PreferencesKeys.MAGHRIB_NOTIFICATION_ENABLED
                "isha" -> PreferencesKeys.ISHA_NOTIFICATION_ENABLED
                else -> return@edit
            }
            prefs[key] = enabled
        }
    }

    // Per-prayer adhan enabled
    val fajrAdhanEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.FAJR_ADHAN_ENABLED] ?: true }
    val dhuhrAdhanEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DHUHR_ADHAN_ENABLED] ?: true }
    val asrAdhanEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ASR_ADHAN_ENABLED] ?: true }
    val maghribAdhanEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.MAGHRIB_ADHAN_ENABLED] ?: true }
    val ishaAdhanEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ISHA_ADHAN_ENABLED] ?: true }

    suspend fun setPrayerAdhanEnabled(prayer: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val key = when (prayer.lowercase()) {
                "fajr" -> PreferencesKeys.FAJR_ADHAN_ENABLED
                "dhuhr" -> PreferencesKeys.DHUHR_ADHAN_ENABLED
                "asr" -> PreferencesKeys.ASR_ADHAN_ENABLED
                "maghrib" -> PreferencesKeys.MAGHRIB_ADHAN_ENABLED
                "isha" -> PreferencesKeys.ISHA_ADHAN_ENABLED
                else -> return@edit
            }
            prefs[key] = enabled
        }
    }

    /**
     * Check if adhan is enabled for a specific prayer.
     * Sunrise always returns false (uses beep only).
     */
    fun isAdhanEnabledForPrayer(prayer: String): Flow<Boolean> {
        return when (prayer.lowercase()) {
            "fajr" -> fajrAdhanEnabled
            "dhuhr" -> dhuhrAdhanEnabled
            "asr" -> asrAdhanEnabled
            "maghrib" -> maghribAdhanEnabled
            "isha" -> ishaAdhanEnabled
            "sunrise" -> kotlinx.coroutines.flow.flowOf(false) // Sunrise never gets adhan
            else -> kotlinx.coroutines.flow.flowOf(false)
        }
    }

    val notificationVibration: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATION_VIBRATION] ?: true
    }

    suspend fun setNotificationVibration(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATION_VIBRATION] = enabled }
    }

    val notificationReminderMinutes: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATION_REMINDER_MINUTES] ?: 15
    }

    suspend fun setNotificationReminderMinutes(minutes: Int) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATION_REMINDER_MINUTES] = minutes }
    }

    val showReminderBefore: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_REMINDER_BEFORE] ?: true
    }

    suspend fun setShowReminderBefore(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_REMINDER_BEFORE] = enabled }
    }

    val persistentNotification: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PERSISTENT_NOTIFICATION] ?: false
    }

    suspend fun setPersistentNotification(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.PERSISTENT_NOTIFICATION] = enabled }
    }

    // Quran Settings
    val quranTranslatorId: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.QURAN_TRANSLATOR_ID] ?: "sahih_international"
    }

    suspend fun setQuranTranslatorId(translatorId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.QURAN_TRANSLATOR_ID] = translatorId
        }
    }

    val showTranslation: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_TRANSLATION] ?: true
    }

    suspend fun setShowTranslation(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TRANSLATION] = show
        }
    }

    val showTransliteration: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_TRANSLITERATION] ?: false
    }

    suspend fun setShowTransliteration(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TRANSLITERATION] = show
        }
    }

    val selectedReciterId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_RECITER_ID]
    }

    suspend fun setSelectedReciterId(reciterId: String?) {
        dataStore.edit {
            if (reciterId != null) it[PreferencesKeys.SELECTED_RECITER_ID] = reciterId
            else it.remove(PreferencesKeys.SELECTED_RECITER_ID)
        }
    }

    val quranArabicFontSize: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.QURAN_ARABIC_FONT_SIZE] ?: 28f
    }

    suspend fun setQuranArabicFontSize(size: Float) {
        dataStore.edit { it[PreferencesKeys.QURAN_ARABIC_FONT_SIZE] = size }
    }

    val quranTranslationFontSize: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.QURAN_TRANSLATION_FONT_SIZE] ?: 16f
    }

    suspend fun setQuranTranslationFontSize(size: Float) {
        dataStore.edit { it[PreferencesKeys.QURAN_TRANSLATION_FONT_SIZE] = size }
    }

    val continuousReading: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CONTINUOUS_READING] ?: true
    }

    suspend fun setContinuousReading(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.CONTINUOUS_READING] = enabled }
    }

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_ON] = enabled }
    }

    val showTajweed: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_TAJWEED] ?: false
    }

    suspend fun setShowTajweed(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_TAJWEED] = enabled }
    }

    // Tasbih Settings
    val tasbihVibrationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TASBIH_VIBRATION_ENABLED] ?: true
    }

    suspend fun setTasbihVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASBIH_VIBRATION_ENABLED] = enabled
        }
    }

    val tasbihSoundEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TASBIH_SOUND_ENABLED] ?: true
    }

    suspend fun setTasbihSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASBIH_SOUND_ENABLED] = enabled
        }
    }

    // Location
    val latitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LATITUDE] ?: 0.0
    }

    val longitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LONGITUDE] ?: 0.0
    }

    val locationName: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LOCATION_NAME] ?: ""
    }

    suspend fun updateLocation(latitude: Double, longitude: Double, name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LATITUDE] = latitude
            preferences[PreferencesKeys.LONGITUDE] = longitude
            preferences[PreferencesKeys.LOCATION_NAME] = name
        }
    }

    // Combined user preferences
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "system",
            dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false,
            appLanguage = preferences[PreferencesKeys.APP_LANGUAGE] ?: "en",
            calculationMethod = preferences[PreferencesKeys.CALCULATION_METHOD] ?: "MUSLIM_WORLD_LEAGUE",
            asrCalculation = preferences[PreferencesKeys.ASR_CALCULATION] ?: "standard",
            latitude = preferences[PreferencesKeys.LATITUDE] ?: 0.0,
            longitude = preferences[PreferencesKeys.LONGITUDE] ?: 0.0,
            locationName = preferences[PreferencesKeys.LOCATION_NAME] ?: "",
            prayerNotificationsEnabled = preferences[PreferencesKeys.PRAYER_NOTIFICATIONS_ENABLED] ?: true,
            quranTranslatorId = preferences[PreferencesKeys.QURAN_TRANSLATOR_ID] ?: "sahih_international",
            showTranslation = preferences[PreferencesKeys.SHOW_TRANSLATION] ?: true
        )
    }
}

data class UserPreferences(
    val onboardingCompleted: Boolean,
    val themeMode: String,
    val dynamicColor: Boolean,
    val appLanguage: String,
    val calculationMethod: String,
    val asrCalculation: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val prayerNotificationsEnabled: Boolean,
    val quranTranslatorId: String,
    val showTranslation: Boolean
)
