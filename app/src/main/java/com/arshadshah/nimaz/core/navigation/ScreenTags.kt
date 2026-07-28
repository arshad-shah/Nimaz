package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.core.navigation.ScreenTags.bottomNav


/**
 * Stable Compose `testTag` values for navigation, used by the instrumented UI tests
 * to assert *which* screen is showing — independent of locale or on-screen copy.
 *
 * Each routed destination in [NavGraph] is wrapped in a tagged container (see
 * `taggedComposable`), and each bottom-navigation item carries [bottomNav]. Tests
 * reference these constants (mirrored in the androidTest `Selectors`) rather than
 * matching user-visible text, so a label change never breaks a navigation test.
 *
 * Naming: one constant per `Route` simple name; the tag string is `screen_<route>`.
 * This object is the single source of truth — add a new destination's tag here and
 * wrap it in [NavGraph].
 */
object ScreenTags {
    const val Onboarding = "screen_onboarding"
    const val Home = "screen_home"
    const val Quran = "screen_quran"
    const val Tasbih = "screen_tasbih"
    const val QiblaNav = "screen_qibla_nav"
    const val QaidaHome = "screen_qaida_home"
    const val QaidaReader = "screen_qaida_reader"
    const val QaidaLetters = "screen_qaida_letters"
    const val More = "screen_more"
    const val QuranReader = "screen_quran_reader"
    const val TafseerChapters = "screen_tafseer_chapters"
    const val Tafseer = "screen_tafseer"
    const val SurahInfo = "screen_surah_info"
    const val QuranPage = "screen_quran_page"
    const val QuranJuz = "screen_quran_juz"
    const val QuranSearch = "screen_quran_search"
    const val QuranBookmarks = "screen_quran_bookmarks"
    const val HadithHome = "screen_hadith_home"
    const val HadithBook = "screen_hadith_book"
    const val HadithChapter = "screen_hadith_chapter"
    const val HadithReader = "screen_hadith_reader"
    const val HadithSettings = "screen_hadith_settings"
    const val HadithSearch = "screen_hadith_search"
    const val HadithBookmarks = "screen_hadith_bookmarks"
    const val DuaHome = "screen_dua_home"
    const val DuaCategory = "screen_dua_category"
    const val DuaReader = "screen_dua_reader"
    const val DuaSettings = "screen_dua_settings"
    const val DuaFavorites = "screen_dua_favorites"
    const val DuaSearch = "screen_dua_search"
    const val PrayerTimes = "screen_prayer_times"
    const val PrayerTracker = "screen_prayer_tracker"
    const val PrayerStats = "screen_prayer_stats"
    const val QadaPrayers = "screen_qada_prayers"
    const val MonthlyPrayerTimes = "screen_monthly_prayer_times"
    const val FastingHome = "screen_fasting_home"
    const val FastingTracker = "screen_fasting_tracker"
    const val NightWorship = "screen_night_worship"
    const val FastingStats = "screen_fasting_stats"
    const val TasbihHome = "screen_tasbih_home"
    const val TasbihCounterScreen = "screen_tasbih_counter"
    const val TasbihPresets = "screen_tasbih_presets"
    const val TasbihStats = "screen_tasbih_stats"
    const val TasbihHistory = "screen_tasbih_history"
    const val TasbihAddPreset = "screen_tasbih_add_preset"
    const val ZakatCalculator = "screen_zakat_calculator"
    const val ZakatHistory = "screen_zakat_history"
    const val Qibla = "screen_qibla"
    const val IslamicCalendar = "screen_islamic_calendar"
    const val IslamicMonth = "screen_islamic_month"
    const val Settings = "screen_settings"
    const val SettingsPrayerCalculation = "screen_settings_prayer_calculation"
    const val SettingsNotifications = "screen_settings_notifications"
    const val SettingsAppearance = "screen_settings_appearance"
    const val SettingsLanguage = "screen_settings_language"
    const val SettingsLocation = "screen_settings_location"
    const val SettingsQuran = "screen_settings_quran"
    const val SelectReciter = "screen_select_reciter"
    const val SelectTranslation = "screen_select_translation"
    const val SettingsWidgets = "screen_settings_widgets"
    const val SettingsAbout = "screen_settings_about"
    const val Licenses = "screen_licenses"
    const val LicenseDetail = "screen_license_detail"
    const val AsmaUlHusnaList = "screen_asma_ul_husna_list"
    const val AsmaUlHusnaDetail = "screen_asma_ul_husna_detail"
    const val AsmaUnNabiList = "screen_asma_un_nabi_list"
    const val AsmaUnNabiDetail = "screen_asma_un_nabi_detail"
    const val ProphetsList = "screen_prophets_list"
    const val ProphetDetail = "screen_prophet_detail"
    const val KhatamList = "screen_khatam_list"
    const val KhatamDetail = "screen_khatam_detail"
    const val KhatamCreate = "screen_khatam_create"
    const val KhatamEdit = "screen_khatam_edit"
    const val SettingsHelp = "screen_settings_help"
    const val HelpTopicDetail = "screen_help_topic_detail"
    const val HelpGuide = "screen_help_guide"
    const val SettingsSync = "screen_settings_sync"
    const val AllBookmarks = "screen_all_bookmarks"
    const val GlobalSearch = "screen_global_search"
    const val SearchSettings = "screen_search_settings"

    /** Scrollable lists on hub screens — let UI tests scroll to off-screen entries. */
    const val MoreList = "more_menu_list"
    const val SettingsList = "settings_list"
    const val AppearanceList = "settings_appearance_list"
    const val NotificationsList = "settings_notifications_list"

    /** Interactive elements exercised by behavior tests. */
    const val TasbihCounter = "tasbih_counter"
    const val TasbihCount = "tasbih_count"

    /** Quran browse — the scrollable surah list (behavior tests scroll-to a surah). */
    const val QuranSurahList = "quran_surah_list"

    /** Hadith home — the scrollable list holding the collections grid (scroll-to a book). */
    const val HadithBookList = "hadith_book_list"

    /** Tag for a bottom-navigation tab, keyed by its label (e.g. "Home"). */
    fun bottomNav(label: String): String = "bottomnav_$label"
}
