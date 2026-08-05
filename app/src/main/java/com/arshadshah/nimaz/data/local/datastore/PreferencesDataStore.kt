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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nimaz_preferences")

@Singleton
class PreferencesDataStore @Inject constructor(
    private val context: Context
) : SettingsRepository {
    private val dataStore = context.dataStore

    /** Observe a preference, falling back to [default] when it has not been set. */
    private fun <T> preference(key: Preferences.Key<T>, default: T): Flow<T> =
        dataStore.data.map { it[key] ?: default }

    /** Observe a nullable preference (no default — emits null until set). */
    private fun <T> preference(key: Preferences.Key<T>): Flow<T?> =
        dataStore.data.map { it[key] }

    /** Persist a single preference value. */
    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    // Keys
    private object PreferencesKeys {
        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Theme
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        // Appearance
        val SHOW_ISLAMIC_PATTERNS = booleanPreferencesKey("show_islamic_patterns")

        // Which ornament style; stored as the NimazPatternStyle enum name. "NONE"
        // means the reader turned the ornament off, so this doubles as the on/off
        // state — SHOW_ISLAMIC_PATTERNS is kept only for import/export compatibility.
        val PATTERN_STYLE = stringPreferencesKey("pattern_style")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")

        // Display
        val SHOW_COUNTDOWN = booleanPreferencesKey("show_countdown")
        val SHOW_QUICK_ACTIONS = booleanPreferencesKey("show_quick_actions")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val TASBIH_BEAD_MODE = booleanPreferencesKey("tasbih_bead_mode")
        val TASBIH_BEAD_DESIGN = stringPreferencesKey("tasbih_bead_design")
        val TASBIH_SELECTED_PRESET = longPreferencesKey("tasbih_selected_preset")
        val TASBIH_PRESET_SEED_VERSION = intPreferencesKey("tasbih_preset_seed_version")
        val TASBIH_FAVORITES = stringSetPreferencesKey("tasbih_favorites")
        val TASBIH_LEFT_HANDED = booleanPreferencesKey("tasbih_left_handed")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val USE_HIJRI_PRIMARY = booleanPreferencesKey("use_hijri_primary")

        // Signed day offset (-2..+2) correcting the tabular Hijri date for local
        // moon-sighting differences.
        val HIJRI_DAY_OFFSET = intPreferencesKey("hijri_day_offset")

        // Language
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ARABIC_FONT_SIZE = stringPreferencesKey("arabic_font_size")

        // Help content (data-driven; bumped when help.json content changes)


        // Line-accurate mushaf editions and Quran translations are each seeded lazily, per
        // item, so their versions are stored as "<key>:<version>" string sets rather than an
        // int key per item — adding an edition or a translation touches no preference code.

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
        val FRIDAY_REMINDER_ENABLED = booleanPreferencesKey("friday_reminder_enabled")
        val FRIDAY_REMINDER_MINUTES = intPreferencesKey("friday_reminder_minutes")
        val KHATAM_REMINDER_ENABLED = booleanPreferencesKey("khatam_reminder_enabled")
        val KHATAM_REMINDER_TIME = stringPreferencesKey("khatam_reminder_time")
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
        val ADHAN_RESPECT_DND = booleanPreferencesKey("adhan_respect_dnd")

        // Which split of the per-prayer alert style / reminder preferences this install has
        // been through. See PrayerNotificationPrefsMigration.
        val NOTIFICATION_PREFS_MIGRATION_VERSION =
            intPreferencesKey("notification_prefs_migration_version")

        // Quran Settings
        val QURAN_TRANSLATOR_ID = stringPreferencesKey("quran_translator_id")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val SHOW_TRANSLITERATION = booleanPreferencesKey("show_transliteration")
        val SELECTED_RECITER_ID = stringPreferencesKey("selected_reciter_id")
        val QURAN_ARABIC_FONT = stringPreferencesKey("quran_arabic_font")

        // Which Mushaf edition/layout the page reader renders; stored as the MushafScript
        // enum name. "MADANI" (Uthmani, 604 pages) is the default; "INDOPAK_16" selects the
        // line-accurate 16-line IndoPak view (548 pages). See MushafScript / issue #270.
        val QURAN_MUSHAF_SCRIPT = stringPreferencesKey("quran_mushaf_script")
        val QURAN_ARABIC_FONT_SIZE = floatPreferencesKey("quran_arabic_font_size")
        val QURAN_TRANSLATION_FONT_SIZE = floatPreferencesKey("quran_translation_font_size")
        val ZAKAT_GOLD_PRICE_PER_GRAM = doublePreferencesKey("zakat_gold_price_per_gram")
        val ZAKAT_SILVER_PRICE_PER_GRAM = doublePreferencesKey("zakat_silver_price_per_gram")
        val ZAKAT_CURRENCY = stringPreferencesKey("zakat_currency")
        val CONTINUOUS_READING = booleanPreferencesKey("continuous_reading")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_TAJWEED = booleanPreferencesKey("show_tajweed")
        val TAJWEED_UNDERLINE = booleanPreferencesKey("tajweed_underline")

        // Dua Settings
        val DUA_ARABIC_FONT = stringPreferencesKey("dua_arabic_font")
        val DUA_ARABIC_FONT_SIZE = floatPreferencesKey("dua_arabic_font_size")
        val DUA_TRANSLATION_FONT_SIZE = floatPreferencesKey("dua_translation_font_size")
        val DUA_SHOW_ARABIC = booleanPreferencesKey("dua_show_arabic")
        val DUA_SHOW_TRANSLITERATION = booleanPreferencesKey("dua_show_transliteration")
        val DUA_SHOW_TRANSLATION = booleanPreferencesKey("dua_show_translation")
        val DUA_CATEGORIES_SORT_ALPHABETICAL =
            booleanPreferencesKey("dua_categories_sort_alphabetical")

        // Hadith Settings
        val HADITH_ARABIC_FONT = stringPreferencesKey("hadith_arabic_font")
        val HADITH_ARABIC_FONT_SIZE = floatPreferencesKey("hadith_arabic_font_size")
        val HADITH_TRANSLATION_FONT_SIZE = floatPreferencesKey("hadith_translation_font_size")
        val HADITH_SHOW_ARABIC = booleanPreferencesKey("hadith_show_arabic")
        val HADITH_SHOW_TRANSLATION = booleanPreferencesKey("hadith_show_translation")
        val HADITH_SHOW_GRADE = booleanPreferencesKey("hadith_show_grade")
        val HADITH_SHOW_CHAIN = booleanPreferencesKey("hadith_show_chain")

        // Tasbih Settings
        val TASBIH_VIBRATION_ENABLED = booleanPreferencesKey("tasbih_vibration_enabled")
        val TASBIH_SOUND_ENABLED = booleanPreferencesKey("tasbih_sound_enabled")

        // Location
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val LOCATION_NAME = stringPreferencesKey("location_name")

        // AI — Ask with Proof (opt-in; all off/neutral by default)
        val AI_ASK_ENABLED = booleanPreferencesKey("ai_ask_enabled")
        val AI_CONSENT_TIMESTAMP = longPreferencesKey("ai_consent_timestamp")
        val AI_HISTORY_ENABLED = booleanPreferencesKey("ai_history_enabled")
        val AI_ASK_HINT_DISMISSED = booleanPreferencesKey("ai_ask_hint_dismissed")

        // JSON-encoded List<String> of recent AI questions (only persisted when
        // AI_HISTORY_ENABLED). See recent-searches mechanism in SearchViewModel.
        val AI_QUESTION_HISTORY = stringPreferencesKey("ai_question_history")
    }

    override suspend fun clearAllData() {
        dataStore.edit { it.clear() }
    }

    // Onboarding
    override val onboardingCompleted: Flow<Boolean> =
        preference(PreferencesKeys.ONBOARDING_COMPLETED, false)

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        put(PreferencesKeys.ONBOARDING_COMPLETED, completed)

    // Theme
    override val themeMode: Flow<String> = preference(PreferencesKeys.THEME_MODE, "system")

    override suspend fun setThemeMode(mode: String) = put(PreferencesKeys.THEME_MODE, mode)

    override val dynamicColor: Flow<Boolean> = preference(PreferencesKeys.DYNAMIC_COLOR, false)

    override suspend fun setDynamicColor(enabled: Boolean) =
        put(PreferencesKeys.DYNAMIC_COLOR, enabled)

    // Appearance
    override val showIslamicPatterns: Flow<Boolean> =
        preference(PreferencesKeys.SHOW_ISLAMIC_PATTERNS, true)

    override suspend fun setShowIslamicPatterns(enabled: Boolean) =
        put(PreferencesKeys.SHOW_ISLAMIC_PATTERNS, enabled)

    // Default matches NimazPatternStyle.CORNER_MEDALLION. Stored as a raw string so
    // the data layer stays free of the presentation enum; presentation maps it.
    override val patternStyle: Flow<String> =
        preference(PreferencesKeys.PATTERN_STYLE, "CORNER_MEDALLION")

    override suspend fun setPatternStyle(style: String) =
        put(PreferencesKeys.PATTERN_STYLE, style)

    override val animationsEnabled: Flow<Boolean> =
        preference(PreferencesKeys.ANIMATIONS_ENABLED, true)

    override suspend fun setAnimationsEnabled(enabled: Boolean) =
        put(PreferencesKeys.ANIMATIONS_ENABLED, enabled)

    // Display
    override val showCountdown: Flow<Boolean> = preference(PreferencesKeys.SHOW_COUNTDOWN, true)

    override suspend fun setShowCountdown(enabled: Boolean) =
        put(PreferencesKeys.SHOW_COUNTDOWN, enabled)

    override val showQuickActions: Flow<Boolean> =
        preference(PreferencesKeys.SHOW_QUICK_ACTIONS, true)

    override suspend fun setShowQuickActions(enabled: Boolean) =
        put(PreferencesKeys.SHOW_QUICK_ACTIONS, enabled)

    override val hapticFeedback: Flow<Boolean> = preference(PreferencesKeys.HAPTIC_FEEDBACK, true)

    override suspend fun setHapticFeedback(enabled: Boolean) =
        put(PreferencesKeys.HAPTIC_FEEDBACK, enabled)

    override val use24HourFormat: Flow<Boolean> =
        preference(PreferencesKeys.USE_24_HOUR_FORMAT, false)

    override suspend fun setUse24HourFormat(enabled: Boolean) =
        put(PreferencesKeys.USE_24_HOUR_FORMAT, enabled)

    override val useHijriPrimary: Flow<Boolean> =
        preference(PreferencesKeys.USE_HIJRI_PRIMARY, false)

    override suspend fun setUseHijriPrimary(enabled: Boolean) =
        put(PreferencesKeys.USE_HIJRI_PRIMARY, enabled)

    override val hijriDayOffset: Flow<Int> =
        preference(PreferencesKeys.HIJRI_DAY_OFFSET, 0)

    override suspend fun setHijriDayOffset(days: Int) =
        put(PreferencesKeys.HIJRI_DAY_OFFSET, days.coerceIn(-2, 2))

    // Language
    override val appLanguage: Flow<String> = preference(PreferencesKeys.APP_LANGUAGE, "en")

    override suspend fun setAppLanguage(language: String) =
        put(PreferencesKeys.APP_LANGUAGE, language)

    // Tasbih counter style — true = bead strand, false = classic circle.
    override val tasbihBeadMode: Flow<Boolean> = preference(PreferencesKeys.TASBIH_BEAD_MODE, false)

    override suspend fun setTasbihBeadMode(enabled: Boolean) =
        put(PreferencesKeys.TASBIH_BEAD_MODE, enabled)

    // Tasbih bead design — stable key of the chosen BeadDesign (default "wood").
    override val tasbihBeadDesign: Flow<String> =
        preference(PreferencesKeys.TASBIH_BEAD_DESIGN, "wood")

    override suspend fun setTasbihBeadDesign(key: String) =
        put(PreferencesKeys.TASBIH_BEAD_DESIGN, key)

    // Currently selected tasbih preset id (-1 = free count). Shared across screens
    // so the Choose-Dhikr picker can drive the counter on a separate back-stack entry.
    override val tasbihSelectedPresetId: Flow<Long> =
        preference(PreferencesKeys.TASBIH_SELECTED_PRESET, -1L)

    override suspend fun setTasbihSelectedPresetId(id: Long) =
        put(PreferencesKeys.TASBIH_SELECTED_PRESET, id)

    // Versioned runtime seed of new default presets (prepackaged DB only has the
    // original five). Bump the latest version in the VM when new defaults are added.
    override val tasbihPresetSeedVersion: Flow<Int> =
        preference(PreferencesKeys.TASBIH_PRESET_SEED_VERSION, 0)

    override suspend fun setTasbihPresetSeedVersion(version: Int) =
        put(PreferencesKeys.TASBIH_PRESET_SEED_VERSION, version)

    // Favourite preset ids (stored as strings).
    override val tasbihFavorites: Flow<Set<String>> =
        preference(PreferencesKeys.TASBIH_FAVORITES, emptySet())

    override suspend fun setTasbihFavorites(ids: Set<String>) =
        put(PreferencesKeys.TASBIH_FAVORITES, ids)

    // Bead strand handedness — false = right-handed (beads advance right→left),
    // true = left-handed (beads advance left→right).
    override val tasbihLeftHanded: Flow<Boolean> =
        preference(PreferencesKeys.TASBIH_LEFT_HANDED, false)

    override suspend fun setTasbihLeftHanded(enabled: Boolean) =
        put(PreferencesKeys.TASBIH_LEFT_HANDED, enabled)

    // Dua content version (0 = never seeded)
    // Qaida content version (0 = never seeded)
    override val arabicFontSize: Flow<String> =
        preference(PreferencesKeys.ARABIC_FONT_SIZE, "medium")

    override suspend fun setArabicFontSize(size: String) =
        put(PreferencesKeys.ARABIC_FONT_SIZE, size)

    // Prayer Settings
    override val calculationMethod: Flow<String> =
        preference(PreferencesKeys.CALCULATION_METHOD, "MUSLIM_WORLD_LEAGUE")

    override suspend fun setCalculationMethod(method: String) =
        put(PreferencesKeys.CALCULATION_METHOD, method)

    override val asrCalculation: Flow<String> =
        preference(PreferencesKeys.ASR_CALCULATION, "standard")

    override suspend fun setAsrCalculation(calculation: String) =
        put(PreferencesKeys.ASR_CALCULATION, calculation)

    override val highLatitudeRule: Flow<String> =
        preference(PreferencesKeys.HIGH_LATITUDE_RULE, "MIDDLE_OF_NIGHT")

    override suspend fun setHighLatitudeRule(rule: String) =
        put(PreferencesKeys.HIGH_LATITUDE_RULE, rule)

    override val currentLocationId: Flow<Long?> = preference(PreferencesKeys.CURRENT_LOCATION_ID)

    override suspend fun setCurrentLocationId(id: Long) =
        put(PreferencesKeys.CURRENT_LOCATION_ID, id)

    // Prayer Adjustments
    override val fajrAdjustment: Flow<Int> = preference(PreferencesKeys.FAJR_ADJUSTMENT, 0)
    override val sunriseAdjustment: Flow<Int> = preference(PreferencesKeys.SUNRISE_ADJUSTMENT, 0)
    override val dhuhrAdjustment: Flow<Int> = preference(PreferencesKeys.DHUHR_ADJUSTMENT, 0)
    override val asrAdjustment: Flow<Int> = preference(PreferencesKeys.ASR_ADJUSTMENT, 0)
    override val maghribAdjustment: Flow<Int> = preference(PreferencesKeys.MAGHRIB_ADJUSTMENT, 0)
    override val ishaAdjustment: Flow<Int> = preference(PreferencesKeys.ISHA_ADJUSTMENT, 0)

    override suspend fun setPrayerAdjustment(prayer: String, minutes: Int) {
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
    override val prayerNotificationsEnabled: Flow<Boolean> =
        preference(PreferencesKeys.PRAYER_NOTIFICATIONS_ENABLED, true)

    override suspend fun setPrayerNotificationsEnabled(enabled: Boolean) =
        put(PreferencesKeys.PRAYER_NOTIFICATIONS_ENABLED, enabled)

    override val adhanEnabled: Flow<Boolean> = preference(PreferencesKeys.ADHAN_ENABLED, false)

    override suspend fun setAdhanEnabled(enabled: Boolean) =
        put(PreferencesKeys.ADHAN_ENABLED, enabled)

    override val selectedAdhanSound: Flow<String> =
        preference(PreferencesKeys.SELECTED_ADHAN_SOUND, "MISHARY")

    override suspend fun setSelectedAdhanSound(sound: String) =
        put(PreferencesKeys.SELECTED_ADHAN_SOUND, sound)

    override val fajrNotificationEnabled: Flow<Boolean> =
        preference(PreferencesKeys.FAJR_NOTIFICATION_ENABLED, true)
    override val sunriseNotificationEnabled: Flow<Boolean> =
        preference(PreferencesKeys.SUNRISE_NOTIFICATION_ENABLED, false)
    override val dhuhrNotificationEnabled: Flow<Boolean> =
        preference(PreferencesKeys.DHUHR_NOTIFICATION_ENABLED, true)
    override val asrNotificationEnabled: Flow<Boolean> =
        preference(PreferencesKeys.ASR_NOTIFICATION_ENABLED, true)
    override val maghribNotificationEnabled: Flow<Boolean> =
        preference(PreferencesKeys.MAGHRIB_NOTIFICATION_ENABLED, true)
    override val ishaNotificationEnabled: Flow<Boolean> =
        preference(PreferencesKeys.ISHA_NOTIFICATION_ENABLED, true)

    override suspend fun setPrayerNotificationEnabled(prayer: String, enabled: Boolean) {
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
    override val fajrAdhanEnabled: Flow<Boolean> =
        preference(PreferencesKeys.FAJR_ADHAN_ENABLED, true)
    override val dhuhrAdhanEnabled: Flow<Boolean> =
        preference(PreferencesKeys.DHUHR_ADHAN_ENABLED, true)
    override val asrAdhanEnabled: Flow<Boolean> =
        preference(PreferencesKeys.ASR_ADHAN_ENABLED, true)
    override val maghribAdhanEnabled: Flow<Boolean> =
        preference(PreferencesKeys.MAGHRIB_ADHAN_ENABLED, true)
    override val ishaAdhanEnabled: Flow<Boolean> =
        preference(PreferencesKeys.ISHA_ADHAN_ENABLED, true)

    override suspend fun setPrayerAdhanEnabled(prayer: String, enabled: Boolean) {
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
    override fun isAdhanEnabledForPrayer(prayer: String): Flow<Boolean> {
        return when (prayer.lowercase()) {
            "fajr" -> fajrAdhanEnabled
            "dhuhr" -> dhuhrAdhanEnabled
            "asr" -> asrAdhanEnabled
            "maghrib" -> maghribAdhanEnabled
            "isha" -> ishaAdhanEnabled
            "sunrise" -> flowOf(false) // Sunrise never gets adhan
            else -> flowOf(false)
        }
    }

    override val adhanRespectDnd: Flow<Boolean> =
        preference(PreferencesKeys.ADHAN_RESPECT_DND, true)

    override suspend fun setAdhanRespectDnd(enabled: Boolean) =
        put(PreferencesKeys.ADHAN_RESPECT_DND, enabled)

    override val notificationVibration: Flow<Boolean> =
        preference(PreferencesKeys.NOTIFICATION_VIBRATION, true)

    override suspend fun setNotificationVibration(enabled: Boolean) =
        put(PreferencesKeys.NOTIFICATION_VIBRATION, enabled)

    override val notificationReminderMinutes: Flow<Int> =
        preference(PreferencesKeys.NOTIFICATION_REMINDER_MINUTES, 15)

    override suspend fun setNotificationReminderMinutes(minutes: Int) =
        put(PreferencesKeys.NOTIFICATION_REMINDER_MINUTES, minutes)

    override val showReminderBefore: Flow<Boolean> =
        preference(PreferencesKeys.SHOW_REMINDER_BEFORE, true)

    override suspend fun setShowReminderBefore(enabled: Boolean) =
        put(PreferencesKeys.SHOW_REMINDER_BEFORE, enabled)

    override val persistentNotification: Flow<Boolean> =
        preference(PreferencesKeys.PERSISTENT_NOTIFICATION, false)

    override suspend fun setPersistentNotification(enabled: Boolean) =
        put(PreferencesKeys.PERSISTENT_NOTIFICATION, enabled)

    override val fridayReminderEnabled: Flow<Boolean> =
        preference(PreferencesKeys.FRIDAY_REMINDER_ENABLED, false)

    override suspend fun setFridayReminderEnabled(enabled: Boolean) =
        put(PreferencesKeys.FRIDAY_REMINDER_ENABLED, enabled)

    override val fridayReminderMinutes: Flow<Int> =
        preference(PreferencesKeys.FRIDAY_REMINDER_MINUTES, 60)

    override suspend fun setFridayReminderMinutes(minutes: Int) =
        put(PreferencesKeys.FRIDAY_REMINDER_MINUTES, minutes)

    override val khatamReminderEnabled: Flow<Boolean> =
        preference(PreferencesKeys.KHATAM_REMINDER_ENABLED, false)

    override suspend fun setKhatamReminderEnabled(enabled: Boolean) =
        put(PreferencesKeys.KHATAM_REMINDER_ENABLED, enabled)

    override val khatamReminderTime: Flow<String> =
        preference(PreferencesKeys.KHATAM_REMINDER_TIME, "06:00")

    override suspend fun setKhatamReminderTime(time: String) =
        put(PreferencesKeys.KHATAM_REMINDER_TIME, time)

    // Extended worship reminders (Tahajjud, Suhoor, Iftar, …). Dynamic keys keyed by
    // WorshipReminderType.key so all 11 reminders share one uniform surface — no per-type
    // boilerplate. Default off; offsets default per type. See spec §2/§8 (epic #300).
    override fun worshipReminderEnabled(key: String): Flow<Boolean> =
        preference(booleanPreferencesKey("worship_${key}_enabled"), false)

    override suspend fun setWorshipReminderEnabled(key: String, enabled: Boolean) =
        put(booleanPreferencesKey("worship_${key}_enabled"), enabled)

    override fun worshipReminderOffset(key: String, default: Int): Flow<Int> =
        preference(intPreferencesKey("worship_${key}_offset"), default)

    override suspend fun setWorshipReminderOffset(key: String, minutes: Int) =
        put(intPreferencesKey("worship_${key}_offset"), minutes)

    override fun worshipReminderMode(key: String, default: String): Flow<String> =
        preference(stringPreferencesKey("worship_${key}_mode"), default)

    override suspend fun setWorshipReminderMode(key: String, mode: String) =
        put(stringPreferencesKey("worship_${key}_mode"), mode)

    // Per-prayer alert style and reminder. Dynamic keys keyed by prayer name, in the same
    // shape as the worship reminders above. These replace the global adhan on/off pair and
    // the single pre-adhan reminder; existing installs are carried across by
    // migratePrayerNotificationPreferences(). See PrayerNotificationPrefsMigration.
    override fun prayerAlertStyle(prayer: String): Flow<PrayerAlertStyle> =
        dataStore.data.map { prefs ->
            PrayerAlertStyle.fromStorage(prefs[alertStyleKey(prayer)])
        }

    override suspend fun setPrayerAlertStyle(prayer: String, style: PrayerAlertStyle) =
        put(alertStyleKey(prayer), style.name)

    override fun prayerReminderEnabled(prayer: String): Flow<Boolean> =
        preference(reminderEnabledKey(prayer), false)

    override suspend fun setPrayerReminderEnabled(prayer: String, enabled: Boolean) =
        put(reminderEnabledKey(prayer), enabled)

    override fun prayerReminderMinutes(prayer: String): Flow<Int> =
        preference(reminderMinutesKey(prayer), DEFAULT_REMINDER_MINUTES)

    override suspend fun setPrayerReminderMinutes(prayer: String, minutes: Int) =
        put(reminderMinutesKey(prayer), minutes)

    /**
     * Splits the old global adhan and pre-adhan preferences into the per-prayer ones, once.
     *
     * Guarded by a stored version so it cannot run twice and cannot reset a choice the user
     * has since made. Called from `AppInitializer` before anything reads the new keys.
     */
    override suspend fun migratePrayerNotificationPreferences() {
        val alreadyMigrated = dataStore.data
            .map { it[PreferencesKeys.NOTIFICATION_PREFS_MIGRATION_VERSION] ?: 0 }
            .first()
        if (alreadyMigrated >= PrayerNotificationPrefsMigration.VERSION) return

        val legacy = dataStore.data.map { prefs ->
            LegacyPrayerNotificationPrefs(
                adhanEnabled = prefs[PreferencesKeys.ADHAN_ENABLED] ?: false,
                perPrayerAdhanEnabled = ALERT_STYLE_PRAYERS.associateWith { prayer ->
                    prefs[legacyAdhanKey(prayer)] ?: true
                },
                showReminderBefore = prefs[PreferencesKeys.SHOW_REMINDER_BEFORE] ?: true,
                reminderMinutes = prefs[PreferencesKeys.NOTIFICATION_REMINDER_MINUTES]
                    ?: DEFAULT_REMINDER_MINUTES,
            )
        }.first()

        val migrated = PrayerNotificationPrefsMigration.plan(legacy)

        dataStore.edit { prefs ->
            migrated.alertStyle.forEach { (prayer, style) ->
                prefs[alertStyleKey(prayer)] = style.name
            }
            migrated.reminderEnabled.forEach { (prayer, enabled) ->
                prefs[reminderEnabledKey(prayer)] = enabled
            }
            migrated.reminderMinutes.forEach { (prayer, minutes) ->
                prefs[reminderMinutesKey(prayer)] = minutes
            }
            prefs[PreferencesKeys.NOTIFICATION_PREFS_MIGRATION_VERSION] =
                PrayerNotificationPrefsMigration.VERSION
        }
    }

    // Composed per prayer rather than declared five times each. The local is named `key`
    // so the literal reads "${key}_…", which is the shape PreferenceCodecTest scans for.
    private fun alertStyleKey(prayer: String): Preferences.Key<String> {
        val key = prayer.lowercase()
        return stringPreferencesKey("${key}_alert_style")
    }

    private fun reminderEnabledKey(prayer: String): Preferences.Key<Boolean> {
        val key = prayer.lowercase()
        return booleanPreferencesKey("${key}_reminder_enabled")
    }

    private fun reminderMinutesKey(prayer: String): Preferences.Key<Int> {
        val key = prayer.lowercase()
        return intPreferencesKey("${key}_reminder_minutes")
    }

    // The legacy per-prayer adhan flags are a closed set of five that will never grow —
    // they exist only for the migration to read — so they resolve to the declared keys
    // rather than composing a name.
    private fun legacyAdhanKey(prayer: String): Preferences.Key<Boolean> =
        when (prayer.lowercase()) {
            "fajr" -> PreferencesKeys.FAJR_ADHAN_ENABLED
            "dhuhr" -> PreferencesKeys.DHUHR_ADHAN_ENABLED
            "asr" -> PreferencesKeys.ASR_ADHAN_ENABLED
            "maghrib" -> PreferencesKeys.MAGHRIB_ADHAN_ENABLED
            else -> PreferencesKeys.ISHA_ADHAN_ENABLED
        }

    // Quran Settings
    override val quranTranslatorId: Flow<String> =
        preference(PreferencesKeys.QURAN_TRANSLATOR_ID, "sahih_international")

    override suspend fun setQuranTranslatorId(translatorId: String) =
        put(PreferencesKeys.QURAN_TRANSLATOR_ID, translatorId)

    override val showTranslation: Flow<Boolean> = preference(PreferencesKeys.SHOW_TRANSLATION, true)

    override suspend fun setShowTranslation(show: Boolean) =
        put(PreferencesKeys.SHOW_TRANSLATION, show)

    override val showTransliteration: Flow<Boolean> =
        preference(PreferencesKeys.SHOW_TRANSLITERATION, false)

    override suspend fun setShowTransliteration(show: Boolean) =
        put(PreferencesKeys.SHOW_TRANSLITERATION, show)

    override val selectedReciterId: Flow<String?> = preference(PreferencesKeys.SELECTED_RECITER_ID)

    override suspend fun setSelectedReciterId(reciterId: String?) {
        dataStore.edit {
            if (reciterId != null) it[PreferencesKeys.SELECTED_RECITER_ID] = reciterId
            else it.remove(PreferencesKeys.SELECTED_RECITER_ID)
        }
    }

    override val quranArabicFont: Flow<String> =
        preference(PreferencesKeys.QURAN_ARABIC_FONT, "amiri")

    override suspend fun setQuranArabicFont(fontId: String) =
        put(PreferencesKeys.QURAN_ARABIC_FONT, fontId)

    // Default matches MushafScript.DEFAULT (MADANI). Stored as a raw enum-name string so the
    // data layer maps via MushafScript at the domain boundary; presentation reads the enum.
    override val quranMushafScript: Flow<String> =
        preference(PreferencesKeys.QURAN_MUSHAF_SCRIPT, MushafScript.DEFAULT.name)

    override suspend fun setQuranMushafScript(script: String) =
        put(PreferencesKeys.QURAN_MUSHAF_SCRIPT, script)

    override val quranArabicFontSize: Flow<Float> =
        preference(PreferencesKeys.QURAN_ARABIC_FONT_SIZE, 28f)

    override suspend fun setQuranArabicFontSize(size: Float) =
        put(PreferencesKeys.QURAN_ARABIC_FONT_SIZE, size)

    override val quranTranslationFontSize: Flow<Float> =
        preference(PreferencesKeys.QURAN_TRANSLATION_FONT_SIZE, 16f)

    override suspend fun setQuranTranslationFontSize(size: Float) =
        put(PreferencesKeys.QURAN_TRANSLATION_FONT_SIZE, size)

    override val zakatGoldPricePerGram: Flow<Double> =
        preference(PreferencesKeys.ZAKAT_GOLD_PRICE_PER_GRAM, ZakatDefaults.GOLD_PRICE_PER_GRAM)

    override suspend fun setZakatGoldPricePerGram(pricePerGram: Double) =
        put(PreferencesKeys.ZAKAT_GOLD_PRICE_PER_GRAM, pricePerGram)

    override val zakatSilverPricePerGram: Flow<Double> =
        preference(PreferencesKeys.ZAKAT_SILVER_PRICE_PER_GRAM, ZakatDefaults.SILVER_PRICE_PER_GRAM)

    override suspend fun setZakatSilverPricePerGram(pricePerGram: Double) =
        put(PreferencesKeys.ZAKAT_SILVER_PRICE_PER_GRAM, pricePerGram)

    override val zakatCurrency: Flow<String> =
        preference(PreferencesKeys.ZAKAT_CURRENCY, ZakatDefaults.CURRENCY)

    override suspend fun setZakatCurrency(currency: String) =
        put(PreferencesKeys.ZAKAT_CURRENCY, currency)

    override val continuousReading: Flow<Boolean> =
        preference(PreferencesKeys.CONTINUOUS_READING, true)

    override suspend fun setContinuousReading(enabled: Boolean) =
        put(PreferencesKeys.CONTINUOUS_READING, enabled)

    override val keepScreenOn: Flow<Boolean> = preference(PreferencesKeys.KEEP_SCREEN_ON, true)

    override suspend fun setKeepScreenOn(enabled: Boolean) =
        put(PreferencesKeys.KEEP_SCREEN_ON, enabled)

    override val showTajweed: Flow<Boolean> = preference(PreferencesKeys.SHOW_TAJWEED, false)

    override suspend fun setShowTajweed(enabled: Boolean) =
        put(PreferencesKeys.SHOW_TAJWEED, enabled)

    override val tajweedUnderline: Flow<Boolean> =
        preference(PreferencesKeys.TAJWEED_UNDERLINE, false)

    override suspend fun setTajweedUnderline(enabled: Boolean) =
        put(PreferencesKeys.TAJWEED_UNDERLINE, enabled)

    // Dua Settings
    override val duaArabicFont: Flow<String> = preference(PreferencesKeys.DUA_ARABIC_FONT, "amiri")

    override suspend fun setDuaArabicFont(fontId: String) =
        put(PreferencesKeys.DUA_ARABIC_FONT, fontId)

    override val duaArabicFontSize: Flow<Float> =
        preference(PreferencesKeys.DUA_ARABIC_FONT_SIZE, 28f)

    override suspend fun setDuaArabicFontSize(size: Float) =
        put(PreferencesKeys.DUA_ARABIC_FONT_SIZE, size)

    override val duaTranslationFontSize: Flow<Float> =
        preference(PreferencesKeys.DUA_TRANSLATION_FONT_SIZE, 16f)

    override suspend fun setDuaTranslationFontSize(size: Float) =
        put(PreferencesKeys.DUA_TRANSLATION_FONT_SIZE, size)

    override val duaShowArabic: Flow<Boolean> = preference(PreferencesKeys.DUA_SHOW_ARABIC, true)

    override suspend fun setDuaShowArabic(show: Boolean) =
        put(PreferencesKeys.DUA_SHOW_ARABIC, show)

    override val duaShowTransliteration: Flow<Boolean> =
        preference(PreferencesKeys.DUA_SHOW_TRANSLITERATION, true)

    override suspend fun setDuaShowTransliteration(show: Boolean) =
        put(PreferencesKeys.DUA_SHOW_TRANSLITERATION, show)

    override val duaShowTranslation: Flow<Boolean> =
        preference(PreferencesKeys.DUA_SHOW_TRANSLATION, true)

    override suspend fun setDuaShowTranslation(show: Boolean) =
        put(PreferencesKeys.DUA_SHOW_TRANSLATION, show)

    override val duaCategoriesSortAlphabetical: Flow<Boolean> =
        preference(PreferencesKeys.DUA_CATEGORIES_SORT_ALPHABETICAL, false)

    override suspend fun setDuaCategoriesSortAlphabetical(enabled: Boolean) =
        put(PreferencesKeys.DUA_CATEGORIES_SORT_ALPHABETICAL, enabled)

    // Hadith Settings
    override val hadithArabicFont: Flow<String> =
        preference(PreferencesKeys.HADITH_ARABIC_FONT, "amiri")

    override suspend fun setHadithArabicFont(fontId: String) =
        put(PreferencesKeys.HADITH_ARABIC_FONT, fontId)

    override val hadithArabicFontSize: Flow<Float> =
        preference(PreferencesKeys.HADITH_ARABIC_FONT_SIZE, 24f)

    override suspend fun setHadithArabicFontSize(size: Float) =
        put(PreferencesKeys.HADITH_ARABIC_FONT_SIZE, size)

    override val hadithTranslationFontSize: Flow<Float> =
        preference(PreferencesKeys.HADITH_TRANSLATION_FONT_SIZE, 16f)

    override suspend fun setHadithTranslationFontSize(size: Float) =
        put(PreferencesKeys.HADITH_TRANSLATION_FONT_SIZE, size)

    override val hadithShowArabic: Flow<Boolean> =
        preference(PreferencesKeys.HADITH_SHOW_ARABIC, true)

    override suspend fun setHadithShowArabic(show: Boolean) =
        put(PreferencesKeys.HADITH_SHOW_ARABIC, show)

    override val hadithShowTranslation: Flow<Boolean> =
        preference(PreferencesKeys.HADITH_SHOW_TRANSLATION, true)

    override suspend fun setHadithShowTranslation(show: Boolean) =
        put(PreferencesKeys.HADITH_SHOW_TRANSLATION, show)

    override val hadithShowGrade: Flow<Boolean> =
        preference(PreferencesKeys.HADITH_SHOW_GRADE, true)

    override suspend fun setHadithShowGrade(show: Boolean) =
        put(PreferencesKeys.HADITH_SHOW_GRADE, show)

    override val hadithShowChain: Flow<Boolean> =
        preference(PreferencesKeys.HADITH_SHOW_CHAIN, true)

    override suspend fun setHadithShowChain(show: Boolean) =
        put(PreferencesKeys.HADITH_SHOW_CHAIN, show)

    // Tasbih Settings
    override val tasbihVibrationEnabled: Flow<Boolean> =
        preference(PreferencesKeys.TASBIH_VIBRATION_ENABLED, true)

    override suspend fun setTasbihVibrationEnabled(enabled: Boolean) =
        put(PreferencesKeys.TASBIH_VIBRATION_ENABLED, enabled)

    override val tasbihSoundEnabled: Flow<Boolean> =
        preference(PreferencesKeys.TASBIH_SOUND_ENABLED, true)

    override suspend fun setTasbihSoundEnabled(enabled: Boolean) =
        put(PreferencesKeys.TASBIH_SOUND_ENABLED, enabled)

    // Location
    override val latitude: Flow<Double> = preference(PreferencesKeys.LATITUDE, 0.0)

    override val longitude: Flow<Double> = preference(PreferencesKeys.LONGITUDE, 0.0)

    override val locationName: Flow<String> = preference(PreferencesKeys.LOCATION_NAME, "")

    override suspend fun updateLocation(latitude: Double, longitude: Double, name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LATITUDE] = latitude
            preferences[PreferencesKeys.LONGITUDE] = longitude
            preferences[PreferencesKeys.LOCATION_NAME] = name
        }
    }

    // Export all preferences as key-value map for sync
    override suspend fun exportAllPreferences(): Map<String, String> {
        val preferences = dataStore.data.first()
        return preferences.asMap().map { (key, value) ->
            key.name to PreferenceCodec.encode(value)
        }.toMap()
    }

    // Import preferences from sync payload
    /**
     * Writes an exported preference map back, using each key's **declared** type.
     *
     * This used to infer the type from the shape of the value and substrings of the key name.
     * DataStore keys are typed and reading one back at the wrong type throws, so the six keys
     * the heuristic missed did not merely import wrong — they crashed on next read after any
     * sync. See [PreferenceCodec] and `PreferenceCodecTest`.
     */
    override suspend fun importPreferences(prefsMap: Map<String, String>) {
        dataStore.edit { preferences ->
            prefsMap.forEach { (key, value) ->
                val (typedKey, typedValue) = PreferenceCodec.decode(key, value) ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                preferences[typedKey as Preferences.Key<Any>] = typedValue
            }
        }
    }

    // AI — Ask with Proof
    override val aiAskEnabled: Flow<Boolean> = preference(PreferencesKeys.AI_ASK_ENABLED, false)

    override suspend fun setAiAskEnabled(enabled: Boolean) =
        put(PreferencesKeys.AI_ASK_ENABLED, enabled)

    override val aiConsentTimestamp: Flow<Long> =
        preference(PreferencesKeys.AI_CONSENT_TIMESTAMP, 0L)

    override suspend fun setAiConsentTimestamp(timestamp: Long) =
        put(PreferencesKeys.AI_CONSENT_TIMESTAMP, timestamp)

    override val aiHistoryEnabled: Flow<Boolean> =
        preference(PreferencesKeys.AI_HISTORY_ENABLED, false)

    override suspend fun setAiHistoryEnabled(enabled: Boolean) =
        put(PreferencesKeys.AI_HISTORY_ENABLED, enabled)

    override val aiAskHintDismissed: Flow<Boolean> =
        preference(PreferencesKeys.AI_ASK_HINT_DISMISSED, false)

    override suspend fun setAiAskHintDismissed(dismissed: Boolean) =
        put(PreferencesKeys.AI_ASK_HINT_DISMISSED, dismissed)

    override val aiQuestionHistory: Flow<String> =
        preference(PreferencesKeys.AI_QUESTION_HISTORY, "")

    override suspend fun setAiQuestionHistory(json: String) =
        put(PreferencesKeys.AI_QUESTION_HISTORY, json)

    // Combined user preferences
    override val userPreferences: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "system",
            dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false,
            appLanguage = preferences[PreferencesKeys.APP_LANGUAGE] ?: "en",
            calculationMethod = preferences[PreferencesKeys.CALCULATION_METHOD]
                ?: "MUSLIM_WORLD_LEAGUE",
            asrCalculation = preferences[PreferencesKeys.ASR_CALCULATION] ?: "standard",
            latitude = preferences[PreferencesKeys.LATITUDE] ?: 0.0,
            longitude = preferences[PreferencesKeys.LONGITUDE] ?: 0.0,
            locationName = preferences[PreferencesKeys.LOCATION_NAME] ?: "",
            prayerNotificationsEnabled = preferences[PreferencesKeys.PRAYER_NOTIFICATIONS_ENABLED]
                ?: true,
            quranTranslatorId = preferences[PreferencesKeys.QURAN_TRANSLATOR_ID]
                ?: "sahih_international",
            showTranslation = preferences[PreferencesKeys.SHOW_TRANSLATION] ?: true
        )
    }
}
