package com.arshadshah.nimaz.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    // Main screens (Bottom navigation)
    @Serializable
    data object Home : Route

    @Serializable
    data object Quran : Route

    @Serializable
    data object Tasbih : Route

    @Serializable
    data object QiblaNav : Route

    @Serializable
    data object More : Route

    // Quran screens
    @Serializable
    data class QuranReader(val surahNumber: Int, val ayahNumber: Int = 1) : Route

    @Serializable
    data class QuranPage(val pageNumber: Int) : Route

    @Serializable
    data class QuranJuz(val juzNumber: Int) : Route

    @Serializable
    data object QuranSearch : Route

    @Serializable
    data object QuranBookmarks : Route

    // Hadith screens
    @Serializable
    data object HadithHome : Route

    @Serializable
    data class HadithBook(val bookId: String) : Route

    @Serializable
    data class HadithChapter(val bookId: String, val chapterId: String) : Route

    @Serializable
    data class HadithReader(val hadithId: String) : Route

    @Serializable
    data object HadithSearch : Route

    @Serializable
    data object HadithBookmarks : Route

    // Dua screens
    @Serializable
    data object DuaHome : Route

    @Serializable
    data class DuaCategory(val categoryId: String) : Route

    @Serializable
    data class DuaReader(val duaId: String) : Route

    @Serializable
    data object DuaFavorites : Route

    @Serializable
    data object DuaSearch : Route

    // Prayer screens
    @Serializable
    data object PrayerTimes : Route

    @Serializable
    data class PrayerTracker(val initialTab: Int = 0) : Route

    @Serializable
    data object PrayerStats : Route

    @Serializable
    data object QadaPrayers : Route

    @Serializable
    data object MonthlyPrayerTimes : Route

    // Fasting screens
    @Serializable
    data object FastingHome : Route

    @Serializable
    data object FastingTracker : Route

    @Serializable
    data object FastingStats : Route

    @Serializable
    data object MakeupFasts : Route

    // Tasbih screens
    @Serializable
    data object TasbihHome : Route

    @Serializable
    data class TasbihCounter(val presetId: Long? = null) : Route

    @Serializable
    data object TasbihPresets : Route

    @Serializable
    data object TasbihStats : Route

    @Serializable
    data object TasbihHistory : Route

    @Serializable
    data object TasbihAddPreset : Route

    // Zakat screens
    @Serializable
    data object ZakatCalculator : Route

    @Serializable
    data object ZakatHistory : Route

    // Qibla screen
    @Serializable
    data object Qibla : Route

    // Islamic Calendar screens
    @Serializable
    data object IslamicCalendar : Route

    @Serializable
    data class IslamicMonth(val month: Int, val year: Int) : Route

    // Settings screens
    @Serializable
    data object Settings : Route

    @Serializable
    data object SettingsPrayerCalculation : Route

    @Serializable
    data object SettingsNotifications : Route

    @Serializable
    data object SettingsAppearance : Route

    @Serializable
    data object SettingsLanguage : Route

    @Serializable
    data object SettingsLocation : Route

    @Serializable
    data object SettingsAbout : Route

    @Serializable
    data object SettingsQuran : Route

    @Serializable
    data object SettingsWidgets : Route

    @Serializable
    data object SettingsHelp : Route

    // Onboarding
    @Serializable
    data object Onboarding : Route

    // All Bookmarks
    @Serializable
    data object AllBookmarks : Route

    // Global Search
    @Serializable
    data object GlobalSearch : Route

    // Surah Info
    @Serializable
    data class SurahInfo(val surahNumber: Int) : Route

    // Select Reciter
    @Serializable
    data object SelectReciter : Route
}

// Navigation destinations for bottom navigation
enum class BottomNavDestination(val route: Route, val title: String, val icon: String) {
    HOME(Route.Home, "Home", "home"),
    QURAN(Route.Quran, "Quran", "menu_book"),
    TASBIH(Route.Tasbih, "Tasbih", "counter"),
    QIBLA(Route.QiblaNav, "Qibla", "compass"),
    MORE(Route.More, "More", "more_horiz")
}
