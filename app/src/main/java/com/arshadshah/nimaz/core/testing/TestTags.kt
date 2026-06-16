package com.arshadshah.nimaz.core.testing

/**
 * Central registry of Compose UI test tags ("selectors").
 *
 * Single source of truth so instrumented / UI tests select elements by a stable
 * id instead of localized, visible text — and so a tag can be renamed in exactly
 * one place rather than chasing raw strings across the codebase.
 *
 * Usage in a composable:
 * ```
 * Scaffold(modifier = Modifier.testTag(TestTags.SCREEN_HOME)) { ... }
 * ```
 * Usage in a test:
 * ```
 * composeRule.onNodeWithTag(TestTags.SCREEN_HOME).assertIsDisplayed()
 * ```
 *
 * Conventions:
 * - `NAV_*`     bottom-navigation / rail destinations
 * - `SCREEN_*`  the root container of a screen (for "did it render / not crash")
 * - everything else: a specific interactive element (button, field, list, …)
 */
object TestTags {

    // ── Bottom navigation ────────────────────────────────────────────
    const val NAV_HOME = "nav.home"
    const val NAV_QURAN = "nav.quran"
    const val NAV_TASBIH = "nav.tasbih"
    const val NAV_QIBLA = "nav.qibla"
    const val NAV_MORE = "nav.more"

    // ── Common, reused across screens ────────────────────────────────
    const val BACK_BUTTON = "common.back"
    const val TOP_APP_BAR = "common.topAppBar"
    const val LOADING_INDICATOR = "common.loading"
    const val ERROR_STATE = "common.error"
    const val EMPTY_STATE = "common.empty"

    // ── Screen roots (one per destination) ───────────────────────────
    const val SCREEN_ONBOARDING = "screen.onboarding"
    const val SCREEN_HOME = "screen.home"
    const val SCREEN_QURAN = "screen.quran"
    const val SCREEN_QURAN_READER = "screen.quranReader"
    const val SCREEN_TAFSEER = "screen.tafseer"
    const val SCREEN_SURAH_INFO = "screen.surahInfo"
    const val SCREEN_QURAN_SEARCH = "screen.quranSearch"
    const val SCREEN_BOOKMARKS = "screen.bookmarks"
    const val SCREEN_SELECT_RECITER = "screen.selectReciter"
    const val SCREEN_HADITH = "screen.hadith"
    const val SCREEN_HADITH_CHAPTERS = "screen.hadithChapters"
    const val SCREEN_HADITH_READER = "screen.hadithReader"
    const val SCREEN_DUA = "screen.dua"
    const val SCREEN_DUA_CATEGORY = "screen.duaCategory"
    const val SCREEN_DUA_READER = "screen.duaReader"
    const val SCREEN_DUA_COLLECTION = "screen.duaCollection"
    const val SCREEN_SEARCH = "screen.search"
    const val SCREEN_PRAYER_TRACKER = "screen.prayerTracker"
    const val SCREEN_PRAYER_STATS = "screen.prayerStats"
    const val SCREEN_MONTHLY_PRAYER_TIMES = "screen.monthlyPrayerTimes"
    const val SCREEN_FASTING = "screen.fasting"
    const val SCREEN_TASBIH = "screen.tasbih"
    const val SCREEN_TASBIH_HISTORY = "screen.tasbihHistory"
    const val SCREEN_TASBIH_ADD_PRESET = "screen.tasbihAddPreset"
    const val SCREEN_ZAKAT_CALCULATOR = "screen.zakatCalculator"
    const val SCREEN_ZAKAT_HISTORY = "screen.zakatHistory"
    const val SCREEN_QIBLA = "screen.qibla"
    const val SCREEN_ISLAMIC_CALENDAR = "screen.islamicCalendar"
    const val SCREEN_MORE = "screen.more"
    const val SCREEN_SETTINGS = "screen.settings"
    const val SCREEN_SETTINGS_PRAYER = "screen.settingsPrayer"
    const val SCREEN_SETTINGS_NOTIFICATIONS = "screen.settingsNotifications"
    const val SCREEN_SETTINGS_APPEARANCE = "screen.settingsAppearance"
    const val SCREEN_SETTINGS_LANGUAGE = "screen.settingsLanguage"
    const val SCREEN_SETTINGS_LOCATION = "screen.settingsLocation"
    const val SCREEN_SETTINGS_QURAN = "screen.settingsQuran"
    const val SCREEN_SETTINGS_WIDGETS = "screen.settingsWidgets"
    const val SCREEN_SETTINGS_HELP = "screen.settingsHelp"
    const val SCREEN_SETTINGS_SYNC = "screen.settingsSync"
    const val SCREEN_ABOUT = "screen.about"
    const val SCREEN_LICENSES = "screen.licenses"
    const val SCREEN_LICENSE_DETAIL = "screen.licenseDetail"
    const val SCREEN_ASMA_UL_HUSNA = "screen.asmaUlHusna"
    const val SCREEN_ASMA_UL_HUSNA_DETAIL = "screen.asmaUlHusnaDetail"
    const val SCREEN_ASMA_UN_NABI = "screen.asmaUnNabi"
    const val SCREEN_ASMA_UN_NABI_DETAIL = "screen.asmaUnNabiDetail"
    const val SCREEN_PROPHETS = "screen.prophets"
    const val SCREEN_PROPHET_DETAIL = "screen.prophetDetail"
    const val SCREEN_KHATAM = "screen.khatam"
    const val SCREEN_KHATAM_DETAIL = "screen.khatamDetail"
    const val SCREEN_KHATAM_CREATE = "screen.khatamCreate"

    // ── Feature-specific primary actions ─────────────────────────────
    // Tasbih
    const val TASBIH_COUNTER = "tasbih.counter"
    const val TASBIH_INCREMENT = "tasbih.increment"
    const val TASBIH_RESET = "tasbih.reset"

    // Zakat
    const val ZAKAT_CASH_FIELD = "zakat.cashField"
    const val ZAKAT_CALCULATE = "zakat.calculate"
    const val ZAKAT_RESULT = "zakat.result"

    // Prayer tracker
    const val PRAYER_TRACKER_TABS = "prayerTracker.tabs"
    const val PRAYER_MARK_PRAYED = "prayerTracker.markPrayed"

    /** Stable per-prayer toggle tag, e.g. tag for "FAJR". */
    fun prayerToggle(prayerName: String): String = "prayerTracker.toggle.$prayerName"
}
