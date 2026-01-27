package com.arshadshah.nimaz.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
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
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        // Language
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ARABIC_FONT_SIZE = stringPreferencesKey("arabic_font_size") // "small", "medium", "large"

        // Prayer Settings
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val ASR_CALCULATION = stringPreferencesKey("asr_calculation")
        val HIGH_LATITUDE_RULE = stringPreferencesKey("high_latitude_rule")
        val CURRENT_LOCATION_ID = longPreferencesKey("current_location_id")

        // Notifications
        val PRAYER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("prayer_notifications_enabled")
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val PRE_NOTIFICATION_MINUTES = stringPreferencesKey("pre_notification_minutes")

        // Quran Settings
        val QURAN_TRANSLATOR_ID = stringPreferencesKey("quran_translator_id")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val SHOW_TRANSLITERATION = booleanPreferencesKey("show_transliteration")

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

    val currentLocationId: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENT_LOCATION_ID]
    }

    suspend fun setCurrentLocationId(id: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_LOCATION_ID] = id
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

    // Quran Settings
    val quranTranslatorId: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.QURAN_TRANSLATOR_ID] ?: "en.sahih"
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
            quranTranslatorId = preferences[PreferencesKeys.QURAN_TRANSLATOR_ID] ?: "en.sahih",
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
