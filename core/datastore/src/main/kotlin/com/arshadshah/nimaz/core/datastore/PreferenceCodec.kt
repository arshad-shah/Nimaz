package com.arshadshah.nimaz.core.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/** The DataStore type a preference is stored under. */
internal enum class PrefType { BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, STRING_SET }

/**
 * Turns preferences into strings for the sync wire and back into **typed** DataStore entries.
 *
 * `exportAllPreferences` flattens values with `toString()`, so the wire is `Map<String, String>`
 * and the type is gone. The import used to guess it back from the shape of the value and
 * substrings of the key name, which got six keys wrong — and since DataStore keys are typed,
 * reading one back at the wrong type throws, so a sync left `tasbih_preset_seed_version`,
 * `current_location_id` and four others crashing on next read.
 *
 * So the type is declared, not inferred. [TYPES] mirrors `PreferencesDataStore.PreferencesKeys`
 * exactly, and `PreferenceCodecTest` reads those declarations out of the source file and fails
 * if the two ever drift.
 *
 * A key that is *not* in [TYPES] comes from a newer sender, and is kept as a string rather than
 * dropped: an unknown preference is worth nothing to this build anyway, and a future build that
 * learns the key can re-type it.
 */
internal object PreferenceCodec {

    /**
     * Set elements are joined on the ASCII unit separator. `Set.toString()` yields `[a, b]`,
     * which cannot be split back safely — an element containing `", "` would break it — and is
     * why `tasbih_favorites` could not survive a sync at all.
     */
    private const val SET_SEPARATOR = "\u001F"

    /** Never taken from the wire: it would drop the receiver into, or out of, onboarding. */
    private const val ONBOARDING_COMPLETED = "onboarding_completed"

    val TYPES: Map<String, PrefType> = mapOf(
        "onboarding_completed" to PrefType.BOOLEAN,
        "theme_mode" to PrefType.STRING,
        "dynamic_color" to PrefType.BOOLEAN,
        "show_islamic_patterns" to PrefType.BOOLEAN,
        "pattern_style" to PrefType.STRING,
        "animations_enabled" to PrefType.BOOLEAN,
        "show_countdown" to PrefType.BOOLEAN,
        "show_quick_actions" to PrefType.BOOLEAN,
        "haptic_feedback" to PrefType.BOOLEAN,
        "tasbih_bead_mode" to PrefType.BOOLEAN,
        "tasbih_bead_design" to PrefType.STRING,
        "tasbih_selected_preset" to PrefType.LONG,
        "tasbih_preset_seed_version" to PrefType.INT,
        "tasbih_favorites" to PrefType.STRING_SET,
        "tasbih_left_handed" to PrefType.BOOLEAN,
        "use_24_hour_format" to PrefType.BOOLEAN,
        "use_hijri_primary" to PrefType.BOOLEAN,
        "hijri_day_offset" to PrefType.INT,
        "app_language" to PrefType.STRING,
        "arabic_font_size" to PrefType.STRING,
        "content_patch_version" to PrefType.INT,
        "calculation_method" to PrefType.STRING,
        "asr_calculation" to PrefType.STRING,
        "high_latitude_rule" to PrefType.STRING,
        "current_location_id" to PrefType.LONG,
        "fajr_adjustment" to PrefType.INT,
        "sunrise_adjustment" to PrefType.INT,
        "dhuhr_adjustment" to PrefType.INT,
        "asr_adjustment" to PrefType.INT,
        "maghrib_adjustment" to PrefType.INT,
        "isha_adjustment" to PrefType.INT,
        "prayer_notifications_enabled" to PrefType.BOOLEAN,
        "adhan_enabled" to PrefType.BOOLEAN,
        "pre_notification_minutes" to PrefType.STRING,
        "notification_vibration" to PrefType.BOOLEAN,
        "notification_reminder_minutes" to PrefType.INT,
        "show_reminder_before" to PrefType.BOOLEAN,
        "persistent_notification" to PrefType.BOOLEAN,
        "friday_reminder_enabled" to PrefType.BOOLEAN,
        "friday_reminder_minutes" to PrefType.INT,
        "khatam_reminder_enabled" to PrefType.BOOLEAN,
        "khatam_reminder_time" to PrefType.STRING,
        "selected_adhan_sound" to PrefType.STRING,
        "fajr_notification_enabled" to PrefType.BOOLEAN,
        "sunrise_notification_enabled" to PrefType.BOOLEAN,
        "dhuhr_notification_enabled" to PrefType.BOOLEAN,
        "asr_notification_enabled" to PrefType.BOOLEAN,
        "maghrib_notification_enabled" to PrefType.BOOLEAN,
        "isha_notification_enabled" to PrefType.BOOLEAN,
        "fajr_adhan_enabled" to PrefType.BOOLEAN,
        "dhuhr_adhan_enabled" to PrefType.BOOLEAN,
        "asr_adhan_enabled" to PrefType.BOOLEAN,
        "maghrib_adhan_enabled" to PrefType.BOOLEAN,
        "isha_adhan_enabled" to PrefType.BOOLEAN,
        "adhan_respect_dnd" to PrefType.BOOLEAN,
        "quran_translator_id" to PrefType.STRING,
        "show_translation" to PrefType.BOOLEAN,
        "show_transliteration" to PrefType.BOOLEAN,
        "selected_reciter_id" to PrefType.STRING,
        "quran_arabic_font" to PrefType.STRING,
        "quran_mushaf_script" to PrefType.STRING,
        "quran_arabic_font_size" to PrefType.FLOAT,
        "quran_translation_font_size" to PrefType.FLOAT,
        "zakat_gold_price_per_gram" to PrefType.DOUBLE,
        "zakat_silver_price_per_gram" to PrefType.DOUBLE,
        "zakat_currency" to PrefType.STRING,
        "zakat_nisab_type" to PrefType.STRING,
        "continuous_reading" to PrefType.BOOLEAN,
        "keep_screen_on" to PrefType.BOOLEAN,
        "show_tajweed" to PrefType.BOOLEAN,
        "tajweed_underline" to PrefType.BOOLEAN,
        "dua_arabic_font" to PrefType.STRING,
        "dua_arabic_font_size" to PrefType.FLOAT,
        "dua_translation_font_size" to PrefType.FLOAT,
        "dua_show_arabic" to PrefType.BOOLEAN,
        "dua_show_transliteration" to PrefType.BOOLEAN,
        "dua_show_translation" to PrefType.BOOLEAN,
        "dua_categories_sort_alphabetical" to PrefType.BOOLEAN,
        "hadith_arabic_font" to PrefType.STRING,
        "hadith_arabic_font_size" to PrefType.FLOAT,
        "hadith_translation_font_size" to PrefType.FLOAT,
        "hadith_show_arabic" to PrefType.BOOLEAN,
        "hadith_show_translation" to PrefType.BOOLEAN,
        "hadith_show_grade" to PrefType.BOOLEAN,
        "hadith_show_chain" to PrefType.BOOLEAN,
        "tasbih_vibration_enabled" to PrefType.BOOLEAN,
        "tasbih_sound_enabled" to PrefType.BOOLEAN,
        "latitude" to PrefType.DOUBLE,
        "longitude" to PrefType.DOUBLE,
        "location_name" to PrefType.STRING,
        "ai_ask_enabled" to PrefType.BOOLEAN,
        "ai_consent_timestamp" to PrefType.LONG,
        "ai_history_enabled" to PrefType.BOOLEAN,
        "ai_ask_hint_dismissed" to PrefType.BOOLEAN,
        "ai_question_history" to PrefType.STRING,
        "search_results_per_source" to PrefType.INT,
        // A comma-separated list of LibrarySource names, not a STRING_SET, for the same reason
        // as the shortcuts below: an empty value is meaningful here (it means "every source,
        // including ones added later"), and a Set gives no way to tell it from unset.
        "search_sources" to PrefType.STRING,
        "search_strictness" to PrefType.STRING,
        "search_default_scope" to PrefType.STRING,
        // A delimited string of PinnedShortcut keys, not a STRING_SET: the row's order is what
        // the user arranged, and a Set discards it.
        "more_pinned_shortcuts" to PrefType.STRING,
        "notification_prefs_migration_version" to PrefType.INT
    )

    /**
     * Keys composed at runtime, so they cannot be listed by name.
     *
     * `worship_<type>_enabled` and its siblings are built per `WorshipReminderType`, of which
     * there are a dozen and counting; matching the shape keeps this registry from having to
     * track that enum. The per-prayer alert style and reminder keys are composed the same way,
     * one set per prayer.
     */
    private val PATTERNS: List<Pair<Regex, PrefType>> = listOf(
        Regex("^worship_[a-z_]+_enabled$") to PrefType.BOOLEAN,
        Regex("^worship_[a-z_]+_offset$") to PrefType.INT,
        Regex("^worship_[a-z_]+_mode$") to PrefType.STRING,
        // Per-prayer alert style and reminder, one set per prayer.
        Regex("^[a-z]+_alert_style$") to PrefType.STRING,
        Regex("^[a-z]+_reminder_enabled$") to PrefType.BOOLEAN,
        Regex("^[a-z]+_reminder_minutes$") to PrefType.INT,
    )

    /** The declared type of [key], by name or by shape, or null if this build does not know it. */
    fun typeOf(key: String): PrefType? =
        TYPES[key] ?: PATTERNS.firstOrNull { (pattern, _) -> pattern.matches(key) }?.second

    /** A preference value as it travels on the wire. */
    fun encode(value: Any): String = when (value) {
        is Set<*> -> value.joinToString(SET_SEPARATOR)
        else -> value.toString()
    }

    /**
     * The typed key and value to write, or null when the entry must not be imported.
     *
     * A value that will not parse as its declared type is dropped rather than coerced: writing
     * a wrong-typed entry is what this class exists to prevent.
     */
    fun decode(key: String, value: String): Pair<Preferences.Key<*>, Any>? {
        if (key == ONBOARDING_COMPLETED) return null
        // `Pair(...)` rather than `to`: on a Preferences.Key the infix `to` resolves to
        // DataStore's own operator, which builds a Preferences.Pair, not a Kotlin Pair.
        return when (typeOf(key)) {
            PrefType.BOOLEAN ->
                value.toBooleanStrictOrNull()?.let { Pair(booleanPreferencesKey(key), it) }

            PrefType.INT -> value.toIntOrNull()?.let { Pair(intPreferencesKey(key), it) }
            PrefType.LONG -> value.toLongOrNull()?.let { Pair(longPreferencesKey(key), it) }
            PrefType.FLOAT -> value.toFloatOrNull()?.let { Pair(floatPreferencesKey(key), it) }
            PrefType.DOUBLE -> value.toDoubleOrNull()?.let { Pair(doublePreferencesKey(key), it) }
            PrefType.STRING -> Pair(stringPreferencesKey(key), value)
            PrefType.STRING_SET -> Pair(stringSetPreferencesKey(key), decodeSet(value))
            null -> Pair(stringPreferencesKey(key), value)
        }
    }

    /**
     * Splitting `""` yields `[""]`, a set holding one blank string rather than the empty set
     * the sender had. Also accepts the legacy `[a, b]` that `Set.toString()` produced before
     * [encode] existed, so a payload from an older build still lands.
     */
    private fun decodeSet(value: String): Set<String> = when {
        value.isEmpty() -> emptySet()
        value.startsWith("[") && value.endsWith("]") ->
            value.substring(1, value.length - 1)
                .split(", ")
                .filter { it.isNotEmpty() }
                .toSet()

        else -> value.split(SET_SEPARATOR).filter { it.isNotEmpty() }.toSet()
    }
}
