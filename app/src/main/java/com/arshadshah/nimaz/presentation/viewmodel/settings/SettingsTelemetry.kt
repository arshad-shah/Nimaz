package com.arshadshah.nimaz.presentation.viewmodel.settings

/**
 * What each settings-shaped [SettingsEvent] records: the setting's name, and its new value.
 *
 * **Why a table rather than a line in each branch.** `SettingsViewModel.onEvent` is a 78-branch
 * `when`, and 15 of those branches carried a `logSettingChanged` call while 56 carried nothing —
 * including `SetPrayerAdjustment`, which is the first thing anyone would ask for when a user
 * reports that a prayer time is a few minutes out, and every typography and location event. The
 * per-branch approach is what produced that ratio: a new setting is added by copying a
 * neighbouring branch, and whether it gets logged depends on which neighbour was copied.
 *
 * Collected here, the coverage is one list you can read top to bottom, `when` is exhaustive over
 * [SettingsEvent] so **a new event cannot compile without a decision being made about it**, and
 * the naming stays consistent instead of drifting per branch.
 *
 * The 15 names that were already being reported are kept **exactly** as they were, dashboards
 * included — `pre_reminder_enabled` rather than the tidier `show_reminder_before`, because a
 * renamed setting reads as a setting nobody changes any more.
 *
 * Returns null for the events that are not setting changes: the two test-notification actions,
 * the adhan preview, and the three destructive actions. Those are logged where they happen.
 */
internal fun SettingsEvent.asSettingChange(): Pair<String, String>? = when (this) {
    // -- General ---------------------------------------------------------------
    is SettingsEvent.SetTheme -> "theme" to theme.name
    is SettingsEvent.SetLanguage -> "language" to language.name
    is SettingsEvent.SetHijriPrimary -> "hijri_primary" to enabled.toString()
    is SettingsEvent.SetHijriDayOffset -> "hijri_day_offset" to days.toString()
    is SettingsEvent.Set24HourFormat -> "24_hour_format" to enabled.toString()
    is SettingsEvent.SetHapticFeedback -> "haptic_feedback" to enabled.toString()
    is SettingsEvent.SetShowIslamicPatterns -> "show_islamic_patterns" to enabled.toString()
    is SettingsEvent.SetPatternStyle -> "pattern_style" to style.name
    is SettingsEvent.SetAnimationsEnabled -> "animations_enabled" to enabled.toString()
    is SettingsEvent.SetShowCountdown -> "show_countdown" to enabled.toString()
    is SettingsEvent.SetShowQuickActions -> "show_quick_actions" to enabled.toString()

    // -- Prayer calculation ----------------------------------------------------
    is SettingsEvent.SetCalculationMethod -> "calculation_method" to method.name
    is SettingsEvent.SetAsrMethod -> "asr_method" to method.name
    is SettingsEvent.SetHighLatitudeRule -> "high_latitude_rule" to rule.name
    // Keyed by prayer, because "someone adjusted a prayer by 3 minutes" is not the question —
    // "which prayer, and by how much" is, and a per-prayer key is what makes a systematic
    // offset (everyone nudging Fajr the same way) visible as a shape rather than a mean.
    is SettingsEvent.SetPrayerAdjustment -> "prayer_adjustment_$prayer" to minutes.toString()

    // -- Notifications ---------------------------------------------------------
    is SettingsEvent.SetNotificationsEnabled -> "notifications_enabled" to enabled.toString()
    is SettingsEvent.SetPrayerNotification -> "prayer_notification_$prayer" to enabled.toString()
    is SettingsEvent.SetAdhanEnabled -> "adhan_enabled" to enabled.toString()
    is SettingsEvent.SetPrayerAlertStyle -> "alert_style_$prayer" to style.name
    is SettingsEvent.SetPrayerReminderEnabled -> "reminder_enabled_$prayer" to enabled.toString()
    is SettingsEvent.SetPrayerReminderMinutes -> "reminder_minutes_$prayer" to minutes.toString()
    is SettingsEvent.SetVibrationEnabled -> "notification_vibration" to enabled.toString()
    is SettingsEvent.SetRespectDnd -> "respect_dnd" to enabled.toString()
    is SettingsEvent.SetReminderMinutes -> "reminder_minutes" to minutes.toString()
    is SettingsEvent.SetShowReminderBefore -> "pre_reminder_enabled" to enabled.toString()
    is SettingsEvent.SetPersistentNotification -> "persistent_notification" to enabled.toString()
    is SettingsEvent.SetFridayReminderEnabled -> "friday_reminder_enabled" to enabled.toString()
    is SettingsEvent.SetFridayReminderMinutes -> "friday_reminder_minutes" to minutes.toString()
    is SettingsEvent.SetKhatamReminderEnabled -> "khatam_reminder_enabled" to enabled.toString()
    is SettingsEvent.SetKhatamReminderTime -> "khatam_reminder_time" to time
    is SettingsEvent.SetAdhanSound -> "adhan_sound" to sound
    is SettingsEvent.SetWorshipReminderEnabled -> "worship_enabled_$key" to enabled.toString()
    is SettingsEvent.SetWorshipReminderOffset -> "worship_offset_$key" to minutes.toString()
    is SettingsEvent.SetWorshipReminderMode -> "worship_mode_$key" to mode

    // -- Quran reader ----------------------------------------------------------
    is SettingsEvent.SetTranslator -> "quran_translator" to translatorId
    is SettingsEvent.SetArabicFont -> "quran_arabic_font" to fontId
    is SettingsEvent.SetShowTranslation -> "quran_show_translation" to enabled.toString()
    is SettingsEvent.SetShowTransliteration -> "quran_show_transliteration" to enabled.toString()
    is SettingsEvent.SetArabicFontSize -> "quran_arabic_font_size" to size.toString()
    is SettingsEvent.SetTranslationFontSize -> "quran_translation_font_size" to size.toString()
    is SettingsEvent.SetContinuousReading -> "quran_continuous_reading" to enabled.toString()
    is SettingsEvent.SetKeepScreenOn -> "quran_keep_screen_on" to enabled.toString()
    is SettingsEvent.SetReciter -> "quran_reciter" to (reciterId ?: "none")
    is SettingsEvent.SetShowTajweed -> "quran_show_tajweed" to enabled.toString()
    is SettingsEvent.SetTajweedUnderline -> "quran_tajweed_underline" to enabled.toString()
    is SettingsEvent.SetMushafScript -> "quran_mushaf_script" to script.name

    // -- Dua reader ------------------------------------------------------------
    is SettingsEvent.SetDuaArabicFont -> "dua_arabic_font" to fontId
    is SettingsEvent.SetDuaArabicFontSize -> "dua_arabic_font_size" to size.toString()
    is SettingsEvent.SetDuaTranslationFontSize -> "dua_translation_font_size" to size.toString()
    is SettingsEvent.SetDuaShowArabic -> "dua_show_arabic" to enabled.toString()
    is SettingsEvent.SetDuaShowTransliteration -> "dua_show_transliteration" to enabled.toString()
    is SettingsEvent.SetDuaShowTranslation -> "dua_show_translation" to enabled.toString()

    // -- Hadith reader ---------------------------------------------------------
    is SettingsEvent.SetHadithArabicFont -> "hadith_arabic_font" to fontId
    is SettingsEvent.SetHadithArabicFontSize -> "hadith_arabic_font_size" to size.toString()
    is SettingsEvent.SetHadithTranslationFontSize ->
        "hadith_translation_font_size" to size.toString()

    is SettingsEvent.SetHadithShowArabic -> "hadith_show_arabic" to enabled.toString()
    is SettingsEvent.SetHadithShowTranslation -> "hadith_show_translation" to enabled.toString()
    is SettingsEvent.SetHadithShowGrade -> "hadith_show_grade" to enabled.toString()
    is SettingsEvent.SetHadithShowChain -> "hadith_show_chain" to enabled.toString()

    // -- Location --------------------------------------------------------------
    // The place name is never recorded — only that the set changed, and how big it is. A
    // location is the most identifying thing this app holds; `settingChanged` writes its value
    // to an analytics dashboard, and "Ahmed's street" does not belong on one.
    is SettingsEvent.SetCurrentLocation -> "current_location" to "changed"
    is SettingsEvent.AddLocation -> "saved_locations" to "added"
    is SettingsEvent.RemoveLocation -> "saved_locations" to "removed"
    is SettingsEvent.ToggleLocationFavorite -> "saved_locations" to "favourite_toggled"

    // -- Not setting changes ---------------------------------------------------
    SettingsEvent.PreviewAdhanSound,
    SettingsEvent.StopAdhanPreview,
    SettingsEvent.TestNotification,
    SettingsEvent.TestAllNotifications,
    SettingsEvent.ResetToDefaults,
    SettingsEvent.ResetNotifications,
    SettingsEvent.DeleteAllData,
        -> null
}
