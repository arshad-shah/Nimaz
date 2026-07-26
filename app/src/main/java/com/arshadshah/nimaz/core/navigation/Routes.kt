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

    @Serializable
    data object HadithSettings : Route

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

    @Serializable
    data object DuaSettings : Route

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

    /**
     * Night worship (Tahajjud / Witr) — the destination for those two Home worship cards.
     *
     * Exists because those reminders had nowhere useful to go: the app can tell you the last
     * third of the night has begun, but had no screen answering "what now". Everything else a
     * worship card links to (dua categories, the fast tracker) already existed.
     */
    @Serializable
    data object NightWorship : Route

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

    // Qaida (children's Arabic reader) screens
    @Serializable
    data object QaidaHome : Route

    @Serializable
    data class QaidaReader(val lessonId: Int) : Route

    @Serializable
    data object QaidaLetters : Route

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

    /** Extended worship & fasting reminders (Tahajjud, Suhoor, Iftar, adhkar …). */
    @Serializable
    data object SettingsWorshipReminders : Route

    // Notification hub subscreens (#301).
    @Serializable
    data object SettingsNotificationsPrayers : Route

    @Serializable
    data object SettingsNotificationsWeekly : Route

    @Serializable
    data object SettingsNotificationsSound : Route

    @Serializable
    data object SettingsNotificationsTroubleshooting : Route

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

    @Serializable
    data class HelpTopicDetail(val topicId: String) : Route

    @Serializable
    data class HelpGuide(val guideId: String) : Route

    @Serializable
    data object SettingsSync : Route

    // Search & AI settings (Ask with Proof)
    @Serializable
    data object SearchSettings : Route

    // Onboarding
    @Serializable
    data object Onboarding : Route

    // All Bookmarks
    @Serializable
    data object AllBookmarks : Route

    // Global Search
    @Serializable
    data object GlobalSearch : Route

    // Tafseer
    @Serializable
    data object TafseerChapters : Route

    @Serializable
    data class Tafseer(val surahNumber: Int, val ayahNumber: Int = 1) : Route

    // Surah Info
    @Serializable
    data class SurahInfo(val surahNumber: Int) : Route

    // Select Reciter
    @Serializable
    data object SelectReciter : Route

    // Licenses
    @Serializable
    data object Licenses : Route

    @Serializable
    data class LicenseDetail(val libraryHashCode: Int) : Route

    // Asma ul Husna (99 Names of Allah)
    @Serializable
    data object AsmaUlHusnaList : Route

    @Serializable
    data class AsmaUlHusnaDetail(val nameId: Int) : Route

    // Asma un Nabi (99 Names of Prophet Muhammad)
    @Serializable
    data object AsmaUnNabiList : Route

    @Serializable
    data class AsmaUnNabiDetail(val nameId: Int) : Route

    // Prophets of Islam
    @Serializable
    data object ProphetsList : Route

    @Serializable
    data class ProphetDetail(val prophetId: Int) : Route

    // Khatam
    @Serializable
    data object KhatamList : Route

    @Serializable
    data class KhatamDetail(val khatamId: Long) : Route

    @Serializable
    data object KhatamCreate : Route

    @Serializable
    data class KhatamEdit(val khatamId: Long) : Route
}

// Navigation destinations for bottom navigation
enum class BottomNavDestination(val route: Route, val title: String, val icon: String) {
    HOME(Route.Home, "Home", "home"),
    QURAN(Route.Quran, "Quran", "menu_book"),
    TASBIH(Route.Tasbih, "Tasbih", "counter"),
    QIBLA(Route.QiblaNav, "Qibla", "compass"),
    MORE(Route.More, "More", "more_horiz")
}
